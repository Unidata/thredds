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

  PublisherElementParser( XMLEventReader reader,
                          ThreddsBuilderFactory builderFactory,
                          ThreddsMetadataBuilder parentBuilder )
          throws ThreddsXmlParserException
  {
    super( ThreddsMetadataElementNames.PublisherElement, reader, builderFactory );
    this.parentBuilder = parentBuilder;
  }

  static boolean isSelfElementStatic( XMLEvent event )
  {
    return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, ThreddsMetadataElementNames.PublisherElement );
  }

  boolean isSelfElement( XMLEvent event )
  {
    return isSelfElementStatic( event );
  }

  ThreddsBuilder getSelfBuilder()
  {
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

    if ( CreatorElementParser.NameElementParser.isSelfElementStatic( startElement ) )
    {
      CreatorElementParser.NameElementParser elementParser
              = new CreatorElementParser.NameElementParser( this.reader, this.builderFactory, this.selfBuilder );
      elementParser.parse();
    }
    else if ( CreatorElementParser.ContactElementParser.isSelfElementStatic( startElement ) )
    {
      CreatorElementParser.ContactElementParser elementParser
              = new CreatorElementParser.ContactElementParser( this.reader, this.builderFactory, this.selfBuilder );
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
}