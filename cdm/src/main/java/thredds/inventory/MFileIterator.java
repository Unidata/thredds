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

  public MFileIterator(Iterator<MFile> iter) {
    this.iter = iter;
  }

  public void close() throws IOException {
  }

  public boolean hasNext() {
    return iter.hasNext();
  }

  public MFile next() {
    return iter.next();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}
