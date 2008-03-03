/* class RadarLevel2Server
 *
 * Library for the RadarLevel2 Server
 *
 * Finds the near realtime product files for a particular station and time.
 * Outputs either html, dqc  or catalog files.
 *
 * By:  Robb Kambic  09/25/2007
 *
 */

package thredds.server.radarServer;

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
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
//import thredds.datatype.TimeDuration;
import thredds.server.ncSubset.QueryParams;
import thredds.catalog.XMLEntityResolver;
import thredds.catalog.query.Station;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.point.StationObsDatasetInfo;
import ucar.nc2.units.DateFormatter;

public class RadarLevel2Server {

    //static public StringBuilder catalog = null;
    //static public HashMap<String,String> dataPath = new HashMap();
    //static public HashMap<String,String> dataLocation = new HashMap();
    static public List<Station> stationList = null;
    static public HashMap<String, Station> stationMap;
    //static public String radarLevel2DQC = "RadarLevel2DQC.xml";
    static public String radarStations = "RadarNexradStations.xml";

    //private boolean allow = false;
    protected ServerMethods sm;
    private boolean debug = false;
    //private String contentPath = null;
    private PrintWriter pw = null;

    public RadarLevel2Server( ) {}

    public RadarLevel2Server( String contentPath ) {
       //this.contentPath = contentPath;
       sm = new  ServerMethods();
       if( stationList == null ) {
           stationList = sm.getStations( contentPath + getPath() + radarStations );
           stationMap = sm.getStationMap( stationList );
       }
    }

    public Document stationsXML( Document doc, Element rootElem, String path ) {
        // station in this dataset, set by path
        String[] stations = stationsDS( path );
        doc = makeStationDocument( doc, rootElem, stations );
        return doc;
    }

   // must end with "/"
   protected String getPath() {
     return "servers/";
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

// process level2 query get parmameters from servlet call
public void radarLevel2Query(HttpServletRequest req, HttpServletResponse res, String radarDir)
            throws ServletException, IOException {

    // servlet parameters
    //String[] PRODS;
    String accept;
    String serviceName = "OPENDAP";
    String serviceType = "OPENDAP";
    String serviceBase = "/thredds/dodsC";
    Date start = null, end = null; // never initialized ?

    try {
        //ServletUtil.logServerAccessSetup( req );
        if (debug) System.out.println(req.getQueryString());
        sm.setPW( pw );

        // need to extract data according to the (dataset) given
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";
        Boolean level2 = pathInfo.contains( "level2");
        if ( radarDir == null)
            radarDir = RadarServer.dataLocation.get( "nexrad/level2/IDD" ); // default

        String dirStructure = "STN/DAY"; //default needs how to set

        // parse the input
        QueryParams qp = new QueryParams();
        if (!qp.parseQuery(req, res, new String[]{ QueryParams.XML, QueryParams.CSV, QueryParams.RAW, QueryParams.NETCDF}))
          return; // has sent the error message


        if (qp.hasBB) {
          qp.stns = sm.getStationNames(qp.getBB(), stationList );
          if (qp.stns.size() == 0) {
            qp.errs.append("<documentation>ERROR: Bounding Box contains no stations</documentation>\n");
            qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
            return;
          }
        }

        if (qp.hasStns && sm.isStationListEmpty(qp.stns, stationMap )) {
          qp.errs.append("<documentation>ERROR: No valid stations specified</documentation>\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }

        if (qp.hasLatlonPoint) {
          qp.stns = new ArrayList<String>();
          qp.stns.add( sm.findClosestStation(qp.lat, qp.lon, stationList ));
        } else if (qp.fatal) {
          qp.errs.append("<documentation>ERROR: No valid stations specified</documentation>\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }

        boolean useAllStations = (!qp.hasBB && !qp.hasStns && !qp.hasLatlonPoint);
        if (useAllStations)
          qp.stns = sm.getStationNames( stationList ); //need station names
          //qp.stns = new ArrayList<String>(); // empty list denotes all

        /*
        if (qp.hasTimePoint && ( sm.filterDataset(qp.time) == null)) {
          qp.errs.append("<documentation>ERROR: This dataset does not contain the time point= " + qp.time + " </documentation>\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
        */

        /*
        // needs work start and end aren't set, too expensive to set
        if (qp.hasDateRange) {
          DateRange dr = qp.getDateRange();
          if (! sm.intersect(dr, start, end)) {
            qp.errs.append("<documentation>ERROR: This dataset does not contain the time range= " + qp.time + " </documentation>\n");
            qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
            return;
          }
        }
        */

        /*
        if (useAllStations && useAllTimes) {
          qp.errs.append("<documentation>ERROR: You must subset by space or time</documentation>\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
        */
        // if time in not set, return all possible times
        boolean latest = false;
        if( qp.hasTimePoint ) {
            if( qp.time.isPresent() ) {
                latest = true;
                try {
                    qp.time_end = new DateType( "present", null, null);
                    qp.time_start = new DateType(ServerMethods.epic, null, null);
                } catch (java.text.ParseException e) {
                    qp.errs.append("Illegal param= 'time' must be valid ISO Duration\n");
                }
            } else {
                qp.time_end = qp.time;
                qp.time_start = qp.time;
            }
        } else if( qp.hasDateRange ) {
            DateRange dr = qp.getDateRange();
            qp.time_start = dr.getStart();
            qp.time_end = dr.getEnd();
        // return all times available
        } else {
            try {
               qp.time_end = new DateType( "present", null, null);
               qp.time_start = new DateType(ServerMethods.epic, null, null);
           } catch (java.text.ParseException e) {
               qp.errs.append("Illegal param= 'time' must be valid ISO Duration\n");
           }
        }

        // will need for level3 vars
        //PRODS = ServletUtil.getParameterValuesIgnoreCase(req, "PROD");
        if( ! level2 ) {
            //qp.vars

        }

        accept = ServletUtil.getParameterIgnoreCase(req, "accept");
        if (accept == null )  // default
            accept = "xml";

        // set content type default is xml
        //String contentType = qp.acceptType;
        if ( ServerMethods.p_html_i.matcher(accept).find()) { // accept html
            res.setContentType( "text/html");
        }

        serviceBase += pathInfo +"/";

        // write out catalog with datasets
        if ( serviceType.equals( "OPENDAP" )) {
            int level = 2;
            if( ! level2 ) {
                level = 3;
            }
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.print("<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"");
            pw.println(" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Level"+ level +" datasets in near real time\" version=\""+
                    "1.0.1\">");
            pw.println("");
            pw.print("  <service name=\""+ serviceName +"\" serviceType=\""+ serviceType +"\"");
            pw.println(" base=\"" + serviceBase + "\"/>");
            pw.print("    <dataset name=\"RadarLevel"+ level +" datasets for available stations and times\" collectionType=\"TimeSeries\" ID=\""+
 "accept=" + accept + "&amp;");
            if( useAllStations ) {
                pw.print("stn=all&amp;");
            } else if (qp.hasStns && ! sm.isStationListEmpty(qp.stns, stationMap )) {
                for (String station : qp.stns) {
                    pw.print("stn=" + station +"&amp;");
                }
            } else if (qp.hasBB) {

                pw.print("south="+ qp.south +"&amp;north="+ qp.north +"&amp;" );
                pw.print("west="+ qp.west +"&amp;east="+ qp.east +"&amp;" );
            }

            if( qp.hasDateRange ) {
                pw.println("time_start=" + qp.time_start.toDateTimeString()
                        +"&amp;time_end=" + qp.time_end.toDateTimeString() +"\">");
            } else if( latest ) {
                pw.println("time=present\">");
                //pw.println("\">");
            } else if( qp.hasTimePoint ) {
                pw.println("time=" + qp.time.toDateTimeString() +"\">");
            } else {
                pw.println( "\">" );
            }
            pw.println("    <metadata inherited=\"true\">");
            pw.println("      <dataType>Radial</dataType>");
            pw.print("      <dataFormat>" );
            if( level2 ) {
                pw.print( "NEXRAD2" );
            } else {
                pw.print( "NIDS" );
            }
            pw.println( "</dataFormat>");
            pw.println("      <serviceName>" + serviceName + "</serviceName>");
            pw.println("    </metadata>");
            pw.println();

        } else if ( ServerMethods.p_html_i.matcher(accept).find()) { // accept html
            pw.println("<Head><Title>THREDDS RadarLevel2 Server</Title></Head>");
            pw.println("<body bgcolor=\"lightblue\" link=\"red\" alink=\"red\" vlink=\"red\">");
            pw.println("<center><H1>RadarLevel2 Selection Results</H1></center>");

        } else if ( ServerMethods.p_ascii_i.matcher(accept).find()) {

        } else if ( ServerMethods.p_xml_i.matcher(accept).find()) {
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
        if (  sm.isStationListEmpty(qp.stns, stationMap )) {
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
        String dateStart =  yyyymmddStart +"_"+ sm.hhmm( qp.time_start.toDateTimeString() );
        String dateEnd =  yyyymmddEnd +"_"+ sm.hhmm( qp.time_end.toDateTimeString() );

        boolean dataFound = false; // checks if any data found
        boolean done = false; // used for early exit when all data not required
        // loop on qp.stns DAYS
        if( dirStructure.startsWith( "STN")) {
            for (String station : qp.stns) {
                done = false;
                // Obtain the days available
                String[] DAYS = sm.getDAYS(radarDir +"/"+ station, pw);
                if (DAYS == null || DAYS.length == 0)
                    continue;
                for (int i = 0; i < DAYS.length && !done; i++) {
                    if( ! sm.isValidDay( DAYS[ i ], yyyymmddStart, yyyymmddEnd ) )
                        continue;

                     ArrayList<String> files = new ArrayList();
                     File dir = new File(radarDir +"/"+ station +"/"+ DAYS[ i ]);
                     String[] FILES = dir.list();
                     for( int t = 0; t < FILES.length; t++ ) {
                         if( ! sm.isValidDate( FILES[ t ], dateStart, dateEnd ) )
                             continue;
                         files.add( FILES[ t ] );

                     }
                     if( files.size() > 0 ) {
                         dataFound = true;
                         done = qp.hasTimePoint;
                         processDS( files, DAYS[ i ], accept, station, latest, pw );
                     }

                }
            }
        // loop on DAY STN
        } else if( dirStructure.startsWith( "DAY")) {
            String[] DAYS = sm.getDAYS(radarDir, pw);
                for (int i = 0; i < DAYS.length; i++) {
                    if( ! sm.isValidDay( DAYS[ i ], yyyymmddStart, yyyymmddEnd ) )
                        continue;
                    for (String station : qp.stns) {
                    //for (int j = 0; j < qp.stns.size(); j++ ) {
                        //String station = qp.stns.get( j );
                        ArrayList<String> files = new ArrayList();
                        File dir = new File(radarDir +"/"+ DAYS[ i ] +"/"+ station);
                        if( !dir.exists() )
                            continue;
                        String[] FILES = dir.list();
                        for( int t = 0; t < FILES.length; t++ ) {
                             if( ! sm.isValidDate( FILES[ t ], dateStart, dateEnd ) )
                                  continue;
                             files.add( FILES[ t ] );

                        }
                        if( files.size() > 0 ) {
                            dataFound = true;
                            done = qp.hasTimePoint;
                            processDS( files, DAYS[ i ], accept, station, latest, pw );
                        }

                    }
                }
        }

        // add ending tags
        if ( ServerMethods.p_xml_i.matcher(accept).find() ) {
            if (! dataFound) {
                pw.println("      <documentation>No data available for station(s) "+
                    "and time range</documentation>");
            }
            pw.println("    </dataset>");
            pw.println("</catalog>");
        } else if ( ServerMethods.p_html_i.matcher(accept).find()) {
            pw.println("</html>");
         }

        } catch (Throwable t) {
           ServletUtil.handleException(t, res);
        }
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1);
    } // end radarLevel2Query

    protected void processDS( ArrayList files, String day, String accept, String station, Boolean latest,
                            PrintWriter pw ) {

        Collections.sort(files, new CompareKeyDescend());
        boolean firstTime = true;

        // write out data/datasets element is latest first order
        for( int t = 0; t < files.size(); t++ ) {
            if ( ServerMethods.p_html_i.matcher(accept).find()) {
                if (firstTime) {
                    pw.println("<h3>Report(s) for station " + station + "</h3>");
                    firstTime = false;
                }
                //pw.print( "<p><a href=\""+ serviceBase + station +"/"+
                //   DAYS[ i ] +"/"+ files.get( t ) +"\">" );
                 pw.println( files.get( t ) + "</a></p>\n");
            } else if (ServerMethods.p_xml_i.matcher(accept).find()) {
                catalogOut(  (String)files.get( t ), station, day, pw );
            } else if ( ServerMethods.p_ascii_i.matcher(accept).find()) {
                    pw.println(files.get( t ));
            } else if (accept.equals( "data" )) {
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

        String pDate = sm.getObTimeISO( product );

        pw.print("        urlPath=\"");
        pw.println( stn +"/"+ day +"/"+ product +"\">" );
        pw.println( "        <date type=\"start of ob\">"+ pDate +"</date>" );
        pw.println( "      </dataset>" );

    } // end catalogOut

    /*
    private void wants(HttpServletResponse res, boolean wantXml, boolean wantStationXml,
                       boolean wantDQC, PrintWriter pw) throws IOException {
      String infoString = null;

      if (wantXml) {
        //Document doc = soc.getDoc();
        //XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        //infoString = fmt.outputString(doc);

      } else if (wantStationXml) {
        //Document doc = makeStationDocument();
        //XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        //infoString = fmt.outputString(doc);

      } else {
        //InputStream xslt = getXSLT("ncssRadarLevel2.xsl");
        InputStream xslt = sm.getInputStream(contentPath + getPath() + "ncssRadarLevel2.xsl", RadarLevel2Server.class);
        Document doc = getDoc();

        try {
          XSLTransformer transformer = new XSLTransformer(xslt);
          Document html = transformer.transform(doc);
          XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
          infoString = fmt.outputString(html);

        } catch (Exception e) {
          //log.error("RadarServer internal error", e);
          ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "RadarLevel2Server internal error");
          return;
        }
      }

      pw.println( infoString );

    }
    */

 public Document getDoc() throws IOException {

    Element root = new Element("RadarLevel2");
    Document doc = new Document(root);
    //Element root = doc.getRootElement();

    // fix the location
    root.setAttribute("location", "/thredds/radarServer/nexrad/level2");

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
    Date startNow =   now.getTime();

    timeSpan.addContent(new Element("begin").addContent(format.toDateTimeStringISO(startNow)));
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

  /**
   * Create an XML document from this info
   */
  public Document makeStationDocument( Document doc, Element rootElem, String[] stns ) {

      //System.out.println("nstns = "+stns.size());
      for (int i = 0; i < stns.length; i++) {
        if( stns[ i ].length() == 3 )
            stns[ i ] = "K"+ stns[ i ];
        Station s = stationMap.get( stns[ i ]);
        Element sElem = new Element("station");
        if( s == null ) { // stn not in table
            sElem.setAttribute("id", stns[ i ] );
            sElem.setAttribute("state", "XXX");
            sElem.setAttribute("country", "XX");
            sElem.addContent(new Element("name").addContent("Unknown"));
            sElem.addContent(new Element("longitude").addContent( "0.0" ));
            sElem.addContent(new Element("latitude").addContent( "0.0" ));
            sElem.addContent(new Element("elevation").addContent( "0" ));
            rootElem.addContent(sElem);
            continue;
        }
        sElem.setAttribute("id",s.getValue());
        if( s.getState() != null )
            sElem.setAttribute("state",s.getState());
        if( s.getCountry() != null )  
            sElem.setAttribute("country",s.getCountry());
        //if (s.getWmoId() != null)
        //  sElem.setAttribute("wmo_id",s.getWmoId());
        if (s.getName() != null)
          sElem.addContent(new Element("name").addContent(s.getName()));

        sElem.addContent(new Element("longitude").addContent( ucar.unidata.util.Format.d(s.getLocation().getLongitude(), 6)));
        sElem.addContent(new Element("latitude").addContent( ucar.unidata.util.Format.d(s.getLocation().getLatitude(), 6)));
        if (!Double.isNaN(s.getLocation().getElevation()))
           sElem.addContent(new Element("elevation").addContent( ucar.unidata.util.Format.d(s.getLocation().getElevation(), 6)));
        rootElem.addContent(sElem);
      }

      return doc;
    }

    public String[] stationsDS( String path ) {
        String[] stations = null;
        /*
        if( path != null ) {
            File files = new File( path );
            stations = files.list();
        }
        */
        if( path != null ) {
            File files = new File( path );
            stations = files.list();
            for( int i = 0; i < stations.length; i++) {
                if( stations[ i ].equals( "N0R")) { // not stns but products
                    path += "/N0R";
                    files = new File( path );
                    stations = files.list();
                    break;
                }
            }
        }
        if( stations ==  null || stations.length == 0 ) {
            stations = new String[ 1 ];
            stations =   stationMap.keySet().toArray( stations );
        }
        return stations;
   }

    /**
     * Create an XML document from this info
     */
    public void printStations( Document doc, String root ) {
        //NodeList d = doc.getElementsByTagName("discipline");
        //Node n = doc.getDocRoot();

    }

    /**
     * print station in a XML format from this info
     */
    public void printStations( String[] stations ) {
        for (String s : stations ) {
            if( s.length() == 3 )
            s = "K"+ s;
            Station stn = stationMap.get( s );
            if(  stn == null ) {
                pw.println( "   <station id=\""+ s +"\" state=\"XX\" country=\"XX\">");
                pw.println( "      <name>Unknown</name>");
                pw.println( "      <latitude>0.0</latitude>");
                pw.println( "      <longitude>0.0</longitude>");
                pw.println( "      <elevation>0.0</elevation>");
                pw.println( "   </station>");
                continue;
            }
            pw.println( "   <station id=\""+ stn.getValue() +"\" state=\""+ stn.getState()
                    +"\" country=\""+ stn.getCountry() +"\">");
            pw.println( "      <name>"+ stn.getName() +"</name>");
            pw.println( "      <latitude>"+
                    ucar.unidata.util.Format.d(stn.getLocation().getLatitude(), 6) +"</latitude>");
            pw.println( "      <longitude>"+
                    ucar.unidata.util.Format.d(stn.getLocation().getLongitude(), 6) +"</longitude>");

           if (!Double.isNaN(stn.getLocation().getElevation()))
               pw.println( "      <elevation>"+
                    ucar.unidata.util.Format.d(stn.getLocation().getElevation(), 6) +"</elevation>");

           pw.println( "   </station>");
        }

    }

    // create xml tags for the reports parameters
    public void outputData(String fileName, PrintWriter pw) {
    //File file = getFile( req);
    //ServletUtil.returnFile(this, req, res, file, null);

    } // end outputData

    public void setPW( PrintWriter pw ){
        this.pw = pw;
    }

    protected class CompareKeyDescend implements Comparator {

        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;

            return s2.compareTo(s1);
        }
    }

} // end RadarLevel2Server
