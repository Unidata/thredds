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

import com.eclipsesource.restfuse.Destination;
import com.eclipsesource.restfuse.HttpJUnitRunner;
import com.eclipsesource.restfuse.Method;
import com.eclipsesource.restfuse.Response;
import com.eclipsesource.restfuse.annotation.Context;
import com.eclipsesource.restfuse.annotation.HttpTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import thredds.TestWithLocalServer;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.writer.DataFactory;
import thredds.server.catalog.TestTdsLocal;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.List;

import static com.eclipsesource.restfuse.Assert.assertBadRequest;
import static com.eclipsesource.restfuse.Assert.assertOk;

@RunWith(HttpJUnitRunner.class)
@Category(NeedsCdmUnitTest.class)
public class TestTdsDodsServer {
  private static URLEncoder encoder = new URLEncoder();

  @Rule
  public Destination destination = new Destination(TestWithLocalServer.server);

  @Context
  private Response response; // will be injected after every request


  @HttpTest(method = Method.GET, path = "/dodsC/scanCdmUnitTests/tds/ncep/NAM_CONUS_20km_selectsurface_20100913_0000.grib2.badascii?Visibility_surface[0:1:0][0:1:0][0:1:0]")
  public void checkBadRequest() {
    assertBadRequest(response);
  }

  @HttpTest(method = Method.GET, path = "/dodsC/scanCdmUnitTests/tds/ncep/NAM_CONUS_20km_selectsurface_20100913_0000.grib2.ascii?Visibility_surface[0:1:0][0:1:0][0:1:0]")
  public void testGridArrayAscii() {
    assertOk(response);
    String ress = response.getBody(String.class);
    assert ress.contains("scanCdmUnitTests/tds/ncep/NAM_CONUS_20km_selectsurface_20100913_0000.grib2");
    assert ress.contains("15636.879");
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

   private void testSingleDataset() throws IOException {
     Catalog cat = TestTdsLocal.open(null);

    Dataset ds = cat.findDatasetByID("testDataset2");
    assert (ds != null) : "cant find dataset 'testDataset'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();

    DataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

    assert dataResult != null;
    assert !dataResult.fatalError;
    assert dataResult.featureDataset != null;

    GridDataset gds = (GridDataset) dataResult.featureDataset;
    GridDatatype grid = gds.findGridDatatype("Z_sfc");
    assert grid != null;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;
    assert null == gcs.getVerticalAxis();

    CoordinateAxis1D time = gcs.getTimeAxis1D();
    assert time != null;
    assert time.getSize() == 1;
    assert 102840.0 == time.readScalarDouble();

    dataResult.featureDataset.close();
  }

  private void doOne(String urlString) throws IOException {
    System.out.printf("Open and read %s%n", urlString);

    NetcdfFile ncd = NetcdfDataset.openFile(urlString, null);
    assert ncd != null;

    // pick a random variable to read
    List vlist = ncd.getVariables();
    int n = vlist.size();
    assert n > 0;
    Variable v = (Variable) vlist.get(n/2);
    System.out.printf("Read all data from %s%n", v.getName());
    Array data = v.read();
    assert data.getSize() == v.getSize();

    ncd.close();
  }

  public void testCompareWithFile() throws IOException {
    final String urlPrefix = TestWithLocalServer.withPath("/dodsC/opendapTest/");
    final String dirName = TestDir.cdmUnitTestDir + "tds/opendap/";  // read all files from this dir

    TestDir.actOnAll(dirName, new TestDir.FileFilterNoWant(".gbx8 .gbx9 .ncx"), new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        filename = StringUtil2.replace(filename, '\\', "/");
        filename = StringUtil2.remove(filename, dirName);
        String dodsUrl = urlPrefix+filename;
        String localPath = dirName+filename;
        System.out.println("--Compare "+localPath+" to "+dodsUrl);

        NetcdfDataset org_ncfile = NetcdfDataset.openDataset(localPath);
        NetcdfDataset dods_file = NetcdfDataset.openDataset(dodsUrl);
        assert ucar.unidata.util.test.CompareNetcdf.compareFiles(org_ncfile, dods_file);
        return 1;
      }
    }, false);
  }


}
