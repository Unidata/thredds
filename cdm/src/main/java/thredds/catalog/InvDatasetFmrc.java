// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
import ucar.nc2.thredds.MetadataExtractor;
import thredds.datatype.DateRange;

/**
 * InvDatasetFmrc deals with datasetFmrc elementss.
 *
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

  //////////////////////////////////////////////

  private volatile boolean madeDatasets = false, madeFmrc = false;

  private String path;
  private ForecastModelRunCollection fmrc;
  private InvCatalogImpl catalog, catalogRuns, catalogOffsets, catalogForecasts;
  private InventoryParams params;
  private InvDatasetScan scan;
  private String dodsService;
  private boolean runsOnly;

  public InvDatasetFmrc(InvDatasetImpl parent, String name, String path, boolean runsOnly) {
    super(parent, name, "/thredds/catalog/"+path+"/catalog.xml");
    this.path = path;
    this.runsOnly = runsOnly;
  }

  public String getPath() { return path; }
  public InvDatasetScan getRawFileScan()
  {
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
    StringBuffer fname = new StringBuffer( params.location);
    if ( ! params.location.endsWith( "/"))
      fname.append( "/");
    fname.append( ( pos > -1 ) ? remaining.substring( pos + SCAN.length() + 1 ) : remaining);
    return new File( fname.toString() );
  }

  // bit of a kludge to get info into the InvDatasetFmrc
  public void setFmrcInventoryParams(String location, String def, String suffix, String olderThanS) {
    this.params = new InventoryParams();
    this.params.location = location;
    this.params.def = def;
    this.params.suffix = suffix;

    if (olderThanS != null) {
      try {
        TimeUnit tu = new TimeUnit(olderThanS);
        this.params.lastModifiedLimit  = (long) (1000 * tu.getValueInSeconds());
      } catch (Exception e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }
  }

  public class InventoryParams {
    public String location, def, suffix;
    public long lastModifiedLimit = 0;
    public String toString() { return "def="+def +" location="+ location+" suffix="+ suffix+" lastModifiedLimit="+lastModifiedLimit; }
  }

  public boolean hasAccess() {
    return false;
  }

  public boolean hasNestedDatasets() {
    return true;
  }

  public InvCatalogImpl makeCatalog(String match, String orgPath, URI baseURI ) {
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
      logger.error("Error making catalog", e);
      return null;
    }
  }

  public java.util.List getDatasets() {
    if (!madeDatasets) {
      ArrayList datasets = new ArrayList();

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
        ds.setUrlPath(path+"/"+name);
        ds.setID(id+"/"+name);
        ThreddsMetadata tm = ds.getLocalMetadata();
        tm.addDocumentation("summary", "Forecast Model Run Collection (2D time coordinates).");
        ds.getLocalMetadataInheritable().setServiceName(dodsService);
        ds.finish();
        datasets.add( ds);

        ds = new InvDatasetImpl(this, "Best Time Series");
        name = getName()+"_"+BEST;
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

          scan = new InvDatasetScan( (InvCatalogImpl) this.getParentCatalog(), this, "File_Access", path+"/"+SCAN,
                  params.location, ".*"+params.suffix, true, "true", false, null, null, null, params.lastModifiedLimit );

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

  private InvCatalogImpl makeCatalog(URI baseURI) throws IOException, URISyntaxException {
    boolean changed = checkIfChanged();

    if (changed || (catalog == null)) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      URI myURI = baseURI.resolve( getXlinkHref());
      InvCatalogImpl mainCatalog = new InvCatalogImpl( getFullName(), parent.getVersion(), myURI);

      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local

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
      List serviceAll = parent.getServices();
      for (int i = 0; i < serviceAll.size(); i++) {
        InvService service = (InvService) serviceAll.get(i);
        if (!serviceLocal.contains(service))
          mainCatalog.addService(service);
      }
      findDODSService(serviceAll); // LOOK kludgey

      List datasets = getDatasets();
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
        top.addDataset(ds);
      }
      mainCatalog.finish();
      this.catalog = mainCatalog;
    }
    return catalog;
  }

  private void findDODSService(List services) {
    for (int i = 0; i < services.size(); i++) {
      InvService service = (InvService) services.get(i);
      if ((dodsService == null) && (service.getServiceType() == ServiceType.OPENDAP)) {
        dodsService = service.getName();
        return;
      }
      if (service.getServiceType() == ServiceType.COMPOUND)
        findDODSService(service.getServices());
    }
  }

  private InvCatalogImpl makeCatalogRuns(URI baseURI) throws IOException {
    boolean changed = checkIfChanged();

    if (changed || catalogRuns == null) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      URI myURI = baseURI.resolve( getCatalogHref(RUNS));
      InvCatalogImpl runCatalog  = new InvCatalogImpl( getFullName(), parent.getVersion(), myURI);
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      //top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local
      top.setName(TITLE_RUNS);
      runCatalog.addDataset(top);

      // any referenced services need to be local
      ArrayList services = new ArrayList( getServicesLocal());
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        runCatalog.addService(service);

      List datasets = makeRunDatasets();
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
        top.addDataset(ds);
      }
      runCatalog.finish();
      this.catalogRuns = runCatalog;
    }

    return catalogRuns;
  }

  private InvCatalogImpl makeCatalogOffsets(URI baseURI) throws IOException {
    boolean changed = checkIfChanged();

    if (changed || catalogOffsets == null) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      URI myURI = baseURI.resolve( getCatalogHref(OFFSET));
      InvCatalogImpl offCatalog = new InvCatalogImpl( getFullName(), parent.getVersion(), myURI);
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      //top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local

      top.setName(TITLE_OFFSET);
      offCatalog.addDataset(top);

      // any referenced services need to be local
      ArrayList services = new ArrayList( getServicesLocal());
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        offCatalog.addService(service);

      List datasets = makeOffsetDatasets();
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
        top.addDataset(ds);
      }
      offCatalog.finish();
      this.catalogOffsets = offCatalog;
    }
    return catalogOffsets;
  }

  private InvCatalogImpl makeCatalogForecasts(URI baseURI) throws IOException {
    boolean changed = checkIfChanged();

    if (changed || catalogForecasts == null) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      URI myURI = baseURI.resolve( getCatalogHref(FORECAST));
      InvCatalogImpl foreCatalog = new InvCatalogImpl( getFullName(), parent.getVersion(), myURI);
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      // top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local
      top.setName(TITLE_FORECAST);
      foreCatalog.addDataset(top);

      // any referenced services need to be local
      ArrayList services = new ArrayList( getServicesLocal());
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        foreCatalog.addService(service);

      List datasets = makeForecastDatasets();
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
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
    // LOOK: when is fmrc closed? what about caching

    if (madeFmrc)
      return;

    Element ncml = getNcmlElement();
    NetcdfDataset ncd = NcMLReader.readNcML(ncml, null);
    fmrc = new FmrcImpl( ncd);
    madeFmrc = true;
  }

  private List makeRunDatasets() throws IOException {
    makeFmrc();

    ArrayList datasets = new ArrayList();
    DateFormatter formatter = new DateFormatter();

    String id = getID();
    if (id == null)
      id = getPath();

    List runs = fmrc.getRunDates();
    for (int i = 0; i < runs.size(); i++) {
      Date runDate = (Date) runs.get(i);
      //String name = StringUtil.escape(formatter.toDateTimeStringISO( runDate), "");
      String name = getName()+"_"+RUN_NAME+formatter.toDateTimeStringISO( runDate);
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

  private List makeOffsetDatasets() throws IOException {
    makeFmrc();

    ArrayList datasets = new ArrayList();

    String id = getID();
    if (id == null)
      id = getPath();

     List offsets = fmrc.getForecastOffsets();
     for (int i = 0; i < offsets.size(); i++) {
       Double offset = (Double) offsets.get(i);
       String name = getName()+"_"+OFFSET_NAME+offset+"hr";
       InvDatasetImpl nested = new InvDatasetImpl(this, name);
       nested.setUrlPath(path+"/"+OFFSET+"/"+name);
       nested.setID(id+"/"+OFFSET+"/"+name);
       ThreddsMetadata tm = nested.getLocalMetadata();
       tm.addDocumentation("summary", "Data from the "+offset+" hour forecasts, across different model runs.");
       datasets.add( nested);
     }

    return datasets;
  }

  private List makeForecastDatasets() throws IOException {
    makeFmrc();

    ArrayList datasets = new ArrayList();
    DateFormatter formatter = new DateFormatter();

    String id = getID();
    if (id == null)
      id = getPath();

     List forecasts = fmrc.getForecastDates();
     for (int i = 0; i < forecasts.size(); i++) {
       Date forecastDate = (Date) forecasts.get(i);
       String name = getName()+"_"+FORECAST_NAME+formatter.toDateTimeStringISO( forecastDate);
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
  public NetcdfDataset getDataset(String path) throws IOException {
    int pos = path.indexOf("/");
    String type = (pos > -1) ? path.substring(0, pos) : path;
    String name = (pos > -1) ? path.substring(pos+1) : "";

    // check SCAN type before we have to do makeFmrc()
    if (type.equals(SCAN) && (params != null)) {
      String filename = new StringBuffer( params.location )
              .append( params.location.endsWith( "/" ) ? "" : "/" )
              .append( name ).toString();
      return NetcdfDataset.acquireDataset( filename, null);
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
        if (date == null)
         logger.warn("Cant parse date "+id);
       else {
          result = fmrc.getForecastTimeDataset(date);
          if (result == null)
            logger.warn("Dont have forecast date "+id);
        }
      }
    }

    if (null != result) result.setLocation( location);
    return result;
  }

}
