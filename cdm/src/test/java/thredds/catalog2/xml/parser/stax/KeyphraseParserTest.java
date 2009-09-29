package thredds.catalog2.xml.parser.stax;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.URISyntaxException;
import java.net.URI;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.CatalogXmlUtils;
import thredds.catalog2.xml.names.CatalogNamespace;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.simpleImpl.ThreddsBuilderFactoryImpl;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class KeyphraseParserTest
{
  private ThreddsBuilderFactory fac;
  private ThreddsMetadataBuilder tmBldr;

  @Before
  public void createMockObjects()
  {
    this.fac = new ThreddsBuilderFactoryImpl();
    this.tmBldr = this.fac.newThreddsMetadataBuilder();
  }

  @Test
  public void checkParseCompleteKeywordElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "keyword";
    String authority = "GCMD";
    String phrase = "some keyphrase";

    Map<String, String> attributes = new HashMap<String, String>();
    if ( authority != null )
      attributes.put( "vocabulary", authority );

    String xml = StaxParserUtils.wrapContentXmlInXmlDocRootElement( elemName, attributes, phrase );

    QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );

    XMLEventReader reader = StaxParserUtils.createXmlEventReaderOnXmlString( xml, "http://test.catalog2.thredds/DateTypeParserTest/someTest.xml" );

    KeyphraseElementParser.Factory fac = new KeyphraseElementParser.Factory();
    StaxParserUtils.advanceReaderToFirstStartElement( reader );
    assertTrue( fac.isEventMyStartElement( reader.peek() ) );

    KeyphraseElementParser keyphraseParser = fac.getNewParser( reader, this.fac, this.tmBldr );
    ThreddsMetadataBuilder.KeyphraseBuilder keyphraseBldr = (ThreddsMetadataBuilder.KeyphraseBuilder) keyphraseParser.parse();
    assertNotNull( keyphraseBldr );

    assertEquals( authority, keyphraseBldr.getAuthority());
    assertEquals( phrase, keyphraseBldr.getPhrase());
  }

  @Test
  public void checkCatalogDatasetWrappedKeyphraseElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://cat2.stax.KeyphraseParserTest/checkCatDsWrappedKeyphrase.xml";
    String authority = "GCMD";
    String phrase = "some keyphrase";
    String kpXml = "<keyword vocabulary='" + authority + "'>" + phrase + "</keyword>";

    assertCatalogDatasetWrappedKeyphraseAsExpected( docBaseUriString, kpXml, authority, phrase );
  }


  private void assertCatalogDatasetWrappedKeyphraseAsExpected( String docBaseUriString, String kpXml,
                                                               String authority, String phrase )
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    URI docBaseUri = new URI( docBaseUriString );
    String catalogXml = CatalogXmlUtils.wrapThreddsXmlInContainerDataset( kpXml );

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogXml );

    assertNotNull( catBuilder );

    DatasetBuilder dsBldr = CatalogXmlUtils.assertCatalogWithContainerDatasetAsExpected( catBuilder, docBaseUri );
    ThreddsMetadataBuilder tmdBldr = dsBldr.getThreddsMetadataBuilder();
    tmdBldr.getKeyphraseBuilder();
    List<ThreddsMetadataBuilder.KeyphraseBuilder> kpBldrs = tmdBldr.getKeyphraseBuilder();
    assertNotNull( kpBldrs);
    assertFalse( kpBldrs.isEmpty());
    assertEquals( 1, kpBldrs.size());
    ThreddsMetadataBuilder.KeyphraseBuilder kpBldr = kpBldrs.get( 0 );
    assertNotNull( kpBldr);
    assertEquals( authority, kpBldr.getAuthority());
    assertEquals( phrase, kpBldr.getPhrase());
  }
}