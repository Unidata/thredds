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
package thredds.server.radarServer;

import thredds.catalog.query.Station;
import thredds.server.ncSubset.QueryParams;
import ucar.nc2.units.DateType;
import ucar.nc2.units.DateRange;
import thredds.servlet.ServletUtil;
import thredds.servlet.UsageLog;

import java.util.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;

import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

/**
 * By:   Robb Kambic
 * Date: Nov 29, 2008
 * Time: 2:29:36 PM
 */

public class RadarMethods {
  static public final String nexradStations = "RadarNexradStations.xml";
  static public final String terminalStations = "RadarTerminalStations.xml";
  static public List<Station> nexradList = new ArrayList<Station>();
  static public List<Station> terminalList = new ArrayList<Station>();
  static public HashMap<String, Station> nexradMap;
  static public HashMap<String, Station> terminalMap;
  static public final ArrayList<String> nexradVars = new ArrayList<String>();
  static public final ArrayList<String> terminalVars = new ArrayList<String>();
  static private final String serviceName = "OPENDAP";
  static private final String serviceType = "OPENDAP";

  private ServerMethods sm;
  private boolean debug = false;
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  //private org.slf4j.Logger log;

  public RadarMethods( ) {}

  public RadarMethods( String contentPath, org.slf4j.Logger log ) {
     //this.log = log;
     sm = new  ServerMethods( log );
     if( nexradList.size() == 0 ) {
       nexradList = sm.getStations( contentPath + getPath() + nexradStations );
       terminalList = sm.getStations( contentPath + getPath() + terminalStations );
       if( nexradList == null || terminalList == null) {
           log.error( "Station initialization problem using "+
                   contentPath + getPath() + nexradStations +" "+
                   contentPath + getPath() + terminalStations );
           return;
       }
       nexradMap = sm.getStationMap( nexradList );
       terminalMap = sm.getStationMap( terminalList );

       nexradVars.add( "N0R");
       nexradVars.add( "N1R");
       nexradVars.add( "N2R");
       nexradVars.add( "N3R");
       nexradVars.add( "DPA");
       nexradVars.add( "DHR");
       nexradVars.add( "DSP");
       nexradVars.add( "N1P");
       nexradVars.add( "N0S");
       nexradVars.add( "N1S");
       nexradVars.add( "N2S");
       nexradVars.add( "N3S");
       nexradVars.add( "N0V");
       nexradVars.add( "N1V");
       nexradVars.add( "N0Z");
       nexradVars.add( "NCR");
       nexradVars.add( "NET");
       nexradVars.add( "NMD");
       nexradVars.add( "NTP");
       nexradVars.add( "NVL");
       nexradVars.add( "NVW");
       nexradVars.add( "N0Q"); // new vars starting on Feb 8, 2010
       nexradVars.add( "NAQ");
       nexradVars.add( "N1Q");
       nexradVars.add( "NBQ");
       nexradVars.add( "N2Q");
       nexradVars.add( "N3Q");
       nexradVars.add( "N0U");
       nexradVars.add( "NAU");
       nexradVars.add( "N1U");
       nexradVars.add( "NBU");
       nexradVars.add( "N2U");
       nexradVars.add( "N3U");
       nexradVars.add( "DVL");
       nexradVars.add( "EET");

       nexradVars.add( "N0X"); // new vars starting on Nov 17, 2010
       nexradVars.add( "NAX");
       nexradVars.add( "N1X");
       nexradVars.add( "NBX");
       nexradVars.add( "N2X");
       nexradVars.add( "N3X");
       nexradVars.add( "N0C");
       nexradVars.add( "NAC");
       nexradVars.add( "N1C");
       nexradVars.add( "NBC");
       nexradVars.add( "N2C");
       nexradVars.add( "N3C");
       nexradVars.add( "N0K");
       nexradVars.add( "NAK");
       nexradVars.add( "N1K");
       nexradVars.add( "NBK");
       nexradVars.add( "N2K");
       nexradVars.add( "N3K");
       nexradVars.add( "N0H");
       nexradVars.add( "NAH");
       nexradVars.add( "N1H");
       nexradVars.add( "NBH");
       nexradVars.add( "N2H");
       nexradVars.add( "N3H");
       nexradVars.add( "N0M");
       nexradVars.add( "NAM");
       nexradVars.add( "N1M");
       nexradVars.add( "NBM");
       nexradVars.add( "N2M");
       nexradVars.add( "N3M");
       nexradVars.add( "DPR");
       nexradVars.add( "HHC");
       nexradVars.add( "OHA");
       nexradVars.add( "DAA");
       nexradVars.add( "PTA");
       nexradVars.add( "DTA");
       nexradVars.add( "DU3");
       nexradVars.add( "DU6");
       nexradVars.add( "DOD");
       nexradVars.add( "DSD");

       nexradVars.add( "BREF1");// old vars naming conventions
       nexradVars.add( "BREF2");
       nexradVars.add( "BREF248");
       nexradVars.add( "BREF3");
       nexradVars.add( "BREF4");
       nexradVars.add( "LREF1");
       nexradVars.add( "LREF2");
       nexradVars.add( "LREF3");
       nexradVars.add( "CREF");
       nexradVars.add( "BVEL1");
       nexradVars.add( "BVEL2");
       nexradVars.add( "VEL1");
       nexradVars.add( "VEL2");
       nexradVars.add( "VEL3");
       nexradVars.add( "VEL4");
       nexradVars.add( "LREF1");
       nexradVars.add( "PRECIP1");
       nexradVars.add( "PRECIPTOT");
       nexradVars.add( "SRMV1");
       nexradVars.add( "SRMV2");
       nexradVars.add( "SRVEL1");
       nexradVars.add( "SRVEL2");
       nexradVars.add( "SRVEL3");
       nexradVars.add( "SRVEL4");
       nexradVars.add( "TOPS");
       nexradVars.add( "VIL");
       nexradVars.add( "PRE1");
       nexradVars.add( "PRET");
       nexradVars.add( "PREA");
       nexradVars.add( "VAD");

       // add terminal vars
       terminalVars.add( "TR0");
       terminalVars.add( "TR1");
       terminalVars.add( "TR2");
       terminalVars.add( "TV0");
       terminalVars.add( "TV1");
       terminalVars.add( "TV2");
       terminalVars.add( "TZL");
       terminalVars.add( "DHR");
       terminalVars.add( "NCR");
       terminalVars.add( "NET");
       terminalVars.add( "NVW");
       terminalVars.add( "NVL");
       terminalVars.add( "NST");
       terminalVars.add( "NHI");
       terminalVars.add( "NTV");
       terminalVars.add( "FTM");
       terminalVars.add( "N1P");
       terminalVars.add( "NTP");
       terminalVars.add( "DPA");
       terminalVars.add( "SPD");
       terminalVars.add( "DSP");
       terminalVars.add( "NMD");
       terminalVars.add( "RSL");
       terminalVars.add( "GSM");       
     }
  }

  public Document stationsXML( RadarServer.RadarType radarType, Document doc, Element rootElem, String path )
    throws Exception {
      // stations in this dataset, set by path
      String[] stations = stationsDS( radarType, RadarServer.dataLocation.get(path ));
      if( path.contains( "level3") && stations[ 0 ].length() == 4 ) {
          for( int i = 0; i < stations.length; i++ )
               stations[ i ] = stations[ i ].substring( 1 );
      }
      doc = makeStationDocument( doc, rootElem, stations,  radarType );
      return doc;
  }

  // must end with "/"
  protected String getPath() {
    return "servers/";
  }

  // get/check/process query from servlet call
  public void radarQuery(RadarServer.RadarType radarType, HttpServletRequest req, HttpServletResponse res, PrintWriter pw )
            throws ServletException, IOException {

    String radarDir = null;
    try {
//      long  startms = System.currentTimeMillis();
//      long  endms;
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
      if( ! qp.parseQuery(req, res, new String[]{ QueryParams.XML, QueryParams.HTML, QueryParams.RAW, QueryParams.NETCDF}))
        return; // has sent the error message
//      endms = System.currentTimeMillis();
//      System.out.println( "after QueryParams "+ (endms - startms));
//      startms = System.currentTimeMillis();
      // check Query Params
      if( ! checkQueryParms( radarType, qp, level2 ) ) {
        //qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        log.error( "checkQueryParms Failed "+ req.getQueryString() );
        throw new Exception( "checkQueryParms Failed "+ req.getQueryString() );
      }
//      endms = System.currentTimeMillis();
//      System.out.println( "after checkQueryParms "+ (endms - startms));
//      startms = System.currentTimeMillis();
      // check if all data needs to be return, ie not time information given
     // boolean allTimes = ! ( qp.hasTimePoint || qp.hasDateRange );

      // what type of output wanted XML html
      String serviceBase;
      qp.acceptType = qp.acceptType.replaceFirst( ".*/", "" );
      if( ServerMethods.p_html_i.matcher(qp.acceptType).find()) { // accept html
        res.setContentType( qp.acceptType );
        serviceBase =  pathInfo +"/";
      } else {
        serviceBase = "/thredds/dodsC/"+ pathInfo +"/";
      }
      // writes first part of catalog
      if( ! writeHeader( radarType,  qp, pathInfo, pw) ) {
        //qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        log.error( "Write Header Failed "+ req.getQueryString() );
        throw new Exception( "Write Header Failed "+ req.getQueryString() );
      }
//      endms = System.currentTimeMillis();
//      System.out.println( "after writeHeader "+ (endms - startms));
//      startms = System.currentTimeMillis();
      // gets products according to stations, time, and variables
      boolean dataFound = processQuery( radarDir, qp, pw, serviceBase, radarType  );
//      endms = System.currentTimeMillis();
//      System.out.println( "after processQuery "+ (endms - startms));
//      startms = System.currentTimeMillis();
      // add ending tags
      if ( ServerMethods.p_xml_i.matcher(qp.acceptType).find() ) {
          if (! dataFound ) {
              pw.println("      <documentation>No data available for station(s) "+
                  "and time range</documentation>");
          }
          pw.println("    </dataset>");
          pw.println("</catalog>");
      } else if ( ServerMethods.p_html_i.matcher(qp.acceptType).find()) {
          pw.println("  </table>");
          if (! dataFound )
              pw.println("<p>No data available for station(s) and time range "+
                      req.getQueryString() +"</p>");
          pw.println("</html>");
      }
//      endms = System.currentTimeMillis();
//      System.out.println( "after radarQuery "+ (endms - startms));
//      startms = System.currentTimeMillis();
    } catch (Throwable t) {
        log.error("Query error "+ req.getQueryString());
        ServletUtil.handleException(t, res);
    }
  } // end radarNexradQuery

  // check that parms have valid stations, vars or times
  private Boolean checkQueryParms(RadarServer.RadarType radarType, QueryParams qp, Boolean level2 )
    throws IOException {
    try {
      if (qp.hasBB) {
        if( radarType.equals( RadarServer.RadarType.nexrad ) )
          qp.stns = sm.getStationNames(qp.getBB(), nexradList );
        else
          qp.stns = sm.getStationNames(qp.getBB(), terminalList );
        if( ! level2 )
            qp.stns = sm.convert4to3stations( qp.stns );
        if (qp.stns.size() == 0) {
          //qp.errs.append("<documentation>ERROR: Bounding Box contains no stations</documentation>\n");
          //qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          log.error( "Bounding Box contains no stations " );
          throw new Exception( "Bounding Box contains no stations " );
        }
      }

      if (qp.hasStns ) {
        if( isStationListEmpty(qp.stns, radarType )) {
          //qp.errs.append("<documentation>ERROR: No valid stations specified</documentation>\n");
          //qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          log.error( "No valid stations specified 1" );
          throw new Exception( "No valid stations specified" );
        }
      }

      if (qp.hasLatlonPoint) {
        qp.stns = new ArrayList<String>();
        if( radarType.equals( RadarServer.RadarType.nexrad ) )
          qp.stns.add( sm.findClosestStation(qp.lat, qp.lon, nexradList ));
        else
          qp.stns.add( sm.findClosestStation(qp.lat, qp.lon, terminalList ));
        if( ! level2 )
            qp.stns = sm.convert4to3stations( qp.stns );
      } else if (qp.fatal) {
        //qp.errs.append("<documentation>ERROR: No valid stations specified</documentation>\n");
        //qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        log.error( "No valid stations specified 2" );
        throw new Exception( "No valid stations specified" );
      }

      // qp.stns could be null, ouch
      boolean useAllStations = ( qp.stns.get( 0 ).toUpperCase().equals( "ALL"));
      if (useAllStations) {
        if( radarType.equals( RadarServer.RadarType.nexrad ) )
          qp.stns = sm.getStationNames( nexradList ); //need station names
        else
          qp.stns = sm.getStationNames( terminalList ); //need station names
        if( ! level2 )
            qp.stns = sm.convert4to3stations( qp.stns );
      }

      /*
      if (qp.hasTimePoint && ( sm.filterDataset(qp.time) == null)) {
        //qp.errs.append("<documentation>ERROR: This dataset does not contain the time point= " + qp.time + " </documentation>\n");
        //qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        log.error( "No valid stations specified" );
        throw new Exception( "No valid stations specified" );

        return;
      }
      */
      /*
      // needs work start and end aren't set, too expensive to set
      if (qp.hasDateRange) {
        DateRange dr = qp.getDateRange();
        if (! sm.intersect(dr, start, end)) {
          //qp.errs.append("<documentation>ERROR: This dataset does not contain the time range= " + qp.time + " </documentation>\n");
          //qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          log.error( "No valid stations specified" );
          throw new Exception( "No valid stations specified" );
          return;
        }
      }
      */
      /*
      if (useAllStations && useAllTimes) {
        qp.errs.append("<documentation>ERROR: You must subset by space or time</documentation>\n");
        //qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        log.error( "No valid stations specified" );
        throw new Exception( "No valid stations specified" );
        return;
      }
      */
      // if time in not set, return latest
      if( qp.hasTimePoint ) {
          if( qp.time.isPresent() ) {
              try {
                  qp.time_end = new DateType( "present", null, null);
                  qp.time_start = new DateType(ServerMethods.epic, null, null);
              } catch (java.text.ParseException e) {
                  //qp.errs.append("Illegal param= 'time' must be valid ISO Duration\n");
                  log.error("Illegal param= 'time' must be valid ISO Duration");
                  throw new Exception("Illegal param= 'time' must be valid ISO Duration");
              }
          } else {
              qp.time_end = qp.time;
              qp.time_start = qp.time;
          }
      } else if( qp.hasDateRange ) {
          DateRange dr = qp.getDateRange();
          qp.time_start = dr.getStart();
          qp.time_end = dr.getEnd();
      } else {
         //qp.hasTimePoint = true;
         try {
             qp.time = new DateType( "present", null, null);
             qp.time_end = new DateType( "present", null, null);
             qp.time_start = new DateType(ServerMethods.epic, null, null);
         } catch (java.text.ParseException e) {
             //qp.errs.append("Illegal param= 'time' must be valid ISO Duration\n");
             log.error("Illegal param= 'time' must be valid ISO Duration");
             throw new Exception("Illegal param= 'time' must be valid ISO Duration");
         }
      }

      if(  level2 ) {
          qp.vars = null; // level2 can't select vars
      } else {
          if( qp.vars != null ) { // remove desc from vars
            ArrayList<String> tmp = new ArrayList<String>();
            for ( String var:  qp.vars ) {
                tmp.add( var.replaceFirst( "/.*", "" ) );
            }
            qp.vars = tmp;
          }
      }
    } catch ( Exception e ) {
      return false;
    }
    return true;
  }

  // write out catalog Header
  private Boolean writeHeader(RadarServer.RadarType radarType, QueryParams qp, String pathInfo, PrintWriter pw)
    throws IOException {
    try {
      // accept = XML inplies OPeNDAP server
      Boolean level2 = pathInfo.contains( "level2");
      int level = (level2) ? 2 : 3;
      String serviceBase = "";
      if ( ServerMethods.p_xml_i.matcher(qp.acceptType).find()) {
        serviceBase = "/thredds/dodsC/"+ pathInfo +"/";
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.print("<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"");
        pw.println(" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Level"+ level +" datasets in near real time\" version=\""+
                "1.0.1\">");
        pw.println("");
        pw.print("  <service name=\""+ serviceName +"\" serviceType=\""+ serviceType +"\"");
        pw.println(" base=\"" + serviceBase + "\"/>");
        pw.print("    <dataset name=\"RadarLevel"+ level +" datasets for available stations and times\" collectionType=\"TimeSeries\" ID=\""+
  "accept=" + qp.acceptType + "&amp;");

        if( ! level2 && qp.vars != null ) { // add vars
            pw.print("var=");
            for (int i = 0; i < qp.vars.size(); i++ ) {
                pw.print( qp.vars.get( i ) );
                if( i < qp.vars.size() -1 )
                    pw.print( "," );
            }
            pw.print("&amp;");
        }
        // use all stations
        if( qp.stns.get( 0 ).toUpperCase().equals( "ALL") ) {
            pw.print("stn=ALL&amp;");
        } else if (qp.hasStns ) {
            for (String station : qp.stns) {
                pw.print("stn=" + station +"&amp;");
            }
        } else if (qp.hasBB) {
            pw.print("south="+ qp.south +"&amp;north="+ qp.north +"&amp;" );
            pw.print("west="+ qp.west +"&amp;east="+ qp.east +"&amp;" );
        }

        if( qp.hasDateRange ) {
          if( qp.time_start.getDate() == null || qp.time_start.isBlank() ||
            qp.time_end.getDate() == null || qp.time_end.isBlank() ) {
            pw.println("time_start=" + qp.time_start.toString()
                    +"&amp;time_end=" + qp.time_end.toString() +"\">");
            pw.println( "<documentation>need ISO time</documentation>\n" );
            pw.println("    </dataset>");
            pw.println("</catalog>");
            return false;
          } else {
            pw.println("time_start=" + qp.time_start.toDateTimeStringISO()
                    +"&amp;time_end=" + qp.time_end.toDateTimeStringISO() +"\">");
          }
        } else if( qp.time.isPresent() ) {
            pw.println("time=present\">");
        } else if( qp.hasTimePoint ) {
          if( qp.time.getDate() == null || qp.time.isBlank()) {
            pw.println("time=" + qp.time.toString() +"\">");
            pw.println( "<documentation>need ISO time</documentation>\n" );
            pw.println("    </dataset>");
            pw.println("</catalog>");
            return false;
          } else {
            pw.println("time=" + qp.time.toDateTimeStringISO() +"\">");
          }
        } else {
            pw.println( "\">" );
        }
        pw.println("    <metadata inherited=\"true\">");
        pw.println("      <dataType>Radial</dataType>");
        pw.print("      <dataFormat>" );
        if( level2 ) {
            pw.print( "NEXRAD2" );
        } else if( radarType.equals( RadarServer.RadarType.nexrad ) ){
            pw.print( "NIDS" );
        } else {
            pw.print( "TDWR" );
        }
        pw.println( "</dataFormat>");
        pw.println("      <serviceName>" + serviceName + "</serviceName>");
        pw.println("    </metadata>");
        pw.println();

      } else if ( ServerMethods.p_html_i.matcher(qp.acceptType).find()) { // accept html
        pw.println("<Head><Title>THREDDS RadarNexrad Server</Title></Head>");
        pw.println("<body>"); // link=\"red\" alink=\"red\" vlink=\"red\">");
        pw.println("<center><H1>Nexrad Level"+ level +" Radar Results</H1></center>");
        pw.println("  <table align=\"center\" border cellpadding=\"5\" width=\"90%\">");
        pw.println("    <tr>");
        pw.println("    <th scope=\"col\"><u>OPENDAP</u></th>");
        pw.println("    <th scope=\"col\"><u>HTTPServer</u></th>");
        pw.println("    </tr>");
        serviceBase = pathInfo +"/";

      } else if ( ServerMethods.p_ascii_i.matcher(qp.acceptType).find()) {
        pw.println( "<documentation>\n" );
        pw.println( "Request not implemented: "+ pathInfo );
        pw.println( "</documentation>\n" );
        pw.println( "<documentation>need ISO time</documentation>\n" );
        pw.println("    </dataset>");
        pw.println("</catalog>");
        return false;
      }
      // at this point must have stations
      if( isStationListEmpty(qp.stns, radarType )) {
        pw.println("      <documentation>No data available for station(s) "+
            "and time range</documentation>");
        pw.println("    </dataset>");
        pw.println("</catalog>");
        return false;
      }
    } catch (Exception e ) {
      return false;
    }
    return true;
  }

  // This routine is very complex because if figures out different paths to
  // actual products ie stn/var/time stn/time/var  var/stn/time etc.
  // processQuery is limited by the stns, dates and vars in the query
  private Boolean processQuery( String tdir, QueryParams qp, PrintWriter pw,
    String serviceBase, RadarServer.RadarType radarType  ) throws IOException {

    int numProds = 0;
    try { // could have null pointer exceptions on dirs & checks
      // set date info
      String yyyymmddStart = qp.time_start.toDateString();
      yyyymmddStart = yyyymmddStart.replace( "-", "");
      String yyyymmddEnd = qp.time_end.toDateString();
      yyyymmddEnd = yyyymmddEnd.replace( "-", "");
      String dateStart =  yyyymmddStart +"_"+ sm.hhmm( qp.time_start.toDateTimeString() );
      String dateEnd =  yyyymmddEnd +"_"+ sm.hhmm( qp.time_end.toDateTimeString() );
      // top dir has to point to stns, vars, or date dir
      File files = new File( tdir );
      String[] tdirs = files.list();
      if( tdirs == null )
        return false;
      // need to check/eliminate . file names
      ArrayList<String> tmp = new ArrayList<String>();
      for( String name : tdirs ) {
        if( name.startsWith( "."))
          continue;
        tmp.add( name );
      }
      // redo stations array for removal of . files
      if( tdirs.length != tmp.size() ) {
        tdirs = new String[tmp.size()];
        tdirs = (String[]) tmp.toArray( tdirs );
      }
      // decide if directory contains stns, dates or vars, only one true
      Boolean isStns = isStation(tdirs[ 0 ], radarType );
      Boolean isDates = ServerMethods.p_yyyymmdd.matcher(tdirs[ 0 ]).find();
      Boolean isVars = isVar( tdirs[ 0 ].toUpperCase(), radarType  );

      if( ! ( isStns || isDates || isVars ) ) {
        log.error("processQuery error, no valid stn, date, or var "+ qp.toString() );
        pw.println( "<documentation>\n" );
        pw.println( "Query can't be satisfied :<![CDATA["+ qp.toString() +"]]>\n" );
        pw.println( "</documentation>\n" );
        return numProds > 0; // invalid query
      }

      if( isStns ) {
        // limit stations to the ones in the query
        for (String station : qp.stns ) {
          String sDir = tdir +'/'+ station ;
          files = new File( sDir );
          if( ! files.exists())
            continue;
          String[] sdirs = files.list();
          if( sdirs == null)
            continue;
          // need to check next dirs for products, dates or vars
          File file = new File( sDir +"/"+ sdirs[ 0 ] );
          if( file.isFile() ) { // products in dir, process dir
            // TODO: check and delete
            //numProds += processProducts( sdirs, sDir.replaceFirst( tdir, "").substring( 1 ),
            numProds += processProducts( sdirs, sDir.substring( tdir.length() +1),
               dateStart, dateEnd, qp, pw, serviceBase );
          } else if( ServerMethods.p_yyyymmdd.matcher(sdirs[ 0 ]).find() ) { //dates
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
                  // TODO: check and delete
                  //numProds += processProducts( ndirs, dDir.replaceFirst( tdir, "").substring( 1 ),
                  numProds += processProducts( ndirs, dDir.substring( tdir.length() +1),
                     dateStart, dateEnd, qp, pw, serviceBase );
                  if( qp.hasTimePoint ) // only want one product
                    break;
                } else if( nexradVars.contains( ndirs[ 0 ].toUpperCase() ) ) {
                  // not implemented, doesn't make sense stn/date/vars
                }
              }
            }
          } else if( nexradVars.contains( sdirs[ 0 ].toUpperCase() ) ||
            terminalVars.contains( sdirs[ 0 ].toUpperCase() )) { // variable
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
                // TODO: check and delete
               // numProds += processProducts( vdirs, vDir.replaceFirst( tdir, "").substring( 1 ),
                numProds += processProducts( vdirs, vDir.substring( tdir.length() +1),
                   dateStart, dateEnd, qp, pw, serviceBase );

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
            // TODO: check and delete
            //numProds += processProducts( vdirs, vDir.replaceFirst( tdir, "").substring( 1 ),
            numProds += processProducts( vdirs, vDir.substring( tdir.length() +1),
               dateStart, dateEnd, qp, pw, serviceBase );
          // TODO: check and delete
          // nexradMap.get( "K"+ vdirs[ 0 ] ) != null
          } else if( isStation(vdirs[ 0 ], radarType )) {
            for (String station : qp.stns ) {
              String sDir = vDir +'/'+ station ;
              files = new File( sDir );
              if( ! files.exists())
                continue;
              String[] sdirs = files.list();
              if( sdirs == null)
                continue;
              // need to check next dirs for products, dates
              file = new File( sDir +"/"+ sdirs[ 0 ] );
              if( file.isFile() ) { // products in dir, return dir
                 // TODO: check and delete
                //numProds += processProducts( sdirs, sDir.replaceFirst( tdir, "").substring( 1 ),
                numProds += processProducts( sdirs, sDir.substring( tdir.length() +1),
                   dateStart, dateEnd, qp, pw, serviceBase );
              } else if( ServerMethods.p_yyyymmdd.matcher(sdirs[ 0 ]).find() ) { //dates
                java.util.Arrays.sort( sdirs, new CompareKeyDescend() );
                for( int k = 0; k < sdirs.length; k++) {
                  if( sm.isValidDay( sdirs[ k ],  yyyymmddStart, yyyymmddEnd ) ) {
                    // valid date
                    String dDir = sDir +"/"+ sdirs[ k ];
                    files = new File( dDir );
                    String[] ddirs = files.list();
                    // TODO: check and delete
                    //numProds += processProducts( ddirs, dDir.replaceFirst( tdir, "").substring( 1 ),
                    numProds += processProducts( ddirs, dDir.substring( tdir.length() +1),
                       dateStart, dateEnd, qp, pw, serviceBase);
                    if( qp.hasTimePoint ) // only want one product
                      break;
                  }
                }
              }
            }
          }
        }
      } else {
        return numProds > 0; // invalid query
      }
      return numProds > 0;

    } catch ( Exception e ) {
      log.error("radarServer processQuery error" );
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      pw.println( "<documentation>\n" );
      pw.println( "Query can't be satisfied :<![CDATA["+ qp.toString() +"]]>\n" );
      pw.println( "</documentation>\n" );
      return numProds > 0; // partial or invalid query
    }
  }

  // check if product has valid time then creates a dataset for product
  private int processProducts( String[] products, String rPath,
    String dateStart, String dateEnd, QueryParams qp,
    PrintWriter pw, String serviceBase ) throws Exception {

    Boolean latest = qp.hasTimePoint;

    // Just return all products
    boolean allTimes = ! ( qp.hasTimePoint || qp.hasDateRange );
    java.util.Arrays.sort( products, new CompareKeyDescend() );

    int numProducts = 0;
    // write out products with latest first order
    if ( ServerMethods.p_xml_i.matcher(qp.acceptType).find() ) {
      for( int t = 0; t < products.length; t++ ) {
        if( products[ t ].startsWith( "." ) )
          continue;
        if( ! allTimes ) {
          if( ! sm.isValidDate( products[ t ], dateStart, dateEnd ) )
            continue;
        }
        numProducts++;
        XMLdataset(  products[ t ], rPath, pw );
        if ( latest ) {
          break;
        }
      }
    } else {
      for( int t = 0; t < products.length; t++ ) {
        if( products[ t ].startsWith( "." ) )
          continue;
        if( ! allTimes ) {
          if( ! sm.isValidDate( products[ t ], dateStart, dateEnd ) )
            continue;
        }  
        numProducts++;
        HTMLdataset(  products[ t ], rPath, pw, serviceBase );
        if( latest ) {
          break;
        }
      }
    }
    return numProducts;
  }

  // create a XML dataset entry for a catalog
  public void XMLdataset(String product, String rPath, PrintWriter pw ) throws IOException {

    pw.println("      <dataset name=\""+ product +"\" ID=\""+
          product.hashCode() +"\"" );

    String pDate = sm.getObTimeISO( product );
    pw.print("        urlPath=\"");
    pw.println(  rPath +"/"+ product +"\">" );
    pw.println( "        <date type=\"start of ob\">"+ pDate +"</date>" );
    pw.println( "      </dataset>" );

  } // end datasetOut

  // create a HTML dataset entry for a catalog
  public void HTMLdataset(String product, String rPath, PrintWriter pw, String serviceBase )
      throws IOException {

    pw.println( "  <tr>" );
    pw.println("    <td align=center valign=center><a href=\"/thredds/dodsC/"+
        serviceBase + rPath +"/"+ product +".html\">"+ product +"</a></td>" );
    pw.println("    <td align=center valign=center><a href=\"/thredds/fileServer/"+
        serviceBase + rPath +"/"+ product +"\">"+ product +"</a></td>" );
    pw.println( "  </tr>" );

  } // end HTMLdataset

  /**
  * Create an XML station document
   * @param doc
   * @param rootElem
   * @param stations
   * @return Document
  */
  public Document makeStationDocument( Document doc, Element rootElem, String[] stations,
                                       RadarServer.RadarType radarType ) throws Exception {
    for (String s : stations ) {
      Station stn = getStation( s, radarType );

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

  public String[] stationsDS( RadarServer.RadarType radarType, String path ) throws Exception {
    String[] stations = null;

    if( path != null ) {
      File files = new File( path );
      stations = files.list();
      ArrayList<String> tmp = new ArrayList<String>();
      for( String station : stations ) {
        if( station.startsWith( "."))
          continue;
        tmp.add( station );
      }
      // redo stations array for removal of . files
      if( stations.length != tmp.size() ) {
        stations = new String[tmp.size()];
        stations = (String[]) tmp.toArray( stations );
      }
      if( isVar( stations[ 0 ].toUpperCase(), radarType ) ) {
        if( radarType.equals( RadarServer.RadarType.nexrad ) ) {
          //path += "/N0R";
          path += "/"+ nexradVars.get( 0 );
          files = new File( path );
          stations = files.list();
        } else {
          //path += "/TR0";
          path += "/"+ terminalVars.get( 0 );
          files = new File( path );
          stations = files.list();
        }
      }
    }
    // no stations found
    if( stations ==  null || stations.length == 0 ) {
      stations = new String[ 1 ];
      if( radarType.equals( RadarServer.RadarType.nexrad ))
        stations =   nexradMap.keySet().toArray( stations );
      else
        stations =   terminalMap.keySet().toArray( stations );
    }
    return stations;
 }

  /**
   * print station in a XML format from this info
   * @param stations
   * @param pw 
  */
  public void printStations( String[] stations, PrintWriter pw, RadarServer.RadarType radarType ) throws Exception {
    for (String s : stations ) {
      Station stn = getStation( s, radarType );

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

  /**
   * Determine if any of the given station names are actually in the dataset.
   *
   * @param stations List of station names
   * @return true if list is empty, ie no names are in the actual station list
   * @throws IOException if read error
   */
  public boolean isStationListEmpty(List<String> stations, RadarServer.RadarType radarType ) {

    if( stations.get( 0 ).toUpperCase().equals( "ALL") )
        return false;

    for (String s : stations ) {
      if( isStation( s, radarType ))
        return false;
    }
    return true;
  }

  /**
   * returns true if a station
   * @param station
   * @param radarType
   * @return  boolean  isStation
   */
  public boolean isStation( String station, RadarServer.RadarType radarType ) {

    if( station.toUpperCase().equals( "ALL") )
        return true;

    Station stn = null;
    if( station.length() == 3 && radarType.equals( RadarServer.RadarType.terminal ) ) { // terminal level3 station
      stn = terminalMap.get( "T"+ station );
    } else if( station.length() == 3 ) {
      for( Station stn3 : nexradList ) {
         if( stn3.getValue().endsWith( station ) ) {
           stn = stn3;
           break;
         }
      }
    } else if( radarType.equals( RadarServer.RadarType.terminal ) ) {
      stn = terminalMap.get( station );
    } else {
       stn = nexradMap.get( station );
    }
    if( stn != null)
      return true;
    return false;
  }

  /**
   * returns station or null
   * @param station
   * @param radarType
   * @return  station
   */
  public Station getStation( String station, RadarServer.RadarType radarType ) {

    Station stn = null;
    if( station.length() == 3 && radarType.equals( RadarServer.RadarType.terminal ) ) { // terminal level3 station
      stn = terminalMap.get( "T"+ station );
    } else if( station.length() == 3 ) {
      for( Station stn3 : nexradList ) {
         if( stn3.getValue().endsWith( station ) ) {
           stn = stn3;
           break;
         }
      }
    } else if( radarType.equals( RadarServer.RadarType.terminal ) ) {
      stn = terminalMap.get( station );
    } else {
       stn = nexradMap.get( station );
    }
    return stn;
  }

  /**
   * returns stations or null
   * @param radarType
   * @return  station
   */
  public String[] getStations( RadarServer.RadarType radarType ) {
    String[] stn = null;
    if( radarType.equals( RadarServer.RadarType.nexrad ) ) {
      stn = new String[ nexradList.size() ];
      stn = nexradList.toArray( stn );
    } else if( radarType.equals( RadarServer.RadarType.terminal ) ) {
      stn = new String[ terminalList.size() ];
      stn = terminalList.toArray( stn );
    }
    return stn;
  }

  /**
   * returns true if a variable
   * @param var
   * @param radarType
   * @return  boolean  isVar
   */
  public boolean isVar( String var, RadarServer.RadarType radarType ) {

    if( var.toUpperCase().equals( "ALL") )
        return true;
    /*
    if( radarType.equals( RadarServer.RadarType.nexrad ) ) {
      return nexradVars.contains( var );
    } else if( radarType.equals( RadarServer.RadarType.terminal ) ) {
      return terminalVars.contains( var );
    }
    */
    if( nexradVars.contains( var ) ) {
      return true;
    } else if( terminalVars.contains( var ) ) {
      return true;
    }
    return false;
  }

  public String getStartDateTime( String path ) throws Exception {
    String timeDir =  RadarServer.dataLocation.get( path );
    log.debug( "timeDir ="+ timeDir );
    // hard coded, otherwise not sure one get a valid station ID
    if( path.contains( "level3") ) {
       timeDir = timeDir + "/N0R/TLX";
    } else if( path.contains( "level2") ) {
       timeDir = timeDir + "/KTLX";
    }
    File dir = new File( timeDir );
    String[] files = dir.list();
    java.util.Arrays.sort( files, new CompareKeyDescend() );
//    for( String file : files ) {
//        System.out.println( file );
//    }
    String year = files[ files.length -1 ].substring( 0, 4);
    String month = files[ files.length -1 ].substring( 4, 6);
    String day = files[ files.length -1 ].substring( 6, 8);
    log.debug( year +"-"+ month +"-"+ day +"T:00:00:00Z" );

    return year +"-"+ month +"-"+ day +"T:00:00:00Z";

  }

  protected class CompareKeyDescend implements Comparator<String> {
    /*
    public int compare(Object o1, Object o2) {
      String s1 = (String) o1;
      String s2 = (String) o2;

      return s2.compareTo(s1);
    }
    */
    public int compare(String s1, String s2 ) {
      return s2.compareTo(s1);
    }
  }


  public static void main(String args[]) throws IOException  {
    try {
      DateType dte = new DateType( "present", null, null);
      DateType dts = new DateType(ServerMethods.epic, null, null);
      System.out.println("DateType = (" + dts.toString() + ")");
      System.out.println("Date = (" + dts.getDate() + ")");
    } catch (java.text.ParseException e) {
        System.out.println("Illegal param= 'time' must be valid ISO Duration\n");
    }
  }
}
