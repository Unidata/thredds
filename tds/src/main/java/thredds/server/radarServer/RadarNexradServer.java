/* class RadarNexradServer
 *
 * Library for the RadarNexrad Server
 *
 * Finds the files for a particular station, product and time.
 * Outputs either html, or xml catalog files.
 *
 * By:  Robb Kambic  09/25/2007
 *
 */

package thredds.server.radarServer;

import thredds.servlet.*;
import java.io.*;
import java.util.*;
import java.util.Calendar;
import javax.servlet.*;
import javax.servlet.http.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import thredds.server.ncSubset.QueryParams;
import thredds.catalog.XMLEntityResolver;
import thredds.catalog.query.Station;
import org.jdom.Document;
import org.jdom.Element;
import ucar.nc2.units.DateFormatter;

public class RadarNexradServer {

    static public String radarStations = "RadarNexradStations.xml";
    static public List<Station> stationList = null;
    static public HashMap<String, Station> stationMap;

    static public ArrayList<String> allVars = new ArrayList();

    //private boolean allow = false;
    protected ServerMethods sm;
    private boolean debug = false;
    private PrintWriter pw = null;
    private org.slf4j.Logger log;

    public RadarNexradServer( ) {}

    public RadarNexradServer( String contentPath, org.slf4j.Logger log ) {
       this.log = log;
       sm = new  ServerMethods( log );
       if( stationList == null ) {
           stationList = sm.getStations( contentPath + getPath() + radarStations );
           if( stationList == null ) {
               log.error( "Station initialization problem using "+
                       contentPath + getPath() + radarStations );
               return;
           }
           stationMap = sm.getStationMap( stationList );
           // should be a more efficient way
           allVars.add( "DPA");
           allVars.add( "DHR");
           allVars.add( "N1P");
           allVars.add( "N0R");
           allVars.add( "N1R");
           allVars.add( "N2R");
           allVars.add( "N3R");
           allVars.add( "N0S");
           allVars.add( "N1S");
           allVars.add( "N2S");
           allVars.add( "N3S");
           allVars.add( "N0V");
           allVars.add( "N1V");
           allVars.add( "N0Z");
           allVars.add( "NCR");
           allVars.add( "NET");
           allVars.add( "NMD");
           allVars.add( "NTP");
           allVars.add( "NVL");
           allVars.add( "NVW");
           allVars.add( "BREF1");// old vars naming conventions
           allVars.add( "BREF2");
           allVars.add( "BREF248");
           allVars.add( "BREF3");
           allVars.add( "BREF4");
           allVars.add( "LREF1");
           allVars.add( "LREF2");
           allVars.add( "LREF3");
           allVars.add( "CREF");
           allVars.add( "BVEL1");
           allVars.add( "BVEL2");
           allVars.add( "VEL1");
           allVars.add( "VEL2");
           allVars.add( "VEL3");
           allVars.add( "VEL4");
           allVars.add( "LREF1");
           allVars.add( "PRECIP1");
           allVars.add( "PRECIPTOT");
           allVars.add( "SRMV1");
           allVars.add( "SRMV2");
           allVars.add( "SRVEL1");
           allVars.add( "SRVEL2");
           allVars.add( "SRVEL3");
           allVars.add( "SRVEL4");
           allVars.add( "TOPS");
           allVars.add( "VIL");
           allVars.add( "PRE1");
           allVars.add( "PRET");
           allVars.add( "PREA");
           allVars.add( "VAD");
        }
    }

    public Document stationsXML( Document doc, Element rootElem, String path ) {
        // station in this dataset, set by path
        String[] stations = stationsDS( RadarServer.dataLocation.get(path ));
        if( path.contains( "level3") && stations[ 0 ].length() == 4 ) {
            for( int i = 0; i < stations.length; i++ )
                 stations[ i ] = stations[ i ].substring( 1 );
        }
        doc = makeStationDocument( doc, rootElem, stations );
        return doc;
    }

   // must end with "/"
   protected String getPath() {
     return "servers/";
   }

// process Nexrad query get parmameters from servlet call
public void radarNexradQuery(HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {

    String accept;
    String serviceName = "OPENDAP";
    String serviceType = "OPENDAP";
    String serviceBase = "/thredds/dodsC/";
    String radarDir = null;

    try {
        if (debug) System.out.println(req.getQueryString());
        sm.setPW( pw );

        // need to extract data according to the (dataset) given
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";
        if( pathInfo.startsWith( "/"))
                pathInfo = pathInfo.substring( 1 );
        Boolean level2 = pathInfo.contains( "level2");
        radarDir = RadarServer.dataLocation.get( pathInfo );
        if ( radarDir == null)
            radarDir = RadarServer.dataLocation.get( "nexrad/level2/IDD" ); // default

        // parse the input
        QueryParams qp = new QueryParams();
        if (!qp.parseQuery(req, res, new String[]{ QueryParams.XML, QueryParams.CSV, QueryParams.RAW, QueryParams.NETCDF}))
          return; // has sent the error message


        if (qp.hasBB) {
          qp.stns = sm.getStationNames(qp.getBB(), stationList );
          if( ! level2 )
              qp.stns = sm.convert4to3stations( qp.stns );
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
          if( ! level2 )
              qp.stns = sm.convert4to3stations( qp.stns );
        } else if (qp.fatal) {
          qp.errs.append("<documentation>ERROR: No valid stations specified</documentation>\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }

        //boolean useAllStations = (!qp.hasBB && !qp.hasStns && !qp.hasLatlonPoint);
        boolean useAllStations = ( qp.stns.get( 0 ).toUpperCase().equals( "ALL"));
        if (useAllStations) {
          qp.stns = sm.getStationNames( stationList ); //need station names
          if( ! level2 )
              qp.stns = sm.convert4to3stations( qp.stns );
        }

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

            if( ! level2 && qp.vars != null ) { // add vars
                pw.print("var=");
                for (int i = 0; i < qp.vars.size(); i++ ) {
                    pw.print( qp.vars.get( i ) );
                    if( i < qp.vars.size() -1 )
                        pw.print( "," );
                }
                pw.print("&amp;");
            } else if( level2 ) {
                qp.vars = null; // level2 can't select vars
            }

            if( useAllStations ) {
                pw.print("stn=ALL&amp;");
            } else if (qp.hasStns && ! sm.isStationListEmpty(qp.stns, stationMap )) {
                for (String station : qp.stns) {
                    pw.print("stn=" + station +"&amp;");
                }
            } else if (qp.hasBB) {

                pw.print("south="+ qp.south +"&amp;north="+ qp.north +"&amp;" );
                pw.print("west="+ qp.west +"&amp;east="+ qp.east +"&amp;" );
            }

            if( qp.hasDateRange ) {
                pw.println("time_start=" + qp.time_start.toDateTimeStringISO()
                        +"&amp;time_end=" + qp.time_end.toDateTimeStringISO() +"\">");
            } else if( latest ) {
                pw.println("time=present\">");
                //pw.println("\">");
            } else if( qp.hasTimePoint ) {
                pw.println("time=" + qp.time.toDateTimeStringISO() +"\">");
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
            pw.println("<Head><Title>THREDDS RadarNexrad Server</Title></Head>");
            pw.println("<body bgcolor=\"lightblue\" link=\"red\" alink=\"red\" vlink=\"red\">");
            pw.println("<center><H1>RadarNexrad Selection Results</H1></center>");

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
        // at this point must have stations
        if (  sm.isStationListEmpty(qp.stns, stationMap )) {
            pw.println("      <documentation>No data available for station(s) "+
                "and time range</documentation>");
            pw.println("    </dataset>");
            pw.println("</catalog>");
            return;
        }

        // qualifies products according to stations, time, and variables
        boolean dataFound = processQuery( radarDir, qp, res );

        // add ending tags
        if ( ServerMethods.p_xml_i.matcher(accept).find() ) {
            if (! dataFound ) {
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
    } // end radarNexradQuery

    // processQuery is limited by the stns, dates and vars in the query
    protected Boolean processQuery( String tdir, QueryParams qp, HttpServletResponse res )
        throws IOException {

        // set date info
        String yyyymmddStart = qp.time_start.toDateString();
        yyyymmddStart = yyyymmddStart.replace( "-", "");
        String yyyymmddEnd = qp.time_end.toDateString();
        yyyymmddEnd = yyyymmddEnd.replace( "-", "");
        String dateStart =  yyyymmddStart +"_"+ sm.hhmm( qp.time_start.toDateTimeString() );
        String dateEnd =  yyyymmddEnd +"_"+ sm.hhmm( qp.time_end.toDateTimeString() );

        Boolean dataFound = false;

        try { // could have null pointer exceptions on dirs & checks

        // top dir has to point to stns, vars, or date dir
        File files = new File( tdir );
        String[] tdirs = files.list();
        if( tdirs == null )
            return false;

        // decide if directory contains stns, dates or vars, only one true
        Boolean isStns = false;
        if( tdirs[ 0 ].length() == 3 ) {
            isStns = ( stationMap.get( "K"+ tdirs[ 0 ] ) != null );
        } else {
            isStns = ( stationMap.get( tdirs[ 0 ] ) != null );
        }
        Boolean isDates = sm.p_yyyymmdd.matcher(tdirs[ 0 ]).find();
        Boolean isVars = allVars.contains( tdirs[ 0 ].toUpperCase() );

        if( ! ( isStns || isDates || isVars ) )
            return false;  // invalid request

        if( isStns ) {
            // limit stations to the ones in the query
            for (String station : qp.stns ) {
                String sDir = tdir +'/'+ station ;
                files = new File( sDir );
                String[] sdirs = files.list();
                if( sdirs == null)
                    continue;
                // need to check next dirs for products, dates or vars
                File file = new File( sDir +"/"+ sdirs[ 0 ] );
                if( file.isFile() ) { // products in dir, process dir
                    dataFound = sdirs.length > 0;
                    processProducts( sdirs, sDir.replaceFirst( tdir, "").substring( 1 ),
                        qp.hasTimePoint, dateStart, dateEnd );
                } else if( sm.p_yyyymmdd.matcher(sdirs[ 0 ]).find() ) { //dates
                    java.util.Arrays.sort( sdirs, new CompareKeyDescend() );
                    for( int j = 0; j < sdirs.length; j++) {
                        if( sm.isValidDay( sdirs[ j ],  yyyymmddStart, yyyymmddEnd ) ) {
                            // valid date
                            // check for products or vars
                            String dDir = sDir +"/"+ sdirs[ j ];
                            files = new File( dDir );
                            String[] ndirs = files.list();
                            file = new File( dDir +"/"+ ndirs[ 0 ]);
                            if( file.isFile() ) { // products in dir, process dir
                                dataFound = ndirs.length > 0;
                                processProducts( ndirs, dDir.replaceFirst( tdir, "").substring( 1 ),
                                    qp.hasTimePoint, dateStart, dateEnd );
                                if( qp.hasTimePoint ) // only want one product
                                    break;
                            } else if( allVars.contains( ndirs[ 0 ].toUpperCase() ) ) {
                                // not implemented, doesn't make sense stn/date/vars
                            }
                        }
                    }
                } else if( allVars.contains( sdirs[ 0 ].toUpperCase() ) ) { // variable
                    if( qp.vars == null )
                        return false;
                    for (String variable : qp.vars ) {
                        String vDir = sDir +'/'+ variable ;
                        files = new File( vDir );
                        String[] vdirs = files.list();
                        // need to check next dirs for products, dates
                        file = new File( vDir +"/"+ vdirs[ 0 ] );
                        if( file.isFile() ) { // products in dir, return dir
                            //dirTree.add( vDir );
                            dataFound = vdirs.length > 0;
                            processProducts( vdirs, vDir.replaceFirst( tdir, "").substring( 1 ),
                                qp.hasTimePoint, dateStart, dateEnd );

                        }
                    }
                }
            }
        } else if( isDates ) {
            // limit dates to yyyymmddStart and yyyymmddEnd from query
            // need to check next dirs for stns and vars
        } else if( isVars ) {
            if( qp.vars == null )
                return false;
            // limit vars to ones in query
            for (String variable : qp.vars ) {
                String vDir = tdir +'/'+ variable ;
                files = new File( vDir );
                String[] vdirs = files.list();
                // need to check next dirs for products, dates or stations
                File file = new File( vDir +"/"+ vdirs[ 0 ] );
                if( file.isFile() ) { // products in dir, process dir
                    dataFound = vdirs.length > 0;
                    processProducts( vdirs, vDir.replaceFirst( tdir, "").substring( 1 ),
                        qp.hasTimePoint, dateStart, dateEnd );
                } else if( stationMap.get( "K"+ vdirs[ 0 ] ) != null ) { // got to be level3 station
                    for (String station : qp.stns ) {
                        String sDir = vDir +'/'+ station ;
                        files = new File( sDir );
                        String[] sdirs = files.list();
                        if( sdirs == null)
                            continue;
                        // need to check next dirs for products, dates
                        file = new File( sDir +"/"+ sdirs[ 0 ] );
                        if( file.isFile() ) { // products in dir, return dir
                            dataFound = sdirs.length > 0;
                            processProducts( sdirs, sDir.replaceFirst( tdir, "").substring( 1 ),
                                qp.hasTimePoint, dateStart, dateEnd );
                        } else if( sm.p_yyyymmdd.matcher(sdirs[ 0 ]).find() ) { //dates
                            java.util.Arrays.sort( sdirs, new CompareKeyDescend() );
                            for( int k = 0; k < sdirs.length; k++) {
                                if( sm.isValidDay( sdirs[ k ],  yyyymmddStart, yyyymmddEnd ) ) {
                                    // valid date
                                    String dDir = sDir +"/"+ sdirs[ k ];
                                    files = new File( dDir );
                                    String[] ddirs = files.list();
                                    dataFound = ddirs.length > 0;
                                    processProducts( ddirs, dDir.replaceFirst( tdir, "").substring( 1 ),
                                        qp.hasTimePoint, dateStart, dateEnd );
                                    if( qp.hasTimePoint ) // only want one product
                                        break;
                                }    
                            }
                        }
                    }
                }
            }
        } else {
            return dataFound; // invalid query
        }
        return dataFound;

        } catch ( Exception e ) {
            //log.error("radarServer processQuery error" );
            ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "radarServer processQuery error");
            //pw.println( "<documentation>\n" );
            //pw.println( "Query can't be satisfied :"+ qp.toString() +"\n" );
            //pw.println( "</documentation>\n" );
            return dataFound; // partial or invalid query
        }

    }

    // check if product has valid time then creates a dataset for product
    protected void processProducts( String[] products, String rPath,
        Boolean latest, String dateStart, String dateEnd ) {

        java.util.Arrays.sort( products, new CompareKeyDescend() );
        
        // write out products with latest first order
        for( int t = 0; t < products.length; t++ ) {
            if( products[ t ].startsWith( "." ) )
                continue;
            if( ! sm.isValidDate( products[ t ], dateStart, dateEnd ) )
                continue;
            datasetOut(  products[ t ], rPath );
            if ( latest ) {
                break;
            }
         }
         return;
    }

    // create a dataset entry for a catalog
    public void datasetOut(String product, String rPath ) {

           pw.println("      <dataset name=\""+ product +"\" ID=\""+
              product.hashCode() +"\"" );

           String pDate = sm.getObTimeISO( product );

           pw.print("        urlPath=\"");
           pw.println(  rPath +"/"+ product +"\">" );
           pw.println( "        <date type=\"start of ob\">"+ pDate +"</date>" );
           pw.println( "      </dataset>" );

       } // end datasetOut

    /*   save for example sxlt process
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
        //InputStream xslt = getXSLT("ncssRadarNexrad.xsl");
        InputStream xslt = sm.getInputStream(contentPath + getPath() + "ncssRadarNexrad.xsl", RadarNexradServer.class);
        Document doc = getDoc();

        try {
          XSLTransformer transformer = new XSLTransformer(xslt);
          Document html = transformer.transform(doc);
          XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
          infoString = fmt.outputString(html);

        } catch (Exception e) {
          //log.error("RadarServer internal error", e);
          ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "RadarNexradServer internal error");
          return;
        }
      }

      pw.println( infoString );

    }
    */

 public Document getDoc() throws IOException {

    Element root = new Element("RadarNexrad");
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
    stnList.setAttribute("href", "/thredds/idd/radarNexrad/stations.xml", XMLEntityResolver.xlinkNS);  // LOOK kludge
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
  public Document makeStationDocument( Document doc, Element rootElem, String[] stations ) {

      //System.out.println("nstns = "+stations.size());
      for (String s : stations ) {
         Station stn;
         if( s.length() == 3 ) { // level3 station
            stn = stationMap.get( "K"+ s );
         } else {
            stn = stationMap.get( s );
         }

         Element sElem = new Element("station");
         if( stn == null ) { // stn not in table
             sElem.setAttribute("id", s );
             sElem.setAttribute("state", "XXX");
             sElem.setAttribute("country", "XX");
             sElem.addContent(new Element("name").addContent("Unknown"));
             sElem.addContent(new Element("longitude").addContent( "0.0" ));
             sElem.addContent(new Element("latitude").addContent( "0.0" ));
             sElem.addContent(new Element("elevation").addContent( "0" ));
             rootElem.addContent(sElem);
             continue;
         }
         sElem.setAttribute("id",s );
         if( stn.getState() != null )
             sElem.setAttribute("state",stn.getState());
         if( stn.getCountry() != null )
             sElem.setAttribute("country",stn.getCountry());
         //if (s.getWmoId() != null)
         //  sElem.setAttribute("wmo_id",s.getWmoId());
         if (stn.getName() != null)
            sElem.addContent(new Element("name").addContent(stn.getName()));

         sElem.addContent(new Element("longitude").addContent( ucar.unidata.util.Format.d(stn.getLocation().getLongitude(), 6)));
         sElem.addContent(new Element("latitude").addContent( ucar.unidata.util.Format.d(stn.getLocation().getLatitude(), 6)));
         if (!Double.isNaN(stn.getLocation().getElevation()))
            sElem.addContent(new Element("elevation").addContent( ucar.unidata.util.Format.d(stn.getLocation().getElevation(), 6)));
         rootElem.addContent(sElem);
      }

      return doc;
    }

    public String[] stationsDS( String path ) {
        String[] stations = null;

        if( path != null ) {
            File files = new File( path );
            stations = files.list();
            // actually not a station, it's a var so get next dir down
            if( stations[ 0 ].length() == 3 && allVars.contains( stations[ 0 ].toUpperCase() ) ) {
                //path += "/N0R";
                path += "/"+ stations[ 0 ];
                files = new File( path );
                stations = files.list();
            }
        }
        if( stations ==  null || stations.length == 0 ) {
            stations = new String[ 1 ];
            stations =   stationMap.keySet().toArray( stations );
        }
        return stations;
   }

    /**
     * print station in a XML format from this info
    */
    public void printStations( String[] stations ) {
        for (String s : stations ) {
            Station stn;
            if( s.length() == 3 ) { // level3 stns
                stn = stationMap.get( "K"+ s );
            } else {
                stn = stationMap.get( s );
            }
            if(  stn == null ) {
                pw.println( "   <station id=\""+ s +"\" state=\"XX\" country=\"XX\">");
                pw.println( "      <name>Unknown</name>");
                pw.println( "      <latitude>0.0</latitude>");
                pw.println( "      <longitude>0.0</longitude>");
                pw.println( "      <elevation>0.0</elevation>");
                pw.println( "   </station>");
                continue;
            }
            pw.println( "   <station id=\""+ s +"\" state=\""+ stn.getState()
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

} // end RadarNexradServer
