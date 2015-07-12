/* Copyright */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.Array;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.test.util.CompareNetcdf;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GridCoverage Subsetting
 *
 * @author caron
 * @since 6/1/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridCoverageSubset {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", CoverageCoordSys.Type.Grid, 4, 4, 31});  // NUWG - has CoordinateAlias
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", CoverageCoordSys.Type.Grid, 4, 5, 5});  // scalar runtime
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", CoverageCoordSys.Type.Grid, 3, 3, 4});  // both x,y and lat,lon

    return result;
  }

  String endpoint;
  CoverageCoordSys.Type expectType;
  int domain, range, ncoverages;


  public TestGridCoverageSubset(String endpoint, CoverageCoordSys.Type expectType, int domain, int range, int ncoverages) {
    this.endpoint = endpoint;
    this.expectType = expectType;
    this.domain = domain;
    this.range = range;
    this.ncoverages = ncoverages;
  }

  @Test
  public void testGridCoverageDataset() throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    try (CoverageDataset gcs = CoverageDatasetFactory.openCoverage(endpoint)) {
      Assert.assertNotNull(endpoint, gcs);
      Assert.assertEquals("NGrids", ncoverages, gcs.getCoverageCount());
      Assert.assertEquals(expectType, gcs.getCoverageType());

      // check DtCoverageCS
      try (GridDataset ds = GridDataset.open(endpoint)) {
        for (GridDatatype dt : ds.getGrids()) {
          GridCoordSystem csys = dt.getCoordinateSystem();
          CoordinateAxis1DTime rtAxis = csys.getRunTimeAxis();
          CoordinateAxis1D ensAxis = csys.getEnsembleAxis();
          CoordinateAxis1DTime timeAxis = csys.getTimeAxis1D();
          CoordinateAxis1D vertAxis = csys.getVerticalAxis();

          Coverage cover = gcs.findCoverage(dt.getFullName());
          if (cover == null) {
            System.out.printf("Cant find %s%n", dt.getFullName());
            continue;
          }
          System.out.printf(" Grid %s%n", dt.getFullName());

          readOneSliceRunTime(cover, dt, rtAxis, ensAxis, timeAxis, vertAxis);
        }
      }
    }
  }

  private void readOneSliceRunTime(Coverage cover, GridDatatype dt, CoordinateAxis1DTime runtimeAxis, CoordinateAxis1D ensAxis, CoordinateAxis1DTime timeAxis, CoordinateAxis1D vertAxis) {
    if (runtimeAxis == null)
      readOneSliceEns(cover, dt, null, -1, ensAxis, timeAxis, vertAxis);
    else {
      for (int i = 0; i < runtimeAxis.getSize(); i++)
        readOneSliceEns(cover, dt, runtimeAxis.getCalendarDate(i), i, ensAxis, timeAxis, vertAxis);
    }
  }


  private void readOneSliceEns(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx, CoordinateAxis1D ensAxis, CoordinateAxis1DTime timeAxis, CoordinateAxis1D vertAxis) {
    if (ensAxis == null)
      readOneSliceTime(cover, dt, rt_val, rt_idx, 0, -1, timeAxis, vertAxis);
    else {
      for (int i = 0; i < ensAxis.getSize(); i++)
        readOneSliceTime(cover, dt, rt_val, rt_idx, ensAxis.getCoordValue(i), i, timeAxis, vertAxis);
    }
  }


  private void readOneSliceTime(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx, double ens_val, int ens_idx,
                                CoordinateAxis1DTime timeAxis, CoordinateAxis1D vertAxis) {
    if (timeAxis == null)
      readOneSliceVert(cover, dt, rt_val, rt_idx, ens_val, ens_idx, null, -1, vertAxis);
    else {
      for (int i = 0; i < timeAxis.getSize(); i++)
        readOneSliceVert(cover, dt, rt_val, rt_idx, ens_val, ens_idx, timeAxis.getCalendarDate(i), i, vertAxis);
    }
  }

  private void readOneSliceVert(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx, double ens_val, int ens_idx,
                                CalendarDate time_val, int time_idx, CoordinateAxis1D vertAxis) {
    if (vertAxis == null)
      readOneSlice(cover, dt, rt_val, rt_idx, ens_val, ens_idx, time_val, time_idx, 0, -1);
    else {
      for (int i = 0; i < vertAxis.getSize(); i++)
        readOneSlice(cover, dt, rt_val, rt_idx, ens_val, ens_idx, time_val, time_idx, vertAxis.getCoordValue(i), i);
    }
  }

  private void readOneSlice(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx, double ens_val, int ens_idx,
                            CalendarDate time_val, int time_idx, double vert_val, int vert_idx) {
    System.out.printf("   Slice runtime=%s (%d) ens=%f (%d) time=%s (%d) vert=%f (%d) %n", rt_val, rt_idx, ens_val, ens_idx, time_val, time_idx, vert_val, vert_idx);

    Array dt_array;
    try {
      dt_array = dt.readDataSlice(rt_idx, ens_idx, time_idx, vert_idx, -1, -1);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    SubsetParams subset = new SubsetParams();
    if (rt_idx >= 0)
      subset.set(SubsetParams.runtime, rt_val);
    if (ens_idx >= 0)
      subset.set(SubsetParams.ensCoord, ens_val);
    if (time_idx >= 0)
      subset.set(SubsetParams.date, time_val);
    if (vert_idx >= 0)
      subset.set(SubsetParams.vertCoord, vert_val);

    ArrayWithCoordinates gc_array;
    try {
      gc_array = cover.readData(subset);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    CompareNetcdf.compareData(dt_array, gc_array.getData());
    //NCdumpW.printArray(dt_array);
  }


}
