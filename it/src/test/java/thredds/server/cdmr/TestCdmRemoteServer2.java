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
package thredds.server.cdmr;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.*;
import thredds.client.catalog.writer.CatalogCrawler;
import thredds.client.catalog.writer.DataFactory;
import thredds.server.catalog.TestTdsLocal;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

@Category(NeedsCdmUnitTest.class)
public class TestCdmRemoteServer2 {

  @Test
  public void testCdmRemote() throws IOException {
    Catalog cat = TestTdsLocal.open(null);

    Dataset ds = cat.findDatasetByID("testClimatology");
    assert (ds != null) : "cant find dataset 'testClimatology'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();

    DataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

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
    assert time.getSize() == 12 : time.getSize();

    double[] expect = new double[] {366.0, 1096.485, 1826.97, 2557.455, 3287.94, 4018.425, 4748.91, 5479.395, 6209.88, 6940.365, 7670.85, 8401.335};
    Array data = time.read();
    for (int i=0; i<expect.length; i++)
      assert Misc.closeEnough(expect[i], data.getDouble(i));

    dataResult.featureDataset.close();
  }

  @Test
  public void testUrlReading() throws IOException {
    Catalog cat = TestTdsLocal.open("catalog/scanCdmUnitTests/formats/netcdf3/catalog.xml");
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.Type.all_direct, 0, null, new CatalogCrawler.Listener() {

      @Override
      public void getDataset(Dataset dd, Object context) {
        try {
          doOne(dd);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      @Override
      public boolean getCatalogRef(CatalogRef cat, Object context) {
        System.out.format("***CatalogRef %s %n", cat.getCatalogUrl());
        return true;
      }
    });
    long start = System.currentTimeMillis();
    try {
      crawler.crawl(cat, null, null, null, new Indent(2));
    } finally {
      long took = (System.currentTimeMillis() - start);
      System.out.format("***Done " + cat + " took = " + took + " msecs%n");
    }
  }

  private void doOne(Dataset ds) throws IOException {
    Access access = ds.getAccess(ServiceType.CdmRemote);
    if (access == null) {
      System.out.printf("No cdmremote access for %s%n", ds.getName());
      return;
    }

    DataFactory fac = new DataFactory();
    DataFactory.Result dataResult = fac.openFeatureDataset( access, null);
    System.out.println("DataFactory.Result= "+dataResult);
  }

}
