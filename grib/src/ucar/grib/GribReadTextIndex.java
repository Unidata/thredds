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
 *
 * By:   Robb Kambic
 * Date: Feb 10, 2009
 * Time: 9:32:38 AM
 *
 */

package ucar.grib;

import ucar.grid.GridIndex;
import ucar.grid.GridDefRecord;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.net.URL;


/**
 * Read an old style text index and returns a GridIndex
 */
public class GribReadTextIndex {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribReadTextIndex.class);
  static private boolean debugTiming = false;
  static private boolean debugParse = false;

  /**
   * Current Text index version. If before this version, the gdsKey needs to be reduced to an int.
   * Also the Text index version has to be < 7.0
   */
  static public String currentTextIndexVersion = "6.5";

  private final java.text.SimpleDateFormat dateFormat;
  private final Calendar calendar;

  /**
   * Constructor for reading an existing text Index and making a GridIndex object.
   */
  public GribReadTextIndex() {
    dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC

    calendar = Calendar.getInstance();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  /**
   * open Grib Index file for scanning.
   *
   * @param location URL or local filename of Grib Index file
   * @return false if does not match current version; you should regenerate it in that case.
   * @throws java.io.IOException on read error
   */
  public GridIndex open(String location) throws IOException {
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
   * @return gridIndex.
   * @throws java.io.IOException on read error
   */
  public GridIndex open(String location, InputStream ios) throws IOException {

    long start = System.currentTimeMillis();

    GridIndex gridIndex = new GridIndex();
    BufferedReader dataIS = null;
    boolean old_index_version = false;
    try {
      dataIS = new BufferedReader(new InputStreamReader(ios));

      // section 1 - global attributes
      String centerS = null, sub_centerS = null, table_versionS = null;
      while (true) {
        String line = dataIS.readLine();
        if (line == null || line.length() == 0) { // 0 length/corrupted index
          return gridIndex;
        }
        if (line.startsWith("--")) {
          break;
        }

        int pos = line.indexOf(" = ");
        if (pos > 0) {
          String key = line.substring(0, pos);
          String value = line.substring(pos + 3).replaceAll( " ", "%20" );
          gridIndex.addGlobalAttribute(key, value);
          if (key.equals("center")) {
            centerS = value;
          } else if (key.equals("sub_center")) {
            sub_centerS = value;
          } else if (key.equals("table_version")) {
            table_versionS = value;
          } else if (key.equals("index_version")) {
            old_index_version = ! value.startsWith( currentTextIndexVersion );
          }
        }
      }

      // section 2 -- grib records
      while (true) {
        String line = dataIS.readLine();
        if (line == null || line.length() == 0) { // 0 length/corrupted index
          return gridIndex;
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
        if( old_index_version )
          gdsKey = Integer.toString( gdsKey.hashCode() );
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
            : centerS;
        String subCenter = stoke.hasMoreTokens()
            ? stoke.nextToken()
            : sub_centerS;
        String table = stoke.hasMoreTokens()
            ? stoke.nextToken()
            : table_versionS;

        GribGridRecord ggr = new GribGridRecord(calendar, dateFormat,
            productType,
            discipline, category,
            param, typeGenProcess, levelType1,
            levelValue1, levelType2,
            levelValue2, refTime, foreTime,
            gdsKey, offset1, offset2,
            decimalScale, bmsExists,
            center, subCenter, table);

        gridIndex.addGridRecord(ggr);
        if (debugParse) {
          System.out.println(ggr.toString());
        }
      }

      // section 3+ - GDS
      GribGridDefRecord gds;
      StringBuilder sb = new StringBuilder();
      while (true) {
        String line = dataIS.readLine();
        if (line == null || line.length() == 0) {
          break;
        }
        if (line.startsWith("--")) {
          gds = new GribGridDefRecord(sb.toString());
          sb.setLength(0); //reset it
          gridIndex.addHorizCoordSys(gds);
          continue;
        }
        int pos = line.indexOf(" = ");
        // need to convert long GDSkey to int
        if ( line.startsWith("GDSkey")) {
          if( old_index_version ) {
            int hc = line.substring(pos + 3).hashCode();
            sb.append(line.substring(0, pos)).append("\t").append(Integer.toString(hc));
          } else {
            sb.append(line.substring(0, pos)).append("\t").append(line.substring(pos + 3));
          }
          continue;
        }
        if (pos > 0) {
          sb.append("\t").append(line.substring(0, pos)).append("\t").append(line.substring(pos + 3));
        }
      }
      // remove the Grib1 and Grib2 text indexes differences
      String gdsStr = sb.toString();
      if( ! gdsStr.contains( GridDefRecord.RADIUS_SPHERICAL_EARTH ))
        gdsStr = gdsStr.replace( "radius_spherical_earth", GridDefRecord.RADIUS_SPHERICAL_EARTH);
      gds = new GribGridDefRecord( gdsStr );
      gridIndex.addHorizCoordSys( gds );

      //dataIS.close();

      if (debugTiming) {
        long took = System.currentTimeMillis() - start;
        System.out.println(" Index read " + location + " count="
            + gridIndex.getGridCount() + " took=" + took + " msec ");
      }
      log.debug("Text index read: " + location);
      log.debug("Number Records =" + gridIndex.getGridCount() + " at " +
          dateFormat.format(Calendar.getInstance().getTime()));
      return gridIndex;

    } catch (IOException e) {
      log.error("open(): reading text index " + "[" + location + "]");
      throw new IOException(e);

    } finally {
      if (dataIS != null)
        dataIS.close();
    }

  }
  /**
   * testing
   *
   * @param args index to read
   * @throws java.io.IOException on read error
   */
  static public void main(String[] args) throws IOException {

    File gbx = new File(  "C:/data/NDFD.grib2.gbx" );
    if( ! gbx.exists() ) { // work machine
      gbx = new File( "/local/robb/data/grib/ruc_sample.grib2.gbx");
    }

    //debugTiming = true;
    //debugParse = true;
    GridIndex index;
    if (args.length < 1) {
      index = new GribReadTextIndex().open(gbx.getPath());
    } else {
      index = new GribReadTextIndex().open(args[0]);
    }
    if (debugTiming)
      return;

    // test GDS conversions to int and double
//    List<GridDefRecord> gcs = index.getHorizCoordSys();
//    for( GridDefRecord gdr : gcs ) {
//      String wind = gdr.getParam( "Winds" );
//      int nx = gdr.getParamInt( "Nx");
//      System.out.println( "Nx ="+ nx );
//      nx = gdr.getParamInt( "Nx");
//      System.out.println( "Nx ="+ nx );
//      double la1 = gdr.getParamDouble( "La1");
//      System.out.println( "La1 ="+ la1 );
//      la1 = gdr.getParamDouble( "La1");
//      System.out.println( "La1 ="+ la1 );
//    }
  }
}
