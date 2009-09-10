package thredds.catalog2.xml.parser;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.AccessBuilder;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Check that DatasetBuilder.getAccessBuilder() has the expected values for various catalog XML situations.
 *
 * <ol>
 * <li> dataset[urlPath='dir/file1.nc'] && dataset@serviceName</li>
 * <li> dataset[urlPath='dir/file1.nc'] && dataset/serviceName</li>
 * <li> dataset[urlPath='dir/file1.nc'] && dataset/metadata/serviceName</li>
 * <li> dataset[urlPath='dir/file1.nc'] && dataset/metadata[inherited=true]/serviceName</li>
 * </ol>
 *
 * As well as
 * <ul>
 * <li> not inheriting from the first three,</li>
 * <li> inheriting from the fourth, and</li>
 * <li> overriding the inherited serviceName when child dataset has form of any of the four options above.</li>
 * </ul>
 *
 * And finally: repeat all the above using dataset/access
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceNameInheritanceTest
{
  @Test
  public void checkDatasetWithServiceNameAttribute()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/attribute.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetWithServiceNameAttribute();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetWithServiceNameAttribute()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc' serviceName='odap' />" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetWithServiceNameInChildElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/childElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetWithServiceNameInChildElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetWithServiceNameInChildElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetWithServiceNameInMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/metadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetWithServiceNameInMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetWithServiceNameInMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "  <metadata>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  </metadata>" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetWithServiceNameInInheritedMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/inheritedMetadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetWithServiceNameInInheritedMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetWithServiceNameInInheritedMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  </metadata>" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetNotInheritingServiceNameFromParentAuthorityAttribute()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/notInheritAttribute.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetNotInheritingServiceNameFromParentAuthorityAttribute();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
    assertDatasetTwoHasNoAccess( catBuilder );
  }

  private static String setupDatasetNotInheritingServiceNameFromParentAuthorityAttribute()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc' serviceName='odap'>" )
            .append( "  <dataset name='dataset 2' ID='DS2' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetNotInheritingServiceNameFromSiblingServiceElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/notInheritFromSiblingElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetNotInheritingServiceNameFromSiblingServiceElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
    assertDatasetTwoHasNoAccess( catBuilder );
  }

  private static String setupDatasetNotInheritingServiceNameFromSiblingServiceElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  <dataset name='dataset 2' ID='DS2' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetNotInheritingServiceNameFromParentMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/notInheritFromParentMetadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetNotInheritingServiceNameFromParentMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
    assertDatasetTwoHasNoAccess( catBuilder );
  }

  private static String setupDatasetNotInheritingServiceNameFromParentMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "  <metadata>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='dataset 2' ID='DS2' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetInheritingServiceNameFromParentInheritedMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/notInheritFromParentInheritedMetadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetInheritingServiceNameFromParentInheritedMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
    assertDatasetTwoHasOdapAccess( catBuilder );
  }

  private static String setupDatasetInheritingServiceNameFromParentInheritedMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='ds2' ID='DS2' urlPath='dir/file2.nc' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetOverridingInheritedServiceNameWithAttribute()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/overrideInheritedServiceNameWithAttribute.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetOverridingInheritedServiceNameWithAttribute();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
    assertDatasetTwoHasWcsAccess( catBuilder );
  }

  private static String setupDatasetOverridingInheritedServiceNameWithAttribute()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='ds2' ID='DS2' urlPath='dir/file2.nc' serviceName='wcs' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetOverridingInheritedServiceNameWithChildElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/overrideInheritedServiceNameWithChildElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetOverridingInheritedServiceNameWithChildElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
    assertDatasetTwoHasWcsAccess( catBuilder );
  }

  private static String setupDatasetOverridingInheritedServiceNameWithChildElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='ds2' ID='DS2' urlPath='dir/file2.nc'>" )
            .append( "    <serviceName>wcs</serviceName>" )
            .append( "  </dataset>" )
            .append( "</dataset>" );

    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetOverridingInheritedServiceNameWithChildMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/overrideInheritedServiceNameWithChildMetadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetOverridingInheritedServiceNameWithChildMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
    assertDatasetTwoHasWcsAccess( catBuilder );
  }

  private static String setupDatasetOverridingInheritedServiceNameWithChildMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='ds2' ID='DS2' urlPath='dir/file2.nc'>" )
            .append( "    <metadata>" )
            .append( "      <serviceName>wcs</serviceName>" )
            .append( "    </metadata>" )
            .append( "  </dataset>" )
            .append( "</dataset>" );

    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetOverridingInheritedServiceWithChildInheritedMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/overrideInheritedServiceNameWithChildInheritedMetadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetOverridingInheritedServiceWithChildInheritedMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
    assertDatasetTwoHasWcsAccess( catBuilder );
  }

  private static String setupDatasetOverridingInheritedServiceWithChildInheritedMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' urlPath='dir/file1.nc'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <serviceName>odap</serviceName>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='ds2' ID='DS2' urlPath='dir/file2.nc'>" )
            .append( "    <metadata inherited='true'>" )
            .append( "      <serviceName>wcs</serviceName>" )
            .append( "    </metadata>" )
            .append( "  </dataset>" )
            .append( "</dataset>" );

    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetAccessWithLocalServiceNameAttribute()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/dsAccessWithLocalAttribute.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetAccessWithLocalServiceNameAttribute();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetAccessWithLocalServiceNameAttribute()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1'>" )
            .append( "  <access urlPath='dir/file1.nc' serviceName='odap' />")
            .append( "</dataset>");
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetAccessWithParentServiceNameAttribute()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/dsAccessWithParentAttribute.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetAccessWithParentServiceNameAttribute();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetAccessWithParentServiceNameAttribute()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1' serviceName='odap'>" )
            .append( "  <access urlPath='dir/file1.nc' />")
            .append( "</dataset>");
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetAccessWithServiceNameElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/dsAccessWithServiceNameElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetAccessWithServiceNameElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetAccessWithServiceNameElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1'>" )
            .append( "  <serviceName>odap</serviceName>")
            .append( "  <access urlPath='dir/file1.nc' />")
            .append( "</dataset>");
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetAccessWithMetadataServiceNameElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/dsAccessWithMetadataServiceNameElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetAccessWithMetadataServiceNameElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetAccessWithMetadataServiceNameElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1'>" )
            .append( "  <metadata>")
            .append( "    <serviceName>odap</serviceName>")
            .append( "  </metadata>" )
            .append( "  <access urlPath='dir/file1.nc' />")
            .append( "</dataset>");
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  @Test
  public void checkDatasetAccessWithInheritedMetadataServiceNameElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/ServiceNameInheritanceTest/dsAccessWithInheritedMetadataServiceNameElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetAccessWithInheritedMetadataServiceNameElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasOdapAccess( catBuilder );
  }

  private static String setupDatasetAccessWithInheritedMetadataServiceNameElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='ds1' ID='DS1'>" )
            .append( "  <metadata inherited='true'>")
            .append( "    <serviceName>odap</serviceName>")
            .append( "  </metadata>" )
            .append( "  <access urlPath='dir/file1.nc' />")
            .append( "</dataset>");
    return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  private static void assertDatasetOneHasOdapAccess( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetOne( catBuilder );
    List<AccessBuilder> accessBldrs = dsBldr.getAccessBuilders();
    assertEquals( 1, accessBldrs.size());
    AccessBuilder access = accessBldrs.get( 0);
    assertOdapAccessToFile1( access );
  }

  private static void assertDatasetTwoHasOdapAccess( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetTwo( catBuilder );
    List<AccessBuilder> accessBldrs = dsBldr.getAccessBuilders();
    assertEquals( 1, accessBldrs.size());
    AccessBuilder access = accessBldrs.get( 0);
    assertOdapAccessToFile2( access );
  }

  private static void assertDatasetTwoHasWcsAccess( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetTwo( catBuilder );
    List<AccessBuilder> accessBldrs = dsBldr.getAccessBuilders();
    assertEquals( 1, accessBldrs.size());
    AccessBuilder access = accessBldrs.get( 0);
    assertWcsAccess( access );
  }

  private static void assertDatasetTwoHasNoAccess( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetTwo( catBuilder );
    assertEquals( 0, dsBldr.getAccessBuilders().size());
  }

  private static void assertOdapAccessToFile1( AccessBuilder access )
  {
    assertEquals( "dir/file1.nc", access.getUrlPath() );
    assertEquals( "odap", access.getServiceBuilder().getName() );
    assertEquals( ServiceType.OPENDAP, access.getServiceBuilder().getType() );
    assertEquals( "/thredds/dodsC/", access.getServiceBuilder().getBaseUri().toString() );
  }

  private static void assertOdapAccessToFile2( AccessBuilder access )
  {
    assertEquals( "dir/file2.nc", access.getUrlPath() );
    assertEquals( "odap", access.getServiceBuilder().getName() );
    assertEquals( ServiceType.OPENDAP, access.getServiceBuilder().getType() );
    assertEquals( "/thredds/dodsC/", access.getServiceBuilder().getBaseUri().toString() );
  }

  private static void assertWcsAccess( AccessBuilder access )
  {
    assertEquals( "dir/file2.nc", access.getUrlPath() );
    assertEquals( "wcs", access.getServiceBuilder().getName() );
    assertEquals( ServiceType.WCS, access.getServiceBuilder().getType() );
    assertEquals( "/thredds/wcs/", access.getServiceBuilder().getBaseUri().toString() );
  }

  private static DatasetBuilder assertGetDatasetOne( CatalogBuilder catBuilder ) {
    List<DatasetNodeBuilder> datasetNodes = catBuilder.getDatasetNodeBuilders();
    assertEquals( 1, datasetNodes.size() );
    DatasetBuilder dsBldr = (DatasetBuilder) datasetNodes.get( 0 );
    assertEquals( "DS1", dsBldr.getId() );
    return dsBldr;
  }

  private static DatasetBuilder assertGetDatasetTwo( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetOne( catBuilder);

    List<DatasetNodeBuilder> datasetNodes = dsBldr.getDatasetNodeBuilders();
    assertEquals( 1, datasetNodes.size() );
    dsBldr = (DatasetBuilder) datasetNodes.get( 0 );
    assertEquals( "DS2", dsBldr.getId() );
    return dsBldr;
  }

}