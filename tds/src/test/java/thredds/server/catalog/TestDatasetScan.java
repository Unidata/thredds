/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.catalog;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import thredds.client.catalog.writer.CatalogXmlWriter;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Describe
 *
 * @author caron
 * @since 1/21/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestDatasetScan {
  static private final boolean showCats = true;

  @Before
  public void setup() {
    ConfigCatalog.addAlias("${cdmUnitTest}", TestDir.cdmUnitTestDir);
  }

  @Test
  public void testMakeCatalog() throws IOException {
    String filePath = "../tds/src/test/content/thredds/catalog.xml";
    ConfigCatalog cat = TestServerCatalogs.open("file:" + filePath);
    CatalogXmlWriter writer = new CatalogXmlWriter();
    // System.out.printf("%s%n",  writer.writeXML( cat ));

    List<DatasetRootConfig> roots = cat.getDatasetRoots();
    for (DatasetRootConfig root : roots)
      System.out.printf("DatasetRoot %s -> %s%n", root.path, root.location);
    assert roots.size() == 3;

    Dataset ds = cat.findDatasetByID("scanCdmUnitTests");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("all");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("scanCdmUnitTests", cat.getBaseURI());
    assert scanCat != null;
    System.out.printf("%n%s%n",  writer.writeXML( scanCat ));

    scanCat = dss.makeCatalogForDirectory("scanCdmUnitTests/ncss/test", cat.getBaseURI());
    System.out.printf("%s%n",  writer.writeXML( scanCat ));
  }

  @Test
  public void testReverseSort() throws IOException {
    ConfigCatalog cat = TestServerCatalogs.getFromResource("thredds/server/catalog/TestDatasetScan.xml");

    Dataset ds = cat.findDatasetByID("NWS/NPN/6min");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("all");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("station/profiler/wind/06min", cat.getBaseURI());
    assert scanCat != null;

    CatalogXmlWriter writer = new CatalogXmlWriter();
    if (showCats) System.out.printf("%n%s%n", writer.writeXML(scanCat));
    assertEquals(1, scanCat.getDatasets().size());
    Dataset root = scanCat.getDatasets().get(0);
    assertEquals(3, root.getDatasets().size());

    // directories get reverse sorted
    List<Dataset> list = root.getDatasets();
    String name0 = list.get(0).getName();
    String name1 = list.get(1).getName();
    assert name0.compareTo(name1) > 0;

    scanCat = dss.makeCatalogForDirectory("station/profiler/wind/06min/20131102", cat.getBaseURI());
    assert scanCat != null;
    if (showCats) System.out.printf("%n%s%n", writer.writeXML(scanCat));

    assert scanCat.getDatasets().size() == 1;
    root = scanCat.getDatasets().get(0);
    assertEquals(3, root.getDatasets().size());

    // files get reverse sorted
    list = root.getDatasets();
    name0 = list.get(0).getName();
    name1 = list.get(1).getName();
    assert name0.compareTo(name1) > 0;
  }

  @Test
  public void testTimeCoverage() throws IOException, ParseException {
    ConfigCatalog cat = TestServerCatalogs.getFromResource("thredds/server/catalog/TestDatasetScan.xml");

    Dataset ds = cat.findDatasetByID("NWS/NPN/6min");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("all");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("station/profiler/wind/06min/20131102", cat.getBaseURI());
    assert scanCat != null;

    CatalogXmlWriter writer = new CatalogXmlWriter();
    if (showCats) System.out.printf("%n%s%n", writer.writeXML(scanCat));
    assertEquals(1, scanCat.getDatasets().size());
    Dataset root = scanCat.getDatasets().get(0);
    assertEquals(3, root.getDatasets().size());

    List<Dataset> list = root.getDatasets();
    Dataset ds0 = list.get(1);  // first one is latest
    Dataset ds1 = list.get(2);

    DateRange dr0 = ds0.getTimeCoverage();
    assert dr0 != null;
    assert dr0.getStart().getCalendarDate().equals(CalendarDateFormatter.isoStringToCalendarDate(null, "2013-11-02T23:54:00"));
    assert dr0.getDuration().equals( new TimeDuration("1 hour"));

    DateRange dr1 = ds1.getTimeCoverage();
    assert dr1 != null;
    assert dr1.getStart().getCalendarDate().equals(CalendarDateFormatter.isoStringToCalendarDate(null, "2013-11-02T23:48:00"));
    assert dr1.getDuration().equals( new TimeDuration("1 hour"));
  }

  @Test
  public void testLatest() throws IOException, ParseException {
    ConfigCatalog cat = TestServerCatalogs.getFromResource("thredds/server/catalog/TestDatasetScan.xml");

    Dataset ds = cat.findDatasetByID("NWS/NPN/6min");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("all");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("station/profiler/wind/06min/20131102", cat.getBaseURI());
    assert scanCat != null;

    CatalogXmlWriter writer = new CatalogXmlWriter();
    if (showCats) System.out.printf("%n%s%n", writer.writeXML(scanCat));
    assert scanCat.getDatasets().size() == 1;
    Dataset root = scanCat.getDatasets().get(0);

    Service latestService = null;
    for (Service s : scanCat.getServices()) {
      if (s.getName().equals("latest")) latestService = s;
    }
    assert latestService != null;

    Dataset latestDataset = null;
    for (Dataset nds : root.getDatasets()) {
      Service s = nds.getServiceDefault();
      assert s != null;
      if (s.equals(latestService)) latestDataset = nds;
    }
    assert latestDataset != null;

  }

}
