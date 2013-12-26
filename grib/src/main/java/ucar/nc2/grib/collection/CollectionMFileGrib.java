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
 *
 * @author caron
 * @since 12/3/13
 */
public class CollectionMFileGrib extends CollectionAbstract {
  CollectionSpecParser sp;

  public CollectionMFileGrib(String collectionName, String collectionSpec, Formatter errlog) {
    super(collectionName, null);
    sp = new CollectionSpecParser(collectionSpec, errlog);
    List<MFileFilter> filters = new ArrayList<MFileFilter>(2);
    if (null != sp.getFilter())
      filters.add(new WildcardMatchOnName(sp.getFilter()));
    dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
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
