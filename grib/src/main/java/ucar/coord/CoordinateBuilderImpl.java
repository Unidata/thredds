package ucar.coord;

import java.util.*;

/**
 * Builds one Coordinate of one Variable,
 * by keeping the Set of Values found in the records.
 *
 * @author caron
 * @since 11/27/13
 */
public abstract class CoordinateBuilderImpl<T> implements CoordinateBuilder<T> {
  protected Set<Object> valSet = new HashSet<>(100);
  protected Map<Object, Integer> valMap;    // map of values to index in Coordinate
  protected Coordinate coord;

  @Override
  public void addRecord(T gr) {
    Object val = extract(gr);
    valSet.add(val);
  }

  @Override
  public void addAll(Coordinate coord) {
   for (Object val : coord.getValues())
      valSet.add(val);
  }

  public void add(Object val) {
    valSet.add(val);
  }

  @Override
  public void addAll(List<Object> coords) {
   for (Object val : coords)
      valSet.add(val);
  }

  @Override
  public Coordinate finish() {
    List<Object> valList = new ArrayList<>(valSet.size());
    for (Object off : valSet) valList.add(off);
    coord =  makeCoordinate(valList);
    valSet = null;

    List<Object> values = (List<Object>) coord.getValues();
    if (values != null) {
      valMap = new HashMap<>(coord.getSize() * 2);
      for (int i = 0; i < values.size(); i++)
        valMap.put(values.get(i), i);
    }
    return coord;
  }

  // used by CoordinateND.makeSparseArray
  // not used by CoordinateTime2D
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
