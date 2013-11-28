package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 11/27/13
 */
public abstract class CoordinateBuilderImpl implements CoordinateBuilder {
  protected Object val;
  protected CoordinateBuilder builderBuilder;
  protected Map<Object, CoordinateBuilder> nextMap;
  protected List<Grib2Record> records;
  protected Coordinate coord;

  protected CoordinateBuilderImpl(Object val) {
    this.val = val;
    nextMap = new HashMap<>();
  }

  @Override
  public List<Grib2Record> getRecords() {
    return records;
  }

  @Override
  public CoordinateBuilder chainTo(CoordinateBuilder builder) {
    builderBuilder = builder;
    return builder;
  }

  abstract protected Object extract(Grib2Record gr);
  abstract protected Coordinate makeCoordinate(List<Object> values, List<Coordinate> subdivide);  // subdivide may be null

  @Override
  public void add(Grib2Record gr) {
    Object val = extract(gr);
    if (builderBuilder == null) {
      nextMap.put(val, null);
      if (records == null) records = new ArrayList<>();
      records.add(gr);
      return;                     // store record only at leaf ??
    }

    CoordinateBuilder next = nextMap.get(val);
    if (next == null) {
      next = builderBuilder.makeBuilder(val);
      nextMap.put(val, next);                     // seperate builder for each - nested, also could do orthogonal
    }
    next.add(gr);
  }

  @Override
  public Coordinate finish() {
    Set<Object> keys = nextMap.keySet();

    List<Object> values = new ArrayList<>(keys.size());
    for (Object off : keys) values.add(off);

    if (builderBuilder == null) {
      this.coord =  makeCoordinate(values, null);
      return this.coord;
    }

    List<Coordinate> subdivide = new ArrayList<>(values.size());
    for (Object key : values) {
      CoordinateBuilder next = nextMap.get(key);
      subdivide.add(next.finish());
    }
    this.coord =  makeCoordinate(values, subdivide);
    return this.coord;
  }

  @Override
  public Coordinate getCoordinate() {
    return coord;
  }

  @Override
  public boolean hasChildBuilders() {
    return nextMap != null;
  }

  @Override
  public CoordinateBuilder getChildBuilder(Object key) {
    if (nextMap == null) return null;
    return nextMap.get(key);
  }
}
