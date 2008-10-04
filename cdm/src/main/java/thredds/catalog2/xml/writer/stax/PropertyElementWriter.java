package thredds.catalog2.xml.writer.stax;

import thredds.catalog2.Property;
import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.PropertyElementUtils;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class PropertyElementWriter implements AbstractElementWriter
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  public PropertyElementWriter() {}

  public void writeElement( Property property, XMLStreamWriter writer, boolean isDocRoot )
          throws ThreddsXmlWriterException
  {
    try
    {
      if ( isDocRoot )
      {
        writer.writeStartDocument();
        writer.writeCharacters( "\n" );
      }
      writer.writeEmptyElement( PropertyElementUtils.ELEMENT_NAME );
      if ( isDocRoot )
      {
        writer.writeNamespace( CatalogNamespace.CATALOG_1_0.getStandardPrefix(),
                               CatalogNamespace.CATALOG_1_0.getNamespaceUri() );
        writer.writeNamespace( CatalogNamespace.XLINK.getStandardPrefix(),
                               CatalogNamespace.XLINK.getNamespaceUri() );
      }
      writer.writeAttribute( PropertyElementUtils.NAME_ATTRIBUTE_NAME, property.getName() );
      writer.writeAttribute( PropertyElementUtils.VALUE_ATTRIBUTE_NAME, property.getValue() );

      if ( isDocRoot )
        writer.writeEndDocument();
      writer.flush();
      if ( isDocRoot )
        writer.close();
    }
    catch ( XMLStreamException e )
    {
      log.error( "writeElement(): Failed while writing to XMLStreamWriter: " + e.getMessage());
      throw new ThreddsXmlWriterException( "Failed while writing to XMLStreamWriter: " + e.getMessage(), e );
    }
  }
}
