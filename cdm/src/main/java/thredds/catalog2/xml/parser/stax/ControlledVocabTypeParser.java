package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.ThreddsMetadataElementNames;

import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.XMLEventReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class ControlledVocabTypeParser
{
  private QName elementName;

  private String vocabAuth;
  private String value;

  private ControlledVocabTypeParser( QName elementName) {
    this.elementName = elementName;
  }

  String getVocabAuth() {
    return this.vocabAuth;
  }

  String getValue() {
    return this.value;
  }

  void parseElement( XMLEventReader reader )
          throws ThreddsXmlParserException
  {
    StartElement startElement = StaxThreddsXmlParserUtils.readNextEventCheckItIsStartElementWithExpectedName( reader, this.elementName );

    Attribute vocabAuthAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.ControlledVocabType_Authority );
    vocabAuth = vocabAuthAtt != null ? vocabAuthAtt.getValue() : null;

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

    ControlledVocabTypeParser getNewDateTypeParser() {
      return new ControlledVocabTypeParser( this.elementName );
    }
  }
}