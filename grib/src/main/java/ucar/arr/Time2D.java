package ucar.arr;

import ucar.nc2.time.CalendarDate;

import java.util.HashMap;
import java.util.Map;

/**
 * Describe
 *
 * @author caron
 * @since 11/24/13
 */
public class Time2D {
  int estSize;
  Map<CalendarDate, SparseArray> rundate;

  public Time2D(int estRundates, int estSize) {
    rundate = new HashMap<>(2*estRundates);
    this.estSize = estSize;
  }

  public void addRecord(CalendarDate cd, int offset) {
    SparseArray sparse =  rundate.get(cd);
    if (sparse == null) {
      sparse = new SparseArray(estSize);
    }
    sparse.addOffset(offset);
  }


}
