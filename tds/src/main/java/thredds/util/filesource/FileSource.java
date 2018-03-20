/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util.filesource;

import java.io.File;

/**
 * Represent a source for java.io.Files.
 *
 * @author edavis
 * @since 4.0
 */
public interface FileSource
{
  /**
   * Return the File, if it exists, that the given path represents for the
   * FileSource implementation in use.
   *
   * Different implementations of FileSource can have different restrictions
   * on the set of recognized paths. For instance, some implementations may
   * only be able to handle paths that reference Files under a particular
   * root directory.
   *
   * @param path the path to the desired File.
   * @return the File represented by the path or null.
   */
  File getFile( String path);
}
