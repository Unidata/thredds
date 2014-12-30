/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.inventory.partition;

import thredds.inventory.MFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Knows how to read ncx Index files, to decouple eg from GRIB
 *
 * @author caron
 * @since 11/10/13
 */
public interface IndexReader {

  /**
   * Open a Partition ncx file and read children indexes
   * @param indexFile the Partition ncx index file to open
   * @param callback for each child index, call this back
   * @return true if indexFile is a partition collection
   * @throws IOException on bad things
   */
  public boolean readChildren(Path indexFile, AddChildCallback callback) throws IOException;

  public interface AddChildCallback {
    /**
     * Callback for readChildren
     * @param topDir          the directory of the child collection
     * @param filename        the index filename of the child collection
     * @param lastModified    last modified for child collection
     * @throws IOException on bad
     */
    public void addChild(String topDir, String filename, long lastModified) throws IOException;
  }

  /**
   * Open an ncx file and find out what type it is
   * @param indexFile the ncx index file to open
   * @return true if its a partition type index
   * @throws IOException on bad
   */
  public boolean isPartition(Path indexFile) throws IOException;

  /**
   * Read the MFiles from a GribCollection index file
   * @param indexFile the Partition ncx index file to open
   * @param result put results in this list
   * @return true if indexFile is a GribCollection collection, and read ok
   */
  public boolean readMFiles(Path indexFile, List<MFile> result) throws IOException;


}
