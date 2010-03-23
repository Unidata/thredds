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

package thredds.catalog;

import org.slf4j.Logger;
import thredds.inventory.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.units.DateRange;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Feature Collection (experimental)
 *
 * @author caron
 * @since Mar 3, 2010
 */
public class InvDatasetFeatureCollection extends InvCatalogRef {
  static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFeatureCollection.class);
  static private final String FMRC = "fmrc.ncd";
  static private final String BEST = "best.ncd";

  private final String path;
  private Fmrc fmrc;
  private FeatureType featureType;

  public InvDatasetFeatureCollection(InvDatasetImpl parent, String name, String path, String featureType) {
    super(parent, name, "/thredds/catalog/" + path + "/catalog.xml");
    this.path = path;
    this.featureType = FeatureType.getType(featureType);
  }

  public void setCollection(String spec, String olderThan, String recheckEvery) {
    Formatter errlog = new Formatter();
    CollectionSpecParser sp = new CollectionSpecParser(spec, errlog);
    DatasetCollectionManager dcm = new DatasetCollectionManager(sp, olderThan, errlog);
    dcm.setRecheck(recheckEvery);
    CollectionManager manager = dcm;

    // optional date extraction is used to get rundates when not embedded in the file
    DateExtractor dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    fmrc = new Fmrc(manager, dateExtractor);
  }

  public String getPath() {
    return path;
  }

  public NetcdfDataset getNetcdfDataset(String name) throws IOException {
    GridDataset gds = getGridDataset(name);
    return (NetcdfDataset) gds.getNetcdfFile();
  }

  public GridDataset getGridDataset(String name) throws IOException {
    return fmrc.getDataset2D(false);
  }

  public InvCatalogImpl makeCatalog(String match, String orgPath, URI baseURI) {
    try {
      return makeCatalog(baseURI);
    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
      return null;
    }
  }

  /**
   * Make the top catalog.
   *
   * @param baseURI base URI of the request
   * @return the top FMRC catalog
   * @throws java.io.IOException         on I/O error
   * @throws java.net.URISyntaxException if path is misformed
   */
  private InvCatalogImpl makeCatalog(URI baseURI) throws IOException, URISyntaxException {

    //if (topCatalog == null) {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getXlinkHref());
    InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), myURI);

    InvDatasetImpl top = new InvDatasetImpl(this); // LOOK clone correct ??
    top.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      top.transferMetadata(parent); // make all inherited metadata local

    String id = getID();
    if (id == null)
      id = getPath();
    top.setID(id);

    GridDataset gds = getGridDataset(null);

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (tmi.getVariables().size() == 0) {
      ThreddsMetadata.Variables vars = MetadataExtractor.extractVariables(this, gds);
      if (vars != null)
        tmi.addVariables(vars);
    }
    if (tmi.getGeospatialCoverage() == null) {
      ThreddsMetadata.GeospatialCoverage gc = MetadataExtractor.extractGeospatial(gds);
      if (gc != null)
        tmi.setGeospatialCoverage(gc);
    }
    if (tmi.getTimeCoverage() == null) {
      DateRange dateRange = MetadataExtractor.extractDateRange(gds);
      if (dateRange != null)
        tmi.setTimeCoverage(dateRange);
    }

    mainCatalog.addDataset(top);

    // any referenced services need to be local
    List serviceLocal = getServicesLocal();
    for (InvService service : parentCatalog.getServices()) {
      if (!serviceLocal.contains(service))
        mainCatalog.addService(service);
    }
    findDODSService(parentCatalog.getServices()); // LOOK kludgey

    for (InvDataset ds : getDatasets()) {
      top.addDataset((InvDatasetImpl) ds);
    }
    mainCatalog.finish();
    InvCatalogImpl topCatalog = mainCatalog;
    //}

    return topCatalog;
  }

  private String dodsService; // LOOK ??

  private void findDODSService(List<InvService> services) {
    for (InvService service : services) {
      if ((dodsService == null) && (service.getServiceType() == ServiceType.OPENDAP)) {
        dodsService = service.getName();
        return;
      }
      if (service.getServiceType() == ServiceType.COMPOUND)
        findDODSService(service.getServices());
    }
  }

  @Override
  public java.util.List<InvDataset> getDatasets() {
    List<InvDataset> datasets = new ArrayList<InvDataset>();

    String id = getID();
    if (id == null)
      id = getPath();

    InvDatasetImpl ds = new InvDatasetImpl(this, "Forecast Model Run Collection (2D time coordinates)");
    String name = getName() + "_" + FMRC;
    name = StringUtil.replace(name, ' ', "_");
    ds.setUrlPath(path + "/" + name);
    ds.setID(id + "/" + name);
    ThreddsMetadata tm = ds.getLocalMetadata();
    tm.addDocumentation("summary", "Forecast Model Run Collection (2D time coordinates).");
    ds.getLocalMetadataInheritable().setServiceName(dodsService); // LOOK why ??
    ds.finish();
    datasets.add(ds);

    ds = new InvDatasetImpl(this, "Best Time Series");
    name = getName() + "_" + BEST;
    name = StringUtil.replace(name, ' ', "_");
    ds.setUrlPath(path + "/" + name);
    ds.setID(id + "/" + name);
    tm = ds.getLocalMetadata();
    tm.addDocumentation("summary", "Best time series, taking the data from the most recent run available.");
    ds.finish();
    datasets.add(ds);

    this.datasets = datasets;

    finish();
    return datasets;
  }


}
