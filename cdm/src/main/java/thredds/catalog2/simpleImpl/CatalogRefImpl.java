package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.*;
import thredds.catalog2.*;

import java.util.List;
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
  public List<String> getPropertyNames()
  {
    return null;
  }

  public String getPropertyValue( String name )
  {
    return null;
  }

  public CatalogBuilder getParentCatalogBuilder()
  {
    return null;
  }

  public DatasetBuilder getParentDatasetBuilder()
  {
    return null;
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    return null;
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    return null;
  }

  public DatasetNodeBuilder getDatasetNodeBuilderByName( String name )
  {
    return null;
  }

  public void setReference( URI reference )
  {
  }

  public boolean isFinished()
  {
    return false;
  }

  public CatalogRef finish()
  {
    return null;
  }

  public Property getProperty( String name )
  {
    return null;
  }

  public URI getReference()
  {
    return null;
  }
}
