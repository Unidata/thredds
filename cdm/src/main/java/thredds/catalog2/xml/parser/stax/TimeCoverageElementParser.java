package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.ThreddsXmlParserIssue;
import thredds.catalog2.xml.names.ThreddsMetadataElementNames;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.namespace.QName;

/**
 * _more_
*
* @author edavis
* @since 4.0
*/
class TimeCoverageElementParser extends AbstractElementParser
{
  private final ThreddsMetadataBuilder parentBuilder;
  private ThreddsMetadataBuilder.DateRangeBuilder selfBuilder;

  private DateTypeParser.Factory startElementParserFactory;
  private DateTypeParser startElementParser;

  private DateTypeParser.Factory endElementParserFactory;
  private DateTypeParser endElementParser;

  private DurationParser.Factory durationParserFactory;
  private DurationParser durationParser;

  private DurationParser.Factory resolutionParserFactory;
  private DurationParser resolutionParser;

  private TimeCoverageElementParser( QName elementName,
                                     XMLEventReader reader,
                                     ThreddsBuilderFactory builderFactory,
                                     ThreddsMetadataBuilder parentBuilder )
  {
    super( elementName, reader, builderFactory );
    this.parentBuilder = parentBuilder;

    this.startElementParserFactory = new DateTypeParser.Factory( ThreddsMetadataElementNames.DateRangeType_StartElement );
    this.endElementParserFactory = new DateTypeParser.Factory( ThreddsMetadataElementNames.DateRangeType_EndElement );
    this.durationParserFactory = new DurationParser.Factory( ThreddsMetadataElementNames.DateRangeType_DurationElement);
    this.resolutionParserFactory = new DurationParser.Factory( ThreddsMetadataElementNames.DateRangeType_ResolutionElement);
  }

  ThreddsBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    this.getNextEventIfStartElementIsMine();
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.startElementParserFactory.isEventMyStartElement( startElement ) )
    {
      this.startElementParser = this.startElementParserFactory.getNewDateTypeParser();
      this.startElementParser.parseElement( this.reader );
    }
    else if ( this.endElementParserFactory.isEventMyStartElement( startElement ) )
    {
      this.endElementParser = this.endElementParserFactory.getNewDateTypeParser();
      this.endElementParser.parseElement( this.reader );
    }
    else if ( this.durationParserFactory.isEventMyStartElement( startElement ) )
    {
      this.durationParser = this.durationParserFactory.getNewParser();
      this.durationParser.parseElement( this.reader );
    }
    else if ( this.resolutionParserFactory.isEventMyStartElement( startElement ) )
    {
      this.resolutionParser = this.resolutionParserFactory.getNewParser();
      this.resolutionParser.parseElement( this.reader );
    }
    else
    {
      String unexpectedElement = StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
      ThreddsXmlParserIssue issue = new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.ERROR, "Unrecognized element: " + unexpectedElement, this.parentBuilder, null);
      throw new ThreddsXmlParserException( issue);
    }
  }

  void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
    String startDate = this.startElementParser != null ? this.startElementParser.getValue() : null;
    String startDateFormat = this.startElementParser != null ? this.startElementParser.getFormat() : null;
    String endDate = this.endElementParser != null ? this.endElementParser.getValue() : null;
    String endDateFormat = this.endElementParser != null ? this.endElementParser.getFormat() : null;
    String duration = this.durationParser != null ? this.durationParser.getValue() : null;
    String resolution = this.resolutionParser != null ? this.resolutionParser.getValue() : null;

    this.selfBuilder = this.parentBuilder.setTemporalCoverageBuilder( startDate, startDateFormat,
                                                                      endDate, endDateFormat,
                                                                      duration, resolution );
  }

  static class Factory
  {
    private QName elementName;

    Factory() {
      this.elementName = ThreddsMetadataElementNames.TimeCoverageElement;
    }

    boolean isEventMyStartElement( XMLEvent event )
    {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    TimeCoverageElementParser getNewParser( XMLEventReader reader,
                                            ThreddsBuilderFactory builderFactory,
                                            ThreddsMetadataBuilder parentBuilder )
    {
      return new TimeCoverageElementParser( this.elementName, reader, builderFactory, parentBuilder );
    }
  }

  static class DurationParser
  {
    private QName elementName;

    private String value;

    private DurationParser( QName elementName ) {
      this.elementName = elementName;
    }

    String getValue() {
      return this.value;
    }

    void parseElement( XMLEventReader reader )
            throws ThreddsXmlParserException
    {
      StaxThreddsXmlParserUtils.readNextEventCheckItIsStartElementWithExpectedName( reader, this.elementName );

      this.value = StaxThreddsXmlParserUtils.getCharacterContent( reader, this.elementName );

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

      DurationParser getNewParser() {
        return new DurationParser( this.elementName );
      }
    }
  }
}