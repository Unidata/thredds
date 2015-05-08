/* Copyright */
package thredds.server.ncss.params;

import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import java.text.ParseException;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 4/29/2015
 */
public class NcssGridParamsBean extends NcssParamsBean {

  public boolean intersectsTime(GridCoverageDataset gcd, Formatter errs) throws ParseException {
    CalendarDateRange have =  gcd.getCalendarDateRange();
    if (have == null) return true;
    Calendar dataCal = have.getStart().getCalendar(); // use the same calendar as the dataset

    CalendarDateRange want = getCalendarDateRange(dataCal);
    if (want != null) {
      if (have.intersects(want)) {
        return true;
      } else {
        errs.format("Requested time range %s does not intersect actual time range %s", want, have);
        return false;
      }
    }

    CalendarDate wantTime = getRequestedTime(dataCal);
    if (wantTime == null) return true;
    if (!have.includes(wantTime)) {
      errs.format("Requested time %s does not intersect actual time range %s", wantTime, have);
      return false;
    }
    return true;
  }

}
