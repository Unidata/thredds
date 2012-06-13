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
  static String base = "thredds:resolve:http://motherlode.ucar.edu:8080/thredds/";

  public TestReadAndCountDods( String name) {
    super(name);
  }

  public void testRead() throws Exception {

    /* this has a Grid that returns a structure
    try {
      doOne("dods://iridl.ldeo.columbia.edu/SOURCES/.CAYAN/dods", "", 5, 1, 3, 0);
    } catch (Throwable e) {
      System.out.println(" -- barf");
    }  // */

    /* this has a Grid that returns a bare array
    try {
      doOne("dods://usgodae2.usgodae.org:80/dods/GDS/coamps_cent_am/COAMPS_cent_am_0001_000000-000000ltnt_heat_flux", "", 1, 1, 3, 0);
    } catch (Throwable e) {
      System.out.println(" -- barf");
    }  // */

    /* IDV netcdf files, one from each model
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?nam_211", 41, 7, 9, 5);
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?gfs_211", 31, 6, 8, 4);
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?gfs_37-44", 31, 4, 8, 4);
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?gfs_25-26", 4, 2, 5, 1);
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?ruc_211", 31, 5, 7, 3);
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?ruc2_236", 48, 4, 7, 3);
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?sst_21-24", 1, 1, 4, 0);
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?sst_61-64", 1, 1, 4, 0);
    TestReadandCount.doOne(base,"dqc/latestModel-InvCat1.0?ocean_21-24", 5, 1, 4, 0);  // */

    // Problem with latest showing incomplete files !!
    // Grib files, one from each model
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/Alaska_191km/files/latest.xml", 22, 10, 12, 7); //
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/CONUS_80km/files/latest.xml", 31, 12, 15, 8);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/CONUS_95km/files/latest.xml", 30, 10, 12, 8);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/CONUS_191km/files/latest.xml", 20, 8, 10, 7);    
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/Global_0p5deg/files/latest.xml", 133, 26, 27, 21);    
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/Global_onedeg/files/latest.xml", 133, 26, 27, 21);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/Global_2p5deg/files/latest.xml", 133, 24, 25, 21);    
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/Hawaii_160km/files/latest.xml", 15, 8, 10, 6);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/N_Hemisphere_381km/files/latest.xml", 23, 9, 11, 6);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/GFS/Puerto_Rico_191km/files/latest.xml", 15, 8, 10, 6);

    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/Alaska_11km/files/latest.xml", 59, 16, 18, 13);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/Alaska_22km/files/latest.xml", 25, 8, 10, 6);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/Alaska_45km/noaaport/files/latest.xml", 21, 6, 8, 4);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/Alaska_45km/conduit/files/latest.xml", 154, -1, -1, 31);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/Alaska_95km/files/latest.xml", 29, 12, 14, 9);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/CONUS_12km/files/latest.xml", 59, 16, 18, 13);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/CONUS_20km/surface/files/latest.xml", 54, -1, -1, 12);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/CONUS_20km/selectsurface/files/latest.xml", 54, -1, -1, 12);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/CONUS_20km/noaaport/files/latest.xml", 33, 9, 11, 7);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/CONUS_40km/conduit/files/latest.xml", 176, -1, -1, 25);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/CONUS_80km/files/latest.xml", 41, 11, 13, 8);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/NAM/Polar_90km/files/latest.xml", 133, 28, 30, 25);

    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RAP/CONUS_13km/files/latest.xml", 52, 13, 15, 9);
    //TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_13km/files/latest.xml", 52, 13, 15, 9);        
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RAP/CONUS_20km/files/latest.xml", 74, 15, 17, 11);
    //TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_20km/surface/files/latest.xml", 74, 15, 17, 11); // now Rapid refresh 5/4/2012
    //TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_20km/pressure/files/latest.xml", -1, 12, 14, 9);
    //TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_20km/hybrid/files/latest.xml", 55, 10, 12, 8);
    
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_40km/files/latest.xml", 48, -1, -1, 10);
    //TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC/CONUS_80km/files/latest.xml", 31, 9, 11, 5);  // */

    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/DGEX/CONUS_12km/files/latest.xml", 23, 11, 13, 8);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/DGEX/Alaska_12km/files/latest.xml", 23, 11, 13, 8);
  }

  public void utestProblem() throws Exception {
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_13km/files/latest.xml", 52, 13, 15, 9);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_20km/surface/files/latest.xml", 74, 15, 17, 11); // now Rapid refresh 5/4/2012
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_20km/pressure/files/latest.xml", -1, 12, 14, 9);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_20km/hybrid/files/latest.xml", 55, 10, 12, 8);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC2/CONUS_40km/files/latest.xml", 48, 15, 17, 10);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/RUC/CONUS_80km/files/latest.xml", 31, 9, 11, 5);  // */

    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/DGEX/CONUS_12km/files/latest.xml", 23, 11, 13, 8);
    TestReadandCount.doOne(base,"catalog/fmrc/NCEP/DGEX/Alaska_12km/files/latest.xml", 23, 11, 13, 8);
  }

  static void doOne(String dir, String filename, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) throws Exception {
    TestReadandCount.doOne(dir, filename, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }

   public static void main( String arg[]) throws Exception {
     // new TestReadandCount("dummy").doOne("C:/data/conventions/wrf/","wrf.nc", 33, 5, 7, 7);  // missing TSLB
     new TestReadandCountGrib("dummy").testRead();  // missing TSLB
  }

}
