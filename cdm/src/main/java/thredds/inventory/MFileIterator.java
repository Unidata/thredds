package thredds.inventory;

import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.util.Iterator;

/**
 * An iterator over MFiles, closeable so its a target for try-with-resource
 *
 * @author caron
 * @since 11/20/13
 */
public class MFileIterator implements CloseableIterator<MFile> {
  private Iterator<MFile> iter;
  private MFileFilter filter;
  private MFile nextMfile;

  /**
   * Constructor
   * @param iter   iterator over MFiles
   * @param filter optional filter, may be null
   */
  public MFileIterator(Iterator<MFile> iter, MFileFilter filter) {
    this.iter = iter;
    this.filter = filter;
  }

  public void close() throws IOException {
  }

  public boolean hasNext() {
    while (true) {
      if (!iter.hasNext()) return false;
      nextMfile = iter.next();
      if (filter == null || filter.accept(nextMfile)) return true;
    }
  }

  public MFile next() {
    return nextMfile;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}
