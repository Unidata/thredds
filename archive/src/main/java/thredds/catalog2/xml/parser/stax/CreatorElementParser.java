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
import javax.xml.stream.events.Attribute;
import javax.xml.namespace.QName;

/**
 * _more_
*
* @author edavis
* @since 4.0
*/
class CreatorElementParser extends AbstractElementParser
{
  private final ThreddsMetadataBuilder parentBuilder;
  private ThreddsMetadataBuilder.ContributorBuilder selfBuilder;

  private final NameElementParser.Factory nameElemParserFactory;
  private final ContactElementParser.Factory contactElemParserFactory;

  private CreatorElementParser( QName elementName,
                                XMLEventReader reader,
                                ThreddsBuilderFactory builderFactory,
                                ThreddsMetadataBuilder parentBuilder )
  {
    super( elementName, reader, builderFactory );
    this.parentBuilder = parentBuilder;

    this.nameElemParserFactory = new NameElementParser.Factory();
    this.contactElemParserFactory = new ContactElementParser.Factory();
  }

  ThreddsBuilder getSelfBuilder() {
    return null;
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    this.getNextEventIfStartElementIsMine();

    this.selfBuilder = this.parentBuilder.addCreator();
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.nameElemParserFactory.isEventMyStartElement( startElement ) )
    {
      NameElementParser elementParser = this.nameElemParserFactory.getNewParser( this.reader,
                                                                                 this.builderFactory,
                                                                                 this.selfBuilder );
      elementParser.parse();
    }
    else if ( this.contactElemParserFactory.isEventMyStartElement( startElement ) )
    {
      ContactElementParser elementParser = this.contactElemParserFactory.getNewParser( this.reader,
                                                                                       this.builderFactory,
                                                                                       this.selfBuilder );
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

  static class NameElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder.ContributorBuilder parentBuilder;

    private NameElementParser( QName elementName,
                               XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory,
                               ThreddsBuilder parentBuilder )
    {
      super( elementName, reader, builderFactory );
      this.parentBuilder = (ThreddsMetadataBuilder.ContributorBuilder) parentBuilder;
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement() throws ThreddsXmlParserException
    {
      StartElement startElement = this.getNextEventIfStartElementIsMine();

      Attribute namingAuthAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.CreatorElement_NameElement_NamingAuthority );
      String namingAuth = namingAuthAtt != null ? namingAuthAtt.getValue() : null;

      String name = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, this.elementName );

      this.parentBuilder.setName( name );
      this.parentBuilder.setNamingAuthority( namingAuth );
    }

    void handleChildStartElement() throws ThreddsXmlParserException {
      String unexpectedElement = StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
      ThreddsXmlParserIssue issue = new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.ERROR, "Unrecognized element: " + unexpectedElement, this.parentBuilder, null );
      throw new ThreddsXmlParserException( issue );
    }

    void postProcessingAfterEndElement() throws ThreddsXmlParserException {
      return;
    }

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.CreatorElement_NameElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      NameElementParser getNewParser( XMLEventReader reader,
                                         ThreddsBuilderFactory builderFactory,
                                         ThreddsBuilder parentBuilder )
      {
        return new NameElementParser( this.elementName, reader, builderFactory, parentBuilder );
      }
    }
  }

  /**
   * Parser for child element "contact".
   */
  static class ContactElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder.ContributorBuilder parentBuilder;

    ContactElementParser( QName elementName,
                          XMLEventReader reader,
                          ThreddsBuilderFactory builderFactory,
                          ThreddsBuilder parentBuilder )
    {
      super( elementName, reader, builderFactory );
      this.parentBuilder = (ThreddsMetadataBuilder.ContributorBuilder) parentBuilder;
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement() throws ThreddsXmlParserException
    {
      StartElement startElement = this.getNextEventIfStartElementIsMine();

      Attribute emailAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.CreatorElement_ContactElement_Email );
      String emailAuth = emailAtt != null ? emailAtt.getValue() : null;
      Attribute urlAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.CreatorElement_ContactElement_Url );
      String url = urlAtt != null ? urlAtt.getValue() : null;

      this.parentBuilder.setEmail( emailAuth );
      this.parentBuilder.setWebPage( url );
    }

    void handleChildStartElement() throws ThreddsXmlParserException {
      String unexpectedElement = StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
      ThreddsXmlParserIssue issue = new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.ERROR, "Unrecognized element: " + unexpectedElement, this.parentBuilder, null );
      throw new ThreddsXmlParserException( issue );
    }

    void postProcessingAfterEndElement() throws ThreddsXmlParserException {
      return;
    }

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.CreatorElement_ContactElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      ContactElementParser getNewParser( XMLEventReader reader,
                                         ThreddsBuilderFactory builderFactory,
                                         ThreddsBuilder parentBuilder )
      {
        return new ContactElementParser( this.elementName, reader, builderFactory, parentBuilder );
      }
    }
  }

  static class Factory
  {
    private QName elementName;

    Factory() {
      this.elementName = ThreddsMetadataElementNames.CreatorElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    CreatorElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       ThreddsMetadataBuilder parentBuilder )
    {
      return new CreatorElementParser( this.elementName, reader, builderFactory, parentBuilder );
    }
  }
}
