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
class ProjectElementParser extends AbstractElementParser
{
  private final ThreddsMetadataBuilder parentBuilder;
  private ThreddsMetadataBuilder.ProjectNameBuilder selfBuilder;

  private ProjectElementParser( QName elementName,
                                XMLEventReader reader,
                                ThreddsBuilderFactory builderFactory,
                                ThreddsMetadataBuilder parentBuilder )
  {
    super( elementName, reader, builderFactory );
    this.parentBuilder = parentBuilder;
  }

  ThreddsBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();
    Attribute namingAuthAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.ControlledVocabType_Authority );
    String namingAuth = namingAuthAtt != null ? namingAuthAtt.getValue() : null;

    String name = StaxThreddsXmlParserUtils.getCharacterContent( this.reader,
                                                                 this.elementName );
    this.selfBuilder = this.parentBuilder.addProjectName( namingAuth, name);
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    String unexpectedElement = StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
    ThreddsXmlParserIssue issue = new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.ERROR, "Unrecognized element: " + unexpectedElement, this.selfBuilder, null);
    throw new ThreddsXmlParserException( issue);
  }

  void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
  }

  static class Factory
  {
    private QName elementName;

    Factory() {
      this.elementName = ThreddsMetadataElementNames.ProjectElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    ProjectElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       ThreddsMetadataBuilder parentBuilder )
    {
      return new ProjectElementParser( this.elementName, reader, builderFactory, parentBuilder );
    }
  }
}