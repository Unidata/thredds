/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dt.grid;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.Range;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.ft.cover.impl.CoverageCSFactory;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test Grid CoordSystem and subsets are ok
 *
 * @author caron
 * @since 4/11/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridSubsetCoordinateSystem {

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx3", "Temperature_sigma"});         // SRC                               // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx3", "Pressure_surface"});                                         // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/grib1/2001/ds083.2_Aggregation-2001.ncx3", "Temperature_surface"});  // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/grib1/ds083.2_Aggregation.ncx3", "Temperature_surface"});  // TPofP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3", "Best/Soil_temperature_depth_below_surface_layer"});  // TwoD Best
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3", "TwoD/Soil_temperature_depth_below_surface_layer"});  // TwoD
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.1/yearPartition-1979.ncx3", "Runoff_surface_12_Hour_Average"});  // MRSTC

    //result.add(new Object[]{"B:/ncdc/0409/narr/Narr_A_fc.ncx3", "Accum_snow_surface"});
    //result.add(new Object[]{"B:/ncdc/0409/narr/Narr_A_fc.ncx3", "Convective_cloud_cover_entire_atmosphere_3_Hour_Average"});  // need more than one time/reftime

    return result;
  }

  final String filename, gridName;
  public TestGridSubsetCoordinateSystem(String filename, String gridName) {
    this.filename = filename;
    this.gridName = gridName;
  }

      // has runtime(time), time(time)
  @Test
  public void testGridDomain() throws Exception {
    System.err.printf("%nOpen %s grid='%s'%n", filename, gridName);
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    try (GridDataset dataset = GridDataset.open(filename)) {
      GeoGrid grid = dataset.findGridByName(gridName);
      GridCoordSystem gcs = grid.getCoordinateSystem();
      System.err.printf("%s%n", gcs);
      testDomain("original grid", grid.getDimensions(), gcs.getCoordinateAxes());

      GridDatatype gridSubset = grid.makeSubset(null, null, new Range(0, 0), null, null, null);
      GridCoordSystem gcsSubset = gridSubset.getCoordinateSystem();
      System.err.printf("%s%n", gcsSubset);
      testDomain("subset grid", gridSubset.getDimensions(), gcsSubset.getCoordinateAxes());

    } finally {
      GribIosp.setDebugFlags(new DebugFlagsImpl(""));
    }
  }

  private void testDomain(String which, List<Dimension> domain, List<CoordinateAxis> axes) {
    for (CoordinateAxis axis : axes) {
      List<Dimension> dims = axis.getDimensions();
      for (Dimension d : dims)
        if (!domain.contains(d)) {
          System.err.printf("    %s: illegal dimension '%s' in axis %s%n", which, d.getFullName(), axis.getNameAndDimensions());
          assert false;
      }
    }
  }

  @Test
  public void testCoverageDomain() throws Exception {
    System.err.printf("%nOpen %s grid='%s'%n", filename, gridName);
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));

    try (NetcdfDataset ncd = NetcdfDataset.openDataset(filename)) {
      VariableDS vds = (VariableDS) ncd.findVariable(gridName);
      for (CoordinateSystem cs : vds.getCoordinateSystems()) {
        Formatter errlog = new Formatter();
        CoverageCS cov = CoverageCSFactory.make(ncd, cs, errlog);
        System.err.printf("%s errs='%s'%n", cov, errlog);
        for (CoordinateAxis axis :  cov.getCoordinateAxes()) {
          System.err.printf(" %s%n", axis.getNameAndDimensions());
        }
        // testDomain("coverage", cov.getDimensions(), gcsSubset.getCoordinateAxes());

      }

    } finally {
      GribIosp.setDebugFlags(new DebugFlagsImpl(""));
    }
  }

  @Test
  public void testCoordinateSystemDomain() throws Exception {
    System.err.printf("%nOpen %s grid='%s'%n", filename, gridName);
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));

    try (NetcdfDataset ncd = NetcdfDataset.openDataset(filename)) {
      VariableDS vds = (VariableDS) ncd.findVariable(gridName);
      for (CoordinateSystem cs : vds.getCoordinateSystems()) {
        System.err.printf("  CoordinateSystem= '%s'%n", cs);
        testDomain("CoordinateSystem ", vds.getDimensions(), cs.getCoordinateAxes());
      }

    } finally {
      GribIosp.setDebugFlags(new DebugFlagsImpl(""));
    }
  }

}
