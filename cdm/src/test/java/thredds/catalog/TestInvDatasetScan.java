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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs);
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    System.out.println( "NEEDS WORK: don't have a real use case here - probably need a different ProxyDatasetHandler." );
    System.out.println( "            Possible use case: current DQC Latest server URLs like \"/thredds/dqc/latest?eta_211\"." );

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog1, expectedCatalog1ResourceName, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( catalog2, expectedCatalog2ResourceName, debugShowCatalogs );
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

    TestCatalogGen.compareCatalogToCatalogResource( cat, configResourcePath + "/" + resFileName, debugShowCatalogs );
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

    TestCatalogGen.compareCatalogToCatalogResource( cat1, res1Name, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( cat2, res2Name, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( cat3, res3Name, debugShowCatalogs );
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

    TestCatalogGen.compareCatalogToCatalogResource( cat1, res1Name, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( cat2, res2Name, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( cat3, res3Name, debugShowCatalogs );
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

    TestCatalogGen.compareCatalogToCatalogResource( cat1, res1Name, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( cat2, res2Name, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( cat3, res3Name, debugShowCatalogs );
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

    TestCatalogGen.compareCatalogToCatalogResource( cat1, res1Name, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( cat2, res2Name, debugShowCatalogs );
    TestCatalogGen.compareCatalogToCatalogResource( cat3, res3Name, debugShowCatalogs );

    assertTrue( "Unexpected non-null NAM catalog, should be excluded by filter.",
                cat4 == null );
  }
}
