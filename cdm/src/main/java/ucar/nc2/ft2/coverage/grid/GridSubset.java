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
  private final Map<String, Object> req = new HashMap<>();

  public Set<Map.Entry<String, Object>> getEntries() {
    return req.entrySet();
  }
  public Set<String> getKeys() {
    return req.keySet();
  }

  public void set(String key, Object value) {
    req.put(key, value);
  }

  public void set(GridCoordAxis.Type type, Object value) {
    req.put(type.name(), value);
  }

  public Object get(GridCoordAxis.Type type) {
     return req.get(type.name());
   }

  public Object get(String key) {
     return req.get(key);
   }

  public Double getDouble(String key) {
    Object val = req.get(key);
    if (val == null) return null;
    Double dval;
    if (val instanceof Double) dval = (Double) val;
    else dval = Double.parseDouble((String) val);
    return dval;
  }

}
