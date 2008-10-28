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
  private boolean isBuilt = false;

  protected CatalogRefImpl( String name, URI reference, CatalogImpl parentCatalog, DatasetNodeImpl parent )
  {
    super( name, parentCatalog, parent);
    if ( reference == null ) throw new IllegalArgumentException( "CatalogRef reference URI must not be null." );
    this.reference = reference;
  }

  public void setReference( URI reference )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This CatalogRefBuilder has been built.");
    if ( reference == null ) throw new IllegalArgumentException( "CatalogRef reference URI must not be null." );
    this.reference = reference;
  }

  public URI getReference()
  {
    return this.reference;
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  @Override
  public boolean isBuildable( List<BuilderIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderIssue> localIssues = new ArrayList<BuilderIssue>();
    super.isBuildable( issues );

    // ToDo Check any invariants.

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  public CatalogRef build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    List<BuilderIssue> issues = new ArrayList<BuilderIssue>();
    if ( !isBuildable( issues ) )
      throw new BuilderException( issues );

    super.build();

    this.isBuilt = true;
    return this;
  }
}
