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
  public PropertyElementWriter() {}

  public void writeElement( Property property, XMLStreamWriter writer )
          throws ThreddsXmlWriterException
  {
    try
    {
      writer.writeStartElement( PropertyElementUtils.ELEMENT_NAME );
      writer.writeNamespace( CatalogNamespace.CATALOG_1_0.getStandardPrefix(),
                             CatalogNamespace.CATALOG_1_0.getNamespaceUri() );
      writer.writeNamespace( CatalogNamespace.XLINK.getStandardPrefix(),
                             CatalogNamespace.XLINK.getNamespaceUri() );
      writer.writeAttribute( PropertyElementUtils.NAME_ATTRIBUTE_NAME, property.getName() );
      writer.writeAttribute( PropertyElementUtils.VALUE_ATTRIBUTE_NAME, property.getValue() );
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlWriterException( "Failed while writing to XMLStreamWriter.", e );
    }
  }
}
