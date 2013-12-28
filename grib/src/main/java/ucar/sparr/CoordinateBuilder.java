package ucar.sparr;

import java.util.List;

/**
 * Build Coordinates heirarchically, keeping records at the leaves
 * Finds distinct values
 *
 * @author caron
 * @since 11/27/13
 */
public interface CoordinateBuilder<T> {

  public void addRecord(T r);

  public Object extract(T gr);

  // must sort values; Coordinate must be immutable
  public Coordinate makeCoordinate(List<Object> values);

  public Coordinate finish();

  public int getIndex(T gr);

  public Coordinate getCoordinate(); // call only after finish

  public void addAll(Coordinate coord);

  public void addAll(List<Object> coords);
}
