/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.grib;

import ucar.grib.Index;
import ucar.grib.TableLookup;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;

import java.util.*;

/**
 * A Time Coordinate for a Grib dataset.
 * @author caron
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
    return timeList.equals( times);
  }

  void setSequence( int seq) { this.seq = seq; }

  String getName() {
    if (name != null) return name;
    return (seq == 0) ? "time" : "time"+seq;
  }

  void addDimensionsToNetcdfFile( NetcdfFile ncfile, Group g) {
    Collections.sort( times);
    ncfile.addDimension(g, new Dimension(getName(), getNTimes(), true));
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
    Array dataArray = Array.factory( DataType.INT.getClassType(), new int [] {ntimes}, data);

    v.setDimensions( v.getShortName());
    v.setCachedData(dataArray, false);

    Date d = lookup.getFirstBaseTime();

    v.addAttribute( new Attribute("units", timeUnit+" since "+ refDate));
    v.addAttribute( new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO( d)));
    v.addAttribute( new Attribute("GRIB2_significanceOfRTName", lookup.getFirstSignificanceOfRTName()));
    v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    ncfile.addVariable( g, v);
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
      validTime = formatter.getISODate( record.refTime);
    } catch (Throwable e) {
      log.error("getValidTime("+record.refTime+")", e);
      return null;
    }

    int calandar_unit = Calendar.HOUR;
    int factor = 1;
    String timeUnit = lookup.getFirstTimeRangeUnitName();

    if (timeUnit.equalsIgnoreCase("hour") || timeUnit.equalsIgnoreCase("hours")) {
      factor = 1;  // common case
    } else if (timeUnit.equalsIgnoreCase("minutes") || timeUnit.equalsIgnoreCase("minute")) {
      calandar_unit = Calendar.MINUTE;
    } else if (timeUnit.equalsIgnoreCase("second") || timeUnit.equalsIgnoreCase("secs")) {
      calandar_unit = Calendar.SECOND;
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
      factor = 6;
    }

    calendar.setTime(validTime);
    calendar.add(calandar_unit, factor * record.forecastTime);
    validTime = calendar.getTime();

    record.setValidTime(validTime);
    return validTime;
  }

}