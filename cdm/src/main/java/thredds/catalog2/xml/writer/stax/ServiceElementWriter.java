package thredds.catalog2.xml.writer.stax;

import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.ServiceElementUtils;
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
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  public ServiceElementWriter() {}

  public void writeElement( Service service, XMLStreamWriter writer, int nestLevel )
          throws ThreddsXmlWriterException
  {
    String indentString = StaxWriter.getIndentString( nestLevel );
    try
    {
      if ( nestLevel == 0 )
      {
        writer.writeStartDocument();
        writer.writeCharacters( "\n" );
      }
      else
        writer.writeCharacters( indentString );

      boolean isEmptyElement = service.getProperties().isEmpty() && service.getServices().isEmpty();
      if ( isEmptyElement )
        writer.writeEmptyElement( ServiceElementUtils.ELEMENT_NAME );
      else
        writer.writeStartElement( ServiceElementUtils.ELEMENT_NAME );
      if ( nestLevel == 0 )
      {
        writer.writeNamespace( CatalogNamespace.CATALOG_1_0.getStandardPrefix(),
                               CatalogNamespace.CATALOG_1_0.getNamespaceUri() );
        writer.writeNamespace( CatalogNamespace.XLINK.getStandardPrefix(),
                               CatalogNamespace.XLINK.getNamespaceUri() );
      }
      writer.writeAttribute( ServiceElementUtils.NAME_ATTRIBUTE_NAME, service.getName() );
      writer.writeAttribute( ServiceElementUtils.SERVICE_TYPE_ATTRIBUTE_NAME, service.getType().toString() );
      writer.writeAttribute( ServiceElementUtils.BASE_ATTRIBUTE_NAME, service.getBaseUri().toString() );

      if ( service.getSuffix() != null )
      {
        writer.writeAttribute( ServiceElementUtils.SUFFIX_ATTRIBUTE_NAME, service.getSuffix() );
      }
      if ( service.getDescription() != null )
      {
        writer.writeAttribute( ServiceElementUtils.DESCRIPTION_ATTRIBUTE_NAME, service.getDescription() );
      }

      writer.writeCharacters( "\n" );
      for ( Property curProperty : service.getProperties() )
        new PropertyElementWriter().writeElement( curProperty, writer, nestLevel + 1 );
      for ( Service curService : service.getServices() )
        new ServiceElementWriter().writeElement( curService, writer, nestLevel + 1 );

      if ( ! isEmptyElement )
      {
        writer.writeCharacters( indentString );
        writer.writeEndElement();
        writer.writeCharacters( "\n" );
      }
      if ( nestLevel == 0 )
        writer.writeEndDocument();
      writer.flush();
      if ( nestLevel == 0 )
        writer.close();
    }
    catch ( XMLStreamException e )
    {
      log.error( "writeElement(): Failed while writing to XMLStreamWriter: " + e.getMessage() );
      throw new ThreddsXmlWriterException( "Failed while writing to XMLStreamWriter: " + e.getMessage(), e );
    }
  }

}
