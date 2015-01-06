package ucar.coord;

import ucar.nc2.util.Indent;

import java.util.Formatter;
import java.util.List;

/**
 * Abstract coordinate
 *
 * @author caron
 * @since 11/24/13
 */
public interface Coordinate {
  /**
   * Enumerated list of coordinate types
   */
  public enum Type {runtime, time, timeIntv, vert, time2D, ens }  // cant change order, protobuf uses the ordinal

  void showInfo(Formatter info, Indent indent);
  void showCoords(Formatter info);

  List<? extends Object> getValues(); // get sorted list of values
  Object getValue(int idx);  // get the ith value
  int getIndex(Object val);  // LOOK assumes the values are unique;
  int getSize();             // how many values ??
  int estMemorySize();       // estimated memory size in bytes (debugging)

  int getCode();
  Type getType();
  String getName();
  String getUnit();
}
