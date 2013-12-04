package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;

import java.util.List;
import java.util.Set;

/**
 * Build Coordinates heirarchically, keeping records at the leaves
 * Finds distinct values
 *
 * @author caron
 * @since 11/27/13
 */
public interface CoordinateBuilder {

  public void addRecord(Grib2Record r);

  public Object extract(Grib2Record gr);

  // must sort values; Coordinate must be immutable
  public Coordinate makeCoordinate(List<Object> values);

  public Coordinate finish();

  public int getIndex(Grib2Record gr);

  public Coordinate getCoordinate(); // call only after finish

  public void addAll(Coordinate coord);

}
