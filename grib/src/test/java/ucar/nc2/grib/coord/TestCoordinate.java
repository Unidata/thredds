package ucar.nc2.grib.coord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Indent;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test Coordinate class
 *
 * @author caron
 * @since 12/10/13
 */
public class TestCoordinate implements Coordinate {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  List<Object> vals;
  Coordinate.Type type = Type.vert;

  TestCoordinate(int nvals) {
    vals = new ArrayList<>(nvals);
    for (int i=0; i<nvals; i++) vals.add(i);
  }

  public static Coordinate factory(int nvals, Coordinate.Type type) {
    CalendarPeriod period = CalendarPeriod.of("1 hour");
    switch (type) {
      case runtime:
        List<Long> cd = new ArrayList<>(nvals);
        for (int i=0; i<nvals; i++) cd.add( CalendarDate.of(null, 1953, 11, i+1, 9, i+1, 0).getMillis());
        return new CoordinateRuntime(cd, period);
      case time:
        List<Integer> vals = new ArrayList<>(nvals);
        for (int i=0; i<nvals; i++) vals.add(i);
        return new CoordinateTime(0, period, null, vals, null);
      case vert:
        List<VertCoordValue> vert = new ArrayList<>(nvals);
        for (int i=0; i<nvals; i++) vert.add(new VertCoordValue((double) (i+1), (double) (i+2)));
        return new CoordinateVert(1, new VertCoordType(11, "m", null, true), vert);  // random vert unit
     }
    return null;
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void showCoords(Formatter info) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Counters calcDistributions() {
    return null;
  }

  @Override
  public List<? extends Object> getValues() {
    return vals;
  }

  @Override
  public Object getValue(int idx) {
    return vals.get(idx);
  }

  @Override
  public int getIndex(Object val) {
    return vals.indexOf(val);
  }

  @Override
  public int getSize() {
    return vals.size();
  }

  @Override
  public int getNCoords() {
    return getSize();
  }

  @Override
  public int estMemorySize() {
    return 0;
  }

  @Override
  public int getCode() {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getName() {
    return "testVals for type "+type.toString();
  }

  @Override
  public String getUnit() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
