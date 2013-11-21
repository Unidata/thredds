package thredds.inventory.partition;

import thredds.inventory.Collection;

import java.io.IOException;

/**
 * Manages time partitions
 *
 * @author caron
 * @since 11/11/13
 */
public interface PartitionManager extends Collection {

  public Iterable<Collection> makePartitions() throws IOException;

}
