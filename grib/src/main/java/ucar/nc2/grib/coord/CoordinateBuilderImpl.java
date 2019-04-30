/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import java.util.*;

/**
 * Builds one Coordinate of one Variable,
 * by keeping the Set of Values found in the records.
 *
 * @author caron
 * @since 11/27/13
 */
public abstract class CoordinateBuilderImpl<T> implements CoordinateBuilder<T> {
  private Set<Object> valSet = new HashSet<>(100);
  private Map<Object, Integer> valMap;    // map of values to index in Coordinate
  protected Coordinate coord;

  @Override
  public void addRecord(T gr) {
    Object val = extract(gr);
    valSet.add(val);
  }

  @Override
  public void addAll(Coordinate coord) {
    valSet.addAll(coord.getValues());
  }

  public void add(Object val) {
    valSet.add(val);
  }

  @Override
  public void addAll(List<Object> coords) {
    valSet.addAll(coords);
  }

  @Override
  public Coordinate finish() {
    List<Object> valList = new ArrayList<>(valSet.size());
    valList.addAll(valSet);
    coord =  makeCoordinate(valList);
    valSet = null;

    List<?> values = coord.getValues();
    if (values != null) {
      valMap = new HashMap<>(coord.getSize() * 2);
      for (int i = 0; i < values.size(); i++)
        valMap.put(values.get(i), i);
    }
    return coord;
  }

  // Used by CoordinateND.makeSparseArray; not used by CoordinateTime2D
  @Override
  public int getIndex(T gr) {
    Integer result =  valMap.get( extract(gr));
    return (result == null) ? 0 : result;
  }

  @Override
  public Coordinate getCoordinate() {
    return coord;
  }

}
