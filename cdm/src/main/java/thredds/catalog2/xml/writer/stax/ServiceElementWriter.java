package thredds.catalog2.xml.writer.stax;

import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.xml.CatalogNamespace;
import thredds.catalog2.xml.ServiceElementUtils;
import thredds.catalog2.Service;
import thredds.catalog2.Property;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceElementWriter implements AbstractElementWriter
{
  public ServiceElementWriter() {}

  public void writeElement( Service service, XMLStreamWriter writer )
          throws ThreddsXmlWriterException
  {
    try
    {
      writer.writeStartElement( ServiceElementUtils.ELEMENT_NAME );
      writer.writeNamespace( CatalogNamespace.CATALOG_1_0.getStandardPrefix(),
                             CatalogNamespace.CATALOG_1_0.getNamespaceUri() );
      writer.writeNamespace( CatalogNamespace.XLINK.getStandardPrefix(),
                             CatalogNamespace.XLINK.getNamespaceUri() );
      writer.writeAttribute( ServiceElementUtils.NAME_ATTRIBUTE_NAME, service.getName() );
      writer.writeAttribute( ServiceElementUtils.SERVICE_TYPE_ATTRIBUTE_NAME, service.getType().toString() );
      writer.writeAttribute( ServiceElementUtils.BASE_ATTRIBUTE_NAME, service.getBaseUri().toString() );

      if ( service.getSuffix() != null )
        writer.writeAttribute( ServiceElementUtils.SUFFIX_ATTRIBUTE_NAME, service.getSuffix() );
      if ( service.getDescription() != null )
        writer.writeAttribute( ServiceElementUtils.DESCRIPTION_ATTRIBUTE_NAME, service.getDescription() );

      for ( Property curProperty : service.getProperties() )
      {
        new PropertyElementWriter().writeElement( curProperty, writer );
      }
      for ( Service curService : service.getServices() )
      {
        new ServiceElementWriter().writeElement( curService, writer );
      }
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlWriterException( "Failed while writing to XMLStreamWriter.", e );
    }
  }

}
