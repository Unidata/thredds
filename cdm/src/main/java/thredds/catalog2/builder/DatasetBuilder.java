package thredds.catalog2.builder;

import thredds.catalog2.Dataset;
import thredds.catalog2.explorer.DatasetExplorer;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetBuilder extends DatasetNodeBuilder
{
  public void setAlias( String alias );
  public void setAlias( Dataset aliasDataset );

  public AccessBuilder addAccess();

  public void finish();
}
