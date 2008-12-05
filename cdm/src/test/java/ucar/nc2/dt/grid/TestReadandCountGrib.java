package ucar.nc2.dt.grid;

import junit.framework.*;
import ucar.nc2.TestAll;

/** Count geogrid objects - sanity check when anything changes. */

public class TestReadandCountGrib extends TestCase {

  public TestReadandCountGrib( String name) {
    super(name);
  }

  public void testRead() throws Exception {

    // our grib reader
    doOne("grib1/data/","cfs.wmo", 51, 4, 6, 3);
    doOne("grib1/data/","eta218.grb", 14, 5, 7, 4);
    doOne("grib1/data/","extended.wmo", 8, 6, 10, 4);
    doOne("grib1/data/","ensemble.wmo", 24, 16, 18, 10);
    // formerly doOne("grib1/data/","ecmf.wmo", 56, 44, 116, 58);
    doOne("grib1/data/","ecmf.wmo", 56, 44, 112, 56);     
    doOne("grib1/data/","don_ETA.wmo", 28, 11, 13, 8);
    doOne("grib1/data/","pgbanl.fnl", 76, 15, 17, 14);
    doOne("grib1/data/","radar_national_rcm.grib", 1, 1, 3, 0);
    doOne("grib1/data/","radar_national.grib", 1, 1, 3, 0);
    doOne("grib1/data/","thin.wmo", 240, 87, 117, 63);
    doOne("grib1/data/","ukm.wmo", 96, 49, 69, 32);  
    doOne("grib1/data/","AVN.wmo", 22, 9, 11, 7);
    doOne("grib1/data/","AVN-I.wmo", 20, 8, 10, 7); //
    doOne("grib1/data/","MRF.wmo", 15, 8, 10, 6); //
    doOne("grib1/data/","OCEAN.wmo", 4, 4, 12, 0);
    doOne("grib1/data/","RUC.wmo", 27, 7, 10, 5);
    doOne("grib1/data/","RUC2.wmo", 44, 10, 13, 5);
    doOne("grib1/data/","WAVE.wmo", 28, 12, 24, 4); //
    doOne("grib2/data/","eta2.wmo", 35, 7, 9, 6);
    doOne("grib2/data/","ndfd.wmo", 1, 1, 3, 0); //
    doOne("grib2/data/","eta218.wmo", 57, 13, 18, 10);
    doOne("grib2/data/","PMSL_000", 1, 1, 3, 0);
    doOne("grib2/data/","CLDGRIB2.2005040905", 5, 1, 3, 0);
    doOne("grib2/data/","LMPEF_CLM_050518_1200.grb", 1, 1, 3, 0);
    doOne("grib2/data/","AVOR_000.grb", 1, 2, 4, 1); //
    doOne("grib2/data/","AVN.5deg.wmo", 117, 13, 15, 12);  // */
    //TestReadandCount.doOne(TestAll.upcShareTestDataDir+"ncml/nc/narr/", "narr-a_221_20070411_0600_000.grb", 48, 13, 15, 12);
  }

  private void doOne(String dir, String filename, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) throws Exception {
    dir = TestAll.upcShareTestDataDir+ "grid/grib/" + dir;
    TestReadandCount.doOne(dir, filename, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }

   public static void main( String arg[]) throws Exception {
     // new TestReadandCount("dummy").doOne("C:/data/conventions/wrf/","wrf.nc", 33, 5, 7, 7);  // missing TSLB
     new TestReadandCountGrib("dummy").testRead();  // missing TSLB
     //new TestReadandCount("dummy").doOne(TestAll.upcShareTestDataDir+"grid/grib/grib1/data/","ukm.wmo", 96, 49, 69, 32);
  }

}
