package ucar.sparr;

import ucar.nc2.util.Indent;

import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 11/24/13
 */
public interface Coordinate {
  public enum Type {runtime, time, timeIntv, vert}

  void showInfo(Formatter info, Indent indent);
  void showCoords(Formatter info);

  List<? extends Object> getValues();
  Object getValue(int idx);
  int getIndex(Object val);

  int getSize();

  int getCode();
  Type getType();
  String getName();
  String getUnit();
}
