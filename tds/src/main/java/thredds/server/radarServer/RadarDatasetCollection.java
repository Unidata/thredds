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
import java.util.*;
import java.io.IOException;
import java.io.File;

/**
 * Maintains the Radar collection of days for a Radar Dataset.  The purpose
 * is to consolidate RadarDayCollection objects into one object.
 * 
 */
public class RadarDatasetCollection {

  public static final Pattern p_yyyymmdd_hhmm = Pattern.compile("\\d{8}_(\\d{4})");

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

    //this.stnTime = type;
    //this.yyyymmdd = yyyymmdd;
    this.product = product;

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
        if ( ! child.startsWith( "2"))  //TODO: changge back to .2
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
   * returns the information including times for this station in a RadarStationCollection object
   * @param station String
   */
  public boolean getStationTimes( String station ) {
    ArrayList<String> dal = yyyymmdd.get( station );
    Collections.sort(dal, new CompareKeyDescend());
    for ( String day : dal ) {
      ArrayList<String> tal = hhmm.get( station + day );
      for ( String hm : tal ) {
        System.out.println( day +"_"+ hm );
      }
    }
    return true;
  }
  
  public static void main(String[] args) throws IOException {

    String tdir = null;
    boolean type = true;
    String day = null;
    String product = null;
    if ( true || args.length == 4) {
      tdir = args[0];
      //type = (args[1].equals("true")) ? true : false;
      //day = args[2];
      product = (args[1].equals("null")) ? null : args[1];
    } else {
      System.out.println("Not the correct parameters: tdir, structType, day, product");
      return;
    }
    // create/populate
    RadarDatasetCollection rdc = new RadarDatasetCollection( tdir, product );
    System.out.println( "Dates for station KAMX" );
    rdc.getStationTimes( "KAMX");
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
