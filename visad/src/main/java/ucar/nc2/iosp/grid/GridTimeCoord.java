/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.grid;

import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;

import java.util.*;

/**
 * A Time Coordinate for a Grid dataset.
 *
 * @author caron
 */
public class GridTimeCoord implements Comparable<GridTimeCoord> {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridTimeCoord.class);

  protected int seq = 0; // for getting a unique name
  protected String timeUdunit;
  protected int timeUnit;

  protected Date baseDate; // earliest base date
  protected boolean refDateDiffers = false;
  protected List<Date> times;
  protected List<TimeCoordWithInterval> timeIntvs; // only if interval coord
  protected int constantInterval = -1;
  protected int[] coordData;

  /**
   * Create a new GridTimeCoord from the list of GridRecord
   *
   * @param records records to use
   * @param where file location for warn message
   */
  protected GridTimeCoord(List<GridRecord> records, String where) {

    // check time units, get earliest reference date
    for (GridRecord record : records) {
      if (this.baseDate == null) {
        this.baseDate = record.getReferenceTime();
        this.timeUdunit = record.getTimeUdunitName();
        this.timeUnit = record.getTimeUnit();
      }

      Date ref = record.getReferenceTime();
      if (!baseDate.equals(ref)) {
        refDateDiffers = true;
        if (ref.before(this.baseDate)) // use earlier reference date
          this.baseDate = ref;
      }

      if (record.getTimeUnit() != this.timeUnit) {
        // ignore for now - 4.2 is apparently fixed
        //throw new IllegalStateException("time units must match");
        log.warn("time units mismatch {} != {}", record.getTimeUnit(),this.timeUnit);
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
  protected boolean matchTimes(List<GridRecord> records) {
    // make sure that the time units agree
    for (GridRecord record : records) {
      if (!this.timeUdunit.equals(record.getTimeUdunitName()))
        return false;
    }

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
  public String getName() {
    return (seq == 0) ? "time" : "time" + seq;
  }

  /**
   * Add this as a dimension to a netCDF file
   *
   * @param ncfile the netCDF file
   * @param g      the group in the file
   */
  void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
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
      vb.setDimensions(getName() + " "+ bd.getShortName());
      vb.addAttribute(new Attribute("long_name", "bounds for " + getName()));
      vb.addAttribute(new Attribute("units", timeUdunit + " since " + refDate));

      // add data
      vb.setCachedData(boundsArray, false);
      ncfile.addVariable(g, vb);
    }

    ncfile.addVariable(g, v);
  }

  /**
   * Find the index of a GridRecord in the list of times
   *
   * @param record the GridRecord
   * @return the index in the list of time values, or -1 if not found
   */
  public int findIndex(GridRecord record) {
    Date validTime = record.getValidTime();
    return times.indexOf(validTime);
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
  public boolean isInterval() {
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

  static protected class TimeCoordWithInterval implements Comparable<TimeCoordWithInterval> {
    public Date coord;
    public int start, interval;

    public TimeCoordWithInterval(Date coord, int start, int interval) {
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
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TimeCoordWithInterval that = (TimeCoordWithInterval) o;
      if (interval != that.interval) return false;
      if (!coord.equals(that.coord)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result = coord.hashCode();
      result = 31 * result + interval;
      return result;
    }

    @Override
    public String toString() {
      return "start=" + start +", interval=" + interval;
    }
  }

}
