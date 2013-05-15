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

import java.io.IOException;

import junit.framework.TestCase;
import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.ServiceType;
import thredds.catalog.crawl.CatalogCrawler;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.util.Misc;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.util.StringUtil2;

public class TestCdmRemoteServer extends TestCase {
  public TestCdmRemoteServer( String name) {
    super(name);
  }

  public void testCdmRemote() throws IOException {
    InvCatalogImpl cat = TestTdsLocal.open(null);

    InvDataset ds = cat.findDatasetByID("testClimatology");
    assert (ds != null) : "cant find dataset 'testClimatology'";
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

    GridDatatype grid = gds.findGridDatatype("sst");
    assert grid != null;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;
    assert null == gcs.getVerticalAxis();

    CoordinateAxis1D time = gcs.getTimeAxis1D();
    assert time != null;
    assert time.getSize() == 1;
    assert Misc.closeEnough(time.readScalarDouble(), 102840.0) : time.readScalarDouble();

    dataResult.featureDataset.close();
  }

  public void testUrlReading() throws IOException {
    InvCatalogImpl cat = TestTdsLocal.open("/catalog/testCdmremote/netcdf3/catalog.xml");
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
      public boolean getCatalogRef(InvCatalogRef cat, Object context) {
        System.out.format("***CatalogRef %s %n", cat.getCatalogUrl());
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

  private void doOne(InvDataset ds) throws IOException {
    InvAccess access = ds.getAccess(ServiceType.CdmRemote);
    if (access == null) {
      System.out.printf("No cdmremote access for %s%n", ds.getFullName());
      return;
    }

    ThreddsDataFactory fac = new ThreddsDataFactory();
    ThreddsDataFactory.Result dataResult = fac.openFeatureDataset( access, null);
    System.out.println("ThreddsDataFactory.Result= "+dataResult);
  }

  //////////////////////////////////////////////////

  public void testCompareWithFile() throws IOException {
    final String urlPrefix = CdmRemote.SCHEME+TestTdsLocal.top+"/cdmremote/opendapTest/";
    final String dirName = TestDir.cdmUnitTestDir + "tds/opendap/";  // read all files from this dir

    TestDir.actOnAll(dirName, new TestDir.FileFilterNoWant(".gbx8 .gbx9 .ncx"), new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        filename = StringUtil2.replace(filename, '\\', "/");
        filename = StringUtil2.remove(filename, dirName);
        String cdmrUrl = urlPrefix+filename;
        String localPath = dirName+filename;
        System.out.println("--Compare "+localPath+" to "+cdmrUrl);

        NetcdfDataset org_ncfile = NetcdfDataset.openDataset(localPath);
        NetcdfDataset dods_file = NetcdfDataset.openDataset(cdmrUrl);
        ucar.unidata.test.util.CompareNetcdf.compareFiles(org_ncfile, dods_file);
        return 1;
      }
    });
  }


}