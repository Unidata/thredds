/* Copyright */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageDataset;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/17/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridCoverageMisc {

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters() {
      List<Object[]> result = new ArrayList<>();

      result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", CoverageCoordSys.Type.Grid});  // NUWG - has CoordinateAlias
      result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", CoverageCoordSys.Type.Grid});  // scalar runtime
      result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", CoverageCoordSys.Type.Grid});  // both x,y and lat,lon
      result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx3", "Relative_humidity_sigma_layer"}); // SRC                               // TP

      return result;
    }

    String endpoint;
    CoverageCoordSys.Type expectType;
    //int domain, range, ncoverages;

    public TestGridCoverageMisc(String endpoint, CoverageCoordSys.Type expectType) {
      this.endpoint = endpoint;
      this.expectType = expectType;
    }


  @Test
  public void testFail() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx3";
    String covName = "Relative_humidity_sigma_layer";
    CalendarDate rt_val = CalendarDate.parseISOformat(null, "2015-03-26T06:00:00Z");
    CalendarDate time_val = CalendarDate.parseISOformat(null, "2015-03-26T06:00:00Z");

    //    Slice runtime=2015-03-26T06:00:00Z (0) ens=0.000000 (-1) time=2015-03-26T06:00:00Z (0) vert=0.665000 (1)
    //testReadGridCoverageSlice(endpoint, covName, rt_val, null, time_val, 0.580000);
    testReadGridCoverageSlice(endpoint, covName, rt_val, null, time_val, 0.665000);
  }

  private void testReadGridCoverageSlice(String endpoint, String covName, CalendarDate rt_val, Double ens_val, CalendarDate time_val, Double vert_val) throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    try (CoverageDataset gcs = CoverageDatasetFactory.openCoverage(endpoint)) {
      Assert.assertNotNull(endpoint, gcs);
      Coverage cover = gcs.findCoverage(covName);

      // check DtCoverageCS
      try (GridDataset ds = GridDataset.open(endpoint)) {
        GridDatatype dt = ds.findGridByName(covName);

        GridCoordSystem csys = dt.getCoordinateSystem();
        CoordinateAxis1DTime rtAxis = csys.getRunTimeAxis();
        CoordinateAxis1D ensAxis = csys.getEnsembleAxis();
        CoordinateAxis1DTime timeAxis = csys.getTimeAxis1D();
        CoordinateAxis1D vertAxis = csys.getVerticalAxis();

        int rt_idx = (rtAxis == null || rt_val == null) ? -1 : rtAxis.findTimeIndexFromCalendarDate(rt_val);
        int ens_idx = (ensAxis == null || ens_val == null) ? -1 : ensAxis.findCoordElement(ens_val);
        int time_idx = (timeAxis == null || time_val == null) ? -1 : timeAxis.findTimeIndexFromCalendarDate(time_val);
        int vert_idx = (vertAxis == null || vert_val == null) ? -1 : vertAxis.findCoordElement(vert_val);

        System.out.printf(" Grid %s%n", dt.getFullName());

        TestGridCoverageReading.readOneSlice(cover, dt,
                rt_val, rt_idx,
                ens_val == null ? 0 : ens_val, ens_idx,
                time_val, time_idx,
                vert_val == null ? 0 : vert_val, vert_idx);
      }
    }
  }

}
