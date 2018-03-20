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
import thredds.client.catalog.*;
import thredds.client.catalog.tools.CatalogXmlWriter;
import thredds.server.catalog.builder.ConfigCatalogBuilder;
import ucar.nc2.util.AliasTranslator;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 1/15/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestConfigCatalogBuilder {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Before
  public void setup() {
    AliasTranslator.addAlias("${cdmUnitTest}", TestDir.cdmUnitTestDir);
  }

  static public ConfigCatalog getFromResource(String catName) throws IOException {
    // get test catalog location
    ClassLoader cl = TestConfigCatalogBuilder.class.getClassLoader();
    URL url = cl.getResource(catName);
    if (url == null) return null;

    ConfigCatalogBuilder builder = new ConfigCatalogBuilder();
    Catalog cat = builder.buildFromLocation("file:" + url.getPath(), null);
    if (builder.hasFatalError()) {
      System.out.printf("%s%n", builder.getErrorMessage());
      assert false;
      return null;
    } else {
      String mess = builder.getErrorMessage();
      if (mess.length() > 0)
        System.out.printf(" parse Messages = %s%n", builder.getErrorMessage());
    }

    Assert.assertTrue(cat instanceof ConfigCatalog);
    return (ConfigCatalog) cat;
  }

  static public ConfigCatalog open(String urlString) throws IOException {
    System.out.printf("Open %s%n", urlString);
    ConfigCatalogBuilder builder = new ConfigCatalogBuilder();
    Catalog cat = builder.buildFromLocation(urlString, null);
    if (builder.hasFatalError()) {
      System.out.printf("%s%n", builder.getErrorMessage());
      assert false;
      return null;
    } else {
      String mess = builder.getErrorMessage();
      if (mess.length() > 0)
        System.out.printf(" parse Messages = %s%n", builder.getErrorMessage());
    }

    Assert.assertTrue(cat instanceof ConfigCatalog);
    return (ConfigCatalog) cat;
  }


    //private String dsScanDir = "src/test/data";
  private String dsScanDir = TestDir.cdmLocalTestDataDir;
  private String dsScanFilter = ".*\\.nc$";

  private String serviceName = "ncdods";
  private String baseURL = "http://localhost:8080/thredds/docsC";

  private File dsScanTmpDir;
  private File expectedResultsDir;

  private String configResourcePath = "/thredds/catalog";
  private String testInvDsScan_emptyServiceBase_ResourceName = "testInvDsScan.emptyServiceBase.result.xml";
  private String testInvDsScan_topLevelCat_ResourceName = "testInvDsScan.topLevelCat.result.xml";
  private String testInvDsScan_secondLevelCat_ResourceName = "testInvDsScan.secondLevelCat.result.xml";
  private String testInvDsScan_timeCoverage_ResourceName = "testInvDsScan.timeCoverage.result.xml";
  private String testInvDsScan_addIdTopLevel_ResourceName = "testInvDsScan.addIdTopLevel.result.xml";
  private String testInvDsScan_addIdLowerLevel_ResourceName = "testInvDsScan.addIdLowerLevel.result.xml";

  private String testInvDsScan_compoundServiceLower_ResourceName = "testInvDsScan.compoundServiceLower.result.xml";
  private String testInvDsScan_addDatasetSize_ResourceName = "testInvDsScan.addDatasetSize.result.xml";
  private String testInvDsScan_addLatest_ResourceName = "testInvDsScan.addLatest.result.xml";

  private String testInvDsScan_compoundServerFilterProblem_1_ResourceName = "testInvDsScan.compoundServerFilterProblem.1.result.xml";
  private String testInvDsScan_compoundServerFilterProblem_2_ResourceName = "testInvDsScan.compoundServerFilterProblem.2.result.xml";

  @Test
  public void testReadCatalog() throws IOException {
    String filePath = "../tds/src/test/content/thredds/catalog.xml";
    ConfigCatalog cat = open("file:"+filePath);
    assert cat != null;
    CatalogXmlWriter writer = new CatalogXmlWriter();
    System.out.printf("%s%n",  writer.writeXML( cat ));

    List<DatasetRootConfig> roots = cat.getDatasetRoots();
    for (DatasetRootConfig root : roots)
      System.out.printf("DatasetRoot %s -> %s%n", root.path, root.location);
    Assert.assertTrue("Incorrect # of catalog roots: expect 5 found "+roots.size(),roots.size() == 5);

    Dataset ds = cat.findDatasetByID("Hyrax2TDS");
    assert ds != null;
    Object ncml = ds.getLocalField(Dataset.Ncml);
    assert ncml != null;

    ds = cat.findDatasetByID("scanCdmUnitTests");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    String serviceName = ds.getServiceNameDefault();
    assert serviceName.equals("all");

    ds = cat.findDatasetByID("testGridScan");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    ncml = ds.getLocalField(Dataset.Ncml);
    assert ncml != null;
  }

  @Test
   public void testReadFc() throws IOException {
     String filePath = "../tds/src/test/content/thredds/catalogGrib.xml";
     ConfigCatalog cat = open("file:"+filePath);
     CatalogXmlWriter writer = new CatalogXmlWriter();
     String catalogAsString = writer.writeXML( cat );
     System.out.printf("%s%n",  catalogAsString);

     List<Service> ss = cat.getServices();
     for (Service s : ss)
       System.out.printf("Service %s%n", s);
     assert ss.size() == 2;
   }

  public static void compareCatalogToCatalogDocFile( ConfigCatalog expandedCatalog, File expectedCatalogDocFile)
          throws IOException {
    Assert.assertNotNull(expandedCatalog);
    Assert.assertNotNull(expectedCatalogDocFile);
    Assert.assertTrue("File doesn't exist [" + expectedCatalogDocFile.getPath() + "].", expectedCatalogDocFile.exists());
    Assert.assertTrue("File is a directory [" + expectedCatalogDocFile.getPath() + "].", expectedCatalogDocFile.isFile());
    Assert.assertTrue("Can't read file [" + expectedCatalogDocFile.getPath() + "].", expectedCatalogDocFile.canRead());

    // Read in expected result catalog.
    ConfigCatalog expectedCatalog = open("file:" + expectedCatalogDocFile.getPath());

    String expectedCatalogAsString;
    String catalogAsString;
    CatalogXmlWriter writer = new CatalogXmlWriter();
    try {
      expectedCatalogAsString = writer.writeXML( expectedCatalog );
      catalogAsString = writer.writeXML( expandedCatalog );
    } catch ( IOException e ) {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      return;
    }
    // Print expected and resulting catalogs to std out.
    if ( true ) {
      System.out.println( "Expected catalog (" + expectedCatalogDocFile.getPath() + "):" );
      System.out.println( "--------------------" );
      System.out.println( expectedCatalogAsString );
      System.out.println( "--------------------" );
      //System.out.println( "Resulting catalog (" + expandedCatalog.getUriString() + "):" );
      System.out.println( "--------------------" );
      System.out.println( catalogAsString );
      System.out.println( "--------------------\n" );
    }
    Assert.assertEquals(expectedCatalogAsString, catalogAsString);
    System.out.println( "Expected catalog as String equals resulting catalog as String");

    // Compare the two catalogs.
    //assertTrue( "Expanded catalog object does not equal expected catalog object.",
    //            ( (InvCatalogImpl) expandedCatalog ).equals( expectedCatalog ) );
  }

    /////////////////////////////////////
  @Test
  public void testMisspell() throws IOException {
        // get test catalog location
    ClassLoader cl = this.getClass().getClassLoader();
    URL url = cl.getResource("thredds/server/catalog/testInheritied.xml");
    assert (url != null);

    ConfigCatalogBuilder catFactory = new ConfigCatalogBuilder();
    Catalog cat = catFactory.buildFromLocation("file:" + url.getPath(), null);
    CatalogXmlWriter writer = new CatalogXmlWriter();
    System.out.printf("%s%n", writer.writeXML(cat));

    Dataset ds = cat.findDatasetByID("test");
    assert (ds != null) : "cant find dataset 'test'";
    List<ThreddsMetadata.MetadataOther> list = ds.getMetadataOther();
    assert list != null;
    assert list.size() == 0;

   }


}
