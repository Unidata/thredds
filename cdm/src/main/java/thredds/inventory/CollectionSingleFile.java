/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A CollectionManager consisting of a single file
 *
 * @author caron
 * @since 12/23/11
 */
public class CollectionSingleFile extends CollectionList {

  public CollectionSingleFile(MFile file, org.slf4j.Logger logger) {
    super(file.getName(), logger);
    mfiles.add(file);
    Path p = Paths.get(file.getPath());
    if (p.getParent() != null)
      this.root = p.getParent().toString();
    else
      this.root = System.getProperty("user.dir");

    this.lastModified = file.getLastModified();
  }

}
