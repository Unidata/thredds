/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import thredds.filesystem.MFileOS7;
import thredds.inventory.*;
import thredds.inventory.filter.WildcardMatchOnName;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Static collection, always excludes indexes
 * Not used.
 *
 * @author caron
 * @since 12/3/13
 */
public class CollectionMFileGrib extends CollectionAbstract {
  CollectionSpecParser sp;

  public CollectionMFileGrib(String collectionName, String collectionSpec, Formatter errlog) {
    super(collectionName, null);
    this.sp = new CollectionSpecParser(collectionSpec, errlog);
    List<MFileFilter> filters = new ArrayList<MFileFilter>(2);
    if (null != sp.getFilter())
      filters.add(new WildcardMatchOnName(sp.getFilter()));
    this.dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
  }

  @Override
  public String getRoot() {
    return sp.getRootDir();
  }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    List<MFile> list = new ArrayList<>(100);
    try (CloseableIterator<MFile> iter = getFileIterator()) {
       while (iter.hasNext()) {
         list.add(iter.next());
       }
     }
    if (hasDateExtractor()) {
      Collections.sort(list, new DateSorter());  // sort by date
    } else {
      Collections.sort(list);                    // sort by name
    }
    return list;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return null; // new MFileIterator(topDir, new MyFilter());
  }

  @Override
  public void close() {
  }

  // returns everything in the current directory
  private class MFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;

    MFileIterator(Path dir, DirectoryStream.Filter<Path> filter) throws IOException {
      if (filter != null)
        dirStream = Files.newDirectoryStream(dir, filter);
      else
        dirStream = Files.newDirectoryStream(dir);

      dirStreamIterator = dirStream.iterator();
    }

    public boolean hasNext() {
      return dirStreamIterator.hasNext();
    }

    public MFile next() {
      try {
        MFileOS7 mfile = new MFileOS7(dirStreamIterator.next());
        lastModified = Math.max(lastModified, mfile.getLastModified());
        return mfile;

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    // better alternative is for caller to send in callback (Visitor pattern)
    // then we could use the try-with-resource
    public void close() throws IOException {
      dirStream.close();
    }
  }

}
