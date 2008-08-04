package thredds.catalog2.builder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetNodeBuilder
{
  public String getId();
  public void setId( String id );

  public String getName();
  public void setName( String name );

  public void addProperty( String name, String value );

  public MetadataBuilder addMetadata();

  public DatasetBuilder addDataset();
  public DatasetBuilder addDataset( int index );

  public CatalogRefBuilder addCatalogRef();
  public CatalogRefBuilder addCatalogRef( int index );
}
