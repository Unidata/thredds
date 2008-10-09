package thredds.catalog2.simpleImpl;

import thredds.catalog2.DatasetAlias;
import thredds.catalog2.DatasetNode;
import thredds.catalog2.builder.*;

import java.util.List;
import java.util.ArrayList;

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
    if ( ! this.finished )
      throw new IllegalStateException( "This DatasetAlias has escaped its DatasetAliasBuilder before build() was called.");
    return this.alias;
  }

  public DatasetBuilder getAliasBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetAliasBuilder has been finished()." );
    return (DatasetBuilder) this.alias;
  }

  @Override
  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.finished )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();
    super.isBuildable( issues );

    this.alias.isBuildable( localIssues );

    // ToDo Check any invariants.

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  @Override
  public DatasetAlias build() throws BuilderException
  {
    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !isBuildable( issues ) )
      throw new BuilderException( issues );

    this.finished = true;
    return this;
  }
}
