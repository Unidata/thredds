package thredds.catalog2.xml.parser.stax;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.URISyntaxException;
import java.net.URI;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.CatalogXmlUtils;
import thredds.catalog2.xml.names.ThreddsMetadataElementNames;
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
public class ProjectParserTest
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
  public void checkParseCompleteProjectElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String docBaseUriString = "http://test.catalog2.thredds/ProjectParserTest/completeProjectElement.xml";

    String elemName = ThreddsMetadataElementNames.ProjectElement.getLocalPart();
    String authorityAttName = ThreddsMetadataElementNames.ControlledVocabType_Authority.getLocalPart();
    String authorityAttValue = "GCMD";
    String projName = "some proj name";

    Map<String, String> attributes = new HashMap<String, String>();
    if ( authorityAttValue != null )
      attributes.put( authorityAttName, authorityAttValue );

    String xml = StaxParserUtils.wrapContentXmlInXmlDocRootElement( elemName, attributes, projName );

    XMLEventReader reader = StaxParserUtils.createXmlEventReaderOnXmlString( xml, docBaseUriString );

    ProjectElementParser.Factory fac = new ProjectElementParser.Factory();
    StaxParserUtils.advanceReaderToFirstStartElement( reader );
    assertTrue( fac.isEventMyStartElement( reader.peek() ) );

    ProjectElementParser projectNameParser = fac.getNewParser( reader, this.fac, this.tmBldr );
    ThreddsMetadataBuilder.ProjectNameBuilder projNameBldr = (ThreddsMetadataBuilder.ProjectNameBuilder) projectNameParser.parse();
    assertNotNull( projNameBldr );

    assertEquals( authorityAttValue, projNameBldr.getNamingAuthority() );
    assertEquals( projName, projNameBldr.getName() );
  }

  @Test
  public void checkCatalogDatasetWrappedProjectElement()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String docBaseUriString = "http://test.catalog2.thredds/ProjectParserTest/wrappedProjectElement.xml";
    String elemName = ThreddsMetadataElementNames.ProjectElement.getLocalPart();
    String authorityAttName = ThreddsMetadataElementNames.ControlledVocabType_Authority.getLocalPart();
    String authorityAttValue = "GCMD";
    String projName = "some proj name";
    String kpXml = "<" + elemName + " " + authorityAttName + "='" + authorityAttValue + "'>"
                   + projName
                   + "</" + elemName + ">";

    assertCatalogDatasetWrappedProjectNameAsExpected( docBaseUriString, kpXml, authorityAttValue, projName );
  }


  private void assertCatalogDatasetWrappedProjectNameAsExpected( String docBaseUriString, String kpXml,
                                                                 String authority, String projName )
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    URI docBaseUri = new URI( docBaseUriString );
    String catalogXml = CatalogXmlUtils.wrapThreddsXmlInContainerDataset( kpXml );

    CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogXml );

    assertNotNull( catBuilder );

    DatasetBuilder dsBldr = CatalogXmlUtils.assertCatalogWithContainerDatasetAsExpected( catBuilder, docBaseUri );
    ThreddsMetadataBuilder tmdBldr = dsBldr.getThreddsMetadataBuilder();
    assertNotNull( tmdBldr);
    List<ThreddsMetadataBuilder.ProjectNameBuilder> projNameBldrs = tmdBldr.getProjectNameBuilders();
    assertNotNull( projNameBldrs );
    assertFalse( projNameBldrs.isEmpty() );
    assertEquals( 1, projNameBldrs.size() );
    ThreddsMetadataBuilder.ProjectNameBuilder projNameBldr = projNameBldrs.get( 0 );
    assertNotNull( projNameBldr );
    assertEquals( authority, projNameBldr.getNamingAuthority() );
    assertEquals( projName, projNameBldr.getName() );
  }
}