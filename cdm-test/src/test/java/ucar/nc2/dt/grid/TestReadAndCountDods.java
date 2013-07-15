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
package ucar.nc2.dt.grid;

import junit.framework.*;

/** Count geogrid objects - sanity check when anything changes. */

public class TestReadAndCountDods extends TestCase {
  static String base = "thredds:resolve:http://thredds.ucar.edu/thredds/";

  public TestReadAndCountDods( String name) {
    super(name);
  }

  public void testRead() throws Exception {
    // Grib files, one from each model
    TestReadandCount.doOne(base,"catalog/grib/NCEP/DGEX/CONUS_12km/files/latest.xml", 23, 11, 13, 8);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/DGEX/Alaska_12km/files/latest.xml", 23, 11, 13, 8);

    TestReadandCount.doOne(base,"catalog/grib/NCEP/GEFS/Global_1p0deg_Ensemble/members/files/latest.xml", 35, 11, 13, 6);
    TestReadandCount.doOne(base,"grib/NCEP/GEFS/Global_1p0deg_Ensemble/derived/files/latest.xml", 70, 11, 12, 6);

    // 133, 26, 27, 21 vs 133, 31, 29, 21
    TestReadandCount.doOne(base,"catalog/grib/NCEP/GFS/Global_0p5deg/files/latest.xml", 133, -1, -1, 21);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/GFS/Global_onedeg/files/latest.xml", 133, 26, 27, 21);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/GFS/Global_2p5deg/files/latest.xml", 133, 24, 25, 21);
    // flipping between 23, 9, 11, 6 and 23, 12, 14, 6
    TestReadandCount.doOne(base,"catalog/grib/NCEP/GFS/N_Hemisphere_381km/files/latest.xml", 23, -1, -1, 6);
    // flipping between 50, 14, 15, 8 and  50, 11, 13, 8
    TestReadandCount.doOne(base,"catalog/grib/NCEP/GFS/Pacific_40km/files/latest.xml", 50, -1, -1, 8);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/GFS/Puerto_Rico_0p5deg/files/latest.xml", 50, 11, 13, 8);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/GFS/CONUS_80km/files/latest.xml", 31, 12, 15, 8);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/GFS/CONUS_95km/files/latest.xml", 30, 10, 12, 8);

    // flipping 59, 15, 17, 13,  and 59, 16, 18, 13,
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/Alaska_11km/files/latest.xml", 59, -1, -1, 13);
    // flipping between 154, 34, 36, 31 and 154, 33, 35, 31
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/Alaska_45km/conduit/files/latest.xml", 154, -1, -1, 31);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/CONUS_12km/files/latest.xml", 59, 15, 17, 13);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/CONUS_40km/conduit/files/latest.xml", 176, 29, 31, 25);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/Polar_90km/files/latest.xml", 133, 28, 30, 25);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/Polar_90km/files/latest.xml", 133, 28, 30, 25);

    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/Alaska_22km/files/latest.xml", 25, 8, 10, 6);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/Alaska_45km/noaaport/files/latest.xml", 21, 6, 8, 4);
    // flipping between 29, 12, 14, 9, and 29, 15, 15, 9
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/Alaska_95km/files/latest.xml", 29, -1, -1, 9);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/CONUS_20km/noaaport/files/latest.xml", 33, 9, 11, 7);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/CONUS_80km/files/latest.xml", 41, 11, 13, 8);

    TestReadandCount.doOne(base,"catalog/grib/NCEP/RAP/CONUS_13km/files/latest.xml", 53, 12, 14, 9);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/RAP/CONUS_20km/files/latest.xml", 74, 15, 17, 11);
    TestReadandCount.doOne(base,"catalog/grib/NCEP/RAP/CONUS_40km/files/latest.xml", 74, 15, 17, 11);
  }

  public void utestProblem() throws Exception {
    TestReadandCount.doOne(base,"catalog/grib/NCEP/NAM/CONUS_20km/surface/files/latest.xml", 54, 16, 18, 12);
  }

  static void doOne(String dir, String filename, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) throws Exception {
    TestReadandCount.doOne(dir, filename, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }

   public static void main( String arg[]) throws Exception {
     // new TestReadandCount("dummy").doOne("C:/data/conventions/wrf/","wrf.nc", 33, 5, 7, 7);  // missing TSLB
     new TestReadandCountGrib("dummy").testRead();  // missing TSLB
  }

}
