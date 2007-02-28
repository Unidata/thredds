/* class MetarServlet
 *
 * Library for the Metar Servlet
 *
 * Finds the near realtime product files for a particular station and time.
 * Outputs either html, text, dqc  or catalog files.
 *
 * By:  Robb Kambic  04/25/2005
 *
 */

package thredds.servlet.idd;

import thredds.servlet.*;
import java.io.*;
import java.util.*;
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

public class MetarServlet extends LdmServlet {

    // static configuration file ThreddsIDD.cfg parameters
    static protected String catalogVersion;
    static protected String defaultCatalog;
    static protected String backgroundImage;
    static protected String metarDQC;
    static protected String metarDir = null;
    static protected String metarHTTPServiceName;
    static protected String metarHTTPServiceType;
    static protected String metarHTTPServiceBase;
    static protected String metarHTTPDataFormatType;
    //static protected String metarTEXTServiceName;
    //static protected String metarTEXTUrlPath;
    //static protected String metarTEXTDataFormatType;
    //static protected String metarADDEServiceName;
    //static protected String metarADDEUrlPath;
    static protected String metarDODSServiceName;
    static protected String metarDODSServiceType;
    static protected String metarDODSServiceBase;

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
        String y0 = null;
        String y1 = null;
        String x0 = null;
        String x1 = null;
        String serviceName = null;
        String serviceType = null;
        String serviceBase = null;
        //String urlPath = null;
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
            //if (dtime == null)
            //    dtime = "latest";

            dateStart = ServletUtil.getParameterIgnoreCase(req, "dateStart");

            dateEnd = ServletUtil.getParameterIgnoreCase(req, "dateEnd");

            returns = ServletUtil.getParameterIgnoreCase(req, "returns");
            if (returns == null) // default
                returns = "catalog";

            serviceType = ServletUtil.getParameterIgnoreCase(req, "serviceType");
            if (serviceType == null)
                serviceType = "HTTPServer";

            serviceName = ServletUtil.getParameterIgnoreCase(req, "serviceName");
            if (serviceName == null)
                serviceName = "HTTPServer";

            //urlPath = ServletUtil.getParameterIgnoreCase(req, "urlPath");

            // bounding box min/max lat  min/max lon
            y0 = ServletUtil.getParameterIgnoreCase(req, "y0");
            y1 = ServletUtil.getParameterIgnoreCase(req, "y1");
            x0 = ServletUtil.getParameterIgnoreCase(req, "x0");
            x1 = ServletUtil.getParameterIgnoreCase(req, "x1");

            // set ContentType
            // requesting a catalog
            if ( p.p_catalog_i.matcher(returns).find() ) {
                res.setContentType("text/xml");
            // rest requesting data
            } else if (p.p_xml_i.matcher(returns).find()) {
                res.setContentType("text/xml");
            } else if (p.p_text_i.matcher(returns).find()) {
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
            STNS[0] = "KDEN";
            STNS[1] = "KSEA";
            //STNS = null;
            dtime = "latest";
            //dtime = null;
            //dtime = "2006-12-28T16:53:00";
            //dtime = "6hour";
            //dtime = "5day";
            dateStart = "2007-02-12T15:12:30";
            dateEnd = "2007-02-12T18:12:30";

            returns = "catalog";
            returns = "text";
            //returns = "html";
            //returns = "xml";
            //returns = "dqc";

            serviceType = "HTTPServer";
            //serviceType = "";
            serviceName = "HTTPServer";
            //serviceName = "DODS";
            y0 = "39";
            //y0 = null;
            y1 = "40";
            x0 = "-105";
            x1 = "-104";
        
            } // end command line testing

            //  get configurations from ThreddsIDD.cfg
            if( metarDir == null )
               getConfigurations("buoy", pw);
            // pw.println( "metarDQC =" + metarDQC );
            // pw.println( "metarDir =" + metarDir );

        if (p.p_qc_or_dqc_i.matcher(returns).find()) { // returns dqc doc

            //if( ll != null && ur != null ) {
                //STNS = boundingBox(ll, ur, metarDQC, pw);
                //pw.println( "<stations>" );
                //for (int j = 0; j < STNS.length; j++) {
                //    pw.println( "    <station name=\""+ STNS[ j ] +"\" />" );
                //}
                //pw.println( "</stations>" );
            //} else {
      
                //pw.println(metarDQC);
                BufferedReader br = getInputStreamReader(metarDQC);
                String input = "";
                while ((input = br.readLine()) != null) {
                    pw.println(input);
                }
                br.close();
            //}
            return;
        }

        // requesting a catalog with different serviceName
        //  serviceName =~ /DODS/i
        if (p.p_DODS_i.matcher(serviceName).find()) {
                serviceName = metarDODSServiceName;
                serviceType = metarDODSServiceType;
                serviceBase = metarDODSServiceBase;
                returns = "catalog";

        //  serviceName =~ /HTTPService/i
        // else if ( p.p_catalog_i.matcher(returns).find() )  
        } else {
                serviceName = metarHTTPServiceName;
                serviceType = metarHTTPServiceType;
                serviceBase = metarHTTPServiceBase;
        }
        // write out catalog with datasets
        if ( p.p_catalog_i.matcher(returns).find() ) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.print("<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"");
            pw.println(" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Metar datasets in near real time\" version=\""+ catalogVersion +"\">");
            pw.println("");
            pw.print("  <service name=\""+ serviceName +"\" serviceType=\""+ serviceType +"\"");
            pw.println(" base=\"" + serviceBase + "\"/>");
            pw.print("    <dataset name=\"Metar datasets for available stations and times\" collectionType=\"TimeSeries\" ID=\"returns=text&amp;");
            //pw.print("    <dataset name=\"Metar datasets for available stations and times\" collectionType=\"TimeSeries\" ID=\"returns=" + returns + "&amp;");
            if( STNS != null ) {
                for (int i = 0; i < STNS.length; i++) {
                    pw.print("stn=" + STNS[i] +"&amp;");
                }
            } else if( y0 != null && y1 != null && x0 != null && x1 != null ) {
                pw.print("y0="+ y0 +"&amp;y1="+ y1 +"&amp;" );
                pw.print("x0="+ x0 +"&amp;x1="+ x1 +"&amp;" );
            } else {
                pw.print("stn=all&amp;");
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
            pw.println("</catalog>");
            return;

        } else if (p.p_html_i.matcher(returns).find()) { // returns html

            pw.println("<Head><Title>THREDDS Metar Server</Title></Head>");
            pw.println("<body bgcolor=\"lightblue\" link=\"red\" alink=\"red\" vlink=\"red\">");
            pw.println("<center><H1>Metar Selection Results</H1></center>");
            pw.println("<pre>");

        } else if (p.p_text_i.matcher(returns).find()) {

        } else if (p.p_xml_i.matcher(returns).find()) {

            //pw.println("<reports>");
            pw.println("<reports>");

        }
//
//
//
//
//
// main code body, no configurations below this line
//
        MetarQuery mq = new MetarQuery( "/local/ldm/data/pub/native/surface/metar", pw );

        // return TimeSeries for a set of stations
        if( STNS != null ) {
            //pw.println( "STNS length ="+ STNS.length );
            for( int i = 0; i < STNS.length; i++ )
                mq.getTimeSeries( STNS[ i ] );
            return;
        }

        HashMap report = null;
        // use bounding box given to determine stations
        //pw.println( "<p>minLat="+ y0 +" minLon="+ x0 +" maxLat="+ y1 +" maxLon="+ x1 +"</p>" );
        if( y0 != null && y1 != null && x0 != null && x1 != null ) {
            //STNS = boundingBox( y0, y1, x0, x1, metarDQC, pw);
            STNS = boundingBox( y0, y1, x0, x1, metarDQC, null);
            pw.println(  "STNS.length="+ STNS.length +" ="+ STNS[ 0 ]);
            if (STNS.length == 0) {
                pw.println("      <documentation>No data available for station(s) "+
                    "and time range</documentation>");
                pw.println("    </dataset>");
                pw.println("</catalog>");
                return;
            }
        } //end bounding box

        // if no dataStart/dateEnd given, look for time place holders
        if( dateEnd == null ) {
            Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
            long start = cal.getTimeInMillis(); 

            //Calendar now = Calendar.getInstance();
            //dateEnd = dateFormatISO.format(now.getTime());
            if (dtime == null || dtime.equals("latest")) {
                //now.add( Calendar.HOUR, -120 );
                //dateStart = dateFormatISO.format(now.getTime());
                //dtime = "latest";
                start = start -3600000;
            } else if (dtime.equals("1hour")) {
                //now.add( Calendar.HOUR, -1 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -3600000;
            } else if (dtime.equals("6hour")) {
                //now.add( Calendar.HOUR, -6 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -21600000;
            } else if (dtime.equals("12hour")) {
                //now.add( Calendar.HOUR, -12 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -43200000;
            } else if (dtime.equals("1day")) {
                //now.add( Calendar.HOUR, -24 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -86400000;
            } else if (dtime.equals("2day")) {
                //now.add( Calendar.HOUR, -48 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -172800000;
            } else if (dtime.equals("3day")) {
                //now.add( Calendar.HOUR, -72 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -259200000;
            } else if (dtime.equals("4day")) {
                //now.add( Calendar.HOUR, -96 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -345600000;
            } else if (dtime.equals("5day")) {
                //now.add( Calendar.HOUR, -120 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -432000000;
            } else if (dtime.equals("6day")) {
                //now.add( Calendar.HOUR, -144 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -518400000;
            } else if (dtime.equals("7day") || dtime.equals("all")) {
                //now.add( Calendar.HOUR, -168 );
                //dateStart = dateFormatISO.format(now.getTime());
                start = start -604800000;
            //      individual data report return for set time dtime
            //} else if (p.p_isodate.matcher(dtime).find()) {
            //    dateStart = dateEnd = dtime;
            } else {
                pw.println("time is invalid " + dtime);
                return;
            }
                                    //pw.println("inside MetarQuery");
            if( y0 != null ) {
                report = mq.getFromTime( start, STNS );
            } else {
		        //pw.println("called mq.getFromTime( start ) = "+ start );
                report = mq.getFromTime( start );
                //pw.println("report ="+ report.size() );
            }
            //return;
        } else { // dateStart and dateEnd processing
            pw.println("dateStart =" + dateStart);
            Date start = dateFormatISO.parse( dateStart);
            pw.println("dateEnd =" + dateEnd);
            Date end = dateFormatISO.parse( dateEnd);
            if( y0 != null ) {
                mq.getTimeRange(start.getTime(), end.getTime(), STNS );
            } else {
                mq.getTimeRange(start.getTime(), end.getTime());
            }
        }

        // add ending tags
/*
        if ( p.p_catalog_i.matcher(returns).find() ) {
            if (nodata) {
                pw.println("      <documentation>No data available for station(s) "+
                    "and time range</documentation>");
            }
            pw.println("    </dataset>");
            pw.println("</catalog>");
        } else
*/
        if (p.p_html_i.matcher(returns).find()) {
            pw.println("</pre>");
            pw.println("</html>");
        } else if (p.p_xml_i.matcher(returns).find()) {
            outputXML( report, pw );
            pw.println("</reports>");
        }
        if( req != null )
            ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1);

        } catch (Throwable t) {
            if( req != null )
               ServletUtil.handleException(t, res);
        }
    } // end doGet

    // create xml tags for the reports parameters
    public void outputXML(HashMap report, PrintWriter pw) {

        for( Iterator rit = report.keySet().iterator(); rit.hasNext();) {
            String key = (String)rit.next();
            MetarObservation ob = (MetarObservation)report.get( key );

            MetarParseReport mpr = new MetarParseReport();
            mpr.parseReport(ob.report);
            LinkedHashMap field = mpr.getFields();
            HashMap unit = mpr.getUnits();

            if (field == null) {
                System.out.println("return null Hash parse");
                //System.exit( 1 );
                return;
            }
            pw.println("<station name=\"" + ob.station + "\">");
            pw.println("\t<parameter name=\"Date\" value=\""+ ob.dateISO +" units=\"\"/>");
            pw.println("\t<parameter name=\"Report\" value=\""+ ob.report
               +"\" units=\"\"/>");
            for( Iterator it = field.keySet().iterator(); it.hasNext(); ) {
                key = (String) it.next();
                if( key.equals( "Day") || key.equals( "Hour") || key.equals( "Minute")
                    || key.equals( "Station") )
                     continue;

                   //System.out.println( key + "\t\t" + (String) field.get( key ) );
                   //pw.println("\t<parameter name=\""+ key +"\" value=\""+
                   //    (String) metar.get(key) + "\"/>");
                   pw.println("\t<parameter name=\""+ key +"\" value=\""+
                   (String) field.get(key) +"\" units=\""+ (String) unit.get(key)
                    + "\"/>");

             }
             pw.println("</station>");
        }
    } // end outputXML

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
                //pw.println( "variable =" + variable );
                //pw.println( "value =" + value );

                if (variable.equals("catalogVersion")) {
                    catalogVersion = value;
                } else if (variable.equals("defaultCatalog")) {
                    defaultCatalog = value;
                } else if (variable.equals("backgroundImage")) {
                    backgroundImage = contentPath + getPath() + value;
                } else if (variable.equals("metarDQC")) {
                    metarDQC = contentPath + getPath() + value;
                } else if (variable.equals("metarDir")) {
                    metarDir = value;
                } else if (variable.equals("metarHTTPServiceName")) {
                    metarHTTPServiceName = value;
                } else if (variable.equals("metarHTTPServiceType")) {
                    metarHTTPServiceType = value;
                } else if (variable.equals("metarHTTPServiceBase")) {
                    metarHTTPServiceBase = value;
                } else if (variable.equals("metarHTTPDataFormatType")) {
                    metarHTTPDataFormatType = value;
                //} else if (variable.equals("metarTEXTServiceName")) {
                //    metarTEXTServiceName = value;
                //} else if (variable.equals("metarTEXTUrlPath")) {
                //    metarTEXTUrlPath = value;
                //} else if (variable.equals("metarTEXTDataFormatType")) {
                //    metarTEXTDataFormatType = value;
                //} else if (variable.equals("metarADDEServiceName")) {
                //    metarADDEServiceName = value;
                //} else if (variable.equals("metarADDEUrlPath")) {
                //    metarADDEUrlPath = value;
                } else if (variable.equals("metarDODSServiceName")) {
                    metarDODSServiceName = value;
                } else if (variable.equals("metarDODSServiceType")) {
                    metarDODSServiceType = value;
                } else if (variable.equals("metarDODSServiceBase")) {
                    metarDODSServiceBase = value;
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
        MetarServlet ms = new MetarServlet();

        ms.doGet(null, null);
    }

} // end MetarServlet
