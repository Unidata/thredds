/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.IOException;
import javax.annotation.Nullable;

/**
 * An abstraction for java.io.File / java.nio.file.Path
 *
 * @author caron
 * @since Jun 30, 2009
 */
public interface MFile extends Comparable<MFile> {

  /**
   * Get time of last modification at the time the MFile object was created
   * @return time of last modification in Unix time (msecs since reference), or -1 if unknown
   */
  long getLastModified();

  /**
   * Size of file in bytes
   * @return  Size of file in bytes or -1 if unknown
   */
  long getLength();

  boolean isDirectory();

  /**
   * Get full path name, replace \\ with /
   * @return full path name
   */
  String getPath();

  /**
   * The name is the <em>farthest</em> element from the root in the directory hierarchy.
   * @return the file name
   */
  String getName();

  /**
   * Get the parent of this
   * @return  the parent or null
   * @throws IOException
   */
  MFile getParent() throws IOException;

  int compareTo(MFile o);

  // does not survive serialization ??
  @Nullable Object getAuxInfo();
  void setAuxInfo(Object info);
}
