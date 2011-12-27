/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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
package thredds.inventory;

import java.util.*;
import java.util.regex.Pattern;
import java.io.File;

import net.jcip.annotations.ThreadSafe;
import ucar.unidata.util.StringUtil2;

/**
 * Parses the collection specification string.
 * <p>the idea  is that one copies the full path of an example dataset, then edits it</p>
 * <p>Example: "/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/** /GFS_Alaska_191km_#yyyyMMdd_HHmm#\.grib1$"</p>
 * <ul>
 * <li> rootDir ="/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km"/</li>
 * <li>    subdirs=true (because ** is present) </li>
 * <li>    dateFormatMark="GFS_Alaska_191km_#yyyyMMdd_HHmm"</li>
 * <li>    regExp='GFS_Alaska_191km_.............\.grib1$</li>
 * </ul>
 * <p>Example: "Q:/grid/grib/grib1/data/agg/.*\.grb"</p>
 * <ul>
 * <li> rootDir ="Q:/grid/grib/grib1/data/agg/"/</li>
 * <li>    subdirs=false</li>
 * <li>    dateFormatMark=null</li>
 * <li>    useName=yes</li>
 * <li>    regexp= ".*\.grb" (anything ending with .grb)</li>
 * </ul>
 *
 * @see "http://www.unidata.ucar.edu/projects/THREDDS/tech/tds4.2/reference/collections/CollectionSpecification.html"
 * @author caron
 * @since Jul 7, 2009
 */
@ThreadSafe
public class CollectionSpecParser {
  private final String spec;
  private final String rootDir;
  private final boolean subdirs; // recurse into subdirectories under the root dir
  private final java.util.regex.Pattern filter; // regexp filter
  private final String dateFormatMark;
  //private final boolean useName; // true = use name, false = use path for dateFormatMark

  /**
   * Single spec : "/topdir/** /#dateFormatMark#regExp"
   * This only allows the dateFormatMark to be in the file name, not anywhere else in the filename path,
   *  and you cant use any part of the dateFormat to filter on.
   * @param collectionSpec the collection Spec
   * @param errlog put error messages here
   */
  public CollectionSpecParser(String collectionSpec, Formatter errlog) {
    this.spec = collectionSpec.trim();
    int posFilter;

    int posGlob = collectionSpec.indexOf("/**/");
    if (posGlob > 0) {
      rootDir = collectionSpec.substring(0, posGlob);
      posFilter = posGlob + 3;
      subdirs = true;

    } else {
      subdirs = false;
      posFilter = collectionSpec.lastIndexOf('/');
      if (posFilter > 0)
        rootDir = collectionSpec.substring(0, posFilter);
      else
        rootDir = System.getProperty("user.dir"); // working directory
    }

    File locFile = new File(rootDir);
    if (!locFile.exists()) {
      errlog.format(" Directory %s does not exist %n", rootDir);
    }

    // optional filter
    String filter = null;
    if (posFilter < collectionSpec.length() - 2)
      filter = collectionSpec.substring(posFilter + 1); // remove topDir

    if (filter != null) {
      // optional dateFormatMark
      int posFormat = filter.indexOf('#');
      if (posFormat >= 0) {
        // check for two hash marks
        int posFormat2 = filter.lastIndexOf('#');

        if (posFormat != posFormat2) { // two hash
          dateFormatMark = filter.substring(0, posFormat2); // everything up to the second hash
          filter = StringUtil2.remove(filter, '#'); // remove hashes, replace with .
          StringBuilder sb = new StringBuilder(filter);
          for (int i = posFormat; i < posFormat2 - 1; i++)
            sb.setCharAt(i, '.');
          String regExp = sb.toString();
          this.filter = java.util.regex.Pattern.compile(regExp);

        } else { // one hash
          dateFormatMark = filter; // everything
          String regExp = filter.substring(0, posFormat) + "*";
          this.filter = java.util.regex.Pattern.compile(regExp);
        }

      } else { // no hash (dateFormatMark)
        dateFormatMark = null;
        this.filter = java.util.regex.Pattern.compile(filter);
      }
    } else {
      dateFormatMark = null;
      this.filter = null;
    }
    //useName = true;
  }

  /*
   * Seperate the spec, with no dateMatcher, and a seperate string with a dateMatcher
   * This only allows the dateFormatMark to be in the file name, not anywhere else in the filename path
   * @param collectionSpec the collection Spec, no dateMatcher
   * @param dateMatcher regexp the dateMatcher regular expression
   * @param errlog put error messages here
   *
  public CollectionSpecParser(String collectionSpec, String dateMatcher, Formatter errlog) {
    this.spec = collectionSpec.trim();

    int posGlob = collectionSpec.indexOf("/** /");
    if (posGlob > 0) {
      rootDir = collectionSpec.substring(0, posGlob);
      int posFilter = posGlob + 3;
      subdirs = true;
      String regexp = collectionSpec.substring(posFilter+1);
      this.filter = java.util.regex.Pattern.compile(regexp);

    } else {
      int posFilter = collectionSpec.lastIndexOf('/');
      rootDir = collectionSpec.substring(0, posFilter);
      subdirs = false;
      String regexp = collectionSpec.substring(posFilter+1);
      this.filter = java.util.regex.Pattern.compile(regexp);
    }

    File locFile = new File(rootDir);
    if (!locFile.exists()) {
      errlog.format(" Directory %s does not exist %n", rootDir);
    }

    this.dateFormatMark = dateMatcher;

    /* int hashPos = -1;
    if ((hashPos = dateMatcher.indexOf('#', hashPos+1)) >= 0) {
      // check for two hash marks
      hashPos = dateMatcher.indexOf('#', hashPos+1);
      int secondHash = hashPos;

      if (secondHash > 0) { // two hashes
        dateFormatMark = dateMatcher.substring(0, secondHash+1); // everything up to the second hash
      } else { // one hash
        dateFormatMark = dateMatcher; // everything
      }

    } else  { // no hashes
      errlog.format(" No DateMatcher specified in '%s'%n", dateMatcher);
      dateFormatMark = null;
    }

    useName = false;
  } */

  public String getSpec() {
    return spec;
  }

  public String getRootDir() {
    return rootDir;
  }

  public boolean wantSubdirs() {
    return subdirs;
  }

  //public boolean useName() {
  //  return true;
  //}

  public Pattern getFilter() {
    return filter;
  }

  public String getDateFormatMark() {
    return dateFormatMark;
  }

  @Override
  public String toString() {
    return "CollectionSpecParser{" +
            "\n   topDir='" + rootDir + '\'' +
            "\n   subdirs=" + subdirs +
            "\n   regExp='" + filter + '\'' +
            "\n   dateFormatMark='" + dateFormatMark + '\'' +
  //          "\n   useName=" + useName +
            "\n}";
  }

  /////////////////////////////////////////////////////////
  // debugging

  /* private static void doit2(String spec, String timePart, Formatter errlog) {
    CollectionSpecParser specp = new CollectionSpecParser(spec, timePart, errlog);
    System.out.printf("spec= %s timePart=%s%n%s%n", spec, timePart, specp);
    String err = errlog.toString();
    if (err.length() > 0)
      System.out.printf("%s%n", err);
    System.out.printf("-----------------------------------%n");
  }


  public static void main(String arg[]) {
    doit2("G:/nomads/cfsr/timeseries/** /.*grb2$", "G:/nomads/cfsr/#timeseries/#yyyyMM", new Formatter());
    //doit("C:/data/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", new Formatter());
    // doit("Q:/station/ldm/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc", new Formatter());
  }  */

  private static void doit(String spec, Formatter errlog) {
    CollectionSpecParser specp = new CollectionSpecParser(spec, errlog);
    System.out.printf("spec= %s%n%s%n", spec, specp);
    String err = errlog.toString();
    if (err.length() > 0)
      System.out.printf("%s%n", err);
    System.out.printf("-----------------------------------%n");
  }


  public static void main(String arg[]) {
    doit("/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/**/GFS_Alaska_191km_#yyyyMMdd_HHmm#\\.grib1$", new Formatter());
    doit("Q:/grid/grib/grib1/data/agg/.*\\.grb", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#\\.nc", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#.nc", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_yyyyMMdd_HHmm.nc", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/*", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/*", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/T*.T", new Formatter());
  }

}
