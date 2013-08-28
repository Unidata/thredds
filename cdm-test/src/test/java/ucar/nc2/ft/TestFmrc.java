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

package ucar.nc2.ft;

import org.junit.Test;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.unidata.test.util.TestDir;

import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

/**
 * Test FMRC aggregation
 *
 * @author caron
 * @since Feb 25, 2010
 */
public class TestFmrc {
  private static String datadir = TestDir.cdmUnitTestDir + "ft/fmrc/";
  private static boolean showCount = true;

  @Test
  public void testCollections() throws Exception {
    try {
      FeatureCollectionConfig.setRegularizeDefault(true);

      // spec
      doOne(datadir + "toms/hiig_#yyyyMMdd#.nc", 25, 11, 13, 2, "u", 4, 25, 58);
      doOne(TestDir.cdmUnitTestDir + "ncml/nc/ruc_conus40/RUC_CONUS_40km_#yyyyMMdd_HHmm#.grib1", 48, 12, 16, 6, "Pressure_tropopause", 3, 9, 9);

      // really a joinExisting
      doOne(TestDir.cdmUnitTestDir + "ncml/agg/#yyyyMMdd_HHmm#.nc$", 10, 4, 8, 2, "Visibility", 4, 2, 8);

      // catalog
      //doOne("catalog:http://motherlode.ucar.edu:8081/thredds/catalog/fmrc/NCEP/GFS/Hawaii_160km/files/catalog.xml", 15, 8, 11, 6, "Temperature", -1, 21, -1);

      // from an ncml aggregation
      doOne(datadir + "bom/BoM_test.ncml", 1, 3, 8, 0, "eta_t", 2, 7, 10);
      doOne(datadir + "ncom/ncom_fmrc.ncml", 1, 1, 5, 1, "surf_el", 3, 25, 41);
      //doOne(datadir + "rtofs/rtofs.ncml", 9, 6, 10, 1, "N3-D_Temperature", 2, 3, 4); // weird change in variable "V" name, causes out-of-heap due to caching in FmrcDataset

      doOne(TestDir.cdmUnitTestDir + "ncml/AggFmrcGribRunseq.ncml", 13, 5, 7, 2, "Temperature_height_above_ground", 4, 29, 35);
      doOne(TestDir.cdmUnitTestDir + "ncml/AggFmrcGrib.ncml", 54, 19, 19, 12, "Temperature_height_above_ground", 8, 29, 72);
      doOne(TestDir.cdmUnitTestDir + "ncml/AggFmrcNonuniform.ncml", 48, 12, 16, 6, "Temperature_height_above_ground", 3, 9, 9);
      doOne(TestDir.cdmUnitTestDir + "ncml/AggForecastModel.ncml", 41, 6, 10, 4, "u", 14, 11, 37);     //*/

      // fmrcSingle
      // doOne(TestDir.cdmLocalTestDataDir + "ncml/offsite/aggFmrcScan2.ncml", 158, 27, 28, 23, "Temperature", 2, 3,  6);
      // doOne(datadir + "nomads/nomads.ncml", 118, 20, 21, 14, "Temperature", 1, 3, 3);

      // needs ncmlInner to work
      //doOne(datadir + "gomoos/grid.ncml", 16, -1, 7, 1, "salt", 2, 21, 29);     //*/

      // ncml with remote scan (collection)

      // blank
      // doOne(datadir + "rtofs/rtofs.ncml", -1, -1, -1, -1, "Temperature", -1, -1, -1);     //*/

    } finally {
//      MetadataManager.closeAll();
    }
  }

  @Test
   public void testCollectionsNotRegular() throws Exception {
     try {
       FeatureCollectionConfig.setRegularizeDefault(false);
       doOne(datadir + "bom/**/ocean_fc_#yyyyMMdd#_..._eta.nc$", 1, 1, 8, 0, "eta_t", 2, 7, 7); // Q:/cdmUnitTest/fmrc/bom/**/ocean_fc_#yyyyMMdd#_..._eta.nc$

     } finally {
 //      MetadataManager.closeAll();
     }
   }

  public void testProblem() throws Exception {
    doOne(datadir + "bom/**/ocean_fc_#yyyyMMdd#_..._eta.nc$", 1, 1, 8, 0, "eta_t", 2, 7, 14); // Q:/cdmUnitTest/fmrc/bom/**/ocean_fc_#yyyyMMdd#_..._eta.nc$
  }


  static void doOne(String pathname, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes,
                    String gridName, int nruns, int ntimes, int nbest) throws Exception {

    System.out.println("\ntest read Fmrc = " + pathname);
    doOne2D(pathname, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes, gridName, nruns, ntimes);
    doOneBest(pathname, ngrids, ncoordSys, ncoordAxes - 1, nVertCooordAxes, gridName, nruns, nbest);

  }

  static void doOne2D(String pathname, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes,
                      String gridName, int nruns, int ntimes) throws Exception {

    System.out.println("2D dataset");
    Formatter errlog = new Formatter();
    Fmrc fmrc = Fmrc.open(pathname, errlog);
    if (fmrc == null) {
      System.out.printf("Fmrc failed to open %s%n", pathname);
      System.out.printf("errlog= %s%n", errlog.toString());
      return;
    }

    ucar.nc2.dt.GridDataset gridDs = fmrc.getDataset2D(null);
    NetcdfDataset ncd = (NetcdfDataset) gridDs.getNetcdfFile();

    int countGrids = gridDs.getGrids().size();
    int countCoordAxes = ncd.getCoordinateAxes().size();
    int countCoordSys = ncd.getCoordinateSystems().size();

    // count vertical axes
    int countVertCooordAxes = 0;
    List axes = ncd.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      AxisType t = axis.getAxisType();
      if ((t == AxisType.GeoZ) || (t == AxisType.Height) || (t == AxisType.Pressure))
        countVertCooordAxes++;
    }

    if (showCount) {
      System.out.println(" grids=" + countGrids + ((ngrids < 0) ? " *" : ""));
      System.out.println(" coordSys=" + countCoordSys + ((ncoordSys < 0) ? " *" : ""));
      System.out.println(" coordAxes=" + countCoordAxes + ((ncoordAxes < 0) ? " *" : ""));
      System.out.println(" vertAxes=" + countVertCooordAxes + ((nVertCooordAxes < 0) ? " *" : ""));
    }

    Iterator iter = gridDs.getGridsets().iterator();
    while (iter.hasNext()) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) iter.next();
      gridset.getGeoCoordSystem();
    }

    GridDatatype grid = gridDs.findGridDatatype(gridName);
    assert (grid != null) : "Cant find grid " + gridName;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1DTime runtime = gcs.getRunTimeAxis();
    assert (runtime != null) : "Cant find runtime for " + gridName;
    CoordinateAxis time = gcs.getTimeAxis();
    assert (time != null) : "Cant find time for " + gridName;
    assert (time.getRank() == 2) : "Time should be 2D " + gridName;

    if (showCount) {
      System.out.println(" runtimes=" + runtime.getSize());
      System.out.println(" ntimes=" + time.getDimension(1).getLength());
    }

    if (ngrids >= 0)
      assert ngrids == countGrids : "Grids " + ngrids + " != " + countGrids;
    //if (ncoordSys >= 0)
    //  assert ncoordSys == countCoordSys : "CoordSys " + ncoordSys + " != " + countCoordSys;
    if (ncoordAxes >= 0)
      assert ncoordAxes == countCoordAxes : "CoordAxes " + ncoordAxes + " != " + countCoordAxes;
    if (nVertCooordAxes >= 0)
      assert nVertCooordAxes == countVertCooordAxes : "VertAxes" + nVertCooordAxes + " != " + countVertCooordAxes;

    if (nruns >= 0)
      assert runtime.getSize() == nruns : runtime.getSize() + " != " + nruns;
    if (nruns >= 0)
      assert time.getDimension(0).getLength() == nruns : " nruns should be " + nruns;
    if (ntimes >= 0)
      assert time.getDimension(1).getLength() == ntimes : " ntimes should be " + ntimes+" actual "+time.getDimension(1).getLength();

    gridDs.close();
  }

  static void doOneBest(String pathname, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes,
                        String gridName, int nruns, int ntimes) throws Exception {

    System.out.println("Best dataset");
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
    List axes = ncd.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      AxisType t = axis.getAxisType();
      if ((t == AxisType.GeoZ) || (t == AxisType.Height) || (t == AxisType.Pressure))
        countVertCooordAxes++;
    }

    if (showCount) {
      System.out.println(" grids=" + countGrids + ((ngrids < 0) ? " *" : ""));
      System.out.println(" coordSys=" + countCoordSys + ((ncoordSys < 0) ? " *" : ""));
      System.out.println(" coordAxes=" + countCoordAxes + ((ncoordAxes < 0) ? " *" : ""));
      System.out.println(" vertAxes=" + countVertCooordAxes + ((nVertCooordAxes < 0) ? " *" : ""));
    }

    Iterator iter = gridDs.getGridsets().iterator();
    while (iter.hasNext()) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) iter.next();
      gridset.getGeoCoordSystem();
    }

    GridDatatype grid = gridDs.findGridDatatype(gridName);
    assert (grid != null) : "Cant find grid " + gridName;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1DTime runtime = gcs.getRunTimeAxis();
    //System.out.println(" has runtime axis=" +  (runtime != null));

    assert (runtime != null) : "Cant find runtime for " + gridName;
    //assert (runtime == null) : "Should not have runtime coord= "+runtime;

    CoordinateAxis time = gcs.getTimeAxis();
    assert (time != null) : "Cant find time for " + gridName;

    if (showCount) {
      System.out.println(" ntimes=" + time.getDimension(0).getLength());
    }

    if (ngrids >= 0)
      assert ngrids == countGrids : "Grids " + ngrids + " != " + countGrids;
    /* if (ncoordSys >= 0)
      assert ncoordSys == countCoordSys : "CoordSys " + ncoordSys + " != " + countCoordSys;
    if (ncoordAxes >= 0)
      assert ncoordAxes == countCoordAxes : "CoordAxes " + ncoordAxes + " != " + countCoordAxes;
    if (nVertCooordAxes >= 0)
      assert nVertCooordAxes == countVertCooordAxes : "VertAxes" + nVertCooordAxes + " != " + countVertCooordAxes;  */
    if (ntimes >= 0)
      assert time.getDimension(0).getLength() == ntimes : " ntimes should be " + ntimes + " instead = " + time.getDimension(0).getLength();


    gridDs.close();
  }

}
