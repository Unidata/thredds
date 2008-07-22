package thredds.catalog2.builder;

import thredds.catalog2.explorer.DatasetNodeExplorer;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetNodeBuilder extends DatasetNodeExplorer
{
  public void setId( String id );

  public void setName( String name );

  public void addProperty( String name, String value );

  public MetadataBuilder addMetadata();

  public DatasetBuilder addDataset();
  public DatasetBuilder addDataset( int index );

  public CatalogRefBuilder addCatalogRef();
  public CatalogRefBuilder addCatalogRef( int index );
}
