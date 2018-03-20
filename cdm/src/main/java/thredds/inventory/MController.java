/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.IOException;
import java.util.Iterator;

/**
 * Inventory Management Controller
 *
 * @author caron
 * @since Jun 25, 2009
 */
public interface MController {

  /**
   * Returns all leaves in collection, recursing into subdirectories.
   * @param mc defines the collection to scan
   * @param recheck if false, may use cached results. otherwise must sync with File OS
   * @return iterator over Mfiles, or null if collection does not exist
   */
  public Iterator<MFile> getInventoryAll(CollectionConfig mc, boolean recheck);

  /**
   * Returns all leaves in top collection, not recursing into subdirectories.
   * @param mc defines the collection to scan
   * @param recheck if false, may use cached results. otherwise must sync with File OS
   * @return iterator over Mfiles, or null if collection does not exist
   */
  public Iterator<MFile> getInventoryTop(CollectionConfig mc, boolean recheck) throws IOException;

  /**
   * Returns all subdirectories in top collection.
   * @param mc defines the collection to scan
   * @param recheck if false, may use cached results. otherwise must sync with File OS
   * @return iterator over Mfiles, or null if collection does not exist
   */
  public Iterator<MFile> getSubdirs(CollectionConfig mc, boolean recheck);

  public void close();

}
