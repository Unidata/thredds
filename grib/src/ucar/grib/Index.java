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

// $Id: Index.java,v 1.24 2006/08/04 17:59:32 rkambic Exp $


package ucar.grib;


import java.io.*;

import java.net.URL;

import java.util.*;
import java.text.ParseException;


/**
 * An "in memory" index for Grib (ver 1 or 2) files. May be constructed by
 * scanning the file with GribInput, or by
 * reading a "Grib File Index" that was created by GribIndexer.
 * <p/>
 * This has all the info in it needed to construct a netcdf object.
 * see <a href="../../IndexFormat.txt"> IndexFormat.txt</a>
 * .
 * @deprecated
 * @author caron
 */
public final class Index {

  /**
   * _more_
   */
  static private boolean debugTiming = false;

  /**
   * _more_
   */
  static private boolean debugParse = false;

  /**
   * used to check versions of already created indexes.
   */
  static public final String current_index_version = "6.5";

  /**
   * contains GribRecords of the index that has fields: productType, discipline,
   * category, param, levelType1, levelValue1, levelType2, levelValue2,
   * refTime, foreTime, gdsKey, offset1, offset2, decimalScale, bmsExists.
   */
  private final List<GribRecord> index = new ArrayList<GribRecord>();

  /**
   * contains gdsRecords (GDS) of the Index.
   */
  private final List<GdsRecord> gcs = new ArrayList<GdsRecord>();

  /**
   * contains global attributes of the Index.
   */
  private final Map<String, String> atts = new HashMap<String, String>();

  private java.text.SimpleDateFormat dateFormat;

  /**
   * Constructor for creating an Index from the Grib file.
   * Use the addXXX() methods.
   */
  public Index() {
    dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC
  }

  /**
   * open Grib Index file for scanning.
   *
   * @param location URL or local filename of Grib Index file
   * @return false if does not match current version; you should regenerate it in that case.
   * @throws IOException
   */
  public final boolean open(String location) throws IOException {
    InputStream ios;
    if (location.startsWith("http:")) {
      URL url = new URL(location);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(location);
    }

    return open(location, ios);
  }

  /**
   * open Grib Index file for scanning.
   *
   * @param location URL or local filename of Grib Index file
   * @param ios      input stream
   * @return false if does not match current version; you should regenerate it in that case.
   * @throws IOException
   */
  public final boolean open(String location, InputStream ios)
          throws IOException {

    long start = System.currentTimeMillis();

    BufferedReader dataIS =
            new BufferedReader(new InputStreamReader(ios));

    // section 1 - global attributes
    //boolean versionOk = false;
    while (true) {
      String line = dataIS.readLine();
      if (line == null || line.length() == 0) { // 0 length/corrupted index
        return false;
      }
      if (line.startsWith("--")) {
        break;
      }

      int pos = line.indexOf(" = ");
      if (pos > 0) {
        String key = line.substring(0, pos);
        String value = line.substring(pos + 3);
        atts.put(key, value);
        //if (key.equals("index_version")) {
        //    versionOk = value.equalsIgnoreCase(current_index_version);
        //}

      }
    }
    //if ( !versionOk) {
    //    return false;
    //}

    // section 2 -- grib records
    while (true) {
      String line = dataIS.readLine();
      if (line == null || line.length() == 0) { // 0 length/corrupted index
        return false;
      }
      if (line.startsWith("--")) {
        break;
      }

      StringTokenizer stoke = new StringTokenizer(line);

      String productType = stoke.nextToken();
      String discipline = stoke.nextToken();
      String category = stoke.nextToken();
      String param = stoke.nextToken();
      String typeGenProcess = stoke.nextToken();
      String levelType1 = stoke.nextToken();
      String levelValue1 = stoke.nextToken();
      String levelType2 = stoke.nextToken();
      String levelValue2 = stoke.nextToken();
      String refTime = stoke.nextToken();
      String foreTime = stoke.nextToken();
      String gdsKey = stoke.nextToken();
      String offset1 = stoke.nextToken();
      String offset2 = stoke.nextToken();
      String decimalScale = stoke.hasMoreTokens()
              ? stoke.nextToken()
              : null;
      String bmsExists = stoke.hasMoreTokens()
              ? stoke.nextToken()
              : null;
      String center = stoke.hasMoreTokens()
              ? stoke.nextToken()
              : null;
      String subCenter = stoke.hasMoreTokens()
              ? stoke.nextToken()
              : null;
      String table = stoke.hasMoreTokens()
              ? stoke.nextToken()
              : null;

      GribRecord s = new GribRecord(productType, discipline, category,
              param, typeGenProcess, levelType1,
              levelValue1, levelType2,
              levelValue2, refTime, foreTime,
              gdsKey, offset1, offset2,
              decimalScale, bmsExists,
              center, subCenter, table);

      index.add(s);

      if (debugParse) {
        System.out.println(s.typeGenProcess);
      }
    }

    // section 3+ - GDS
    GdsRecord gds = new GdsRecord();
    gcs.add(gds);

    while (true) {
      String line = dataIS.readLine();
      if (line == null) {
        break;
      }
      if (line.length() == 0) {
        return false;
      }
      if (line.startsWith("--")) {
        gds.finish();
        gds = new GdsRecord();
        gcs.add(gds);
        continue;
      }

      int pos = line.indexOf(" = ");
      if (pos > 0) {
        gds.addParam(line.substring(0, pos), line.substring(pos + 3));
      }
    }
    gds.finish();

    dataIS.close();

    if (debugTiming) {
      long took = System.currentTimeMillis() - start;
      System.out.println(" Index read " + location + " count="
              + index.size() + " took=" + took + " msec ");
    }
    return true;

  }

  /**
   * GlobalAttributes of index.
   *
   * @return HashMap of type GlobalAttributes.
   */
  public final Map<String, String> getGlobalAttributes() {
    return atts;
  }

   public final String getGlobalAttribute(String key) {
    return atts.get(key);
  }

  /**
   * Grib records of index, one for each parameter.
   *
   * @return list of type GribRecord.
   */
  public final List<GribRecord> getGribRecords() {
    return index;
  }

  /**
   * GDSs of the index.
   *
   * @return list of type GdsRecord.
   */
  public final List<GdsRecord> getHorizCoordSys() {
    return gcs;
  }

  /**
   * adds a GribRecord to the index.
   *
   * @param gr GribRecord
   */
  public final void addGribRecord(GribRecord gr) {
    index.add(gr);
  }

  /**
   * adds a GdsRecord to the index.
   *
   * @param gds GdsRecord
   */
  public final void addHorizCoordSys(GdsRecord gds) {
    gcs.add(gds);
  }

  /**
   * adds a GlobalAttribute to the index.
   *
   * @param name  GlobalAttribute
   * @param value String
   */
  public final void addGlobalAttribute(String name, String value) {
    atts.put(name, value);
  }

  /**
   * _more_
   *
   * @return _more_
   */
  public GribRecord getGribRecord() {
    return new GribRecord();
  }

  /**
   * class to represent each record (parameter) in the index.
   * purpose is to convert from String representation to native value.
   */
  public final class GribRecord {

    /**
     * _more_
     */
    public int productType, discipline, category, paramNumber;

    /**
     * _more_
     */
    public String typeGenProcess;

    /**
     * _more_
     */
    public int levelType1, levelType2;

    /**
     * _more_
     */
    public float levelValue1, levelValue2;

    /**
     * _more_
     */
    public String gdsKey;

    /**
     * _more_
     */
    public long offset1;

    /**
     * _more_
     */
    public long offset2;

    /**
     * _more_
     */
    public Date refTime;

    /**
     * _more_
     */
    public int forecastTime;

    /**
     * _more_
     */
    public int decimalScale = 0;

    /**
     * _more_
     */
    public boolean bmsExists = true;

    /**
     * _more_
     */
    public int center, subCenter, table;


    /**
     * constructor.
     */
    public GribRecord() {
    }

    /**
     * constructor given all parameters as Strings.
     *
     * @param productTypeS
     * @param disciplineS
     * @param categoryS
     * @param paramS
     * @param typeGenProcessS
     * @param levelType1S
     * @param levelValue1S
     * @param levelType2S
     * @param levelValue2S
     * @param refTime
     * @param foreTimeS
     * @param gdsKeyS
     * @param offset1S
     * @param offset2S
     * @param decimalScaleS
     * @param bmsExistsS      either true or false bit-map exists
     * @param centerS
     * @param subCenterS
     * @param tableS
     */
    GribRecord(String productTypeS, String disciplineS, String categoryS,
            String paramS, String typeGenProcessS, String levelType1S,
            String levelValue1S, String levelType2S,
            String levelValue2S, String refTime, String foreTimeS,
            String gdsKeyS, String offset1S, String offset2S,
            String decimalScaleS, String bmsExistsS,
            String centerS, String subCenterS, String tableS) {

      try {
        this.refTime = dateFormat.parse( refTime);
        this.gdsKey = gdsKeyS.trim().intern();

        productType = Integer.parseInt(productTypeS);
        discipline = Integer.parseInt(disciplineS);
        category = Integer.parseInt(categoryS);
        paramNumber = Integer.parseInt(paramS);
        typeGenProcess = typeGenProcessS.intern();
        levelType1 = Integer.parseInt(levelType1S);
        levelValue1 = Float.parseFloat(levelValue1S);
        levelType2 = Integer.parseInt(levelType2S);
        levelValue2 = Float.parseFloat(levelValue2S);
        forecastTime = Integer.parseInt(foreTimeS);
        offset1 = Long.parseLong(offset1S);
        offset2 = Long.parseLong(offset2S);
        if (decimalScaleS != null) {
          decimalScale = Integer.parseInt(decimalScaleS);
        }
        if (bmsExistsS != null) {
          bmsExists = bmsExistsS.equals("true");
        }
        if (centerS != null) {
          center = Integer.parseInt(centerS);
        }
        if (subCenterS != null) {
          subCenter = Integer.parseInt(subCenterS);
        }
        if (tableS != null) {
          table = Integer.parseInt(tableS);
        }
      } catch (NumberFormatException e) {
        throw new RuntimeException(e);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * _more_
     */
    private Date validTime = null;

    /**
     * _more_
     *
     * @return _more_
     */
    public Date getValidTime() {
      return validTime;
    }

    /**
     * _more_
     *
     * @param t _more_
     */
    public void setValidTime(Date t) {
      validTime = t;
    }
  }

  /**
   * class to represent GDS in the index.
   * purpose is to convert from String representation to native value.
   */
  static public final class GdsRecord {

    /**
     * _more_
     */
    public HashMap params = new HashMap();

    /**
     * _more_
     */
    public String gdsKey, winds;

    /**
     * _more_
     */
    public int grid_type, nx, ny, resolution;

    /**
     * _more_
     */
    public double dx, dy;

    /**
     * _more_
     */
    public double latin1, latin2, La1, Lo1, LaD, LoV;

    /**
     * _more_
     */
    public int grid_shape_code;

    /**
     * _more_
     */
    public double radius_spherical_earth, major_axis_earth,
            minor_axis_earth;

    // --Commented out by Inspection START (12/8/05 12:48 PM):
    //  /**
    //  * Given the name of the param return the value
    //  * @param name of the param
    //  */
    //    public final String getValue( String name) {
    //      return (String) params.get( name);
    //    }
    // --Commented out by Inspection STOP (12/8/05 12:48 PM)

    /**
     * constructor.
     */
    public GdsRecord() {
    }

    /**
     * adds a param and value.
     *
     * @param key   name of the param
     * @param value of the param
     */
    public final void addParam(String key, String value) {
      if (debugParse) {
        System.out.println(" adding " + key + " = " + value);
      }
      params.put(key.trim(), value);
    }

    /**
     * _more_
     */
    final void finish() {

      try {
        gdsKey = ((String) params.get("GDSkey")).trim();

        grid_type =
                Integer.parseInt((String) params.get("grid_type"));
        grid_shape_code =
                Integer.parseInt((String) params.get("grid_shape_code"));
        nx = Integer.parseInt((String) params.get("Nx"));
        ny = Integer.parseInt((String) params.get("Ny"));

        dx = readDouble("Dx");
        dy = readDouble("Dy");
        latin1 = readDouble("Latin1");
        latin2 = readDouble("Latin2");
        La1 = readDouble("La1");
        Lo1 = readDouble("Lo1");
        LaD = readDouble("LaD");
        LoV = readDouble("LoV");
        winds = (String) params.get("Winds");
        if (winds != null)
          winds = winds.trim();

        radius_spherical_earth = readDouble("radius_spherical_earth");
        // done because some values are radius_spherical_earth and other are
        // grid_radius_spherical_earth
        if ( Double.isNaN( radius_spherical_earth ))
          radius_spherical_earth = readDouble("grid_radius_spherical_earth");
        major_axis_earth = readDouble("major_axis_earth");
        if ( Double.isNaN( major_axis_earth ))
          major_axis_earth = readDouble("grid_major_axis_earth");
        minor_axis_earth = readDouble("minor_axis_earth");
        if ( Double.isNaN( minor_axis_earth ))
          minor_axis_earth = readDouble("grid_minor_axis_earth");

      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }

    /**
     * returns the value of the param.
     *
     * @param name
     * @return value, or NaN if value doest exist
     */
    public final double readDouble(String name) {
      String s = (String) params.get(name);
      if (s == null) {
        return Double.NaN;
      }
      try {
        return Double.parseDouble(s);
      } catch (NumberFormatException e) {
        e.printStackTrace();
        return Double.NaN;
      }
    }
  }

  /**
   * main.
   *
   * @param args empty
   * @throws IOException
   */
  static public void main(String[] args) throws IOException {

    final String testName = "/home/rkambic/code/grib/20070420_1800.grib2.gbx";

    //debugTiming = true;
    //debugParse = true;
    if (args.length < 1) {
      new Index().open(testName);
    } else {
      new Index().open(args[0]);
    }
  }
}

