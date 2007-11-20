/* class RadarLevel2Servlet
 *
 * Library for the RadarLevel2 Servlet
 *
 * Finds the near realtime product files for a particular station and time.
 * Outputs either html, dqc  or catalog files.
 *
 * By:  Robb Kambic  09/25/2007
 *
 */

package thredds.servlet.idd;

import thredds.servlet.*;
import java.io.*;
import java.util.*;
//import java.lang.String.*;
//import java.net.URL;
//import java.net.URLConnection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;
//import java.util.Date;
//import java.util.TimeZone;
//import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;
//import thredds.server.ncSubset.QueryParams;
import thredds.datatype.DateRange;
import thredds.datatype.DateType;
//import thredds.datatype.TimeDuration;
import thredds.server.ncSubset.QueryParams;
import thredds.catalog.XMLEntityResolver;
import thredds.catalog.query.Station;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.point.StationObsDatasetInfo;
import ucar.nc2.units.DateFormatter;

public class RadarLevel2Servlet extends AbstractServlet {

    // static configuration file ThreddsIDD.cfg parameters
    //static protected String catalogVersion;
    //static protected String defaultCatalog;
    //static protected String backgroundImage;
    static protected String radarLevel2DQC;

    static protected String radarLevel2Dir = null;

    static protected HashMap < String, String> configurations = null;

    private boolean allow = false;
    private IddMethods im;
    private boolean debug = false;

    protected long getLastModified(HttpServletRequest req) {
        //  get configurations from ThreddsIDD.cfg
        if( configurations == null ) {
            contentPath = ServletUtil.getContentPath(this);
            getConfigurations("synoptic", null);
            radarLevel2DQC = contentPath + im.getPath() + configurations.get( "radarLevel2DQC" );
        }
        File file = new File( radarLevel2DQC );

        return file.lastModified();
    }

   // must end with "/"
   protected String getPath() {
     return "idd/";
   }

  protected void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("NetcdfSubsetServer");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showRadarLevel2Files", "Show RadarLevel2 Files") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println("RadarLevel2 Files\n");
        //ArrayList<RadarLevel2Collection.Dataset> list = rl2c.getDatasets();
        //for (RadarLevel2Collection.Dataset ds : list) {
          //e.pw.println(" " + ds);
        //}
      }
    };
    debugHandler.addAction(act);
  }

  public void init() throws ServletException  {
      super.init();
      allow = ThreddsConfig.getBoolean("NetcdfSubsetService.allow", true);
      //String radarLevel2Dir = ThreddsConfig.get("NetcdfSubsetService.radarLevel2DataDir", "/data/ldm/pub/native/radar/level2/");
      if (!allow) return;

      im = new IddMethods();
      //String ISO = im.getObTimeISO( "KDMX19980629_185623");
      if (null == im.stationList){
         if( radarLevel2Dir == null ) {
             contentPath = ServletUtil.getContentPath(this);
             getConfigurations("synoptic", null);
             radarLevel2DQC = contentPath + im.getPath() + configurations.get( "radarLevel2DQC" );
          }
          im.getStations( radarLevel2DQC );
      }
  }



// get parmameters from servlet call
public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

    // servlet parameters
    //String[] PRODS;
    String dtime;
    String accept, returns;
    String serviceName = null;
    String serviceType = null;
    String serviceBase = null;
    PrintWriter pw;


    try {
        ServletUtil.logServerAccessSetup( req );
        if (debug) System.out.println(req.getQueryString());
        pw = res.getWriter();
        im.setPW( pw );
        contentPath = ServletUtil.getContentPath(this);

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        boolean wantXML = pathInfo.endsWith("dataset.xml");
        boolean showForm = pathInfo.endsWith("dataset.html");
        boolean wantDQC = pathInfo.endsWith("datasetDQC.xml");
        boolean wantStationXML = pathInfo.endsWith("stations.xml");
        if (wantXML || showForm || wantStationXML || wantDQC ) {
           wants(res, wantXML, wantStationXML, wantDQC, pw);
           return;
        }
        // parse the input
        QueryParams qp = new QueryParams();
        if (!qp.parseQuery(req, res, new String[]{ QueryParams.XML, QueryParams.CSV, QueryParams.RAW, QueryParams.NETCDF}))
          return; // has sent the error message


        if (qp.hasBB) {
          qp.stns = im.getStationNames(qp.getBB());
          if (qp.stns.size() == 0) {
            qp.errs.append("ERROR: Bounding Box contains no stations\n");
            qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
            return;
          }
        } else { // old style
          // bounding box min/max lat  min/max lon
          qp.south = qp.parseLat( req, "y0");
          qp.north = qp.parseLat( req, "y1");
          qp.west = qp.parseLon( req, "x0");
          qp.east = qp.parseLon( req, "x1");
        }

        if (qp.hasStns && im.isStationListEmpty(qp.stns)) {
          qp.errs.append("ERROR: No valid stations specified\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }

        if (qp.hasLatlonPoint) {
          qp.stns = new ArrayList<String>();
          qp.stns.add( im.findClosestStation(qp.lat, qp.lon));
        } else if (qp.fatal) {
          qp.errs.append("ERROR: No valid stations specified\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }

        boolean useAllStations = (!qp.hasBB && !qp.hasStns && !qp.hasLatlonPoint);
        if (useAllStations)
          qp.stns = im.getStationNames(); //need station names
          //qp.stns = new ArrayList<String>(); // empty list denotes all

        // get the requested dataset
        String ds = ServletUtil.getParameterIgnoreCase(req, "ds");
        if( ds == null )
            ds = "TDS";
        if (IddMethods.p_all_i.matcher(ds).find()) { // ds names
            res.setContentType("text/html");
            String datasets = configurations.get( "radarLevel2Ds" );
            pw.println( datasets.replaceAll( "/", " "));
            return;
        }

        // need to extract data according to the ds(dataset) given
        radarLevel2Dir = configurations.get( "radarLevel2Dir" + ds );
        String dirStructure = configurations.get( "radarLevel2DirStructure" + ds );
 
        /*
        if (qp.hasTimePoint && ( im.filterDataset(qp.time) == null)) {
          qp.errs.append("ERROR: This dataset does not contain the time point= " + qp.time + " \n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
        */

        if (qp.hasDateRange) {
          DateRange dr = qp.getDateRange();
          if (! im.intersect(dr)) {
            qp.errs.append("ERROR: This dataset does not contain the time range= " + qp.time + " \n");
            qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
            return;
          }
        }

        /*
        if (useAllStations && useAllTimes) {
          qp.errs.append("ERROR: You must subset by space or time\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
        */
        boolean latest = ((!qp.hasTimePoint && !qp.hasDateRange) || qp.time.isPresent() );
        // old style of relative time
        dtime = ServletUtil.getParameterIgnoreCase(req, "dtime");
        latest = (dtime != null? false : latest);
        if( latest ) {
           try {
               Calendar now = Calendar.getInstance();
               qp.time_end = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
               now.add( Calendar.HOUR, -120 );
               qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
           } catch (java.text.ParseException e) {
               qp.errs.append("Illegal param= 'latest' must be valid ISO Duration\n");
           }
        } else if( qp.hasDateRange ) {
            DateRange dr = qp.getDateRange();
            qp.time_start = dr.getStart();
            qp.time_end = dr.getEnd();

        } else if( dtime != null) {
            Calendar now = Calendar.getInstance();
            qp.time_end = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            if (dtime == null || dtime.equals("latest")) {
                now.add( Calendar.HOUR, -120 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("1hour")) {
                now.add( Calendar.HOUR, -1 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("6hour")) {
                now.add( Calendar.HOUR, -6 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("12hour")) {
                now.add( Calendar.HOUR, -12 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("1day")) {
                now.add( Calendar.HOUR, -24 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("2day")) {
                now.add( Calendar.HOUR, -48 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("3day")) {
                now.add( Calendar.HOUR, -72 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("4day")) {
                now.add( Calendar.HOUR, -96 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("5day")) {
                now.add( Calendar.HOUR, -120 );
                qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
            } else if (dtime.equals("all")) {
                //now.add( Calendar.HOUR, -168 );
                //qp.time_start = new DateType(im.dateFormatISO.format(now.getTime()), null, null);
                qp.time_start = new DateType( "1971-01-00T00:00:00Z", null, null);
            }

        } else { // old style
          qp.time_start = qp.parseDate(  req, "dateStart");
          qp.time_end = qp.parseDate(  req, "dateEnd");
        }


        // will need for level3 vars
        //PRODS = ServletUtil.getParameterValuesIgnoreCase(req, "PROD");

        returns = ServletUtil.getParameterIgnoreCase(req, "returns");
        if (returns == null )  // default
            returns = "catalog";

        serviceType = ServletUtil.getParameterIgnoreCase(req, "serviceType");
        if (serviceType == null)
            serviceType = "OPENDAP";

        serviceName = ServletUtil.getParameterIgnoreCase(req, "serviceName");

        // set content type
        String contentType = qp.acceptType;
        if (IddMethods.p_html_i.matcher(returns).find()) { // returns html
            contentType = "text/html";
        }
        res.setContentType(contentType);

        //  get configurations from ThreddsIDD.cfg
        if( radarLevel2Dir == null ) {
           getConfigurations("synoptic", pw);
           radarLevel2DQC = contentPath + im.getPath() + configurations.get( "radarLevel2DQC" );
        }

        if (IddMethods.p_qc_or_dqc_i.matcher(returns).find()) { // returns dqc doc

            BufferedReader br = im.getInputStreamReader( radarLevel2DQC );
            String input = "";
            while ((input = br.readLine()) != null) {
                pw.println(input);
            }
            br.close();
            return;
        }
        if (IddMethods.p_stn_i.matcher(returns).find()) { // returns stn names
            res.setContentType("text/html");
            if( ds.equals( "TDS")) {
                 List<String> stations = im.getStationNames();
                 for (String s : stations) {
                     pw.print( s +" ");
                 }
                 pw.println();
                 return;
            }
            if( dirStructure.startsWith( "STN")) {
                File dir = new File(radarLevel2Dir );
                String[] stations = dir.list();
                for (String s : stations) {
                     pw.print( s +" ");
                 }
                 pw.println();
                 return;
            }
            if( dirStructure.startsWith( "DAY")) {
                File dir = new File(radarLevel2Dir );
                String[] days = dir.list();
                for (String d : days) {
                     pw.println( d +":" );
                     dir = new File(radarLevel2Dir +"/"+ d );
                     String[] stations = dir.list();
                     for (String s : stations) {
                         pw.print( s +" ");
                     }
                     pw.println();

                 }
                 pw.println();
                 return;
            }
        }
        // requesting a catalog with different serviceTypes
        if (IddMethods.p_HTTPServer_i.matcher(serviceType).find() ) {
                serviceName = configurations.get( "radarLevel2HTTPServiceName");
                serviceType = configurations.get( "radarLevel2HTTPServiceType");
                serviceBase = configurations.get( "radarLevel2HTTPServiceBase" +ds);
        //  serviceType =~ /OPENDAP/i
        } else if (IddMethods.p_DODS_i.matcher(serviceType).find()) {
                serviceName = configurations.get( "radarLevel2DODSServiceName");
                serviceType = configurations.get( "radarLevel2DODSServiceType");
                serviceBase = configurations.get( "radarLevel2DODSServiceBase" +ds);
        }
        // write out catalog with datasets
        if ( ! serviceType.equals( "" )) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.print("<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"");
            pw.println(" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Level2 datasets in near real time\" version=\""+
                    configurations.get( "catalogVersion") +"\">");
            pw.println("");
            pw.print("  <service name=\""+ serviceName +"\" serviceType=\""+ serviceType +"\"");
            pw.println(" base=\"" + serviceBase + "\"/>");
            if( ! ds.equals( "TDS"))
                pw.println(" <datasetRoot path=\""+ ds +"\" location=\""+ radarLevel2Dir +"\" />");
            pw.print("    <dataset name=\"RadarLevel2 datasets for available stations and times\" collectionType=\"TimeSeries\" ID=\"serviceType="+
serviceType +"&amp;returns=" + returns + "&amp;");

            if( useAllStations ) {
                pw.print("stn=all&amp;");
            } else if (qp.hasStns && ! im.isStationListEmpty(qp.stns)) {
                for (String station : qp.stns) {
                    pw.print("stn=" + station +"&amp;");
                }
            } else if (qp.hasBB) {

                pw.print("south="+ qp.south +"&amp;north="+ qp.north +"&amp;" );
                pw.print("west="+ qp.west +"&amp;east="+ qp.east +"&amp;" );
            }

            if( dtime != null ) {
                pw.println("dtime=" + dtime + "\">");
            } else if( qp.hasDateRange ) {
                pw.println("time_start=" + qp.time_start.toDateTimeString()
                        +"&amp;time_end=" + qp.time_end.toDateTimeString() +"\">");
            } else if( latest ) {
                pw.println("time=latest\">");
            }
            pw.println("    <metadata inherited=\"true\">");
            pw.println("      <dataType>Radial</dataType>");
            pw.println("      <dataFormat>" + "NEXRAD2" + "</dataFormat>");
            pw.println("      <serviceName>" + serviceName + "</serviceName>");
            pw.println("    </metadata>");
            pw.println();

        } else if (IddMethods.p_html_i.matcher(returns).find()) { // returns html
            pw.println("<Head><Title>THREDDS RadarLevel2 Server</Title></Head>");
            pw.println("<body bgcolor=\"lightblue\" link=\"red\" alink=\"red\" vlink=\"red\">");
            pw.println("<center><H1>RadarLevel2 Selection Results</H1></center>");

        } else if (IddMethods.p_ascii_i.matcher(returns).find()) {

        } else if (IddMethods.p_xml_i.matcher(returns).find()) {
            pw.println("<reports>");
        }
//
//
//
//
//
// main code body, no configurations below this line
//
        // at this point should have stations
        if (  im.isStationListEmpty(qp.stns)) {
            pw.println("      <documentation>No data available for station(s) "+
                "and time range</documentation>");
            pw.println("    </dataset>");
            pw.println("</catalog>");
            return;
        }

        // set date info 
        String yyyymmddStart = qp.time_start.toDateString();
        yyyymmddStart = yyyymmddStart.replace( "-", "");
        String yyyymmddEnd = qp.time_end.toDateString();
        yyyymmddEnd = yyyymmddEnd.replace( "-", "");
        String dateStart =  yyyymmddStart +"_"+ im.hhmm( qp.time_start.toDateTimeString() );
        String dateEnd =  yyyymmddEnd +"_"+ im.hhmm( qp.time_end.toDateTimeString() );

        boolean dataFound = false; // checks if any data found
        boolean done = false; // used for early exit when all data not required
        // loop on qp.stns DAYS
        if( dirStructure.startsWith( "STN")) {
            for (String station : qp.stns) {
                done = false;
                // Obtain the days available
                String[] DAYS = im.getDAYS(radarLevel2Dir +"/"+ station, pw);
                if (DAYS == null || DAYS.length == 0)
                    continue;
                for (int i = 0; i < DAYS.length && !done; i++) {
                    if( ! im.isValidDay( DAYS[ i ], yyyymmddStart, yyyymmddEnd ) )
                        continue;

                     ArrayList<String> files = new ArrayList();
                     File dir = new File(radarLevel2Dir +"/"+ station +"/"+ DAYS[ i ]);
                     String[] FILES = dir.list();
                     for( int t = 0; t < FILES.length; t++ ) {
                         if( ! im.isValidDate( FILES[ t ], dateStart, dateEnd ) )
                             continue;
                         files.add( FILES[ t ] );

                     }
                     if( files.size() > 0 ) {
                         dataFound = true;
                         done = latest;
                         processDS( files, DAYS[ i ], returns, station, latest, pw );
                     }

                }
            }
        // loop on DAY STN
        } else if( dirStructure.startsWith( "DAY")) {
            String[] DAYS = im.getDAYS(radarLevel2Dir, pw);
                for (int i = 0; i < DAYS.length; i++) {
                    if( ! im.isValidDay( DAYS[ i ], yyyymmddStart, yyyymmddEnd ) )
                        continue;
                    for (String station : qp.stns) {
                    //for (int j = 0; j < qp.stns.size(); j++ ) {
                        //String station = qp.stns.get( j );
                        ArrayList<String> files = new ArrayList();
                        File dir = new File(radarLevel2Dir +"/"+ DAYS[ i ] +"/"+ station);
                        if( !dir.exists() )
                            continue;
                        String[] FILES = dir.list();
                        for( int t = 0; t < FILES.length; t++ ) {
                             if( ! im.isValidDate( FILES[ t ], dateStart, dateEnd ) )
                                  continue;
                             files.add( FILES[ t ] );

                        }
                        if( files.size() > 0 ) {
                            dataFound = true;
                            done = latest;
                            processDS( files, DAYS[ i ], returns, station, latest, pw );
                        }

                    }
                }
        }

        // add ending tags
        if (IddMethods.p_catalog_i.matcher(returns).find() ) {
            if (! dataFound) {
                pw.println("      <documentation>No data available for station(s) "+
                    "and time range</documentation>");
            }
            pw.println("    </dataset>");
            pw.println("</catalog>");
        } else if (IddMethods.p_html_i.matcher(returns).find()) {
            pw.println("</html>");
        } else if (IddMethods.p_xml_i.matcher(returns).find()) {
            pw.println("</reports>");
        }

        } catch (Throwable t) {
           ServletUtil.handleException(t, res);
        }
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1);
    } // end doGet

    private void processDS( ArrayList files, String day, String returns, String station, Boolean latest,
                            PrintWriter pw ) {

        Collections.sort(files, new CompareKeyDescend());
        boolean firstTime = true;

        // write out data/datasets element is latest first order
        for( int t = 0; t < files.size(); t++ ) {
            if (IddMethods.p_html_i.matcher(returns).find()) {
                if (firstTime) {
                    pw.println("<h3>Report(s) for station " + station + "</h3>");
                    firstTime = false;
                }
                //pw.print( "<p><a href=\""+ serviceBase + station +"/"+
                //   DAYS[ i ] +"/"+ files.get( t ) +"\">" );
                 pw.println( files.get( t ) + "</a></p>\n");
            } else if (returns.equals( "catalog" )) {
                catalogOut(  (String)files.get( t ), station, day, pw );
            } else if (IddMethods.p_ascii_i.matcher(returns).find()) {
                    pw.println(files.get( t ));
            } else if (returns.equals( "data" )) {
               //pw.println( "data for file"+ files.get( t ));
               //File file = new File( (String)files.get( t ) );
               //ServletUtil.returnFile(this, req, res, file, null);
            }
            if ( latest ) {
                break;
            }
        }
        return;
     }

    // create a dataset entry for a catalog
    public void catalogOut(String product, String stn, String day, PrintWriter pw ) {

        pw.println("      <dataset name=\""+ product +"\" ID=\""+
           product.hashCode() +"\"" );

        String pDate = im.getObTimeISO( product );

        pw.print("        urlPath=\"");
        pw.println( stn +"/"+ day +"/"+ product +"\">" );
        pw.println( "        <date type=\"start of ob\">"+ pDate +"</date>" );
        pw.println( "      </dataset>" );

    } // end catalogOut

    private void wants(HttpServletResponse res, boolean wantXml, boolean wantStationXml,
                       boolean wantDQC, PrintWriter pw) throws IOException {
      String infoString = null;

      if (wantXml) {
        //Document doc = soc.getDoc();
        //XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        //infoString = fmt.outputString(doc);

      } else if (wantStationXml) {
        Document doc = makeStationDocument();
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(doc);

      } else if (wantDQC) {
            //pw.println(radarLevel2DQC);
            BufferedReader br = im.getInputStreamReader(radarLevel2DQC);
            String input = "";
            while ((input = br.readLine()) != null) {
                pw.println(input);
            }
            br.close();
            return;
      } else {
        //InputStream xslt = getXSLT("ncssRadarLevel2.xsl");
        InputStream xslt = im.getInputStream(contentPath + getPath() + "ncssRadarLevel2.xsl", RadarLevel2Servlet.class);
        Document doc = getDoc();

        try {
          XSLTransformer transformer = new XSLTransformer(xslt);
          Document html = transformer.transform(doc);
          XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
          infoString = fmt.outputString(html);

        } catch (Exception e) {
          log.error("SobsServlet internal error", e);
          ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SobsServlet internal error");
          return;
        }
      }

      //s.setContentLength(infoString.length());
      if (wantXml || wantStationXml)
        res.setContentType("text/xml; charset=iso-8859-1");
      else
        res.setContentType("text/html; charset=iso-8859-1");

      pw.println( infoString );

      ServletUtil.logServerAccess(HttpServletResponse.SC_OK, infoString.length());
    }

 public Document getDoc() throws IOException {

    Element root = new Element("RadarLevel2");
    Document doc = new Document(root);
    //Element root = doc.getRootElement();

    // fix the location
    root.setAttribute("location", "/thredds/idd/radarLevel2");

    // spatial range
    Element LatLonBox = new Element("LatLonBox");
    LatLonBox.addContent( new Element("north").addContent( "75.0000"));
    LatLonBox.addContent( new Element("south").addContent( "19.0000"));
    LatLonBox.addContent( new Element("east").addContent( "-75.0000"));
    LatLonBox.addContent( new Element("west").addContent( "-175.0000"));
    root.addContent( LatLonBox ); 

    // fix the time range
    Element timeSpan = new Element("TimeSpan");
    //Element timeSpan = root.getChild("TimeSpan");
    //timeSpan.removeContent();
    DateFormatter format = new DateFormatter();
    Calendar now = Calendar.getInstance();
    Date end =   now.getTime();
    now.add( Calendar.HOUR, -120 );
    Date start =   now.getTime();

    timeSpan.addContent(new Element("begin").addContent(format.toDateTimeStringISO(start)));
    //timeSpan.addContent(new Element("end").addContent(isRealtime ? "present" : format.toDateTimeStringISO(end)));
    timeSpan.addContent(new Element("end").addContent( "present" ));
    root.addContent( timeSpan );
    // add pointer to the station list XML
    Element stnList = new Element("stationList");
    stnList.setAttribute("title", "Available Stations", XMLEntityResolver.xlinkNS);
    stnList.setAttribute("href", "/thredds/idd/radarLevel2/stations.xml", XMLEntityResolver.xlinkNS);  // LOOK kludge
    root.addContent(stnList);

    // add accept list
    Element a = new Element("AcceptList");
    //elem.addContent(new Element("accept").addContent("raw"));
    a.addContent(new Element("accept").addContent("xml"));
    //elem.addContent(new Element("accept").addContent("csv"));
    //elem.addContent(new Element("accept").addContent("netcdf"));
    root.addContent( a );

    return doc;
  }
    
  private InputStream getXSLT(String xslName) {
      return getClass().getResourceAsStream("/resources/xsl/" + xslName);
  }
  /**
   * Create an XML document from this info
   */
  public Document makeStationDocument() throws IOException {
      Element rootElem = new Element("RadarLevel2Stations");
      Document doc = new Document(rootElem);

      List<Station> stns = im.getStationList();
      //System.out.println("nstns = "+stns.size());
      //for (int i = 0; i < stns.size(); i++) {
      for (Station s : stns) {
        Element sElem = new Element("station");
        sElem.setAttribute("name",s.getValue());
        //if (s.getWmoId() != null)
        //  sElem.setAttribute("wmo_id",s.getWmoId());
        if (s.getName() != null)
          sElem.addContent(new Element("description").addContent(s.getName()));

        sElem.addContent(new Element("longitude").addContent( ucar.unidata.util.Format.d(s.getLongitude(), 6)));
        sElem.addContent(new Element("latitide").addContent( ucar.unidata.util.Format.d(s.getLatitude(), 6)));
       //if (!Double.isNaN(s.getAltitude()))
       //   sElem.addContent(new Element("altitude").addContent( ucar.unidata.util.Format.d(s.getAltitude(), 6)));
        rootElem.addContent(sElem);
      }

      return doc;
    }

    // create xml tags for the reports parameters
    public void outputData(String fileName, PrintWriter pw) {
    //File file = getFile( req);
    //ServletUtil.returnFile(this, req, res, file, null);

    } // end outputData

    // get configurations from ThreddsIDD.cfg file
    public void getConfigurations(String stopAt, PrintWriter pw) {

        Pattern p_stop = Pattern.compile(stopAt);
        Matcher m;
        String variable, value;

        try {
            //pw.println(contentPath + getPath() +"ThreddsIDD.cfg" );
            configurations = new HashMap();
            BufferedReader br = im.getInputStreamReader(contentPath + getPath() +"ThreddsIDD.cfg");
            String input = "";
            while ((input = br.readLine()) != null) {

                // skip lines starting with #
                if (IddMethods.p_B_pound.matcher(input).find() || input.length() == 0)
                    continue;

                // stop at end of requested section
                if (p_stop.matcher(input).find())
                    break;
                m = IddMethods.p_config.matcher(input);
                if (m.find()) {
                    variable = m.group(1);
                    // trim trailing spaces
                    value = IddMethods.p_spaces.matcher(m.group(2)).replaceAll("");
                    configurations.put( variable, value);
                } else {
                    pw.println("error in configuration line parse");
                }
            }
        br.close();
        return;
        } catch ( IOException ioe ) {
            
        }
    } // end getConfigurations

    protected class CompareKeyDescend implements Comparator {

        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;

            return s2.compareTo(s1);
        }
    }

} // end RadarLevel2Servlet
