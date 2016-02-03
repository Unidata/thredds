/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thredds.server.dataset.DatasetException;
import thredds.server.dataset.TdsRequestedDataset;
import thredds.server.wms.config.WmsDetailedConfig;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.cdm.CdmUtils;
import uk.ac.rdg.resc.edal.cdm.DataReadingStrategy;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * A {@link uk.ac.rdg.resc.ncwms.wms.Dataset} that provides access to layers read from
 * {@link ucar.nc2.dataset.NetcdfDataset} objects.
 *
 * @author Jon
 */
public class ThreddsDataset implements Dataset {

  private static final Logger log = LoggerFactory.getLogger(ThreddsDataset.class);

  private final String urlPath;
  private final String title;
  private final Map<String, ThreddsScalarLayer> scalarLayers = new LinkedHashMap<>();
  private final Map<String, ThreddsVectorLayer> vectorLayers = new LinkedHashMap<>();

  /**
   * Creates a new ThreddsDataset with the given id from the given NetcdfDataset
   */
  private ThreddsDataset(String urlPath, GridDataset gridDataset, WmsDetailedConfig wmsConfig) throws IOException {
    this.urlPath = urlPath;
    this.title = gridDataset.getTitle();

    NetcdfDataset ncDataset = (NetcdfDataset) gridDataset.getNetcdfFile();

    DataReadingStrategy drStrategy = CdmUtils.getOptimumDataReadingStrategy(ncDataset);
    // Now load the scalar layers
    Collection<CoverageMetadata> ccm = CdmUtils.readCoverageMetadata(gridDataset);
    Iterator<CoverageMetadata> icm = ccm.iterator();
    while (icm.hasNext()) {
      CoverageMetadata cm = icm.next();
      // Get the most appropriate data-reading strategy for this dataset
      //PixelMap pxm = new PixelMap(cm.getHorizontalGrid(), null);
      //DataReadingStrategy drStrategy = CdmUtils.getOptimumDataReadingStrategy( pxm, ncDataset );
      GridDatatype gdt = gridDataset.findGridDatatype(cm.getId());
      //GridDatatype gdt = gridDataset.findGridByShortName(cm.getId());
      ThreddsScalarLayer tsl = ThreddsScalarLayer.getNewLayer(cm, gdt, drStrategy, this, wmsConfig);
      this.scalarLayers.put(tsl.getName(), tsl);
    }
    //CdmUtils.findAndUpdateLayers( gridDataset, THREDDS_LAYER_BUILDER, this.scalarLayers );


    // Find the vector quantities
    Collection<VectorLayer> vectorLayersColl = WmsUtils.findVectorLayers(this.scalarLayers.values());
    // Add the vector quantities to the map of layers
    for (VectorLayer vecLayer : vectorLayersColl) {
      // We must wrap these vector layers as ThreddsVectorLayers to ensure that
      // the name of each layer matches its id.
      ThreddsVectorLayer tdsVecLayer = new ThreddsVectorLayer(vecLayer);
      tdsVecLayer.setLayerSettings(wmsConfig.getSettings(tdsVecLayer));
      this.vectorLayers.put(vecLayer.getId(), tdsVecLayer);
    }
  }

  /**
   * Creates a new ThreddsDataset for one single layer
   */
  private ThreddsDataset(String urlPath, GridDataset gd, List<String> layers, WmsDetailedConfig wmsConfig) {

    this.urlPath = urlPath;
    this.title = gd.getTitle();

    NetcdfDataset ncDataset = (NetcdfDataset) gd.getNetcdfFile();
    DataReadingStrategy drStrategy = CdmUtils.getOptimumDataReadingStrategy(ncDataset);

    for (String layer : layers) {
      //GridDatatype gdt = gd.findGridByShortName(layer);
      GridDatatype gdt = gd.findGridDatatype(layer);
      CoverageMetadata cm = CdmUtils.readCoverageMetadata(gdt);
      ThreddsScalarLayer tsl = ThreddsScalarLayer.getNewLayer(cm, gdt, drStrategy, this, wmsConfig);
      this.scalarLayers.put(tsl.getName(), tsl);
    }

    // Find the vector quantities
    Collection<VectorLayer> vectorLayersColl = WmsUtils.findVectorLayers(this.scalarLayers.values());
    // Add the vector quantities to the map of layers
    for (VectorLayer vecLayer : vectorLayersColl) {
      // We must wrap these vector layers as ThreddsVectorLayers to ensure that
      // the name of each layer matches its id.
      ThreddsVectorLayer tdsVecLayer = new ThreddsVectorLayer(vecLayer);
      tdsVecLayer.setLayerSettings(wmsConfig.getSettings(tdsVecLayer));
      this.vectorLayers.put(vecLayer.getId(), tdsVecLayer);
    }

  }

  /**
   * Uses the {@link #getDatasetPath() url path} as the unique id.
   */
  @Override
  public String getId() {
    return this.urlPath;
  }

  @Override
  public String getTitle() {
    return this.title;
  }

  /**
   * Gets the path that was specified on the incoming URL
   */
  public String getDatasetPath() {
    return this.urlPath;
  }

  /**
   * Returns the current time, since datasets could change at any time without
   * our knowledge.
   *
   * @see ThreddsServerConfig#getLastUpdateTime()
   */
  @Override
  public DateTime getLastUpdateTime() {
    return new DateTime();
  }

  /**
   * Gets the {@link uk.ac.rdg.resc.ncwms.wms.Layer} with the given {@link uk.ac.rdg.resc.ncwms.wms.Layer#getId() id}.  The id
   * is unique within the dataset, not necessarily on the whole server.
   *
   * @return The layer with the given id, or null if there is no layer with
   * the given id.
   * @todo repetitive of code in ncwms.config.Dataset: any way to refactor?
   */
  @Override
  public ThreddsLayer getLayerById(String layerId) {
    ThreddsLayer layer = this.scalarLayers.get(layerId);
    if (layer == null)
      layer = this.vectorLayers.get(layerId);

    return layer;
  }

  /**
   * @todo repetitive of code in ncwms.config.Dataset: any way to refactor?
   */
  @Override
  public Set<Layer> getLayers() {
    Set<Layer> layerSet = new LinkedHashSet<>();
    layerSet.addAll(this.scalarLayers.values());
    layerSet.addAll(this.vectorLayers.values());
    return layerSet;
  }

  /**
   * Returns an empty string
   */
  @Override
  public String getCopyrightStatement() {
    return "";
  }

  /**
   * Returns an empty string
   */
  @Override
  public String getMoreInfoUrl() {
    return "";
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public boolean isLoading() {
    return false;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public Exception getException() {
    return null;
  }

  @Override
  public boolean isDisabled() {
    return false;
  }

  /**
   * Builds a ThreddsDataset specific for each WMS requests that contains only the layers needed by the requests.
   *
   * @return ThreddsDataset
   * @throws IOException
   * @throws DatasetException
   * @throws WmsException
   */
  static ThreddsDataset getThreddsDatasetForRequest(String request, GridDataset gridDataset, TdsRequestedDataset reqDataset, WmsDetailedConfig wmsConfig, RequestParams params) throws IOException, DatasetException, WmsException {

    ThreddsDataset tdsds = null;

    //GetLegendGraphic may not even need to create a ThreddsDataset (if no text is required)
    if (request.equals("GetLegendGraphic") && params.getString("LAYER") == null) return null;

    if (params.getString("LAYERS") == null && params.getString("LAYER") == null && params.getString("LAYERNAME") == null) {
      tdsds = new ThreddsDataset(reqDataset.getPath(), gridDataset, wmsConfig);
    } else {
      //Only one layer for each request. Need two components for vector layers!
      String layers = params.getString("LAYERS");
      if (layers == null)
        layers = params.getString("LAYER");
      if (layers == null)
        layers = params.getString("LAYERNAME");

      List<String> requestedLayers = getLayerComponents(gridDataset, layers);
      tdsds = new ThreddsDataset(reqDataset.getPath(), gridDataset, requestedLayers, wmsConfig);
    }
    return tdsds;
  }

  /**
   * Here we have to revert what was done in WMSUtils methods for creating the virtual datasets
   */
  private static List<String> getLayerComponents(GridDataset gd, String layer) {

    List<String> layers = new ArrayList<>();


    //Layer name is grid.getFullName()  --> vs.getFullName()
    GridDatatype grid = gd.findGridDatatype(layer); //Actually searches by vs.getFullName()


    if (grid == null) {
      List<GridDatatype> grids = gd.getGrids();
      Iterator<GridDatatype> gridsIt = grids.iterator();

      while (gridsIt.hasNext() && layers.size() < 2) {
        GridDatatype g = gridsIt.next();
        //Search the components by standard_name, long_name and fullname
        VariableDS var = g.getVariable();
        Attribute stdName = var.findAttributeIgnoreCase("standard_name");
        if (stdName != null) {

          if (isComponent(layer, stdName.getStringValue()))
            layers.add(var.getFullName());

        } else {
          Attribute longName = var.findAttributeIgnoreCase("long_name");
          if (longName != null) {
            if (isComponent(layer, longName.getStringValue()))
              layers.add(var.getFullName());

          } else {//full name
            if (isComponent(layer, var.getFullName()))
              layers.add(var.getFullName());
          }
        }

      }

    } else {
      layers.add(layer);
    }

    return layers;
  }

  /**
   * Returns true if varAtt is the name of a scalar component of the vector layer layerName
   *
   * Check if the result of removing standard components prefixes for CF-1.0 or
   * Grib convention from varAtt is the layerName.
   *
   * Handled standard components:
   *    - x, y
   *    - eastward, northward
   *    - Meridional, Zonal
   *    - u-component of, v-component of
   *
   * They can appear at the begining, in the middle or at the end, separated by _ or space characters
   *
   * @param layerName
   * @param varAtt
   * @return
    */
  static boolean isComponent(String layerName, String varAtt) {

    String componentRegex = "(x|y|eastward|northward|meridional|zonal|u-component of|v-component of)";
    String separatorRegex = "(_|\\s+)";
    String lookBehindRegex = "(?<=_|\\s|^)";

    // Regex in two cases:
    //  1: begining and middle (ex: eastward_sea_water_velocity or barotropic_sea_water_x_velocity)
    //  2: end (ex: sea_water_velocity_x)
    String regex = lookBehindRegex + componentRegex + separatorRegex + "|" +
                   separatorRegex + componentRegex + "$";

		return layerName.equals(varAtt.replaceAll("(?i)" + regex, ""));
  }
}
