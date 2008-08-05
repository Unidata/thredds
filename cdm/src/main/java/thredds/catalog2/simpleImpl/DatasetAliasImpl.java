package thredds.catalog2.simpleImpl;

import thredds.catalog2.DatasetAlias;
import thredds.catalog2.DatasetNode;
import thredds.catalog2.builder.DatasetAliasBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.builder.CatalogBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetAliasImpl
        extends DatasetNodeImpl
        implements DatasetAlias, DatasetAliasBuilder
{
  private DatasetNodeImpl alias;

  private boolean finished = false;

  protected DatasetAliasImpl( String name, DatasetNodeBuilder aliasRef, CatalogBuilder parentCatalog, DatasetNodeBuilder parent )
  {
    super( name, parentCatalog, parent);
    if ( aliasRef == null ) throw new IllegalArgumentException( "Alias may not be null.");
    this.alias = (DatasetNodeImpl) aliasRef;
  }

  public void setAlias( DatasetNodeBuilder aliasRef )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetAliasBuilder has been finished().");
    if ( aliasRef == null ) throw new IllegalArgumentException( "Alias may not be null." );
    this.alias = (DatasetNodeImpl) aliasRef;
  }

  public DatasetNode getAlias()
  {
    if ( ! this.finished ) throw new IllegalStateException( "This DatasetAlias has escaped its DatasetAliasBuilder before being finished().");
    return this.alias;
  }

  public DatasetBuilder getAliasBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetAliasBuilder has been finished()." );
    return (DatasetBuilder) this.alias;
  }

  public boolean isFinished()
  {
    return this.finished;
  }
  
  public DatasetAlias finish()
  {
    if ( this.finished )
      return this;

    super.finish();
    this.finished = true;
    return this;
  }
}
