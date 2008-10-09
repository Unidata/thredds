package thredds.catalog2.builder;

import thredds.catalog2.DatasetAlias;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetAliasBuilder extends DatasetNodeBuilder
{
  public DatasetBuilder getAliasBuilder();

  public void setAlias( DatasetNodeBuilder aliasDataset );

  public boolean isBuildable( List<BuilderFinishIssue> issues );
  public DatasetAlias build() throws BuilderException;
}
