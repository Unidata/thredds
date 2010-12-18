package thredds.catalog;

import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import thredds.inventory.FeatureCollectionConfig;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.unidata.util.StringUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Feature Collection for Fmrc
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

  /////////////////////////////////////////////////////////////////////////////

  private final Fmrc fmrc;
  private final Set<FeatureCollectionConfig.FmrcDatasetType> wantDatasets;
  private InvService orgService, virtualService;

  public InvDatasetFcFmrc(InvDatasetImpl parent, String name, String path, FeatureType featureType, FeatureCollectionConfig config) {
    super(parent, name, path, featureType, config);
    tmi.setDataType( FeatureType.GRID); // override FMRC
    finish(); // ??

    Formatter errlog = new Formatter();
    try {
      fmrc = new Fmrc(dcm, config);
    } catch (Exception e) {
      throw new RuntimeException(errlog.toString());
    }

    this.wantDatasets = config.fmrcConfig.datasets;
  }

  @Override
  public void update() {
    fmrc.update();
  }

  @Override
  public void updateProto() {
    fmrc.updateProto();
  }

  @Override
  protected State checkState() throws IOException {

    synchronized (lock) {
      boolean checkInv = true;
      boolean checkProto = true;

      if (state == null) {
        orgService = getServiceDefault();
        virtualService = makeVirtualService(orgService);
      } else {
        fmrc.checkNeeded(false);
        checkInv = fmrc.checkInvState(state.lastInvChange);
        checkProto = fmrc.checkProtoState(state.lastProtoChange);
        if (!checkInv && !checkProto) return state;
      }

      // copy on write
      State localState = new State(state);

      if (checkProto) {
         // add Variables, GeospatialCoverage, TimeCoverage
        GridDataset gds = fmrc.getDataset2D(null);
        if (null != gds) {
          localState.vars = MetadataExtractor.extractVariables(this, gds);
          localState.gc = MetadataExtractor.extractGeospatial(gds);
          localState.dateRange = MetadataExtractor.extractDateRange(gds);
        }
        localState.lastProtoChange = System.currentTimeMillis();
      }

      if (checkInv) {
        makeDatasets(localState);
        localState.lastInvChange = System.currentTimeMillis();
      }

      state = localState;
      return state;
    }
  }

    ///////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when the catref is requested

  @Override
  public InvCatalogImpl makeCatalog(String match, String orgPath, URI baseURI)  {
    logger.debug("FMRC make catalog for " + match + " " + baseURI);
    State localState = null;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    try {
      if ((match == null) || (match.length() == 0)) {
        InvCatalogImpl main  = makeCatalogTop(baseURI, localState);
        main.addService(virtualService);
        main.getDataset().getLocalMetadataInheritable().setServiceName(virtualService.getName());
        main.finish();
        return main;
      }

      else if (match.equals(RUNS) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs))
        return makeCatalogRuns(baseURI, localState);

      else if (match.equals(OFFSET) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets))
        return makeCatalogOffsets(baseURI, localState);

      else if (match.equals(FORECAST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts))
        return makeCatalogForecasts(baseURI, localState);

      else if (match.startsWith(FILES) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Files)) {
        InvCatalogImpl files   = localState.scan.makeCatalogForDirectory(orgPath, baseURI);
        files.addService(InvService.latest);
        files.addService(orgService);
        files.getDataset().getLocalMetadataInheritable().setServiceName(orgService.getName());
        files.finish();
        return files;
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }

   private InvCatalogImpl makeCatalogRuns(URI baseURI, State localState) throws IOException {

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getCatalogHref(RUNS));
    InvCatalogImpl runCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), myURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(RUN_TITLE);
    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
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

  private InvCatalogImpl makeCatalogOffsets(URI baseURI, State localState) throws IOException {

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getCatalogHref(OFFSET));
    InvCatalogImpl offCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), myURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
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

  private InvCatalogImpl makeCatalogForecasts(URI baseURI, State localState) throws IOException {

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getCatalogHref(FORECAST));
    InvCatalogImpl foreCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), myURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(FORECAST_TITLE);
    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
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

    List<InvDatasetImpl> datasets = new ArrayList<InvDatasetImpl>();
    DateFormatter formatter = new DateFormatter();

    String id = getID();
    if (id == null)
      id = getPath();

    for (Date runDate : fmrc.getRunDates()) {
      String name = getName() + "_" + RUN_NAME + formatter.toDateTimeStringISO(runDate);
      name = StringUtil.replace(name, ' ', "_");
      InvDatasetImpl nested = new InvDatasetImpl(this, name);
      nested.setUrlPath(path + "/" + RUNS + "/" + name);
      nested.setID(id + "/" + RUNS + "/" + name);
      ThreddsMetadata tm = nested.getLocalMetadata();
      tm.addDocumentation("summary", "Data from Run " + name);
      DateRange dr = fmrc.getDateRangeForRun(runDate);
      if (dr != null)
        tm.setTimeCoverage(dr);
      datasets.add(nested);
    }

    Collections.reverse(datasets);
    return datasets;
  }

  private List<InvDatasetImpl> makeOffsetDatasets() throws IOException {

    List<InvDatasetImpl> datasets = new ArrayList<InvDatasetImpl>();

    String id = getID();
    if (id == null)
      id = getPath();

    for (double offset : fmrc.getForecastOffsets()) {
      String name = getName() + "_" + OFFSET_NAME + offset + "hr";
      name = StringUtil.replace(name, ' ', "_");
      InvDatasetImpl nested = new InvDatasetImpl(this, name);
      nested.setUrlPath(path + "/" + OFFSET + "/" + name);
      nested.setID(id + "/" + OFFSET + "/" + name);
      ThreddsMetadata tm = nested.getLocalMetadata();
      tm.addDocumentation("summary", "Data from the " + offset + " hour forecasts, across different model runs.");
      DateRange dr = fmrc.getDateRangeForOffset(offset);
      if (dr != null)
        tm.setTimeCoverage(dr);
      datasets.add(nested);
    }

    return datasets;
  }

  private List<InvDatasetImpl> makeForecastDatasets() throws IOException {

    List<InvDatasetImpl> datasets = new ArrayList<InvDatasetImpl>();
    DateFormatter formatter = new DateFormatter();

    String id = getID();
    if (id == null)
      id = getPath();

    for (Date forecastDate : fmrc.getForecastDates()) {
      String name = getName() + "_" + FORECAST_NAME + formatter.toDateTimeStringISO(forecastDate);
      name = StringUtil.replace(name, ' ', "_");
      InvDatasetImpl nested = new InvDatasetImpl(this, name);
      nested.setUrlPath(path + "/" + FORECAST + "/" + name);
      nested.setID(id + "/" + FORECAST + "/" + name);
      ThreddsMetadata tm = nested.getLocalMetadata();
      tm.addDocumentation("summary", "Data with the same forecast date, " + name + ", across different model runs.");
      tm.setTimeCoverage( new DateRange(forecastDate, forecastDate));
      datasets.add(nested);
    }

    return datasets;
  }


  /////////////////////////////////////////////////////////////////////////

  private void makeDatasets(State localState) {
     List<InvDataset> datasets = new ArrayList<InvDataset>();

     String id = getID();
     if (id == null) id = getPath();

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.TwoD)) {

       InvDatasetImpl ds = new InvDatasetImpl(this, "Forecast Model Run Collection (2D time coordinates)");
       String name = getName() + "_" + FMRC;
       name = StringUtil.replace(name, ' ', "_");
       ds.setUrlPath(this.path + "/" + name);
       ds.setID(id + "/" + name);
       ThreddsMetadata tm = ds.getLocalMetadata();
       tm.addDocumentation("summary", "Forecast Model Run Collection (2D time coordinates).");
       //ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
       ds.finish();
       datasets.add(ds);
     }

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Best)) {

      InvDatasetImpl ds = new InvDatasetImpl(this, "Best Time Series");
      String name = getName() + "_" + BEST;
      name = StringUtil.replace(name, ' ', "_");
      ds.setUrlPath(this.path + "/" + name);
      ds.setID(id + "/" + name);
      ThreddsMetadata tm = ds.getLocalMetadata();
      tm.addDocumentation("summary", "Best time series, taking the data from the most recent run available.");
      //ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
      ds.finish();
      datasets.add(ds);
    }

    if (config.fmrcConfig.getBestDatasets() != null) {
      for (FeatureCollectionConfig.BestDataset bd : config.fmrcConfig.getBestDatasets()) {
        InvDatasetImpl ds = new InvDatasetImpl(this, bd.name);
        String name = getName() + "_" + bd.name;
        name = StringUtil.replace(name, ' ', "_");
        ds.setUrlPath(this.path + "/" + name);
        ds.setID(id + "/" + name);
        ThreddsMetadata tm = ds.getLocalMetadata();
        tm.addDocumentation("summary", "Best time series, excluding offset hours less than "+bd.greaterThan);
        //ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
        ds.finish();
        datasets.add(ds);
      }
    }

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs)) {
       InvDatasetImpl ds = new InvCatalogRef(this, RUN_TITLE, getCatalogHref(RUNS));
       ds.finish();
       datasets.add(ds);
     }

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts)) {
       InvDatasetImpl ds = new InvCatalogRef(this, FORECAST_TITLE, getCatalogHref(FORECAST));
       ds.finish();
       datasets.add(ds);
     }

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets)) {
       InvDatasetImpl ds = new InvCatalogRef(this, OFFSET_TITLE, getCatalogHref(OFFSET));
       ds.finish();
       datasets.add(ds);
     }

     if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Files) && (topDirectory != null)) {

       // LOOK - replace this with InvDatasetScan( collectionManager) or something
       long olderThan = (long) (1000 * dcm.getOlderThanFilterInSecs());
       ScanFilter scanFilter = new ScanFilter(filter, olderThan);
       InvDatasetScan scanDataset = new InvDatasetScan((InvCatalogImpl) this.getParentCatalog(), this, "File_Access", path + "/" + FILES,
               topDirectory, scanFilter, true, "true", false, null, null, null);

       scanDataset.addService(orgService);

       ThreddsMetadata tmi = scanDataset.getLocalMetadataInheritable();
       tmi.setServiceName(orgService.getName());
       tmi.addDocumentation("summary", "Individual data file, which comprise the Forecast Model Run Collection.");
       tmi.setGeospatialCoverage(null);
       tmi.setTimeCoverage(null);
       scanDataset.setServiceName(orgService.getName());
       scanDataset.finish();
       datasets.add(scanDataset);

       // replace all at once
       localState.scan = scanDataset;
     }

     localState.datasets = datasets;
     this.datasets = datasets;
     finish();
   }


  @Override
  public GridDataset getGridDataset(String matchPath) throws IOException {
    int pos = matchPath.indexOf("/");
    String wantType = (pos > -1) ? matchPath.substring(0, pos) : matchPath;
    String wantName = (pos > -1) ? matchPath.substring(pos + 1) : matchPath;
    String hasName = StringUtil.replace(name, ' ', "_") + "_";

    try {
      if (wantType.equals(FILES)) {
        NetcdfDataset ncd = getNetcdfDataset(matchPath);
        return ncd == null ? null : new ucar.nc2.dt.grid.GridDataset(ncd);

      } else if (wantName.equals(hasName + FMRC) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.TwoD)) {
        return fmrc.getDataset2D(null);

      } else if (wantName.equals(hasName + BEST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Best)) {
        return fmrc.getDatasetBest();

      } else if (wantType.equals(OFFSET) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets)) {
        int pos1 = wantName.indexOf(OFFSET_NAME);
        int pos2 = wantName.indexOf("hr");
        if ((pos1<0) || (pos2<0)) return null;
        String id = wantName.substring(pos1+OFFSET_NAME.length(), pos2);
        double hour = Double.parseDouble(id);
        return fmrc.getConstantOffsetDataset( hour);

      } else if (wantType.equals(RUNS) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs)) {
        int pos1 = wantName.indexOf(RUN_NAME);
        if (pos1<0) return null;
        String id = wantName.substring(pos1+RUN_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        return fmrc.getRunTimeDataset(date);

      } else if (wantType.equals(FORECAST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts)) {
        int pos1 = wantName.indexOf(FORECAST_NAME);
        if (pos1<0) return null;
        String id = wantName.substring(pos1+FORECAST_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
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

}
