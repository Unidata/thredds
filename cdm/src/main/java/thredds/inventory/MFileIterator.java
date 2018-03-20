/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
      if (!iter.hasNext()) {
        nextMfile = null;
        return false;
      }
      nextMfile = iter.next();
      if (filter == null || filter.accept(nextMfile)) return true;
    }
  }

  public MFile next() {
    if (nextMfile == null) throw new NoSuchElementException();
    return nextMfile;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}
