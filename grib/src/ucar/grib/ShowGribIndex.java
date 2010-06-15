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
 * Date: Feb 13, 2009
 * Time: 12:31:28 PM
 *
 */

package ucar.grib;

import ucar.grib.grib2.Grib2WriteIndex;
import ucar.grib.grib1.*;
import ucar.grid.GridDefRecord;
import ucar.grid.GridIndex;
import ucar.grid.GridRecord;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * Displays the binary or text index to stdout
 */

public class ShowGribIndex {
  /**
   * _more_
   */
  static private boolean debugTiming = true;

  /**
   * _more_
   */
  static private boolean debugParse = false;

  private java.text.SimpleDateFormat dateFormat;
  private static String divider =
      "------------------------------------------------------------------";

  /**
   * Constructor for displaying an Index from the Grib file.
   */
  public ShowGribIndex() {
    dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC
  }

  /**
   * show Grib Index file.
   *
   * @param location URL or local filename of Grib Index file
   * @throws java.io.IOException
   */
  public final void show(String location) throws IOException {
    try {
      GridIndex index = new GribReadIndex().open(location);
      Map<String, String> attrs = index.getGlobalAttributes();
      System.out.println("index_version = " + attrs.get("index_version"));
      System.out.println("grid_edition = " + attrs.get("grid_edition"));
      System.out.println("location = " + attrs.get("location"));
      System.out.println("length = " + attrs.get("length"));
      System.out.println("created = " + attrs.get("created"));
      System.out.println("center = " + attrs.get("center"));
      System.out.println("sub_center = " + attrs.get("sub_center"));
      System.out.println("table_version = " + attrs.get("table_version"));
      String basetime = attrs.get("basetime");
      System.out.println("basetime = " + basetime);
      System.out.println("ensemble = " + attrs.get("ensemble"));
 
      System.out.println("-----------------------------------------------------------------");

      List<GridRecord> records = index.getGridRecords();
      for (GridRecord gr : records) {
        GribGridRecord ggr = (GribGridRecord) gr;
        System.out.println(ggr.productTemplate + " " + ggr.discipline + " " +
            ggr.category + " " + ggr.paramNumber + " " +
            ggr.typeGenProcess + " " + ggr.levelType1 + " " +
            ggr.levelValue1 + " " + ggr.levelType2 + " " +
            //ggr.levelValue2 + " " + dateFormat.format(ggr.getValidTime()) + " " +
            ggr.levelValue2 + " " + basetime + " " +
            ggr.forecastTime + " " + ggr.gdsKey + " " + ggr.offset1 + " " + ggr.offset2 + " " +
            ggr.decimalScale + " " + ggr.bmsExists + " " + ggr.center + " " +
            ggr.subCenter + " " + ggr.table + " " +
            ggr.type + " " + ggr.numberForecasts + " " + ggr.lowerLimit + " " + ggr.upperLimit);
      }

      System.out.println("-----------------------------------------------------------------");

      List<GridDefRecord> gdrs = index.getHorizCoordSys();

      for (GridDefRecord gdr : gdrs) {
        System.out.println(GridDefRecord.GDS_KEY + " = " + gdr.getParam(GridDefRecord.GDS_KEY));
        System.out.println(GridDefRecord.GRID_TYPE + " = " + gdr.getParamInt(GridDefRecord.GRID_TYPE));
        System.out.println(GridDefRecord.GRID_NAME + " = " + gdr.getParam(GridDefRecord.GRID_NAME));

        int shape = gdr.getParamInt(GridDefRecord.GRID_SHAPE_CODE);
        System.out.println(GridDefRecord.GRID_SHAPE_CODE + " = " + shape);
        System.out.println(GridDefRecord.GRID_SHAPE + " = " + gdr.getParam(GridDefRecord.GRID_SHAPE));
        if (shape < 2 || shape == 6 || shape == 8) {
          System.out.println(GridDefRecord.RADIUS_SPHERICAL_EARTH + " = " + gdr.getParam(GridDefRecord.RADIUS_SPHERICAL_EARTH));
        } else if ((shape > 1) && (shape < 6) || shape == 7) {
          System.out.println(GridDefRecord.MAJOR_AXIS_EARTH + " = " + gdr.getParam(GridDefRecord.MAJOR_AXIS_EARTH));
          System.out.println(GridDefRecord.MINOR_AXIS_EARTH + " = " + gdr.getParam(GridDefRecord.MINOR_AXIS_EARTH));
        }
        
        System.out.println(GridDefRecord.NX + " = " + gdr.getParam(GridDefRecord.NX));
        System.out.println(GridDefRecord.NY + " = " + gdr.getParam(GridDefRecord.NY));
        System.out.println(GridDefRecord.LA1 + " = " + gdr.getParam(GridDefRecord.LA1));
        System.out.println(GridDefRecord.LO1 + " = " + gdr.getParam(GridDefRecord.LO1));
        System.out.println(GridDefRecord.DX + " = " + gdr.getParam(GridDefRecord.DX));
        System.out.println(GridDefRecord.DY + " = " + gdr.getParam(GridDefRecord.DY));

        // Dump rest of variables
        java.util.Set<String> keys = gdr.getKeys();
        List<String> keylist = new ArrayList( keys );
        Collections.sort( keylist, new CompareKeyAscend() );
        for( String key : keylist ) {
          if( key.startsWith( "grid") || key.startsWith( "Dx") || key.startsWith( "Dy")
            || key.startsWith( "La1") || key.startsWith( "Lo1")|| key.startsWith( "GDS" )
            || key.startsWith( "Nx") || key.startsWith( "Ny") )
            continue;
          System.out.println( key +" = "+ gdr.getParam( key ) );
        }

        System.out.println(GridDefRecord.GRID_UNITS + " = " + gdr.getParam(GridDefRecord.GRID_UNITS));
      }

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * main.
   *
   * @param args index to read
   * @throws IOException
   */
  static public void main(String[] args) throws IOException {

    final String testName = "C:/data/NDFD.grib2" + GribIndexName.currentSuffix;

    debugTiming = false;
    String gbxName = null;
    if (args.length < 1) {
      new ShowGribIndex().show(testName);
    } else if (args[0].endsWith(GribIndexName.oldSuffix) ||
        args[0].endsWith(GribIndexName.currentSuffix)) {
      File gbx = new File(args[0]);
      if (gbx.exists()) {
        new ShowGribIndex().show(gbx.getPath());
        return;
      }
    } else {  // no suffix given
      gbxName = GribIndexName.getCurrentSuffix(args[0]);
      File gbx = new File(gbxName);
      if (gbx.exists()) {
        new ShowGribIndex().show(gbx.getPath());
        return;
      }
    }

    // create/show index
    String gribName = args[0].replaceAll(GribIndexName.currentSuffix, "");
    gbxName = GribIndexName.getCurrentSuffix(gribName);
    ucar.unidata.io.RandomAccessFile raf = new RandomAccessFile(gribName, "r");
    int edition = GribChecker.getEdition(raf);
    if (edition == 1) {
      Grib1WriteIndex.main(new String[]{gribName, gbxName});
    } else if (edition == 2) {
      Grib2WriteIndex.main(new String[]{gribName, gbxName});
    } else {
      System.out.println("Not a Grib file");
      return;
    }
    new ShowGribIndex().show(gbxName);
  }

  protected class CompareKeyAscend implements Comparator<String> {
    /*
    public int compare(Object o1, Object o2) {
      String s1 = (String) o1;
      String s2 = (String) o2;

      return s2.compareTo(s1);
    }
    */
    public int compare(String s1, String s2) {
      return s1.compareTo(s2);
    }
  }
}
