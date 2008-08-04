package thredds.catalog2.simpleImpl;

import thredds.catalog2.Dataset;
import thredds.catalog2.Access;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.AccessBuilder;
import thredds.catalog.ServiceType;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetImpl
        extends DatasetNodeImpl
        implements Dataset, DatasetBuilder
{
  public void setAlias( DatasetBuilder aliasDataset )
  {
  }

  public Dataset getAlias()
  {
    return null;
  }

  public DatasetBuilder getAliasBuilder()
  {
    return null;
  }

  public AccessBuilder addAccess()
  {
    return null;
  }

  public boolean isAccessible()
  {
    return false;
  }

  public List<Access> getAccesses()
  {
    return null;
  }

  public Access getAccessByType( ServiceType type )
  {
    return null;
  }

  public List<AccessBuilder> getAccessBuilders()
  {
    return null;
  }

  public AccessBuilder getAccessBuilderByType( ServiceType type )
  {
    return null;
  }

  public boolean isFinished()
  {
    return false;
  }

  public Dataset finish()
  {
    return null;
  }
}
