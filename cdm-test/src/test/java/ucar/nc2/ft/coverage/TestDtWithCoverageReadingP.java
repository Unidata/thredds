/* Copyright */
package ucar.nc2.ft.coverage;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.CompareNetcdf;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GridCoverage Subsetting
 * Read all the data for a coverage variable and compare it to dt.Grid
 *
 * @author caron
 * @since 6/1/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestDtWithCoverageReadingP {

  @BeforeClass
  public static void before() {
    GribDataReader.validator = new GribCoverageValidator();
  }

  @AfterClass
  public static void after() {
    GribDataReader.validator = null;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", FeatureType.GRID});  // NUWG - has CoordinateAlias
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", FeatureType.GRID});  // scalar runtime
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", FeatureType.GRID});  // both x,y and lat,lon
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4", FeatureType.GRID}); // SRC

    // not GRID
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/MM_cnrm_129_red.ncml", FeatureType.FMRC}); // ensemble, time-offset
    // result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ukmo.nc", FeatureType.FMRC});              // scalar vert LOOK change to TimeOffset ??
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc", FeatureType.CURVILINEAR});  // x,y axis but no projection

    return result;
  }

  String endpoint;
  FeatureType expectType;
  //int domain, range, ncoverages;

  public TestDtWithCoverageReadingP(String endpoint, FeatureType expectType) {
    this.endpoint = endpoint;
    this.expectType = expectType;
  }

  @Ignore("takes too long")
  @Test
  public void testGridCoverageDataset() throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(expectType);
      Assert.assertNotNull(expectType.toString(), gcs);

      // check DtCoverageCS
      try (GridDataset ds = GridDataset.open(endpoint)) {
        for (GridDatatype dt : ds.getGrids()) {
          if (expectType == FeatureType.FMRC && !dt.getFullName().startsWith("TwoD")) continue;
          if (expectType == FeatureType.GRID && dt.getFullName().startsWith("TwoD")) continue;

          GridCoordSystem csys = dt.getCoordinateSystem();
          CoordinateAxis1DTime rtAxis = csys.getRunTimeAxis();
          CoordinateAxis1D ensAxis = csys.getEnsembleAxis();
          CoordinateAxis1D vertAxis = csys.getVerticalAxis();

          Coverage cover = gcs.findCoverage(dt.getShortName());
          if (cover == null) {
            System.out.printf("Cant find %s%n", dt.getFullName());
            continue;
          }
          System.out.printf(" Grid %s%n", dt.getFullName());

          readAllRuntimes(cover, dt, rtAxis, ensAxis, vertAxis);
        }
      }
    }
  }

  static void readAllRuntimes(Coverage cover, GridDatatype dt, CoordinateAxis1DTime runtimeAxis, CoordinateAxis1D ensAxis, CoordinateAxis1D vertAxis) {
    GridCoordSystem csys = dt.getCoordinateSystem();
    CoordinateAxis1DTime timeAxis1D = csys.getTimeAxis1D();
    CoordinateAxis timeAxis = csys.getTimeAxis();
    CoordinateAxis2D timeAxis2D = (timeAxis instanceof CoordinateAxis2D) ? (CoordinateAxis2D) timeAxis : null;

    if (runtimeAxis == null)
      readAllTimes1D(cover, dt, null, -1, timeAxis1D, ensAxis, vertAxis);

    else if (timeAxis2D == null) {  // 1D time or no time
      for (int i = 0; i < runtimeAxis.getSize(); i++)
        readAllTimes1D(cover, dt, runtimeAxis.getCalendarDate(i), i, timeAxis1D, ensAxis, vertAxis);

    } else {  // 2D time
      TimeHelper helper = TimeHelper.factory(timeAxis.getUnitsString(), timeAxis.getAttributeContainer());

      if (timeAxis2D.isInterval()) {
        ArrayDouble.D3 bounds = timeAxis2D.getCoordBoundsArray();
        for (int i = 0; i < runtimeAxis.getSize(); i++)
           readAllTimes2D(cover, dt, runtimeAxis.getCalendarDate(i), i, helper, bounds.slice(0, i), ensAxis, vertAxis);

      } else {
        ArrayDouble.D2 coords = timeAxis2D.getCoordValuesArray();
        for (int i = 0; i < runtimeAxis.getSize(); i++)
          readAllTimes2D(cover, dt, runtimeAxis.getCalendarDate(i), i, helper, coords.slice(0, i), ensAxis, vertAxis);
      }
    }
  }

  static void readAllTimes1D(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx,
                             CoordinateAxis1DTime timeAxis, CoordinateAxis1D ensAxis, CoordinateAxis1D vertAxis) {
    if (timeAxis == null)
      readAllEnsembles(cover, dt, rt_val, rt_idx, null, -1, ensAxis, vertAxis);
    else {
      for (int i = 0; i < timeAxis.getSize(); i++) {
        CalendarDate timeDate = timeAxis.isInterval() ? timeAxis.getCoordBoundsMidpointDate(i) :  timeAxis.getCalendarDate(i);
        readAllEnsembles(cover, dt, rt_val, rt_idx, timeDate, i, ensAxis, vertAxis);
      }
    }
  }

  static void readAllTimes2D(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx,
                             TimeHelper helper, Array timeVals, CoordinateAxis1D ensAxis, CoordinateAxis1D vertAxis) {

    int[] shape = timeVals.getShape();
    if (timeVals.getRank() == 1) {
      timeVals.resetLocalIterator();
      int time_idx = 0;
      while (timeVals.hasNext()) {
        double timeVal = timeVals.nextDouble();
        readAllEnsembles(cover, dt, rt_val, rt_idx, helper.makeDate(timeVal), time_idx++, ensAxis, vertAxis);
      }

    } else {
      Index index = timeVals.getIndex();
      for (int i=0; i<shape[0]; i++) {
        double timeVal = (timeVals.getDouble(index.set(i,0)) + timeVals.getDouble(index.set(i,1))) / 2;
        readAllEnsembles(cover, dt, rt_val, rt_idx, helper.makeDate(timeVal), i, ensAxis, vertAxis);
      }
    }
  }

  static void readAllEnsembles(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx, CalendarDate time_val, int time_idx,
                               CoordinateAxis1D ensAxis, CoordinateAxis1D vertAxis) {
    if (ensAxis == null)
      readAllVertLevels(cover, dt, rt_val, rt_idx, time_val, time_idx, 0, -1, vertAxis);
    else {
      for (int i = 0; i < ensAxis.getSize(); i++)
        readAllVertLevels(cover, dt, rt_val, rt_idx, time_val, time_idx, ensAxis.getCoordValue(i), i, vertAxis);
    }
  }

  static void readAllVertLevels(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx, CalendarDate time_val, int time_idx,
                                double ens_val, int ens_idx, CoordinateAxis1D vertAxis) {
    if (vertAxis == null)
      readOneSlice(cover, dt, rt_val, rt_idx, time_val, time_idx, ens_val, ens_idx, 0, -1);
    else {
      for (int i = 0; i < vertAxis.getSize(); i++) {
        double levVal = vertAxis.isInterval() ? vertAxis.getCoordBoundsMidpoint(i) :  vertAxis.getCoordValue(i);
        readOneSlice(cover, dt, rt_val, rt_idx, time_val, time_idx, ens_val, ens_idx, levVal, i);
      }
    }
  }

  static void readOneSlice(Coverage cover, GridDatatype dt, CalendarDate rt_val, int rt_idx, CalendarDate time_val, int time_idx,
                           double ens_val, int ens_idx, double vert_val, int vert_idx) {
    System.out.printf("%n===Slice %s runtime=%s (%d) ens=%f (%d) time=%s (%d) vert=%f (%d) %n", cover.getName(), rt_val, rt_idx, ens_val, ens_idx, time_val, time_idx, vert_val, vert_idx);

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
      subset.set(SubsetParams.time, time_val);
    if (vert_idx >= 0)
      subset.set(SubsetParams.vertCoord, vert_val);

    GeoReferencedArray gc_array;
    try {
      gc_array = cover.readData(subset);
    } catch (IOException | InvalidRangeException e) {
      e.printStackTrace();
      return;
    }

    try {
      CompareNetcdf.compareData(dt_array, gc_array.getData());
    } catch (Throwable t) {
      try {
        dt.readDataSlice(rt_idx, ens_idx, time_idx, vert_idx, -1, -1);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    //NCdumpW.printArray(dt_array);
  }


}
