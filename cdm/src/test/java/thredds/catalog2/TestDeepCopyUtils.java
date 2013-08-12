package thredds.catalog2;

import org.junit.Assert;
import org.junit.Test;

import thredds.catalog.*;
import thredds.catalog.ThreddsMetadata;
import thredds.catalog.util.DeepCopyUtils;
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
public class TestDeepCopyUtils
{

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

    InvCatalog resultCatalog = DeepCopyUtils.copyCatalog(catalog);

    assertCatalogAsExpected( resultCatalog, docBaseUri);

    String resultCatalogAsString = fac.writeXML( (InvCatalogImpl) resultCatalog );

    Assert.assertEquals(origCatAsString, resultCatalogAsString);

    Assert.assertEquals(catalog, resultCatalog);
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
    Assert.assertNotNull("Catalog is null.", catalog);
    Assert.assertNotNull("DocBase URI is null.", docBaseUri);

    Assert.assertEquals(docBaseUri.toString() + "/ds2", catalog.getUriString());
    Assert.assertEquals("ds 2", catalog.getName());

    List<InvService> services = catalog.getServices();
    InvService allService = assertCatalogServicesAsExpected( services );

    List<InvDataset> dsList = catalog.getDatasets();
    Assert.assertNotNull(dsList);
    Assert.assertEquals(1, dsList.size());
    InvDataset ds2 = dsList.get( 0 );
    assertDs2AsExpected( allService, ds2, true );
  }

  private void assertCatalogAsExpected( InvCatalog catalog, URI docBaseUri )
  {
    Assert.assertNotNull("Catalog is null.", catalog);
    Assert.assertNotNull("DocBase URI is null.", docBaseUri);

    Assert.assertEquals(catalog.getUriString(), docBaseUri.toString());
    Assert.assertEquals(catalog.getName(), "Catalog 1");

    List<InvService> services = catalog.getServices();
    InvService allService = assertCatalogServicesAsExpected( services );

    List<InvDataset> dsList = catalog.getDatasets();
    Assert.assertNotNull(dsList);
    Assert.assertEquals(1, dsList.size());
    InvDataset ds1 = dsList.get( 0 );

    assertDs1AsExpected( allService, ds1 );

  }

  private InvService assertCatalogServicesAsExpected( List<InvService> services )
  {
    Assert.assertNotNull("Null set of services.", services);
    Assert.assertEquals(1, services.size());
    InvService allService = services.get( 0);
    Assert.assertEquals("all", allService.getName());
    Assert.assertEquals(3, allService.getServices().size());
    return allService;
  }

  private void assertDs1AsExpected( InvService allService, InvDataset ds1 )
  {
    List<InvDataset> dsList;
    Assert.assertEquals("ds 1", ds1.getName());
    Assert.assertEquals("ds1", ds1.getID());
    Assert.assertEquals(allService, ds1.getServiceDefault());
    List<InvMetadata> mdList = ds1.getMetadata();
    Assert.assertNotNull(mdList);
    Assert.assertEquals(0, mdList.size());
    assertThreddsMetadataIsEmpty( ((InvDatasetImpl) ds1).getLocalMetadata(), Collections.<String>emptyList());
    thredds.catalog.ThreddsMetadata tm = ((InvDatasetImpl) ds1).getLocalMetadataInheritable();
    List<String> skipList = new ArrayList<String>();
    skipList.add( "serviceName" );
    skipList.add( "documentation");
    assertThreddsMetadataIsEmpty( tm, skipList);
    Assert.assertEquals("all", tm.getServiceName());
    List<InvDocumentation> docs = tm.getDocumentation();
    Assert.assertNotNull(docs);
    Assert.assertEquals(1, docs.size());
    Assert.assertEquals("fun", docs.get(0).getInlineContent());

    dsList = ds1.getDatasets();
    Assert.assertNotNull(dsList);
    Assert.assertEquals(1, dsList.size());
    InvDataset ds2 = dsList.get( 0);
    assertDs2AsExpected( allService, ds2, false );
  }

  private void assertDs2AsExpected( InvService allService, InvDataset ds2, boolean afterSubset )
  {
    Assert.assertEquals("ds 2", ds2.getName());
    Assert.assertEquals("ds2", ds2.getID());
    Assert.assertEquals(allService, ds2.getServiceDefault());
    List<InvMetadata> mdList = ds2.getMetadata();
    Assert.assertNotNull(mdList);
    Assert.assertEquals(0, mdList.size());

    List<InvDocumentation> docs = ds2.getDocumentation();
    Assert.assertNotNull(docs);
    Assert.assertEquals(2, docs.size());
    Assert.assertEquals("more fun", docs.get(0).getInlineContent());
    Assert.assertEquals("fun", docs.get(1).getInlineContent());

    assertThreddsMetadataIsEmpty( ( (InvDatasetImpl) ds2 ).getLocalMetadata(), Collections.<String>emptyList() );
    thredds.catalog.ThreddsMetadata tm = ( (InvDatasetImpl) ds2 ).getLocalMetadataInheritable();
    List<String> skipList = new ArrayList<String>();
    if ( afterSubset ) {
      skipList.add( "serviceName");
      skipList.add( "documentation");
    } else {
      skipList.add( "documentation" );
    }

    assertThreddsMetadataIsEmpty( tm, skipList );
    docs = tm.getDocumentation();
    Assert.assertNotNull(docs);
    if ( afterSubset ) {
      Assert.assertEquals(2, docs.size());
      Assert.assertEquals("more fun", docs.get(0).getInlineContent());
      Assert.assertEquals("fun", docs.get(1).getInlineContent());
    } else {
      Assert.assertEquals(1, docs.size());
      Assert.assertEquals("more fun", docs.get(0).getInlineContent());
    }

    List<InvDataset> dsList = ds2.getDatasets();
    Assert.assertNotNull(dsList);
    Assert.assertEquals(2, dsList.size());
    InvDataset test1 = dsList.get( 0 );
    assertTest1AsExpected( allService, test1 );

    InvDataset test2 = dsList.get( 1 );
    assertTest2AsExpected( allService, test2 );
  }

  private static void assertTest1AsExpected( InvService allService, InvDataset test1 )
  {
    Assert.assertEquals("Test 1", test1.getName());
    Assert.assertEquals("Test1", test1.getID());
    Assert.assertEquals(allService, test1.getServiceDefault());
    Assert.assertEquals("test/test1.nc", ((InvDatasetImpl) test1).getUrlPath());
    List<InvAccess> access = test1.getAccess();
    Assert.assertNotNull(access);
    Assert.assertEquals(3, access.size());
    Assert.assertEquals(allService.getServices().get(0), access.get(0).getService());
    Assert.assertEquals("test/test1.nc", access.get(0).getUrlPath());

    List<InvMetadata> mdList = test1.getMetadata();
    Assert.assertNotNull(mdList);
    Assert.assertEquals(0, mdList.size());

    List<InvDocumentation> docs = test1.getDocumentation();
    Assert.assertNotNull(docs);
    Assert.assertEquals(3, docs.size());
    Assert.assertEquals("even more fun", docs.get(0).getInlineContent());
    Assert.assertEquals("more fun", docs.get(1).getInlineContent());
    Assert.assertEquals("fun", docs.get(2).getInlineContent());

    List<InvProperty> properties = test1.getProperties();
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("viewer", properties.get(0).getName());
    Assert.assertEquals("a viewer", properties.get(0).getValue());
    Assert.assertEquals("viewer2", properties.get(1).getName());
    Assert.assertEquals("yet another viewer", properties.get(1).getValue());

    thredds.catalog.ThreddsMetadata tm = ( (InvDatasetImpl) test1 ).getLocalMetadata();
    List<String> skipList = new ArrayList<String>();
    skipList.add( "properties" );
    skipList.add( "documentation" );
    assertThreddsMetadataIsEmpty( tm, skipList);
    docs = tm.getDocumentation();
    Assert.assertNotNull(docs);
    Assert.assertEquals(1, docs.size());
    Assert.assertEquals("even more fun", docs.get(0).getInlineContent());
    properties = tm.getProperties();
    Assert.assertNotNull(properties);
    Assert.assertEquals(3, properties.size());
    Assert.assertEquals("viewer", properties.get(0).getName());
    Assert.assertEquals("a viewer", properties.get(0).getValue());
    Assert.assertEquals("viewer", properties.get(1).getName());
    Assert.assertEquals("another viewer", properties.get(1).getValue());
    Assert.assertEquals("viewer2", properties.get(2).getName());
    Assert.assertEquals("yet another viewer", properties.get(2).getValue());

    assertThreddsMetadataIsEmpty( ( (InvDatasetImpl) test1 ).getLocalMetadataInheritable(), Collections.<String>emptyList() );
  }
  private static void assertTest2AsExpected( InvService allService, InvDataset test2 )
  {
    Assert.assertEquals("Test 2", test2.getName());
    Assert.assertEquals("Test2", test2.getID());
    Assert.assertEquals(allService, test2.getServiceDefault());
    Assert.assertEquals("test/test2.nc", ((InvDatasetImpl) test2).getUrlPath());
    List<InvAccess> access = test2.getAccess();
    Assert.assertNotNull(access);
    Assert.assertEquals(3, access.size());
    Assert.assertEquals(allService.getServices().get(0), access.get(0).getService());
    Assert.assertEquals("test/test2.nc", access.get(0).getUrlPath());

    List<InvMetadata> mdList = test2.getMetadata();
    Assert.assertNotNull(mdList);
    Assert.assertEquals(0, mdList.size());

    List<InvDocumentation> docs = test2.getDocumentation();
    Assert.assertNotNull(docs);
    Assert.assertEquals(2, docs.size());
    Assert.assertEquals("more fun", docs.get(0).getInlineContent());
    Assert.assertEquals("fun", docs.get(1).getInlineContent());

    assertThreddsMetadataIsEmpty( ( (InvDatasetImpl) test2 ).getLocalMetadata(), Collections.<String>emptyList());
    assertThreddsMetadataIsEmpty( ( (InvDatasetImpl) test2 ).getLocalMetadataInheritable(), Collections.<String>emptyList() );
  }

  private static void assertThreddsMetadataIsEmpty( ThreddsMetadata tm, List<String> skipList )
  {
    Assert.assertNotNull(tm);
    Assert.assertNotNull(skipList);

    if ( ! skipList.contains( "serviceName" ))
      Assert.assertNull(tm.getServiceName());
    if ( ! skipList.contains( "authority" ))
      Assert.assertNull(tm.getAuthority());
    if ( ! skipList.contains( "dataType" ) )
      Assert.assertNull(tm.getDataType());
    if ( ! skipList.contains( "dataFormatType" ) )
      Assert.assertNull(tm.getDataFormatType());
    if ( ! skipList.contains( "dataSize" ) )
      Assert.assertTrue(tm.getDataSize() == 0.0 || Double.isNaN(tm.getDataSize()));

    if ( ! skipList.contains( "documentation" ) )
      Assert.assertTrue(tm.getDocumentation().isEmpty());
    if ( ! skipList.contains( "creators" ) )
      Assert.assertTrue(tm.getCreators().isEmpty());
    if ( ! skipList.contains( "contributors" ) )
      Assert.assertTrue(tm.getContributors().isEmpty());
    if ( ! skipList.contains( "keywords" ) )
      Assert.assertTrue(tm.getKeywords().isEmpty());
    if ( ! skipList.contains( "metadata" ) )
      Assert.assertTrue(tm.getMetadata().isEmpty());
    if ( ! skipList.contains( "projects" ) )
      Assert.assertTrue(tm.getProjects().isEmpty());
    if ( ! skipList.contains( "properties" ) )
      Assert.assertTrue(tm.getProperties().isEmpty());
    if ( ! skipList.contains( "publishers" ) )
      Assert.assertTrue(tm.getPublishers().isEmpty());
    if ( ! skipList.contains( "variables" ) )
      Assert.assertTrue(tm.getVariables().isEmpty());
    if ( ! skipList.contains( "authority" ) )
      Assert.assertTrue(tm.getDates().isEmpty());

    if ( ! skipList.contains( "geospatialCoverage" ) )
      Assert.assertTrue(tm.getGeospatialCoverage() == null || tm.getGeospatialCoverage().isEmpty());
    if ( ! skipList.contains( "timeCoverage" ) )
      Assert.assertTrue(tm.getTimeCoverage() == null || tm.getTimeCoverage().isEmpty());
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
    service.addService( new InvService( "wcs", ServiceType.WCS.toString(), "/thredds/wcs/wcs/", null, null ) );

    List<InvService> availServices = new ArrayList<InvService>();
    availServices.add( service);
    Assert.assertEquals(service, DeepCopyUtils.findServiceByName("all", availServices));
    Assert.assertEquals(odapService, DeepCopyUtils.findServiceByName("odap", availServices));

    InvService odapService2 = new InvService( "odap2", ServiceType.OPENDAP.toString(), "/thredds/odap/", null, null);
    availServices.add( odapService2);

    Assert.assertEquals(service, DeepCopyUtils.findServiceByName("all", availServices));
    Assert.assertEquals(odapService, DeepCopyUtils.findServiceByName("odap", availServices));
    Assert.assertEquals(odapService2, DeepCopyUtils.findServiceByName("odap2", availServices));

    Assert.assertNull(DeepCopyUtils.findServiceByName("junk", availServices));
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

    Assert.assertFalse(service == serviceCopy);
    Assert.assertEquals(name, serviceCopy.getName());
    Assert.assertEquals(serviceType, serviceCopy.getServiceType());
    Assert.assertEquals(base, serviceCopy.getBase());
    Assert.assertEquals(suffix, serviceCopy.getSuffix());
    Assert.assertEquals(description, serviceCopy.getDescription());
  }

  @Test
  public void checkServiceCopySimplest()
  {
    String name = "odap";
    ServiceType serviceType = ServiceType.OPENDAP;
    String base = "/thredds/dodsC/";
    InvService service = new InvService( name, serviceType.toString(), base, null, null );
    InvService serviceCopy = DeepCopyUtils.copyService( service );

    Assert.assertFalse(service == serviceCopy);
    Assert.assertEquals(name, serviceCopy.getName());
    Assert.assertEquals(serviceType, serviceCopy.getServiceType());
    Assert.assertEquals(base, serviceCopy.getBase());
    // InvService() converts null to "".
    Assert.assertEquals("", serviceCopy.getSuffix());
    // getDescription() returns service type if description is null
    Assert.assertEquals(ServiceType.OPENDAP.toString(), serviceCopy.getDescription());
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
    service.addService( new InvService( "wcs", ServiceType.WCS.toString(), "/thredds/wcs/wcs/", null, null) );

    InvService serviceCopy = DeepCopyUtils.copyService( service );

    Assert.assertFalse(service == serviceCopy);
    Assert.assertEquals(name, serviceCopy.getName());
    Assert.assertEquals(serviceType, serviceCopy.getServiceType());
    Assert.assertEquals(base, serviceCopy.getBase());

    List<InvService> services = service.getServices();
    Assert.assertNotNull(services);
    Assert.assertEquals(3, services.size());
    InvService copyOdapService = services.get( 0 );
    Assert.assertEquals("odap", copyOdapService.getName());
    Assert.assertEquals("wms", services.get(1).getName());
    Assert.assertEquals("wcs", services.get(2).getName());

    Assert.assertEquals(ServiceType.OPENDAP, copyOdapService.getServiceType());
    Assert.assertEquals(ServiceType.WMS, services.get(1).getServiceType());
    Assert.assertEquals(ServiceType.WCS, services.get(2).getServiceType());

    Assert.assertEquals("/thredds/dodsC/", copyOdapService.getBase());
    Assert.assertEquals("/thredds/wms/", services.get(1).getBase());
    Assert.assertEquals("/thredds/wcs/wcs/", services.get(2).getBase());

    List<InvProperty> properties = copyOdapService.getProperties();
    Assert.assertNotNull(properties);
    Assert.assertEquals(1, properties.size());
    Assert.assertEquals("aProp", properties.get(0).getName());
    Assert.assertEquals("aPropVal", properties.get(0).getValue());

    List<InvProperty> dsRoots = copyOdapService.getDatasetRoots();
    Assert.assertNotNull(dsRoots);
    Assert.assertEquals(1, dsRoots.size());
    Assert.assertEquals("aDsRoot", dsRoots.get(0).getName());
    Assert.assertEquals("aDsRootVal", dsRoots.get(0).getValue());
  }

  @Test
  public void checkPropertyCopy()
  {
    String propName = "propName";
    String propValue = "propVal";
    InvProperty prop = new InvProperty( propName, propValue );
    InvProperty copyProp = DeepCopyUtils.copyProperty( prop );

    Assert.assertFalse(prop == copyProp);
    Assert.assertEquals(prop.getName(), copyProp.getName());
    Assert.assertEquals(prop.getValue(), copyProp.getValue());
  }
}
