package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.builder.CoordinateRuntime;
import ucar.nc2.grib.grib2.builder.CoordinateTime;
import ucar.nc2.time.CalendarDate;

import java.util.*;

/**
 * Create CoordinateTime for each runTime
 *
 * @author caron
 * @since 11/24/13
 */
public class Time2D {
  CoordinateRuntime runtime;
  Map<CalendarDate, CoordinateTime.Builder> timeMap;
  List<CoordinateTime> timesSorted;
  SparseArray<Grib2Record> sa;

  public Time2D(CoordinateRuntime runtime) {
    this.runtime = runtime;
    List<CalendarDate> runs = runtime.getRuntimesSorted();
    timeMap = new HashMap<>(runs.size() * 2);
    for (CalendarDate cd : runs)
      timeMap.put(cd, new CoordinateTime.Builder(cd));
  }

  public void add(Grib2Record gr) {
    CalendarDate cd = CoordinateRuntime.extractRunDate(gr);
    CoordinateTime.Builder time = timeMap.get(cd);
    time.add(gr);
  }

  public void finish() {
    int maxTimes = 0;
    int nruns = timeMap.values().size();
    timesSorted = new ArrayList<>(nruns);
    for (CoordinateTime.Builder bucket : timeMap.values()) {
      CoordinateTime tc = null; // bucket.finish();
      timesSorted.add(tc);
      maxTimes = Math.max(maxTimes, tc.getSize());
    }
    timeMap = null;
    Collections.sort(timesSorted);

    /* now we can create the sparse array
    sa = new SparseArray<>(nruns, maxTimes);
    int runIdx = 0;
    for (CoordinateTime time : timesSorted) { // one for each runtime
      for (int timeIdx = 0; timeIdx < time.getSize(); timeIdx++) {
        for (Grib2Record r : time.getRecordList(timeIdx))
          sa.add(r, runIdx, timeIdx);
      }
      runIdx++;
    } */
  }

  public void showInfo(Formatter info) {
    info.format(" %20s    Offsets %n", "RunTime");
    for (CoordinateTime time : timesSorted) {
      info.format(" %20s    ", time.getRuntime());
      for (int off : time.getOffsetSorted())
        info.format(" %3d,", off);
      info.format("%n");
    }
    sa.showInfo(info);
  }

}
