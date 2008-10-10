package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.*;
import thredds.catalog2.*;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogRefImpl
        extends DatasetNodeImpl
        implements CatalogRef, CatalogRefBuilder
{
  private URI reference;
  private boolean finished = false;

  protected CatalogRefImpl( String name, URI reference, CatalogBuilder parentCatalog, DatasetNodeBuilder parent )
  {
    super( name, parentCatalog, parent);
    if ( reference == null ) throw new IllegalArgumentException( "CatalogRef reference URI must not be null." );
    this.reference = reference;
  }

  public void setReference( URI reference )
  {
    if ( this.finished ) throw new IllegalStateException( "This CatalogRefBuilder has been finished().");
    if ( reference == null ) throw new IllegalArgumentException( "CatalogRef reference URI must not be null." );
    this.reference = reference;
  }

  public URI getReference()
  {
    return this.reference;
  }

  @Override
  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.finished )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();
    super.isBuildable( issues );

    // ToDo Check any invariants.

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  @Override
  public CatalogRef build() throws BuilderException
  {
    if ( this.finished )
      return this;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !isBuildable( issues ) )
      throw new BuilderException( issues );

    super.build();

    this.finished = true;
    return this;
  }
}
