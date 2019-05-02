/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import java.util.List;

/**
 * Builds Coordinates by finding distinct values.
 *
 * @author caron
 * @since 11/27/13
 */
public interface CoordinateBuilder<T> {

  void addRecord(T r);

  Object extract(T gr);

  // must sort values; Coordinate must be immutable
  Coordinate makeCoordinate(List<Object> values);

  Coordinate finish();

  int getIndex(T gr);

  Coordinate getCoordinate(); // call only after finish

  void addAll(Coordinate coord);

  void addAll(List<Object> coords);

  interface TwoD<T> {
    int[] getCoordIndices(T gr);
  }
}
