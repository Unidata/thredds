/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import thredds.client.catalog.tools.CatalogXmlWriter;
import thredds.core.StandardService;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.AliasTranslator;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test DatasetScan
 *
 * @author caron
 * @since 1/21/2015
 * 
 * Excution notes:
 * If you plan to run this under Intellij IDE,
 * you will need to modify the 'Before Launch' window
 * in the Edit Configuration window and add the following
 * two gradle tasks in the thredds:tds project
 * 1. processResources
 * 2. processTestResources
 * For both of them, you will need to ensure that the following
 * VM arguments are defined.
 * 1. -Dunidata.testdata.path=...
 * 2. -Dtds.content.root.path=.../tds/src/test/content
 */

@Category(NeedsCdmUnitTest.class)
public class TestDatasetScan {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static private final boolean showCats = true;

  @Before
  public void setup() {
    AliasTranslator.addAlias("${cdmUnitTest}", TestDir.cdmUnitTestDir);
    StandardService ss = StandardService.resolver;
    Service latest = new Service(ss.getType().toString(), ss.getBase(), ss.getType().toString(), ss.getType().getDescription(),
            null, null, null, ss.getType().getAccessType());
    StandardService ss2 = StandardService.httpServer;
    Service httpServer = new Service(ss2.getType().toString(), ss2.getBase(), ss2.getType().toString(), ss2.getType().getDescription(),
            null, null, null, ss2.getType().getAccessType());

    DatasetScan.setSpecialServices(latest, httpServer);
  }

  @Test
  public void testMakeCatalog() throws IOException {
    String filePath = "../tds/src/test/content/thredds/catalog.xml";
    ConfigCatalog cat = TestConfigCatalogBuilder.open("file:" + filePath);
    CatalogXmlWriter writer = new CatalogXmlWriter();
    // System.out.printf("%s%n",  writer.writeXML( cat ));

    List<DatasetRootConfig> roots = cat.getDatasetRoots();
    for (DatasetRootConfig root : roots)
      System.out.printf("DatasetRoot %s -> %s%n", root.path, root.location);
    Assert.assertTrue("Incorrect # of catalog roots: expect 5 found "+roots.size(),roots.size() == 5);

    Dataset ds = cat.findDatasetByID("scanCdmUnitTests");
    Assert.assertTrue("Null dataset", ds != null);
    Assert.assertTrue("dataset not DatasetScan", ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    Assert.assertTrue("Servicename default is not 'all'",serviceName.equals("all"));
    Assert.assertTrue("has DatasetScan property", ds.hasProperty("DatasetScan"));

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("scanCdmUnitTests", cat.getBaseURI()).makeCatalog();
    assert scanCat != null;
    System.out.printf("%n%s%n",  writer.writeXML( scanCat ));

    scanCat = dss.makeCatalogForDirectory("scanCdmUnitTests/ncss/test", cat.getBaseURI()).makeCatalog();
    System.out.printf("%s%n",  writer.writeXML( scanCat ));
  }

  @Test
  public void testReverseSort() throws IOException {
    ConfigCatalog cat = TestConfigCatalogBuilder.getFromResource("thredds/server/catalog/TestDatasetScan.xml");

    Dataset ds = cat.findDatasetByID("NWS/NPN/6min");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("all");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("station/profiler/wind/06min", cat.getBaseURI()).makeCatalog();
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

    scanCat = dss.makeCatalogForDirectory("station/profiler/wind/06min/20131102", cat.getBaseURI()).makeCatalog();
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
    ConfigCatalog cat = TestConfigCatalogBuilder.getFromResource("thredds/server/catalog/TestDatasetScan.xml");

    Dataset ds = cat.findDatasetByID("NWS/NPN/6min");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("all");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("station/profiler/wind/06min/20131102", cat.getBaseURI()).makeCatalog();
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
    ConfigCatalog cat = TestConfigCatalogBuilder.getFromResource("thredds/server/catalog/TestDatasetScan.xml");

    Dataset ds = cat.findDatasetByID("NWS/NPN/6min");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("all");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("station/profiler/wind/06min/20131102", cat.getBaseURI()).makeCatalog();
    assert scanCat != null;

    CatalogXmlWriter writer = new CatalogXmlWriter();
    if (showCats) System.out.printf("%n%s%n", writer.writeXML(scanCat));
    assert scanCat.getDatasets().size() == 1;
    Dataset root = scanCat.getDatasets().get(0);

    Service latestService = null;
    for (Service s : scanCat.getServices()) {
      if (s.getName().equalsIgnoreCase("Resolver")) latestService = s;
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

  @Test
  public void testEsgfProblems() throws IOException, ParseException {
    String filePath = "../tds/src/test/content/thredds/testEsgfProblems.xml";
    ConfigCatalog cat = TestConfigCatalogBuilder.open("file:" + filePath);
    assert cat != null;

    Dataset ds = cat.findDatasetByID("gass-ytoc-mip");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("fileservice");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("gass-ytoc-mip", cat.getBaseURI()).makeCatalog();
    assert scanCat != null;

    CatalogXmlWriter writer = new CatalogXmlWriter();
    if (showCats) System.out.printf("%n%s%n", writer.writeXML(scanCat));
    assert scanCat.getDatasets().size() == 1;
    Dataset root = scanCat.getDatasets().get(0);
    String sn = root.getServiceNameDefault();
    assert sn != null;
    assert sn.equals("fileservice");

    for (Dataset nds : root.getDatasets()) {
      if (nds.getName().equals("latest.xml")) continue;
      Service s = nds.getServiceDefault();
      assert s != null;
      assert s.getName().equals("fileservice");
    }
  }


}
