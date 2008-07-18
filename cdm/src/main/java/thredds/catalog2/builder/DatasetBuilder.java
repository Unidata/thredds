package thredds.catalog2.builder;

import thredds.catalog2.Dataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetBuilder extends Dataset
{
  public void setName( String name );
  public void setId( String id );
  public void setAlias( String alias );

  public void addProperty( String name, String value );
  public MetadataBuilder addMetadata();
  public AccessBuilder addAccess();

  public DatasetBuilder addDataset();
  public DatasetBuilder addDataset( int index );

  public CatalogRefBuilder addCatalogRef();
  public CatalogRefBuilder addCatalogRef( int index );

  public void finish();
}
