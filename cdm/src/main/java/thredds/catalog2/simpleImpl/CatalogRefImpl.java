package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.*;
import thredds.catalog2.*;

import java.net.URI;

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

  @Override
  public void setReference( URI reference )
  {
    if ( this.finished ) throw new IllegalStateException( "This CatalogRefBuilder has been finished().");
    if ( reference == null ) throw new IllegalArgumentException( "CatalogRef reference URI must not be null." );
    this.reference = reference;
  }

  @Override
  public URI getReference()
  {
    return this.reference;
  }

  @Override
  public boolean isFinished()
  {
    return this.finished;
  }

  @Override
  public CatalogRef finish()
  {
    super.finish();
    this.finished = true;
    return this;
  }
}
