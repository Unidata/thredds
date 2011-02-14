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

import ucar.grib.GribPds;
import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.grid.GridRecord;
import ucar.grib.GribGridRecord;

import java.util.*;

/**
 * A Time Coordinate for a Grid dataset.
 *
 * @author caron
 */
public class GridTimeCoord implements Comparable<GridTimeCoord> {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridTimeCoord.class);

  //private GridTableLookup lookup;
  private int seq = 0; // for getting a unique name
  private String timeUdunit;
  private int timeUnit;

  private Date baseDate;
  private List<Date> times;
  private List<TimeCoordWithInterval> timeIntvs;
  private int constantInterval = -1;
  private int[] coordData;

  /**
   * Create a new GridTimeCoord from the list of GridRecord
   *
   * @param records records to use
   * @param where file location for warn message
   */
  GridTimeCoord(List<GridRecord> records, String where) {

    // check time units, get earliest reference date
    for (GridRecord record : records) {
      if (this.baseDate == null) {
        this.baseDate = record.getReferenceTime();
        this.timeUdunit = record.getTimeUdunitName();
        this.timeUnit = record.getTimeUnit();
        //System.out.printf("%s%n", record.getParameterDescription());
      }

      // use earlier reference date LOOK WHY?
      Date ref = record.getReferenceTime();
      if (ref.before(this.baseDate))
        this.baseDate = ref;
    }

    // interval case - only GRIB
    if (records.get(0) instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) records.get(0);

      if (ggr.isInterval()) {
        timeIntvs = new ArrayList<TimeCoordWithInterval>();

        boolean same = true;
        int intv = -1;
        for (GridRecord gr : records) {
          ggr = (GribGridRecord) gr;

          // make sure that the reference date agrees
          Date ref = gr.getReferenceTime();
          if (!baseDate.equals(ref)) {
            log.warn(gr + " does not have same base date= " + baseDate + " != " + ref+ " for " + where);
            // continue; LOOK
          }

          GribPds pds = ggr.getPds();
          int[] timeInv = pds.getForecastTimeInterval(this.timeUnit);
          int start = timeInv[0];
          int end = timeInv[1];
          int intv2 = end - start;

          if (intv2 > 0) { // skip those weird zero-intervals when testing for constant interval
            if (intv < 0) intv = intv2;
            else same = same && (intv == intv2);
          }

          Date validTime = gr.getValidTime();
          TimeCoordWithInterval timeCoordIntv = new TimeCoordWithInterval(validTime, start, intv2);
          if (!timeIntvs.contains(timeCoordIntv))
            timeIntvs.add(timeCoordIntv);
        }
        if (same) constantInterval = intv;
        Collections.sort(timeIntvs);
        return;
      }
    }

    // non - interval case
    // get list of unique valid times
    times = new ArrayList<Date>();
    for (GridRecord gr : records) {
      Date validTime = gr.getValidTime();
      if (validTime == null) validTime = gr.getReferenceTime();
      if (!times.contains(validTime)) {
        times.add(validTime);
      }
    }
    Collections.sort(times);
  }

  /**
   * match time values - can this list of GridRecords use this coordinate?
   *
   * @param records list of records
   * @return true if they are the same as this
   */
  boolean matchTimes(List<GridRecord> records) {
    // make sure that the time units agree
    for (GridRecord record : records) {
      if (!this.timeUdunit.equals(record.getTimeUdunitName()))
        return false;
    }

    // check intervals match
    if (records.get(0) instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) records.get(0);
      if (ggr.isInterval() != isInterval())
        return false;
    }

    if (isInterval()) {
      // first create a new list
      List<TimeCoordWithInterval> timeList = new ArrayList<TimeCoordWithInterval>(records.size());
      for (GridRecord record : records) {
        // make sure that the base times agree
        Date ref = record.getReferenceTime();
        if (!baseDate.equals(ref))
          return false;

        GribGridRecord ggr = (GribGridRecord) record;
        GribPds pds = ggr.getPds();
        int[] timeInv = pds.getForecastTimeInterval(this.timeUnit);

        int start = timeInv[0];
        int end = timeInv[1];
        int intv2 = end - start;
        Date validTime = record.getValidTime();
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
        Date validTime = record.getValidTime();
        if (validTime == null)
          validTime = record.getReferenceTime();
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

    //Date baseTime = lookup.getFirstBaseTime();
    //String timeUnit = lookup.getFirstTimeRangeUnitName();
    // String timeUnit = lookup.getTimeRangeUnitName(this.timeUnit);

    DateFormatter formatter = new DateFormatter();
    String refDate = formatter.toDateTimeStringISO(baseDate);
    String udunit = timeUdunit + " since " + refDate;
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
    coordData = new int[ntimes];
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
      v.addAttribute(new Attribute("units", timeUdunit + " since " + refDate));

    } else {
      Formatter intervalName = new Formatter();
      if (constantInterval < 0)
        intervalName.format("(mixed intervals)");
      else
        intervalName.format("(%d %s intervals)", constantInterval, this.timeUdunit);
      v.addAttribute(new Attribute("long_name", "forecast time for " + intervalName.toString()));
      v.addAttribute(new Attribute("units", timeUdunit + " since " + refDate));
      v.addAttribute(new Attribute("bounds", getName() + "_bounds"));

      // add times bound variable
      if (g == null) g = ncfile.getRootGroup();
      Dimension bd = ucar.nc2.dataset.DatasetConstructor.getBoundsDimension(ncfile);

      Variable vb = new Variable(ncfile, g, null, getName() + "_bounds");
      vb.setDataType(DataType.INT);
      vb.setDimensions(getName() + " "+ bd.getName());
      vb.addAttribute(new Attribute("long_name", "bounds for " + getName()));
      vb.addAttribute(new Attribute("units", timeUdunit + " since " + refDate));

      // add data
      vb.setCachedData(boundsArray, false);
      ncfile.addVariable(g, vb);
    }

    /* Date d = lookup.getFirstBaseTime();
    if (lookup instanceof Grib2GridTableLookup) {
      Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
      v.addAttribute(new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO(d)));
      v.addAttribute(new Attribute("GRIB2_significanceOfRTName", g2lookup.getFirstSignificanceOfRTName()));
    } else if (lookup instanceof Grib1GridTableLookup) {
      Grib1GridTableLookup g1lookup = (Grib1GridTableLookup) lookup;
      v.addAttribute(new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO(d)));
      v.addAttribute(new Attribute("GRIB2_significanceOfRTName", g1lookup.getFirstSignificanceOfRTName()));
    }
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));  */

    ncfile.addVariable(g, v);
  }

  /**
   * Find the index of a GridRecord in the list of times
   *
   * @param record the GridRecord
   * @return the index in the list of time values, or -1 if not found
   */
  int findIndex(GridRecord record) {
    Date validTime = record.getValidTime();
    if (!isInterval())
      return times.indexOf(validTime);
    else {
      int index = 0;
      for (TimeCoordWithInterval t : timeIntvs) {
        GribGridRecord ggr = (GribGridRecord) record;  // only true for Grib      
        GribPds pds = ggr.getPds();
        int[] intv = pds.getForecastTimeInterval(timeUnit); // may need to convert units
        int diff = intv[1] - intv[0];
        if (t.coord.equals(validTime) && t.interval == diff) return index;
        index++;
      }
      return -1;
    }
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
    return timeUdunit;
  }

  /**
   * is this a mixed interval ?
   * Only true for Grib
   *
   * @return mixed
   */
  boolean isInterval() {
    return timeIntvs != null;
  }

  public String getCoord(int i) {
    if (timeIntvs == null)
      return coordData[i]+" ";
    else {
      TimeCoordWithInterval ti = timeIntvs.get(i);
      return coordData[i]+"=" + ti.start+"/"+ti.interval;
    }
  }

  @Override
  public int compareTo(GridTimeCoord o) {
    return o.getNTimes() - getNTimes(); // reverse sort on number of coords
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
      return (diff == 0) ? (o.interval - interval) : diff;  // longer intervals first
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

    @Override
    public String toString() {
      return "start=" + start +", interval=" + interval;
    }
  }

}
