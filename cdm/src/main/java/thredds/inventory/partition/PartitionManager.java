package thredds.inventory.partition;

import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionManagerRO;

import java.io.IOException;
import java.util.List;

/**
 * Manages time partitions
 *
 * @author caron
 * @since 11/11/13
 */
public interface PartitionManager extends CollectionManager {

  public Iterable<CollectionManagerRO> makePartitions() throws IOException;

}
