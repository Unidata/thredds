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
  @Override
  public CatalogBuilder newCatalogBuilder( String name, URI docBaseUri, String version, Date expires, Date lastModified )
  {
    return new CatalogImpl( name, docBaseUri, version, expires, lastModified );
  }

  @Override
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

  @Override
  public ServiceBuilder newServiceBuilder( String name, ServiceType type, URI baseUri )
  {
    return new ServiceImpl( name, type, baseUri, null, null );
  }

  @Override
  public ServiceBuilder newServiceBuilder( Service service )
  {
    return null;
  }

  @Override
  public DatasetBuilder newDatasetBuilder( String name )
  {
    return new DatasetImpl( name, null, null );
  }

  @Override
  public DatasetBuilder newDatasetBuilder( Dataset dataset )
  {
    return null;
  }

  @Override
  public DatasetAliasBuilder newDatasetAliasBuilder( String name, DatasetNodeBuilder dsToAlias )
  {
    return new DatasetAliasImpl( name, dsToAlias, null, null );
  }

  @Override
  public DatasetAliasBuilder newDatasetAliasBuilder( DatasetAlias dsAlias )
  {
    return null;
  }

  @Override
  public CatalogRefBuilder newCatalogRefBuilder( String name, URI reference )
  {
    return new CatalogRefImpl( name, reference, null, null );
  }

  @Override
  public CatalogRefBuilder newCatalogRefBuilder( CatalogRef catRef )
  {
    return null;
  }

  @Override
  public MetadataBuilder newMetadataBuilder()
  {
    return null;
  }

  @Override
  public AccessBuilder newAccessBuilder( ServiceImpl service, String urlPath )
  {
    return new AccessImpl( service, urlPath );
  }

  @Override
  public AccessBuilder newAccessBuilder( Access access )
  {
    return null;
  }
}