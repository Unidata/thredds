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
import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.CompareNetcdf;

/** Count geogrid objects - sanity check when anything changes. */

public class TestReadandCountGrib extends TestCase {

  public TestReadandCountGrib( String name) {
    super(name);
  }

  public void testRead() throws Exception {

    // our grib reader
    doOne("grib1/data/","cfs.wmo", 51, 4, 6, 3);
    doOne("grib1/data/","eta218.grb", 14, 6, 8, 4);
    doOne("grib1/data/","extended.wmo", 8, 6, 10, 4);
    doOne("grib1/data/","ensemble.wmo", 24, 16, 20, 10);
    // doOne("grib1/data/","ecmf.wmo", 56, 44, 116, 58);
    doOne("grib1/data/","don_ETA.wmo", 28, 11, 13, 8);
    doOne("grib1/data/","pgbanl.fnl", 76, 15, 17, 14);
    doOne("grib1/data/","radar_national_rcm.grib", 1, 1, 3, 0);
    doOne("grib1/data/","radar_national.grib", 1, 1, 3, 0);
    //doOne("grib1/data/","thin.wmo", 240, 87, 117, 63);
    //doOne("grib1/data/","ukm.wmo", 96, 49, 68, 32);
    doOne("grib1/data/","AVN.wmo", 22, 10, 12, 7);
    doOne("grib1/data/","AVN-I.wmo", 20, 8, 10, 7); //
    doOne("grib1/data/","MRF.wmo", 15, 8, 10, 6); //
    doOne("grib1/data/","OCEAN.wmo", 4, 4, 12, 0);
    doOne("grib1/data/","RUC.wmo", 27, 7, 10, 5);
    doOne("grib1/data/","RUC2.wmo", 44, 10, 13, 5);
    doOne("grib1/data/","WAVE.wmo", 28, 12, 24, 4); //
    doOne("grib2/data/","eta2.wmo", 35, 9, 11, 7);
    doOne("grib2/data/","ndfd.wmo", 1, 1, 3, 0); //
    //doOne("grib2/data/","eta218.wmo", 57, 13, 29, 20); // multiple horiz coords == groups
    doOne("grib2/data/","PMSL_000", 1, 1, 3, 0);
    doOne("grib2/data/","CLDGRIB2.2005040905", 5, 1, 3, 0);
    doOne("grib2/data/","LMPEF_CLM_050518_1200.grb", 1, 1, 3, 0);
    doOne("grib2/data/","AVOR_000.grb", 1, 2, 4, 1); //
    doOne("grib2/data/","AVN.5deg.wmo", 117, 15, 16, 12);  // */
    //TestReadandCount.doOne(TestAll.testdataDir+"ncml/nc/narr/", "narr-a_221_20070411_0600_000.grb", 48, 13, 15, 12);
  }

  private void doOne(String dir, String filename, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) throws Exception {
    dir = TestAll.testdataDir + "grid/grib/" + dir;
    TestReadandCount.doOne(dir, filename, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }

  public void testProblem() throws Exception {
    doOne("grib2/data/","CLDGRIB2.2005040905", 5, 1, 3, 0);    
  }

   public static void main( String arg[]) throws Exception {
     //TestReadandCount.doOne("Q:/grid/grib/grib1/data/", "ukm.wmo", -1, -1, -1, -1);

     NetcdfFile ncfile = NetcdfFile.open("//shemp/testdata/grid/grib/grib1/data/ukm.wmo", null);
     NetcdfFile ncfile1 = NetcdfFile.open("//shemp/testdata/grid/grib/grib1/data/ukm1.wmo", null);
     CompareNetcdf.compareFiles(ncfile, ncfile1, false, false, false);
  }

}
