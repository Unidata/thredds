package ucar.coord;

import java.util.List;

/**
 * Builds Coordinates
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

  static public interface TwoD<T> {
    public int[] getCoordIndices(T gr);
  }
}
