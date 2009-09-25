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

  CreatorElementParser( XMLEventReader reader,
                        ThreddsBuilderFactory builderFactory,
                        ThreddsMetadataBuilder parentBuilder )
          throws ThreddsXmlParserException
  {
    super( ThreddsMetadataElementNames.CreatorElement, reader, builderFactory );
    this.parentBuilder = parentBuilder;
  }

  static boolean isSelfElementStatic( XMLEvent event )
  {
    return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, ThreddsMetadataElementNames.CreatorElement );
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

    this.selfBuilder = this.parentBuilder.addCreator();
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( NameElementParser.isSelfElementStatic( startElement ) )
    {
      NameElementParser elementParser = new NameElementParser( this.reader,
                                                               this.builderFactory,
                                                               this.selfBuilder );
      elementParser.parse();
    }
    else if ( ContactElementParser.isSelfElementStatic( startElement ) )
    {
      ContactElementParser elementParser = new ContactElementParser( this.reader,
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

    NameElementParser( XMLEventReader reader,
                       ThreddsBuilderFactory builderFactory,
                       ThreddsBuilder parentBuilder )
            throws ThreddsXmlParserException
    {
      super( ThreddsMetadataElementNames.CreatorElement_NameElement, reader, builderFactory );
      this.parentBuilder = (ThreddsMetadataBuilder.ContributorBuilder) parentBuilder;
    }

    static boolean isSelfElementStatic( XMLEvent event )
    {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, ThreddsMetadataElementNames.CreatorElement_NameElement );
    }

    boolean isSelfElement( XMLEvent event )
    {
      return isSelfElementStatic( event );
    }

    ThreddsBuilder getSelfBuilder()
    {
      return null;
    }

    void parseStartElement() throws ThreddsXmlParserException
    {
      StartElement startElement = this.getNextEventIfStartElementIsMine();

      Attribute namingAuthAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.CreatorElement_NameElement_NamingAuthority );
      String namingAuth = namingAuthAtt != null ? namingAuthAtt.getValue() : null;

      String name = StaxThreddsXmlParserUtils.getCharacterContent( this.reader,
                                                                   ThreddsMetadataElementNames.CreatorElement_NameElement );

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
  }

  /**
   * Parser for child element "contact".
   */
  static class ContactElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder.ContributorBuilder parentBuilder;

    ContactElementParser( XMLEventReader reader,
                       ThreddsBuilderFactory builderFactory,
                       ThreddsBuilder parentBuilder )
            throws ThreddsXmlParserException
    {
      super( ThreddsMetadataElementNames.CreatorElement_ContactElement, reader, builderFactory );
      this.parentBuilder = (ThreddsMetadataBuilder.ContributorBuilder) parentBuilder;
    }

    static boolean isSelfElementStatic( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, ThreddsMetadataElementNames.CreatorElement_ContactElement );
    }

    boolean isSelfElement( XMLEvent event ) {
      return isSelfElementStatic( event );
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
  }
}
