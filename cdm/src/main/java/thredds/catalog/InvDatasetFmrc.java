/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

import org.jdom.Element;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URISyntaxException;
import java.net.URI;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dt.fmrc.ForecastModelRunCollection;
import ucar.nc2.dt.fmrc.FmrcImpl;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.units.DateRange;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheNOP;
import ucar.unidata.util.StringUtil;

/**
 * InvDatasetFmrc represents an <datasetFmrc> element in a TDS catalog.
 *
 * @deprecated use InvDatasetFeatureCollection (must replace datasetFmrc with featureCollection in the TDS config catalog)
 * @author caron
 */
public class InvDatasetFmrc extends InvCatalogRef {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFmrc.class);

  static private final String FMRC = "fmrc.ncd";
  static private final String BEST = "best.ncd";
  static private final String RUNS = "runs";
  static private final String RUN_NAME = "RUN_";
  static private final String FORECAST = "forecast";
  static private final String FORECAST_NAME = "ConstantForecast_";
  static private final String OFFSET = "offset";
  static private final String OFFSET_NAME = "Offset_";

  static private final String TITLE_RUNS = "Forecast Model Run";
  static private final String TITLE_OFFSET = "Constant Forecast Offset";
  static private final String TITLE_FORECAST = "Constant Forecast Date";

  static private final String SCAN = "files";

  // this prevents 2d dataset from being closed. see makeFmrc
  static private FileCache fileCache = new FileCacheNOP();

  //////////////////////////////////////////////

  private volatile boolean madeDatasets = false, madeFmrc = false;

  private final String path;
  private final boolean runsOnly;

  // effectively final
  private InventoryParams params;
  private String dodsService;

  private ForecastModelRunCollection fmrc;
  private InvCatalogImpl catalog, catalogRuns, catalogOffsets, catalogForecasts;
  private InvDatasetScan scan;

  public InvDatasetFmrc(InvDatasetImpl parent, String name, String path, boolean runsOnly) {
    super(parent, name, "/thredds/catalog/"+path+"/catalog.xml");
    this.path = path;
    this.runsOnly = runsOnly;
  }

  public String getPath() { return path; }
  public boolean isRunsOnly() { return runsOnly; }
  public InvDatasetScan getRawFileScan() {
    if ( ! madeDatasets )
      getDatasets();
    return scan;
  }

  public InventoryParams getFmrcInventoryParams() {
    return params;
  }

  public File getFile(String remaining) {
    if( null == params) return null;
    int pos = remaining.indexOf(SCAN);
    StringBuilder fname = new StringBuilder( params.location);
    if ( ! params.location.endsWith( "/"))
      fname.append( "/");
    fname.append( ( pos > -1 ) ? remaining.substring( pos + SCAN.length() + 1 ) : remaining);
    return new File( fname.toString() );
  }

  // bit of a kludge to get info into the InvDatasetFmrc
  public void setFmrcInventoryParams(String location, String def, String suffix, String olderThanS, String subdirs) {
    this.params = new InventoryParams();
    this.params.location = location;
    this.params.def = def;
    this.params.suffix = suffix;
    this.params.subdirs = subdirs != null && subdirs.equalsIgnoreCase("true");

    if (olderThanS != null) {
      try {
        TimeUnit tu = new TimeUnit(olderThanS);
        this.params.lastModifiedLimit  = (long) (1000 * tu.getValueInSeconds());
      } catch (Exception e) {
        logger.error("Invalid TimeUnit = "+olderThanS);
        throw new IllegalArgumentException("Invalid TimeUnit = "+olderThanS);
      }
    }
  }

  public class InventoryParams {
    public String location, def, suffix;
    public boolean subdirs;
    public long lastModifiedLimit = 0;
    public String toString() { return "def="+def +" location="+ location+" suffix="+ suffix+" lastModifiedLimit="+lastModifiedLimit+
            " subdirs="+subdirs; }
  }

  public boolean hasAccess() {
    return false;
  }

  public boolean hasNestedDatasets() {
    return true;
  }

  /**
   * Create the FMRC catalog, or one of its nested catalogs.
   * @param match which catalog: one of null, RUNS, OFFSET, FORECAST, or SCAN
   * @param orgPath the path for the requested catalog.
   * @param baseURI the base URI for the catalog, used to resolve relative URLs.
   * @return the requested catalog
   */
  public InvCatalogImpl makeCatalog(String match, String orgPath, URI baseURI ) {
    logger.debug("FMRC make catalog for "+match+" "+baseURI);
    try {
      if ((match == null) || (match.length() == 0))
        return makeCatalog(baseURI);
      else if (match.equals(RUNS))
        return makeCatalogRuns(baseURI);
      else if (match.equals(OFFSET))
        return makeCatalogOffsets(baseURI);
      else if (match.equals(FORECAST))
        return makeCatalogForecasts(baseURI);
      else if (match.equals(SCAN))
        return makeCatalogScan(orgPath, baseURI);
      else
        return null;
    } catch (Exception e) {
      logger.error("Error making catalog for "+path, e);
      return null;
    }
  }

  @Override
  public java.util.List<InvDataset> getDatasets() {
    if (!madeDatasets) {
      List<InvDataset> datasets = new ArrayList<InvDataset>();

      if (runsOnly) {
        InvDatasetImpl ds = new InvCatalogRef(this, TITLE_RUNS, getCatalogHref(RUNS));
        ds.finish();
        datasets.add( ds);

      } else {
        String id = getID();
        if (id == null)
          id = getPath();

        InvDatasetImpl ds = new InvDatasetImpl(this, "Forecast Model Run Collection (2D time coordinates)");
        String name = getName()+"_"+FMRC;
        name = StringUtil.replace(name, ' ', "_");
        ds.setUrlPath(path+"/"+name);
        ds.setID(id+"/"+name);
        ThreddsMetadata tm = ds.getLocalMetadata();
        tm.addDocumentation("summary", "Forecast Model Run Collection (2D time coordinates).");
        ds.getLocalMetadataInheritable().setServiceName(dodsService); // LOOK why ??
        ds.finish();
        datasets.add( ds);

        ds = new InvDatasetImpl(this, "Best Time Series");
        name = getName()+"_"+BEST;
        name = StringUtil.replace(name, ' ', "_");
        ds.setUrlPath(path+"/"+name);
        ds.setID(id+"/"+name);
        tm = ds.getLocalMetadata();
        tm.addDocumentation("summary", "Best time series, taking the data from the most recent run available.");
        ds.finish();
        datasets.add( ds);

        // run datasets as catref
        ds = new InvCatalogRef(this, TITLE_RUNS, getCatalogHref(RUNS));
        ds.finish();
        datasets.add( ds);

        // run datasets as catref
        ds = new InvCatalogRef(this, TITLE_OFFSET, getCatalogHref(OFFSET));
        ds.finish();
        datasets.add( ds);

        // run datasets as catref
        ds = new InvCatalogRef(this, TITLE_FORECAST, getCatalogHref(FORECAST));
        ds.finish();
        datasets.add( ds);

        if (params != null) {
          /* public InvDatasetScan( InvDatasetImpl parent, String name, String path, String scanDir,
                           String filter,
                           boolean addDatasetSize, String addLatest, boolean sortOrderIncreasing,
                           String datasetNameMatchPattern, String startTimeSubstitutionPattern, String duration,
                           long lastModifiedMsecs ) */

          InvDatasetFeatureCollection.ScanFilter filter = new InvDatasetFeatureCollection.ScanFilter(java.util.regex.Pattern.compile(".*"+params.suffix), params.lastModifiedLimit);
          scan = new InvDatasetScan( (InvCatalogImpl) this.getParentCatalog(), this, "File_Access", path+"/"+SCAN,
                  params.location, filter, true, "true", false, null, null, null );

          ThreddsMetadata tmi = scan.getLocalMetadataInheritable();
          tmi.setServiceName("fileServices");
          tmi.addDocumentation("summary", "Individual data file, which comprise the Forecast Model Run Collection.");
          // LOOK, we'd like to screen files by lastModified date.

          scan.finish();
          datasets.add( scan);
        }
      }

      this.datasets = datasets;
      madeDatasets = true;
    }

    finish();
    return datasets;
  }

  private String getCatalogHref( String what) {
    return "/thredds/catalog/"+path+"/"+what+"/catalog.xml";
  }

  ////////////////////////////////////////////////////////////////////////

  private synchronized boolean checkIfChanged() throws IOException {
    boolean changed =  madeFmrc && fmrc.sync();
    if (changed) {
      catalog = null;
      catalogRuns = null;
      catalogOffsets = null;
      catalogForecasts = null;
    }
    return changed;
  }

  /**
   * Make the top FMRC catalog.
   *
   * @param baseURI base URI of the request
   * @return the top FMRC catalog
   * @throws IOException on I/O error
   * @throws URISyntaxException if path is misformed
   */
  private InvCatalogImpl makeCatalog(URI baseURI) throws IOException, URISyntaxException {

    if ((catalog == null) || checkIfChanged()) {
      InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
      URI myURI = baseURI.resolve( getXlinkHref());
      InvCatalogImpl mainCatalog = new InvCatalogImpl( getFullName(), parentCatalog.getVersion(), myURI);

      InvDatasetImpl top = new InvDatasetImpl(this); // LOOK clone correct ??
      top.setParent( null);
      InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
      if (parent != null)
        top.transferMetadata( parent ); // make all inherited metadata local

      String id = getID();
      if (id == null)
        id = getPath();
      top.setID(id);

      makeFmrc();

      // add Variables, GeospatialCoverage, TimeCoverage
      ThreddsMetadata tmi = top.getLocalMetadataInheritable();
      if (tmi.getVariables().size() == 0) {
        ThreddsMetadata.Variables vars = MetadataExtractor.extractVariables( this, fmrc.getGridDataset());
        if (vars != null)
          tmi.addVariables(vars);
      }
      if (tmi.getGeospatialCoverage() == null) {
        ThreddsMetadata.GeospatialCoverage gc = MetadataExtractor.extractGeospatial(fmrc.getGridDataset());
        if (gc != null)
          tmi.setGeospatialCoverage(gc);
      }
      if (tmi.getTimeCoverage() == null) {
        DateRange dateRange = MetadataExtractor.extractDateRange(fmrc.getGridDataset());
        if (dateRange != null)
          tmi.setTimeCoverage(dateRange);
      }

      if (null != params) {
        ThreddsMetadata tm = top.getLocalMetadata();
        InvDocumentation doc = new InvDocumentation();
        String path = getPath();
        if (!path.endsWith("/")) path = path + "/";
        doc.setXlinkHref( "/thredds/modelInventory/"+path);
        doc.setXlinkTitle( "Available Inventory");
        tm.addDocumentation( doc);
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
      this.catalog = mainCatalog;
    }

    return catalog;
  }

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

  private InvCatalogImpl makeCatalogRuns(URI baseURI) throws IOException {

    if ((catalogRuns == null) || checkIfChanged()) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      URI myURI = baseURI.resolve( getCatalogHref(RUNS));
      InvCatalogImpl runCatalog  = new InvCatalogImpl( getFullName(), parent.getVersion(), myURI);
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local
      top.setName(TITLE_RUNS);
      runCatalog.addDataset(top);

      // any referenced services need to be local
      List<InvService> services = new ArrayList<InvService>( getServicesLocal());
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        runCatalog.addService(service);

      for (InvDatasetImpl ds : makeRunDatasets()) {
        top.addDataset(ds);
      }
      runCatalog.finish();
      this.catalogRuns = runCatalog;
    }

    return catalogRuns;
  }

  private InvCatalogImpl makeCatalogOffsets(URI baseURI) throws  IOException {

    if ((catalogOffsets == null) || checkIfChanged()) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      URI myURI = baseURI.resolve( getCatalogHref(OFFSET));
      InvCatalogImpl offCatalog = new InvCatalogImpl( getFullName(), parent.getVersion(), myURI);
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local

      top.setName(TITLE_OFFSET);
      offCatalog.addDataset(top);

      // any referenced services need to be local
      List<InvService> services = getServicesLocal();
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        offCatalog.addService(service);

      for (InvDatasetImpl ds : makeOffsetDatasets()) {
        top.addDataset(ds);
      }
      offCatalog.finish();
      this.catalogOffsets = offCatalog;
    }
    return catalogOffsets;
  }

  private InvCatalogImpl makeCatalogForecasts(URI baseURI) throws IOException {

    if ((catalogForecasts == null) || checkIfChanged()){
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      URI myURI = baseURI.resolve( getCatalogHref(FORECAST));
      InvCatalogImpl foreCatalog = new InvCatalogImpl( getFullName(), parent.getVersion(), myURI);
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local
      top.setName(TITLE_FORECAST);
      foreCatalog.addDataset(top);

      // any referenced services need to be local
      List<InvService> services = getServicesLocal();
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        foreCatalog.addService(service);

      for (InvDatasetImpl ds : makeForecastDatasets()) {
        top.addDataset(ds);
      }
      foreCatalog.finish();
      this.catalogForecasts = foreCatalog;
    }
    return catalogForecasts;
  }

  private InvCatalogImpl makeCatalogScan(String orgPath, URI baseURI) {
    if ( !madeDatasets)
      getDatasets();

    return scan.makeCatalogForDirectory( orgPath, baseURI);
  }

  private synchronized void makeFmrc() throws IOException {

    if (madeFmrc) {
      checkIfChanged();
      return;
    }

    Element ncml = getNcmlElement();
    NetcdfDataset ncd = NcMLReader.readNcML(path, ncml, null);
    ncd.setFileCache( fileCache); // LOOK: this dataset never gets closed

    fmrc = new FmrcImpl( ncd);
    madeFmrc = true;
  }

  private List<InvDatasetImpl> makeRunDatasets() throws IOException {
    makeFmrc();

    List<InvDatasetImpl> datasets = new ArrayList<InvDatasetImpl>();
    DateFormatter formatter = new DateFormatter();

    String id = getID();
    if (id == null)
      id = getPath();

    for (Date runDate : fmrc.getRunDates()) {
      //String name = StringUtil.escape(formatter.toDateTimeStringISO( runDate), "");
      String name = getName()+"_"+RUN_NAME+formatter.toDateTimeStringISO( runDate);
      name = StringUtil.replace(name, ' ', "_");
      InvDatasetImpl nested = new InvDatasetImpl(this, name);
      nested.setUrlPath(path+"/"+RUNS+"/"+name);
      nested.setID(id+"/"+RUNS+"/"+name);
      ThreddsMetadata tm = nested.getLocalMetadata();
      tm.addDocumentation("summary", "Data from Run "+name);
      datasets.add( nested);
    }

    Collections.reverse( datasets);
    return datasets;
  }

  private List<InvDatasetImpl> makeOffsetDatasets() throws  IOException {
    makeFmrc();

    List<InvDatasetImpl> datasets = new ArrayList<InvDatasetImpl>();

    String id = getID();
    if (id == null)
      id = getPath();

     for (Double offset : fmrc.getForecastOffsets()) {
       String name = getName()+"_"+OFFSET_NAME+offset+"hr";
       name = StringUtil.replace(name, ' ', "_");
       InvDatasetImpl nested = new InvDatasetImpl(this, name);
       nested.setUrlPath(path+"/"+OFFSET+"/"+name);
       nested.setID(id+"/"+OFFSET+"/"+name);
       ThreddsMetadata tm = nested.getLocalMetadata();
       tm.addDocumentation("summary", "Data from the "+offset+" hour forecasts, across different model runs.");
       datasets.add( nested);
     }

    return datasets;
  }

  private List<InvDatasetImpl> makeForecastDatasets() throws IOException {
    makeFmrc();

    List<InvDatasetImpl> datasets = new ArrayList<InvDatasetImpl>();
    DateFormatter formatter = new DateFormatter();

    String id = getID();
    if (id == null)
      id = getPath();

     for (Date forecastDate : fmrc.getForecastDates()) {
       String name = getName()+"_"+FORECAST_NAME+formatter.toDateTimeStringISO( forecastDate);
       name = StringUtil.replace(name, ' ', "_");
       InvDatasetImpl nested = new InvDatasetImpl(this, name);
       nested.setUrlPath(path+"/"+FORECAST+"/"+name);
       nested.setID(id+"/"+FORECAST+"/"+name);
       ThreddsMetadata tm = nested.getLocalMetadata();
       tm.addDocumentation("summary", "Data with the same forecast date, "+name+", across different model runs.");
       datasets.add( nested);
     }

    return datasets;
  }


  /////////////////////////////////////////////////////////////////////////

  // this is called in a multithreaded enviro.
  // fmrc has state that can change.

  /**
   * Get the dataset named by the path
   * @param path URL path
   * @return requested dataset
   * @throws IOException if read error
   */
  public NetcdfDataset getDataset(String path) throws IOException {
    int pos = path.indexOf("/");
    String type = (pos > -1) ? path.substring(0, pos) : path;
    String name = (pos > -1) ? path.substring(pos+1) : "";

    // check SCAN type before we have to do makeFmrc()
    if (type.equals(SCAN) && (params != null)) {
      String filename = new StringBuilder( params.location )
              .append( params.location.endsWith( "/" ) ? "" : "/" )
              .append( name ).toString();
      return NetcdfDataset.acquireDataset( null, filename, null, -1, null, null); // no enhancement
    }

    makeFmrc();
    NetcdfDataset result = null;
    String location = path;

    if (path.endsWith(FMRC))
      result = fmrc.getFmrcDataset();

    else if (path.endsWith(BEST))
      result = fmrc.getBestTimeSeries();

    else {
      location = name;

      if (type.equals(OFFSET)) {
        int pos1 = name.indexOf(OFFSET_NAME);
        int pos2 = name.indexOf("hr");
        if ((pos1<0) || (pos2<0)) return null;
        String id = name.substring(pos1+OFFSET_NAME.length(), pos2);
        double hour = Double.parseDouble(id);
        result = fmrc.getForecastOffsetDataset( hour);

      } else if (type.equals(RUNS)) {
        int pos1 = name.indexOf(RUN_NAME);
        if (pos1<0) return null;
        String id = name.substring(pos1+RUN_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        result = fmrc.getRunTimeDataset(date);

      } else if (type.equals(FORECAST)) {
        int pos1 = name.indexOf(FORECAST_NAME);
        if (pos1<0) return null;
        String id = name.substring(pos1+FORECAST_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        result = fmrc.getForecastTimeDataset(date);
      }
    }

    if (null != result) result.setLocation( location);
    return result;
  }

}
