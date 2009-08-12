package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.names.CatalogElementNames;
import thredds.catalog2.xml.names.DatasetElementNames;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class ElementParserFactory
{
  //private Map<QName,>
  ElementParserFactory() {}
  // ToDo Need to do some registration of elements



  AbstractElementParser getDocRootElementParser(
          String baseDocUri, XMLEventReader reader,
          ThreddsBuilderFactory factory
  )
          throws ThreddsXmlParserException,
                 XMLStreamException
  {
    if ( reader == null )
      throw new IllegalArgumentException( "XML event reader may not be null." );
    if ( factory == null )
      throw new IllegalArgumentException( "ThreddsBuilder Factory may not be null." );
    if ( !reader.hasNext() )
      throw new IllegalArgumentException( "XML event reader must have a next event." );

    XMLEvent event = reader.peek();
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "XML event reader's first event must be a start element." );

    StartElement startElem = event.asStartElement();
    if ( startElem.getName().equals( CatalogElementNames.CatalogElement ))
      return new CatalogElementParser( baseDocUri, reader, factory );
//    else if ( startElem.getName().equals( DatasetElementNames.DatasetElement ))
//      return new DatasetElementParser( baseDocUri, reader, factory );
    else
      throw new ThreddsXmlParserException( "");
  }

  AbstractElementParser getElementParser(
          ThreddsBuilder parentBuilder,
          XMLEventReader reader )
          throws ThreddsXmlParserException,
                 XMLStreamException
  {
    if ( reader == null )
      throw new IllegalArgumentException( "XML event reader may not be null." );
    if ( parentBuilder == null )
      throw new IllegalArgumentException( "Parent ThreddsBuilder may not be null." );
    if ( !reader.hasNext() )
      throw new IllegalArgumentException( "XML event reader must have a next event." );

    XMLEvent event = reader.peek();
    if ( ! event.isStartElement() )
      throw new IllegalArgumentException( "XML event reader's first event must be a start element." );
    StartElement startElem = event.asStartElement();

    return null;
  }
}
