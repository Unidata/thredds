package thredds.catalog2.simpleImpl;

import thredds.catalog2.DatasetAlias;
import thredds.catalog2.DatasetNode;
import thredds.catalog2.builder.DatasetAliasBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.DatasetNodeBuilder;

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
  public void setAlias( DatasetNodeBuilder aliasDataset )
  {
  }

  public DatasetNode getAlias()
  {
    return null;
  }

  public DatasetBuilder getAliasBuilder()
  {
    return null;
  }
}
