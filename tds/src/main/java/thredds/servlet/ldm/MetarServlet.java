/* class MetarServlet
 *
 * Library for the Metar Servlet
 *
 * Finds the near realtime product files for a particular station and time.
 * Outputs either html, ascii, dqc  or catalog files.
 *
 * By:  Robb Kambic  04/25/2005
 *
 */

package thredds.servlet.ldm;

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
    static protected String metarASCIIServiceName;
    static protected String metarASCIIUrlPath;
    static protected String metarASCIIDataFormatType;
    static protected String metarADDEServiceName;
    static protected String metarADDEUrlPath;
    static protected String metarDODSServiceName;
    static protected String metarDODSUrlPath;

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
        String ll = null;
        String ur = null;
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
                serviceType = "";

            serviceName = ServletUtil.getParameterIgnoreCase(req, "serviceName");

            urlPath = ServletUtil.getParameterIgnoreCase(req, "urlPath");

            ll = ServletUtil.getParameterIgnoreCase(req, "ll");

            ur = ServletUtil.getParameterIgnoreCase(req, "ur");

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
            STNS[0] = "KDEN";
            STNS[1] = "KSEA";
            //dtime = "latest";
            dtime = "2006-12-28T16:53:00";
            dtime = "6hour";
            //dtime = "5day";
            //dateStart = "2006-12-26T00:12:30";
            //dateEnd = "2006-12-26T12:12:30";

            returns = "catalog";
            //returns = "ascii";
            //returns = "html";
            //returns = "xml";
            //returns = "dqc";

            //serviceType = "HTTPServer";
            serviceType = "";
            //serviceName = metarHTTPServiceName;
            //pw.println( "serviceName =" + serviceName );
            //pw.println( "urlPath =" + urlPath );
            //ll = "-15:-90";
            //ur = "15:90";
        
            } // end command line testing

            //  get configurations from ThreddsIDD.cfg
            if( metarDir == null )
               getConfigurations("buoy", pw);
            // pw.println( "metarDQC =" + metarDQC );
            // pw.println( "metarDir =" + metarDir );

        if (p.p_qc_or_dqc_i.matcher(returns).find()) { // returns dqc doc

            if( ll != null && ur != null ) {
                STNS = boundingBox(ll, ur, metarDQC, pw);
                //pw.println( "<stations>" );
                //for (int j = 0; j < STNS.length; j++) {
                //    pw.println( "    <station name=\""+ STNS[ j ] +"\" />" );
                //}
                //pw.println( "</stations>" );
            } else {
      
                //pw.println(metarDQC);
                BufferedReader br = getInputStreamReader(metarDQC);
                String input = "";
                while ((input = br.readLine()) != null) {
                    pw.println(input);
                }
                br.close();
            }
            return;
        }

        // requesting a catalog with different serviceTypes
        if (p.p_HTTPServer_i.matcher(serviceType).find() ||
                (p.p_catalog_i.matcher(returns).find()) ) { // backward capatiablity
                serviceName = metarHTTPServiceName;
                serviceType = metarHTTPServiceType;
                serviceBase = metarHTTPServiceBase;
                if (p.p_catalog_i.matcher(returns).find()) {
                    returns = "xml";  // default
                }
        //  serviceType =~ /ADDE/i
        //} else if (p.p_ADDE_i.matcher(serviceType).find()) {
        //        serviceName = metarADDEServiceName;
        //        urlPath = metarADDEUrlPath;
        //  serviceType =~ /DODS/i
        } else if (p.p_DODS_i.matcher(serviceType).find()) {
                serviceName = metarDODSServiceName;
                urlPath = metarDODSUrlPath;
        }
        // write out catalog with datasets
        if ( ! serviceType.equals( "" )) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.print("<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"");
            pw.println(" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Metar datasets in near real time\" version=\""+ catalogVersion +"\">");
            pw.println("");
            pw.print("  <service name=\""+ serviceName +"\" serviceType=\""+ serviceType +"\"");
            pw.println(" base=\"" + serviceBase + "\"/>");
            pw.print("    <dataset name=\"Metar datasets for available stations and times\" collectionType=\"TimeSeries\" ID=\"returns=" + returns + "&");
            if( STNS != null ) {
                for (int i = 0; i < STNS.length; i++) {
                    pw.print("stn=" + STNS[i] +"&");
                }
            } else if( ll != null && ur != null ) {
                pw.print("ll="+ ll +"&ur="+ ur +"&" );
            } else {
                pw.print("stn=all&");
            }

            if( dtime != null ) {
                pw.println("dtime=" + dtime + "\">");
            } else if( dateEnd != null ) {
                pw.println("dateStart=" + dateStart +"&dateEnd=" +dateEnd +"\">");
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

            pw.println("<Head><Title>THREDDS Metar Server</Title></Head>");
            pw.println("<body bgcolor=\"lightblue\" link=\"red\" alink=\"red\" vlink=\"red\">");
            pw.println("<center><H1>Metar Selection Results</H1></center>");

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
        // bounding box given to determine stns
        if (ll != null && ur != null) {
            //STNS = boundingBox(ll, ur, metarDQC, pw);
            STNS = boundingBox(ll, ur, metarDQC, null);
            //pw.println(  "STNS.length="+ STNS.length );
            if (STNS.length == 0) {
                pw.println("      <documentation>No data available for station(s) "+
                    "and time range</documentation>");
                pw.println("    </dataset>");
                pw.println("</catalog>");
                return;
            }
        } //end bounding box

        // Obtain the days available
        String[] DAYS = getDAYS(metarDir, pw);
        //pw.println( "DAYS.length =" + DAYS.length);
        if (DAYS.length == 0)
            return;

        // if request for all stns given, populate STNS 
        if (STNS == null || p.p_all_i.matcher(STNS[0]).find()) {
            //pw.println( "metarDir + DAYS[ 0 ] =" + metarDir +"/"+  DAYS[ 0 ] );
            File dir = new File(metarDir + "/" + DAYS[0]);
            STNS = dir.list();
            //pw.println( "STNS.length =" + STNS.length );
            //dtime = "latest";
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

            //      individual data report return for set time dtime
            } else if (p.p_isodate.matcher(dtime).find()) {
                dateStart = dateEnd = dtime;
            } else {
                pw.println("time is invalid " + dtime);
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
            boolean notDone = true;
            boolean firstTime = true; // so header only printed once
            File fstn;
            Matcher m;
            String key = "", var1 = "", var2 = "", var3 = "";

            for (int i = 0; i < DAYS.length && notDone; i++) {
                //pw.println( "<p>dir =" + DAYS[ i ] + "</p>\n");
                if( ! isValidDay( DAYS[ i ], yyyymmddStart, yyyymmddEnd ) )
                    continue;

                String day = DAYS[ i ].substring( 0, 4 ) +"-"+
                    DAYS[ i ].substring( 4, 6 ) +"-";
//+ DAYS[ i ].substring( 6 ) + "T";
                //pw.println( "<p>day =" + day + "</p>\n");
                // optimize for 3 char US stations then Canadians
                if (station.length() == 3) {
                    String tmp = "K" + station;
                    fstn = new File(metarDir + "/" + DAYS[i], tmp);
                    if (fstn.exists()) {
                        station = tmp;
                    } else {
                        tmp = "C" + station;
                        fstn = new File(metarDir + "/" + DAYS[i], tmp);
                        if (fstn.exists()) {
                            station = tmp;
                        } else {
                            continue;
                        }
                    }
                } else {
                    fstn = new File(metarDir + "/" + DAYS[i], station);
                    if (!fstn.exists()) {
                        continue;
                    }
                }
                BufferedReader br = getInputStreamReader(metarDir + "/" + DAYS[i] + "/" + station);
                TreeMap RPTS = new TreeMap(new CompareKeyDescend());
                String input = "";
                // check for valid date, eliminate dups and store rpt keys hhmm
                while ((input = br.readLine()) != null) {
                    m = p.p_station_dateZ2.matcher(input);
                    if (m.lookingAt()) {
                        var1 = m.group(1);
                        var2 = m.group(2);
                        var3 = m.group(3);
                        //pw.println( "<p>dateReport ="+ day + var1 +"T"+ var2 +":"+ var3 +":00"+ "</p>\n");
                        if( ! isValidDate( day + var1 +"T"+ var2 +":"+ var3 +":00", dateStart, dateEnd ) )
                            continue;
                        //pw.println( "<p>key =" + var1 +":"+ var2 +":"+ var3 +"</p>\n");

                        key = var1 + var2 + var3;
                        if (RPTS.containsKey( key )) {
                            String rptsValue = (String) RPTS.get( key );
                            if (rptsValue.length() < input.length()) {
                                RPTS.put( key, input);
                            }
                        } else {
                            RPTS.put( key, input);
                            //pw.println( "<p>entering =" + var2 +":"+ var3 +"</p>\n");
                            
                        }
                    }
                } // end while check for valid date, eliminate dups and store 
                br.close();

                // write out data/datasets element is latest first order
                for (Iterator it = RPTS.keySet().iterator(); it.hasNext();) {
                    key = (String) it.next();
                    String report = (String) RPTS.get(key);
                    //pw.println("<p>" + report + "</p>\n");
                    if (p.p_html_i.matcher(returns).find()) {
                        if (firstTime) {
                            pw.println("<h3>Report(s) for station "
                                    + station + "</h3>");
                            firstTime = false;
                            nodata = false;
                        }
                        pw.println("<p>" + report + "</p>\n");
                    } else if (!serviceType.equals( "" )) {
                        nodata = false;
                        m = p.p_station_dateZ.matcher(report);
                        if (m.lookingAt()) {
                            var1 = m.group(1);
                            catalogOut(day, key, station, pw, serviceType, serviceBase, returns);
                        }
                    } else if (p.p_ascii_i.matcher(returns).find()) {
                            pw.println(report);
                    } else if (p.p_xml_i.matcher(returns).find()) {
                            outputXML(day, report, pw);
                    }
                    if (dtime == null || dtime.equals("latest")) {
                        notDone = false;
                        break;
                    }
                } // end RPTS.keySet
            } //end for DAYS
        } //end foreach station

        // add ending tags
        if (!serviceType.equals( "" )) {
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

    // returns if day is between dayStart and dayEnd
    public boolean isValidDay( String dateDir, String yyyymmddStart, String yyyymmddEnd )
    {

       if( dateDir.compareTo( yyyymmddStart ) >= 0 && 
           dateDir.compareTo( yyyymmddEnd ) <= 0 )
           return true;

       return false;

    }

    // returns if date is between dateStart and dateEnd
    public boolean isValidDate( String dateReport, String dateStart, 
       String dateEnd ) {

       if( dateReport.compareTo( dateStart ) >= 0 &&
           dateReport.compareTo( dateEnd ) <= 0 )
           return true;

       return false;

    }

    // create a dataset entry for a catalog
    public void catalogOut(String day, String ddhhmm, String stn, PrintWriter pw, String serviceType, String serviceBase, String returns) {

        //String theTime = "";

        pw.print("      <dataset name=\"");

        String theTime = day + ddhhmm.substring(0,2) +"T"+ ddhhmm.substring(2, 4) +":"+ ddhhmm.substring(4, 6) +":00";

//  + stn + " Metar data\" " ;

        //pw.print(theTime);

        //pw.print(" " + stn + " Metar data\" urlPath=\""+ serviceBase);
        //theTime = theTime +" " + stn + " Metar data\" " ; 
        pw.print( theTime +" " + stn + " Metar data\"" ); 
        pw.println( " ID=\""+ theTime.hashCode() +"\"" ); 
        pw.print("        urlPath=\"");
        if (p.p_HTTPServer_i.matcher(serviceType).find()) {
            pw.println("returns="+ returns +"&stn=" + stn + "&dtime=" + theTime + "\"/>");
        } else if (p.p_ADDE_i.matcher(serviceType).find()) {
            pw.println("group=rtptsrc&descr=sfchourly&select='id%20" + stn + "'" +
                    "&num=all&param=day%20time%20t%20td%20psl\"/>");
        } else if (p.p_DODS_i.matcher(serviceType).find()) {
            pw.println("group=rtptsrc&descr=sfchourly&select='id " + stn + "'" +
                    "&num=all&param=day time t td psl\"/>");
        }

    } // end catalogOut

    // create xml tags for the reports parameters
    public void outputXML(String day, String report, PrintWriter pw) {

        MetarParseReport mpr = new MetarParseReport();
        LinkedHashMap metar = mpr.parseReport(report);
        String key;

        if (metar == null) {
            System.out.println("return null Hash parse");
            //System.exit( 1 );
            return;
        }
        pw.println("<station name=\"" + (String) metar.get("Station") + "\">");
        pw.println("\t<parameter name=\"Date\" value=\""+ day +
            (String) metar.get( "Hour") +":"+ (String) metar.get( "Minute") +":00\"/>");
        pw.println("\t<parameter name=\"Report\" value=\"" + report + "\"/>");
        for( Iterator it = metar.keySet().iterator(); it.hasNext(); ) {
            key = (String) it.next();
            if( key.equals( "Day") || key.equals( "Hour") || key.equals( "Minute") 
                || key.equals( "Station") )
                continue;

            //System.out.println( key + "\t\t" + (String) metar.get( key ) );
            pw.println("\t<parameter name=\""+ key +"\" value=\""+ 
                (String) metar.get(key) + "\"/>");

        }
        pw.println("</station>");
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
                } else if (variable.equals("metarASCIIServiceName")) {
                    metarASCIIServiceName = value;
                } else if (variable.equals("metarASCIIUrlPath")) {
                    metarASCIIUrlPath = value;
                } else if (variable.equals("metarASCIIDataFormatType")) {
                    metarASCIIDataFormatType = value;
                } else if (variable.equals("metarADDEServiceName")) {
                    metarADDEServiceName = value;
                } else if (variable.equals("metarADDEUrlPath")) {
                    metarADDEUrlPath = value;
                } else if (variable.equals("metarDODSServiceName")) {
                    metarDODSServiceName = value;
                } else if (variable.equals("metarDODSUrlPath")) {
                    metarDODSUrlPath = value;
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
