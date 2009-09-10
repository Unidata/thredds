package thredds.catalog2.xml.parser;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.builder.DatasetBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Check that DatasetBuilder.getIdAuthority() gets set to the expected value for various catalog XML situations.
 *
 * <pre>
 * - dataset@attribute
 * - dataset/attribute
 * - dataset/metadata/attribute
 * - dataset/metadata[inherited=true]/attribute
 * </pre>
 *
 * As well as not inheriting from the first three, inheriting from the fourth, and overriding the fourth when any of the four are child datasets.  
 *
 * @author edavis
 * @since 4.0
 */
public class IdAuthorityInheritanceTest
{
  @Test
  public void checkDatasetWithIdAuthorityAsAttributeAsExpected()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/attribute.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetWithIdAuthorityAsAttribute();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasAuthorityOne( catBuilder );
  }

  private static String setupDatasetWithIdAuthorityAsAttribute()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset' ID='DS1' authority='auth1' />" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetWithIdAuthorityAsChildElementAsExpected()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/childElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetWithIdAuthorityAsChildElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasAuthorityOne( catBuilder );
  }

  private static String setupDatasetWithIdAuthorityAsChildElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <authority>auth1</authority>" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetWithIdAuthorityInMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/metadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetWithIdAuthorityInMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasAuthorityOne( catBuilder );
  }

  private static String setupDatasetWithIdAuthorityInMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <metadata>" )
            .append( "    <authority>auth1</authority>" )
            .append( "  </metadata>" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetWithIdAuthorityInInheritedMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/inheritedMetadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetWithIdAuthorityInInheritedMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetOneHasAuthorityOne( catBuilder );
  }

  private static String setupDatasetWithIdAuthorityInInheritedMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <authority>auth1</authority>" )
            .append( "  </metadata>" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetNotInheritingIdAuthorityFromParentAuthorityAttribute()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/notInheritFromParentAttribute.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetNotInheritingIdAuthorityFromParentAuthorityAttribute();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetTwoHasNoAuthority( catBuilder );
  }

  private static String setupDatasetNotInheritingIdAuthorityFromParentAuthorityAttribute()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1' authority='auth1'>" )
            .append( "  <dataset name='dataset 2' ID='DS2' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetNotInheritingIdAuthorityFromParentAuthorityElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/notInheritFromParentElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetNotInheritingIdAuthorityFromParentAuthorityElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetTwoHasNoAuthority( catBuilder );
  }

  private static String setupDatasetNotInheritingIdAuthorityFromParentAuthorityElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <authority>auth1</authority>" )
            .append( "  <dataset name='dataset 2' ID='DS2' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetNotInheritingIdAuthorityFromParentMetadataAuthorityElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/notInheritFromParentMetadataAuthorityElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetNotInheritingIdAuthorityFromParentMetadataAuthorityElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetTwoHasNoAuthority( catBuilder );
  }

  private static String setupDatasetNotInheritingIdAuthorityFromParentMetadataAuthorityElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <metadata>" )
            .append( "    <authority>auth1</authority>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='dataset 2' ID='DS2' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetInheritingIdAuthorityFromParentInheritedMetadataAuthorityElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/inheritFromParentInheritedMetadataAuthorityElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetInheritingIdAuthorityFromParentInheritedMetadataAuthorityElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetTwoHasAuthorityOne( catBuilder );
  }

  private static String setupDatasetInheritingIdAuthorityFromParentInheritedMetadataAuthorityElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <authority>auth1</authority>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='dataset 2' ID='DS2' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetOverridingInheritedIdAuthorityWithAttribute()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/overrideInheritedWithAttribute.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetOverridingInheritedIdAuthorityWithAttribute();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetTwoHasAuthorityTwo( catBuilder );
  }

  private static String setupDatasetOverridingInheritedIdAuthorityWithAttribute()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <authority>auth1</authority>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='dataset 2' ID='DS2' authority='auth2' />" )
            .append( "</dataset>" );
    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetOverridingInheritedIdAuthorityWithChildElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/overrideInheritedWithElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetOverridingInheritedIdAuthorityWithChildElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetTwoHasAuthorityTwo( catBuilder );
  }

  private static String setupDatasetOverridingInheritedIdAuthorityWithChildElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <authority>auth1</authority>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='dataset 2' ID='DS2'>" )
            .append( "    <authority>auth2</authority>" )
            .append( "  </dataset>" )
            .append( "</dataset>" );

    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetOverridingInheritedIdAuthorityWithChildMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/overrideInheritedWithchildMetadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetOverridingInheritedIdAuthorityWithChildMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetTwoHasAuthorityTwo( catBuilder );
  }

  private static String setupDatasetOverridingInheritedIdAuthorityWithChildMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <authority>auth1</authority>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='dataset 2' ID='DS2'>" )
            .append( "    <metadata>" )
            .append( "      <authority>auth2</authority>" )
            .append( "    </metadata>" )
            .append( "  </dataset>" )
            .append( "</dataset>" );

    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  @Test
  public void checkDatasetOverridingInheritedIdAuthorityWithChildInheritedMetadataElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/IdAuthorityInheritanceTest/overrideInheritedWithchildInheritedMetadataElement.xml";
    URI docBaseUri = new URI( docBaseUriString );
    String catalogAsString = setupDatasetOverridingInheritedIdAuthorityWithChildInheritedMetadataElement();

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogAsString );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, null );
    assertDatasetTwoHasAuthorityTwo( catBuilder );
  }

  private static String setupDatasetOverridingInheritedIdAuthorityWithChildInheritedMetadataElement()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<dataset name='dataset 1' ID='DS1'>" )
            .append( "  <metadata inherited='true'>" )
            .append( "    <authority>auth1</authority>" )
            .append( "  </metadata>" )
            .append( "  <dataset name='dataset 2' ID='DS2'>" )
            .append( "    <metadata inherited='true'>" )
            .append( "      <authority>auth2</authority>" )
            .append( "    </metadata>" )
            .append( "  </dataset>" )
            .append( "</dataset>" );

    return CatalogXmlUtils.wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  private static void assertDatasetOneHasAuthorityOne( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetOne( catBuilder );
    assertEquals( "auth1", dsBldr.getIdAuthority() );
  }

  private static void assertDatasetTwoHasNoAuthority( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetTwo( catBuilder );
    assertNull( dsBldr.getIdAuthority() );
  }

  private static void assertDatasetTwoHasAuthorityOne( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetTwo( catBuilder );
    assertEquals( "auth1", dsBldr.getIdAuthority() );
  }

  private static void assertDatasetTwoHasAuthorityTwo( CatalogBuilder catBuilder ) {
    DatasetBuilder dsBldr = assertGetDatasetTwo( catBuilder );
    assertEquals( "auth2", dsBldr.getIdAuthority() );
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
