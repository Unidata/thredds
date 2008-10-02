package thredds.catalog2.builder;

import thredds.catalog2.CatalogRef;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogRefBuilder
        extends DatasetNodeBuilder
{
  public URI getReference();
  public void setReference( URI reference );

  public boolean isFinished();
  public CatalogRef finish() throws BuildException;
}
