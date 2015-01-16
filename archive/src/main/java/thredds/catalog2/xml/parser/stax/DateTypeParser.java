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
class DateTypeParser
{
  private QName elementName;

  private String format;
  private String type;
  private String value;

  private DateTypeParser( QName elementName) {
    this.elementName = elementName;
  }

  String getFormat() {
    return this.format;
  }

  String getType() {
    return this.type;
  }

  String getValue() {
    return this.value;
  }

  void parseElement( XMLEventReader reader )
          throws ThreddsXmlParserException
  {
    StartElement startElement = StaxThreddsXmlParserUtils.readNextEventCheckItIsStartElementWithExpectedName( reader, this.elementName);

    Attribute formatAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.DateType_Format );
    format = formatAtt != null ? formatAtt.getValue() : null;

    Attribute typeAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.DateType_Type );
    type = typeAtt != null ? typeAtt.getValue() : null;

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

    DateTypeParser getNewDateTypeParser() {
      return new DateTypeParser( this.elementName );
    }
  }
}
