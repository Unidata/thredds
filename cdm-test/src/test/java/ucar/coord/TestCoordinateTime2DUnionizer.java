package ucar.coord;

import org.junit.Test;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 11/25/2014
 */
public class TestCoordinateTime2DUnionizer {
  static int code = 0;
  static CalendarPeriod timeUnit = CalendarPeriod.of("1 hour");
  static CalendarDate startDate = CalendarDate.parseISOformat(null, "1970-01-01T00:00:00");

  @Test
  public void testOrthoginalization() {
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
   }

  @Test
  public void testMakeCoordinate2D() {
    List<CoordinateTime2D> coords = new ArrayList<>();
    coords.add(makeTimeCoordinate2D( 12, 6));
    coords.add(makeTimeCoordinate2D( 4, 24));

    int count = 0;
    Formatter f = new Formatter(System.out);
    f.format("Original%n");
    for (CoordinateTime2D coord : coords) {
      f.format("CoordinateTime2D %d%n", count++);
      coord.showInfo(f, new Indent(2));
      f.format("%n%n");
    }

    f.format("Orthononalize%n");
    for (CoordinateTime2D coord : coords) {
      f.format("CoordinateTime2D %d%n", count++);
      CoordinateTime2D result = testOrthogonalizaition(coord);
      result.showInfo(f, new Indent(2));
      f.format("%n%n");
      assert result.isOrthogonal();
     }
  }

  CoordinateTime2D testOrthogonalizaition(CoordinateTime2D coord2D) {
    CoordinateTime2DUnionizer unionizer = new CoordinateTime2DUnionizer(false, timeUnit, code, false);
    unionizer.addAll(coord2D);
    unionizer.finish();
    return (CoordinateTime2D) unionizer.getCoordinate();
  }



  @Test
  public void testCoordinateUnionizer() {

    List<CoordinateTime2D> coord2Ds = new ArrayList<>();
    for (int i = 5; i < 15; i += 2) {
      coord2Ds.add(makeTimeCoordinate2D(i, 3));
    }

    CoordinateTime2DUnionizer unionizer = new CoordinateTime2DUnionizer(false, timeUnit, code, false);
    for (CoordinateTime2D coord2D : coord2Ds) {
      unionizer.addAll(coord2D);
    }
    unionizer.finish();
    CoordinateTime2D result = (CoordinateTime2D) unionizer.getCoordinate();

    Formatter f = new Formatter();
    f.format("Original%n");
    for (CoordinateTime2D coord2D : coord2Ds) {
      coord2D.showInfo(f, new Indent(2));
    }

    f.format("%nResult%n");
    result.showInfo(f, new Indent(2));
   }

  static public CoordinateTime makeTimeCoordinate(CalendarDate refDate, int size, int spacing) {
    List<Integer> offsetSorted = new ArrayList<>();
    for (int i=0; i<size; i++) offsetSorted.add(i * spacing);
    return new CoordinateTime(code, timeUnit, refDate, offsetSorted, null);
  }

  static public CoordinateTime2D makeTimeCoordinate2D(int nruns, int ntimes) {
    CoordinateRuntime.Builder1 runBuilder = new CoordinateRuntime.Builder1(timeUnit);
    Map<Object, CoordinateBuilderImpl<Grib1Record>> timeBuilders = new HashMap<>();

    List<CoordinateTime2D.Time2D> vals = new ArrayList<>(nruns*ntimes);
    for (int j=0; j<nruns; j++) {
      CalendarDate runDate = startDate.add(j, CalendarPeriod.Field.Hour);
      for (int i = 0; i < ntimes; i++) {
        CoordinateTime2D.Time2D time2D = new CoordinateTime2D.Time2D(runDate, i, null);
        vals.add(time2D);

        runBuilder.add( time2D.run);
        CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(time2D.run);
        if (timeBuilder == null) {
          timeBuilder = new CoordinateTime.Builder1(null, code, timeUnit, time2D.getRefDate());
          timeBuilders.put(time2D.run, timeBuilder);
        }
        timeBuilder.add(time2D.time);
      }
    }

    CoordinateRuntime runCoord = (CoordinateRuntime) runBuilder.finish();

    List<Coordinate> times = new ArrayList<>(runCoord.getSize());
    for (int idx=0; idx<runCoord.getSize(); idx++) {
      long runtime = runCoord.getRuntime(idx);
      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(runtime);
      times.add(timeBuilder.finish());
    }

    Collections.sort(vals);

    return new CoordinateTime2D(code, timeUnit, vals, runCoord, times);
  }
}
