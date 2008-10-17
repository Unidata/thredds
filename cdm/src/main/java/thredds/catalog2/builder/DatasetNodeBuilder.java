package thredds.catalog2.builder;

import thredds.catalog2.DatasetNode;
import thredds.catalog2.ThreddsMetadata;

import java.util.List;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetNodeBuilder extends ThreddsBuilder
{
  public String getId();
  public void setId( String id );

  public String getIdAuthority();
  public void setIdAuthority( String idAuthority );

  public String getName();
  public void setName( String name );

  public void addProperty( String name, String value );
  public boolean removeProperty( String name );
  public List<String> getPropertyNames();
  public String getPropertyValue( String name );

  public void setThreddsMetadata( ThreddsMetadata threddsMetadata );
  public ThreddsMetadata getThreddsMetadata();

  public MetadataBuilder addMetadata();
  public boolean removeMetadata( MetadataBuilder metadataBuilder );
  public List<MetadataBuilder> getMetadataBuilders();

  public CatalogBuilder getParentCatalogBuilder();
  public DatasetNodeBuilder getParentDatasetBuilder();

  public boolean isCollection();

  public DatasetBuilder addDataset( String name );
  public CatalogRefBuilder addCatalogRef( String name, URI reference);

  public boolean removeDatasetNode( DatasetNodeBuilder datasetBuilder );

  public List<DatasetNodeBuilder> getDatasetNodeBuilders();
  public DatasetNodeBuilder getDatasetNodeBuilderById( String id );

  public boolean isBuildable( List<BuilderFinishIssue> issues );
  public DatasetNode build() throws BuilderException;
}
