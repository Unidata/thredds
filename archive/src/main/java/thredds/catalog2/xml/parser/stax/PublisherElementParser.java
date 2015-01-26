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
class PublisherElementParser extends AbstractElementParser
{
  private final ThreddsMetadataBuilder parentBuilder;
  private ThreddsMetadataBuilder.ContributorBuilder selfBuilder;

  private final CreatorElementParser.NameElementParser.Factory nameElemParserFactory;
  private final CreatorElementParser.ContactElementParser.Factory contactElemParserFactory;

  private PublisherElementParser( QName elementName,
                                  XMLEventReader reader,
                                  ThreddsBuilderFactory builderFactory,
                                  ThreddsMetadataBuilder parentBuilder )
  {
    super( elementName, reader, builderFactory );
    this.parentBuilder = parentBuilder;

    this.nameElemParserFactory = new CreatorElementParser.NameElementParser.Factory();
    this.contactElemParserFactory = new CreatorElementParser.ContactElementParser.Factory();
  }

  ThreddsBuilder getSelfBuilder() {
    return null;
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    this.getNextEventIfStartElementIsMine();

    this.selfBuilder = this.parentBuilder.addPublisher();
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.nameElemParserFactory.isEventMyStartElement( startElement ) )
    {
      CreatorElementParser.NameElementParser elementParser
              = this.nameElemParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
      elementParser.parse();
    }
    else if ( this.contactElemParserFactory.isEventMyStartElement( startElement ) )
    {
      CreatorElementParser.ContactElementParser elementParser
              = this.contactElemParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
      elementParser.parse();
    }
    else
    {
      String unexpectedElement = StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
      ThreddsXmlParserIssue issue = new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.ERROR, "Unrecognized element: " + unexpectedElement, this.selfBuilder, null);
      throw new ThreddsXmlParserException( issue);
    }

  }

  void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
  }

  static class Factory
  {
    private QName elementName;

    Factory() {
      this.elementName = ThreddsMetadataElementNames.PublisherElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    PublisherElementParser getNewParser( XMLEventReader reader,
                                         ThreddsBuilderFactory builderFactory,
                                         ThreddsMetadataBuilder parentBuilder )
    {
      return new PublisherElementParser( this.elementName, reader, builderFactory, parentBuilder );
    }
  }
}