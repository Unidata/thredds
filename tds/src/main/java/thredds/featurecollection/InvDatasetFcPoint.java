package thredds.featurecollection;

import org.slf4j.Logger;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.inventory.CollectionUpdateType;
import thredds.client.catalog.tools.ThreddsMetadataAcdd;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.catalog.writer.ThreddsMetadataExtractor;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.ft.point.collection.CompositeDatasetFactory;
import ucar.nc2.ft.point.collection.UpdateableCollection;
import ucar.nc2.units.DateRange;
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

  InvDatasetFcPoint(FeatureCollectionRef parent, FeatureCollectionConfig config) {
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
      ((UpdateableCollection) fd).update();
    } catch (IOException e) {
      logger.error("update failed", e);
    }

    // time coverage = expect it may be changing
    if (fd.getCalendarDateRange() != null)
      localState.dateRange = new DateRange(fd.getCalendarDateRange());
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
  protected DatasetBuilder makeDatasetTop(URI catURI, State localState) {
    DatasetBuilder top = new DatasetBuilder(null);
    top.transferMetadata(parent, true); // make all inherited metadata local
    top.setName(name);

    ThreddsMetadata tmi = top.getInheritableMetadata();
    tmi.set(Dataset.FeatureType, FeatureType.GRID.toString()); // override GRIB
    tmi.set(Dataset.ServiceName, Virtual_Services_Name);
    if (localState.coverage != null) tmi.set(Dataset.GeospatialCoverage, localState.coverage);
    if (localState.dateRange != null) tmi.set(Dataset.TimeCoverage, localState.dateRange);
    if (localState.vars != null) tmi.set(Dataset.VariableGroups, localState.vars);

    if (wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.cdmrFeature)) {
      DatasetBuilder ds = new DatasetBuilder(top);
      ds.setName("Feature Collection");
      String myname = name + "_" + FC;
      myname = StringUtil2.replace(myname, ' ', "_");
      ds.put(Dataset.UrlPath, this.configPath + "/" + myname);
      ds.put(Dataset.Id, this.configPath + "/" + myname);
      ds.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Collection of Point Data"));
      top.addDataset(ds);
    }

    if (wantDatasets.contains(FeatureCollectionConfig.PointDatasetType.Files) && (topDirectory != null)) {
      CatalogRefBuilder filesCat = new CatalogRefBuilder(top);
      filesCat.setName(FILES);
      filesCat.setHref(getCatalogHref(FILES));
      top.addDataset(filesCat);
    }

    // pull out ACDD metadata from feature collection and put into the catalog
    ThreddsMetadataAcdd acdd = new ThreddsMetadataAcdd(Attribute.makeMap(fd.getGlobalAttributes()), top);
    acdd.extract();

    // spatial coverage
    ThreddsMetadataExtractor extractor = new ThreddsMetadataExtractor();
    ThreddsMetadata.GeospatialCoverage coverage = (ThreddsMetadata.GeospatialCoverage) top.get(Dataset.GeospatialCoverage);
    if (fd.getBoundingBox() == null) {   // pull out catalog BB, put into the feature collection. this will override ACDD
      if (coverage != null)
        ((PointDatasetImpl) fd).setBoundingBox(coverage.getBoundingBox()); // override in fd

    } else if (coverage == null) {  // otherwise extract bb from featureDataset and add to the catalog metadata
      coverage = extractor.extractGeospatial(fd);
      if (coverage != null)
        tmi.set(Dataset.GeospatialCoverage, coverage);
    }

    tmi.set(Dataset.VariableGroups, extractor.extractVariables(fd));


    return top;
  }


}
