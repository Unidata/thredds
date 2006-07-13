// $Id: TestInvDatasetScan.java 61 2006-07-12 21:36:00Z edavis $
package thredds.catalog;

import junit.framework.TestCase;
import thredds.cataloggen.TestCatalogGen;

import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A description
 * <p/>
 * User: edavis
 * Date: Dec 13, 2004
 * Time: 1:00:42 PM
 */
public class TestInvDatasetScan extends TestCase
{

  private boolean debugShowCatalogs = true;

  private String dsScanName = "Test Data";
  private String dsScanPath = "testData";

  //private String dsScanDir = "C:/Ethan/dev/devel/netcdf-java-2.2/test/data";
  private String dsScanDir = "../netcdf-java-2.2/test/data";
  private String dsScanFilter = ".*\\.nc$";

  private String serviceName = "ncdods";
  private String baseURL = "http://localhost:8080/thredds/docsC";

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


  public TestInvDatasetScan( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testEmptyServiceBase()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_emptyServiceBase_ResourceName;

    InvCatalogImpl configCat = null;
    configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", null );

    InvService myService = new InvService( serviceName, ServiceType.DODS.toString(),
                                           "", null, null );
    configCat.addService( myService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, dsScanFilter, false, "false", false, null, null, null );
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String reqUriString = baseURL + "/" + dsScanPath + "/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/catalog.xml" );

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testTopLevelCatalog()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_topLevelCat_ResourceName;

    InvCatalogImpl configCat = null;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false);
    }

    InvService myService = new InvService( serviceName, ServiceType.DODS.toString(),
                                           baseURL, null, null );
    configCat.addService( myService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, dsScanFilter, false, "false", false, null, null, null );
    // me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me);

    configCat.finish();

    URI reqURI = null;
    String reqUriString = baseURL + "/" + dsScanPath + "/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/catalog.xml" );

    if (debugShowCatalogs)
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName);
  }

  public void testSecondLevelCatalog()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_secondLevelCat_ResourceName;

    InvCatalogImpl configCat = null;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL ) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false );
    }

    InvService myService = new InvService( serviceName, ServiceType.DODS.toString(),
                                           baseURL, null, null );
    configCat.addService( myService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir , dsScanFilter, false, "false", false, null, null, null );
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String reqUriString = baseURL + "/" + dsScanPath + "/trajectory/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/trajectory/catalog.xml" );

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testAddTimeCoverage()
  {
    String dsScanName = "Test Data";
    String dsScanPath = "testData";

    String dsScanDir = "test/data/thredds/cataloggen/testData/model";
    String dsScanFilter = ".*\\.nc$";

    String serviceName = "ncdods";
    String baseURL = "http://localhost:8080/thredds/docsC";
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_timeCoverage_ResourceName;

    String matchPattern = "([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9][0-9])";
    String substitutionPattern = "$1-$2-$3T$4:00:00";
    String duration = "60 hours";

    InvCatalogImpl configCat = null;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL ) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false );
    }

    InvService myService = new InvService( serviceName, ServiceType.DODS.toString(),
                                           baseURL, null, null );
    configCat.addService( myService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, dsScanFilter, false, "false", false, matchPattern, substitutionPattern, duration );
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String reqUriString = baseURL + "/" + dsScanPath + "/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/catalog.xml" );

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testAddIdLowerLevel()
  {
    String dsScanName = "Test Data";
    String dsScanPath = "testData";

    String dsScanDir = "test/data/thredds/cataloggen/testData/modelNotFlat";
    String dsScanFilter = ".*\\.nc$";

    String serviceName = "ncdods";
    String baseURL = "http://localhost:8080/thredds/docsC";
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_addIdLowerLevel_ResourceName;

    String topLevelId = "my/data/models";

    InvCatalogImpl configCat = null;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL ) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false );
    }

    InvService myService = new InvService( serviceName, ServiceType.DODS.toString(),
                                           baseURL, null, null );
    configCat.addService( myService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, dsScanFilter, false, "false", false, null, null, null );
    me.setID( topLevelId);
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String s = baseURL + "/" + dsScanPath + "/eta_211/catalog.xml";
    try
    {
      reqURI = new URI( s );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + s + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/eta_211/catalog.xml" );

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testAddIdTopLevel()
  {
    String dsScanName = "Test Data";
    String dsScanPath = "testData";

    String dsScanDir = "test/data/thredds/cataloggen/testData/model";
    String dsScanFilter = ".*\\.nc$";

    String serviceName = "ncdods";
    String baseURL = "http://localhost:8080/thredds/docsC";
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_addIdTopLevel_ResourceName;

    String topLevelId = "my/data/models";

    InvCatalogImpl configCat = null;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL ) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false );
    }

    InvService myService = new InvService( serviceName, ServiceType.DODS.toString(),
                                           baseURL, null, null );
    configCat.addService( myService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, dsScanFilter, false, "false", false, null, null, null );
    me.setID( topLevelId );
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String s = baseURL + "/" + dsScanPath + "/catalog.xml";
    try
    {
      reqURI = new URI( s );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + s + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/catalog.xml" );

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testCompoundServiceLower()
  {
    String dsScanName = "Test Data";
    String dsScanPath = "testData";

    String dsScanDir = "test/data/thredds/cataloggen/testData/modelNotFlat";
    String dsScanFilter = ".*\\.nc$";

    String serviceName = "both";
    String baseURL = "http://localhost:8080/thredds";
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_compoundServiceLower_ResourceName;

    String topLevelId = "my/data/models";

    InvCatalogImpl configCat = null;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL ) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false );
    }

    InvService myService = new InvService( serviceName, ServiceType.COMPOUND.toString(), "", null, null );
    myService.addService( new InvService( "tdsDods", ServiceType.DODS.toString(), "/thredds/dodsC", null, null) );
    myService.addService( new InvService( "tdsHttp", ServiceType.HTTPServer.toString(), "/thredds/fileServer", null, null) );

    configCat.addService( myService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, dsScanFilter, false, "false", false, null, null, null );
    me.setID( topLevelId );
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String reqUriString = baseURL + "/" + dsScanPath + "/eta_211/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/eta_211/catalog.xml" );

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testAddDatasetSize()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_addDatasetSize_ResourceName;

    InvCatalogImpl configCat = null;
    configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", null );

    InvService myService = new InvService( serviceName, ServiceType.DODS.toString(),
                                           "", null, null );
    configCat.addService( myService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, ".*\\.OIS$", true, "false", false, null, null, null );
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String reqUriString = baseURL + "/" + dsScanPath + "/dmsp/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/dmsp/catalog.xml" );

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testAddLatest()
  {
    String dsScanName = "Test Data";
    String dsScanPath = "testData";

    String dsScanDir = "test/data/thredds/cataloggen/testData/modelNotFlat";
    String dsScanFilter = ".*\\.nc$";

    String baseURL = "http://localhost:8080/thredds/docsC";
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_addLatest_ResourceName;

    String topLevelId = "my/data/models";

    InvCatalogImpl configCat = null;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL ) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false );
    }

    InvService myService;
    myService = new InvService( "myserver", ServiceType.COMPOUND.toString(), "", null, null );
    myService.addService( new InvService( "tdsDods", ServiceType.DODS.toString(), "/thredds/dodsC", null, null ) );
    myService.addService( new InvService( "tdsHttp", ServiceType.HTTPServer.toString(), "/thredds/fileServer", null, null ) );
    configCat.addService( myService );

    InvService latestService = new InvService( "latest", ServiceType.RESOLVER.toString(), "", null, null );
    configCat.addService( latestService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, dsScanFilter, false, "true", false, null, null, null );
    me.setID( topLevelId );
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String s = baseURL + "/" + dsScanPath + "/eta_211/catalog.xml";
    try
    {
      reqURI = new URI( s );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + s + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/eta_211/catalog.xml" );

    // Print catalog to std out.
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testAddLatestServiceBaseNotEmpty()
  {
    String dsScanName = "Test Data";
    String dsScanPath = "testData";

    String dsScanDir = "test/data/thredds/cataloggen/testData/modelNotFlat";
    String dsScanFilter = ".*\\.nc$";

    String baseURL = "http://localhost:8080/thredds/docsC";
    String expectedCatalogResourceName = configResourcePath + "/" + testInvDsScan_addLatest_ResourceName;

    String topLevelId = "my/data/models";

    InvCatalogImpl configCat = null;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL ) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false );
    }

    InvService myService;
    myService = new InvService( "myserver", ServiceType.COMPOUND.toString(), "", null, null );
    myService.addService( new InvService( "tdsDods", ServiceType.DODS.toString(), "/thredds/dodsC", null, null ) );
    myService.addService( new InvService( "tdsHttp", ServiceType.HTTPServer.toString(), "/thredds/fileServer", null, null ) );
    configCat.addService( myService );

    InvService latestService = new InvService( "latest", ServiceType.RESOLVER.toString(), "/thredds/dqc/latest", null, null );
    configCat.addService( latestService );

    InvDatasetScan me = new InvDatasetScan( configCat, null, dsScanName, dsScanPath, dsScanDir, dsScanFilter, false, "true", false, null, null, null );
    me.setID( topLevelId );
    //me.setServiceName( serviceName );
    // Set the serviceName (inherited) in InvDatasetScan.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( me, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    me.setLocalMetadata( tm2 );

    configCat.addDataset( me );

    configCat.finish();

    URI reqURI = null;
    String s = baseURL + "/" + dsScanPath + "/eta_211/catalog.xml";
    try
    {
      reqURI = new URI( s );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + s + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog = me.makeCatalogForDirectory( reqURI, dsScanPath + "/eta_211/catalog.xml" );

    // Print catalog to std out.
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    System.out.println( "NEEDS WORK: don't have a real use case here - probably need a different ProxyDatasetHandler." );
    System.out.println( "            Possible use case: current DQC Latest server URLs like \"/thredds/dqc/latest?eta_211\"." );

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testCompoundServerFilterProblem()
  {
    String expectedCatalog1ResourceName =
            configResourcePath + "/" + testInvDsScan_compoundServerFilterProblem_1_ResourceName;
    String expectedCatalog2ResourceName =
            configResourcePath + "/" + testInvDsScan_compoundServerFilterProblem_2_ResourceName;
    InvCatalogImpl configCat;
    try
    {
      configCat = new InvCatalogImpl( "Test Data Catalog for NetCDF-OPeNDAP Server", "1.0.1", new URI( baseURL ) );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + baseURL + ">: " + e.getMessage(),
                  false );
      return;
    }

    /*
    <service name="myserver" serviceType="Compound" base="">
      <service name="ncdods" serviceType="OpenDAP" base="/thredds/dodsC/"/>
      <service name="HTTPServer" serviceType="HTTPServer" base="/thredds/fileServer/"/>
      <service name="WCS" serviceType="WCS" base="/thredds/wcs/"/>
    </service>
    <!--service name="myserver" serviceType="OpenDAP" base=""/-->
    <service name="latest" serviceType="Resolver" base="" />
    */
    InvService myService;
    myService = new InvService( "myserver", ServiceType.COMPOUND.toString(), "", null, null );
    myService.addService( new InvService( "tdsDods", ServiceType.DODS.toString(), "/thredds/dodsC", null, null ) );
    myService.addService( new InvService( "tdsHttp", ServiceType.HTTPServer.toString(), "/thredds/fileServer", null, null ) );
    myService.addService( new InvService( "tdsWcs", ServiceType.WCS.toString(), "/thredds/wcs", null, null ) );
//    myService = new InvService( "myserver", ServiceType.OPENDAP.toString(), "", null, null );

    configCat.addService( myService );

    /*
    <datasetScan name="Data" path="testRelative" dirLocation="content/dodsC/data"
          ID="test/relative" addDatasetSize="true"
          filter="^test.*nc$">
      <metadata inherited="true">
        <serviceName>myserver</serviceName>
      </metadata>
    </datasetScan>
    */
    InvDatasetScan dsScan1 =
            new InvDatasetScan( configCat, null, "Data", "testRelative", "C:\\Program Files\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\dodsC\\data", "^test.*nc$",
                                true, "false", false,
                                null, null, null );
    dsScan1.setID( "test/relative" );
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( dsScan1, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    dsScan1.setLocalMetadata( tm2 );

    configCat.addDataset( dsScan1 );

    /*
    <datasetScan name="Eta" path="testRelativeEta" dirLocation="content/dodsC/data"
          ID="test/eta" addDatasetSize="true"
          filter="^2004.*nc$" addLatest="true" >
      <metadata inherited="true">
        <serviceName>myserver</serviceName>
      </metadata>
    </datasetScan>

    */
    InvDatasetScan dsScan2 =
            new InvDatasetScan( configCat, null, "Eta", "testRelativeEta", "C:\\Program Files\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\dodsC\\data", "^2004.*nc$",
                                true, "false", false,
                                null, null, null );
    dsScan2.setID( "test/eta" );
    tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    md = new InvMetadata( dsScan2, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    dsScan2.setLocalMetadata( tm2 );

    configCat.addDataset( dsScan2 );


    configCat.finish();

    // Generate catalogs
    URI reqURI = null;
    String reqUriString = baseURL + "/testRelative/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog1 = dsScan1.makeCatalogForDirectory( reqURI, "testRelative/catalog.xml" );
    URI reqURI2 = null;
    String reqUriString2 = baseURL + "/testRelativeEta/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString2 );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString2 + ">: " + e.getMessage(),
                  false );
    }
    InvCatalog catalog2 = dsScan2.makeCatalogForDirectory( reqURI2, "testRelativeEta/catalog.xml" );

    // Print catalog to std out.
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) catalog1 ) );
      System.out.println( "" );
      System.out.println( fac.writeXML( (InvCatalogImpl) catalog2 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog1, expectedCatalog1ResourceName );
    TestCatalogGen.compareCatalogToCatalogResource( catalog2, expectedCatalog2ResourceName );
  }

  /**
   * Testing subsetting on a selected dataset ID. Used by
   * InvDatasetScan.makeLatestCatalogForDirectory() to generate
   * the latest catalog.
   */
  public void testSubsetCatOnDs()
  {
    String filePath = "test/data/thredds/catalog";
    String inFileName = "testInvDsScan.subsetCatOnDs.input.xml";
    String resFileName = "testInvDsScan.subsetCatOnDs.result.xml";

    String targetDatasetID = "NCEP/GFS/Alaska_191km/GFS_Alaska_191km_20051011_1800.grib1";
    File inFile = new File( filePath, inFileName );

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    InvCatalogImpl cat = fac.readXML( inFile.toURI() );

    InvDataset targetDs = cat.findDatasetByID( targetDatasetID );
    cat.subset( targetDs );

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    TestCatalogGen.compareCatalogToCatalogResource( cat, configResourcePath + "/" + resFileName );
  }

  public void testDsScanPreRefactor()
  {
    String filePath = "test/data/thredds/catalog";
    String inFileName = "testInvDsScan.dsScanPreRefactor.input.xml";
    String res1Name = "/thredds/catalog/testInvDsScan.dsScanPreRefactor.result1.xml";
    String res2Name = "/thredds/catalog/testInvDsScan.dsScanPreRefactor.result2.xml";
    String res3Name = "/thredds/catalog/testInvDsScan.dsScanPreRefactor.result3.xml";

    File inFile = new File( filePath, inFileName );

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    InvCatalogImpl catalog = fac.readXML( inFile.toURI() );

    assertTrue( "Read resulted in null catalog.",
                catalog != null );

    String baseURI = catalog.getBaseURI().toString();
    String expectedBaseURI = inFile.toURI().toString();
    assertTrue( "Base URI <" + baseURI + "> not as expected <" + expectedBaseURI + ">.",
                baseURI.equals( expectedBaseURI ) );

    String targetDsId = "myGridDataID";
    InvDataset targetDs = catalog.findDatasetByID( targetDsId );

    assertTrue( "Did not find dataset <id=" + targetDsId + ">.",
                targetDs != null );
    assertTrue( "Found dataset <id=" + targetDsId + "> not an InvDatasetScan.",
                targetDs instanceof InvDatasetScan );

    InvDatasetScan scan = (InvDatasetScan) targetDs;

    URI reqURI = null;
    String reqUriString = baseURI + "/myGridData/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat1 = scan.makeCatalogForDirectory( reqURI, "myGridData/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/GFS/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat2 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/GFS/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/GFS/Alaska_191km/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat3 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/GFS/Alaska_191km/catalog.xml" );

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat1 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat2 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat3 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    TestCatalogGen.compareCatalogToCatalogResource( cat1, res1Name );
    TestCatalogGen.compareCatalogToCatalogResource( cat2, res2Name );
    TestCatalogGen.compareCatalogToCatalogResource( cat3, res3Name );
  }

  public void testDsScanRefactor()
  {
    String filePath = "test/data/thredds/catalog";
    String inFileName = "testInvDsScan.dsScanRefactor.input.xml";
    String res1Name = "/thredds/catalog/testInvDsScan.dsScanPreRefactor.result1.xml";
    String res2Name = "/thredds/catalog/testInvDsScan.dsScanPreRefactor.result2.xml";
    String res3Name = "/thredds/catalog/testInvDsScan.dsScanPreRefactor.result3.xml";

    File inFile = new File( filePath, inFileName );

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    InvCatalogImpl catalog = fac.readXML( inFile.toURI() );

    assertTrue( "Read resulted in null catalog.",
                catalog != null);

    String baseURI = catalog.getBaseURI().toString();
    String expectedBaseURI = inFile.toURI().toString();
    assertTrue( "Base URI <" + baseURI + "> not as expected <" + expectedBaseURI + ">.",
                baseURI.equals( expectedBaseURI ));

    String targetDsId = "myGridDataID";
    InvDataset targetDs = catalog.findDatasetByID( targetDsId );

    assertTrue( "Did not find dataset <id=" + targetDsId + ">.",
                targetDs != null );
    assertTrue( "Found dataset <id=" + targetDsId + "> not an InvDatasetScan.",
                targetDs instanceof InvDatasetScan );

    InvDatasetScan scan = (InvDatasetScan) targetDs;

    URI reqURI = null;
    String reqUriString = baseURI + "/myGridData/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat1 = scan.makeCatalogForDirectory( reqURI, "myGridData/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/GFS/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat2 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/GFS/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/GFS/Alaska_191km/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat3 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/GFS/Alaska_191km/catalog.xml" );

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat1 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat2 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat3 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    TestCatalogGen.compareCatalogToCatalogResource( cat1, res1Name );
    TestCatalogGen.compareCatalogToCatalogResource( cat2, res2Name );
    TestCatalogGen.compareCatalogToCatalogResource( cat3, res3Name );
  }

  public void testDsScanRefactorWithNamer()
  {
    String filePath = "test/data/thredds/catalog";
    String inFileName = "testInvDsScan.dsScanRefactorWithNamer.input.xml";
    String res1Name = "/thredds/catalog/testInvDsScan.dsScanRefactorWithNamer.result1.xml";
    String res2Name = "/thredds/catalog/testInvDsScan.dsScanRefactorWithNamer.result2.xml";
    String res3Name = "/thredds/catalog/testInvDsScan.dsScanRefactorWithNamer.result3.xml";

    File inFile = new File( filePath, inFileName );

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    InvCatalogImpl catalog = fac.readXML( inFile.toURI() );

    assertTrue( "Read resulted in null catalog.",
                catalog != null);

    String baseURI = catalog.getBaseURI().toString();
    String expectedBaseURI = inFile.toURI().toString();
    assertTrue( "Base URI <" + baseURI + "> not as expected <" + expectedBaseURI + ">.",
                baseURI.equals( expectedBaseURI ));

    String targetDsId = "myGridDataID";
    InvDataset targetDs = catalog.findDatasetByID( targetDsId );

    assertTrue( "Did not find dataset <id=" + targetDsId + ">.",
                targetDs != null );
    assertTrue( "Found dataset <id=" + targetDsId + "> not an InvDatasetScan.",
                targetDs instanceof InvDatasetScan );

    InvDatasetScan scan = (InvDatasetScan) targetDs;

    URI reqURI = null;
    String reqUriString = baseURI + "/myGridData/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat1 = scan.makeCatalogForDirectory( reqURI, "myGridData/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/GFS/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat2 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/GFS/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/GFS/Alaska_191km/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat3 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/GFS/Alaska_191km/catalog.xml" );

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat1 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat2 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat3 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    TestCatalogGen.compareCatalogToCatalogResource( cat1, res1Name );
    TestCatalogGen.compareCatalogToCatalogResource( cat2, res2Name );
    TestCatalogGen.compareCatalogToCatalogResource( cat3, res3Name );
  }

  public void testDsScanRefactorWithDirExclude()
  {
    String filePath = "test/data/thredds/catalog";
    String inFileName = "testInvDsScan.dsScanRefactorWithDirExclude.input.xml";
    String res1Name = "/thredds/catalog/testInvDsScan.dsScanPreRefactor.result1.xml";
    String res2Name = "/thredds/catalog/testInvDsScan.dsScanRefactorWithDirExclude.resultNCEP.xml";
    String res3Name = "/thredds/catalog/testInvDsScan.dsScanRefactorWithDirExclude.resultGFS.xml";
//    String res4Name = "/thredds/catalog/testInvDsScan.dsScanRefactorWithDirExclude.resultNAM.xml";

    File inFile = new File( filePath, inFileName );

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    InvCatalogImpl catalog = fac.readXML( inFile.toURI() );

    assertTrue( "Read resulted in null catalog.",
                catalog != null);

    String baseURI = catalog.getBaseURI().toString();
    String expectedBaseURI = inFile.toURI().toString();
    assertTrue( "Base URI <" + baseURI + "> not as expected <" + expectedBaseURI + ">.",
                baseURI.equals( expectedBaseURI ));

    String targetDsId = "myGridDataID";
    InvDataset targetDs = catalog.findDatasetByID( targetDsId );

    assertTrue( "Did not find dataset <id=" + targetDsId + ">.",
                targetDs != null );
    assertTrue( "Found dataset <id=" + targetDsId + "> not an InvDatasetScan.",
                targetDs instanceof InvDatasetScan );

    InvDatasetScan scan = (InvDatasetScan) targetDs;

    URI reqURI = null;
    String reqUriString = baseURI + "/myGridData/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat1 = scan.makeCatalogForDirectory( reqURI, "myGridData/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat2 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/GFS/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat3 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/GFS/catalog.xml" );

    reqUriString = baseURI + "/myGridData/NCEP/NAM/catalog.xml";
    try
    {
      reqURI = new URI( reqUriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Bad URI syntax <" + reqUriString + ">: " + e.getMessage(),
                  false );
    }
    InvCatalogImpl cat4 = scan.makeCatalogForDirectory( reqURI, "myGridData/NCEP/NAM/catalog.xml" );

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat1 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat2 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) cat3 ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    TestCatalogGen.compareCatalogToCatalogResource( cat1, res1Name );
    TestCatalogGen.compareCatalogToCatalogResource( cat2, res2Name );
    TestCatalogGen.compareCatalogToCatalogResource( cat3, res3Name );

    assertTrue( "Unexpected non-null NAM catalog, should be excluded by filter.",
                cat4 == null );
  }
}

/*
 * $Log: TestInvDatasetScan.java,v $
 * Revision 1.20  2006/05/19 19:23:06  edavis
 * Convert DatasetInserter to ProxyDatasetHandler and allow for a list of them (rather than one) in
 * CatalogBuilders and CollectionLevelScanner. Clean up division between use of url paths (req.getPathInfo())
 * and translated (CrawlableDataset) paths.
 *
 * Revision 1.19  2006/03/30 21:50:15  edavis
 * Minor fixes to get tests running.
 *
 * Revision 1.18  2006/01/26 18:20:46  edavis
 * Add CatalogRootHandler.findRequestedDataset() method (and supporting methods)
 * to check that the requested dataset is allowed, i.e., not filtered out.
 *
 * Revision 1.17  2006/01/10 23:21:15  edavis
 * Document changes to datasetScan. Also, make a few simplifying and clarifying changes to code and XSD that came up while documenting.
 *
 * Revision 1.16  2005/12/30 00:18:55  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.15  2005/12/16 23:19:38  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.14  2005/12/06 19:39:21  edavis
 * Last CatalogBuilder/CrawlableDataset changes before start using in InvDatasetScan.
 *
 * Revision 1.13  2005/11/18 23:51:05  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.12  2005/10/12 21:02:48  edavis
 * Add handling of inherited metadata when subsetting a catalog.
 *
 * Revision 1.11  2005/08/22 19:39:13  edavis
 * Changes to switch /thredds/dqcServlet URLs to /thredds/dqc.
 * Expand testing for server installations: TestServerSiteFirstInstall
 * and TestServerSite. Fix problem with compound services breaking
 * the filtering of datasets.
 *
 * Revision 1.10  2005/07/22 16:19:51  edavis
 * Allow DatasetSource and InvDatasetScan to add dataset size metadata.
 *
 * Revision 1.9  2005/07/21 17:19:18  edavis
 * Fix for InvDatasetScan and catalog relative services.
 *
 * Revision 1.8  2005/07/20 22:44:55  edavis
 * Allow InvDatasetScan to work with a service that is not catalog relative.
 * (DatasetSource can now add a prefix path name to resulting urlPaths.)
 *
 * Revision 1.7  2005/07/14 20:01:26  edavis
 * Make ID generation mandatory for datasetScan generated catalogs.
 * Also, remove log4j from some tests.
 *
 * Revision 1.6  2005/06/30 14:42:00  edavis
 * Change how LocalDatasetSource compares datasets to the accessPointHeader.
 * Using File.getPath() has problems with this (".") and parent ("..") directories.
 * Using File.getCanonicalPath() has problems if a symbolic link is in the dataset
 * path above the accessPointHeader path. So, do both if necessary.
 *
 * Revision 1.5  2005/06/28 18:36:30  edavis
 * Fixes to adding TimeCoverage and ID to datasets.
 *
 * Revision 1.4  2005/06/24 22:00:57  edavis
 * Write DatasetEnhancer1 to allow adding metadata to datasets.
 * Implement DatasetEnhancers for adding timeCoverage and for
 * adding ID to datasets. Also fix DatasetFilter so that 1) if
 * no filter is applicable for collection datasets, allow all
 * collection datasets and 2) if no filter is applicable for
 * atomic datasets, allow all atomic datasets.
 *
 * Revision 1.3  2005/06/08 21:20:15  edavis
 * Fixed naming of top dataset in InvDatasetScan produced catalogs
 * (removed "/" from end of name). Added to TestInvDatasetScan.
 *
 * Revision 1.2  2005/06/07 22:50:24  edavis
 * Fixed catalogRef links so relative to catalog instead of to service.
 * Fixed all tests in TestAllCatalogGen (including changing directory
 * filters because catalogRef names no longer contain slashes ("/").
 *
 * Revision 1.1  2005/06/06 18:25:53  edavis
 * Update DirectoryScanner to allow all directories even if name
 * doesn't send with "/".
 *
 * Revision 1.7  2005/06/03 19:12:42  edavis
 * Start adding wildcard handling in DirectoryScanner. Change
 * how DatasetSource names datasets and how catalogRefs are
 * constructed in DatasetSource.expand().
 *
 * Revision 1.6  2005/05/04 03:37:06  edavis
 * Remove several unnecessary methods in DirectoryScanner.
 *
 * Revision 1.5  2005/05/03 17:11:31  edavis
 * Fix TestDirectoryScanner.testWithDirPattern().
 *
 * Revision 1.4  2005/05/03 17:04:03  edavis
 * Add sort to datasetScan element and handle wildcard character in directory name.
 *
 * Revision 1.3  2005/04/29 14:55:57  edavis
 * Fixes for change in InvCatalogFactory.writeXML( cat, filename) method
 * signature. And start on allowing wildcard characters in pathname given
 * to DirectoryScanner.
 *
 * Revision 1.2  2005/04/27 23:05:41  edavis
 * Move sorting capabilities into new DatasetSorter class.
 * Fix a bunch of tests and such.
 *
 * Revision 1.1  2005/03/30 05:41:19  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.5  2005/02/01 22:55:16  edavis
 * Add dataset filtering to DirectoryScanner.
 *
 * Revision 1.4  2005/01/20 23:13:30  edavis
 * Extend DirectoryScanner to handle catalog generation for a list of top-level
 * data directories:
 * 1) add getMainCatalog(List):void to DirectoryScanner;
 * 2) add expand(List):void to DatasetSource, and
 * 3) two changes to the abstract methods in DatasetSource:
 *   a) add createDataset(String):InvDataset and
 *   b) rename getTopLevelDataset():InvDataset to
 *      createSkeletonCatalog():InvDataset.
 *
 * Revision 1.3  2005/01/14 18:02:24  edavis
 * Add createCatalogRef to DirectoryScanner constructor. Add testing.
 *
 * Revision 1.2  2004/12/15 17:51:03  edavis
 * Changes to clean up ResultService. Changes to add a server title to DirectoryScanner (becomes the title of the top-level dataset).
 *
 * Revision 1.1  2004/12/14 22:47:22  edavis
 * Add simple interface to thredds.cataloggen and continue adding catalogRef capabilities.
 *
 */