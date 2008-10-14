package thredds.catalog2.simpleImpl;

import thredds.catalog2.*;
import thredds.catalog2.builder.*;
import thredds.catalog2.simpleImpl.ServiceImpl;
import thredds.catalog.ServiceType;

import java.util.Date;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogBuilderFactoryImpl implements CatalogBuilderFactory
{
  public CatalogBuilder newCatalogBuilder( String name, URI docBaseUri, String version, Date expires, Date lastModified )
  {
    return new CatalogImpl( name, docBaseUri, version, expires, lastModified );
  }

  public CatalogBuilder newCatalogBuilder( Catalog catalog )
  {
//    if ( catalog instanceof CatalogImpl )
//    {
//      CatalogBuilder cb = (CatalogBuilder) catalog;
//      cb.unfinish();
//      return cb;
//    }
//    throw new IllegalArgumentException( "Given catalog not correct implementation for this CatalogBuilderFactory.");
    return null;
  }

  public ServiceBuilder newServiceBuilder( String name, ServiceType type, URI baseUri )
  {
    return new ServiceImpl( name, type, baseUri, null );
  }

  public ServiceBuilder newServiceBuilder( Service service )
  {
    return null;
  }

  public DatasetBuilder newDatasetBuilder( String name )
  {
    return new DatasetImpl( name, null, null );
  }

  public DatasetBuilder newDatasetBuilder( Dataset dataset )
  {
    return null;
  }

  public CatalogRefBuilder newCatalogRefBuilder( String name, URI reference )
  {
    return new CatalogRefImpl( name, reference, null, null );
  }

  public CatalogRefBuilder newCatalogRefBuilder( CatalogRef catRef )
  {
    return null;
  }

  public MetadataBuilder newMetadataBuilder()
  {
    return null;
  }

  public AccessBuilder newAccessBuilder()
  {
    return new AccessImpl();
  }

  public AccessBuilder newAccessBuilder( Access access )
  {
    return null;
  }
}