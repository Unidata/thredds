/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

// $Id:GridTimeCoord.java 63 2006-07-12 21:50:51Z edavis $

package ucar.nc2.iosp.grid;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;

import java.util.*;


/**
 * A Time Coordinate for a Grid dataset.
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class GridTimeCoord {

    /** logger */
    static private org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(GridTimeCoord.class);

    // for parsing dates

    /** date formatter */
    private DateFormatter formatter = new DateFormatter();

    /** calendar */
    private Calendar calendar;

    /** name */
    private String name;

    /** lookup table */
    private GridTableLookup lookup;

    /** list of times */
    private ArrayList times = new ArrayList();  //  Date
    //private double[] offsetHours;

    /** sequence # */
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
     * @param records  records to use
     * @param lookup   lookup table
     */
    GridTimeCoord(List records, GridTableLookup lookup) {
        this();
        this.lookup = lookup;
        addTimes(records);
        Collections.sort(times);
    }

    /**
     * Create a new GridTimeCoord with the name, forecast times and  lookup
     *
     * @param name   name
     * @param offsetHours   forecast hours
     * @param lookup        lookup table
     */
    GridTimeCoord(String name, double[] offsetHours, GridTableLookup lookup) {
        this();
        this.name = name;
        //this.offsetHours = offsetHours;
        this.lookup = lookup;

        Date   baseTime = lookup.getFirstBaseTime();
        String refDate  = formatter.toDateTimeStringISO(baseTime);

        // the offset hours are reletive to whatever the base date is
        DateUnit convertUnit = null;
        try {
            convertUnit = new DateUnit("hours since " + refDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // now create a list of valid dates
        times = new ArrayList(offsetHours.length);
        for (int i = 0; i < offsetHours.length; i++) {
            double offsetHour = offsetHours[i];
            times.add(convertUnit.makeDate(offsetHour));
        }
    }

    /**
     * Add the times from the list of records
     *
     * @param records   list of records
     */
    void addTimes(List records) {
        for (int i = 0; i < records.size(); i++) {
            GridRecord record    = (GridRecord) records.get(i);
            Date       validTime = getValidTime(record, lookup);
            if ( !times.contains(validTime)) {
                times.add(validTime);
            }
        }
    }

    /**
     * match levels
     *
     * @param records  list of records
     *
     * @return true if they are the same as this
     */
    boolean matchLevels(List records) {

        // first create a new list
        ArrayList timeList = new ArrayList(records.size());
        for (int i = 0; i < records.size(); i++) {
            GridRecord record    = (GridRecord) records.get(i);
            Date       validTime = getValidTime(record, lookup);
            if ( !timeList.contains(validTime)) {
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
     * @param ncfile  the netCDF file
     * @param g       the group in the file
     */
    void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
        Collections.sort(times);
        ncfile.addDimension(g, new Dimension(getName(), getNTimes(), true));
    }

    /**
     * Add this as a variable to the netCDF file
     *
     * @param ncfile  the netCDF file
     * @param g       the group in the file
     */
    void addToNetcdfFile(NetcdfFile ncfile, Group g) {
        Variable v = new Variable(ncfile, g, null, getName());
        v.setDataType(DataType.INT);
        v.addAttribute(new Attribute("long_name", "forecast time"));
        //v.addAttribute( new Attribute("standard_name", "forecast_reference_time"));

        int      ntimes   = getNTimes();
        int[]    data     = new int[ntimes];

        Date     baseTime = lookup.getFirstBaseTime();
        String   timeUnit = lookup.getFirstTimeRangeUnitName();
        String   refDate  = formatter.toDateTimeStringISO(baseTime);
        DateUnit dateUnit = null;
        try {
            dateUnit = new DateUnit(timeUnit + " since " + refDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // convert the date into the time unit.
        for (int i = 0; i < times.size(); i++) {
            Date validTime = (Date) times.get(i);
            data[i] = (int) dateUnit.makeValue(validTime);
        }
        Array dataArray = Array.factory(DataType.INT,
                                        new int[] { ntimes }, data);

        v.setDimensions(v.getShortName());
        v.setCachedData(dataArray, false);

        Date d = lookup.getFirstBaseTime();

        v.addAttribute(new Attribute("units",
                                     timeUnit + " since " + refDate));
        //v.addAttribute( new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO( d)));
        //v.addAttribute( new Attribute("GRIB2_significanceOfRTName", lookup.getFirstSignificanceOfRTName()));
        v.addAttribute(new Attribute(_Coordinate.AxisType,
                                     AxisType.Time.toString()));

        ncfile.addVariable(g, v);
    }

    /**
     * Get the index of a GridRecord
     *
     * @param record  the record
     *
     * @return  the index or -1 if not found
     */
    int getIndex(GridRecord record) {
        Date validTime = getValidTime(record, lookup);
        return times.indexOf(validTime);
    }

    /**
     * Get the valid time for a GridRecord
     *
     * @param record   the record
     *
     * @return  the valid time
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
     * @param record   the record in question
     * @param lookup   the lookup table
     *
     * @return  the valid time
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

        int    calandar_unit = Calendar.HOUR;
        int    factor        = 1;
        String timeUnit      = lookup.getFirstTimeRangeUnitName();

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
            // TODO: fix this in GRIB world
            factor = 12;
        }

        calendar.setTime(validTime);
        calendar.add(calandar_unit, factor * record.getValidTimeOffset());
        validTime = calendar.getTime();

        // TODO: should this just be done when the record is created?
        //record.setValidTime(validTime);
        return validTime;
    }

}

