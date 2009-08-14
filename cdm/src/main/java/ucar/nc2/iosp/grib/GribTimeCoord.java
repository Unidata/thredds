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
package ucar.nc2.iosp.grib;

import ucar.grib.Index;
import ucar.grib.TableLookup;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;

import java.util.*;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * A Time Coordinate for a Grib dataset.
 * @author caron
 * @deprecated
 */
public class GribTimeCoord {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribTimeCoord.class);

    // for parsing dates
  private DateFormatter formatter = new DateFormatter();
  private Calendar calendar;

  private String name;
  private TableLookup lookup;
  private List<Date> times = new ArrayList<Date>(); //  Date
  //private double[] offsetHours;
  private int seq = 0;

  GribTimeCoord() {
     // need to have this non-static for thread safety
    calendar = Calendar.getInstance();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  GribTimeCoord(List<Index.GribRecord> records, TableLookup lookup) {
    this();
    this.lookup = lookup;
    addTimes( records);
    Collections.sort( times);
  }

  GribTimeCoord(String name, double[] offsetHours, TableLookup lookup) {
    this();
    this.name = name;
    //this.offsetHours = offsetHours;
    this.lookup = lookup;

    Date baseTime = lookup.getFirstBaseTime();
    String refDate = formatter.toDateTimeStringISO( baseTime);

    // the offset hours are reletive to whatever the base date is
    DateUnit convertUnit;
    try {
      convertUnit = new DateUnit("hours since "+ refDate);
    } catch (Exception e) {
      log.error("TimeCoord not added, cant make DateUnit from String 'hours since "+ refDate+"'",e);
      return;
    }

    // now create a list of valid dates
    times = new ArrayList<Date>( offsetHours.length);
    for (double offsetHour : offsetHours) {
      times.add(convertUnit.makeDate(offsetHour));
    }
  }

  void addTimes( List<Index.GribRecord> records) {
    for (Index.GribRecord record : records) {
      Date validTime = getValidTime(record, lookup);
      if (!times.contains(validTime))
        times.add(validTime);
    }
  }

  boolean matchLevels( List<Index.GribRecord> records) {

    // first create a new list
    List<Date> timeList = new ArrayList<Date>( records.size());
    for (Index.GribRecord record : records) {
      Date validTime = getValidTime(record, lookup);
      if (!timeList.contains(validTime))
        timeList.add(validTime);
    }

    Collections.sort( timeList );
    boolean isEqual = timeList.equals( times);
    return isEqual;
  }

  void setSequence( int seq) { this.seq = seq; }

  String getName() {
    if (name != null) return name;
    return (seq == 0) ? "time" : "time"+seq;
  }

  void addDimensionsToNetcdfFile( NetcdfFile ncfile, Group g) {
    Collections.sort( times);
    ncfile.addDimension(g, new Dimension(getName(), getNTimes()));
  }

  void addToNetcdfFile( NetcdfFile ncfile, Group g) {
    Variable v = new Variable( ncfile, g, null, getName());
    v.setDataType( DataType.INT);
    v.addAttribute( new Attribute("long_name", "forecast time"));
    //v.addAttribute( new Attribute("standard_name", "forecast_reference_time"));

    int ntimes  = getNTimes();
    int[] data = new int[ntimes];

    Date baseTime = lookup.getFirstBaseTime();
    String timeUnit =  lookup.getFirstTimeRangeUnitName();
    String refDate = formatter.toDateTimeStringISO( baseTime);
    DateUnit dateUnit;
    try {
      dateUnit = new DateUnit(timeUnit+" since "+ refDate);
    } catch (Exception e) {
      log.error("TimeCoord not added, cant make DateUnit from String '"+timeUnit+" since "+ refDate+"'",e);
      return;
    }

    // convert the date into the time unit.
    for (int i = 0; i < times.size(); i++) {
      Date validTime = times.get(i);
      data[i] = (int) dateUnit.makeValue( validTime);
    }
    Array dataArray = Array.factory( DataType.INT, new int [] {ntimes}, data);

    v.setDimensions( v.getShortName());
    v.setCachedData(dataArray, false);

    Date d = lookup.getFirstBaseTime();

    v.addAttribute( new Attribute("units", timeUnit+" since "+ refDate));
    v.addAttribute( new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO( d)));
    v.addAttribute( new Attribute("GRIB2_significanceOfRTName", lookup.getFirstSignificanceOfRTName()));
    v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    ncfile.addVariable( g, v);

    /*  debug time coords
    try {
      NCdumpW.printArray(dataArray, "Added time coord "+v.getName(), new PrintWriter(System.out), null);
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } */
  }

  int getIndex(Index.GribRecord record) {
    Date validTime = getValidTime(record, lookup);
    return times.indexOf( validTime);
  }

  Date getValidTime(Index.GribRecord record) {
    return getValidTime(record, lookup);
  }

  int getNTimes() {
    return times.size();
  }

  private Date getValidTime(Index.GribRecord record, TableLookup lookup) {
    Date validTime = record.getValidTime();
    if (validTime != null)
      return validTime;

    try {
      validTime = record.refTime;
    } catch (Throwable e) {
      log.error("getValidTime("+record.refTime+")", e);
      return null;
    }

    int calendar_unit = Calendar.HOUR;
    int factor = 1;
    String timeUnit = lookup.getFirstTimeRangeUnitName();

    if (timeUnit.equalsIgnoreCase("hour") || timeUnit.equalsIgnoreCase("hours")) {
      factor = 1;  // common case
    } else if (timeUnit.equalsIgnoreCase("minutes") || timeUnit.equalsIgnoreCase("minute")) {
      calendar_unit = Calendar.MINUTE;
    } else if (timeUnit.equalsIgnoreCase("second") || timeUnit.equalsIgnoreCase("secs")) {
      calendar_unit = Calendar.SECOND;
    } else if (timeUnit.equalsIgnoreCase("day") || timeUnit.equalsIgnoreCase("days")) {
      factor = 24;
    } else if (timeUnit.equalsIgnoreCase("month") || timeUnit.equalsIgnoreCase("months")) {
      factor = 24 * 30; // ??
    } else if (timeUnit.equalsIgnoreCase("year") || timeUnit.equalsIgnoreCase("years")|| timeUnit.equalsIgnoreCase("1year")) {
      factor = 24 * 365; // ??
    } else if (timeUnit.equalsIgnoreCase("decade")) {
      factor = 24 * 365 * 10; // ??
    } else if (timeUnit.equalsIgnoreCase("century")) {
      factor = 24 * 365 * 100; // ??
    } else if (timeUnit.equalsIgnoreCase("3hours")) {
      factor = 3;
    } else if (timeUnit.equalsIgnoreCase("6hours")) {
      factor = 6;
    } else if (timeUnit.equalsIgnoreCase("12hours")) {
      factor = 12;
    }

    calendar.setTime(validTime);
    calendar.add(calendar_unit, factor * record.forecastTime);
    validTime = calendar.getTime();

    record.setValidTime(validTime);
    return validTime;
  }

}