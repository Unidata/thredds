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

package thredds.inventory;

import org.slf4j.Logger;
import thredds.filesystem.MFileOS;
import ucar.nc2.util.CloseableIterator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * MCollection that is initialized by specific list of MFiles.
 * Sorted by name. no date extractor.
 *
 * @author caron
 * @since 11/13/13
 */
public class CollectionList extends CollectionAbstract {
  protected List<MFile> mfiles = new ArrayList<>();

  public CollectionList(String collectionName, String list, Logger logger) {
    super(collectionName, logger);

    if (list.startsWith(CollectionAbstract.LIST))
      list = list.substring(CollectionAbstract.LIST.length());

    long lastModified = 0;
    String[] files = list.split(";");
    for (String s : files) {
      String filename = s.trim();
      if (filename.length() == 0) continue;
      Path p = Paths.get(filename);
      if (Files.exists(p)) {
        MFileOS mfile = new MFileOS(filename);
        mfiles.add(new MFileOS(filename));
        lastModified = Math.max(lastModified, mfile.getLastModified());
      }
    }

    Collections.sort(mfiles);
    this.lastModified = lastModified;
    this.root = System.getProperty("user.dir");
  }

  public CollectionList(String collectionName, String root, List<MFile> mfiles, Logger logger) {
    super(collectionName, logger);
    setRoot(root);
    this.mfiles = mfiles;
  }

  protected CollectionList(String collectionName, Logger logger) {
    super(collectionName, logger);
  }

  @Override
  public Iterable<MFile> getFilesSorted() {
    return mfiles;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator(mfiles.iterator(), null);
  }

  @Override
  public void close() {  }

}
