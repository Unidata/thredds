package thredds.catalog2.xml.parser.stax;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.ThreddsMetadataElementNames;
import thredds.catalog2.xml.names.CatalogNamespaceUtils;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.simpleImpl.ThreddsBuilderFactoryImpl;

/**
 * Test the thredds.catalog2.xml.parser.stax.GeospatialRangeTypeParser in isolation.
 *
 * @author edavis
 * @since 4.0
 */
public class GeospatialRangeTypeParserTest
{
  private ThreddsBuilderFactory fac;
  private ThreddsMetadataBuilder.GeospatialCoverageBuilder gspCovBldr;

  private String rootDocBaseUri;
  private String startElementName;
  private String sizeElementName;
  private String resolutionElementName;
  private String unitsElementName;

  @Before
  public void createMockObjects()
  {
    this.fac = new ThreddsBuilderFactoryImpl();
    this.gspCovBldr = this.fac.newThreddsMetadataBuilder().getGeospatialCoverageBuilder();

    this.rootDocBaseUri = "http://test/thredds/catalog2/xml/parser/stax/GeospatialRangeTypeParserTest/";

    this.startElementName = ThreddsMetadataElementNames.SpatialRangeType_Start.getLocalPart();
    this.sizeElementName = ThreddsMetadataElementNames.SpatialRangeType_Size.getLocalPart();
    this.resolutionElementName = ThreddsMetadataElementNames.SpatialRangeType_Resolution.getLocalPart();
    this.unitsElementName = ThreddsMetadataElementNames.SpatialRangeType_Units.getLocalPart();

  }

  @Test
  public void checkFullySpecifiedGeospatialRangeType() throws XMLStreamException, ThreddsXmlParserException
  {
    String docBaseUri = this.rootDocBaseUri + "checkFullySpecifiedGeospatialRangeType.test";
    String elementName = "someElemOfTypeGeospatialRange";

    String start = "-55.5";
    String size = "23.0";
    String resolution = "0.5";
    String units = "degrees_east";

    String xml = buildGeospatialRangeTypeElementAsDocRoot( elementName, start, size, resolution, units );

    assertGeospatialRangeTypeXmlAsExpected( xml, docBaseUri, elementName, start, size, resolution, units );
  }

  private String buildGeospatialRangeTypeElementAsDocRoot( String elementName, String start, String size,
                                                           String resolution, String units )
  {
    StringBuilder sb = new StringBuilder();
    if ( start != null )
      sb.append( buildGeospatialRangeTypeStartElement( start ));
    if ( size != null )
      sb.append( "  <end>" ).append( size ).append( "</end>\n" );
    if ( resolution != null )
      sb.append( "  <resolution>" ).append( resolution ).append( "</resolution>\n" );
    if ( units != null )
      sb.append( "  <duration>" ).append( units ).append( "</duration>\n" );

    return StaxParserUtils.wrapContentXmlInXmlDocRootElement( elementName, null, sb.toString() );
  }

  private String buildGeospatialRangeTypeStartElement( String start )
  {
    StringBuilder sb = new StringBuilder().append( "<" ).append( this.startElementName).append( ">" )
            .append( start ).append( "</").append( this.startElementName).append(">\n" );
    return sb.toString();
  }

  private String buildGeospatialRangeTypeSizeElement( String size )
  {
    StringBuilder sb = new StringBuilder().append( "<" ).append( this.sizeElementName).append( ">" )
            .append( size ).append( "</").append( this.sizeElementName).append(">\n" );
    return sb.toString();
  }

  private String buildGeospatialRangeTypeResolutionElement( String resolution )
  {
    StringBuilder sb = new StringBuilder().append( "<" ).append( this.resolutionElementName).append( ">" )
            .append( resolution ).append( "</").append( this.resolutionElementName).append(">\n" );
    return sb.toString();
  }

  private String buildGeospatialRangeTypeUnitsElement( String units )
  {
    StringBuilder sb = new StringBuilder().append( "<" ).append( this.unitsElementName).append( ">" )
            .append( units ).append( "</").append( this.unitsElementName).append(">\n" );
    return sb.toString();
  }

  private void assertGeospatialRangeTypeXmlAsExpected( String docXml, String docBaseUri,
                                                       String expectedRootElementName,
                                                       String expectedStart, String expectedSize,
                                                       String expectedResolution, String expectedUnits )
          throws XMLStreamException, ThreddsXmlParserException
  {
    XMLEventReader reader = StaxParserUtils.createXmlEventReaderOnXmlString( docXml, docBaseUri );
    StaxParserUtils.advanceReaderToFirstStartElement( reader );

    QName rootElemQName = CatalogNamespaceUtils.getThreddsCatalogElementQualifiedName( expectedRootElementName );
    GeospatialRangeTypeParser.Factory factory = new GeospatialRangeTypeParser.Factory( rootElemQName );
    assertNotNull( factory);

    assertTrue( factory.isEventMyStartElement( reader.peek() ));

    GeospatialRangeTypeParser parser = factory.getNewParser( reader, this.fac, this.gspCovBldr );
    assertNotNull( parser);

    ThreddsMetadataBuilder.GeospatialRangeBuilder bldr = (ThreddsMetadataBuilder.GeospatialRangeBuilder) parser.parse();
    assertNotNull( bldr );
//
//    assertTrue( bldr instanceof ThreddsMetadataBuilder.DateRangeBuilder );
//    ThreddsMetadataBuilder.DateRangeBuilder tmBldr = (ThreddsMetadataBuilder.DateRangeBuilder) bldr;
//
//    assertEquals( startDate, tmBldr.getStartDate());
//    assertNull( tmBldr.getStartDateFormat());
//    assertEquals( endDate, tmBldr.getEndDate());
//    assertNull( tmBldr.getEndDateFormat() );
//    assertEquals( duration, tmBldr.getDuration() );
//    assertEquals( resolution, tmBldr.getResolution() );
  }

}