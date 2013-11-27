package ucar.nc2.grib.grib2.builder;

import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarDate;

import java.util.*;

/**
 * Grib runtime coordinate
 *
 * @author caron
 * @since 11/24/13
 */
public class CoordinateRuntime implements Coordinate {
  List<CalendarDate> runtimeSorted;
  Set<CalendarDate> runtimes;

  CoordinateRuntime(int estSize) {
    runtimes = new HashSet<>(estSize);
  }

  void add(Grib2Record r) {
    runtimes.add( extract(r));
  }

  void finish() {
    runtimeSorted = new ArrayList<>(runtimes.size());
    for (CalendarDate cd : runtimes) runtimeSorted.add(cd);
    Collections.sort(runtimeSorted);
    runtimes = null;
  }

  @Override
  public CalendarDate extract(Grib2Record gr) {
    return gr.getReferenceDate();
  }
}
