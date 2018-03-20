/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

/**
 * Filter on MFiles
 *
 * @author caron
 * @since Jun 26, 2009
 */
public interface MFileFilter {
  /**
   * Tests if a specified MFile should be included in a file collection.
   *
   * @param mfile the MFile
   * @return <code>true</code> if the mfile should be included in the file collection; <code>false</code> otherwise.
   */
  boolean accept(MFile mfile);
}
