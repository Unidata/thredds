package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.ThreddsMetadataElementNames;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.builder.ThreddsMetadataBuilder;

import javax.xml.namespace.QName;
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
class VariableGroupElementParser extends AbstractElementParser
{
  private final ThreddsMetadataBuilder parentBuilder;

  private ThreddsMetadataBuilder.VariableGroupBuilder selfBuilder;

  private VariableElementParser.Factory varElemParserFactory;
  private VariableElementParser varElemParser;

  private VariableGroupElementParser( QName elementName,
                                      XMLEventReader reader,
                                      ThreddsBuilderFactory builderFactory,
                                      ThreddsBuilder parentBuilder )
  {
    super( elementName, reader, builderFactory);

    this.parentBuilder = (ThreddsMetadataBuilder) parentBuilder;

    this.varElemParserFactory = new VariableElementParser.Factory();
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    this.selfBuilder = this.parentBuilder.addVariableGroupBuilder();

    Attribute att = startElement.getAttributeByName( ThreddsMetadataElementNames.VariablesElement_vocabAuthorityId );
    if ( att != null )
      this.selfBuilder.setVocabularyAuthorityId(  att.getValue() );

    att = startElement.getAttributeByName( ThreddsMetadataElementNames.VariablesElement_vocabAuthorityUrl );
    if ( att != null)
      this.selfBuilder.setVocabularyAuthorityUrl( att.getValue());
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.varElemParserFactory.isEventMyStartElement( startElement ))
    {
      this.varElemParser = this.varElemParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
      this.varElemParser.parseElement();
    }
  }

  void postProcessingAfterEndElement() throws ThreddsXmlParserException {
    return;
  }

  ThreddsBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  static class Factory
  {
    private QName elementName;

    Factory() {
      this.elementName = ThreddsMetadataElementNames.VariablesElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    VariableGroupElementParser getNewParser( XMLEventReader reader,
                                             ThreddsBuilderFactory builderFactory,
                                             ThreddsBuilder parentBuilder)
    {
      return new VariableGroupElementParser( this.elementName, reader, builderFactory, parentBuilder );
    }
  }

  static class VariableElementParser
  {
    private final QName elementName;
    private final XMLEventReader reader;
    private final ThreddsBuilderFactory builderFactory;
    private final ThreddsMetadataBuilder.VariableGroupBuilder parentBuilder;
    private ThreddsMetadataBuilder.VariableBuilder selfBuilder;

    private VariableElementParser( QName elementName,
                                   XMLEventReader reader,
                                   ThreddsBuilderFactory builderFactory,
                                   ThreddsBuilder parentBuilder ) {
      this.elementName = elementName;
      this.reader = reader;
      this.builderFactory = builderFactory;
      this.parentBuilder = (ThreddsMetadataBuilder.VariableGroupBuilder) parentBuilder;
    }

    void parseElement()
            throws ThreddsXmlParserException
    {
      StartElement startElement
              = StaxThreddsXmlParserUtils.readNextEventCheckItIsStartElementWithExpectedName( this.reader, this.elementName );

      Attribute att = startElement.getAttributeByName( ThreddsMetadataElementNames.VariablesElement_VariableElement_name );
      String name = att != null ? att.getValue() : null;

      att = startElement.getAttributeByName( ThreddsMetadataElementNames.VariablesElement_VariableElement_units );
      String units = att != null ? att.getValue() : null;

      att = startElement.getAttributeByName( ThreddsMetadataElementNames.VariablesElement_VariableElement_vocabularyId );
      String vocabularyId = att != null ? att.getValue() : null;

      att = startElement.getAttributeByName( ThreddsMetadataElementNames.VariablesElement_VariableElement_vocabularyName );
      String vocabularyName = att != null ? att.getValue() : null;

      String description = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, this.elementName );

      this.selfBuilder = parentBuilder.addVariableBuilder( name, description, units, vocabularyId, vocabularyName );

      StaxThreddsXmlParserUtils.readNextEventCheckItIsEndElementWithExpectedName( this.reader, this.elementName );
    }

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.VariablesElement_VariableElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      VariableElementParser getNewParser( XMLEventReader reader,
                                          ThreddsBuilderFactory builderFactory,
                                          ThreddsBuilder parentBuilder) {
        return new VariableElementParser( this.elementName, reader, builderFactory, parentBuilder );
      }
    }
  }
}

