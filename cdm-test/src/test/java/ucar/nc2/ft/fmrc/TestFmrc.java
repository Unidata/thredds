/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.fmrc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.ma2.Array;
import ucar.nc2.NCdumpW;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
/**
 * Test FMRC aggregation
 *
 * @author caron
 * @since Feb 25, 2010
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestFmrc {
  private static String datadir = TestDir.cdmUnitTestDir + "ft/fmrc/";
  private static boolean showDetails = true;

  @Parameterized.Parameters(name="{0}")
  public static Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]{
            {TestDir.cdmUnitTestDir+"ft/fmrc/fp_precision/sediment_thickness_#yyMMddHHmm#.*\\.nc$", 2, 2, 4, 0, "thickness_of_sediment", 2, 72, 74, false},

            // String pathname, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes, String gridName, int nruns, int ntimes, int nbest) {
            {datadir + "toms/hiig_#yyyyMMdd#.nc", 25, 11, 13, 2, "u", 4, 25, 58, true},
            {TestDir.cdmUnitTestDir + "ncml/nc/ruc_conus40/RUC_CONUS_40km_#yyyyMMdd_HHmm#.grib1", 48, 15, -1, 6, "Pressure_tropopause", 3, 9, 9, true},
            {TestDir.cdmUnitTestDir + "ncml/agg/#yyyyMMdd_HHmm#.nc$", 10, 4, 8, 2, "Visibility", 4, 2, 8, true},
            {datadir + "bom/BoM_test.ncml", 1, 3, 8, 0, "eta_t", 2, 7, 10, true},
            {datadir + "ncom/ncom_fmrc.ncml", 1, 1, 5, 1, "surf_el", 3, 25, 41, true},
            // {datadir + "rtofs/rtofs.ncml", 9, 6, 10, 1, "N3-D_Temperature", 2, 3, 4, true}, // GRIB

            // ncml uses FMRC
            {TestDir.cdmUnitTestDir + "ncml/AggFmrcGribRunseq.ncml", 19, 6, 9, 2, "Temperature_height_above_ground", 4, 29, 35, true},
            // {TestDir.cdmUnitTestDir + "ncml/AggFmrcGrib.ncml", 58, 27, 22, 12, "Temperature_height_above_ground", 8, 29, 72, true}, bad idea FMRC on GRIB
            {TestDir.cdmUnitTestDir + "ncml/AggFmrcNonuniform.ncml", 48, 15, 17, 6, "Temperature_height_above_ground", 3, 9, 9, true},
            {TestDir.cdmUnitTestDir + "ncml/AggForecastModel.ncml", 41, 6, 10, 4, "u", 15, 11, 39, true},

            // fmrcSingle
            // {datadir + "nomads/nomads.ncml", 118, 20, 21, 14, "Temperature", 1, 3, 3, true}, GRIB

            // not regular
            {datadir + "bom/**/ocean_fc_#yyyyMMdd#_..._eta.nc$", 1, -1, 8, 0, "eta_t", 2, 7, 7, false},
    });
  }


  String pathname;
  int ngrids, ncoordSys, ncoordAxes, nVertCooordAxes;
  String gridName;
  int nruns, ntimes, nbest;

  public TestFmrc(String pathname, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes, String gridName, int nruns, int ntimes, int nbest, boolean regular) {
    this.pathname = pathname;
    this.ngrids = ngrids;
    this.ncoordSys = ncoordSys;
    this.ncoordAxes = ncoordAxes;
    this.nVertCooordAxes = nVertCooordAxes;
    this.gridName = gridName;
    this.nruns = nruns;
    this.ntimes = ntimes;
    this.nbest = nbest;

    FeatureCollectionConfig.setRegularizeDefault(regular);
  }

  @Test
  public void doOne2D() throws Exception {

    System.out.printf("%n====================2D dataset %s%n", pathname);
    Formatter errlog = new Formatter();
    Fmrc fmrc = Fmrc.open(pathname, errlog);
    assert (fmrc != null) : errlog;

    try (ucar.nc2.dt.GridDataset gridDs = fmrc.getDataset2D(null)) {
      NetcdfDataset ncd = (NetcdfDataset) gridDs.getNetcdfFile();

      int countGrids = gridDs.getGrids().size();
      int countCoordAxes = ncd.getCoordinateAxes().size();
      int countCoordSys = ncd.getCoordinateSystems().size();

      // count vertical axes
      int countVertCooordAxes = 0;
      for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
        AxisType t = axis.getAxisType();
        if ((t == AxisType.GeoZ) || (t == AxisType.Height) || (t == AxisType.Pressure))
          countVertCooordAxes++;
      }

      if (showDetails) {
        System.out.println(" grids=" + countGrids + ((ngrids < 0) ? " *" : ""));
        System.out.println(" coordSys=" + countCoordSys + ((ncoordSys < 0) ? " *" : ""));
        System.out.println(" coordAxes=" + countCoordAxes + ((ncoordAxes < 0) ? " *" : ""));
        System.out.println(" vertAxes=" + countVertCooordAxes + ((nVertCooordAxes < 0) ? " *" : ""));
      }

      for (ucar.nc2.dt.GridDataset.Gridset gridset1 : gridDs.getGridsets()) {
        gridset1.getGeoCoordSystem();
      }

      GridDatatype grid = gridDs.findGridDatatype(gridName);
      assert (grid != null) : "Cant find grid " + gridName;

      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1DTime runtime = gcs.getRunTimeAxis();
      assert (runtime != null) : "Cant find runtime for " + gridName;
      CoordinateAxis time = gcs.getTimeAxis();
      assert (time != null) : "Cant find time for " + gridName;
      assert (time.getRank() == 2) : "Time should be 2D " + gridName;

      if (showDetails) {
        System.out.println(" runtimes=" + runtime.getSize());
        System.out.println(" ntimes=" + time.getDimension(1).getLength());
      }

      if (ngrids >= 0)
        Assert.assertEquals("Number of Grids", ngrids, countGrids);
      if (ncoordSys >= 0)
        Assert.assertEquals("Number of CoordSys", ncoordSys, countCoordSys);

      if (ncoordAxes >= 0 && showDetails) { //  && (ncoordAxes != countCoordAxes)) {
        for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
          System.out.printf("axis= %s%n", axis.getNameAndDimensions());
          if (axis.getShortName().startsWith("layer_between")) {
            CoordinateAxis1D axis1 = (CoordinateAxis1D) axis;
            Array data = axis.read();
            NCdumpW.printArray(data);
            Formatter f = new Formatter();
            f.format("%n bounds1=");
            showArray(f, axis1.getBound1());
            f.format("%n bounds2=");
            showArray(f, axis1.getBound2());
            System.out.printf("%s%n", f);
          }
        }
      }

      if (ncoordAxes >= 0)
        Assert.assertEquals("Number of CoordAxes", ncoordAxes, countCoordAxes);
      if (nVertCooordAxes >= 0)
        Assert.assertEquals("Number of VertCooordAxes", nVertCooordAxes, countVertCooordAxes);

      if (nruns >= 0)
        Assert.assertEquals("Number of runs", nruns, runtime.getSize());
        assert runtime.getSize() == nruns : runtime.getSize() + " != " + nruns;
      if (nruns >= 0)
        Assert.assertEquals("Time Dimension(0) length", nruns, time.getDimension(0).getLength());
      if (ntimes >= 0)
        Assert.assertEquals("Time Dimension(1) ntimes", ntimes, time.getDimension(1).getLength());
    }
  }

  void showArray(Formatter f, double[] array) {
    for (double d : array) f.format("%f ", d);
  }

  @Test
  public void doOneBest() throws Exception {

    System.out.printf("%n=========================Best dataset %s grid=%s%n", pathname, gridName);
    Formatter errlog = new Formatter();
    Fmrc fmrc = Fmrc.open(pathname, errlog);
    if (fmrc == null) {
      System.out.printf("Fmrc failed to open %s%n", pathname);
      System.out.printf("errlog= %s%n", errlog.toString());
      return;
    }

    ucar.nc2.dt.GridDataset gridDs = fmrc.getDatasetBest();
    NetcdfDataset ncd = (NetcdfDataset) gridDs.getNetcdfFile();

    int countGrids = gridDs.getGrids().size();
    int countCoordAxes = ncd.getCoordinateAxes().size();
    int countCoordSys = ncd.getCoordinateSystems().size();

    // count vertical axes
    int countVertCooordAxes = 0;
    for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
      AxisType t = axis.getAxisType();
      if ((t == AxisType.GeoZ) || (t == AxisType.Height) || (t == AxisType.Pressure))
        countVertCooordAxes++;
    }

    if (showDetails) {
      System.out.println(" grids=" + countGrids + ((ngrids < 0) ? " *" : ""));
      System.out.println(" coordSys=" + countCoordSys + ((ncoordSys < 0) ? " *" : ""));
      System.out.println(" coordAxes=" + countCoordAxes + ((ncoordAxes < 0) ? " *" : ""));
      System.out.println(" vertAxes=" + countVertCooordAxes + ((nVertCooordAxes < 0) ? " *" : ""));
    }

    for (ucar.nc2.dt.GridDataset.Gridset gridset : gridDs.getGridsets()) {
      gridset.getGeoCoordSystem();
    }

    if (ngrids >= 0)
      Assert.assertEquals("Number of Grids", ngrids, countGrids);

    GridDatatype grid = gridDs.findGridDatatype(gridName);
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1DTime runtime = gcs.getRunTimeAxis();

    assert (runtime != null) : "Cant find runtime for " + gridName;

    CoordinateAxis time = gcs.getTimeAxis();
    assert (time != null) : "Cant find time for " + gridName;
    Assert.assertEquals("Rank of Best times", 1, time.getRank());

    if (showDetails) {
      System.out.println(" ntimes=" + time.getDimension(0).getLength());
    }

    if (nbest >= 0)
      Assert.assertEquals("Number of Best times for "+gridName, nbest, time.getDimension(0).getLength());

    gridDs.close();
  }

}
