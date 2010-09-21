package thredds.catalog.util;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import thredds.catalog.*;
import thredds.catalog2.xml.parser.CatalogXmlUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * _more_
 *
 * @author edavis
 * @since 4.2
 */
public class DeepCopyUtilsTest
{
  @Test
  public void checkCopyCatalog()
          throws URISyntaxException, IOException
  {
    String docBaseUriString = "http://test/thredds/catalog/util/DeepCopyUtilsTest/checkCopyCatalog.xml";
    URI docBaseUri = new URI( docBaseUriString );

    String catalogAsString = setupCatalogWithNestedDatasets();

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
    InvCatalogImpl catalog = fac.readXML( catalogAsString, docBaseUri );

    assertCatalogAsExpected( catalog, docBaseUri );

    String origCatAsString = fac.writeXML( catalog );

    InvCatalog resultCatalog = DeepCopyUtils.copyCatalog( catalog );

    assertCatalogAsExpected( resultCatalog, docBaseUri);

    String resultCatalogAsString = fac.writeXML( (InvCatalogImpl) resultCatalog );

    assertEquals( origCatAsString, resultCatalogAsString );

    assertEquals( catalog, resultCatalog);
  }

  @Test
  public void checkSubsetCatalogOnDataset()
          throws URISyntaxException, IOException
  {
    String docBaseUriString = "http://test/thredds/catalog/util/DeepCopyUtilsTest/checkSubsetCatalogOnDataset.xml";
    URI docBaseUri = new URI( docBaseUriString );

    String catalogAsString = setupCatalogWithNestedDatasets();

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
    InvCatalogImpl catalog = fac.readXML( catalogAsString, docBaseUri );

    InvCatalog resultCatalog = DeepCopyUtils.subsetCatalogOnDataset( catalog, "ds2" );

    // Check that catalog was not affected by subsetting.
    assertCatalogAsExpected( catalog, docBaseUri );

    assertSubsetCatalogOnDatasetAsExpected( resultCatalog, docBaseUri );
  }

  private void assertSubsetCatalogOnDatasetAsExpected( InvCatalog catalog, URI docBaseUri )
  {
    assertNotNull( "Catalog is null.", catalog );
    assertNotNull( "DocBase URI is null.", docBaseUri );

    assertEquals( docBaseUri.toString() + "/ds2", catalog.getUriString());
    assertEquals( "ds 2", catalog.getName() );

    List<InvService> services = catalog.getServices();
    InvService allService = assertCatalogServicesAsExpected( services );

    List<InvDataset> dsList = catalog.getDatasets();
    assertNotNull( dsList );
    assertEquals( 1, dsList.size() );
    InvDataset ds2 = dsList.get( 0 );
    assertDs2AsExpected( allService, ds2, true );
  }

  private void assertCatalogAsExpected( InvCatalog catalog, URI docBaseUri )
  {
    assertNotNull( "Catalog is null.", catalog );
    assertNotNull( "DocBase URI is null.", docBaseUri );

    assertEquals( catalog.getUriString(), docBaseUri.toString() );
    assertEquals( catalog.getName(), "Catalog 1" );

    List<InvService> services = catalog.getServices();
    InvService allService = assertCatalogServicesAsExpected( services );

    List<InvDataset> dsList = catalog.getDatasets();
    assertNotNull( dsList );
    assertEquals( 1, dsList.size() );
    InvDataset ds1 = dsList.get( 0 );

    assertDs1AsExpected( allService, ds1 );

  }

  private InvService assertCatalogServicesAsExpected( List<InvService> services )
  {
    assertNotNull( "Null set of services.", services );
    assertEquals( 1, services.size() );
    InvService allService = services.get( 0);
    assertEquals( "all", allService.getName() );
    assertEquals( 3, allService.getServices().size() );
    return allService;
  }

  private void assertDs1AsExpected( InvService allService, InvDataset ds1 )
  {
    List<InvDataset> dsList;
    assertEquals( "ds 1", ds1.getName() );
    assertEquals( "ds1", ds1.getID() );
    assertEquals( allService, ds1.getServiceDefault() );
    List<InvMetadata> mdList = ds1.getMetadata();
    assertNotNull( mdList);
    assertEquals( 0, mdList.size());
    assertThreddsMetadataIsEmpty( ((InvDatasetImpl) ds1).getLocalMetadata(), Collections.<String>emptyList());
    ThreddsMetadata tm = ((InvDatasetImpl) ds1).getLocalMetadataInheritable();
    List<String> skipList = new ArrayList<String>();
    skipList.add( "serviceName" );
    skipList.add( "documentation");
    assertThreddsMetadataIsEmpty( tm, skipList);
    assertEquals( "all", tm.getServiceName() );
    List<InvDocumentation> docs = tm.getDocumentation();
    assertNotNull( docs);
    assertEquals( 1, docs.size());
    assertEquals( "fun", docs.get( 0).getInlineContent());

    dsList = ds1.getDatasets();
    assertNotNull( dsList);
    assertEquals( 1, dsList.size());
    InvDataset ds2 = dsList.get( 0);
    assertDs2AsExpected( allService, ds2, false );
  }

  private void assertDs2AsExpected( InvService allService, InvDataset ds2, boolean afterSubset )
  {
    assertEquals( "ds 2", ds2.getName() );
    assertEquals( "ds2", ds2.getID() );
    assertEquals( allService, ds2.getServiceDefault() );
    List<InvMetadata> mdList = ds2.getMetadata();
    assertNotNull( mdList );
    assertEquals( 0, mdList.size() );

    List<InvDocumentation> docs = ds2.getDocumentation();
    assertNotNull( docs);
    assertEquals( 2, docs.size());
    assertEquals( "more fun", docs.get( 0 ).getInlineContent());
    assertEquals( "fun", docs.get( 1 ).getInlineContent());

    assertThreddsMetadataIsEmpty( ( (InvDatasetImpl) ds2 ).getLocalMetadata(), Collections.<String>emptyList() );
    ThreddsMetadata tm = ( (InvDatasetImpl) ds2 ).getLocalMetadataInheritable();
    List<String> skipList = new ArrayList<String>();
    if ( afterSubset ) {
      skipList.add( "serviceName");
      skipList.add( "documentation");
    } else {
      skipList.add( "documentation" );
    }

    assertThreddsMetadataIsEmpty( tm, skipList );
    docs = tm.getDocumentation();
    assertNotNull( docs );
    if ( afterSubset ) {
      assertEquals( 2, docs.size() );
      assertEquals( "more fun", docs.get( 0 ).getInlineContent() );
      assertEquals( "fun", docs.get( 1 ).getInlineContent() );
    } else {
      assertEquals( 1, docs.size() );
      assertEquals( "more fun", docs.get( 0 ).getInlineContent() );
    }

    List<InvDataset> dsList = ds2.getDatasets();
    assertNotNull( dsList);
    assertEquals( 2, dsList.size() );
    InvDataset test1 = dsList.get( 0 );
    assertTest1AsExpected( allService, test1 );

    InvDataset test2 = dsList.get( 1 );
    assertTest2AsExpected( allService, test2 );
  }

  private static void assertTest1AsExpected( InvService allService, InvDataset test1 )
  {
    assertEquals( "Test 1", test1.getName() );
    assertEquals( "Test1", test1.getID() );
    assertEquals( allService, test1.getServiceDefault() );
    assertEquals( "test/test1.nc", ( (InvDatasetImpl) test1 ).getUrlPath());
    List<InvAccess> access = test1.getAccess();
    assertNotNull( access );
    assertEquals( 3, access.size());
    assertEquals( allService.getServices().get( 0), access.get( 0).getService());
    assertEquals( "test/test1.nc", access.get( 0).getUrlPath());

    List<InvMetadata> mdList = test1.getMetadata();
    assertNotNull( mdList );
    assertEquals( 0, mdList.size() );

    List<InvDocumentation> docs = test1.getDocumentation();
    assertNotNull( docs );
    assertEquals( 3, docs.size() );
    assertEquals( "even more fun", docs.get( 0 ).getInlineContent() );
    assertEquals( "more fun", docs.get( 1 ).getInlineContent() );
    assertEquals( "fun", docs.get( 2 ).getInlineContent() );

    List<InvProperty> properties = test1.getProperties();
    assertNotNull( properties);
    assertEquals( 2, properties.size() );
    assertEquals( "viewer", properties.get( 0).getName());
    assertEquals( "a viewer", properties.get( 0).getValue());
    assertEquals( "viewer2", properties.get( 1).getName());
    assertEquals( "yet another viewer", properties.get( 1).getValue());

    ThreddsMetadata tm = ( (InvDatasetImpl) test1 ).getLocalMetadata();
    List<String> skipList = new ArrayList<String>();
    skipList.add( "properties" );
    skipList.add( "documentation" );
    assertThreddsMetadataIsEmpty( tm, skipList);
    docs = tm.getDocumentation();
    assertNotNull( docs );
    assertEquals( 1, docs.size() );
    assertEquals( "even more fun", docs.get( 0 ).getInlineContent() );
    properties = tm.getProperties();
    assertNotNull( properties );
    assertEquals( 3, properties.size() );
    assertEquals( "viewer", properties.get( 0 ).getName() );
    assertEquals( "a viewer", properties.get( 0 ).getValue() );
    assertEquals( "viewer", properties.get( 1 ).getName() );
    assertEquals( "another viewer", properties.get( 1 ).getValue() );
    assertEquals( "viewer2", properties.get( 2 ).getName() );
    assertEquals( "yet another viewer", properties.get( 2 ).getValue() );

    assertThreddsMetadataIsEmpty( ( (InvDatasetImpl) test1 ).getLocalMetadataInheritable(), Collections.<String>emptyList() );
  }
  private static void assertTest2AsExpected( InvService allService, InvDataset test2 )
  {
    assertEquals( "Test 2", test2.getName() );
    assertEquals( "Test2", test2.getID() );
    assertEquals( allService, test2.getServiceDefault() );
    assertEquals( "test/test2.nc", ( (InvDatasetImpl) test2 ).getUrlPath());
    List<InvAccess> access = test2.getAccess();
    assertNotNull( access );
    assertEquals( 3, access.size());
    assertEquals( allService.getServices().get( 0), access.get( 0).getService());
    assertEquals( "test/test2.nc", access.get( 0).getUrlPath());

    List<InvMetadata> mdList = test2.getMetadata();
    assertNotNull( mdList );
    assertEquals( 0, mdList.size() );

    List<InvDocumentation> docs = test2.getDocumentation();
    assertNotNull( docs );
    assertEquals( 2, docs.size() );
    assertEquals( "more fun", docs.get( 0 ).getInlineContent() );
    assertEquals( "fun", docs.get( 1 ).getInlineContent() );

    assertThreddsMetadataIsEmpty( ( (InvDatasetImpl) test2 ).getLocalMetadata(), Collections.<String>emptyList());
    assertThreddsMetadataIsEmpty( ( (InvDatasetImpl) test2 ).getLocalMetadataInheritable(), Collections.<String>emptyList() );
  }

  private static void assertThreddsMetadataIsEmpty( ThreddsMetadata tm, List<String> skipList )
  {
    assertNotNull( tm);
    assertNotNull( skipList);

    if ( ! skipList.contains( "serviceName" ))
      assertNull( tm.getServiceName());
    if ( ! skipList.contains( "authority" ))
      assertNull( tm.getAuthority());
    if ( ! skipList.contains( "dataType" ) )
      assertNull( tm.getDataType() );
    if ( ! skipList.contains( "dataFormatType" ) )
      assertNull( tm.getDataFormatType() );
    if ( ! skipList.contains( "dataSize" ) )
      assertTrue( tm.getDataSize() == 0.0 || Double.isNaN( tm.getDataSize()) );

    if ( ! skipList.contains( "documentation" ) )
      assertTrue( tm.getDocumentation().isEmpty() );
    if ( ! skipList.contains( "creators" ) )
      assertTrue( tm.getCreators().isEmpty());
    if ( ! skipList.contains( "contributors" ) )
      assertTrue( tm.getContributors().isEmpty());
    if ( ! skipList.contains( "keywords" ) )
      assertTrue( tm.getKeywords().isEmpty());
    if ( ! skipList.contains( "metadata" ) )
      assertTrue( tm.getMetadata().isEmpty());
    if ( ! skipList.contains( "projects" ) )
      assertTrue( tm.getProjects().isEmpty());
    if ( ! skipList.contains( "properties" ) )
      assertTrue( tm.getProperties().isEmpty());
    if ( ! skipList.contains( "publishers" ) )
      assertTrue( tm.getPublishers().isEmpty());
    if ( ! skipList.contains( "variables" ) )
      assertTrue( tm.getVariables().isEmpty());
    if ( ! skipList.contains( "authority" ) )
      assertTrue( tm.getDates().isEmpty());

    if ( ! skipList.contains( "geospatialCoverage" ) )
      assertTrue( tm.getGeospatialCoverage() == null || tm.getGeospatialCoverage().isEmpty() );
    if ( ! skipList.contains( "timeCoverage" ) )
      assertTrue( tm.getTimeCoverage() == null || tm.getTimeCoverage().isEmpty() );
  }

  private static String setupCatalogWithNestedDatasets()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='ds 1' ID='ds1'>\n" )
            .append( "    <metadata inherited='true'>" )
            .append( "      <serviceName>all</serviceName>" )
            .append( "      <documentation>fun</documentation>" )
            .append( "    </metadata>\n" )
            .append( "    <dataset name='ds 2' ID='ds2'>\n" )
            .append( "      <metadata inherited='true'>" )
            .append( "        <documentation>more fun</documentation>" )
            .append( "      </metadata>\n" )
            .append( "      <dataset name='Test 1' ID='Test1' urlPath='test/test1.nc'>\n" )
            .append( "        <documentation>even more fun</documentation>" )
            .append( "        <property name='viewer' value='a viewer' />" )
            .append( "        <property name='viewer' value='another viewer' />" )
            .append( "        <property name='viewer2' value='yet another viewer' />" )
            .append( "      </dataset>" )
            .append( "      <dataset name='Test 2' ID='Test2' urlPath='test/test2.nc' />\n" )
            .append( "    </dataset>" )
            .append( "  </dataset>" );

    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), "Catalog 1", "1.0.3", null );
  }

  @Test
  public void checkFindService()
  {
    String name = "all";
    ServiceType serviceType = ServiceType.COMPOUND;
    String base = "";
    InvService service = new InvService( name, serviceType.toString(), base, null, null );
    InvService odapService = new InvService( "odap", ServiceType.OPENDAP.toString(), "/thredds/dodsC/", null, null );
    odapService.addProperty( new InvProperty( "aProp", "aPropVal" ) );
    odapService.addDatasetRoot( new InvProperty( "aDsRoot", "aDsRootVal" ) );
    service.addService( odapService );
    service.addService( new InvService( "wms", ServiceType.WMS.toString(), "/thredds/wms/", null, null ) );
    service.addService( new InvService( "wcs", ServiceType.WCS.toString(), "/thredds/wcs/", null, null ) );

    List<InvService> availServices = new ArrayList<InvService>();
    availServices.add( service);
    assertEquals( service, DeepCopyUtils.findServiceByName( "all", availServices ));
    assertEquals( odapService, DeepCopyUtils.findServiceByName( "odap", availServices ));

    InvService odapService2 = new InvService( "odap2", ServiceType.OPENDAP.toString(), "/thredds/odap/", null, null);
    availServices.add( odapService2);

    assertEquals( service, DeepCopyUtils.findServiceByName( "all", availServices ) );
    assertEquals( odapService, DeepCopyUtils.findServiceByName( "odap", availServices ) );
    assertEquals( odapService2, DeepCopyUtils.findServiceByName( "odap2", availServices ) );

    assertNull( DeepCopyUtils.findServiceByName( "junk", availServices ));
  }

  @Test
  public void checkServiceCopySimple()
  {
    String name = "odap";
    ServiceType serviceType = ServiceType.OPENDAP;
    String base = "/thredds/dodsC/";
    String suffix = ".dods";
    String description = "description";
    InvService service = new InvService( name, serviceType.toString(), base, suffix, description );
    InvService serviceCopy = DeepCopyUtils.copyService( service );

    assertFalse( service == serviceCopy);
    assertEquals( name, serviceCopy.getName() );
    assertEquals( serviceType, serviceCopy.getServiceType() );
    assertEquals( base, serviceCopy.getBase() );
    assertEquals( suffix, serviceCopy.getSuffix());
    assertEquals( description, serviceCopy.getDescription());
  }

  @Test
  public void checkServiceCopySimplest()
  {
    String name = "odap";
    ServiceType serviceType = ServiceType.OPENDAP;
    String base = "/thredds/dodsC/";
    InvService service = new InvService( name, serviceType.toString(), base, null, null );
    InvService serviceCopy = DeepCopyUtils.copyService( service );

    assertFalse( service == serviceCopy);
    assertEquals( name, serviceCopy.getName() );
    assertEquals( serviceType, serviceCopy.getServiceType() );
    assertEquals( base, serviceCopy.getBase() );
    // InvService() converts null to "".
    assertEquals( "", serviceCopy.getSuffix() );
    // getDescription() returns service type if description is null
    assertEquals( ServiceType.OPENDAP.toString(), serviceCopy.getDescription() );
  }

  @Test
  public void checkServiceCopyNested()
  {
    String name = "all";
    ServiceType serviceType = ServiceType.COMPOUND;
    String base = "";
    InvService service = new InvService( name, serviceType.toString(), base, null, null );
    InvService odapService = new InvService( "odap", ServiceType.OPENDAP.toString(), "/thredds/dodsC/", null, null );
    odapService.addProperty( new InvProperty( "aProp", "aPropVal" ) );
    odapService.addDatasetRoot( new InvProperty( "aDsRoot", "aDsRootVal") );
    service.addService( odapService );
    service.addService( new InvService( "wms", ServiceType.WMS.toString(), "/thredds/wms/", null, null) );
    service.addService( new InvService( "wcs", ServiceType.WCS.toString(), "/thredds/wcs/", null, null) );

    InvService serviceCopy = DeepCopyUtils.copyService( service );

    assertFalse( service == serviceCopy );
    assertEquals( name, serviceCopy.getName() );
    assertEquals( serviceType, serviceCopy.getServiceType() );
    assertEquals( base, serviceCopy.getBase() );

    List<InvService> services = service.getServices();
    assertNotNull( services);
    assertEquals( 3, services.size() );
    InvService copyOdapService = services.get( 0 );
    assertEquals( "odap", copyOdapService.getName());
    assertEquals( "wms", services.get(1).getName());
    assertEquals( "wcs", services.get(2).getName());

    assertEquals( ServiceType.OPENDAP, copyOdapService.getServiceType());
    assertEquals( ServiceType.WMS, services.get(1).getServiceType());
    assertEquals( ServiceType.WCS, services.get(2).getServiceType());

    assertEquals( "/thredds/dodsC/", copyOdapService.getBase());
    assertEquals( "/thredds/wms/", services.get(1).getBase());
    assertEquals( "/thredds/wcs/", services.get(2).getBase());

    List<InvProperty> properties = copyOdapService.getProperties();
    assertNotNull( properties);
    assertEquals( 1, properties.size() );
    assertEquals( "aProp", properties.get(0).getName() );
    assertEquals( "aPropVal", properties.get(0).getValue() );

    List<InvProperty> dsRoots = copyOdapService.getDatasetRoots();
    assertNotNull( dsRoots);
    assertEquals( 1, dsRoots.size());
    assertEquals( "aDsRoot", dsRoots.get(0).getName() );
    assertEquals( "aDsRootVal", dsRoots.get(0).getValue() );
  }

  @Test
  public void checkPropertyCopy()
  {
    String propName = "propName";
    String propValue = "propVal";
    InvProperty prop = new InvProperty( propName, propValue );
    InvProperty copyProp = DeepCopyUtils.copyProperty( prop );

    assertFalse( prop == copyProp );
    assertEquals( prop.getName(), copyProp.getName() );
    assertEquals( prop.getValue(), copyProp.getValue() );
  }
}
