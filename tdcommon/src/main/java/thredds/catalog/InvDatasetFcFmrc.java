package thredds.catalog;

import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFileCollectionManager;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.util.StringUtil2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * InvDataset Feature Collection for Fmrc
 * Generate anew each call; use object caching if needed to improve efficiency
 *
 * @author caron
 * @since Mar 3, 2010
 */

@ThreadSafe
public class InvDatasetFcFmrc extends InvDatasetFeatureCollection {
  static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFcFmrc.class);

  static private final String FMRC = "fmrc.ncd";
  static private final String BEST = "best.ncd";

  static private final String RUNS = "runs";
  static private final String RUN_NAME = "RUN_";
  static private final String RUN_TITLE = "Forecast Model Run";

  static private final String FORECAST = "forecast";
  static private final String FORECAST_NAME = "ConstantForecast_";
  static private final String FORECAST_TITLE = "Constant Forecast Date";

  static private final String OFFSET = "offset";
  static private final String OFFSET_NAME = "Offset_";
  static private final String OFFSET_TITLE = "Constant Forecast Offset";

  //////////////////////////////////////////////////////////////////////////////

  private final Fmrc fmrc;
  private final Set<FeatureCollectionConfig.FmrcDatasetType> wantDatasets;

  InvDatasetFcFmrc(InvDatasetImpl parent, FeatureCollectionConfig config) {
    super(parent, config);
    makeCollection();
    tmi.setDataType( FeatureType.GRID); // override FMRC

    Formatter errlog = new Formatter();
    try {
      fmrc = new Fmrc(datasetCollection, config);
    } catch (Exception e) {
      throw new RuntimeException(errlog.toString());
    }

    this.wantDatasets = config.fmrcConfig.datasets;

    state = new State(null);
    finish(); // ??
  }

  /* @Override  // overriding superclass -WHY?
  public void update(CollectionUpdateType force) {
    fmrc.update();       // so when is work done?
  } */

  protected void update(CollectionUpdateType force) throws IOException {  // this may be called from a background thread, or from checkState() request thread
    logger.debug("update {} force={}", name, force);
    boolean changed;
    MFileCollectionManager dcm;
    switch (force) {
      case always:
      case test:
        dcm = (MFileCollectionManager) getDatasetCollectionManager();
        changed = dcm.scan(false);
        if (changed)
          super.update(force);
        break;

      case never:
        return;

      default :
        super.update(force);
    }
  }


  public void updateProto() {
    fmrc.updateProto();
  }

  @Override
  protected void updateCollection(State localState, CollectionUpdateType force) {  // LOOK probably not right
    try {
      fmrc.update();

      //boolean checkInv = localState.lastInvChange == 0 || fmrc.checkInvState(localState.lastInvChange);
      //boolean checkProto = localState.lastProtoChange == 0 || fmrc.checkProtoState(localState.lastProtoChange);

      //if (checkProto) {
        // add Variables, GeospatialCoverage, TimeCoverage
        GridDataset gds = fmrc.getDataset2D(null);                                   // LOOK is there a resource leak ??
        if (null != gds) {
            localState.vars = MetadataExtractor.extractVariables(this, gds);
            localState.coverage = MetadataExtractor.extractGeospatial(gds);
            localState.dateRange = MetadataExtractor.extractCalendarDateRange(gds);
          }
        localState.lastProtoChange = System.currentTimeMillis();
      //}

     // if (checkInv) {
        makeDatasetTop(localState);
        localState.lastInvChange = System.currentTimeMillis();
      //}

    } catch (IOException e) {
      logger.error("FMRC updateCollection", e);
      e.printStackTrace();
    }

  }

    ///////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when the catref is requested

  @Override
  public InvCatalogImpl makeCatalog(String match, String orgPath, URI catURI) throws IOException {
    logger.debug("FMRC make catalog for " + match + " " + catURI);
    State localState = checkState();

    try {
      if ((match == null) || (match.length() == 0)) {
        InvCatalogImpl main  = makeCatalogTop(catURI, localState);
        main.addService(virtualService);
        main.getDataset().getLocalMetadataInheritable().setServiceName(virtualService.getName());
        main.finish();
        return main;
      }

      else if (match.equals(RUNS) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs))
        return makeCatalogRuns(catURI, localState);

      else if (match.equals(OFFSET) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets))
        return makeCatalogOffsets(catURI, localState);

      else if (match.equals(FORECAST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts))
        return makeCatalogForecasts(catURI, localState);

      else if (match.startsWith(FILES) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Files)) {
        return  makeCatalogFiles(catURI, localState, datasetCollection.getFilenames(), true);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + configPath, e);
    }

    return null;
  }

   private InvCatalogImpl makeCatalogRuns(URI catURI, State localState) throws IOException {

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    //URI myURI = baseURI.resolve(getCatalogHref(RUNS));
    InvCatalogImpl runCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), catURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(RUN_TITLE);
    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.coverage != null) tmi.setGeospatialCoverage(localState.coverage);
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    runCatalog.addDataset(top);

    // services need to be local
    runCatalog.addService(virtualService);
    top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    for (InvDatasetImpl ds : makeRunDatasets())
      top.addDataset(ds);

    runCatalog.finish();

    return runCatalog;
  }

  private InvCatalogImpl makeCatalogOffsets(URI catURI, State localState) throws IOException {

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    //URI myURI = baseURI.resolve(getCatalogHref(OFFSET));
    InvCatalogImpl offCatalog;
    offCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), catURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.coverage != null) tmi.setGeospatialCoverage(localState.coverage);
    if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    top.setName(OFFSET_TITLE);
    offCatalog.addDataset(top);

    // services need to be local
    offCatalog.addService(virtualService);
    top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    for (InvDatasetImpl ds : makeOffsetDatasets())
      top.addDataset(ds);

    offCatalog.finish();

    return offCatalog;
  }

  private InvCatalogImpl makeCatalogForecasts(URI catURI, State localState) throws IOException {

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    //URI myURI = baseURI.resolve(getCatalogHref(FORECAST));
    InvCatalogImpl foreCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), catURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(FORECAST_TITLE);
    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.coverage != null) tmi.setGeospatialCoverage(localState.coverage);
    if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    foreCatalog.addDataset(top);

    // services need to be local
    foreCatalog.addService(virtualService);
    foreCatalog.addService(cdmrService);
    top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    for (InvDatasetImpl ds : makeForecastDatasets())
      top.addDataset(ds);

    foreCatalog.finish();

    return foreCatalog;
  }


  private List<InvDatasetImpl> makeRunDatasets() throws IOException {

    List<InvDatasetImpl> datasets = new ArrayList<>();

    String id = getID();
    if (id == null)
      id = getPath();

    for (CalendarDate runDate : fmrc.getRunDates()) {
      String name = getName() + "_" + RUN_NAME + runDate;
      name = StringUtil2.replace(name, ' ', "_");
      InvDatasetImpl nested = new InvDatasetImpl(this, name);
      nested.setUrlPath(configPath + "/" + RUNS + "/" + name);
      nested.setID(configPath + "/" + RUNS + "/" + name);
      //nested.setID(id + "/" + RUNS + "/" + name);
      ThreddsMetadata tm = nested.getLocalMetadata();
      tm.addDocumentation("summary", "Data from Run " + name);
      CalendarDateRange dr = fmrc.getDateRangeForRun(runDate);
      if (dr != null)
        tm.setTimeCoverage(dr);
      datasets.add(nested);
    }

    Collections.reverse(datasets);
    return datasets;
  }

  private List<InvDatasetImpl> makeOffsetDatasets() throws IOException {

    List<InvDatasetImpl> datasets = new ArrayList<>();

    String id = getID();
    if (id == null)
      id = getPath();

    for (double offset : fmrc.getForecastOffsets()) {
      String name = getName() + "_" + OFFSET_NAME + offset + "hr";
      name = StringUtil2.replace(name, ' ', "_");
      InvDatasetImpl nested = new InvDatasetImpl(this, name);
      nested.setUrlPath(configPath + "/" + OFFSET + "/" + name);
      nested.setID(configPath + "/" + OFFSET + "/" + name);
      //nested.setID(id + "/" + OFFSET + "/" + name);
      ThreddsMetadata tm = nested.getLocalMetadata();
      tm.addDocumentation("summary", "Data from the " + offset + " hour forecasts, across different model runs.");
      CalendarDateRange dr = fmrc.getDateRangeForOffset(offset);
      if (dr != null)
        tm.setTimeCoverage(dr);
      datasets.add(nested);
    }

    return datasets;
  }

  private List<InvDatasetImpl> makeForecastDatasets() throws IOException {

    List<InvDatasetImpl> datasets = new ArrayList<>();

    String id = getID();
    if (id == null)
      id = getPath();

    for (CalendarDate forecastDate : fmrc.getForecastDates()) {
      String name = getName() + "_" + FORECAST_NAME + forecastDate;
      name = StringUtil2.replace(name, ' ', "_");
      InvDatasetImpl nested = new InvDatasetImpl(this, name);
      nested.setUrlPath(configPath + "/" + FORECAST + "/" + name);
      nested.setID(configPath + "/" + FORECAST + "/" + name);
      //nested.setID(id + "/" + FORECAST + "/" + name);
      ThreddsMetadata tm = nested.getLocalMetadata();
      tm.addDocumentation("summary", "Data with the same forecast date, " + name + ", across different model runs.");
      tm.setTimeCoverage(CalendarDateRange.of(forecastDate, forecastDate));
      datasets.add(nested);
    }

    return datasets;
  }


  /////////////////////////////////////////////////////////////////////////

  protected void makeDatasetTop(State localState) {
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      top.transferMetadata(parent, true); // make all inherited metadata local

    /* String id = getID();
    if (id == null) id = getPath();
    top.setID(id); */

    /* called anytime something changes. may need to do it only once ??

    // pull out ACDD metadata and put into the catalog
    MetadataExtractorAcdd acdd = new MetadataExtractorAcdd(Attribute.makeMap(fd.getGlobalAttributes()), this);
    acdd.extract();

    localState.vars = MetadataExtractor.extractVariables(fd);
    localState.dateRange = MetadataExtractor.extractCalendarDateRange(fd);

    // coverage can come in the InvDataset metadata, in which case it overrides whats in the files.
    localState.coverage = getGeospatialCoverage();
    if (localState.coverage != null) {
      // override in fd
      ((PointDatasetImpl) fd).setBoundingBox(localState.coverage.getBoundingBox());

    } else { // look for it in the files
      localState.coverage = MetadataExtractor.extractGeospatial(fd);
    }  */

    // add Variables, GeospatialCoverage, TimeCoverage LOOK doesnt seem to work
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.coverage != null) tmi.setGeospatialCoverage(localState.coverage);
    if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.TwoD)) {

       InvDatasetImpl ds = new InvDatasetImpl(this, "Forecast Model Run Collection (2D time coordinates)");
       String name = getName() + "_" + FMRC;
       name = StringUtil2.replace(name, ' ', "_");
       ds.setUrlPath(this.configPath + "/" + name);
       ds.setID(this.configPath + "/" + name);
       // ds.setID(id + "/" + name);
       ThreddsMetadata tm = ds.getLocalMetadata();
       tm.addDocumentation("summary", "Forecast Model Run Collection (2D time coordinates).");
       //ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
       ds.finish();
       top.addDataset(ds);
     }

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Best)) {

      InvDatasetImpl ds = new InvDatasetImpl(this, "Best Time Series");
      String name = getName() + "_" + BEST;
      name = StringUtil2.replace(name, ' ', "_");
      ds.setUrlPath(this.configPath + "/" + name);
      ds.setID(this.configPath + "/" + name);
      // ds.setID(id + "/" + name);
      ThreddsMetadata tm = ds.getLocalMetadata();
      tm.addDocumentation("summary", "Best time series, taking the data from the most recent run available.");
      //ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
      ds.finish();
      top.addDataset(ds);
    }

    if (config.fmrcConfig.getBestDatasets() != null) {
      for (FeatureCollectionConfig.BestDataset bd : config.fmrcConfig.getBestDatasets()) {
        InvDatasetImpl ds = new InvDatasetImpl(this, bd.name);
        String name = getName() + "_" + bd.name;
        name = StringUtil2.replace(name, ' ', "_");
        ds.setUrlPath(this.configPath + "/" + name);
        ds.setID(this.configPath + "/" + name);
        // ds.setID(id + "/" + name);
        ThreddsMetadata tm = ds.getLocalMetadata();
        tm.addDocumentation("summary", "Best time series, excluding offset hours less than "+bd.greaterThan);
        //ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
        ds.finish();
        top.addDataset(ds);
      }
    }

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs)) {
       InvDatasetImpl ds = new InvCatalogRef(this, RUN_TITLE, getCatalogHref(RUNS));
       ds.finish();
       top.addDataset(ds);
     }

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts)) {
       InvDatasetImpl ds = new InvCatalogRef(this, FORECAST_TITLE, getCatalogHref(FORECAST));
       ds.finish();
       top.addDataset(ds);
     }

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets)) {
       InvDatasetImpl ds = new InvCatalogRef(this, OFFSET_TITLE, getCatalogHref(OFFSET));
       ds.finish();
       top.addDataset(ds);
     }

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Files) && (topDirectory != null)) {
      InvCatalogRef filesCat = new InvCatalogRef(this, FILES, getCatalogHref(FILES));
      filesCat.finish();
      top.addDataset(filesCat);

    }
    /*
       /* LOOK - replace this with InvDatasetScan( collectionManager) or something
       long olderThan = dcm.getOlderThanFilterInMSecs();
       ScanFilter scanFilter = new ScanFilter(null, olderThan);
       InvDatasetScan scanDataset = new InvDatasetScan((InvCatalogImpl) this.getParentCatalog(), this, "File_Access", path + "/" + FILES,
               topDirectory, scanFilter, true, "true", false, null, null, null);

       scanDataset.addService(orgService);

       ThreddsMetadata tmi = scanDataset.getLocalMetadataInheritable();
       tmi.setServiceName(orgService.getName());
       tmi.addDocumentation("summary", "Individual data file, which comprise the Forecast Model Run Collection.");
       tmi.setGeospatialCoverage(null);
       tmi.setTimeCoverage( (DateRange) null);
       scanDataset.setServiceName(orgService.getName());
       scanDataset.finish();
       datasets.add(scanDataset);

       // replace all at once
       localState.scan = scanDataset;
     }  */

     localState.top = top;
     finish();
   }


  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(String matchPath) throws IOException {
    State localState =  checkState();

    int pos = matchPath.indexOf('/');
    String wantType = (pos > -1) ? matchPath.substring(0, pos) : matchPath;
    String wantName = (pos > -1) ? matchPath.substring(pos + 1) : matchPath;
    String hasName = StringUtil2.replace(name, ' ', "_") + "_";

    try {
      if (wantType.equalsIgnoreCase(FILES)) {
        NetcdfDataset ncd = getNetcdfDataset(matchPath);
        return ncd == null ? null : new ucar.nc2.dt.grid.GridDataset(ncd);

      } else if (wantName.equals(hasName + FMRC) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.TwoD)) {
        return fmrc.getDataset2D(null);

      } else if (wantName.equals(hasName + BEST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Best)) {
        return fmrc.getDatasetBest();

      } else if (wantType.equals(OFFSET) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets)) {
        int pos1 = wantName.lastIndexOf(OFFSET_NAME);
        int pos2 = wantName.lastIndexOf("hr");
        if ((pos1<0) || (pos2<0)) return null;
        String id = wantName.substring(pos1+OFFSET_NAME.length(), pos2);
        try {
            double hour = Double.parseDouble(id);
            return fmrc.getConstantOffsetDataset( hour);
        } catch (NumberFormatException e) {
          return null; // user input error
        }

      } else if (wantType.equals(RUNS) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs)) {
        int pos1 = wantName.indexOf(RUN_NAME);
        if (pos1<0) return null;
        String id = wantName.substring(pos1+RUN_NAME.length());

        CalendarDate date = CalendarDate.parseISOformat(null, id);
        if (date == null) return null; // user input error
        return fmrc.getRunTimeDataset(date);

      } else if (wantType.equals(FORECAST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts)) {
        int pos1 = wantName.indexOf(FORECAST_NAME);
        if (pos1<0) return null;
        String id = wantName.substring(pos1+FORECAST_NAME.length());

        CalendarDate date = CalendarDate.parseISOformat(null, id);
        if (date == null) return null; // user input error
        return fmrc.getConstantForecastDataset(date);

      } else if (config.fmrcConfig.getBestDatasets() != null) {
        for (FeatureCollectionConfig.BestDataset bd : config.fmrcConfig.getBestDatasets()) {
          if (wantName.endsWith(bd.name)) {
            return fmrc.getDatasetBest(bd);
          }
        }
      }

    } catch (FileNotFoundException e) {
      return null;
    }

    return null;
  }

  @Override
  public FeatureDataset getFeatureDataset() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

}
