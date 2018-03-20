/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.partition;

import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;

import java.io.IOException;

/**
 * Manages time partitions
 *
 * @author caron
 * @since 11/11/13
 */
public interface PartitionManager extends MCollection {

  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException;

  public void removePartition( MCollection partition);

}
