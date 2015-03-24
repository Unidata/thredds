package thredds.featurecollection;

import org.slf4j.Logger;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.ft.point.collection.CompositeDatasetFactory;
import ucar.nc2.ft.point.collection.UpdateableCollection;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.thredds.MetadataExtractorAcdd;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * InvDataset Feature Collection for Point types.
 * Implement with CompositeDatasetFactory
 *
 * @author caron
 * @since Nov 20, 2010
 */
public class InvDatasetFcPoint extends InvDatasetFeatureCollection {
  static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFcPoint.class);
  static private final String FC = "fc.cdmr";

  private final FeatureDatasetPoint fd;  // LOOK this stays open
  private final Set<FeatureCollectionConfig.PointDatasetType> wantDatasets;

  InvDatasetFcPoint(Dataset parent, FeatureCollectionConfig config) {
    super(parent, config);
    makeCollection();

    Formatter errlog = new Formatter();
    try {
      fd = (FeatureDatasetPoint) CompositeDatasetFactory.factory(name, fcType.getFeatureType(), datasetCollection, errlog);

    } catch (Exception e) {

      if (e.getCause() != null)
        throw new RuntimeException("Failed to create InvDatasetFcPoint, cause=", e.getCause());
      else
        throw new RuntimeException("Failed to create InvDatasetFcPoint", e);
    }

    state = new State(null);
    this.wantDatasets = config.pointConfig.datasets;
  }

  @Override
  public void finishConstruction() {
    super.finishConstruction();

    ThreddsMetadata tmi = getLocalMetadataInheritable();

    // pull out ACDD metadata from feature collection and put into the catalog
    MetadataExtractorAcdd acdd = new MetadataExtractorAcdd( Attribute.makeMap(fd.getGlobalAttributes()), this, tmi);
    acdd.extract();
    finish();

    // spatial coverage
    if (fd.getBoundingBox() == null) {
      // pull out catalog BB, put into the feature collection. this will override ACDD
      thredds.catalog.ThreddsMetadata.GeospatialCoverage coverage = getGeospatialCoverage();
      if (coverage != null)
        ((PointDatasetImpl) fd).setBoundingBox(coverage.getBoundingBox()); // override in fd

    } else if (getGeospatialCoverage() == null) {
      tmi.setGeospatialCoverage( MetadataExtractor.extractGeospatial(fd));
    }

    tmi.addVariables(MetadataExtractor.extractVariables(fd));

    finish();
  }

  @Override
  public FeatureDataset getFeatureDataset() {
    return fd;
  }
  
    // for gridded data
  protected Service makeDefaultServices() {
    List<Service> nested = new ArrayList<>();
    // allowedServices.addIfAllowed(ServiceType.CdmrFeature, nested);
    allowedServices.addIfAllowed(ServiceType.NetcdfSubset, nested);

    return new Service(Default_Services_Name, "", ServiceType.Compound.toString(), null, null, nested, null);
  }

  @Override
  public void updateCollection(State localState, CollectionUpdateType force) {
    try {
      ((UpdateableCollection)fd).update();
    } catch (IOException e) {
      logger.error("update failed", e);
    }

    // time coverage = expect it may be changing
    if (fd.getCalendarDateRange() != null)
      localState.dateRange = fd.getCalendarDateRange();
  }

  @Override
  public Catalog makeCatalog(String match, String orgPath, URI catURI) throws IOException {
    logger.debug("FcPoint make catalog for " + match + " " + catURI);
    State localState = checkState();

    try {
      if ((match == null) || (match.length() == 0)) {
        CatalogBuilder main = makeCatalogTop(catURI, localState);
        main.addService(virtualService);
        return main.makeCatalog();

      } else if (match.startsWith(FILES) && wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.Files)) {
        return makeCatalogFiles(catURI, localState, datasetCollection.getFilenames(), true);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + configPath, e);
    }

    return null;
  }

  @Override
  protected void makeDatasetTop(State localState) {
    DatasetBuilder top = new DatasetBuilder(null);
    top.transferMetadata(parent, true); // make all inherited metadata local
    top.setName(name);

    String id = getId();
    top.put(Dataset.Id, id);

    Map<String, Object> tmi = top.getInheritableMetadata().getFlds();
    tmi.put(Dataset.FeatureType, FeatureType.GRID.toString()); // override GRIB
    tmi.put(Dataset.ServiceName, Virtual_Services_Name);
    if (localState.coverage != null) tmi.put(Dataset.GeospatialCoverage, localState.coverage);
    if (localState.dateRange != null) tmi.put(Dataset.TimeCoverage, localState.dateRange);
    if (localState.vars != null) tmi.put(Dataset.VariableGroups, localState.vars);

    if (wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.cdmrFeature)) {
      DatasetBuilder ds = new DatasetBuilder(top);
      ds.setName("Feature Collection");
      String myname = name + "_" + FC;
      myname = StringUtil2.replace(myname, ' ', "_");
      ds.put(Dataset.UrlPath, this.configPath + "/" + myname);
      ds.put(Dataset.Id, id + "/" + myname);
      ds.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Collection of Point Data"));
      top.addDataset(ds);
    }

    if (wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.Files) && (topDirectory != null)) {
      CatalogRefBuilder filesCat = new CatalogRefBuilder(top);
      filesCat.setName(FILES);
      filesCat.setHref(getCatalogHref(FILES));
      top.addDataset(filesCat);
    }

    localState.top = top;
  }

  /*
  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(String matchPath) throws IOException {
    return null;
  }

  @Override
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    return null;
  }   */



}
