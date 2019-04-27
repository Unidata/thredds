package ucar.nc2.grib.coord;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;

import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * Test CoordinateTime2DUnionizer
 *
 * @author caron
 * @since 11/25/2014
 */
public class TestCoordinateTime2DUnionizer {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static int code = 0;
  static CalendarPeriod timeUnit = CalendarPeriod.of("1 hour");
  static CalendarDate startDate = CalendarDate.parseISOformat(null, "1970-01-01T00:00:00");

  @Test
  public void testOrthogonalization() {
    List<CoordinateTimeAbstract> coords = new ArrayList<>();
    coords.add(makeTimeCoordinate(startDate, 12, 6));
    coords.add(makeTimeCoordinate(startDate, 4, 24));

    CoordinateTimeAbstract result = CoordinateTime2DUnionizer.testOrthogonal(coords);

    Formatter f = new Formatter(System.out);
    f.format("Original%n");
    for (CoordinateTimeAbstract coord : coords) {
      coord.showInfo(f, new Indent(2));
    }

    f.format("%nResult%n");
    if (result == null) f.format("not orthogonal");
    else result.showInfo(f, new Indent(2));

    Assert.assertNull(result);
  }

  private CoordinateTime makeTimeCoordinate(CalendarDate refDate, int size, int spacing) {
    List<Integer> offsetSorted = new ArrayList<>();
    for (int i = 0; i < size; i++) offsetSorted.add(i * spacing);
    return new CoordinateTime(code, timeUnit, refDate, offsetSorted, null);
  }

  @Test
  public void testMakeCoordinate2D() {
    List<CoordinateTime2D> coords = new ArrayList<>();
    coords.add(makeTimeCoordinate2D(12, 6));
    coords.add(makeTimeCoordinate2D(4, 24));

    int count = 0;
    Formatter f = new Formatter(System.out);
    f.format("Original%n");
    for (CoordinateTime2D coord : coords) {
      f.format("CoordinateTime2D %d%n", count++);
      coord.showInfo(f, new Indent(2));
      f.format("%n%n");
    }

    f.format("Unionize%n");
    for (CoordinateTime2D coord : coords) {
      f.format("CoordinateTime2D %d%n", count++);
      CoordinateTime2D result = testUnionizer(coord);
      Assert.assertNotNull(result);
      result.showInfo(f, new Indent(2));
      f.format("%n%n");
      Assert.assertTrue(result.isOrthogonal());
    }
  }

  @Test
  public void testCoordinateUnionizer() {
    Formatter f = new Formatter(System.out);
    f.format("Original CoordinateTime2D:%n");

    List<CoordinateTime2D> coord2Ds = new ArrayList<>();
    for (int i = 5; i < 15; i += 2) {
      CoordinateTime2D coord2D = makeTimeCoordinate2D(i, 3);
      coord2Ds.add(coord2D);
      coord2D.showInfo(f, new Indent(2));
    }

    CoordinateTime2DUnionizer unionizer = new CoordinateTime2DUnionizer(false, timeUnit, code, false, null);
    for (CoordinateTime2D coord2D : coord2Ds)
      unionizer.addAll(coord2D);
    unionizer.finish();
    CoordinateTime2D result = (CoordinateTime2D) unionizer.getCoordinate();

    f.format("%nUnionized Result:%n");
    result.showInfo(f, new Indent(2));
    Assert.assertTrue(result.isOrthogonal());
  }

  private CoordinateTime2D testUnionizer(CoordinateTime2D coord2D) {
    CoordinateTime2DUnionizer unionizer = new CoordinateTime2DUnionizer(false, timeUnit, code, false, null);
    unionizer.addAll(coord2D);
    unionizer.finish();
    return (CoordinateTime2D) unionizer.getCoordinate();
  }

  private CoordinateTime2D makeTimeCoordinate2D(int nruns, int ntimes) {
    CoordinateRuntime.Builder1 runBuilder = new CoordinateRuntime.Builder1(timeUnit);
    Map<Object, CoordinateBuilderImpl<Grib1Record>> timeBuilders = new HashMap<>();

    List<CoordinateTime2D.Time2D> vals = new ArrayList<>(nruns * ntimes);
    for (int j = 0; j < nruns; j++) {
      CalendarDate runDate = startDate.add(j, CalendarPeriod.Field.Hour);
      for (int i = 0; i < ntimes; i++) {
        CoordinateTime2D.Time2D time2D = new CoordinateTime2D.Time2D(runDate, i, null);
        vals.add(time2D);

        runBuilder.add(time2D.refDate);
        CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(time2D.refDate);
        if (timeBuilder == null) {
          timeBuilder = new CoordinateTime.Builder1(null, code, timeUnit, time2D.getRefDate());
          timeBuilders.put(time2D.refDate, timeBuilder);
        }
        timeBuilder.add(time2D.time);
      }
    }

    CoordinateRuntime runCoord = (CoordinateRuntime) runBuilder.finish();

    List<Coordinate> times = new ArrayList<>(runCoord.getSize());
    for (int idx = 0; idx < runCoord.getSize(); idx++) {
      long runtime = runCoord.getRuntime(idx);
      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(runtime);
      times.add(timeBuilder.finish());
    }

    Collections.sort(vals);

    return new CoordinateTime2D(code, timeUnit, vals, runCoord, times, null);
  }
}
