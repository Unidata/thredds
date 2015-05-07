/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Describe
 *
 * @author caron
 * @since 5/6/2015
 */
public class GridSubset {
  final GridCoordSys gcs;       // needed?
  Map<String, String> req = new HashMap<>();

  public GridSubset() {
    this.gcs = null;
  }

  public GridSubset(GridCoordSys gcs) {
    this.gcs = gcs;
  }

  public void set(GridCoordAxis.Type type, String value) {
    req.put(type.name(),value);
  }

  public Set<Map.Entry<String, String>> getEntries() {
    return req.entrySet();
  }
}
