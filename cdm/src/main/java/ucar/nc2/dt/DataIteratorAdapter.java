package ucar.nc2.dt;

import java.util.Iterator;
import java.io.IOException;

/**
 * make an Iterator into a DataIterator
 */
public class DataIteratorAdapter implements DataIterator {
    private Iterator iter;
    public DataIteratorAdapter(Iterator iter) {
      this.iter = iter;
    }

    public boolean hasNext() {
      return iter.hasNext();
    }

    public Object nextData() throws IOException {
      return iter.next();
    }

    public Object next() {
      return iter.next();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
}
