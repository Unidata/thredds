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
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.*;
import ucar.nc2.units.DateRange;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.List;

@Category(NeedsCdmUnitTest.class)
public class TestTdsDatasetScan {
  @Test
  public void testSort() throws IOException {
    Catalog cat = TdsLocalCatalog.open("catalog/scanCdmUnitTests/tds/ncep/catalog.xml");

    Dataset last = null;
    for (Dataset ds : cat.getDatasetsLocal()) {
      if (last != null)
        assert ds.getName().compareTo(last.getName()) > 0;
      last = ds;
    }
  }

  @Test
  public void testDatasetScanForLatest() throws IOException {
    Catalog parent = TdsLocalCatalog.open("catalog/testGridScan/catalog.xml");
    Service latestService = parent.findService("Resolver");
    Assert.assertNotNull(latestService);

    List<Dataset> topDatasets = parent.getDatasetsLocal();
    Assert.assertEquals(1, topDatasets.size());
    Dataset topDataset = topDatasets.get(0);

    List<Dataset> dss = topDataset.getDatasetsLocal();
    Assert.assertTrue(dss.size() > 0);

    Dataset latestDataset = topDataset.findDatasetByName("latest.xml");
    Assert.assertNotNull(latestDataset);
    Access latestAccess = latestDataset.getAccess(ServiceType.Resolver);
    Assert.assertNotNull(latestAccess);
    Assert.assertEquals(latestService, latestAccess.getService());
  }

    @Test
   public void testLatestResolver() throws IOException {
    Catalog cat = TdsLocalCatalog.open("catalog/testGridScan/latest.xml");

    List<Dataset> dss = cat.getDatasetsLocal();
    assert (dss.size() == 1);

    Dataset ds = dss.get(0);
    assert ds.hasAccess();
    assert ds.getDatasetsLocal().size() == 0;

    assert ds.getID() != null;
    assert ds.getDataSize() > 0.0;
    assert ds.getId().endsWith("GFS_CONUS_80km_20120229_1200.grib1");;
  }

  @Test
  public void testHarvest() throws IOException {
    Catalog cat = TdsLocalCatalog.open("catalog/testEnhanced/catalog.xml");
    assert cat != null;
    List<Dataset> topList = cat.getDatasetsLocal();
    assert topList.size() == 1;
    Dataset top = topList.get(0);
    assert top != null;
    assert top.isHarvest();

    List<Dataset> dss = top.getDatasetsLocal();
    assert (dss.size() > 0);
    Dataset nested = dss.get(0);
    assert !nested.isHarvest();

    cat = TdsLocalCatalog.open("/catalog.xml");
    Dataset ds = cat.findDatasetByID("testDataset");
    assert ds != null;
    assert !ds.isHarvest();
  }

  @Test
  public void testNestedDirs() throws IOException {
    Catalog cat = TdsLocalCatalog.open("catalog/station/profiler/wind/06min/catalog.xml");

    List<Dataset> topList = cat.getDatasetsLocal();
    assert topList.size() == 1;
    Dataset top = topList.get(0);
    assert top != null;
    List<Dataset> children = top.getDatasetsLocal();
    Assert.assertEquals(3, children.size()); // latest + 2
  }

  /*
  see http://www.freeformatter.com/url-encoder.html

  Current State in 4,6:
  1) no encoding in the XML:

  <dataset name="encoding" ID="scanCdmUnitTests/encoding">
    <catalogRef xlink:href="d2.nc%3Bchunk%3D0/catalog.xml" xlink:title="d2.nc%3Bchunk%3D0" ID="scanCdmUnitTests/encoding/d2.nc%3Bchunk%3D0" name=""/>
    <catalogRef xlink:href="d2.nc;chunk=0/catalog.xml" xlink:title="d2.nc;chunk=0" ID="scanCdmUnitTests/encoding/d2.nc;chunk=0" name=""/>
    <catalogRef xlink:href="dir mit blank/catalog.xml" xlink:title="dir mit blank" ID="scanCdmUnitTests/encoding/dir mit blank" name=""/>
  </dataset>

  2) no url encoding in the HTML:

    <a href='d2.nc%3Bchunk%3D0/catalog.html'><tt>d2.nc%3Bchunk%3D0/</tt></a></td>
    <a href='d2.nc;chunk=0/catalog.html'><tt>d2.nc;chunk=0/</tt></a></td>
    <a href='dir mit blank/catalog.xml'><tt>dir mit blank/</tt></a></td>

  3) drill further in
   3.1)  encoding/d2.nc%3Bchunk%3D0/catalog.xml gets returned and unencoded to encoding/d2.nc;chunk=0/20070301.nc"
         http://localhost:8081/thredds/dodsC/scanCdmUnitTests/encoding/d2.nc;chunk=0/20070301.nc.html fails (wrong directory)

   3.2) http://localhost:8081/thredds/catalog/scanCdmUnitTests/encoding/d2.nc;chunk=0/catalog.xml does not get urlencoded by browser
        HEAD /thredds/catalog/scanCdmUnitTests/encoding/d2.nc;chunk=0/catalog.html
        fails with 404

   3.3) dir mit blank/catalog.xml gets URLencoded by browser to dir%20mit%20blank/catalog.xml
        all seems to work ok (with exception of the containing catalog)
        notice that "dir mit blank/catalog.xml" ends in xml (!) : getting an exception in HtmlWriter

   */

  @Test
  public void testEncodingWithBlanks() throws IOException {
    Catalog cat = TdsLocalCatalog.open("catalog/scanCdmUnitTests/encoding/catalog.xml");

    List<Dataset> ds = cat.getDatasetsLocal();
    assert ds.size() == 1;
    Dataset top = ds.get(0);

    List<Dataset> children = top.getDatasetsLocal();
    assert children.size() == 3 : children.size();
  }

  //////////////////////////////////////////////////////
  // catalog5


  @Test
  public void testGlobalServices() throws IOException {
    String catalog = "/catalog/testStationScan.v5/catalog.xml"; // serviceName ="all" from root catalog
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset top = cat.getDatasetsLocal().get(0);
    Assert.assertTrue(!top.hasAccess());
    Service orgServices = cat.findService("all");
    Assert.assertNotNull(orgServices);
    Assert.assertEquals(ServiceType.Compound, orgServices.getType());
    Assert.assertNotNull(orgServices.getNestedServices());
    Assert.assertEquals(11, orgServices.getNestedServices().size());  // has 11 services
    boolean hasFileServer = false;
    for (Service sn : orgServices.getNestedServices())
      if( ServiceType.HTTPServer == sn.getType()) hasFileServer = true;
    Assert.assertTrue(hasFileServer);
  }

}
