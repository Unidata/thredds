package thredds.catalog2.builder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetAliasBuilder
{
  public DatasetBuilder getAliasBuilder();

  public void setAlias( DatasetNodeBuilder aliasDataset );
}
