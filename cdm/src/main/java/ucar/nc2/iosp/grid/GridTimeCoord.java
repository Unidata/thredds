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
import ucar.grib.GribNumbers;
import ucar.grib.GribGridRecord;

import java.util.*;


/**
 * A Time Coordinate for a Grid dataset.
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class GridTimeCoord {

  /**
   * logger
   */
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridTimeCoord.class);

  // for parsing dates

  /**
   * date formatter
   */
  private DateFormatter formatter = new DateFormatter();

  /**
   * calendar
   */
  private Calendar calendar;

  /**
   * name
   */
  private String name;

  /**
   * lookup table
   */
  private GridTableLookup lookup;

  /**
   * list of times
   */
  private List<Date> times = new ArrayList<Date>();  //  Date
  //private double[] offsetHours;

  /**
   * interval length
   */
  private int intervalLength = GribNumbers.UNDEFINED;

  /**
   * sequence #
   */
  private int seq = 0;

  /**
   * Create a new GridTimeCoord
   */
  GridTimeCoord() {
    // need to have this non-static for thread safety
    calendar = Calendar.getInstance();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  /**
   * Create a new GridTimeCoord with the list of records
   *
   * @param records records to use
   * @param lookup  lookup table
   */
  GridTimeCoord(List<GridRecord> records, GridTableLookup lookup) {
    this();
    this.lookup = lookup;
    if (records.get(0) instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) records.get(0);
      if (ggr.startOfInterval != GribNumbers.UNDEFINED &&
              ggr.productType > 7 && ggr.productType < 16)
        intervalLength = ggr.forecastTime - ggr.startOfInterval;
    }
    addTimes(records);
    Collections.sort(times);
  }

  /**
   * Create a new GridTimeCoord with the name, forecast times and  lookup
   *
   * @param name        name
   * @param offsetHours forecast hours
   * @param lookup      lookup table
   */
  GridTimeCoord(String name, double[] offsetHours, GridTableLookup lookup) {
    this();
    this.name = name;
    //this.offsetHours = offsetHours;
    this.lookup = lookup;

    Date baseTime = lookup.getFirstBaseTime();
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
   * Add the times from the list of records
   *
   * @param records list of records
   */
  void addTimes(List<GridRecord> records) {
    for (GridRecord record : records) {
      Date validTime = getValidTime(record, lookup);
      if (!times.contains(validTime)) {
        times.add(validTime);
      }
    }
  }

  /**
   * match levels
   *
   * @param records list of records
   * @return true if they are the same as this
   */
  boolean matchLevels(List<GridRecord> records) {

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
    if (name != null) {
      return name;
    }
    return (seq == 0)
            ? "time"
            : "time" + seq;
  }

  /**
   * Add this as a dimension to a netCDF file
   *
   * @param ncfile the netCDF file
   * @param g      the group in the file
   */
  void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
    Collections.sort(times);
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

    int ntimes = getNTimes();
    int[] data = new int[ntimes];

    Date baseTime = lookup.getFirstBaseTime();
    String timeUnit = lookup.getFirstTimeRangeUnitName();
    String refDate = formatter.toDateTimeStringISO(baseTime);
    DateUnit dateUnit = null;
    try {
      dateUnit = new DateUnit(timeUnit + " since " + refDate);
    } catch (Exception e) {
      log.error("TimeCoord not added, cant make DateUnit from String '" + timeUnit + " since " + refDate + "'", e);
      return;
    }

    // convert the date into the time unit.
    for (int i = 0; i < times.size(); i++) {
      Date validTime = (Date) times.get(i);
      data[i] = (int) dateUnit.makeValue(validTime);
    }
    Array dataArray = Array.factory(DataType.INT, new int[]{ntimes}, data);
    v.setDimensions(v.getShortName());
    v.setCachedData(dataArray, false);

    if (intervalLength == GribNumbers.UNDEFINED) {
      v.addAttribute(new Attribute("long_name", "forecast time"));
      v.addAttribute(new Attribute("units", timeUnit + " since " + refDate));
    } else {
      String interval = Integer.toString(intervalLength) +
              lookup.getFirstTimeRangeUnitName() + " intervals";
      v.addAttribute(new Attribute("long_name", "time for " + interval));
      v.addAttribute(new Attribute("units", timeUnit + " since " + refDate));
      v.addAttribute(new Attribute("bounds", getName() + "_bounds"));

      // add times bound variable
      Variable vb = new Variable(ncfile, g, null, getName() + "_bounds");
      vb.setDataType(DataType.INT);
      if (g == null) {
        g = ncfile.getRootGroup();
      }
      if (g.findDimension("ncell") == null) {
        ncfile.addDimension(g, new Dimension("ncell", 2, true));
      }
      vb.setDimensions(getName() + " ncell");

      vb.addAttribute(new Attribute("long_name", interval));
      vb.addAttribute(new Attribute("units", timeUnit + " since " + refDate));
      // add data
      Array bdataArray = Array.factory(DataType.INT, new int[]{data.length, 2});
      ucar.ma2.Index ima = bdataArray.getIndex();
      for (int i = 0; i < data.length; i++) {
        bdataArray.setInt(ima.set(i, 0), data[i] - intervalLength);
        bdataArray.setInt(ima.set(i, 1), data[i]);
      }
      vb.setCachedData(bdataArray, true);
      ncfile.addVariable(g, vb);
    }

    Date d = lookup.getFirstBaseTime();
    if (lookup instanceof Grib2GridTableLookup) {
      Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
      v.addAttribute(new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO(d)));
      v.addAttribute(new Attribute("GRIB2_significanceOfRTName",
              g2lookup.getFirstSignificanceOfRTName()));
    } else if (lookup instanceof Grib1GridTableLookup) {
      Grib1GridTableLookup g1lookup = (Grib1GridTableLookup) lookup;
      v.addAttribute(new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO(d)));
      v.addAttribute(new Attribute("GRIB2_significanceOfRTName",
              g1lookup.getFirstSignificanceOfRTName()));
    }
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    ncfile.addVariable(g, v);
  }

  /**
   * Get the index of a GridRecord
   *
   * @param record the record
   * @return the index or -1 if not found
   */
  int getIndex(GridRecord record) {
    Date validTime = getValidTime(record, lookup);
    return times.indexOf(validTime);
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
    return times.size();
  }

  /**
   * Get the valid time for this record
   *
   * @param record the record in question
   * @param lookup the lookup table
   * @return the valid time
   */
  private Date getValidTime(GridRecord record, GridTableLookup lookup) {
    Date validTime = record.getValidTime();
    if (validTime != null) {
      return validTime;
    }

    try {
      validTime =
              formatter.getISODate(record.getReferenceTime().toString());
    } catch (Throwable e) {
      log.error("getValidTime(" + record.getReferenceTime() + ")", e);
      return null;
    }

    int calandar_unit = Calendar.HOUR;
    int factor = 1;
    String timeUnit = lookup.getFirstTimeRangeUnitName();

    if (timeUnit.equalsIgnoreCase("hour")
            || timeUnit.equalsIgnoreCase("hours")) {
      factor = 1;  // common case
    } else if (timeUnit.equalsIgnoreCase("minutes")
            || timeUnit.equalsIgnoreCase("minute")) {
      calandar_unit = Calendar.MINUTE;
    } else if (timeUnit.equalsIgnoreCase("second")
            || timeUnit.equalsIgnoreCase("secs")) {
      calandar_unit = Calendar.SECOND;
    } else if (timeUnit.equalsIgnoreCase("day")
            || timeUnit.equalsIgnoreCase("days")) {
      factor = 24;
    } else if (timeUnit.equalsIgnoreCase("month")
            || timeUnit.equalsIgnoreCase("months")) {
      factor = 24 * 30;  // ??
    } else if (timeUnit.equalsIgnoreCase("year")
            || timeUnit.equalsIgnoreCase("years")
            || timeUnit.equalsIgnoreCase("1year")) {
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

    calendar.setTime(validTime);
    calendar.add(calandar_unit, factor * record.getValidTimeOffset());
    validTime = calendar.getTime();

    return validTime;
  }

}
