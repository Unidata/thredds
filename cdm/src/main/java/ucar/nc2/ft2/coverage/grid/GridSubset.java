/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Describes a subset of a GridCoverage.
 * Coordinate values only, no indices.
 *
 * @author caron
 * @since 5/6/2015
 */
public class GridSubset {
  final Map<String, String> req = new HashMap<>();

  public GridSubset() {
  }

  public void set(GridCoordAxis.Type type, String value) {
    req.put(type.name(),value);
  }

  public Set<Map.Entry<String, String>> getEntries() {
    return req.entrySet();
  }

  public String get(GridCoordAxis.Type type) {
     return req.get(type.name());
   }

  public String get(String key) {
     return req.get(key);
   }


}
