package thredds.catalog2.xml.writer.stax;

import thredds.catalog2.Catalog;
import thredds.catalog2.Service;
import thredds.catalog2.Property;
import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.xml.CatalogElementUtils;
import thredds.catalog2.xml.CatalogNamespace;

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

  public void writeElement( Catalog catalog, XMLStreamWriter writer )
          throws ThreddsXmlWriterException
  {
    try
    {
      writer.writeStartElement( CatalogElementUtils.ELEMENT_NAME );
      writer.writeNamespace( CatalogNamespace.CATALOG_1_0.getStandardPrefix(),
                             CatalogNamespace.CATALOG_1_0.getNamespaceUri() );
      writer.writeNamespace( CatalogNamespace.XLINK.getStandardPrefix(),
                             CatalogNamespace.XLINK.getNamespaceUri() );
      writer.writeAttribute( CatalogElementUtils.NAME_ATTRIBUTE_NAME, catalog.getName() );
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

      for ( Service curService : catalog.getServices() )
      {
        new ServiceElementWriter().writeElement( curService, writer );
      }
      for ( Property curProperty : catalog.getProperties() )
      {
        new PropertyElementWriter().writeElement( curProperty, writer );
      }
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlWriterException( "Failed while writing to XMLStreamWriter.", e );
    }
  }
}
