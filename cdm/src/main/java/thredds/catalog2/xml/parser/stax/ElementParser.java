package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
interface ElementParser
{
  public boolean isSelfElement( XMLEvent event );

  public ThreddsBuilder parse()
          throws ThreddsXmlParserException;

  public void parseStartElement()
          throws ThreddsXmlParserException;

  public void handleChildStartElement()
          throws ThreddsXmlParserException;

  public void postProcessingAfterEndElement()
          throws ThreddsXmlParserException;

  public ThreddsBuilder getSelfBuilder();


  interface Factory
  {
    public boolean isEventMyStartElement( XMLEvent event);

    public ElementParser getNewParser( String docBaseUriString,
                                       XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory );

    public ElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       ThreddsBuilder parentBuilder );
  }
}
