package ucar.nc2.grib.grib2.builder;

import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 11/24/13
 */
public class CoordinateTime implements Coordinate {
  List<TimeCoord> offsetSorted;
  Set<TimeCoord> offset;

  CoordinateTime(int estSize) {
    offset = new HashSet<>(estSize);
  }

  void add(Grib2Record r) {
    offset.add( extract(r));
  }

  void finish() {
    offsetSorted =  new ArrayList<>(offset.size());
    for (TimeCoord off : offset) offsetSorted.add(off);
    Collections.sort(offsetSorted);
    offset = null;
  }

  @Override
  public TimeCoord extract(Grib2Record gr) {
    return gr.getReferenceDate();
  }
}
