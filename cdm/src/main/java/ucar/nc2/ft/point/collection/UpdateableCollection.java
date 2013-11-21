package ucar.nc2.ft.point.collection;

import java.io.IOException;

/**
 * Mixin for update() method.
 *
 * @author caron
 * @since Nov 22, 2010
 */
public interface UpdateableCollection {
  void update() throws IOException;
}
