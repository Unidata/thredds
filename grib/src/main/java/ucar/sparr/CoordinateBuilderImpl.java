package ucar.sparr;

import java.util.*;

/**
 * Builds a Coordinate
 *
 * @author caron
 * @since 11/27/13
 */
public abstract class CoordinateBuilderImpl<T> implements CoordinateBuilder<T> {
  protected Set<Object> valSet = new HashSet<>(100);
  protected Map<Object, Integer> valMap;
  protected Coordinate coord;

  @Override
  public void addRecord(T gr) {
    Object val = extract(gr);
    valSet.add(val);
  }

  public void addAll(Coordinate coord) {
   for (Object val : coord.getValues())
      valSet.add(val);
  }

  @Override
  public Coordinate finish() {
    List<Object> valList = new ArrayList<>(valSet.size());
    for (Object off : valSet) valList.add(off);
    coord =  makeCoordinate(valList);
    valSet = null;

    List<Object> values = (List<Object>) coord.getValues();
    valMap = new HashMap<>(coord.getSize()*2);
    for (int i=0; i< coord.getSize(); i++)
      valMap.put(values.get(i), i);
    return coord;
  }

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
