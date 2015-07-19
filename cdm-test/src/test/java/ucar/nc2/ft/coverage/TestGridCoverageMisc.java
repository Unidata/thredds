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
 * Read specific Grid Coverage fields and compare results with dt.GridDataset.
 * Generally the ones that are failing in TestGridCoverageReading
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

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", "RH_lpdg", null, null, "2003-06-12T19:00:00Z", 150.0});  // NUWG - has CoordinateAlias
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx3", "Relative_humidity_sigma_layer", "2015-03-26T06:00:00Z", null, "2015-03-26T06:00:00Z", 0.580000}); // SRC                               // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx3", "Relative_humidity_sigma_layer", "2015-03-26T06:00:00Z", null, "2015-03-26T06:00:00Z", 0.665000}); // SRC                               // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx3", "Absolute_vorticity_isobaric", "2015-03-26T06:00:00Z", null, "2015-03-26T06:00:00Z", 100000.0}); // SRC                               // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc", "elev", null, null, "2009-10-24T04:14:59.121Z", null});  // x,y axis but no projection

    return result;
  }

  String endpoint;
  String covName;
  CalendarDate rt_val;
  Double ens_val;
  CalendarDate time_val;
  Double vert_val;

  public TestGridCoverageMisc(String endpoint, String covName, String rt_val, Double ens_val, String time_val, Double vert_val) {
    this.endpoint = endpoint;
    this.covName = covName;
    this.rt_val = rt_val == null ? null : CalendarDate.parseISOformat(null, rt_val);
    this.ens_val = ens_val;
    this.time_val = time_val == null ? null : CalendarDate.parseISOformat(null, time_val);
    this.vert_val = vert_val;
  }


  @Test
  public void testReadGridCoverageSlice() throws IOException {
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
