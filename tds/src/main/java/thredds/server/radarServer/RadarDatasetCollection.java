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
 * Date: Jan 14, 2010
 * Time: 1:31:49 PM
 */

package thredds.server.radarServer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Maintains the Radar collection of stations and days for a Radar Dataset.  The
 * purpose is to permit quering the collection by station verses time.
 * 
 */
public class RadarDatasetCollection {

  public static final Pattern p_yyyymmdd_hhmm = Pattern.compile("\\d{8}_(\\d{4})");
  public static boolean debug = false;

  static Calendar cal;
  static SimpleDateFormat dateFormat;
  static {
    cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
    dateFormat = new SimpleDateFormat( "yyyyMMdd", Locale.US );
    dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
  }

  /**
   * Top Directory of dataset
   */
  String tdir;

  /**
   * station/time type directory, typical level2 radar data
   * or
   * product/station/time
   */
  boolean stnTime = true;

  /**
   * station/product/time type directory, typical Gempak radar data
   */
  boolean stnProduct  = false;

  /**
   * Radar product, for level II this is null
   */
  String product = null;

  /**
   * Standard Product Naming ie Level2_KRLX_20100112_1324.ar2v
   */
  boolean standardName = true;

  /**
   * Map of all the stations, ArrayList of yyyyddmm times
   */
  HashMap<String, ArrayList<String>> yyyymmdd = new HashMap<String, ArrayList<String>>();

  /**
   *  Map of key = stn + yyyymmdd, times or full unstandard names
   */
  HashMap<String, ArrayList<String>> hhmm = new HashMap<String, ArrayList<String>>();

  /**
   * constructors
   */
  public RadarDatasetCollection() {
  }

  public RadarDatasetCollection(String tdir,  String product) {

    this.product = product;
    // TODO: check
    if( stnTime ) {
      if (product == null) {
        this.tdir = tdir;
      } else {
        this.tdir = tdir + "/" + product;
      }
    } else { //
      if (product == null) {
        this.tdir = tdir;
      } else {
        this.tdir = tdir + "/"+ product +"/"; // product/station type directory
      }
    }
    // Read in RadarDayCollections for this dataset
    File dir = new File(tdir);
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        if ( ! child.startsWith( ".2"))
          continue;
        child = tdir +"/"+ child;
        RadarDayCollection rdc = new RadarDayCollection().read( child );
        this.standardName = rdc.standardName;
        java.util.Set<String> stations = rdc.getStations();
        for( String station : stations ) {
          ArrayList<String> days = yyyymmdd.get( station );
          if ( days == null )
            days = new ArrayList<String>();
          days.add( rdc.yyyymmdd );
          yyyymmdd.put( station, days );
          hhmm.put( station + rdc.yyyymmdd, rdc.getTimes( station ) );
        }
      }
    }
  }

  /**
   * returns times for this station in the RadarStationCollection object
   * @param rsc RadarStationCollection
   * @return success boolean
   */
  public boolean getStationTimes( RadarStationCollection rsc ) {

    // get todays times for station
    Date now =  cal.getTime();
    String currentDay = dateFormat.format( now );
    String stnDir = tdir +"/"+ rsc.stnName +"/"+ currentDay;
    File dir = new File(stnDir);
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      ArrayList<String> currenthhmm = new ArrayList<String>();
      String[] children = dir.list();
      Matcher m;
      for (String aChildren : children) {
        //File child = new File(dir, aChildren);
        //if (child.isDirectory()) {
        //  continue;
        //} else {
          // Level2_KFTG_20100108_0654.ar2v

          m = p_yyyymmdd_hhmm.matcher(aChildren);
          if (m.find()) {
            if (standardName )
              currenthhmm.add(m.group(1));
            else
              currenthhmm.add(aChildren);
          }
        //}
      }

      if( currenthhmm.size() > 0 ) {
        Collections.sort(currenthhmm, new CompareKeyDescend());
        rsc.yyyymmdd.add( currentDay );
        rsc.hhmm.put( currentDay, currenthhmm );
      }
      if ( debug ) {
        for ( String hm : currenthhmm ) {
          System.out.println( currentDay +"_"+ hm );
        }
      }
    }
    ArrayList<String> dal = yyyymmdd.get( rsc.stnName );
    Collections.sort(dal, new CompareKeyDescend());
    // check for previous day
    cal.add( Calendar.DAY_OF_MONTH, -1 );
    now =  cal.getTime();
    currentDay = dateFormat.format( now );
    if( !currentDay.equals( dal.get( 0 ))) {

    }

    rsc.yyyymmdd.addAll( dal );
    // TODO: need check if new day needs added
    for ( String day : dal ) {
      ArrayList<String> tal = hhmm.get( rsc.stnName + day );
      rsc.hhmm.put( day, tal );
      if ( debug ) {
        for ( String hm : tal ) {
          System.out.println( day +"_"+ hm );
        }
      }
    }
    return true;
  }

  public RadarStationCollection queryStation( String dir, String stnName, String product ) {
    RadarStationCollection rsc =  new RadarStationCollection( dir, stnName, stnTime,  product);
    getStationTimes( rsc );

    return rsc;
  }

  public static void main(String[] args) throws IOException {

    String tdir = null;
    String product = null;
    if (  args.length == 2) {
      tdir = args[0];
      product = (args[1].equals("null")) ? null : args[1];
    } else {
      System.out.println("Not the correct parameters: tdir, product");
      return;
    }
    // create/populate dataset
    RadarDatasetCollection rdc = new RadarDatasetCollection( tdir, product );
    System.out.println( "Dates for station KFTG" );
    RadarStationCollection rsc =  rdc.queryStation( tdir,  "KFTG",  product);
    //RadarStationCollection rsc =  new RadarStationCollection( tdir,  "KFTG", true,  product);
    //rdc.getStationTimes( rsc );
    //rdc.populate(tdir, type, day, product);
    //String sfile = rdc.write();
    //if (sfile == null) {
    //  System.out.println("RadarDayCollection write Unsuccessful");
    //} else {
    //  System.out.println("RadarDayCollection write successful");
    //}
  }

  protected class CompareKeyDescend implements Comparator<String> {
    /*
    public int compare(Object o1, Object o2) {
      String s1 = (String) o1;
      String s2 = (String) o2;

      return s2.compareTo(s1);
    }
    */
    public int compare(String s1, String s2) {
      return s2.compareTo(s1);
    }
  }
}
