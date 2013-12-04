package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * A tree of CoordinateBuilders, branching on unique coordinate values.
 * Store records at the leaves.
 *
 * @author caron
 * @since 11/27/13
 */
public abstract class CoordinateBuilderImpl implements CoordinateBuilder {
  protected Object val;
  protected CoordinateBuilder nestedBuilder;
  protected Map<Object, CoordinateBuilder> nextMap;
  protected Map<Object, List<Grib2Record>> records;
  protected Coordinate coord;

  protected CoordinateBuilderImpl(Object val) {
    this.val = val;
  }

  @Override
  public List<Grib2Record> getRecords(Object key) {
    if (records == null) return null;
    return records.get(key);
  }

  @Override
  public CoordinateBuilder chainTo(CoordinateBuilder nestedBuilder) {
    this.nestedBuilder = nestedBuilder;
    return nestedBuilder;
  }

  abstract protected Object extract(Grib2Record gr);
  abstract protected Coordinate makeCoordinate(List<Object> values, List<Coordinate> subdivide);  // subdivide may be null

  @Override
  public void add(Grib2Record gr) {
    Object val = extract(gr);
    if (nestedBuilder == null) {
      if (records == null) records = new HashMap<>();
      List<Grib2Record> recordsList = records.get(val); // dont think this is needed?
      if (recordsList == null) {
        recordsList = new ArrayList<>();
        records.put(val, recordsList);
      }
      recordsList.add(gr);
      return;
    }

    if (nextMap == null) nextMap = new HashMap<>();
    CoordinateBuilder next = nextMap.get(val);
    if (next == null) {
      next = nestedBuilder.makeBuilder(val);
      nextMap.put(val, next);                     // seperate builder for each - nested, also could do orthogonal
    }
    next.add(gr);
  }

  @Override
  public Coordinate finish() {

    if (nestedBuilder == null) {
      if (records == null) records = new HashMap<>(); // ??
      Set<Object> keys = records.keySet();
      List<Object> values = new ArrayList<>(keys.size());
      for (Object key : keys) values.add(key);

      this.coord = makeCoordinate(values, null);
      return this.coord;
    }

    if (nextMap == null) nextMap = new HashMap<>(); // ??
    Set<Object> keys = nextMap.keySet();
    List<Object> values = new ArrayList<>(keys.size());
    for (Object off : keys) values.add(off);

    List<Coordinate> subdivide = new ArrayList<>(values.size());
    for (Object key : values) {
      CoordinateBuilder next = nextMap.get(key);
      subdivide.add(next.finish());
    }
    this.coord = makeCoordinate(values, subdivide);
    return this.coord;
  }

  public Coordinate makeCoordinate(Set<Object> allCoords) {
    List<Object> values = new ArrayList<>(allCoords.size());
    for (Object off : allCoords) values.add(off);
    return makeCoordinate(values, null);
  }


  @Override
  public Coordinate getCoordinate() {
    return coord;
  }

  @Override
  public CoordinateBuilder getNestedBuilder() {
    return nestedBuilder;
  }

  @Override
  public CoordinateBuilder getChildBuilder(Object key) {
    if (nextMap == null) return null;
    return nextMap.get(key);
  }
}
