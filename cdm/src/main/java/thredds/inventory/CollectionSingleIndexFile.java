/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import org.slf4j.Logger;

/**
 * Collection from a Single Index File.
 * Used by GribCdmIndex.updateGribCollectionFromPCollection() to distinguish from regular file
 *
 * @author caron
 * @since 2/6/14
 */
public class CollectionSingleIndexFile extends CollectionSingleFile {

  public CollectionSingleIndexFile(MFile file, Logger logger) {
    super(file, logger);
  }

  @Override
  public String getIndexFilename(String suffix) {
    return mfiles.get(0).getPath();
  }
}
