package thredds.catalog2.xml.writer.stax;

import thredds.catalog2.Catalog;
import thredds.catalog2.Service;
import thredds.catalog2.Property;
import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.xml.util.CatalogElementUtils;
import thredds.catalog2.xml.util.CatalogNamespace;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

import ucar.nc2.units.DateFormatter;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogElementWriter implements AbstractElementWriter
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  // ToDo How wire catalog elements together?
//  public static enum CatalogElementWriterFactory implements AbstractElementWriterFactory
//  {
//    SINGLETON;
//
//    private List<AbstractElementWriterFactory> factories;
//
//    CatalogElementWriterFactory()
//    {
//      this.factories = Collections.emptyList();
//    }
//
//    public void setElementWriterFactoryList( List<AbstractElementWriterFactory> factoryList )
//    {
//      this.factories = factoryList;
//    }
//  }

  public CatalogElementWriter() {}

  public void writeElement( Catalog catalog, XMLStreamWriter writer, int nestLevel )
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
      
      boolean isEmptyElement = catalog.getServices().isEmpty()
                               && catalog.getProperties().isEmpty()
                               && catalog.getDatasets().isEmpty();
      if ( isEmptyElement )
        writer.writeEmptyElement( CatalogElementUtils.ELEMENT_NAME );
      else
        writer.writeStartElement( CatalogElementUtils.ELEMENT_NAME );
      if ( nestLevel == 0 )
      {
        writer.writeNamespace( CatalogNamespace.CATALOG_1_0.getStandardPrefix(),
                               CatalogNamespace.CATALOG_1_0.getNamespaceUri() );
        writer.writeNamespace( CatalogNamespace.XLINK.getStandardPrefix(),
                               CatalogNamespace.XLINK.getNamespaceUri() );
      }
      if ( catalog.getName() != null )
        writer.writeAttribute( CatalogElementUtils.NAME_ATTRIBUTE_NAME, catalog.getName() );
      if ( catalog.getVersion() != null )
        writer.writeAttribute( CatalogElementUtils.VERSION_ATTRIBUTE_NAME, catalog.getVersion() );

      DateFormatter df = new DateFormatter();
      if ( catalog.getExpires() != null )
      {
        writer.writeAttribute( CatalogElementUtils.EXPIRES_ATTRIBUTE_NAME,
                               df.toDateTimeStringISO( catalog.getExpires() ));
      }
      if ( catalog.getLastModified() != null )
      {
        writer.writeAttribute( CatalogElementUtils.LAST_MODIFIED_ATTRIBUTE_NAME,
                               df.toDateTimeStringISO( catalog.getLastModified() ));
      }
      writer.writeCharacters( "\n" );
      for ( Service curService : catalog.getServices() )
        new ServiceElementWriter().writeElement( curService, writer, nestLevel + 1 );
      for ( Property curProperty : catalog.getProperties() )
        new PropertyElementWriter().writeElement( curProperty, writer, nestLevel + 1 );

      if ( ! isEmptyElement )
      {
        writer.writeCharacters( indentString );
        writer.writeEndElement();
        writer.writeCharacters( "\n" );
      }
      if ( nestLevel == 0)
        writer.writeEndDocument();
      writer.flush();
      if ( nestLevel == 0 )
        writer.close();
    }
    catch ( XMLStreamException e )
    {
      log.error( "writeElement(): Failed while writing to XMLStreamWriter: " + e.getMessage());
      throw new ThreddsXmlWriterException( "Failed while writing to XMLStreamWriter: " + e.getMessage(), e );
    }
  }
}
