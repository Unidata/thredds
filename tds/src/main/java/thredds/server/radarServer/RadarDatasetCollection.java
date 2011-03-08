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
 * purpose is to permit querying the collection by station.
 * 
 */
public class RadarDatasetCollection {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  public static final Pattern p_yyyymmdd_hhmm = Pattern.compile("\\d{8}_(\\d{4})");
  public static boolean debug = false;

  //static Calendar cal;
  static SimpleDateFormat dateFormat;
  static {
    //cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
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
   * isCasestudycollection
   */
  boolean caseStudy = false;

  /**
   * directory in between station and products
   */
  String inbetween = null;

  /**
   * Map of all the stations, ArrayList of yyyyddmm times
   */
  HashMap<String, ArrayList<String>> yyyymmdd = new HashMap<String, ArrayList<String>>();

  /**
   *  Map  key = stn + yyyymmdd, times or full unstandard names
   */
  HashMap<String, ArrayList<String>> hhmm = new HashMap<String, ArrayList<String>>();

  private static int daysToRead = 6;

  public int getDaysToRead() {
    return daysToRead;
  }

  public void setDaysToRead(int daysToRead) {
    this.daysToRead = daysToRead;
  }

  /**
   * constructors
   */
  public RadarDatasetCollection() {
  }

  /**
   * Creates a complete Dataset Collection using the current day and past stored days
   * @param tdir  Location of dataset on disk
   * @param product  if level3, then get product dataset
   */
  public RadarDatasetCollection(String tdir,  String product) {

    if ( tdir == null)
      return;

    caseStudy = tdir.contains( "casestudies");
    
    StringBuffer sb = new StringBuffer( tdir );
    this.product = product;
    if( stnTime ) {
      if (product == null) {
        this.tdir = tdir;
      } else {
        this.tdir = sb.append( "/" ).append( product ).toString();
      }
    } else {  
      if (product == null) {
        this.tdir = tdir;
      } else {
        this.tdir = sb.append( "/" ).append( product ).append( "/" ).toString();
      }
    }
    // Read in RadarDayCollections for this dataset
    File dir = new File(this.tdir);
    if ( !caseStudy && dir.exists() && dir.isDirectory()) {
      ArrayList<String> rdc = new ArrayList<String>();
      sb.setLength( 0 );
      sb.append("In directory ").append(dir.getParent()).append("/").append(dir.getName());
      log.info( sb.toString() );
      String[] children = dir.list();
      for (String child : children) {
        if ( ! child.startsWith( ".2"))
          continue;
        rdc.add( child );
      }
      Collections.sort(rdc, new CompareKeyDescend());
      for (int i = 0; i < rdc.size() && i < daysToRead; i++) {
         readRadarDayCollection( rdc.get( i ) );
      }
    } else { // casestudy collection adhoc   station or station/product or
      this.tdir = tdir;
      ArrayList<String> stations = getStationsFromDir( tdir );
      for (String stn : stations) {
        sb.setLength( 0 );
        dir = new File( sb.append( tdir).append( "/").append( stn).toString());
        if (dir.exists() && dir.isDirectory()) {

          //sb.setLength( 0 );
          //sb.append("In directory ").append(dir.getParent()).append("/").append(dir.getName());
          //log.info( sb.toString() );
          File pdir = null;
          if( product != null ) {
            pdir = new File( sb.append("/").append(product).toString());
          } else {
            pdir = new File( sb.toString());
            // it's possible to have an extra directory in between the data
            String[] children = pdir.list();
            if ( children.length > 0 ) {
              File btwn = new File( sb.append( "/").append( children[0]).toString());
              if (btwn.exists() && btwn.isDirectory()) {
                pdir = btwn;
                inbetween = btwn.getName() +"/";
              }
            }
          }
          if (pdir != null && pdir.exists() && pdir.isDirectory()) {
            String[] children = pdir.list();
            ArrayList<String> times = new ArrayList<String>();
            for (String child : children) {
              if ( child.startsWith( "."))
                continue;
              if ( inbetween != null) {
                sb.setLength( 0 );
                sb.append( inbetween ).append( child );
                times.add( sb.toString() );
              }  else {
                times.add( child );
              }  
            }
            ArrayList<String> days = yyyymmdd.get( stn );
            if ( days == null )
              days = new ArrayList<String>();
            days.add( "all" );
            yyyymmdd.put( stn, days );
            sb.setLength( 0 );
            sb.append( stn ).append("all" );
            Collections.sort(times, new CompareKeyDescend());
            hhmm.put( sb.toString(), times );
          }
        }
      }
    }
  }

  /*
  * returns and ArrayList of stations from a directory
  */
  private ArrayList<String> getStationsFromDir(String stnDir) {

    ArrayList<String> stations = new ArrayList<String>();
    File dir = new File(stnDir);
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String aChild : children) {
        File child = new File(dir, aChild);
        if (child.isDirectory()) {
          stations.add(aChild);
        }
      }
    } else {
      return null;
    }
    return stations;

  }

  /*
   * Read in a RadarDayCollection
   * @parm fileName
   *
   */
  private Boolean readRadarDayCollection( String child ) {
    StringBuffer sb = new StringBuffer( tdir );
    sb.append( "/").append( child );
    RadarDayCollection rdc = new RadarDayCollection().read( sb.toString() );
    if( rdc == null )
      return false ;
    this.standardName = rdc.standardName;
    java.util.Set<String> stations = rdc.getStations();
    for( String station : stations ) {
      ArrayList<String> days = yyyymmdd.get( station );
      if ( days == null )
        days = new ArrayList<String>();
      days.add( rdc.yyyymmdd );
      yyyymmdd.put( station, days );
      sb.setLength( 0 );
      sb.append( station ).append(rdc.yyyymmdd );
      hhmm.put( sb.toString(), rdc.getTimes( station ) );
    }
    return true;
  }

  /*
   returns true if previous days data is now available, causes dataset reread
   */
  public boolean previousDayNowAvailable() {

    if ( caseStudy )  // casestudy data is static
      return false;

    Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
    // check for previous day
    cal.add( Calendar.DAY_OF_MONTH, -1 );
    Date now =  cal.getTime();
    String previousDay = dateFormat.format( now );
    StringBuffer sb = new StringBuffer();
    // check if new day data is available
    java.util.Set<String> stations = yyyymmdd.keySet();
    int count = 0;
    for( String station : stations ) {
      if( ++count == 10 ) // at most check 10 stations for previous day
        return false;
      ArrayList<String> days = yyyymmdd.get( station );
      if( previousDay.equals( days.get( 0 ))) {
        return false;
      } else { // check if previous day available
        sb.setLength( 0 );
        sb.append( tdir ).append( "/").append( '.' ).append( previousDay );
        File dir = new File( sb.toString() );
        if (dir.exists()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * returns times for this station in the RadarStationCollection object
   * @param rsc RadarStationCollection
   * @return success boolean
   */
  public boolean getStationTimes( RadarStationCollection rsc, String currentDay ) {

    // get today's times for station
    //Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
    //Date now =  cal.getTime();
    //String currentDay = dateFormat.format( now );
    StringBuffer sb = new StringBuffer( tdir );
    sb.append( "/" ).append( rsc.stnName ).append( "/" ).append( currentDay );
    File dir = new File( sb.toString() );
    if (dir.exists() && dir.isDirectory()) {
      // TODO: make a log message / comment out
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      sb.insert( 0, "In directory ");
      System.out.println( sb.toString() );
      ArrayList<String> currenthhmm = new ArrayList<String>();
      String[] children = dir.list();
      Matcher m;
      for (String child : children) {
        if ( child.startsWith( "."))
          continue;
        // Level2_KFTG_20100108_0654.ar2v
        m = p_yyyymmdd_hhmm.matcher(child);
        if (m.find()) {
          if (standardName )
            currenthhmm.add(m.group(1));
          else
            currenthhmm.add(child);
        }
      }
      if( currenthhmm.size() > 0 ) {
        Collections.sort(currenthhmm, new CompareKeyDescend());
        rsc.yyyymmdd.add( currentDay );
        rsc.hhmm.put( currentDay, currenthhmm );
      }
      if ( debug ) {
        for ( String hm : currenthhmm ) {
          sb.setLength( 0 );
          sb.append( currentDay ).append( "_" ).append( hm );
          System.out.println( sb.toString() );
        }
      }
    }
    ArrayList<String> dal = yyyymmdd.get( rsc.stnName );
    if ( dal == null)
      return false;
    Collections.sort(dal, new CompareKeyDescend());

    rsc.yyyymmdd.addAll( dal );
    for ( String day : dal ) {
      sb.setLength( 0 );
      sb.append( rsc.stnName ).append( day );
      ArrayList<String> tal = hhmm.get( sb.toString() );
      rsc.hhmm.put( day, tal );
      if ( debug ) {
        for ( String hm : tal ) {
          sb.setLength( 0 );
          sb.append( day ).append( "_" ).append( hm );
          System.out.println( sb.toString() );
        }
      }
    }
    return true;
  }

  public RadarStationCollection queryStation( String stnName, String currentDay ) {
    RadarStationCollection rsc =  new RadarStationCollection( tdir, stnName, stnTime, product );
    if (getStationTimes( rsc, currentDay ) )
      return rsc;
    else
      return null;
  }

  public RadarStationCollection queryStation( String dir, String stnName, String product, String currentDay ) {
    RadarStationCollection rsc =  new RadarStationCollection( dir, stnName, stnTime,  product);
    getStationTimes( rsc, currentDay  );

    return rsc;
  }

  public String getTdir() {
    return tdir;
  }

  public boolean isStnTime() {
    return stnTime;
  }

  public boolean isStnProduct() {
    return stnProduct;
  }

  public String getProduct() {
    return product;
  }

  public boolean isStandardName() {
    return standardName;
  }

  public HashMap<String, ArrayList<String>> getYyyymmdd() {
    return yyyymmdd;
  }

  public boolean isCaseStudy() {
    return caseStudy;
  }

  public HashMap<String, ArrayList<String>> getHhmm() {
    return hhmm;
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
    Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
    Date now =  cal.getTime();
    String currentDay = dateFormat.format( now );
    RadarStationCollection rsc =  rdc.queryStation( tdir,  "KFTG",  product, currentDay);
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

    public int compare(String s1, String s2) {
      return s2.compareTo(s1);
    }
  }
}
