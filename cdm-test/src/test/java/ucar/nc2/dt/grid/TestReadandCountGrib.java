/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/** Check opening grib datasets - local files*/

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadandCountGrib {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

     result.add(new Object[]{"grib1/","cfs.wmo", 51, 4, 6, 3});
     result.add(new Object[]{"grib2/","eta218.wmo", 57, 16, 20, 11});
     result.add(new Object[]{"grib1/","extended.wmo", 8, 6, 10, 4});
     //result.add(new Object[]{"grib1/","ensemble.wmo", 24, 16, 20, 10}); not supporting ensembles in GRIB1 yet
     // result.add(new Object[]{"grib1/data/","ecmf.wmo", 56, 44, 116, 58});
     result.add(new Object[]{"grib1/","don_ETA.wmo", 28, 11, 13, 8});
     result.add(new Object[]{"grib1/","pgbanl.fnl", 76, 15, 17, 14});
     result.add(new Object[]{"grib1/","radar_national_rcm.grib", 1, 1, 3, 0});
     result.add(new Object[]{"grib1/","radar_national.grib", 1, 1, 3, 0});
     //result.add(new Object[]{"grib1/data/","thin.wmo", 240, 87, 117, 63});
     //result.add(new Object[]{"grib1/data/","ukm.wmo", 96, 49, 68, 32});
     result.add(new Object[]{"grib1/","AVN.wmo", 22, 10, 12, 7});
     result.add(new Object[]{"grib1/","AVN-I.wmo", 20, 8, 10, 7}); //
     result.add(new Object[]{"grib1/","MRF.wmo", 15, 8, 10, 6}); //
     result.add(new Object[]{"grib1/","OCEAN.wmo", 4, 4, 12, 0});
     result.add(new Object[]{"grib1/","RUC.wmo", 27, 7, 10, 5});
     result.add(new Object[]{"grib1/","RUC2.wmo", 44, 10, 13, 5});
     result.add(new Object[]{"grib1/","WAVE.wmo", 28, 12, 24, 4}); //

     result.add(new Object[]{"grib2/","eta2.wmo", 35, 9, 11, 7});
     result.add(new Object[]{"grib2/","ndfd.wmo", 1, 1, 3, 0}); //
     //result.add(new Object[]{"grib2/","eta218.wmo", 57, 13, 29, 20}); // multiple horiz coords == groups
     result.add(new Object[]{"grib2/","PMSL_000", 1, 1, 3, 0});
     result.add(new Object[]{"grib2/","CLDGRIB2.2005040905", 5, 1, 3, 0});
     // result.add(new Object[]{"grib2/","LMPEF_CLM_050518_1200.grb", 1, 1, 3, 0});
     result.add(new Object[]{"grib2/","AVOR_000.grb", 1, 1, 4, 1}); //
     result.add(new Object[]{"grib2/","AVN.5deg.wmo", 117, 17, 18, 14});  // */

     result.add(new Object[]{"grib2/","gribdecoder-20101101.enspost.t00z.prcp.grib", 2, 2, 5, 0}); // ensemble
    return result;
    }

  String dir, name;
  int ngrids, ncoordSys, ncoordAxes, nVertCooordAxes;

  public TestReadandCountGrib( String dir, String name, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) {
    this.dir = dir;
    this.name = name;
    this.ngrids = ngrids;
    this.ncoordSys = ncoordSys;
    this.ncoordAxes = ncoordAxes;
    this.nVertCooordAxes = nVertCooordAxes;
  }

  @org.junit.Test
  public void doOne() throws Exception {
    dir = TestDir.cdmUnitTestDir + "formats/" + dir;
    TestReadandCount.doOne(dir, name, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }

}
