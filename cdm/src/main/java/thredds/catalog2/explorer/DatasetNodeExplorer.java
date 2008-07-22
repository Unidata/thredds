package thredds.catalog2.explorer;

import thredds.catalog2.DatasetNode;
import thredds.catalog2.Property;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetNodeExplorer extends DatasetNode
{
  public <T extends DatasetNode> T getDatasetByName( String name );

  public <T extends DatasetNode> T getDatasetById( String id );

  public Property getPropertyByName( String name );

}
