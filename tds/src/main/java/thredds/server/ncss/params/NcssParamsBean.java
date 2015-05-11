/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncss.params;

import org.springframework.format.annotation.DateTimeFormat;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.validation.NcssGridRequestConstraint;
import thredds.server.ncss.validation.TimeParamsConstraint;
import thredds.server.ncss.validation.VarParamConstraint;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.text.ParseException;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

/**
 * Ncss Parameters superclass
 *
 * @author caron
 * @since 10/5/13
 */

@TimeParamsConstraint
public class NcssParamsBean {

  protected String accept;            // LOOK use enum ?

  @VarParamConstraint
  protected List<String> var;

 	@DateTimeFormat
  protected String time_start;        // LOOK can be "present"

  @DateTimeFormat
  protected String time_end;           // LOOK can be "present"

  @DateTimeFormat
  protected String time_duration;      // LOOK not a DateTimeFormat

  @DateTimeFormat
  protected String time;               // LOOK can be "present" or "all" ??

  @DateTimeFormat
  protected String time_window;        // time duration
    // time_window is meant to be used with time=present. When time=present it returns the closest time to current in the dataset
    // but if the dataset does not have up to date data that could be really far from the current time and most likely useless (esp for observation data).
    // time_window tells the server give me the data if it's within this period otherwise don't bother. time_window must be a valid W3C time duration.


  protected String temporal;  // == all  // LOOK get rid of

  protected Double north;

  protected Double south;

  protected Double east;

  protected Double west;

  protected Double latitude;

  protected Double longitude;


  /////////////////////////////////////////////////////

  public String getAccept() {
    return accept;
  }

  public void setAccept(String accept) {
    this.accept = accept;
  }

  public List<String> getVar() {
    return var;
  }

  public void setVar(List<String> var) {
    this.var = var;
  }

  public String getTime_start() {
    return time_start;
  }

  // time
  public void setTime_start(String time_start) {
    this.time_start = time_start;
  }

  public String getTime_end() {
    return time_end;
  }

  public void setTime_end(String time_end) {
    this.time_end = time_end;
  }

  public String getTime_duration() {
    return time_duration;
  }

  public void setTime_duration(String time_duration) {
    this.time_duration = time_duration;
  }

  public String getTime_window() {
    return time_window;
  }

  public void setTime_window(String time_window) {
    this.time_window = time_window;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  // latlon
  public Double getNorth() {
    return north;
  }

  public void setNorth(Double north) {
    this.north = north;
  }

  public Double getSouth() {
    return south;
  }

  public void setSouth(Double south) {
    this.south = south;
  }

  public Double getEast() {
    return east;
  }

  public void setEast(Double east) {
    this.east = east;
  }

  public Double getWest() {
    return west;
  }

  public void setWest(Double west) {
    this.west = west;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public boolean hasLatLonPoint() {
    return latitude != null && longitude != null;
  }

  public boolean hasLatLonBB() { // need to validate
    return east != null && west != null && north != null && south != null;
  }

  // old
  public String getTemporal() {
    return temporal;
  }

  public void setTemporal(String temporal) {
    this.temporal = temporal;
  }

  public boolean isAllTimes() {
    return temporal != null && temporal.equalsIgnoreCase("all");
  }

  ///////////////////////////////////////////////////////////////////////////////
  // used ??

  public TimeDuration parseTimeDuration() throws NcssException {
    if (getTime_duration() == null) return null;
    try {
      return TimeDuration.parseW3CDuration(getTime_duration());
    } catch (ParseException e) {
      throw new NcssException("invalid time duration");
    }
  }

  public LatLonRect getBoundingBox() {
    if (!hasLatLonBB()) {
      return null;
    } else {
      double width = getEast() - getWest();
      double height = getNorth() - getSouth();
      return new LatLonRect(new LatLonPointImpl(getSouth(), getWest()), height, width);
    }
  }

  private boolean hasValidTime;
  private boolean hasValidDateRange;

  public void setHasValidTime(boolean hasValidTime) {
    this.hasValidTime = hasValidTime;
  }

  public void setHasValidDateRange(boolean hasValidDateRange) {
    this.hasValidDateRange = hasValidDateRange;
  }

  // return requested CalendarDateRange.
  public CalendarDateRange getCalendarDateRange(Calendar cal) throws ParseException {

    if (hasValidDateRange) {
      DateRange dr;
      if (time == null)
        dr = new DateRange(new DateType(time_start, null, null, cal), new DateType(time_end, null, null, cal), new TimeDuration(time_duration), null);
      else {
        //DateType dtDate = new DateType(time, null, null, cal);
        dr = new DateRange(new DateType(time, null, null, cal), new DateType(time, null, null, cal), new TimeDuration(time_duration), null);
      }
      return CalendarDateRange.of(dr.getStart().getCalendarDate(), dr.getEnd().getCalendarDate());
    }

    if (getTime() != null) {
      CalendarDate date;
      if (getTime().equalsIgnoreCase("present")) {
        date = CalendarDate.present();

      } else if (getTime().equalsIgnoreCase("all")) {
        return null;

      } else {
        date = CalendarDateFormatter.isoStringToCalendarDate(cal, getTime());
      }
      return CalendarDateRange.of(date, date);
    }

    // default is pressent
    return CalendarDateRange.of(CalendarDate.present(), CalendarDate.present());
	}

  public CalendarDate getRequestedTime( Calendar cal ) throws ParseException {
    if (!hasValidTime) return null;

 			if( getTime().equalsIgnoreCase("present") ){
 				 java.util.Calendar c = java.util.Calendar.getInstance();
 				 c.setTime( new Date()  );
 				 return CalendarDate.of( cal, c.getTimeInMillis()  );
 			}

    return CalendarDateFormatter.isoStringToCalendarDate(cal, getTime());
 	}

  public boolean intersectsTime(CalendarDateRange have, Formatter errs) throws ParseException {
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
