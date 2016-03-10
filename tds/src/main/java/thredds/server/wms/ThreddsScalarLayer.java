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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package thredds.server.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.Chronology;
import org.joda.time.DateTime;

import thredds.server.wms.config.LayerSettings;
import thredds.server.wms.config.WmsDetailedConfig;
import ucar.nc2.Attribute;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.cdm.CdmUtils;
import uk.ac.rdg.resc.edal.cdm.DataReadingStrategy;
import uk.ac.rdg.resc.edal.cdm.PixelMap;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer;

/**
 * Wraps a GridDatatype as a ScalarLayer object
 *
 * @author Jon
 * @todo Implement more efficient getTimeseries()
 */
class ThreddsScalarLayer extends AbstractScalarLayer implements ThreddsLayer {

  //private static Logger log = LoggerFactory.getLogger(ThreddsScalarLayer.class);

  private GridDatatype grid;
  private ThreddsDataset dataset;
  private List<DateTime> times;
  private DataReadingStrategy dataReadingStrategy;

  // Will be set in ThreddsWmsController.ThreddsLayerFactory
  private LayerSettings layerSettings;

  public ThreddsScalarLayer(CoverageMetadata cm) {
    super(cm);
  }

  public List<List<Float>> readVerticalSection(DateTime dt, List<Double> list, Domain<HorizontalPosition> domain) throws InvalidDimensionValueException, IOException {

    int tIndex = this.findAndCheckTimeIndex(dt);

    // Defend against null values
    List<Integer> zIndices;
    if (list == null) {
      zIndices = Arrays.asList(-1);
    } else {
      zIndices = new ArrayList<>(list.size());
      for (Double el : list) {
        zIndices.add(this.findAndCheckElevationIndex(el));
      }
    }
    HorizontalGrid hg = CdmUtils.createHorizontalGrid(grid.getCoordinateSystem());
    PixelMap pixelMap = new PixelMap(hg, domain);
    return CdmUtils.readVerticalSection(null, grid, tIndex, zIndices, pixelMap, dataReadingStrategy, (int) domain.size());
  }

  /**
   * Static factory method. Builds a new ThreddsScalarLayer and set several properties
   */
  public static ThreddsScalarLayer getNewLayer(CoverageMetadata cm, GridDatatype gdt, DataReadingStrategy drStrategy, ThreddsDataset ds, WmsDetailedConfig wmsConfig) {
    ThreddsScalarLayer tsl = new ThreddsScalarLayer(cm);
    tsl.setGridDatatype(gdt);
    tsl.setTimeValues(cm.getTimeValues());
    tsl.setDataReadingStrategy(drStrategy);
    tsl.setDataset(ds);
    tsl.setLayerSettings(wmsConfig.getSettings(tsl));
    return tsl;
  }

  @Override
  public String getName() {
    return this.getId();
  }

  @Override
  public ThreddsDataset getDataset() {
    return this.dataset;
  }

  public void setDataset(ThreddsDataset dataset) {
    this.dataset = dataset;
  }

  public void setGridDatatype(GridDatatype grid) {
    this.grid = grid;
  }

  @Override
  public Chronology getChronology() {
    if (this.times == null || this.times.isEmpty()) return null;
    return this.times.get(0).getChronology();
  }

  @Override
  public List<DateTime> getTimeValues() {
    return this.times;
  }

  public void setTimeValues(List<DateTime> timeValues) {
    this.times = timeValues;
  }

  public void setDataReadingStrategy(DataReadingStrategy dataReadingStrategy) {
    this.dataReadingStrategy = dataReadingStrategy;
  }

  @Override
  public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy)
          throws InvalidDimensionValueException, IOException {
    //Domain singlePoint = (Domain<HorizontalPosition>)xy;
    Domain<HorizontalPosition> singlePoint = new HorizontalDomain(xy);
    return this.readHorizontalPoints(time, elevation, singlePoint).get(0);
  }

  @Override
  public List<Float> readHorizontalPoints(DateTime time, double elevation, Domain<HorizontalPosition> points)
          throws InvalidDimensionValueException, IOException {
    int tIndex = this.findAndCheckTimeIndex(time);
    int zIndex = this.findAndCheckElevationIndex(elevation);
    HorizontalGrid hg = CdmUtils.createHorizontalGrid(grid.getCoordinateSystem());
    PixelMap pixelMap = new PixelMap(hg, points);
    List<Float> horizontalPoints;
    try {
      horizontalPoints = CdmUtils.readHorizontalPoints(null, grid, tIndex, zIndex, pixelMap, this.dataReadingStrategy, (int) points.size());
    } catch (Exception e) {
      //Catching and wrapping any exception reading data into a new IOException
      throw new IOException(e);
    }
    return horizontalPoints;
  }
    
  @Override
  public List<Float> readTimeseries(List<DateTime> times, double elevation, HorizontalPosition xy)
      throws InvalidDimensionValueException, IOException
  {
      HorizontalGrid horizGrid = CdmUtils.createHorizontalGrid(grid.getCoordinateSystem());
      List<Integer> tIndices = new ArrayList<Integer>(times.size());
      for (DateTime time : times)
          tIndices.add(this.findAndCheckTimeIndex(time));
      int zIndex = this.findAndCheckElevationIndex(elevation);
      return CdmUtils.readTimeseries(null, grid, horizGrid, tIndices, zIndex, xy);
  }

  public String getStandardName() {
    if (this.grid == null) return null;
    Attribute stdNameAtt = this.grid.findAttributeIgnoreCase("standard_name");
    if (stdNameAtt == null || stdNameAtt.getStringValue().trim().equals("")) {
      return null;
    }
    return stdNameAtt.getStringValue();
  }

  public void setLayerSettings(LayerSettings layerSettings) {
    this.layerSettings = layerSettings;
  }

  /// The properties below are taken from the LayerSettings

  @Override
  public boolean isQueryable() {
    return this.layerSettings.isAllowFeatureInfo();
  }

  //@Override
  public boolean isIntervalTime() {
    return this.layerSettings.isIntervalTime();
  }

  @Override
  public Range<Float> getApproxValueRange() {
    return this.layerSettings.getDefaultColorScaleRange();
  }

  @Override
  public boolean isLogScaling() {
    return this.layerSettings.isLogScaling();
  }

  @Override
  public ColorPalette getDefaultColorPalette() {
    return ColorPalette.get(this.layerSettings.getDefaultPaletteName());
  }

  @Override
  public int getDefaultNumColorBands() {
    return this.layerSettings.getDefaultNumColorBands();
  }

}