/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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
/**
 * User: rkambic
 * Date: Oct 13, 2010
 * Time: 11:19:50 AM
 */

package thredds.server.radarServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import thredds.catalog.*;
import thredds.catalog.query.Station;
import thredds.server.AbstractController;
import thredds.server.config.TdsContext;
import thredds.server.ncss.QueryParams;
import thredds.servlet.HtmlWriter;
import thredds.util.TdsPathUtils;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.unidata.geoloc.LatLonRect;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Processes Queries for the RadarServer  Spring Framework
 */
@Controller
@RequestMapping("/radarServer")
@DependsOn("DataRootHandler")
public class RadarController extends AbstractController {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RadarController.class);

  private static final String CREATE_VIEW = "forward:badquery.htm";  // The view to forward to in case a bad query.
  private static final String MODEL_KEY = "message";  // The model key used to retrieve the message from the model.
  private static final String MSG_CODE = "message.bad.query";  // The unique key for retrieving the text associated with this message.

  private static final String CREATE_STATION_VIEW = "forward:createstation.htm";  // The view to forward to in case an dataset needs to be created.
  private static final String STATION_MSG_CODE = "message.create.station"; // The unique key for retrieving the text associated with this message.

  private static final String CREATE_CATALOG_VIEW = "forward:createdataset.htm";
  private static final String CATALOG_MSG_CODE = "message.create.dataset";

  private static DateType epicDateType;

  static {
    try {
      epicDateType = new DateType(RadarServerUtil.epic, null, null);
    } catch (java.text.ParseException e) {
    }
  }

  // LOOK not thread safe
  private static SimpleDateFormat dateFormat;

  static {
    dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  ///////////////////////////////////////////////////////////////////////

  @Autowired
  private RadarDatasetRepository radarDatasetRepository;

  @Autowired
  private TdsContext tdsContext;

  private boolean releaseDataset = false;
  private boolean enabled = false;

  @Override
  protected String getControllerPath() {
    return "/radarServer/";
  }

  @Override
  protected String[] getEndings() {
    return new String[]{"/stations.xml", "/catalog.xml", "/catalog.html"};

  }

  @PostConstruct
  void init() {
    enabled = radarDatasetRepository.init(tdsContext); // for some reason this is not working directly
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @RequestMapping(value = {"**/stations.xml"}, method = RequestMethod.GET)
  protected ModelAndView handleStationXmlRequest(HttpServletRequest request, HttpServletResponse res) throws IOException {
    if (!enabled) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "Radar server not enabled");
      return null;
    }

    //try {
    // Gather diagnostics for logging request.
    // setup
    String path = TdsPathUtils.extractPath(request, getControllerPath());
    if (path == null) path = "";

    int pos = path.indexOf("/");
    String type = (pos > 0) ? path.substring(0, pos) : "";

    RadarDatasetRepository.RadarType radarType;
    try {
      radarType = RadarDatasetRepository.RadarType.valueOf(type);
    } catch (Exception e) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad radar type=" + type);
      return null;
    }
    // path = path.substring(type.length()+1);

    // return stations of dataset
    Map<String, Object> model = new HashMap<String, Object>();
    stationsXML(radarType, path, model);

    if (model.size() == 0) {
      ModelAndView mav = new ModelAndView(CREATE_STATION_VIEW);
      mav.addObject(MODEL_KEY, STATION_MSG_CODE);
      return mav;
    } else {
      return new ModelAndView("stationXml", model);
    }

    /*} catch (RadarServerException e) {
      throw e; // pass it onto Spring exceptionResolver

    } catch (Throwable e) {
      log.error("handleRequestInternal(): Problem handling request.", e);
      throw new RadarServerException("handleRequestInternal(): Problem handling request.", e);
    }  */
  }

  /*
   * Create an ArrayList of station entries in model for radarType and path
   */
  private void stationsXML(RadarDatasetRepository.RadarType radarType, String path, Map<String, Object> model) {
//          throws Exception {
    // stations in this dataset, set by path
    String[] stations = stationsDS(radarType, radarDatasetRepository.dataRoots.get(path));
    if (path.contains("level3") && stations[0].length() == 4) {
      for (int i = 0; i < stations.length; i++)
        stations[i] = stations[i].substring(1);
    }
    makeStationDocument(stations, radarType, model);
  }

  /*
   * Find the stations for this dataset in path directory
   */
  private String[] stationsDS(RadarDatasetRepository.RadarType radarType, String path) { // throws Exception {

    String[] stations = null;
    // Scan directory looking for actual stations
    if (path != null) {
      File dir = new File(path);
      stations = dir.list();
      // check for level3 stations
      if (path.contains("level3")) {
        dir = null;
        if (radarType.equals(RadarDatasetRepository.RadarType.nexrad)) {
          for (String var : stations) {
            if (var.equals("N0R")) {
              dir = new File(path + "/N0R");
              break;
            }
          }
        } else if (radarType.equals(RadarDatasetRepository.RadarType.terminal)) {
          for (String var : stations) {
            if (var.equals("TR0")) {
              dir = new File(path + "/TR0");
              break;
            }
          }
        }
        if (dir != null) {
          stations = dir.list();
        } else {
          stations = null;
        }
      }
    }
    if (stations != null) {
      // rescan stations array for removal of . files
      ArrayList<String> tmp = new ArrayList<String>();
      for (String station : stations) {
        if (station.startsWith("."))
          continue;
        tmp.add(station);
      }
      if (stations.length != tmp.size()) {
        stations = new String[tmp.size()];
        stations = (String[]) tmp.toArray(stations);
      }
    }
    // no stations found return all known stations for RadarType
    if (stations == null || stations.length == 0) {
      if (stations == null)
        stations = new String[1];
      if (radarType.equals(RadarDatasetRepository.RadarType.nexrad))
        stations = radarDatasetRepository.nexradMap.keySet().toArray(stations);
      else
        stations = radarDatasetRepository.terminalMap.keySet().toArray(stations);
    }
    return stations;
  }

  /**
   * Create  StationEntry objects in entries ArrayList
   *
   * @param stations
   */
  private void makeStationDocument(String[] stations, RadarDatasetRepository.RadarType radarType, Map<String, Object> model) {
//          throws Exception {
    /*
    <station id="KTYX" state="NY" country="US">
      <name>MONTAGUE/Fort_Drum</name>
      <longitude>-75.76</longitude>
      <latitude>43.76</latitude>
      <elevation>562.0</elevation>
    </station>
    */
    List<StationEntry> entries = new ArrayList<StationEntry>();
    for (String s : stations) {
      Station stn = getStation(s, radarType);
      StationEntry se = new StationEntry();
      if (stn == null) { // stn not in table
        se.setId(s);
        se.setState("XXX");
        se.setCountry("XX");
        se.setName("Unknown");
        se.setLongitude("0.0");
        se.setLatitude("0.0");
        se.setElevation("0.0");
        continue;
      }
      // id
      se.setId(s);
      if (stn.getState() != null) {
        se.setState(stn.getState());
      }
      if (stn.getCountry() != null) {
        se.setCountry(stn.getCountry());
      }
      if (stn.getName() != null) {
        se.setName(stn.getName());
      }
      se.setLongitude(ucar.unidata.util.Format.d(stn.getLocation().getLongitude(), 6));
      se.setLatitude(ucar.unidata.util.Format.d(stn.getLocation().getLatitude(), 6));
      if (!Double.isNaN(stn.getLocation().getElevation())) {
        se.setElevation(ucar.unidata.util.Format.d(stn.getLocation().getElevation(), 6));
      }
      entries.add(se);
    }
    model.put("stations", entries);
  }

  /**
   * returns station or null
   *
   * @param station
   * @param radarType
   * @return station
   */
  public Station getStation(String station, RadarDatasetRepository.RadarType radarType) {

    Station stn = null;
    if (station.length() == 3 && radarType.equals(RadarDatasetRepository.RadarType.terminal)) { // terminal level3 station
      stn = radarDatasetRepository.terminalMap.get("T" + station);
    } else if (station.length() == 3) {
      for (Station stn3 : radarDatasetRepository.nexradList) {
        if (stn3.getValue().endsWith(station)) {
          stn = stn3;
          break;
        }
      }
    } else if (radarType.equals(RadarDatasetRepository.RadarType.terminal)) {
      stn = radarDatasetRepository.terminalMap.get(station);
    } else {
      stn = radarDatasetRepository.nexradMap.get(station);
    }
    return stn;
  }

  /*
    StationEntry provides the necessary information for a station entry below.

    <station id="KTYX" state="NY" country="US">
      <name>MONTAGUE/Fort_Drum</name>
      <longitude>-75.76</longitude>
      <latitude>43.76</latitude>
      <elevation>562.0</elevation>
    </station>
    */
  public class StationEntry {

    private String id;
    private String state;
    private String country;
    private String name;
    private String longitude;
    private String latitude;
    private String elevation;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getState() {
      return state;
    }

    public void setState(String state) {
      this.state = state;
    }

    public String getCountry() {
      return country;
    }

    public void setCountry(String country) {
      this.country = country;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getLongitude() {
      return longitude;
    }

    public void setLongitude(String longitude) {
      this.longitude = longitude;
    }

    public String getLatitude() {
      return latitude;
    }

    public void setLatitude(String latitude) {
      this.latitude = latitude;
    }

    public String getElevation() {
      return elevation;
    }

    public void setElevation(String elevation) {
      this.elevation = elevation;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////
  @RequestMapping(value = {"**/*.xml", "**/*.html", "*.xml", "*.html"}, method = RequestMethod.GET)
  protected ModelAndView handleCatalogRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (!enabled) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return null;
    }

    String path = TdsPathUtils.extractPath(request, getControllerPath());
    if (path == null) path = "";

    // return main  catalog xml or html
    if (path.equals("catalog.xml") || path.equals("dataset.xml")) {
      InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false); // no validation
      String catAsString = factory.writeXML(radarDatasetRepository.defaultCat);
      PrintWriter pw = response.getWriter();
      pw.println(catAsString);
      pw.flush();
      return null;
    }

    if (path.equals("catalog.html") || path.equals("dataset.html")) {
      try {
        HtmlWriter.getInstance().writeCatalog(request, response, radarDatasetRepository.defaultCat, true); // show catalog as HTML
      } catch (Exception e) {
        log.error("Radar HtmlWriter failed ", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "radarServer HtmlWriter error " + path);
      }
      return null;
    }

    // default is the complete radarCollection catalog
    InvCatalogImpl catalog = null;
    if (path.contains("level2/catalog.") || path.contains("level3/catalog.") || path.contains("level2/dataset.") || path.contains("level3/dataset.")) {
      catalog = level2level3catalog(radarDatasetRepository.defaultCat, path);
      if (catalog == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, getAbsolutePath(request));
        return null;
      }

      // returns specific dataset information, ie level2/IDD  used by IDV
    } else if (path.endsWith("dataset.xml") || path.endsWith("catalog.xml")) {
      Map<String, Object> model = datasetInfoXml(radarDatasetRepository.defaultCat, path);
      if (model == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, getAbsolutePath(request));
        return null;
      }

      return new ModelAndView("datasetXml", model);
    }

    if (catalog == null) {
      ModelAndView mav = new ModelAndView(CREATE_CATALOG_VIEW); // WTF ??
      mav.addObject(MODEL_KEY, CATALOG_MSG_CODE);
      return mav;

    } else {
      if (path.endsWith(".html")) {
        HtmlWriter.getInstance().writeCatalog(request, response, catalog, true);
        return null;
      } else
        return new ModelAndView("threddsInvCatXmlView", "catalog", catalog);
    }

  }

  /*
   * handles level2 and level3 type of catalog requests
   */
  private InvCatalogImpl level2level3catalog(InvCatalogImpl catalog, String pathInfo) throws IOException {
    //throws RadarServerException, IOException {

    InvCatalogImpl tCat = null;
    // try {
    // extract dataset path
    String dsPath;
    if (pathInfo.indexOf("/dataset") > 0) {
      dsPath = pathInfo.substring(0, pathInfo.indexOf("/dataset"));
    } else if (pathInfo.indexOf("/catalog") > 0) {
      dsPath = pathInfo.substring(0, pathInfo.indexOf("/catalog"));
    } else {
      return null;
    }

    // clone catalog
    ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
    InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false);
    factory.writeXML(catalog, os, true);
    tCat = factory.readXML(new ByteArrayInputStream(os.toByteArray()), null); // RadarDatasetRepository.catURI);

    // modify clone for only requested datasets
    Iterator parents = tCat.getDatasets().iterator();
    while (parents.hasNext()) {
      ArrayList<InvDatasetImpl> delete = new ArrayList<InvDatasetImpl>();
      InvDatasetImpl top = (InvDatasetImpl) parents.next();
      Iterator tDatasets = top.getDatasets().iterator();
      while (tDatasets.hasNext()) {
        InvDatasetImpl ds = (InvDatasetImpl) tDatasets.next();
        if (ds instanceof InvDatasetScan) {
          InvDatasetScan ids = (InvDatasetScan) ds;
          if (ids.getPath() == null)
            continue;
          if (ids.getPath().contains(dsPath)) {
            ids.setXlinkHref(ids.getPath() + "/dataset.xml");
          } else {
            delete.add(ds);
          }
        }
      }
      // remove datasets
      for (InvDatasetImpl idi : delete) {
        top.removeDataset(idi);
      }
    }
    /* } catch (Throwable e) {
      log.error("RadarServer.level2level3catalog", "Invalid url request");
      throw new RadarServerException("Invalid catalog request", e);
    } */
    return tCat;
  }

  /**
   * @param cat      radarCollections catalog
   * @param pathInfo requested dataset by path
   * @return model dataset object to be used by datasetXml.jsjp
   * @throws IOException major
   */
  private Map<String, Object> datasetInfoXml(InvCatalogImpl cat, String pathInfo) throws IOException {
//          throws RadarServerException, IOException {

    // dataset results are stored in model
    Map<String, Object> model = new HashMap<>();

    InvDatasetScan ds = null;
    boolean found = false;
    // try {
    // extract dataset path
    String dsPath;
    if (pathInfo.indexOf("/dataset") > 0) {
      dsPath = pathInfo.substring(0, pathInfo.indexOf("/dataset"));
    } else if (pathInfo.indexOf("/catalog") > 0) {
      dsPath = pathInfo.substring(0, pathInfo.indexOf("/catalog"));
    } else {
      return null;
    }
    // search for dataset by dsPath
    Iterator parents = cat.getDatasets().iterator();
    InvDatasetImpl top = (InvDatasetImpl) parents.next();
    for (Object o : top.getDatasets()) {
      InvDatasetImpl idsi = (InvDatasetImpl) o;
      if (idsi instanceof InvDatasetScan) {
        ds = (InvDatasetScan) idsi;
        if (ds.getPath() == null)
          continue;
        if (ds.getPath().contains(dsPath)) {
          found = true;
          break;
        } else {
          continue;
        }
      }
    }
    /* } catch (Throwable e) {
      log.error("RadarServer.datasetInfoXml", e);
      throw new RadarServerException("CatalogRadarServerController.datasetInfoXml", e);
    }  */
    if (!found) {
      return null;
    }

    // create dataset by storing necessary information in model object that will
    // be displayed by datasetXml.jsp
    // add ID
    model.put("ID", ds.getID());
    model.put("urlPath", ds.getPath());
    model.put("dataFormat", ds.getDataFormatType());
    model.put("documentation", ds.getSummary());
    CalendarDateRange dr = ds.getCalendarDateCoverage();
    /*
    if (pathInfo.contains("IDD")) {
      pw.print(rm.getStartDateTime(ds.getPath()));
    } else {
      pw.print(dr.getStart().toDateTimeStringISO());
    }
    */    //TODO: check
    //pw.print(dr.getStart().toDateTimeStringISO());
    model.put("tstart", dr.getStart().toString());
    model.put("tend", dr.getEnd().toString());
    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    LatLonRect bb = new LatLonRect();
    gc.setBoundingBox(bb);
    model.put("north", gc.getLatNorth());
    model.put("south", gc.getLatSouth());
    model.put("east", gc.getLonEast());
    model.put("west", gc.getLonWest());

    List<ThreddsMetadata.Variables> vars = ds.getVariables();
    if (vars.size() > 0) {
      ThreddsMetadata.Variables cvs = ds.getVariables().get(0);
      List<RsVar> variables = new ArrayList<>();
      for (ThreddsMetadata.Variable v : cvs.getVariableList()) {
        RsVar rsv = new RsVar();
        rsv.setName(v.getName());
        rsv.setVname(v.getVocabularyName());
        rsv.setUnits(v.getUnits());
        variables.add(rsv);
      }
      model.put("variables", variables);
    }
    // not necessary to get stations, IDV does separate request for stations
    //String[] stations = rm.stationsDS(radarType, dataLocation.get(ds.getPath()));
    //rm.printStations(stations, pw, radarType );

    return model;
  }

  /*
   * Used to store the information about a Radar variable from catalog
   */
  public class RsVar {

    private String name;

    private String vname;

    private String units;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getVname() {
      return vname;
    }

    public void setVname(String vname) {
      this.vname = vname;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = units;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Query RadarServer controller for Spring Framework
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @return ModelAndView
   * @throws IOException caught by superclass
   */
  @RequestMapping(value={"/nexrad/level2/{dataset}","/nexrad/level3/{dataset}","/terminal/level3/{dataset}",}, method = RequestMethod.GET)
 // @RequestMapping( value="**",  params={"stn"}, method = RequestMethod.GET)
  protected ModelAndView handleQuery(@RequestParam Map<String,String> allRequestParams, HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (!enabled) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "No radar server");
      return null;
    }

    //try {
    // Gather diagnostics for logging request.
    // catch rogue invalid request here
    if (request.getQueryString() == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No query string");
      return null;
    }

    // Query results in model
    Map<String, Object> model = new HashMap<>();
    radarQuery(request, response, model);
    if (model.size() == 0) {
      ModelAndView mav = new ModelAndView(CREATE_VIEW);
      mav.addObject(MODEL_KEY, MSG_CODE);
      return mav;
    } else {
      return new ModelAndView("queryXml", model);
    }

    /* } catch (Throwable e) {
      log.error("handleRequestInternal(): Problem handling request.", e);
      throw new RadarServerException("handleRequestInternal(): Problem handling request.", e);
    } */
  }

  // get/check/process query
  private void radarQuery(HttpServletRequest req, HttpServletResponse res, Map<String, Object> model) throws IOException { //}, RadarServerException {

    RadarDatasetRepository.RadarType radarType = RadarDatasetRepository.RadarType.nexrad;
    // need to extract data according to the (dataset) given
    String pathInfo = TdsPathUtils.extractPath(req, getControllerPath());
    if (pathInfo == null) pathInfo = "";
    if (pathInfo.startsWith("/"))
      pathInfo = pathInfo.substring(1);
    String rt = pathInfo.substring(0, pathInfo.indexOf('/', 1));

    try {
      radarType = RadarDatasetRepository.RadarType.valueOf(rt);
    } catch (Exception e) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad radarType=" + rt);
      return;

    }
    Boolean level2 = pathInfo.contains("level2");

    // parse the input
    QueryParams qp = new QueryParams();
    if (!qp.parseQuery(req, res, new String[]{QueryParams.XML, QueryParams.HTML, QueryParams.RAW, QueryParams.NETCDF})) {
      //log.error( "parseQuery Failed "+ qp.errs.toString() + req.getQueryString() );
      //throw new RadarServerException( qp.errs.toString() );//+ req.getQueryString() );
      return; //TODO: uncomment above 2 lines when QueryParams exception is fixed
    }

    // check Query Params
    if (!checkQueryParms(radarType, qp, level2)) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad query=" + qp.errs.toString());
      return;
    }

    // check type of output wanted XML html
    qp.acceptType = qp.acceptType.replaceFirst(".*/", "");

    // creates first part of catalog
    if (!createHeader(radarType, qp, pathInfo, model)) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad query=" + qp.errs.toString());
      return;
    }

    // gets products according to stations, time, and variables
    boolean dataFound = false;
    List<DatasetEntry> entries = new ArrayList<>();
    if (qp.vars == null) {
      dataFound = processQuery(pathInfo, qp, null, entries);
      if (releaseDataset)
        radarDatasetRepository.removeRadarDatasetCollection(pathInfo, null);
    } else {
      int count = 0;
      for (String var : qp.vars) {
        dataFound = processQuery(pathInfo, qp, var, entries);
        if (dataFound)
          count++;
        if (releaseDataset)
          radarDatasetRepository.removeRadarDatasetCollection(pathInfo, var);
      }
      if (count > 0)
        dataFound = true;
    }
    // save entries
    model.put("datasets", entries);
    if (dataFound) {
      model.put("documentation", Integer.toString(entries.size()) + " datasets found for query");
    } else if (qp.errs.length() > 0) {
      model.put("documentation", qp.errs.toString());
    } else {
      model.put("documentation", "No data available for station(s) and time range");
    }

  } // end radarNexradQuery

  // check that parms have valid stations, vars and times
  private Boolean checkQueryParms(RadarDatasetRepository.RadarType radarType, QueryParams qp, Boolean level2) {
    //        throws IOException {
    if (qp.hasBB) {
      if (radarType.equals(RadarDatasetRepository.RadarType.nexrad))
        qp.stns = RadarServerUtil.getStationNames(qp.getBB(), radarDatasetRepository.nexradList);
      else
        qp.stns = RadarServerUtil.getStationNames(qp.getBB(), radarDatasetRepository.terminalList);

      if (qp.stns.size() == 0) {
        qp.errs.append("Bounding Box contains no stations ");
        return false;
      }
      if (!level2)
        qp.stns = RadarServerUtil.convert4to3stations(qp.stns);
    }

    if (qp.hasStns) {
      if (radarDatasetRepository.isStationListEmpty(qp.stns, radarType)) {
        qp.errs.append("No valid stations specified, need 1 ");
        return false;
      } else if (level2) {
        for (String stn : qp.stns) {
          if (stn.length() == 3) {
            qp.errs.append("Need 4 character station names ");
            return false;
          }
        }
      } else if (!level2)
        qp.stns = RadarServerUtil.convert4to3stations(qp.stns);
    }

    if (qp.hasLatlonPoint) {
      qp.stns = new ArrayList<String>();
      if (radarType.equals(RadarDatasetRepository.RadarType.nexrad))
        qp.stns.add(RadarServerUtil.findClosestStation(qp.lat, qp.lon, radarDatasetRepository.nexradList));
      else
        qp.stns.add(RadarServerUtil.findClosestStation(qp.lat, qp.lon, radarDatasetRepository.terminalList));
      if (!level2)
        qp.stns = RadarServerUtil.convert4to3stations(qp.stns);
    } else if (qp.fatal) {
      qp.errs.append("No valid stations specified 2 ");
      return false;
    }

    if (qp.stns == null || qp.stns.size() == 0) {
      qp.errs.append("No valid stations specified, need 1 ");
      return false;
    }
    boolean useAllStations = (qp.stns.get(0).toUpperCase().equals("ALL"));
    if (useAllStations) {
      if (radarType.equals(RadarDatasetRepository.RadarType.nexrad))
        qp.stns = RadarServerUtil.getStationNames(radarDatasetRepository.nexradList); //need station names
      else
        qp.stns = RadarServerUtil.getStationNames(radarDatasetRepository.terminalList); //need station names
      if (!level2)
        qp.stns = RadarServerUtil.convert4to3stations(qp.stns);
    }

    if (qp.hasTimePoint) {
      if (qp.time.isPresent()) {
        try {
          qp.time_end = new DateType("present", null, null);
          qp.time_start = epicDateType;
        } catch (java.text.ParseException e) {
          qp.errs.append("Illegal param= 'time' must be valid ISO Duration");
          return false;
        }
      } else {
        qp.time_end = qp.time;
        qp.time_start = qp.time;
      }
    } else if (qp.hasDateRange) {
      DateRange dr = qp.getCalendarDateRange().toDateRange();
      qp.time_start = dr.getStart();
      qp.time_end = dr.getEnd();
    } else { // get all times
      qp.time_latest = 1;
      //qp.hasDateRange = true;
      try {
        qp.time = new DateType("present", null, null);
        qp.time_end = new DateType("present", null, null);
        qp.time_start = epicDateType;
      } catch (java.text.ParseException e) {
        qp.errs.append("Illegal param= 'time' must be valid ISO Duration ");
        return false;
      }
    }

    if (level2) {
      qp.vars = null; // level2 can't select vars
    } else if (qp.vars == null) { //level 3 with no vars
      qp.errs.append("No vars selected ");
      return false;
    } else if (qp.vars.get(0).contains("/")) { // remove desc from vars
      ArrayList<String> tmp = new ArrayList<String>();
      for (String var : qp.vars) {
        tmp.add(var.replaceFirst("/.*", ""));
      }
      qp.vars = tmp;
    }
    return true;
  }

  // create catalog Header
  private Boolean createHeader(RadarDatasetRepository.RadarType radarType, QueryParams qp, String pathInfo, Map<String, Object> model) {
    //         throws IOException {

    Boolean level2 = pathInfo.contains("level2");
    int level = (level2) ? 2 : 3;
    StringBuffer str = new StringBuffer();
    str.append("Radar Level").append(level).append(" datasets in near real time");
    model.put("name", str.toString());
    str.setLength(0);
    str.append("/thredds/dodsC/").append(pathInfo).append("/");
    model.put("base", str.toString());
    str.setLength(0);
    str.append("RadarLevel").append(level).append(" datasets for available stations and times");
    model.put("dname", str.toString());
    str.setLength(0);
    str.append("accept=").append(qp.acceptType).append("&");
    if (!level2 && qp.vars != null) { // add vars
      str.append("var=");
      for (int i = 0; i < qp.vars.size(); i++) {
        str.append(qp.vars.get(i));
        if (i < qp.vars.size() - 1) {
          str.append(",");
        }
      }
      str.append("&");
    }
    // use all stations
    if (qp.stns.get(0).toUpperCase().equals("ALL")) {
      str.append("stn=ALL&");
    } else if (qp.hasStns) {
      for (String station : qp.stns) {
        str.append("stn=").append(station).append("&");
      }
    } else if (qp.hasBB) {
      str.append("south=").append(qp.south).append("&north=").append(qp.north).append("&");
      str.append("west=").append(qp.west).append("&east=").append(qp.east).append("&");
    }

    // no time given
    if (qp.time_latest == 1) {
      //str.deleteCharAt( str.length() -1);
      str.append("time=present");
    } else if (qp.hasDateRange) {
      if (qp.time_start.getDate() == null || qp.time_start.isBlank() ||
              qp.time_end.getDate() == null || qp.time_end.isBlank()) {
        str.append("time_start=").append(qp.time_start.toString());
        str.append("&time_end=").append(qp.time_end.toString());
        qp.errs.append("need ISO time format ");
        return false;
      } else {
        str.append("time_start=").append(qp.time_start.toDateTimeStringISO());
        str.append("&time_end=").append(qp.time_end.toDateTimeStringISO());
      }
    } else if (qp.time.isPresent()) {
      str.append("time=present");
    } else if (qp.hasTimePoint) {
      if (qp.time.getDate() == null || qp.time.isBlank()) {
        str.append("time=").append(qp.time.toString());
        qp.errs.append("need ISO time format ");
        return false;
      } else {
        str.append("time=").append(qp.time.toDateTimeStringISO());
      }
    }
    model.put("ID", str.toString());

    if (level2) {
      model.put("type", "NEXRAD2");
    } else if (radarType.equals(RadarDatasetRepository.RadarType.nexrad)) {
      model.put("type", "NIDS");
    } else {
      model.put("type", "TDWR");
    }

    // at this point must have stations
    if (radarDatasetRepository.isStationListEmpty(qp.stns, radarType)) {
      qp.errs.append("No station(s) meet query criteria ");
      return false;
    }
    return true;
  }


  /*
      Final Output format, save information in DatasetEntry de
       <dataset name="Level2_KFTG_20100121_0000.ar2v" ID="735519521"
          urlPath="KFTG/20100121/Level2_KFTG_20100121_0000.ar2v">
          <date type="start of ob">2010-01-21T00:00:00</date>
        </dataset>
  */
  private Boolean processQuery(String dataset, QueryParams qp, String var, List<DatasetEntry> entries) { // throws RadarServerException {

    Boolean getAllTimes = true;
    String yyyymmddStart = null;
    String yyyymmddEnd = null;
    String dateStart = null;
    String dateEnd = null;

    if (!qp.time_start.equals(epicDateType)) {
      getAllTimes = false;
      yyyymmddStart = qp.time_start.toDateString();
      yyyymmddStart = yyyymmddStart.replace("-", "");
      yyyymmddEnd = qp.time_end.toDateString();
      yyyymmddEnd = yyyymmddEnd.replace("-", "");
      dateStart = yyyymmddStart + "_" + RadarServerUtil.hhmm(qp.time_start.toDateTimeString());
      dateEnd = yyyymmddEnd + "_" + RadarServerUtil.hhmm(qp.time_end.toDateTimeString());
    }

    RadarDatasetCollection rdc = radarDatasetRepository.getRadarDatasetCollection(dataset, var);
    if (rdc == null) {
      qp.errs.append("Invalid dataset =").append(dataset);
      qp.errs.append(" or var =").append(var);
      return false;
    }
    StringBuffer time = new StringBuffer();
    StringBuffer product = new StringBuffer();
    StringBuffer url = new StringBuffer();
    boolean isLevel2 = dataset.contains("level2");
    String type = (isLevel2 ? "Level2" : "Level3");
    String suffix = (isLevel2 ? ".ar2v" : ".nids");
    Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT"));
    Date now = cal.getTime();
    String currentDay = dateFormat.format(now);

    for (String stn : qp.stns) {
      RadarStationCollection rsc = rdc.queryStation(stn, currentDay);
      if (rsc == null)
        continue;
      for (String day : rsc.getDays()) {
        // check for valid day
        if (!getAllTimes &&
                !RadarServerUtil.isValidDay(day, yyyymmddStart, yyyymmddEnd))
          continue;
        ArrayList<String> tal;
        if (rdc.isCaseStudy()) { //
          tal = rsc.getHourMinute("all");
          for (String prod : tal) {
            // check times
            if (!getAllTimes &&
                    !RadarServerUtil.isValidDate(prod, dateStart, dateEnd))
              continue;
            // save this entry
            DatasetEntry de = new DatasetEntry();
            int idx = prod.indexOf('/');
            if (idx > 0) {
              de.setName(prod.substring(idx + 1));
            } else {
              de.setName(prod);
            }
            de.setID(Integer.toString(prod.hashCode()));
            url.setLength(0);
            url.append(stn).append("/");
            if (var != null) {
              url.append(var).append("/");
            }
            url.append(prod);
            de.setUrlPath(url.toString());
            de.setDate(RadarServerUtil.getObTimeISO(prod));
            entries.add(de);
          }
          continue;
        } else {
          tal = rsc.getHourMinute(day);
        }
        if (tal == null)
          continue;
        for (String hm : tal) {
          time.setLength(0);
          time.append(day).append("_").append(hm);
          if (!getAllTimes &&
                  !RadarServerUtil.isValidDate(time.toString(), dateStart, dateEnd))
            continue;

          // save this entry
          DatasetEntry de = new DatasetEntry();

          product.setLength(0);
          product.append(type).append("_").append(rsc.getStnName()).append("_");
          if (!isLevel2)
            product.append(var).append("_");
          product.append(day).append("_").append(hm).append(suffix);

          de.setName(product.toString());
          de.setID(Integer.toString(product.toString().hashCode()));
          url.setLength(0);
          if (!isLevel2) {
            url.append(var).append("/");
          }
          url.append(rsc.getStnName()).append("/").append(day).append("/").append(product.toString());
          de.setUrlPath(url.toString());
          de.setDate(RadarServerUtil.getObTimeISO(product.toString()));
          entries.add(de);
          if (qp.hasTimePoint)
            break;
        }
        if (qp.hasTimePoint)
          break;
      }
    }
    return true;
  }

  /*
   * Used to store the information about a dataset
   */
  public class DatasetEntry {

    private String name;

    private String ID;

    private String urlPath;

    private String date;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getID() {
      return ID;
    }

    public void setID(String ID) {
      this.ID = ID;
    }

    public String getUrlPath() {
      return urlPath;
    }

    public void setUrlPath(String urlPath) {
      this.urlPath = urlPath;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }
  }

}
