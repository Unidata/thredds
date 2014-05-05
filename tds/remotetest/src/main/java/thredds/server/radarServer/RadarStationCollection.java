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
 * Date: Jan 19, 2010
 * Time: 3:06:26 PM
 */

package thredds.server.radarServer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.io.*;

public class RadarStationCollection {

  private static final long serialVersionUID = 20100119L;

  /**
   * Base Directory of file names
   */
  String dir;

  /**
   * station/time type directory, typical IDD level2 radar data
   * or
   * product/station/time directory, typical IDD level3 radar data
   */
  boolean stnTime = true;

  /**
   * station/product/time type directory, typical Gempak radar data
   */
  boolean stnProduct  = false;

  /**
   * String stnName
   */
  String stnName;

  /**
   * Radar product, for level II this is null
   */
  String product = null;

  /**
   * Standard Product Naming ie Level2_KRLX_20100112_1324.ar2v
   */
  boolean standardName = true;

   /**
   * Radar product suffix
   */
  String suffix = ".ar2v";

  /**
   *  ArrayList of yyyyddmm days
   */
  ArrayList<String> yyyymmdd = new ArrayList<String>();

  /**
   *  Map with key day, ArrayList of times for the day
   */
  HashMap<String, ArrayList<String>> hhmm = new HashMap<String, ArrayList<String>>();

  /**
   * constructors
   */
  public RadarStationCollection() {
  }

  public RadarStationCollection(String dir, String stnName, boolean type, String product) {

    StringBuffer sb = new StringBuffer( dir );
    this.stnTime = type;
    this.stnName = stnName;
    this.product = product;

    if( stnTime ) {
      if (product == null) {
        this.dir = dir;
      } else {
        this.dir = sb.append( "/" ).append( product ).toString();
      }
    } else { //  TODO: need test case
      if (product == null) {
        this.dir = dir;
      } else {
        this.dir = sb.append( "/" ).append( product ).append( "/" ).toString();
      }
    }

  }

  /**
   * list of days for this station
   *
   * @return times ArrayList
   */
  public final ArrayList<String> getDays() {
    return yyyymmdd;
  }

  /**
   * list of hhmm for this day
   * @param day String
   * @return hhmm's ArrayList
   */
  public final ArrayList<String> getHourMinute( String day ) {
    return hhmm.get( day );
  }

  /**
   * Base Directory of file names
   * @return dir String
   */
  public String getDir() {
    return dir;
  }

  /**
   * station/time type directory, typical IDD level2 radar data
   * or
   * product/station/time directory, typical IDD level3 radar data
   * @return stnTime boolean
   */
  public boolean isStnTime() {
    return stnTime;
  }

  /**
   * station/product/time type directory, typical Gempak radar data
   * @return stnProduct boolean
   */
  public boolean isStnProduct() {
    return stnProduct;
  }

  /**
   * String stnName
   * @return stnName String
   */
  public String getStnName() {
    return stnName;
  }

  /**
   * Radar product, for level II this is null
   * @return product for this dataset
   */
  public String getProduct() {
    return product;
  }

  /**
   * Standard Product Naming ie Level2_KRLX_20100112_1324.ar2v
   * @return standardName IDD naming convention
   */
  public boolean isStandardName() {
    return standardName;
  }

  public ArrayList<String> getYyyymmdd() {
    return yyyymmdd;
  }

  public HashMap<String, ArrayList<String>> getHhmm() {
    return hhmm;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public void setStnTime(boolean stnTime) {
    this.stnTime = stnTime;
  }

  public void setStnProduct(boolean stnProduct) {
    this.stnProduct = stnProduct;
  }

  public void setStnName(String stnName) {
    this.stnName = stnName;
  }

  public void setProduct(String product) {
    this.product = product;
  }

  public void setStandardName(boolean standardName) {
    this.standardName = standardName;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public void setYyyymmdd(ArrayList<String> yyyymmdd) {
    this.yyyymmdd = yyyymmdd;
  }

  public void setHhmm(HashMap<String, ArrayList<String>> hhmm) {
    this.hhmm = hhmm;
  }

  /**
   * write out this object
   *
   * @return success of write.
   */
  public boolean write( PrintStream ps ) {

    ObjectOutputStream out = null;
    try {
      out = new ObjectOutputStream( ps );
      out.writeObject(this);
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    return true;  // successful serialization
  }

  /**
   * read / return object
   *
   * @param sfile  Serialized file
   * @return RadarStationCollection object
   */
  public RadarStationCollection read(String sfile) {
    RadarStationCollection rdc = null;
    FileInputStream fis = null;
    ObjectInputStream in = null;
    try {
      fis = new FileInputStream(sfile);
      in = new ObjectInputStream(fis);
      rdc = (RadarStationCollection) in.readObject();
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

    String tdir = null;
    boolean type = true;
    String day = null;
    String product = null;
    if (args.length == 4) {
      tdir = args[0];
      type = (args[1].equals("true")) ;
      day = args[2];
      product = (args[3].equals("null")) ? null : args[3];
    } else {
      System.out.println("Not the correct parameters: tdir, structType, day, product");
      return;
    }
    RadarStationCollection rdc = new RadarStationCollection();
  }

  protected class CompareKeyDescend implements Comparator<String> {
    public int compare(String s1, String s2) {
      return s2.compareTo(s1);
    }
  }
}
