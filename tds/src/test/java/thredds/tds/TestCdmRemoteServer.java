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
import thredds.catalog.crawl.CatalogCrawler;
import ucar.nc2.TestAll;
import ucar.nc2.dods.TestLocalDodsServer;
import ucar.nc2.stream.CdmRemote;
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
import java.util.Formatter;
import java.util.List;

public class TestCdmRemoteServer extends TestCase {
  public TestCdmRemoteServer( String name) {
    super(name);
  }

  public void testSingleDataset() throws IOException {
    InvCatalogImpl cat = TestTdsLocal.open(null);

    InvDataset ds = cat.findDatasetByID("testDataset2");
    assert (ds != null) : "cant find dataset 'testDataset2'";
    assert ds.getDataType() == FeatureType.GRID;

    ThreddsDataFactory fac = new ThreddsDataFactory();

    ThreddsDataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

    assert dataResult != null;
    if (dataResult.fatalError) {
      System.out.printf("fatalError= %s%n", dataResult.errLog);
      assert false;
    }
    assert dataResult.featureDataset != null;

    GridDataset gds = (GridDataset) dataResult.featureDataset;
    NetcdfFile nc = gds.getNetcdfFile();
    if (nc != null)  
      System.out.printf(" NetcdfFile location = %s%n", nc.getLocation());

    GridDatatype grid = gds.findGridDatatype("Z_sfc");
    assert grid != null;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;
    assert null == gcs.getVerticalAxis();

    CoordinateAxis1D time = gcs.getTimeAxis1D();
    assert time != null;
    assert time.getSize() == 1;
    assert TestAll.closeEnough(time.readScalarDouble(), 102840.0) : time.readScalarDouble();

    dataResult.featureDataset.close();
  }

  private void doOne(InvDataset ds) throws IOException {
    InvAccess access = ds.getAccess(ServiceType.CdmRemote);
    if (access == null) {
      System.out.printf("No cdmremote access for %s%n", ds);
      return;
    }

    ThreddsDataFactory fac = new ThreddsDataFactory();
    ThreddsDataFactory.Result dataResult = fac.openFeatureDataset( access, null);
    System.out.println("ThreddsDataFactory.Result= "+dataResult);
  }

  public void utestUrlReading() throws IOException {
    InvCatalogImpl cat = TestTdsLocal.open(null);
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL_DIRECT, false, new CatalogCrawler.Listener() {

      @Override
      public void getDataset(InvDataset dd, Object context) {
        try {
          doOne(dd);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      @Override
      public boolean getCatalogRef(InvCatalogRef dd, Object context) {
        return true;
      }
    });
    long start = System.currentTimeMillis();
    try {
      crawler.crawl(cat, null, null, null);
    } finally {
      long took = (System.currentTimeMillis() - start);
      System.out.format("***Done " + cat + " took = " + took + " msecs\n");
    }
  }

  public void testCompareWithFile() throws IOException {
    final String urlPrefix = CdmRemote.SCHEME+"http://localhost:8080/thredds/cdmremote/opendapTest/";
    final String dirName = TestAll.cdmUnitTestDir + "tds/opendap/";  // read all files from this dir

    TestAll.actOnAll(dirName, new TestAll.FileFilterNoWant(".gbx8"), new TestAll.Act() {
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