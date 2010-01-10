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

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: Robb
 * Date: Jan 9, 2010
 * Time: 12:08:55 PM
 */

/**
 * This class stores radar data file names information.
 * The class is serialized so it can later be retrieved by the RadarServer
 * servlet.
 */
public class RadarDayCollection implements Serializable {

  public static final Pattern p_yyyymmdd_hhmm = Pattern.compile("\\d{8}_(\\d{4})");

  /**
   * Base Directory of file names
   */
  String dir;

  /**
   * station/time type directory, typical level2 radar data
   */
  boolean stnTime = true;

  /**
   * product/station/time type directory, typical level3 radar data
   */
  boolean prodStnTime = false;

  /**
   * String yyyymmdd
   */
  String yyyymmdd;

  /**
   * Radar product, for level II this is null
   */
  String product = null;

  /**
   * Map of all the stations for this day, ArrayList of times
   */
  HashMap<String, ArrayList<String>> time = new HashMap<String, ArrayList<String>>();

  /**
   * constructors
   */
  public RadarDayCollection() {
  }

  public RadarDayCollection(String dir, boolean type, String yyyymmdd, String product) {
    this.dir = dir;
    this.stnTime = type;
    this.yyyymmdd = yyyymmdd;
    this.product = product;
  }

  /**
   * reads the data filename information
   *
   * @param dir      base directory of where to look for information
   * @param type     directory type
   * @param yyyymmdd of all stations in this directory
   * @param product  if level3 else null for level2
   * @return success
   * @throws IOException bad read
   */
  public boolean populate(String dir, boolean type, String yyyymmdd, String product)
      throws IOException {
    this.dir = dir;
    this.stnTime = type;
    this.yyyymmdd = yyyymmdd;
    this.product = product;

    ArrayList<String> stations = null;
    if (stnTime) {
      stations = getStationsFromDir(dir);
    }

    // get the times for each station
    for (String stn : stations) {
      populateStationsTimesFromDir(stn, dir + "/" + stn + "/" + yyyymmdd);
    }
    return true;
  }

  /*
  * returns and ArrayList of stations from a directory
  */
  private ArrayList getStationsFromDir(String stnDir) throws IOException {

    ArrayList<String> stations = new ArrayList<String>();
    File dir = new File(stnDir);
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String aChildren : children) {
        //System.out.println( "children i ="+ children[ i ]);
        File child = new File(dir, aChildren);
        //System.out.println( "child ="+ child.getName() );
        if (child.isDirectory()) {
          continue;
//        } else if (aChildren.endsWith(GribIndexName.oldSuffix ) ||
//            aChildren.endsWith(GribIndexName.currentSuffix) ||
//            aChildren.endsWith("xml") ||
//            aChildren.endsWith("tmp") || //index in creation process
//            aChildren.length() == 0) { // zero length file, ugh...
        } else {
          stations.add(child.getPath());
        }
      }
    } else {
      return null;
    }
    return stations;
  }

  /*
  * populates hhmm for a station
  */
  private boolean populateStationsTimesFromDir(String stn, String stnDir)
      throws IOException {

    File dir = new File(stnDir);
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      ArrayList<String> hhmm = new ArrayList<String>();
      String[] children = dir.list();
      for (String aChildren : children) {
        //System.out.println( "children i ="+ children[ i ]);
        File child = new File(dir, aChildren);
        //System.out.println( "child ="+ child.getName() );
        if (child.isDirectory()) {
          continue;
        } else {
          // Level2_KFTG_20100108_0654.ar2v
          Matcher m;
          m = p_yyyymmdd_hhmm.matcher(aChildren);
          if (m.find()) {
            hhmm.add(m.group(1));
          }
        }
      }
      time.put(stn, hhmm);
    } else {
      return false;
    }
    return true;
  }

  /**
   * get the stations
   *
   * @return the set of stations
   */
  public final java.util.Set<String> getKeys() {
    return time.keySet();
  }

  /**
   * list of hhmm for this station
   *
   * @param station station times
   * @return times ArrayList
   */
  public final ArrayList getTimes(String station) {
    return time.get(station);
  }

  /**
   * write out this object
   *
   * @return String filename of write.
   */
  public String write() {

    String filename = dir + "/." + yyyymmdd;
    FileOutputStream fos = null;
    ObjectOutputStream out = null;
    try {
      fos = new FileOutputStream(filename);
      out = new ObjectOutputStream(fos);
      out.writeObject(this);
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    return filename;  // successful serialization
  }

  /**
   * read / return object
   *
   * @return RadarDayCollection object
   */
  public RadarDayCollection read(String sfile) {
    RadarDayCollection rdc = null;
    FileInputStream fis = null;
    ObjectInputStream in = null;
    try {
      fis = new FileInputStream(sfile);
      in = new ObjectInputStream(fis);
      rdc = (RadarDayCollection) in.readObject();
      in.close();
      return rdc;
    } catch (IOException ex) {
      ex.printStackTrace();
    } catch (ClassNotFoundException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public static void main(String[] args) throws IOException {

    String tdir = "/data/ldm/pub/native/radar/level2/";
    if (args.length > 0) {
      tdir = args[0];
    }
    // create/populate/write
    RadarDayCollection rdc = new RadarDayCollection();
    rdc.populate("/data/ldm/pub/native/radar/level2/", true, "20100110", null);
    String sfile = rdc.write();
    if (sfile != null)
      System.out.println("Write successful");

    // read the RadarDayCollection
    //RadarDayCollection rdc = null;
    rdc = null;
    FileInputStream fis = null;
    ObjectInputStream in = null;
    try {
      fis = new FileInputStream(sfile);
      in = new ObjectInputStream(fis);
      rdc = (RadarDayCollection) in.readObject();
      in.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    } catch (ClassNotFoundException ex) {
      ex.printStackTrace();
    }

  }
}
