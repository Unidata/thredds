package ucar.nc2.dt.grid;

import junit.framework.*;
import ucar.nc2.iosp.grib.GribServiceProvider;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.TestAll;

import java.util.List;
import java.util.Iterator;

/** Count geogrid objects - sanity check when anything changes. */

public class TestReadandCount extends TestCase {
  private String outDir = "test/data/dataset/out/";
  private static boolean show = false, showCount = true;

  public TestReadandCount( String name) {
    super(name);
  }

  private String griddir = TestAll.testdataDir+"grid/netcdf/";


  public void testRead() throws Exception {

    doOne(griddir+"atd-radar/","rgg.20020411.000000.lel.ll.nc", 5, 1, 4, 1);
    doOne(griddir+"atd-radar/","SPOL_3Volumes.nc", 3, 1, 4, 1);

    doOne(griddir+"gief/","coamps.wind_uv.nc", 2, 1, 4, 1);

    //// coards derived
    doOne(griddir+"coards/", "cldc.mean.nc", 1, 1, 3, 0);
    doOne(griddir+"coards/","inittest24.QRIDV07200.nc", -1, -1, -1, -1); // no "positive" att on level
    doOne(griddir+"coards/","inittest24.QRIDV07200.ncml", 1, 1, 3, 1);

    doOne(griddir+"csm/","o3monthly.nc", 4, 1, 7, 2);
    doOne(griddir+"csm/","ha0001.nc", 35, 3, 5, 2); //

    doOne(griddir+"cf/","cf1.nc", 1,1,5,2);
    doOne(griddir+"cf/","ccsm2.nc", 107, 3, 5, 2); //
    doOne(griddir+"cf/","tomw.nc", 19, 3, 4, 1);
    doOne(griddir+"cf/","cf1_rap.nc", 11, 1, 4, 0); // not getting x, y
    // doOne("C:/data/conventions/cf/signell/","signell_july2_03.nc", -1, -1, -1, -1); // 2D lat, lon; no x,y
//**    doOne(griddir+"cf/","feb2003_short.nc", 14, 4, 4, 1);
    doOne(griddir+"cf/","feb2003_short2.nc", 22, 9, 8, 1);
    doOne(griddir+"cf/","temperature.nc", 2, 2, 5, 1);

    doOne(griddir+"gdv/","testGDV.nc", 30, 1, 4, 1);
    doOne(griddir+"gdv/","OceanDJF.nc", 15, 1, 4, 1);

    // uses GDV as default
    doOne(griddir+"mars/","temp_air_01082000.nc", 1, 1, 4, 1); // uses GDV
    //doOne("C:/data/conventions/mm5/","n040.nc", -1, -1, -1, -1); // no Conventions code

    //// the uglies
    doOne(griddir+"nuwg/", "avn-x.nc", 31, 4, 8, 4);
    doOne(griddir+"nuwg/", "2003021212_avn-x.nc", 30, 5, 7, 4);
    doOne(griddir+"nuwg/", "avn-q.nc", 22, 7, 9, 6);
    doOne(griddir+"nuwg/", "eta.nc", 28,9,11,8);
    doOne(griddir+"nuwg/", "ocean.nc", 5, 1, 3, 0);
    doOne(griddir+"nuwg/", "ruc.nc", 31,5, 6, 3);
    doOne(griddir+"nuwg/", "CMC-HGT.nc", 1, 1, 3, 0); // */

    doOne(griddir+"wrf/","wrfout_v2_Lambert.nc", 57, 11, 8, 3);
    doOne(griddir+"wrf/","wrf2-2005-02-01_12.nc", 60, 11, 8, 3);
    doOne(griddir+"wrf/","wrfout_d01_2006-03-08_21-00-00", 70, 11, 8, 3);
    doOne(griddir+"wrf/","wrfrst_d01_2002-07-02_12_00_00.nc", 162, 11, 8, 3);

    doOne(griddir+"awips/","19981109_1200.nc", 36, 13, 14, 11);
    doOne(griddir+"awips/","awips.nc", 38, 12, 13, 10); //

    doOne(griddir+"ifps/","HUNGrids.netcdf", 26, 26, 29, 0); // *

    // our grib reader */
    doOne(TestAll.testdataDir+"grid/grib/grib1/data/","AVN.wmo", 22, -1, -1, -1);
    doOne(TestAll.testdataDir+"grid/grib/grib1/data/","RUC_W.wmo", 44,-1, -1, -1);
    doOne(TestAll.testdataDir+"grid/grib/grib1/data/","NOGAPS-Temp-Regional.grib", 1, -1, -1, -1);

    doOne(TestAll.testdataDir+"grid/grib/grib2/data/","eta2.wmo", 35, -1, -1, -1);
    doOne(TestAll.testdataDir+"grid/grib/grib2/data/","ndfd.wmo", 1, -1, -1, -1);

      // radar mosaic
    doOne(TestAll.testdataDir+"grid/grib/grib1/data/","radar_national.grib", 1, -1, -1, -1);
    doOne(TestAll.testdataDir+"grid/grib/grib1/data/","radar_regional.grib", 1, -1, -1, -1);

    // redo grib files, forcing new index
    GribServiceProvider.forceNewIndex = true;
    doOne(TestAll.testdataDir+"grid/grib/grib1/data/","AVN.wmo", 22, -1, -1, -1);
    doOne(TestAll.testdataDir+"grid/grib/grib1/data/","RUC_W.wmo", 44, -1, -1, -1);
    doOne(TestAll.testdataDir+"grid/grib/grib2/data/","eta2.wmo", 35, -1, -1, -1);
    doOne(TestAll.testdataDir+"grid/grib/grib2/data/","ndfd.wmo", 1, -1, -1, -1);
    GribServiceProvider.forceNewIndex = false;
  }

  static void doOne(String dir, String filename, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) throws Exception {
    System.out.println("test read GridDataset = " + dir + filename);
    ucar.nc2.dt.grid.GridDataset gridDs = GridDataset.open(dir + filename);

    int countGrids = gridDs.getGrids().size();
    int countCoordAxes = gridDs.getNetcdfDataset().getCoordinateAxes().size();
    int countCoordSys = gridDs.getNetcdfDataset().getCoordinateSystems().size();

    // count vertical axes
    int countVertCooordAxes = 0;
    List axes = gridDs.getNetcdfDataset().getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis =  (CoordinateAxis) axes.get(i);
      AxisType t = axis.getAxisType();
      if ((t == AxisType.GeoZ) || (t == AxisType.Height) || (t == AxisType.Pressure) )
        countVertCooordAxes++;
    }

    Iterator iter = gridDs.getGridsets().iterator();
    while (iter.hasNext()) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) iter.next();
      GridCoordSys gcs = gridset.getGeoCoordSys();
      //if (gcs.hasTimeAxis())
      //  System.out.println(" "+gcs.isDate()+" "+gcs.getName());
    }

    if (showCount) {
      System.out.println(" grids=" + countGrids + ((ngrids < 0) ? " *" : ""));
      System.out.println(" coordSys=" + countCoordSys + ((ncoordSys < 0) ? " *" : ""));
      System.out.println(" coordAxes=" + countCoordAxes + ((ncoordAxes < 0) ? " *" : ""));
      System.out.println(" vertAxes=" + countVertCooordAxes + ((nVertCooordAxes < 0) ? " *" : ""));
    }

    if (ngrids >= 0)
      assert ngrids == countGrids : "Grids " + ngrids + " != " + countGrids;
    if (ncoordSys >= 0)
      assert ncoordSys == countCoordSys : "CoordSys " + ncoordSys + " != " + countCoordSys;
    if (ncoordAxes >= 0)
      assert ncoordAxes == countCoordAxes : "CoordAxes " + ncoordAxes + " != " + countCoordAxes;
    if (nVertCooordAxes >= 0)
      assert nVertCooordAxes == countVertCooordAxes : "VertAxes" + nVertCooordAxes + " != " + countVertCooordAxes;

    if (show)
      System.out.println(gridDs.getInfo());

    gridDs.close();
  }

   public static void main( String arg[]) throws Exception {
     // new TestReadandCount("dummy").doOne("C:/data/conventions/wrf/","wrf.nc", 33, 5, 7, 7);  // missing TSLB
     new TestReadandCount("dummy").testRead();  // missing TSLB
  }

}
