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
package thredds.server.opendap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.DataFactory;
import thredds.server.catalog.TdsLocalCatalog;
import thredds.util.ContentType;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

@Category(NeedsCdmUnitTest.class)
public class TestTdsDodsServer {

  @Test
  public void checkBadRequest() {
    String endpoint = TestWithLocalServer.withPath("/dodsC/scanCdmUnitTests/tds/ncep/NAM_CONUS_20km_selectsurface_20100913_0000.grib2.badascii?Visibility_surface[0:1:0][0:1:0][0:1:0]");
    byte[] result = TestWithLocalServer.getContent(endpoint, 400, null);
  }

  @Test
  public void testGridArrayAscii() {
    String endpoint = TestWithLocalServer.withPath("/dodsC/scanCdmUnitTests/tds/ncep/NAM_CONUS_20km_selectsurface_20100913_0000.grib2.ascii?Visibility_surface[0:1:0][0:1:0][0:1:0]");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, null);
    Assert.assertNotNull(result);
    String results = new String(result, CDM.utf8Charset);
    assert results.contains("scanCdmUnitTests/tds/ncep/NAM_CONUS_20km_selectsurface_20100913_0000.grib2");
    assert results.contains("15636.879");
  }

  @Test
  public void testUrlReading() throws IOException {
    doOne(TestWithLocalServer.withPath("dodsC/scanCdmUnitTests/tds/ncep/NAM_Alaska_22km_20100504_0000.grib1"));
    doOne(TestWithLocalServer.withPath("dodsC/scanCdmUnitTests/tds/ncep/NAM_Alaska_45km_conduit_20100913_0000.grib2"));
  }

  /*
  Dataset {
      Grid {
       ARRAY:
          Float32 Visibility_surface[time = 1][y = 1][x = 1];
       MAPS:
          Int32 time[time = 1];
          Float32 y[y = 1];
          Float32 x[x = 1];
      } Visibility_surface;
  } testTdsScan/ncep/NAM_CONUS_20km_selectsurface_20100913_0000.grib2;
  ---------------------------------------------
  Visibility_surface.Visibility_surface[1][1][1]
  [0][0], 15636.879

  Visibility_surface.time[1]
  0

  Visibility_surface.y[1]
  -832.6978

  Visibility_surface.x[1]
  -4226.1084
   */

  @Test
  public void testSingleDataset() throws IOException {
    Catalog cat = TdsLocalCatalog.open(null);

    Dataset ds = cat.findDatasetByID("testDataset2");
    assert (ds != null) : "cant find dataset 'testDataset'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {
      Assert.assertTrue(dataResult.errLog.toString(), !dataResult.fatalError);
      Assert.assertNotNull(dataResult.featureDataset);
      Assert.assertEquals( ucar.nc2.dt.grid.GridDataset.class, dataResult.featureDataset.getClass());

      ucar.nc2.dt.grid.GridDataset gds = ( ucar.nc2.dt.grid.GridDataset) dataResult.featureDataset;
      String gridName = "Z_sfc";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      GeoGrid grid = gds.findGridByShortName(gridName);
      Assert.assertNotNull(gridName, grid);

      GridCoordSystem gcs = grid.getCoordinateSystem();
      Assert.assertNotNull(gcs);
      assert null == gcs.getVerticalAxis();

      CoordinateAxis time = gcs.getTimeAxis();
      Assert.assertNotNull("time axis", time);
      Assert.assertEquals(1, time.getSize());
      Array data = time.read();
      Assert.assertEquals(102840.0, data.getFloat(0), Misc.maxReletiveError);
    }
  }

  private void doOne(String urlString) throws IOException {
    System.out.printf("Open and read %s%n", urlString);

    NetcdfFile ncd = NetcdfDataset.openFile(urlString, null);
    assert ncd != null;

    // pick a random variable to read
    List vlist = ncd.getVariables();
    int n = vlist.size();
    assert n > 0;
    Variable v = (Variable) vlist.get(n / 2);
    System.out.printf("Read all data from %s%n", v.getName());
    Array data = v.read();
    assert data.getSize() == v.getSize();

    ncd.close();
  }

  @Test
  public void testCompareWithFile() throws IOException {
    final String urlPrefix = TestWithLocalServer.withPath("/dodsC/scanCdmUnitTests/tds/opendap/");
    final String dirName = TestDir.cdmUnitTestDir + "tds/opendap/";  // read all files from this dir

    TestDir.actOnAll(dirName, new TestDir.FileFilterNoWant(".gbx8 .gbx9 .ncx .ncx2 .ncx3"), new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        filename = StringUtil2.replace(filename, '\\', "/");
        filename = StringUtil2.remove(filename, dirName);
        String dodsUrl = urlPrefix + filename;
        String localPath = dirName + filename;
        System.out.println("--Compare " + localPath + " to " + dodsUrl);

        NetcdfDataset org_ncfile = NetcdfDataset.openDataset(localPath);
        NetcdfDataset dods_file = NetcdfDataset.openDataset(dodsUrl);

        Formatter f = new Formatter();
        CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, false);
        boolean ok = mind.compare(org_ncfile, dods_file, new TestDODScompareWithFiles.DodsObjFilter(), false, false, false);
        if (!ok) {
          System.out.printf("--Compare %s%n", filename);
          System.out.printf("  %s%n", f);
        }
        Assert.assertTrue( localPath+ " != "+dodsUrl, ok);

        return 1;
      }
    }, false);
  }


}
