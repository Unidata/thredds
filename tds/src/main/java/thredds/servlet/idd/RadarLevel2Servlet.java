/* class RadarLevel2Servlet
 *
 * Library for the RadarLevel2 Servlet
 *
 * Finds the near realtime product files for a particular station and time.
 * Outputs either html, ascii, dqc  or catalog files.
 *
 * By:  Robb Kambic  04/25/2005
 *
 */

package thredds.servlet.idd;

import thredds.servlet.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;

public class RadarLevel2Servlet extends LdmServlet {

    // static configuration file ThreddsIDD.cfg parameters
    static protected String catalogVersion;
    static protected String defaultCatalog;
    static protected String backgroundImage;
    static protected String radarLevel2DQC;
    static protected String radarLevel2Dir = null;
    static protected String radarLevel2HTTPServiceName;
    static protected String radarLevel2HTTPServiceType;
    static protected String radarLevel2HTTPServiceBase;
    static protected String radarLevel2HTTPDataFormatType;
    static protected String radarLevel2ASCIIServiceName;
    static protected String radarLevel2ASCIIUrlPath;
    static protected String radarLevel2ASCIIDataFormatType;
    static protected String radarLevel2ADDEServiceName;
    static protected String radarLevel2ADDEUrlPath;
    static protected String radarLevel2DODSServiceName;
    static protected String radarLevel2DODSServiceType;
    static protected String radarLevel2DODSServiceBase;
    //static protected String radarLevel2DODSUrlPath;

    protected long getLastModified(HttpServletRequest req) {
        try { 
            //  get configurations from ThreddsIDD.cfg
            if( radarLevel2Dir == null ) {
                contentPath = ServletUtil.getContentPath(this);
                getConfigurations("synoptic", null);
            }
            File file = new File(  radarLevel2DQC );

            return file.lastModified();
        } catch ( FileNotFoundException fnfe ) {
            return -1;
        } catch ( IOException ioe ) {
            return -1;
        }
    }

    // get parmameters from servlet call
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // servlet parameters
        String[] STNS;
        String[] PRODS;
        String dtime;
        String dateStart = null;
        String dateEnd = null;
        String returns;
        //String ll = null;
        //String ur = null;
        String y0 = null;
        String y1 = null;
        String x0 = null;
        String x1 = null;
        String serviceName = null;
        String serviceType = null;
        String serviceBase = null;
        String urlPath = null;
        PrintWriter pw;

        //ServletUtil.logServerAccessSetup( req );

        try {
            if ( req != null ) {  // parameters from servlet

            ServletUtil.logServerAccessSetup( req );

            STNS = ServletUtil.getParameterValuesIgnoreCase(req, "stn");
            //if (STNS == null) {
            //    STNS = new String[1];
            //    STNS[0] = "KDEN";
            //}

            PRODS = ServletUtil.getParameterValuesIgnoreCase(req, "PROD");
            //if (PRODS == null) {
            //    PRODS = new String[1];
            //    PRODS[0] = "N0R";
            //}

            dtime = ServletUtil.getParameterIgnoreCase(req, "dtime");
            if (dtime == null)
                dtime = "latest";

            dateStart = ServletUtil.getParameterIgnoreCase(req, "dateStart");

            dateEnd = ServletUtil.getParameterIgnoreCase(req, "dateEnd");

            returns = ServletUtil.getParameterIgnoreCase(req, "returns");
            if (returns == null) // default
                returns = "catalog";

            serviceType = ServletUtil.getParameterIgnoreCase(req, "serviceType");
            if (serviceType == null)
                serviceType = "HTTPServer";

            serviceName = ServletUtil.getParameterIgnoreCase(req, "serviceName");

            urlPath = ServletUtil.getParameterIgnoreCase(req, "urlPath");

            //ll = ServletUtil.getParameterIgnoreCase(req, "ll");

            //ur = ServletUtil.getParameterIgnoreCase(req, "ur");

            // bounding box min/max lat  min/max lon
            y0 = ServletUtil.getParameterIgnoreCase(req, "y0");
            y1 = ServletUtil.getParameterIgnoreCase(req, "y1");
            x0 = ServletUtil.getParameterIgnoreCase(req, "x0");
            x1 = ServletUtil.getParameterIgnoreCase(req, "x1");

            // set ContentType
            // requesting a catalog
            if ( ! serviceType.equals( "" ) ||
                (p.p_catalog_i.matcher(returns).find()) ) {
                res.setContentType("text/xml");
            // rest requesting data
            } else if (p.p_xml_i.matcher(returns).find()) {
                res.setContentType("text/xml");
            } else if (p.p_ascii_i.matcher(returns).find()) {
                res.setContentType("text/plain");
            } else if (p.p_qc_or_dqc_i.matcher(returns).find()) {
                res.setContentType("text/xml");
            } else if (p.p_html_i.matcher(returns).find()) {
                res.setContentType("text/html");
            } else {
                res.setContentType("text/plain");
            }
            pw = res.getWriter();
            //pw.println( "pw works" );
            //if( 1 > 0 ) return;
            //pw.println( "returns =" + returns );
            //pw.println( "dtime =" + dtime );
            //for( int i = 0; i < STNS.length; i++ )
                //pw.println( "Station " + i +" = "+ STNS[ i ] );

            contentPath = ServletUtil.getContentPath(this);
            //pw.println( "rootPath =" + ServletUtil.getRootPath( this ) );
            //pw.println( "contentPath =" + contentPath );
            //pw.println( "ThreddsIDD.cfg ="+ contentPath + getPath() +"ThreddsIDD.cfg" );

            //ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1);

            } else { // command line testing here

            contentPath = "/home/rkambic/code/thredds/tds/src/main/initialContent/";
            pw = new PrintWriter(System.out, true);
            pw.println( "command line testing" ) ;

            STNS = new String[2];
            STNS[0] = "KFTG";
            STNS[1] = "KFTG";
            STNS = null;
            dtime = "latest";
            //dtime = "2007-01-05T17:05:00";
            //dtime = "6hour";
            //dtime = "5day";
            //dtime = "1day";
            //dateStart = "2006-12-26T00:12:30";
            //dateEnd = "2006-12-26T12:12:30";

            returns = "catalog";
            //returns = "ascii";
            //returns = "html";
            //returns = "xml";
            //returns = "dqc";
            //returns = "data";

            serviceType = "HTTPServer";
            //serviceType = "";
            serviceType = "OPeNDAP";
            //serviceName = radarLevel2HTTPServiceName;
            //pw.println( "serviceName =" + serviceName );
            //pw.println( "urlPath =" + urlPath );
            //ll = "-15:-90";
            //ur = "15:90";
            y0 = "39";
            y0 = null;
            y1 = "40";
            x0 = "-105";
            x1 = "-100";
        
            } // end command line testing

            //  get configurations from ThreddsIDD.cfg
            if( radarLevel2Dir == null )
               getConfigurations("synoptic", pw);
             //pw.println( "<p>radarLevel2DQC =" + radarLevel2DQC +"</p>");
             //pw.println( "<p>radarLevel2Dir =" + radarLevel2Dir +"</p>");

        if (p.p_qc_or_dqc_i.matcher(returns).find()) { // returns dqc doc

            //if( ll != null && ur != null ) {
                //STNS = boundingBox(ll, ur, radarLevel2DQC, pw);
                //pw.println( "<stations>" );
                //for (int j = 0; j < STNS.length; j++) {
                //    pw.println( "    <station name=\""+ STNS[ j ] +"\" />" );
                //}
                //pw.println( "</stations>" );
            //} else {
      
                //pw.println(radarLevel2DQC);
                BufferedReader br = getInputStreamReader(radarLevel2DQC);
                String input = "";
                while ((input = br.readLine()) != null) {
                    pw.println(input);
                }
                br.close();
            //}
            return;
        }

        // requesting a catalog with different serviceTypes
        if (p.p_HTTPServer_i.matcher(serviceType).find() ) {
                //|| (p.p_catalog_i.matcher(returns).find()) ) { // backward capatiablity
                serviceName = radarLevel2HTTPServiceName;
                serviceType = radarLevel2HTTPServiceType;
                serviceBase = radarLevel2HTTPServiceBase;
                //if (p.p_catalog_i.matcher(returns).find()) {
                //    returns = "data";  // default
                //}
        //  serviceType =~ /OPeNDAP/i
        } else if (p.p_DODS_i.matcher(serviceType).find()) {
                serviceName = radarLevel2DODSServiceName;
                serviceType = radarLevel2DODSServiceType;
                serviceBase = radarLevel2DODSServiceBase;
                //returns = "xml";  // default
        }
        // write out catalog with datasets
        if ( ! serviceType.equals( "" )) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.print("<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"");
            pw.println(" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Level2 datasets in near real time\" version=\""+ catalogVersion +"\">");
            pw.println("");
            pw.print("  <service name=\""+ serviceName +"\" serviceType=\""+ serviceType +"\"");
            pw.println(" base=\"" + serviceBase + "\"/>");
            pw.print("    <dataset name=\"RadarLevel2 datasets for available stations and times\" collectionType=\"TimeSeries\" ID=\"serviceType="+ 
serviceType +"&amp;returns=" + returns + "&amp;");
            if( STNS != null && ! STNS[ 0 ].equals( "all" )) {
                for (int i = 0; i < STNS.length; i++) {
                    pw.print("stn=" + STNS[i] +"&amp;");
                }
            } else if( y0 != null && y1 != null && x0 != null && x1 != null ) {
                pw.print("y0="+ y0 +"&amp;y1="+ y1 +"&amp;" );
                pw.print("x0="+ x0 +"&amp;x1="+ x1 +"&amp;" );
            } else {
                pw.print("stn=all&amp;");
                STNS = boundingBox( "-90", "90", "-180", "180", radarLevel2DQC, null);
                //pw.println(  "STNS.length="+ STNS.length +" ="+ STNS[ 0 ]);
            }

            if( dtime != null ) {
                pw.println("dtime=" + dtime + "\">");
            } else if( dateEnd != null ) {
                pw.println("dateStart=" + dateStart +"&amp;dateEnd=" +dateEnd +"\">");
            } else {
                pw.println("dtime=latest\">");
                dtime = "latest";  // default
            }
            pw.println("    <metadata inherited=\"true\">");
            pw.println("      <dataType>Station</dataType>");
            pw.println("      <dataFormat>" + returns + "</dataFormat>");
            pw.println("      <serviceName>" + serviceName + "</serviceName>");
            pw.println("    </metadata>");
            pw.println();

        } else if (p.p_html_i.matcher(returns).find()) { // returns html
            serviceBase = "/thredds/fileServer/nexrad/level2/";

            pw.println("<Head><Title>THREDDS RadarLevel2 Server</Title></Head>");
            pw.println("<body bgcolor=\"lightblue\" link=\"red\" alink=\"red\" vlink=\"red\">");
            pw.println("<center><H1>RadarLevel2 Selection Results</H1></center>");

        } else if (p.p_ascii_i.matcher(returns).find()) {

        } else if (p.p_xml_i.matcher(returns).find()) {

            pw.println("<reports>");

        } 
//
//
//
//
//
// main code body, no configurations below this line
//
        // use bounding box given to determine stations
        //pw.println( "<p>minLat="+ y0 +" minLon="+ x0 +" maxLat="+ y1 +" maxLon="+ x1 +"</p>" );
        if( y0 != null && y1 != null && x0 != null && x1 != null ) {
            //STNS = boundingBox( y0, y1, x0, x1, radarLevel2DQC, pw);
            STNS = boundingBox( y0, y1, x0, x1, radarLevel2DQC, null);
            //pw.println(  "STNS.length="+ STNS.length +" ="+ STNS[ 0 ]);
        } //end bounding box

        // this point should have stations
        if (STNS == null || STNS.length == 0) {
            pw.println("      <documentation>No data available for station(s) "+
                "and time range</documentation>");
            pw.println("    </dataset>");
            pw.println("</catalog>");
            return;
        }
        // if no dataStart/dateEnd given, look for time place holders
        if( dateEnd == null ) {
            Calendar now = Calendar.getInstance();
            dateEnd = dateFormatISO.format(now.getTime());
            if (dtime == null || dtime.equals("latest")) {
                now.add( Calendar.HOUR, -120 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("1hour")) {
                now.add( Calendar.HOUR, -1 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("6hour")) {
                now.add( Calendar.HOUR, -6 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("12hour")) {
                now.add( Calendar.HOUR, -12 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("1day")) {
                now.add( Calendar.HOUR, -24 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("2day")) {
                now.add( Calendar.HOUR, -48 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("3day")) {
                now.add( Calendar.HOUR, -72 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("4day")) {
                now.add( Calendar.HOUR, -96 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("5day")) {
                now.add( Calendar.HOUR, -120 );
                dateStart = dateFormatISO.format(now.getTime());
            } else if (dtime.equals("all")) {
                now.add( Calendar.HOUR, -168 );
                dateStart = dateFormatISO.format(now.getTime());

            //      individual data report return for set time dtime
            } else if (p.p_isodate.matcher(dtime).find()) {
                dateStart = dateEnd = dtime;
            } else {
                pw.println("        <p>time is invalid "+ dtime +"<p>");
                pw.println("    </dataset>");
                pw.println("</catalog>");
                return;
            }
            //pw.println("dateStart =" + dateStart);
            //pw.println("dateEnd =" + dateEnd);
        }

        // main loop on stns DAYS
        String yyyymmddStart = dateStart.substring( 0, 4 ) + dateStart.substring( 5, 7 ) +
            dateStart.substring( 8, 10 );
        //pw.println( "<p>yyyymmddStart =" + yyyymmddStart + "</p>\n");

        String yyyymmddEnd = dateEnd.substring( 0, 4 ) + dateEnd.substring( 5, 7 ) +
            dateEnd.substring( 8, 10 );
        //pw.println( "<p>yyyymmddEnd =" + yyyymmddEnd + "</p>\n");

        boolean nodata = true; // checks if any data found
        for (int j = 0; j < STNS.length; j++) {
            String station = STNS[j];
            String timeStart = "Level2_" +  station +"_"+ yyyymmddStart +"_"+ dateStart.substring( 11, 13 ) + dateStart.substring( 14, 16 ) +".ar2v";
            //pw.println( "<p>timeStart =" + timeStart + "</p>\n");
            String timeEnd = "Level2_" +  station +"_"+ yyyymmddEnd +"_"+ dateEnd.substring( 11, 13 ) + dateEnd.substring( 14, 16 ) +".ar2v";
            //pw.println( "<p>timeEnd =" + timeEnd + "</p>\n");
            boolean notDone = true;
            boolean firstTime = true; // so header only printed once
            File fstn;
            Matcher m;
            String key = "", var1 = "", var2 = "", var3 = "";

            // Obtain the days available
            //pw.println( "Dir =" + radarLevel2Dir +"/"+ station);
            String[] DAYS = getDAYS(radarLevel2Dir +"/"+ station, pw);
            if (DAYS == null || DAYS.length == 0)
                continue;
            //pw.println( "DAYS.length =" + DAYS.length);

            for (int i = 0; i < DAYS.length && notDone; i++) {
                //pw.println( "<p>dir =" + DAYS[ i ] + "</p>\n");
                if( ! isValidDay( DAYS[ i ], yyyymmddStart, yyyymmddEnd ) )
                    continue;
                //pw.println( "<p>pass dir =" + DAYS[ i ] + "</p>\n");

                String day = DAYS[ i ].substring( 0, 4 ) +"-"+
                    DAYS[ i ].substring( 4, 6 ) +"-";
//+ DAYS[ i ].substring( 6 ) + "T";
                //pw.println( "<p>day =" + day + "</p>\n");
                ArrayList times = new ArrayList();
                File dir = new File(radarLevel2Dir +"/"+ station +"/"+ DAYS[ i ]);
                String[] TIMES = dir.list();
                //pw.println( "TIMES.length =" + TIMES.length);
                for( int t = 0; t < TIMES.length; t++ ) {
                    //pw.println("<p>" + TIMES[ t ] + "</p>\n");
                    if( ! isValidDate( TIMES[ t ], timeStart, timeEnd ) )
                       continue;
                    //pw.println("<p>" + TIMES[ t ] + "</p>\n");
                    times.add( TIMES[ t ] );
                }
                 
                Collections.sort(times, new CompareKeyDescend());
                //pw.println("times =" + times );

                // write out data/datasets element is latest first order
                for( int t = 0; t < times.size(); t++ ) {
                    //pw.println("<p>" + TIMES[ t ] + "</p>\n");
                    if (p.p_html_i.matcher(returns).find()) {
                        if (firstTime) {
                            pw.println("<h3>Report(s) for station "
                                    + station + "</h3>");
                            firstTime = false;
                            nodata = false;
                        }
                       pw.print( "<p><a href=\""+ serviceBase + station +"/"+ 
                          DAYS[ i ] +"/"+ times.get( t ) +"\">" );
                        pw.println( times.get( t ) + "</a></p>\n");
                    } else if (!serviceType.equals( "" )) {
                        nodata = false;
                        catalogOut( (String)times.get( t ), station, pw, serviceType, serviceBase, returns);
                    } else if (p.p_ascii_i.matcher(returns).find()) {
                            pw.println(times.get( t ));
                    } else if (returns.equals( "data" )) {
                       //outputData( (String)times.get( t ), pw);
                       pw.println( "data for file"+ times.get( t ));
                       File file = new File( (String)times.get( t ) );
                       ServletUtil.returnFile(this, req, res, file, null);
                    }
                    if (dtime == null || dtime.equals("latest")) {
                        notDone = false;
                        break;
                    }
                } // end times
            } //end for DAYS
        } //end foreach station

        // add ending tags
        if (p.p_catalog_i.matcher(returns).find() ) { 
            if (nodata) {
                pw.println("      <documentation>No data available for station(s) "+
                    "and time range</documentation>");
            }
            pw.println("    </dataset>");
            pw.println("</catalog>");
        } else if (p.p_html_i.matcher(returns).find()) {
            pw.println("</html>");
        } else if (p.p_xml_i.matcher(returns).find()) {
            pw.println("</reports>");
        }
        if( req != null )
           ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1);

        } catch (Throwable t) {
            if( req != null )
               ServletUtil.handleException(t, res);
        }
    } // end doGet

    // create a dataset entry for a catalog
    public void catalogOut(String product, String stn, PrintWriter pw, String serviceType, String serviceBase, String returns) {
        //pw.println("Station ="+ stn );
        pw.println("      <dataset name=\""+ product +"\" ID=\""+ 
           product.hashCode() +"\"" );

        String pTime = product.substring(12,16) +"-"+ product.substring(16,18)
           +"-"+ product.substring(18,20) +"T"+ product.substring(21, 23) 
           +":"+ product.substring(23, 25) +":00";

        String pDay =  product.substring(12,20);

//  + stn + " RadarLevel2 data\" " ;

        //pw.println(pTime);

        //pw.print(" " + stn + " RadarLevel2 data\" urlPath=\""+ serviceBase);
        //theTime = theTime +" " + stn + " RadarLevel2 data\" " ; 
        //pw.print( theTime +" " + stn + " RadarLevel2 data\"" ); 
        //pw.println( " ID=\""+ theTime.hashCode() +"\"" ); 
        pw.print("        urlPath=\"");
        if (p.p_HTTPServer_i.matcher(serviceType).find()) {
            //pw.println("returns="+ returns +"&amp;stn=" + stn + "&amp;dtime=" + pTime + "\"/>");
             pw.println( stn +"/"+ pDay +"/"+ product +"\">" );
             pw.println( "<date type=\"created\">"+ pTime +"</date>" );
             pw.println( "</dataset>" );

        } else if (p.p_DODS_i.matcher(serviceType).find()) {
            pw.println("serviceType="+ serviceType +"&amp;returns="+ returns +"&amp;stn=" + stn + "&amp;dtime=" + pTime + "\">");
             pw.println( "<date type=\"created\">"+ pTime +"</date>" );
             pw.println( "</dataset>" );
            //pw.println( "<a href=\""+ serviceBase + stn +"/"+ pDay +"\">"+
            //   product +"</a>" );
        }

    } // end catalogOut

    // create xml tags for the reports parameters
    public void outputData(String fileName, PrintWriter pw) {
    //File file = getFile( req);
    //ServletUtil.returnFile(this, req, res, file, null);

    } // end outputData

    // get configurations from ThreddsIDD.cfg file
    public void getConfigurations(String stopAt, PrintWriter pw)
            throws FileNotFoundException, IOException {

        Pattern p_stop = Pattern.compile(stopAt);
        Matcher m;
        String variable, value;

        //pw.println(contentPath + getPath() +"ThreddsIDD.cfg" );
        BufferedReader br = getInputStreamReader(contentPath + getPath() +"ThreddsIDD.cfg");
        String input = "";
        while ((input = br.readLine()) != null) {

            // skip lines starting with #
            if (p.p_B_pound.matcher(input).find() || input.length() == 0)
                continue;
            // stop at end of requested section
            if (p_stop.matcher(input).find())
                break;
            m = p.p_config.matcher(input);
            if (m.find()) {
                variable = m.group(1);
                // trim trailing spaces
                value = p.p_spaces.matcher(m.group(2)).replaceAll("");
                //pw.println( "<p>variable =" + variable +"</p>");
                //pw.println( "<p>value =" + value +"</p>");

                if (variable.equals("catalogVersion")) {
                    catalogVersion = value;
                } else if (variable.equals("defaultCatalog")) {
                    defaultCatalog = value;
                } else if (variable.equals("backgroundImage")) {
                    backgroundImage = contentPath + getPath() + value;
                } else if (variable.equals("radarLevel2DQC")) {
                    radarLevel2DQC = contentPath + getPath() + value;
                } else if (variable.equals("radarLevel2Dir")) {
                    radarLevel2Dir = value;
                } else if (variable.equals("radarLevel2HTTPServiceName")) {
                    radarLevel2HTTPServiceName = value;
                } else if (variable.equals("radarLevel2HTTPServiceType")) {
                    radarLevel2HTTPServiceType = value;
                } else if (variable.equals("radarLevel2HTTPServiceBase")) {
                    radarLevel2HTTPServiceBase = value;
                } else if (variable.equals("radarLevel2HTTPDataFormatType")) {
                    radarLevel2HTTPDataFormatType = value;
                } else if (variable.equals("radarLevel2ASCIIServiceName")) {
                    radarLevel2ASCIIServiceName = value;
                } else if (variable.equals("radarLevel2ASCIIUrlPath")) {
                    radarLevel2ASCIIUrlPath = value;
                } else if (variable.equals("radarLevel2ASCIIDataFormatType")) {
                    radarLevel2ASCIIDataFormatType = value;
                } else if (variable.equals("radarLevel2ADDEServiceName")) {
                    radarLevel2ADDEServiceName = value;
                } else if (variable.equals("radarLevel2ADDEUrlPath")) {
                    radarLevel2ADDEUrlPath = value;
                } else if (variable.equals("radarLevel2DODSServiceName")) {
                    radarLevel2DODSServiceName = value;
                } else if (variable.equals("radarLevel2DODSServiceType")) {
                    radarLevel2DODSServiceType = value;
                } else if (variable.equals("radarLevel2DODSServiceBase")) {
                    radarLevel2DODSServiceBase = value;
                }
            } else {
                pw.println("error in configuration line parse");
            }
            //pw.println( input );
        }
        br.close();
        return;
    } // end getConfigurations

    public static void main(String args[]) throws IOException, ServletException {

        // Function References
        RadarLevel2Servlet ms = new RadarLevel2Servlet();

        ms.doGet(null, null);
    }

} // end RadarLevel2Servlet
