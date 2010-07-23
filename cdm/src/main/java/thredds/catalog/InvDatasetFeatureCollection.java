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

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.inventory.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Feature Collection (experimental).
 * Like InvDatasetFmrc, this is a InvCatalogRef subclass. So the reference is placed in the parent, but
 * the catalog itself isnt constructed until it is the following is called from DataRootHandler.makeDynamicCatalog():
 *       match.dataRoot.featCollection.makeCatalog(match.remaining, path, baseURI);
 *
 * Generate anew each call; use object caching if needed to improve efficiency
 *
 * @author caron
 * @since Mar 3, 2010
 */
@ThreadSafe
public class InvDatasetFeatureCollection extends InvCatalogRef {
  static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFeatureCollection.class);

  static private final String FMRC = "fmrc.ncd";
  static private final String BEST = "best.ncd";
  static private final String SCAN = "files";

  static private final String RUNS = "runs";
  static private final String RUN_NAME = "RUN_";
  static private final String RUN_TITLE = "Forecast Model Run";

  static private final String FORECAST = "forecast";
  static private final String FORECAST_NAME = "ConstantForecast_";
  static private final String FORECAST_TITLE = "Constant Forecast Date";

  static private final String OFFSET = "offset";
  static private final String OFFSET_NAME = "Offset_";
  static private final String OFFSET_TITLE = "Constant Forecast Offset";

  static private final String Virtual_Services = "VirtualServices";

  /////////////////////////////////////////////////////////////////////////////

  private final String path;
  private final FeatureType featureType;
  private final FeatureCollectionConfig.Config config;

  private final Fmrc fmrc;
  private final Set<FeatureCollectionConfig.FmrcDatasetType> wantDatasets;
  private final String topDirectory;
  private final Pattern filter;
  private InvService orgService, virtualService;

  @GuardedBy("lock")
  private State state;
  private Object lock = new Object();

  private class State {
    ThreddsMetadata.Variables vars;
    ThreddsMetadata.GeospatialCoverage gc;
    DateRange dateRange;
    Date lastProtoChange;

    InvDatasetScan scan;
    List<InvDataset> datasets;
    Date lastInvChange;

    State(State from) {
      if (from != null) {
        this.vars = from.vars;
        this.gc = from.gc;
        this.dateRange = from.dateRange;
        this.lastProtoChange = from.lastProtoChange;

        this.scan = from.scan;
        this.datasets = from.datasets;
        this.lastInvChange = from.lastInvChange;
      }
    }
  }

  /* @GuardedBy("lock")
  private volatile InvCatalogImpl topCatalog; // needs to be changed when proto changes, in case metadata has changed

  @GuardedBy("lock")
  private volatile InvCatalogImpl catalogRuns, catalogOffsets, catalogForecasts; // these change when FmrcInv changes

  @GuardedBy("lock")
  private volatile boolean madeDatasets = false; */

  public InvDatasetFeatureCollection(InvDatasetImpl parent, String name, String path, String featureType, FeatureCollectionConfig.Config config) {
    super(parent, name, "/thredds/catalog/" + path + "/catalog.xml");
    this.path = path;
    this.featureType = FeatureType.getType(featureType);
    if ( featureType.equalsIgnoreCase( "FMRC" ))
      this.getLocalMetadataInheritable().setDataType( FeatureType.GRID );
    
    this.config = config;
    this.wantDatasets = config.fmrcConfig.datasets;

    Formatter errlog = new Formatter();
    try {
      fmrc = Fmrc.open(config, errlog);
    } catch (Exception e) {
      throw new RuntimeException(errlog.toString());
    }

    /// hmmmm not so good
    CollectionManager cm = fmrc.getManager();
    if (cm instanceof DatasetCollectionManager) {
      CollectionSpecParser sp = ((DatasetCollectionManager) cm).getCollectionSpecParser();
      topDirectory = sp.getTopDir();
      filter = sp.getFilter();
    } else {
      topDirectory = null;
      filter = null;
    }
  }

  private InvService makeVirtualService(InvService org) {
    if (org.getServiceType() != ServiceType.COMPOUND) return org;

    InvService result = new InvService(Virtual_Services, ServiceType.COMPOUND.toString(), null, null, null);
    for (InvService service : org.getServices()) {
       if (service.getServiceType() != ServiceType.HTTPServer) {
         result.addService(service);
       }
     }
    return result;
   }

  public String getPath() {
    return path;
  }

  public String getTopDirectoryLocation() {
    return topDirectory;
  }

  public FeatureCollectionConfig.Config getConfig() {
    return config;
  }

  public InvDatasetScan getRawFileScan()  {
     try {
      checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
    }
    return state.scan;
  }

  @Override
  public java.util.List<InvDataset> getDatasets() {
    try {
      checkState();
    } catch (Exception e) {
      logger.error("Error in checkState", e);
    }
    return state.datasets;
  }

  // called by scheduler
  public void triggerRescan() throws IOException {
    fmrc.triggerRescan();
  }

  public void triggerProto() throws IOException {
    fmrc.triggerProto();
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when the catref is requested

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
      if ((match == null) || (match.length() == 0))
        return makeCatalogTop(baseURI, localState);

      else if (match.equals(RUNS) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs))
        return makeCatalogRuns(baseURI, localState);

      else if (match.equals(OFFSET) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets))
        return makeCatalogOffsets(baseURI, localState);

      else if (match.equals(FORECAST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts))
        return makeCatalogForecasts(baseURI, localState);

      else if (match.startsWith(SCAN) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Files)) {
        return localState.scan.makeCatalogForDirectory(orgPath, baseURI);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }

  /**
   * Make the top catalog of this catref.
   *
   * @param baseURI base URI of the request
   * @return the top FMRC catalog
   * @throws java.io.IOException         on I/O error
   * @throws java.net.URISyntaxException if path is misformed
   */
  private InvCatalogImpl makeCatalogTop(URI baseURI, State localState) throws IOException, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getXlinkHref());
    InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), myURI);

    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      top.transferMetadata(parent); // make all inherited metadata local

    String id = getID();
    if (id == null)
      id = getPath();
    top.setID(id);

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    mainCatalog.addDataset(top);

    // any referenced services need to be local
    // remove http service for virtual datasets
    mainCatalog.addService(virtualService);
    top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    for (InvDataset ds : getDatasets())
      top.addDataset((InvDatasetImpl) ds);

    mainCatalog.finish();

    return mainCatalog;
  }

   private InvCatalogImpl makeCatalogRuns(URI baseURI, State localState) throws IOException {

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getCatalogHref(RUNS));
    InvCatalogImpl runCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), myURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent()); // make all inherited metadata local
    top.setName(RUN_TITLE);
    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

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
    top.transferMetadata((InvDatasetImpl) this.getParent()); // make all inherited metadata local
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
    top.transferMetadata((InvDatasetImpl) this.getParent()); // make all inherited metadata local
    top.setName(FORECAST_TITLE);
    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    foreCatalog.addDataset(top);

    // services need to be local
    foreCatalog.addService(virtualService);
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
      datasets.add(nested);
    }

    return datasets;
  }


  /////////////////////////////////////////////////////////////////////////

  private State checkState() throws IOException {

    synchronized (lock) {
      boolean checkInv = true;
      boolean checkProto = true;

      if (state == null) {
        orgService = getServiceDefault();
        virtualService = makeVirtualService(orgService);
      } else {
        checkInv = fmrc.checkInvState(state.lastInvChange);
        checkProto = fmrc.checkProtoState(state.lastProtoChange);
        if (!checkInv && !checkProto) return state;
      }

      // copy on write
      State localState = new State(state);

      if (checkProto) {
         // add Variables, GeospatialCoverage, TimeCoverage
        GridDataset gds = getGridDataset(FMRC);
        if (null != gds) {
          localState.vars = MetadataExtractor.extractVariables(this, gds);
          localState.gc = MetadataExtractor.extractGeospatial(gds);
          localState.dateRange = MetadataExtractor.extractDateRange(gds);
        }
        localState.lastProtoChange = new Date();
      }

      if (checkInv) {
        makeDatasets(localState);
        localState.lastInvChange = new Date();
      }

      state = localState;
      return state;
    }
  }

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
       ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
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
      ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
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
        ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());
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
       long olderThan = (long) (1000 * fmrc.getOlderThanFilterInSecs());
       ScanFilter scanFilter = new ScanFilter(filter, olderThan);
       InvDatasetScan scanDataset = new InvDatasetScan((InvCatalogImpl) this.getParentCatalog(), this, "File_Access", path + "/" + SCAN,
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


  private String getCatalogHref(String what) {
    return "/thredds/catalog/" + path + "/" + what + "/catalog.xml";
  }

  // specialized filter handles olderThan and/or filename pattern matching
  public static class ScanFilter implements CrawlableDatasetFilter {
    private Pattern p;
    private long olderThan;

    public ScanFilter(Pattern p, long olderThan) {
      this.p = p;
      this.olderThan = olderThan;
    }

    @Override
    public boolean accept(CrawlableDataset dataset) {
      if (dataset.isCollection()) return true;

      if (p != null) {
        java.util.regex.Matcher matcher = p.matcher(dataset.getName());
        if (!matcher.matches()) return false;
      }

      if (olderThan > 0) {
        Date lastModDate = dataset.lastModified();
        if (lastModDate != null) {
          long now = System.currentTimeMillis();
          if (now - lastModDate.getTime() <= olderThan)
            return false;
        }
      }

      return true;
    }

    @Override
    public Object getConfigObject() {
      return null;
    }
  }

  // called by DatasetHandler.getNetcdfFile()

  /**
   * Get the dataset named by the path
   *
   * @param matchPath remaining path from match
   * @return requested dataset
   * @throws IOException if read error
   */
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    int pos = matchPath.indexOf("/");
    String type = (pos > -1) ? matchPath.substring(0, pos) : matchPath;
    String name = (pos > -1) ? matchPath.substring(pos + 1) : "";

    // this assumes that these are files. also might be remote datasets from a catalog
    if (type.equals(SCAN)) {
      if (topDirectory == null) return null;

      String filename = new StringBuilder(topDirectory)
              .append(topDirectory.endsWith("/") ? "" : "/")
              .append(name).toString();
      return NetcdfDataset.acquireDataset(null, filename, null, -1, null, null); // no enhancement
    }

    GridDataset gds = getGridDataset(matchPath);
    return (gds == null) ? null : (NetcdfDataset) gds.getNetcdfFile();
  }

  // called by DatasetHandler.openGridDataset()
  public GridDataset getGridDataset(String matchPath) throws IOException {
    int pos = matchPath.indexOf("/");
    String type = (pos > -1) ? matchPath.substring(0, pos) : matchPath;
    String name = (pos > -1) ? matchPath.substring(pos + 1) : matchPath;

    try {
      if (type.equals(SCAN)) {
        NetcdfDataset ncd = getNetcdfDataset(matchPath);
        return ncd == null ? null : new ucar.nc2.dt.grid.GridDataset(ncd);

      } else if (name.endsWith(FMRC) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.TwoD)) {
        return fmrc.getDataset2D(null);

      } else if (name.endsWith(BEST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Best)) {
        return fmrc.getDatasetBest();

      } else if (type.equals(OFFSET) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantOffsets)) {
        int pos1 = name.indexOf(OFFSET_NAME);
        int pos2 = name.indexOf("hr");
        if ((pos1<0) || (pos2<0)) return null;
        String id = name.substring(pos1+OFFSET_NAME.length(), pos2);
        double hour = Double.parseDouble(id);
        return fmrc.getConstantOffsetDataset( hour);

      } else if (type.equals(RUNS) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.Runs)) {
        int pos1 = name.indexOf(RUN_NAME);
        if (pos1<0) return null;
        String id = name.substring(pos1+RUN_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        return fmrc.getRunTimeDataset(date);

      } else if (type.equals(FORECAST) && wantDatasets.contains(FeatureCollectionConfig.FmrcDatasetType.ConstantForecasts)) {
        int pos1 = name.indexOf(FORECAST_NAME);
        if (pos1<0) return null;
        String id = name.substring(pos1+FORECAST_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        return fmrc.getConstantForecastDataset(date);

      } else if (config.fmrcConfig.getBestDatasets() != null) {
        for (FeatureCollectionConfig.BestDataset bd : config.fmrcConfig.getBestDatasets()) {
          if (name.endsWith(bd.name)) {
            return fmrc.getDatasetBest(bd);
          }
        }
      }

    } catch (FileNotFoundException e) {
      return null;
    }

    return null;
  }

  // called by DataRootHandler.getCrawlableDatasetAsFile()
  // have to remove the extra "files" from the path
  public File getFile(String remaining) {
    if (null == topDirectory) return null;
    int pos = remaining.indexOf(SCAN);
    StringBuilder fname = new StringBuilder(topDirectory);
    if (!topDirectory.endsWith("/"))
      fname.append("/");
    fname.append((pos > -1) ? remaining.substring(pos + SCAN.length() + 1) : remaining);
    return new File(fname.toString());
  }

}
