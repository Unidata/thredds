package thredds.catalog;

import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.Attribute;
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
  static private final InvService collectionService = new InvService("collectionService", ServiceType.COMPOUND.toString(), "", "", "");
  //static private final InvService fileService = new InvService("fileService", ServiceType.COMPOUND.toString(), "", "", "");

  // LOOK ignoring the configured services
  static {
    //collectionService.addService( InvService.cdmrfeature);
    collectionService.addService( InvService.ncss);
    
    //fileService.addService( InvService.opendap);
    //fileService.addService( InvService.cdmremote);
    //fileService.addService( InvService.fileServer);
  }

  private final FeatureDatasetPoint fd;  // LOOK this stays open
  private final Set<FeatureCollectionConfig.PointDatasetType> wantDatasets;

  InvDatasetFcPoint(InvDatasetImpl parent, FeatureCollectionConfig config) {
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
    finish();

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


  /* @Override
  public void update(CollectionManager.Force force) { // this may be called from a background thread
    // deal with the first call
    boolean firstTime;
    synchronized (lock) {
      firstTime = first;
    }
    if (firstTime) {
      try {
        checkState(); // this will initialize, no update needed
      } catch (IOException e) {
        logger.error("Fail to create/update collection on first time", e);
      }
      return;
    }

    // do the update in a local object
    State localState = new State(state);
    try {
      updateCollection(localState, force);
    } catch (Throwable e) {
      logger.error("Fail to create/update collection", e);
      return;
    }
    makeDatasetTop(localState);
    localState.lastInvChange = System.currentTimeMillis();

    // switch to live
    synchronized (lock) {
      state = localState;
    }
  } */

  @Override
  public void updateCollection(State localState, CollectionUpdateType force) {
    try {
      localState.dateRange = ((UpdateableCollection)fd).update();
    } catch (IOException e) {
      logger.error("update failed", e);
    }

    /* time coverage = expect it may be changing
    if (fd.getCalendarDateRange() != null)
      localState.dateRange = fd.getCalendarDateRange();
    else if (getTimeCoverage() != null)
      localState.dateRange = getCalendarDateCoverage(); */
  }

  /* called when a request comes in, see if everything is ready to go
  // in particular, state.datasets and state.scan
  @Override
  protected State checkState() throws IOException {

    synchronized (lock) {
      if (first) {
        firstInit();
        datasetCollection.scanIfNeeded(); //always fall through to updateCollection
        first = false;
      } else {
        if (!datasetCollection.scanIfNeeded()) // return is not needed
          return state;
      }

      // copy on write
      State localState = new State(state);
      makeDatasetTop(localState); // doesnt actually change i think
      update(CollectionManager.Force.test); // call update on the fd

      state = localState;
      return state;
    }
  }   */

  @Override
  public InvCatalogImpl makeCatalog(String match, String orgPath, URI catURI) throws IOException {
    logger.debug("FcPoint make catalog for " + match + " " + catURI);
    State localState = checkState();

    try {
      if ((match == null) || (match.length() == 0)) {
        InvCatalogImpl main = makeCatalogTop(catURI, localState);
        main.addService(collectionService);
        main.getDataset().getLocalMetadataInheritable().setServiceName(collectionService.getName());
        main.finish();
        return main;

      } else if (match.startsWith(FILES) && wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.Files)) {
        return  makeCatalogFiles(catURI, localState, datasetCollection.getFilenames(), true);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + configPath, e);
    }

    return null;
  }

  @Override
  protected void makeDatasetTop(State localState) {
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      top.transferMetadata(parent, true); // make all inherited metadata local

    String id = getID();
    if (id == null) id = getPath();
    //top.setID(id);

    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    if (wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.cdmrFeature)) {

      InvDatasetImpl ds = new InvDatasetImpl(this, "Feature Collection");
      String name = getName() + "_" + FC;
      name = StringUtil2.replace(name, ' ', "_");
      ds.setUrlPath(this.configPath + "/" + name);
      ds.setID(id + "/" + name);
      ThreddsMetadata tm = ds.getLocalMetadata();
      ds.getLocalMetadataInheritable().setServiceName(collectionService.getName());
      ds.finish();
      top.addDataset(ds);
    }

    if (wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.Files) && (topDirectory != null)) {
      InvCatalogRef filesCat = new InvCatalogRef(this, FILES, getCatalogHref(FILES));
      filesCat.finish();
      top.addDataset(filesCat);
    }

    localState.top = top;
    finish();
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
