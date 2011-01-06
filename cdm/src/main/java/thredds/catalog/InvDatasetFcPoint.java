package thredds.catalog;

import org.slf4j.Logger;
import thredds.inventory.FeatureCollectionConfig;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.collection.CompositeDatasetFactory;
import ucar.nc2.ft.point.collection.UpdateableCollection;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Feature Collection for Point types
 *
 * @author caron
 * @since Nov 20, 2010
 */
public class InvDatasetFcPoint extends InvDatasetFeatureCollection {
  static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFcPoint.class);
  static private final String FC = "fc.cdmr";
  static private final InvService collectionService = new InvService("collectionService", ServiceType.COMPOUND.toString(), "", "", "");
  static private final InvService fileService = new InvService("fileService", ServiceType.COMPOUND.toString(), "", "", "");
  static {
    collectionService.addService( InvService.cdmrfeature);

    fileService.addService( InvService.cdmremote);
    fileService.addService( InvService.fileServer);
    fileService.addService( InvService.opendap);
  }

  private final FeatureDatasetPoint fd;
  private final Set<FeatureCollectionConfig.PointDatasetType> wantDatasets;

  public InvDatasetFcPoint(InvDatasetImpl parent, String name, String path, FeatureType featureType, FeatureCollectionConfig config) {
    super(parent, name, path, featureType, config);

    Formatter errlog = new Formatter();
    try {
      fd = (FeatureDatasetPoint) CompositeDatasetFactory.factory(name, featureType, dcm, errlog);
    } catch (Exception e) {
      // e.printStackTrace(); // not showing up in logs
      throw new RuntimeException("InvDatasetFcPoint", e);
    }

    this.wantDatasets = config.pointConfig.datasets;
  }

  @Override
  public FeatureDatasetPoint getFeatureDatasetPoint() { return fd; }

  @Override
  public void update() {
    ((UpdateableCollection)fd).update();
  }

  @Override
  public void updateProto() {
    // probably dont need this
  }

  // called when a request comes in, see if everything is ready to go
  // in particular, state.datasets and state.scan
  @Override
  protected State checkState() throws IOException {

    synchronized (lock) {
      if (state == null) {
        //orgService = getServiceDefault();
        //virtualService = makeVirtualService(orgService);

      } else {
        if (!dcm.rescanIfNeeded())
          return state;
      }

      // copy on write
      State localState = new State(state);
      makeDatasets(localState); // LOOK whats really needed is just the time range metadata updated
      update();

      if (null != fd) {
        localState.vars = MetadataExtractor.extractVariables(fd);
        localState.gc = MetadataExtractor.extractGeospatial(fd);
        localState.dateRange = MetadataExtractor.extractDateRange(fd);
      }
      //localState.lastProtoChange = System.currentTimeMillis();

      state = localState;
      return state;
    }
  }

  @Override
  public InvCatalogImpl makeCatalog(String match, String orgPath, URI baseURI)  {
    logger.debug("FcPoint make catalog for " + match + " " + baseURI);
    State localState = null;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    try {
      if ((match == null) || (match.length() == 0)) {
        InvCatalogImpl main = makeCatalogTop(baseURI, localState);
        main.addService(collectionService);
        main.getDataset().getLocalMetadataInheritable().setServiceName(collectionService.getName());
        main.finish();
        return main;

      } else if (match.startsWith(FILES) && wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.Files)) {
        InvCatalogImpl files = localState.scan.makeCatalogForDirectory(orgPath, baseURI);
        if (files == null) return null;
        files.addService(InvService.latest);
        files.addService(fileService);
        files.getDataset().getLocalMetadataInheritable().setServiceName(fileService.getName());
        files.finish();
        return files;
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }

  private void makeDatasets(State localState) {
     List<InvDataset> datasets = new ArrayList<InvDataset>();

     String id = getID();
     if (id == null) id = getPath();

     if (wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.cdmrFeature)) {

       InvDatasetImpl ds = new InvDatasetImpl(this, "Feature Collection");
       String name = getName() + "_" + FC;
       name = StringUtil.replace(name, ' ', "_");
       ds.setUrlPath(this.path + "/" + name);
       ds.setID(id + "/" + name);
       ThreddsMetadata tm = ds.getLocalMetadata();
       tm.addDocumentation("summary", "Feature Collection. 'Nuff said");
       ds.getLocalMetadataInheritable().setServiceName(collectionService.getName());
       ds.finish();
       datasets.add(ds);
     }

     if (wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.Files) && (topDirectory != null)) {

       // LOOK - replace this with InvDatasetScan( collectionManager) or something
       //long olderThan = (long) (1000 * fmrc.getOlderThanFilterInSecs());
       ScanFilter scanFilter = new ScanFilter(filter, -1);
       InvDatasetScan scanDataset = new InvDatasetScan((InvCatalogImpl) this.getParentCatalog(), this, "File_Access", path + "/" + FILES,
               topDirectory, scanFilter, true, "true", false, null, null, null);

       //scanDataset.addService(fileService);

       ThreddsMetadata tmi = scanDataset.getLocalMetadataInheritable();
       tmi.setServiceName(fileService.getName());
       tmi.addDocumentation("summary", "Individual data file, which comprise the Forecast Model Run Collection.");
       tmi.setGeospatialCoverage(null);
       tmi.setTimeCoverage(null);
       scanDataset.setServiceName(fileService.getName());
       scanDataset.finish();
       datasets.add(scanDataset);

       // replace all at once
       localState.scan = scanDataset;
     }

     localState.datasets = datasets;
     this.datasets = datasets;
     finish();
   }

}
