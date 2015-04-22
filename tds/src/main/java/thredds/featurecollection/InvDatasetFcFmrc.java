package thredds.featurecollection;

import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFileCollectionManager;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.catalog.writer.ThreddsMetadataExtractor;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
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

  InvDatasetFcFmrc(FeatureCollectionRef parent, FeatureCollectionConfig config) {
    super(parent, config);
    makeCollection();

    Formatter errlog = new Formatter();
    try {
      fmrc = new Fmrc(datasetCollection, config);
    } catch (Exception e) {
      throw new RuntimeException(errlog.toString());
    }

    this.wantDatasets = config.fmrcConfig.datasets;

    state = new State(null);
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

      default:
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

      boolean checkInv = fmrc.checkInvState(localState.lastInvChange);
      boolean checkProto = fmrc.checkProtoState(localState.lastProtoChange);

      if (checkProto) {
        // add Variables, GeospatialCoverage, TimeCoverage
        GridDataset gds = fmrc.getDataset2D(null);
        if (null != gds) {
          ThreddsMetadataExtractor extractor = new ThreddsMetadataExtractor();
          localState.vars = extractor.extractVariables(null, gds);
          localState.coverage = extractor.extractGeospatial(gds);
          localState.dateRange = new DateRange(extractor.extractCalendarDateRange(gds));
        }
        localState.lastProtoChange = System.currentTimeMillis();
      }

      if (checkInv) {
        // makeDatasetTop(localState);
        localState.lastInvChange = System.currentTimeMillis();
      }

    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when the catref is requested

  @Override
  public Catalog makeCatalog(String match, String orgPath, URI catURI) throws IOException {
    logger.debug("FMRC make catalog for " + match + " " + catURI);
    State localState = checkState();

    try {
      if ((match == null) || (match.length() == 0)) {
        CatalogBuilder main = makeCatalogTop(catURI, localState);
        main.addService(virtualService);
        return main.makeCatalog();
      } else if (match.equals(RUNS) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs))
        return makeCatalogRuns(catURI, localState);

      else if (match.equals(OFFSET) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets))
        return makeCatalogOffsets(catURI, localState);

      else if (match.equals(FORECAST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts))
        return makeCatalogForecasts(catURI, localState);

      else if (match.startsWith(FILES) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Files)) {
        return makeCatalogFiles(catURI, localState, datasetCollection.getFilenames(), true);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + configPath, e);
    }

    return null;
  }

  private CatalogBuilder makeCatalog(URI catURI, State localState, String name) throws IOException {
    Catalog parentCatalog = parent.getParentCatalog();

    CatalogBuilder result = new CatalogBuilder();
    result.setName(makeFullName(parent));
    result.setVersion(parentCatalog.getVersion());
    result.setBaseURI(catURI);                       // LOOK is catURI right ??
    result.addService(virtualService);

    DatasetBuilder top = new DatasetBuilder(null);
    top.transferMetadata(parent, true); // make all inherited metadata local
    top.setName(name);

    ThreddsMetadata tmi = top.getInheritableMetadata();
    tmi.set(Dataset.ServiceName, Virtual_Services_Name);
    if (localState.coverage != null) tmi.set(Dataset.GeospatialCoverage, localState.coverage);
    if (localState.dateRange != null) tmi.set(Dataset.TimeCoverage, localState.dateRange);
    if (localState.vars != null) tmi.set(Dataset.VariableGroups, localState.vars);

    result.addDataset(top);

    for (DatasetBuilder ds : makeRunDatasets(top))
      top.addDataset(ds);

    return result;
  }

  private Catalog makeCatalogRuns(URI catURI, State localState) throws IOException {
    CatalogBuilder runCatalog = makeCatalog(catURI, localState, RUN_TITLE);
    DatasetBuilder top = runCatalog.getTop();

    for (DatasetBuilder ds : makeRunDatasets(top))
      top.addDataset(ds);

    return runCatalog.makeCatalog();
  }

  private Catalog makeCatalogOffsets(URI catURI, State localState) throws IOException {
    CatalogBuilder offCatalog = makeCatalog(catURI, localState, OFFSET_TITLE);
    DatasetBuilder top = offCatalog.getTop();

    for (DatasetBuilder ds : makeOffsetDatasets(top))
      top.addDataset(ds);

    return offCatalog.makeCatalog();
  }

  private Catalog makeCatalogForecasts(URI catURI, State localState) throws IOException {
    CatalogBuilder offCatalog = makeCatalog(catURI, localState, OFFSET_TITLE);
    DatasetBuilder top = offCatalog.getTop();

    for (DatasetBuilder ds : makeOffsetDatasets(top))
      top.addDataset(ds);

    return offCatalog.makeCatalog();
  }

  private List<DatasetBuilder> makeRunDatasets(DatasetBuilder parent) throws IOException {
    List<DatasetBuilder> datasets = new ArrayList<>();

    for (CalendarDate runDate : fmrc.getRunDates()) {
      String myname = name + "_" + RUN_NAME + runDate;
      myname = StringUtil2.replace(myname, ' ', "_");

      DatasetBuilder nested = new DatasetBuilder(parent);
      nested.setName(myname);
      nested.put(Dataset.UrlPath, this.configPath + "/" + RUNS + "/" + myname);
      nested.put(Dataset.Id, this.configPath + "/" + RUNS + "/" + myname);
      nested.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Data from Run " + myname));
      CalendarDateRange cdr = fmrc.getDateRangeForRun(runDate);
      if (cdr != null)
        nested.put(Dataset.TimeCoverage, new DateRange(cdr));
      datasets.add(nested);
    }

    Collections.reverse(datasets);
    return datasets;
  }

  private List<DatasetBuilder> makeOffsetDatasets(DatasetBuilder parent) throws IOException {
    List<DatasetBuilder> datasets = new ArrayList<>();

    for (double offset : fmrc.getForecastOffsets()) {
      String myname = name + "_" + OFFSET_NAME + offset + "hr";
      myname = StringUtil2.replace(myname, ' ', "_");

      DatasetBuilder nested = new DatasetBuilder(parent);
      nested.setName(myname);
      nested.put(Dataset.UrlPath, this.configPath + "/" + OFFSET + "/" + myname);
      nested.put(Dataset.Id, this.configPath + "/" + OFFSET + "/" + myname);
      nested.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Data from the " + offset + " hour forecasts, across different model runs."));
      CalendarDateRange cdr = fmrc.getDateRangeForOffset(offset);
      if (cdr != null)
        nested.put(Dataset.TimeCoverage, new DateRange(cdr));
      datasets.add(nested);
    }

    return datasets;
  }

  private List<DatasetBuilder> makeForecastDatasets(DatasetBuilder parent) throws IOException {

    List<DatasetBuilder> datasets = new ArrayList<>();

    for (CalendarDate forecastDate : fmrc.getForecastDates()) {
      String myname = name + "_" + FORECAST_NAME + forecastDate;
      myname = StringUtil2.replace(myname, ' ', "_");

      DatasetBuilder nested = new DatasetBuilder(parent);
      nested.setName(myname);
      nested.put(Dataset.UrlPath, this.configPath + "/" + FORECAST + "/" + myname);
      nested.put(Dataset.Id, this.configPath + "/" + FORECAST + "/" + myname);
      nested.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Data with the same forecast date, " + name + ", across different model runs."));
      nested.put(Dataset.TimeCoverage, new DateRange(CalendarDateRange.of(forecastDate, forecastDate)));
      datasets.add(nested);
    }

    return datasets;
  }


  /////////////////////////////////////////////////////////////////////////

  @Override
  protected DatasetBuilder makeDatasetTop(URI catURI, State localState) {
    DatasetBuilder top = new DatasetBuilder(null);
    top.transferMetadata(parent, true); // make all inherited metadata local
    top.setName(name);

    ThreddsMetadata tmi = top.getInheritableMetadata();  // LOOK allow to change ??
    tmi.set(Dataset.FeatureType, FeatureType.GRID.toString()); // override GRIB
    tmi.set(Dataset.ServiceName, Virtual_Services_Name);
    if (localState.coverage != null) tmi.set(Dataset.GeospatialCoverage, localState.coverage);
    if (localState.dateRange != null) tmi.set(Dataset.TimeCoverage, localState.dateRange);
    if (localState.vars != null) tmi.set(Dataset.VariableGroups, localState.vars);

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.TwoD)) {
      DatasetBuilder twoD = new DatasetBuilder(top);
      twoD.setName("Forecast Model Run Collection (2D time coordinates)");
      String myname = name + "_" + FMRC;
      myname = StringUtil2.replace(myname, ' ', "_");
      twoD.put(Dataset.UrlPath, this.configPath + "/" + myname);
      twoD.put(Dataset.Id, this.configPath + "/" + myname);
      twoD.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Forecast Model Run Collection (2D time coordinates)."));
      top.addDataset(twoD);
    }

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Best)) {
      DatasetBuilder best = new DatasetBuilder(top);
      best.setName("Best Time Series");
      String myname = name + "_" + BEST;
      myname = StringUtil2.replace(myname, ' ', "_");
      best.put(Dataset.UrlPath, this.configPath + "/" + myname);
      best.put(Dataset.Id, this.configPath + "/" + myname);
      best.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Best time series, taking the data from the most recent run available."));
      top.addDataset(best);
    }

    if (config.fmrcConfig.getBestDatasets() != null) {
      for (FeatureCollectionConfig.BestDataset bd : config.fmrcConfig.getBestDatasets()) {
        DatasetBuilder ds = new DatasetBuilder(top);
        ds.setName(bd.name);
        String myname = name + "_" + bd.name;
        myname = StringUtil2.replace(myname, ' ', "_");
        ds.put(Dataset.UrlPath, this.configPath + "/" + myname);
        ds.put(Dataset.Id, this.configPath + "/" + myname);
        ds.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Best time series, excluding offset hours less than " + bd.greaterThan));
        top.addDataset(ds);
      }
    }

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs)) {
      CatalogRefBuilder ds = new CatalogRefBuilder(top);
      ds.setTitle(RUN_TITLE);
      ds.setHref(getCatalogHref(RUNS));
      top.addDataset(ds);
    }

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts)) {
      CatalogRefBuilder ds = new CatalogRefBuilder(top);
      ds.setTitle(FORECAST_TITLE);
      ds.setHref(getCatalogHref(FORECAST));
      top.addDataset(ds);
    }

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets)) {
      CatalogRefBuilder ds = new CatalogRefBuilder(top);
      ds.setTitle(OFFSET_TITLE);
      ds.setHref(getCatalogHref(OFFSET));
      top.addDataset(ds);
    }

    if (wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Files) && (topDirectory != null)) {
      CatalogRefBuilder ds = new CatalogRefBuilder(top);
      ds.setTitle(FILES);
      ds.setHref(getCatalogHref(FILES));
      top.addDataset(ds);
    }

    return top;
  }


  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(String matchPath) throws IOException {
    State localState = checkState();

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
        if ((pos1 < 0) || (pos2 < 0)) return null;
        String id = wantName.substring(pos1 + OFFSET_NAME.length(), pos2);
        try {
          double hour = Double.parseDouble(id);
          return fmrc.getConstantOffsetDataset(hour);
        } catch (NumberFormatException e) {
          return null; // user input error
        }

      } else if (wantType.equals(RUNS) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs)) {
        int pos1 = wantName.indexOf(RUN_NAME);
        if (pos1 < 0) return null;
        String id = wantName.substring(pos1 + RUN_NAME.length());

        CalendarDate date = CalendarDate.parseISOformat(null, id);
        if (date == null) return null; // user input error
        return fmrc.getRunTimeDataset(date);

      } else if (wantType.equals(FORECAST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts)) {
        int pos1 = wantName.indexOf(FORECAST_NAME);
        if (pos1 < 0) return null;
        String id = wantName.substring(pos1 + FORECAST_NAME.length());

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
