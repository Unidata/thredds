/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.dt.grid;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.ncml.NcMLReader;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.ByteArrayInputStream;

/** Count geogrid objects - sanity check when anything changes. */

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadandCount {
  private static final boolean show = false, showCount = true;
  private static String griddir = TestDir.cdmUnitTestDir +"conventions/";
  private static String grib1dir = TestDir.cdmUnitTestDir +"formats/grib1/";
  private static String grib2dir = TestDir.cdmUnitTestDir +"formats/grib2/";

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{griddir + "avhrr/", "amsr-avhrr-v2.20040729.nc", 0, 1, 4, 0});

       /* doOne(griddir+"atd-radar/","rgg.20020411.000000.lel.ll.nc", 5, 1, 4, 1);
    doOne(griddir+"atd-radar/","SPOL_3Volumes.nc", 3, 1, 4, 1);

    doOne(griddir+"gief/","coamps.wind_uv.nc", 2, 1, 4, 1);

    //// coards derived
    doOne(griddir+"coards/", "cldc.mean.nc", 1, 1, 3, 0);
    doOne(griddir+"coards/","inittest24.QRIDV07200.nc", -1, -1, -1, -1); // no "positive" att on level
    */
    result.add(new Object[]{griddir+"coards/","inittest24.QRIDV07200.ncml", 1, 1, 3, 1});
//    result.add(new Object[]{griddir+"avhrr/","amsr-avhrr-v2.20040729.nc", 4, 1, 4, 1});

//    result.add(new Object[]{griddir+"csm/","o3monthly.nc", 4, 1, 7, 2});
    result.add(new Object[]{griddir+"csm/","ha0001.nc", 35, 3, 5, 2}); //

    result.add(new Object[]{griddir+"cf/","cf1.nc", 1,1,5,2});
    result.add(new Object[]{griddir+"cf/","ccsm2.nc", 107, 3, 5, 2}); //
    result.add(new Object[]{griddir+"cf/","tomw.nc", 19, 3, 4, 1});
    result.add(new Object[]{griddir+"cf/","cf1_rap.nc", 11, 2, 4, 0}); // not getting x, y
    // result.add(new Object[]{"C:/data/conventions/cf/signell/","signell_july2_03.nc", -1, -1, -1, -1}); // 2D lat, lon; no x,y
//**    result.add(new Object[]{griddir+"cf/","feb2003_short.nc", 14, 4, 4, 1});
    result.add(new Object[]{griddir+"cf/","feb2003_short2.nc", 22, 9, 8, 1});
    result.add(new Object[]{griddir+"cf/","temperature.nc", 4, 3, 5, 1});

    result.add(new Object[]{griddir+"gdv/","testGDV.nc", 30, 1, 4, 1});
    result.add(new Object[]{griddir+"gdv/","OceanDJF.nc", 15, 1, 4, 1});

    // uses GDV as default
    result.add(new Object[]{griddir+"mars/","temp_air_01082000.ncml", 1, 1, 4, 1}); // uses GDV
    //result.add(new Object[]{"C:/data/conventions/mm5/","n040.nc", -1, -1, -1, -1}); // no Conventions code

    result.add(new Object[]{griddir+"m3io/","agg.cctmJ3fx.b312.nh3c1.dep_wa.annual.2001base.nc", 13, 1, 5, 1}); // m3io
    result.add(new Object[]{griddir+"m3io/","19L.nc", 23, 1, 4, 1});   // M3IOVGGrid

    //// the uglies
    result.add(new Object[]{griddir+"nuwg/", "avn-x.nc", 31, 4, 8, 4});
    result.add(new Object[]{griddir+"nuwg/", "2003021212_avn-x.nc", 30, 5, 7, 4});
    result.add(new Object[]{griddir+"nuwg/", "avn-q.nc", 22, 7, 9, 6});
    result.add(new Object[]{griddir+"nuwg/", "eta.nc", 28,9,11,8});
    result.add(new Object[]{griddir+"nuwg/", "ocean.nc", 5, 1, 3, 0});
    result.add(new Object[]{griddir+"nuwg/", "ruc.nc", 31,5, 6, 3});
    result.add(new Object[]{griddir+"nuwg/", "CMC-HGT.nc", 1, 1, 3, 0}); // */

    result.add(new Object[]{griddir+"wrf/","wrfout_v2_Lambert.nc", 57, 11, 8, 3});
    result.add(new Object[]{griddir+"wrf/","wrf2-2005-02-01_12.nc", 60, 11, 8, 3});
    result.add(new Object[]{griddir+"wrf/","wrfout_d01_2006-03-08_21-00-00", 70, 11, 8, 3});
    result.add(new Object[]{griddir+"wrf/","wrfrst_d01_2002-07-02_12_00_00.nc", 162, 11, 8, 3});

    result.add(new Object[]{griddir+"awips/","19981109_1200.nc", 36, 13, 14, 11});
    result.add(new Object[]{griddir+"awips/","awips.nc", 38, 12, 13, 10}); //

    result.add(new Object[]{griddir+"ifps/","HUNGrids.netcdf", 26, 26, 29, 0}); // *

    // our grib reader */
    result.add(new Object[]{ grib1dir ,"AVN.wmo", 22, -1, -1, -1});
    result.add(new Object[]{ grib1dir,"RUC_W.wmo", 44,-1, -1, -1});
    result.add(new Object[]{ grib1dir,"NOGAPS-Temp-Regional.grib", 1, -1, -1, -1});  // */

    result.add(new Object[]{ grib1dir,"eta.Y.Q.wmo", 25, -1, -1, -1});
    result.add(new Object[]{ grib2dir,"ndfd.wmo", 1, -1, -1, -1});

      // radar mosaic
    result.add(new Object[]{ grib1dir,"radar_national.grib", 1, 1, 3, 0});
    result.add(new Object[]{ grib1dir,"radar_regional.grib", 1, 1, 3, 0});

    return result;
  }


  /* public void testRead() throws Exception {

    /* doOne(griddir+"atd-radar/","rgg.20020411.000000.lel.ll.nc", 5, 1, 4, 1);
    doOne(griddir+"atd-radar/","SPOL_3Volumes.nc", 3, 1, 4, 1);

    doOne(griddir+"gief/","coamps.wind_uv.nc", 2, 1, 4, 1);

    //// coards derived
    doOne(griddir+"coards/", "cldc.mean.nc", 1, 1, 3, 0);
    doOne(griddir+"coards/","inittest24.QRIDV07200.nc", -1, -1, -1, -1); // no "positive" att on level

    doOne(griddir+"coards/","inittest24.QRIDV07200.ncml", 1, 1, 3, 1);
//    doOne(griddir+"avhrr/","amsr-avhrr-v2.20040729.nc", 4, 1, 4, 1);

//    doOne(griddir+"csm/","o3monthly.nc", 4, 1, 7, 2);
    doOne(griddir+"csm/","ha0001.nc", 35, 3, 5, 2); //

    doOne(griddir+"cf/","cf1.nc", 1,1,5,2);
    doOne(griddir+"cf/","ccsm2.nc", 107, 3, 5, 2); //
    doOne(griddir+"cf/","tomw.nc", 19, 3, 4, 1);
    doOne(griddir+"cf/","cf1_rap.nc", 11, 2, 4, 0); // not getting x, y
    // doOne("C:/data/conventions/cf/signell/","signell_july2_03.nc", -1, -1, -1, -1); // 2D lat, lon; no x,y
//**    doOne(griddir+"cf/","feb2003_short.nc", 14, 4, 4, 1);
    doOne(griddir+"cf/","feb2003_short2.nc", 22, 9, 8, 1);
    doOne(griddir+"cf/","temperature.nc", 4, 3, 5, 1);

    doOne(griddir+"gdv/","testGDV.nc", 30, 1, 4, 1);
    doOne(griddir+"gdv/","OceanDJF.nc", 15, 1, 4, 1);

    // uses GDV as default
    doOne(griddir+"mars/","temp_air_01082000.ncml", 1, 1, 4, 1); // uses GDV
    //doOne("C:/data/conventions/mm5/","n040.nc", -1, -1, -1, -1); // no Conventions code

    doOne(griddir+"m3io/","agg.cctmJ3fx.b312.nh3c1.dep_wa.annual.2001base.nc", 13, 1, 5, 1); // m3io
    doOne(griddir+"m3io/","19L.nc", 23, 1, 4, 1);   // M3IOVGGrid

    //// the uglies
    doOne(griddir+"nuwg/", "avn-x.nc", 31, 4, 8, 4);
    doOne(griddir+"nuwg/", "2003021212_avn-x.nc", 30, 5, 7, 4);
    doOne(griddir+"nuwg/", "avn-q.nc", 22, 7, 9, 6);
    doOne(griddir+"nuwg/", "eta.nc", 28,9,11,8);
    doOne(griddir+"nuwg/", "ocean.nc", 5, 1, 3, 0);
    doOne(griddir+"nuwg/", "ruc.nc", 31,5, 6, 3);
    doOne(griddir+"nuwg/", "CMC-HGT.nc", 1, 1, 3, 0); //

    doOne(griddir+"wrf/","wrfout_v2_Lambert.nc", 57, 11, 8, 3);
    doOne(griddir+"wrf/","wrf2-2005-02-01_12.nc", 60, 11, 8, 3);
    doOne(griddir+"wrf/","wrfout_d01_2006-03-08_21-00-00", 70, 11, 8, 3);
    doOne(griddir+"wrf/","wrfrst_d01_2002-07-02_12_00_00.nc", 162, 11, 8, 3);

    doOne(griddir+"awips/","19981109_1200.nc", 36, 13, 14, 11);
    doOne(griddir+"awips/","awips.nc", 38, 12, 13, 10); //

    doOne(griddir+"ifps/","HUNGrids.netcdf", 26, 26, 29, 0); // *

    // our grib reader
    doOne( grib1dir ,"AVN.wmo", 22, -1, -1, -1);
    doOne( grib1dir,"RUC_W.wmo", 44,-1, -1, -1);
    doOne( grib1dir,"NOGAPS-Temp-Regional.grib", 1, -1, -1, -1);  //

    doOne( grib1dir,"eta.Y.Q.wmo", 25, -1, -1, -1);
    doOne( grib2dir,"ndfd.wmo", 1, -1, -1, -1);

      // radar mosaic
    doOne( grib1dir,"radar_national.grib", 1, 1, 3, 0);
    doOne( grib1dir,"radar_regional.grib", 1, 1, 3, 0);
  }  */

  String dir, name;
  int ngrids, ncoordSys, ncoordAxes, nVertCooordAxes;

  public TestReadandCount( String dir, String name, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) {
    this.dir = dir;
    this.name = name;
    this.ngrids = ngrids;
    this.ncoordSys = ncoordSys;
    this.ncoordAxes = ncoordAxes;
    this.nVertCooordAxes = nVertCooordAxes;
  }

  @org.junit.Test
  public void openAsGridAndCount() throws Exception {
    doOne(dir, name, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }

  static public void doOne( String dir, String name, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) throws Exception {
    System.out.printf("test read GridDataset= %s%s%n", dir, name);
    ucar.nc2.dt.grid.GridDataset gridDs = GridDataset.open(dir + name);
    System.out.printf(" location= %s%n%n", gridDs.getLocation());

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
    //if (ncoordSys >= 0)
    //  assert ncoordSys == countCoordSys : "CoordSys " + ncoordSys + " != " + countCoordSys;
    //if (ncoordAxes >= 0)
    //  assert ncoordAxes == countCoordAxes : "CoordAxes " + ncoordAxes + " != " + countCoordAxes;
    if (nVertCooordAxes >= 0)
      assert nVertCooordAxes == countVertCooordAxes : "VertAxes " + nVertCooordAxes + " != " + countVertCooordAxes;

    gridDs.close();
  }


  // test readding as a stream. theres no file location to be reletive to
  public void utestReadNcMLInputStream() throws Exception {
    String ncmlLoc = "file:G:/work/jasmin/snpp.ncml";
    GridDataset fullDataset = GridDataset.open( ncmlLoc);
    System.out.printf("full size= %d%n", fullDataset.getGrids().size());

    // read ncml through a InputStream.
    String ncml =
       "<netcdf xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">\n" +
        "<aggregation type=\"union\">\n" +
        "    <netcdf location=\"file:G:/work/jasmin/SVM01_npp_d20120120_t0531354_e0532596_b01189_c20120120120033631216_noaa_ops.h5\" />\n" +
        "    <netcdf location=\"file:G:/work/jasmin/GMTCO_npp_d20120120_t0531354_e0532596_b01189_c20120120115420527613_noaa_ops.h5\" />\n" +
        "  </aggregation>\n" +
        "</netcdf>";
    NetcdfDataset aggregatedDataset = NcMLReader.readNcML( new ByteArrayInputStream(ncml.getBytes(CDM.utf8Charset)), null );
    GridDataset emptyDataset = new GridDataset( aggregatedDataset );
    System.out.printf("empty= %s%n", emptyDataset.getGrids().size());

    assert emptyDataset.getGrids().size() == fullDataset.getGrids().size();
  }


}
