package ucar.nc2.util;

import java.io.Closeable;
import java.util.Iterator;

/**
 * An iterator that must be closed.
 *
 * try (CloseableIterator iter = getIterator()) {
 *   // do stuff
 * }
 *
 * @author caron
 * @since 11/20/13
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {
}
