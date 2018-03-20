/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.params;

import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.List;

/**
 * Parameters specific to ncss point
 *
 * @author caron
 * @since 4/29/2015
 */
public class NcssPointParamsBean extends NcssParamsBean {

  //// station only
	private List<String> stns;

  public List<String> getStns() {
    return stns;
  }

  public void setStns(List<String> stns) {
    this.stns = stns;
  }

  public boolean hasStations() {
    return stns != null && !stns.isEmpty();
  }

  public NcssPointParamsBean() {}

  public NcssPointParamsBean(NcssParamsBean from) {
    super(from);
  }

  ///////////////////////////////

  public SubsetParams makeSubset() {

    SubsetParams subset = new SubsetParams();

    // vars
    subset.set(SubsetParams.variables, var);

    // horiz
    if (stns != null)
      subset.set(SubsetParams.stations, stns);
    else if (hasLatLonBB())
      subset.set(SubsetParams.latlonBB, getLatLonBoundingBox());
    else if (hasLatLonPoint())
      subset.set(SubsetParams.latlonPoint, new LatLonPointImpl(getLatitude(), getLongitude()));

    // time
    CalendarDate date = getRequestedDate(Calendar.getDefault());
    CalendarDateRange dateRange = getCalendarDateRange(Calendar.getDefault());
    if (isAllTimes()) {
      subset.set(SubsetParams.timeAll, true);

    } else if (date != null) {
      subset.set(SubsetParams.time, date);

    } else if (dateRange != null) {
      subset.set(SubsetParams.timeRange, dateRange);

    } else {
      subset.set(SubsetParams.timePresent, true);
    }

    if (timeWindow != null) {
      CalendarPeriod period = CalendarPeriod.of(timeWindow);
      subset.set(SubsetParams.timeWindow, period);
    }

    return subset;
  }

}
