/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.grid;

import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.grid.GridTableLookup;
import ucar.grid.GridRecord;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib1.Grib1GridTableLookup;
import ucar.grib.GribGridRecord;

import java.util.*;

/**
 * A Time Coordinate for a Grid dataset.
 *
 * @author caron
 */
public class GridTimeCoord {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridTimeCoord.class);

  private Calendar calendar;
  private String name;
  private GridTableLookup lookup;
  private int seq = 0; // for getting a unique name
  private String timeUnit;

  private Date baseDate;
  private List<Date> times;
  private List<TimeCoordWithInterval> timeIntvs;
  private int constantInterval = -1;

  private GridTimeCoord() {
    // need to have this non-static for thread safety
    calendar = Calendar.getInstance();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  private class TimeCoordWithInterval implements Comparable<TimeCoordWithInterval> {
    Date coord;
    int start, interval;

    private TimeCoordWithInterval(Date coord, int start, int interval) {
      this.coord = coord;
      this.start = start;
      this.interval = interval;
    }

    @Override
    public int compareTo(TimeCoordWithInterval o) {
      int diff = coord.compareTo(o.coord);
      return (diff == 0) ? (interval - o.interval) : diff;
    }

    @Override
    public int hashCode() {
      return 17 * coord.hashCode() + interval;
    }

    @Override
    public boolean equals(Object obj) {
      TimeCoordWithInterval o = (TimeCoordWithInterval) obj;
      return coord.equals(o.coord) && (interval == o.interval);
    }
  }

  /**
   * Create a new GridTimeCoord from the list of GridRecord
   *
   * @param records records to use
   * @param lookup  lookup table
   */
  GridTimeCoord(List<GridRecord> records, GridTableLookup lookup) {
    this();
    this.lookup = lookup;

    // make sure that the base times and time units agree
    for (GridRecord record : records) {
      if (this.baseDate == null) {
        this.baseDate = record.getReferenceTime();
        this.timeUnit = record.getTimeUnitName();
      } else {
        Date ref = record.getReferenceTime();
        if (!baseDate.equals(ref))
          log.warn(record + " does not have same base date= " + baseDate + " != " + ref);
        if (this.timeUnit != record.getTimeUnitName())
          log.warn(record + " does not have same time unit= " + this.timeUnit + " != " + record.getTimeUnitName());
      }
    }

    // interval case - currently only GRIB
    if (records.get(0) instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) records.get(0);

      if (ggr.isInterval()) {
        timeIntvs = new ArrayList<TimeCoordWithInterval>();

        boolean same = true;
        int intv = -1;
        for (GridRecord gr : records) {
          ggr = (GribGridRecord) gr;
          int start = ggr.startOfInterval;
          int end = ggr.forecastTime;
          int intv2 = end - start;

          if (intv2 > 0) { // skip those weird zero-intervals when testing for constant interval
            if (intv < 0) intv = intv2;
            else same = same && (intv == intv2);
          }

          Date validTime = getValidTime(gr, lookup);
          TimeCoordWithInterval timeCoordIntv = new TimeCoordWithInterval(validTime, start, intv2);
          if (!timeIntvs.contains(timeCoordIntv))   // LOOK case when multiple validTimes with different intervals
            timeIntvs.add(timeCoordIntv);
        }
        if (same) constantInterval = intv;
        Collections.sort(timeIntvs);
        return;
      }
    }

    // non - interval case
    // get list of unique times
    times = new ArrayList<Date>();
    for (GridRecord record : records) {
      Date validTime = getValidTime(record, lookup);
      if (!times.contains(validTime)) {
        times.add(validTime);
      }
    }
    Collections.sort(times);
  }

  /**
   * Create a new GridTimeCoord with the name, forecast times and lookup
   *
   * @param name        name
   * @param offsetHours forecast hours
   * @param lookup      lookup table
   * @deprecated dont use definition files as of 4.2
   */
  GridTimeCoord(String name, double[] offsetHours, GridTableLookup lookup) {
    this();
    this.name = name;
    //this.offsetHours = offsetHours;
    this.lookup = lookup;

    Date baseTime = lookup.getFirstBaseTime();
    DateFormatter formatter = new DateFormatter();
    String refDate = formatter.toDateTimeStringISO(baseTime);

    // the offset hours are reletive to whatever the base date is
    DateUnit convertUnit = null;
    try {
      convertUnit = new DateUnit("hours since " + refDate);
    } catch (Exception e) {
      log.error("TimeCoord not added, cant make DateUnit from String 'hours since " + refDate + "'", e);
      return;
    }

    // now create a list of valid dates
    times = new ArrayList<Date>(offsetHours.length);
    for (double offsetHour : offsetHours) {
      times.add(convertUnit.makeDate(offsetHour));
    }
  }

  /**
   * match time values - can list of GridRecords use this coordinate?
   *
   * @param records list of records
   * @return true if they are the same as this
   */
  boolean matchTimes(List<GridRecord> records) {
    // make sure that the base times and time units agree
    for (GridRecord record : records) {
      Date ref = record.getReferenceTime();
      if (!baseDate.equals(ref))
        return false;
      if (this.timeUnit != record.getTimeUnitName())
        return false;
    }

    // check intervals match
    if (records.get(0) instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) records.get(0);
      if (ggr.isInterval() != isInterval()) return false;
    }


    if (isInterval()) {
      // first create a new list
      List<TimeCoordWithInterval> timeList = new ArrayList<TimeCoordWithInterval>(records.size());
      for (GridRecord record : records) {
        GribGridRecord ggr = (GribGridRecord) record;
        int start = ggr.startOfInterval;
        int end = ggr.forecastTime;
        int intv2 = end - start;
        Date validTime = getValidTime(record, lookup);
        TimeCoordWithInterval timeCoordIntv = new TimeCoordWithInterval(validTime, start, intv2);
        if (!timeList.contains(timeCoordIntv)) {
          timeList.add(timeCoordIntv);
        }
      }

      Collections.sort(timeList);
      return timeList.equals(timeIntvs);

    } else {
      // first create a new list
      List<Date> timeList = new ArrayList<Date>(records.size());
      for (GridRecord record : records) {
        Date validTime = getValidTime(record, lookup);
        if (!timeList.contains(validTime)) {
          timeList.add(validTime);
        }
      }

      Collections.sort(timeList);
      return timeList.equals(times);
    }

  }

  /**
   * Set the sequence number
   *
   * @param seq the sequence number
   */
  void setSequence(int seq) {
    this.seq = seq;
  }

  /**
   * Get the name
   *
   * @return the name
   */
  String getName() {
    if (name != null) return name;
    return (seq == 0) ? "time" : "time" + seq;
  }

  /**
   * Add this as a dimension to a netCDF file
   *
   * @param ncfile the netCDF file
   * @param g      the group in the file
   */
  void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
    //Collections.sort(times);
    ncfile.addDimension(g, new Dimension(getName(), getNTimes(), true));
  }

  /**
   * Add this as a variable to the netCDF file
   *
   * @param ncfile the netCDF file
   * @param g      the group in the file
   */
  void addToNetcdfFile(NetcdfFile ncfile, Group g) {
    Variable v = new Variable(ncfile, g, null, getName());
    v.setDataType(DataType.INT);

    Date baseTime = lookup.getFirstBaseTime();
    //String timeUnit = lookup.getFirstTimeRangeUnitName();
    // String timeUnit = lookup.getTimeRangeUnitName(this.timeUnit);

    DateFormatter formatter = new DateFormatter();
    String refDate = formatter.toDateTimeStringISO(baseTime);
    String udunit = timeUnit + " since " + refDate;
    DateUnit dateUnit = null;
    try {
      dateUnit = new DateUnit(udunit);
    } catch (Exception e) {
      log.error("TimeCoord not added, cant make DateUnit from String '" + udunit + "'", e);
      return;
    }

    // create the data
    Array coordArray = null;
    Array boundsArray = null;
    int ntimes = getNTimes();
    int[] coordData = new int[ntimes];
    if (!isInterval()) {
      for (int i = 0; i < times.size(); i++)
        coordData[i] = (int) dateUnit.makeValue(times.get(i)); // LOOK why int ?
      coordArray = Array.factory(DataType.INT, new int[]{ntimes}, coordData);

    } else {
      int[] boundsData = new int[ntimes * 2];
      for (int i = 0; i < timeIntvs.size(); i++) {
        TimeCoordWithInterval tintv = timeIntvs.get(i);
        coordData[i] = tintv.start + tintv.interval; // end
        boundsData[2 * i + 1] = tintv.start + tintv.interval; // end
        boundsData[2 * i] = tintv.start; // start
      }
      coordArray = Array.factory(DataType.INT, new int[]{ntimes}, coordData);
      boundsArray = Array.factory(DataType.INT, new int[]{ntimes, 2}, boundsData);
    }

    v.setDimensions(v.getShortName());
    v.setCachedData(coordArray, false);

    if (!isInterval()) {
      v.addAttribute(new Attribute("long_name", "forecast time"));
      v.addAttribute(new Attribute("units", timeUnit + " since " + refDate));

    } else {
      Formatter intervalName = new Formatter();
      if (constantInterval < 0)
        intervalName.format("(mixed intervals)");
      else
        intervalName.format("(%d %s intervals)", constantInterval, this.timeUnit);
      v.addAttribute(new Attribute("long_name", "forecast time for " + intervalName.toString()));
      v.addAttribute(new Attribute("units", timeUnit + " since " + refDate));
      v.addAttribute(new Attribute("bounds", getName() + "_bounds"));

      // add times bound variable
      if (g == null) g = ncfile.getRootGroup();
      if (g.findDimension("ncell") == null)
        ncfile.addDimension(g, new Dimension("ncell", 2, true));

      Variable vb = new Variable(ncfile, g, null, getName() + "_bounds");
      vb.setDataType(DataType.INT);
      vb.setDimensions(getName() + " ncell");
      vb.addAttribute(new Attribute("long_name", "bounds for " + getName()));
      vb.addAttribute(new Attribute("units", timeUnit + " since " + refDate));

      // add data
      vb.setCachedData(boundsArray, false);
      ncfile.addVariable(g, vb);
    }

    Date d = lookup.getFirstBaseTime();
    if (lookup instanceof Grib2GridTableLookup) {
      Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
      v.addAttribute(new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO(d)));
      v.addAttribute(new Attribute("GRIB2_significanceOfRTName", g2lookup.getFirstSignificanceOfRTName()));
    } else if (lookup instanceof Grib1GridTableLookup) {
      Grib1GridTableLookup g1lookup = (Grib1GridTableLookup) lookup;
      v.addAttribute(new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO(d)));
      v.addAttribute(new Attribute("GRIB2_significanceOfRTName", g1lookup.getFirstSignificanceOfRTName()));
    }
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    ncfile.addVariable(g, v);
  }

  /**
   * Get the index of a GridRecord in the list of times
   *
   * @param record the GridRecord
   * @return the index in the list of time values, or -1 if not found
   */
  int getIndex(GridRecord record) {
    Date validTime = getValidTime(record, lookup);
    if (!isInterval())
      return times.indexOf(validTime);
    else {
      int index = 0;
      for (TimeCoordWithInterval t : timeIntvs) {
        if (t.coord.equals(validTime)) return index;
        index++;
      }
      return -1;
    }
  }

  /**
   * Get the valid time for a GridRecord
   *
   * @param record the record
   * @return the valid time
   */
  Date getValidTime(GridRecord record) {
    return getValidTime(record, lookup);
  }

  /**
   * Get the number of times
   *
   * @return the number of times
   */
  int getNTimes() {
    return isInterval() ? timeIntvs.size() : times.size();
  }

  /**
   * Get IntervalLength
   *
   * @return IntervalLength
   */
  int getConstantInterval() {
    return constantInterval;
  }

  /**
   * Get TimeUnit
   *
   * @return TimeUnit
   */
  String getTimeUnit() {
    return timeUnit;
  }

  /**
   * is this a mixed interval
   *
   * @return mixed
   */
  boolean isInterval() {
    return timeIntvs != null;
  }

  /**
   * Get the valid time for this record
   *
   * @param record the record in question
   * @param lookup the lookup table
   * @return the valid time
   */
  private Date getValidTime(GridRecord record, GridTableLookup lookup) {
    if (record.getValidTime() != null) {
      return record.getValidTime();
    }

    /* try {
      validTime = formatter.getISODate(record.getReferenceTime().toString());
    } catch (Throwable e) {
      log.error("getValidTime(" + record.getReferenceTime() + ")", e);
      return null;
    } */

    int calandar_unit = Calendar.HOUR;
    int factor = 1;
    // TODO: this is not always correct but this code doesn't get executed often, can we delete
    String timeUnit = lookup.getFirstTimeRangeUnitName();
    //String timeUnit = lookup.lookup.getTimeRangeUnitName( this.tunit ); //should be

    if (timeUnit.equalsIgnoreCase("hour") || timeUnit.equalsIgnoreCase("hours")) {
      factor = 1;  // common case
    } else if (timeUnit.equalsIgnoreCase("minutes") || timeUnit.equalsIgnoreCase("minute")) {
      calandar_unit = Calendar.MINUTE;
    } else if (timeUnit.equalsIgnoreCase("second") || timeUnit.equalsIgnoreCase("secs")) {
      calandar_unit = Calendar.SECOND;
    } else if (timeUnit.equalsIgnoreCase("day") || timeUnit.equalsIgnoreCase("days")) {
      factor = 24;
    } else if (timeUnit.equalsIgnoreCase("month") || timeUnit.equalsIgnoreCase("months")) {
      factor = 24 * 30;  // ??
    } else if (timeUnit.equalsIgnoreCase("year") || timeUnit.equalsIgnoreCase("years") || timeUnit.equalsIgnoreCase("1year")) {
      factor = 24 * 365;        // ??
    } else if (timeUnit.equalsIgnoreCase("decade")) {
      factor = 24 * 365 * 10;   // ??
    } else if (timeUnit.equalsIgnoreCase("century")) {
      factor = 24 * 365 * 100;  // ??
    } else if (timeUnit.equalsIgnoreCase("3hours")) {
      factor = 3;
    } else if (timeUnit.equalsIgnoreCase("6hours")) {
      factor = 6;
    } else if (timeUnit.equalsIgnoreCase("12hours")) {
      factor = 12;
    }

    calendar.setTime(record.getReferenceTime());
    calendar.add(calandar_unit, factor * record.getValidTimeOffset());
    return calendar.getTime();
  }

}
