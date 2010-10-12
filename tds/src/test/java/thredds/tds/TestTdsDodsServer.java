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
package thredds.tds;

import junit.framework.*;

import thredds.catalog.*;
import ucar.nc2.TestAll;
import ucar.nc2.dods.TestLocalDodsServer;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.dataset.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.util.IO;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.ma2.Array;
import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestTdsDodsServer extends TestCase {

  public TestTdsDodsServer( String name) {
    super(name);
  }

  String dataset = "http://localhost:8080/thredds/dodsC/testCdmUnitTest/grib/nam/c20s/NAM_CONUS_20km_surface_20060317_0000.grib1";
  public void testGrid() {
    String grid = dataset + ".ascii?Visibility[0:1:0][0:1:0][0:1:0]";
    System.out.println(" request= "+grid);
    String result = IO.readURLcontents(grid);
    System.out.println(" result= "+result);
    assert result.indexOf("Error") < 0;  // not an error message
  }

  public void testGridArray() {
    String array = dataset + ".asc?Visibility.Visibility[0:1:0][0:1:0][0:1:0]";
    System.out.println(" request= "+array);
    String result = IO.readURLcontents(array);
    System.out.println(" result= "+result);
    assert result.indexOf("Error") < 0;  // not an error message
  }

  public void testSingleDataset() throws IOException {
    InvCatalogImpl cat = TestTdsLocal.open(null);

    InvDataset ds = cat.findDatasetByID("testDataset");
    assert (ds != null) : "cant find dataset 'testDataset'";
    assert ds.getDataType() == FeatureType.GRID;

    ThreddsDataFactory fac = new ThreddsDataFactory();

    ThreddsDataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

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
    System.out.println("Open and read "+urlString);

    NetcdfFile ncd = NetcdfDataset.openFile(urlString, null);
    assert ncd != null;

    // pick a random variable to read
    List vlist = ncd.getVariables();
    int n = vlist.size();
    assert n > 0;
    Variable v = (Variable) vlist.get(n/2);
    Array data = v.read();
    assert data.getSize() == v.getSize();

    ncd.close();
  }

  public void testUrlReading() throws IOException {
    doOne("http://localhost:8080/thredds/dodsC/testCdmUnitTest/normal/NAM_Alaska_22km_20100504_0000.grib1");
    doOne("http://localhost:8080/thredds/dodsC/testCdmUnitTest/normal/NAM_Alaska_45km_conduit_20100913_0000.grib2");
  }

  public void testCompareWithFile() throws IOException {
    final String urlPrefix = "dods://localhost:8080/thredds/dodsC/opendapTest/";
    final String dirName = TestAll.cdmUnitTestDir + "tds/opendap/";  // read all files from this dir

    TestAll.actOnAll(dirName, null, new TestAll.Act() {
      public int doAct(String filename) throws IOException {
        filename = StringUtil.replace(filename, '\\', "/");
        filename = StringUtil.remove(filename, dirName);
        String dodsUrl = urlPrefix+filename;
        String localPath = dirName+filename;
        System.out.println("--Compare "+localPath+" to "+dodsUrl);

        NetcdfDataset org_ncfile = NetcdfDataset.openDataset(localPath);
        NetcdfDataset dods_file = NetcdfDataset.openDataset(dodsUrl);
        CompareNetcdf.compareFiles(org_ncfile, dods_file);
        return 1;
      }
    });
  }


}