package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.ThreddsMetadataElementNames;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.ThreddsBuilderFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.XMLEventReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class GeospatialRangeTypeParser extends AbstractElementParser
{

  private final ThreddsMetadataBuilder.GeospatialCoverageBuilder parentBuilder;

  private ThreddsMetadataBuilder.GeospatialRangeBuilder selfBuilder;

  private CharContentOnlyElementParser.Factory startFac;
  private CharContentOnlyElementParser.Factory sizeFac;
  private CharContentOnlyElementParser.Factory resolutionFac;
  private CharContentOnlyElementParser.Factory unitsFac;

  private String startAsString;
  private String sizeAsString;
  private String resolutionAsString;
  private String unitsAsString;

  private GeospatialRangeTypeParser( QName elementName,
                                     XMLEventReader reader, ThreddsBuilderFactory builderFactory,
                                     ThreddsMetadataBuilder.GeospatialCoverageBuilder parentBuilder ) {
    super( elementName, reader, builderFactory );
    this.parentBuilder = parentBuilder;

    this.startFac = new CharContentOnlyElementParser.Factory( ThreddsMetadataElementNames.SpatialRangeType_Start );
    this.sizeFac = new CharContentOnlyElementParser.Factory( ThreddsMetadataElementNames.SpatialRangeType_Size );
    this.resolutionFac = new CharContentOnlyElementParser.Factory( ThreddsMetadataElementNames.SpatialRangeType_Resolution );
    this.unitsFac = new CharContentOnlyElementParser.Factory( ThreddsMetadataElementNames.SpatialRangeType_Units );
  }

  @Override
  ThreddsMetadataBuilder.GeospatialRangeBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  @Override
  void parseStartElement()
          throws ThreddsXmlParserException
  {
    StaxThreddsXmlParserUtils.readNextEventCheckItIsStartElementWithExpectedName( reader, this.elementName);
  }

  @Override
  void handleChildStartElement() throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.startFac.isEventMyStartElement( startElement ))
    {
      CharContentOnlyElementParser startParser = this.startFac.getParser();
      startParser.parseElement( this.reader );
      this.startAsString = startParser.getValue();
    }
    else if ( this.sizeFac.isEventMyStartElement( startElement ))
    {
      CharContentOnlyElementParser parser = this.sizeFac.getParser();
      parser.parseElement( this.reader );
      this.sizeAsString = parser.getValue();
    }
    else if ( this.resolutionFac.isEventMyStartElement( startElement ))
    {
      CharContentOnlyElementParser parser = this.resolutionFac.getParser();
      parser.parseElement( this.reader );
      this.resolutionAsString = parser.getValue();
    }
    else if ( this.unitsFac.isEventMyStartElement( startElement ))
    {
      CharContentOnlyElementParser parser = this.unitsFac.getParser();
      parser.parseElement( this.reader );
      this.unitsAsString = parser.getValue();
    }
    else
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
  }

  @Override
  void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
    if ( startAsString != null )
      this.selfBuilder.setStart( this.parseDouble( startAsString ) );
    if ( sizeAsString != null )
      this.selfBuilder.setSize( this.parseDouble( sizeAsString ) );
    if ( resolutionAsString != null )
      this.selfBuilder.setResolution( this.parseDouble( resolutionAsString ) );

    return;
  }
  private double parseDouble( String doubleAsString)
  {
    if ( doubleAsString == null ) return Double.NaN;
    try {
      return Double.parseDouble( doubleAsString);
    }
    catch( NumberFormatException e ) {
      // ToDo add parse issue to
      return Double.NaN;
    }
  }

  static class Factory
  {
    private QName elementName;

    Factory( QName elementName ) {
      this.elementName = elementName;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    GeospatialRangeTypeParser getNewParser( XMLEventReader reader, ThreddsBuilderFactory builderFactory,
                                            ThreddsMetadataBuilder.GeospatialCoverageBuilder parentBuilder ) {
      return new GeospatialRangeTypeParser( this.elementName, reader, builderFactory, parentBuilder );
    }
  }

  private static class CharContentOnlyElementParser
  {
    private QName elementName;

    private String value;

    private CharContentOnlyElementParser( QName elementName ) {
      this.elementName = elementName;
    }

    String getValue() {
      return this.value;
    }

    void parseElement( XMLEventReader reader )
            throws ThreddsXmlParserException
    {
      StartElement startElement = StaxThreddsXmlParserUtils.readNextEventCheckItIsStartElementWithExpectedName( reader, this.elementName );

      value = StaxThreddsXmlParserUtils.getCharacterContent( reader, this.elementName );

      StaxThreddsXmlParserUtils.readNextEventCheckItIsEndElementWithExpectedName( reader, this.elementName );
    }

    static class Factory
    {
      private QName elementName;

      Factory( QName elementName ) {
        this.elementName = elementName;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      CharContentOnlyElementParser getParser() {
        return new CharContentOnlyElementParser( this.elementName );
      }
    }
  }
}