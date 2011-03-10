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
/**
 * User: rkambic
 * Date: Oct 13, 2010
 * Time: 12:30:33 PM
 */

package thredds.server.radarServer;

import thredds.catalog.query.Station;
import ucar.nc2.units.DateType;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RadarServerUtil {

  public static final Pattern p_yymmdd_hhmm = Pattern.compile("(\\d{2})(\\d{4}_\\d{4})");
  public static final Pattern p_yyyymmdd_hhmm = Pattern.compile("(\\d{8}_\\d{4})");
  public static final String epic = "1970-01-01T00:00:00";


  ////////////////////// Station utilities

  public static List<String> getStationNames( List<Station> stations ) {
    ArrayList<String> result = new ArrayList<String>();
    for (Station s : stations) {
        result.add(s.getValue());
    }
    return result;
  }

  public static List<String> convert4to3stations( List<String> stations ) {
    ArrayList<String> result = new ArrayList<String>();
    for (String s : stations) {
      if( s.length() == 4 )
        result.add(s.substring( 1 ));
      else
        result.add( s );
    }
    return result;
  }
  /**
   * Get the list of station names that are contained within the bounding box.
   *
   * @param boundingBox lat/lon bounding box
   * @return list of station names contained within the bounding box
   * @throws java.io.IOException if read error
   */

  public static List<String> getStationNames(LatLonRect boundingBox, List<Station> stations ) {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    ArrayList<String> result = new ArrayList<String>();
    for (Station s : stations) {
      latlonPt.set(s.getLocation().getLatitude(), s.getLocation().getLongitude());
      if (boundingBox.contains(latlonPt)) {
        result.add(s.getValue());
      }
    }
    return result;
  }

  /**
   * Find the station closest to the specified point.
   * The metric is (lat-lat0)**2 + (cos(lat0)*(lon-lon0))**2
   *
   * @param lat latitude value
   * @param lon longitude value
   * @return name of station closest to the specified point
   * @throws java.io.IOException if read error
   */

  public static String findClosestStation(double lat, double lon, List<Station> stations) {
    double cos = Math.cos(Math.toRadians(lat));
    Station min_station = stations.get(0);
    double min_dist = Double.MAX_VALUE;

    for (Station s : stations) {
      double lat1 = s.getLocation().getLatitude();
      double lon1 = LatLonPointImpl.lonNormal(s.getLocation().getLongitude(), lon);
      double dy = Math.toRadians(lat - lat1);
      double dx = cos * Math.toRadians(lon - lon1);
      double dist = dy * dy + dx * dx;
      if (dist < min_dist) {
        min_dist = dist;
        min_station = s;
      }
    }
    return min_station.getValue();
  }
  /**
   * Determine if any of the given station names are actually in the dataset.
   *
   * @param stations List of station names
   * @return true if list is empty, ie no names are in the actual station list
   * @throws java.io.IOException if read error
   */
  public static boolean isStationListEmpty(List<String> stations, DatasetRepository.RadarType radarType ) {

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
  public static boolean isStation( String station, DatasetRepository.RadarType radarType ) {

    if( station.toUpperCase().equals( "ALL") )
        return true;

    Station stn = null;
    // terminal level3 station
    if( station.length() == 3 && radarType.equals( DatasetRepository.RadarType.terminal ) ) {
      stn = DatasetRepository.terminalMap.get( "T"+ station );
    } else if( station.length() == 3 ) {
      for( Station stn3 : DatasetRepository.nexradList ) {
         if( stn3.getValue().endsWith( station ) ) {
           stn = stn3;
           break;
         }
      }
    } else if( radarType.equals( DatasetRepository.RadarType.terminal ) ) {
      stn = DatasetRepository.terminalMap.get( station );
    } else {
       stn = DatasetRepository.nexradMap.get( station );
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
  public static Station getStation( String station, DatasetRepository.RadarType radarType ) {

    Station stn = null;
    // terminal level3 station
    if( station.length() == 3 && radarType.equals( DatasetRepository.RadarType.terminal ) ) {
      stn = DatasetRepository.terminalMap.get( "T"+ station );
    } else if( station.length() == 3 ) {
      for( Station stn3 : DatasetRepository.nexradList ) {
         if( stn3.getValue().endsWith( station ) ) {
           stn = stn3;
           break;
         }
      }
    } else if( radarType.equals( DatasetRepository.RadarType.terminal ) ) {
      stn = DatasetRepository.terminalMap.get( station );
    } else {
       stn = DatasetRepository.nexradMap.get( station );
    }
    return stn;
  }

  /////////////////// time utilities

  // returns ISO time extracted from a product
  public static String getObTimeISO( String product ) {

     String date;
     StringBuffer dsb = new StringBuffer();
     Matcher m = p_yyyymmdd_hhmm.matcher( product );
     if( m.find(0) ) {
         date = m.group( 1 );
     } else { // try date w/o century
         Matcher mm = p_yymmdd_hhmm.matcher( product );
         if( mm.find(0) ) { // add century, fails 2070
             if( Integer.parseInt(mm.group( 1 )) > 69 ) {
                 dsb.append("19").append( mm.group( 1 )).append( mm.group( 2 ));
             } else {
                 dsb.append("20").append( mm.group( 1 )).append( mm.group( 2 ));
             }
             date = dsb.toString();
         } else {
             return epic;
         }
     }
    dsb.setLength( 0 );
    dsb.append( date.substring(0,4)).append( "-" ).append(date.substring(4,6));
    dsb.append("-").append(date.substring(6,8)).append("T").append(date.substring(9, 11));
    dsb.append(":").append( date.substring(11,13) ).append(":00");
    return dsb.toString();
  }

  // returns hhmm of datetime string
  public static String hhmm( String dateTime ) {
     StringBuffer sb = new StringBuffer( dateTime.substring( 11, 13 ) );
     sb.append( dateTime.substring( 14, 16 ));
     return sb.toString();
  }

  // returns if day is between dayStart and dayEnd
  public static boolean isValidDay( String day, String yyyymmddStart, String yyyymmddEnd )
  {
     if ( day.equals( "all")) // for casestudy data
       return true;
     if( day.compareTo( yyyymmddStart ) >= 0 &&
         day.compareTo( yyyymmddEnd ) <= 0 )
         return true;

     return false;

  }

  // returns true if date is between dateStart and dateEnd
  public static boolean isValidDate( String dateReport, String dateStart, String dateEnd ) {

     Matcher m;
     m = p_yyyymmdd_hhmm.matcher( dateReport );
     String date;
     if( m.find() ) {
         date = m.group( 1 );
     } else { // try date w/o century
         m = p_yymmdd_hhmm.matcher( dateReport );
         if( m.find() ) { // add century, fails 2070
             StringBuffer dsb = new StringBuffer();
             if( Integer.parseInt(m.group( 1 )) > 69 ) {
                 dsb.append("19").append( m.group( 1 )).append( m.group( 2 ));
             } else {
                 dsb.append("20").append( m.group( 1 )).append( m.group( 2 ));
             }
             date = dsb.toString();
         } else {
             return false;
         }
     }
     // extract hhmm from product
     if( date.compareTo( dateStart ) >= 0 &&
         date.compareTo( dateEnd ) <= 0 )
         return true;

     return false;

  }
}
