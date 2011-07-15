/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.fmrc;

import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.Misc;

import java.util.*;

/**
 * Represents a list of offset times shared among variables
 * Tracks a list of variables that all have the same list of offset times.
 */
public class TimeCoord implements Comparable {
  static public final TimeCoord EMPTY = new TimeCoord(new Date(), new double[0]);

  private Date runDate;
  private List<GridDatasetInv.Grid> gridInv; // track the grids that use this coord
  private int id; // unique id for serialization
  private String axisName; // time coordinate axis

  // time at point has offsets, intervals have bounds
  private boolean isInterval = false;
  private double[] offset; // hours since runDate
  private double[] bound1, bound2; // hours since runDate [ntimes,2]

  TimeCoord(Date runDate) {
    this.runDate = runDate;
  }

  TimeCoord(Date runDate, double[] offset) {
    this.runDate = runDate;
    this.offset = offset;
  }

  TimeCoord(TimeCoord from) {
    this.runDate = from.runDate;
    this.axisName = from.axisName;
    this.offset = from.offset;
    this.isInterval = from.isInterval;
    this.bound1 = from.bound1;
    this.bound2 = from.bound2;
    this.id = from.id;
  }

  TimeCoord(Date runDate, CoordinateAxis1DTime axis) {
    this.runDate = runDate;
    this.axisName = axis.getFullName();

    DateUnit unit = null;
    try {
      unit = new DateUnit(axis.getUnitsString());
    } catch (Exception e) {
      throw new IllegalArgumentException("Not a unit of time " + axis.getUnitsString());
    }

    int n = (int) axis.getSize();
    if (axis.isInterval()) {
      this.isInterval = true;
      this.bound1 = axis.getBound1();
      this.bound2 = axis.getBound2();
    } else {
      offset = new double[n];
      for (int i = 0; i < axis.getSize(); i++) {
        Date d = unit.makeDate(axis.getCoordValue(i));
        offset[i] = FmrcInv.getOffsetInHours(runDate, d);
      }
    }
  }

  void addGridInventory(GridDatasetInv.Grid grid) {
    if (gridInv == null)
      gridInv = new ArrayList<GridDatasetInv.Grid>();
    gridInv.add(grid);
  }

  public Date getRunDate() {
    return runDate;
  }

  public boolean isInterval() {
    return isInterval;
  }

  /**
   * The list of GridDatasetInv.Grid that use this TimeCoord
   *
   * @return list of GridDatasetInv.Grid that use this TimeCoord
   */
  public List<GridDatasetInv.Grid> getGridInventory() {
    return (gridInv == null) ? new ArrayList<GridDatasetInv.Grid>() : gridInv;
  }

  /**
   * A unique id for this TimeCoord
   *
   * @return unique id for this TimeCoord
   */
  public int getId() {
    return id;
  }

  /**
   * Set the unique id for this TimeCoord
   *
   * @param id id for this TimeCoord
   */
  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    if (this == EMPTY) return "EMPTY";
    return id == 0 ? "time" : "time" + id;
  }

  public String getAxisName() {
    return axisName;
  }

  public int getNCoords() {
    return (isInterval) ? bound1.length : offset.length;
  }

   /**
   * The list of valid times, in units of hours since the run time
   * @return list of valid times, in units of hours since the run time
   */
  public double[] getOffsetTimes() {
    return isInterval ? bound2 : offset;
  }

  public double[] getBound1() {
    return bound1;
  }

  public double[] getBound2() {
    return bound2;
  }

  public void setOffsetTimes(double[] offset) {
    this.offset = offset;
  }

  public void setBounds(double[] bound1, double[] bound2) {
    this.bound1 = bound1;
    this.bound2 = bound2;
    this.isInterval = true;
  }

  public void setBounds(List<TimeCoord.Tinv> tinvs) {
    this.bound1 = new double[tinvs.size()];
    this.bound2 = new double[tinvs.size()];
    int count = 0;
    for (TimeCoord.Tinv tinv : tinvs) {
      this.bound1[count] = tinv.b1;
      this.bound2[count] = tinv.b2;
      count++;
    }
    this.isInterval = true;
  }

  @Override
  public String toString() {
    DateFormatter df = new DateFormatter();
    Formatter out = new Formatter();
    out.format("%-10s %-26s offsets=", getName(), df.toDateTimeString(runDate));
    if (isInterval)
      for (int i=0; i<bound1.length; i++) out.format("(%3.1f,%3.1f) ", bound1[i], bound2[i]);
    else
      for (double val : offset) out.format("%3.1f, ", val);
    return out.toString();
  }

  /**
   * Instances that have the same offsetHours/bounds and runtime are equal
   *
   * @param tother compare this TimeCoord's data
   * @return true if data are equal
   */
  public boolean equalsData(TimeCoord tother) {
    if (getRunDate() != null) {
      if (!getRunDate().equals(tother.getRunDate())) return false;
    }

    if (isInterval != tother.isInterval) return false;

    if (isInterval) {
      if (bound1.length != tother.bound1.length)
        return false;

      for (int i = 0; i < bound1.length; i++) {
        if (!ucar.nc2.util.Misc.closeEnough(bound1[i], tother.bound1[i]))
          return false;
        if (!ucar.nc2.util.Misc.closeEnough(bound2[i], tother.bound2[i]))
          return false;
      }
      return true;

    } else { // non interval

      if (offset.length != tother.offset.length)
        return false;

      for (int i = 0; i < offset.length; i++) {
        if (!ucar.nc2.util.Misc.closeEnough(offset[i], tother.offset[i]))
          return false;
      }
      return true;
    }
  }

  public int findInterval(double b1, double b2) {
    for (int i = 0; i < getNCoords(); i++)
      if (Misc.closeEnough(bound1[i], b1) && Misc.closeEnough(bound2[i], b2))
        return i;
    return -1;
  }

  public int findIndex(double offsetHour) {
    double[] off = getOffsetTimes();
    for (int i = 0; i < off.length; i++)
      if (Misc.closeEnough(off[i], offsetHour))
        return i;
    return -1;
  }

  public int compareTo(Object o) {
    TimeCoord ot = (TimeCoord) o;
    return id - ot.id;
  }

  /////////////////////////////////////////////

  /**
   * Look through timeCoords to see if one matches want.
   * Matches means equalsData() is true.
   * If not found, make a new one and add to timeCoords.
   *
   * @param timeCoords look through this list
   * @param want find equivilent
   * @return return equivilent or make a new one and add to timeCoords
   */
  static public TimeCoord findTimeCoord(List<TimeCoord> timeCoords, TimeCoord want) {
    if (want == null) return null;

    for (TimeCoord tc : timeCoords) {
      if (want.equalsData(tc))
        return tc;
    }

    // make a new one
    TimeCoord result = new TimeCoord(want);
    timeCoords.add(result);
    return result;
  }

  /**
   * Create the union of all the values in the list of TimeCoord, ignoring the TimeCoord's runDate
   * @param timeCoords list of TimeCoord
   * @param baseDate resulting union timeCoord uses this as a base date
   * @return union TimeCoord
   */
  static public TimeCoord makeUnion(List<TimeCoord> timeCoords, Date baseDate) {
    if (timeCoords.size() == 0) return new TimeCoord(baseDate);
    if (timeCoords.size() == 1) return timeCoords.get(0);

    if (timeCoords.get(0).isInterval)
      return makeUnionIntv(timeCoords, baseDate);
    else
      return makeUnionReg(timeCoords, baseDate);
  }

  static private TimeCoord makeUnionReg(List<TimeCoord> timeCoords, Date baseDate) {
    // put into a set for uniqueness
    Set<Double> offsets = new HashSet<Double>();
    for (TimeCoord tc : timeCoords) {
      if (tc.isInterval)
        throw new IllegalArgumentException("Cant mix interval coordinates");
      for (double off : tc.getOffsetTimes()) 
        offsets.add(off);
    }

    // extract into a List
    List<Double> offsetList = Arrays.asList((Double[]) offsets.toArray(new Double[offsets.size()]));

    // sort and extract into double[]
    Collections.sort(offsetList);
    double[] offset = new double[offsetList.size()];
    int count = 0;
    for (double off : offsetList)
      offset[count++] = off;

    // make the resulting time coord
    TimeCoord result = new TimeCoord(baseDate);
    result.setOffsetTimes(offset);
    return result;
  }

  static private TimeCoord makeUnionIntv(List<TimeCoord> timeCoords, Date baseDate) {
    // put into a set for uniqueness
    Set<Tinv> offsets = new HashSet<Tinv>();
    for (TimeCoord tc : timeCoords) {
      if (!tc.isInterval)
        throw new IllegalArgumentException("Cant mix non-interval coordinates");
      for (int i=0; i<tc.bound1.length; i++)
        offsets.add(new Tinv(tc.bound1[i], tc.bound2[i]));
    }

    // extract into a List
    List<Tinv> bounds = Arrays.asList((Tinv[]) offsets.toArray(new Tinv[offsets.size()]));

    // sort and extract into double[] bounds arrays
    Collections.sort(bounds);
    int n = bounds.size();
    double[] bounds1 = new double[n];
    double[] bounds2 = new double[n];
    for (int i=0; i<n; i++) {
      Tinv tinv = bounds.get(i);
      bounds1[i] = tinv.b1;
      bounds2[i] = tinv.b2;
    }

    // make the resulting time coord
    TimeCoord result = new TimeCoord(baseDate);
    result.setBounds(bounds1, bounds2);
    return result;
  }

  // use for matching intervals
  public static class Tinv implements Comparable<Tinv> {
    private double b1, b2;  // bounds

    public Tinv(double offset) {
      this.b2 = offset;
    }

    public Tinv(double b1, double b2) {
      this.b1 = b1;
      this.b2 = b2;
    }

    @Override
    public boolean equals(Object o) {
      Tinv tinv = (Tinv) o;
      if (Double.compare(tinv.b1, b1) != 0) return false;
      if (Double.compare(tinv.b2, b2) != 0) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      temp = b1 != +0.0d ? Double.doubleToLongBits(b1) : 0L;   // Bloch item 9
      result = (int) (temp ^ (temp >>> 32));
      temp = b2 != +0.0d ? Double.doubleToLongBits(b2) : 0L;
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }

    @Override
    public int compareTo(Tinv o) {
      int c1 = Double.compare(b2, o.b2);
      if (c1 == 0) return  Double.compare(b1, o.b1);
      return c1;
    }
  }

  /*
   * Create the union of all the values in the list of TimeCoord, converting all to a common baseDate
   * @param timeCoords list of TimeCoord
   * @param baseDate resulting union timeCoord uses this as a base date
   * @return union TimeCoord
   *
  static public TimeResult makeUnionConvert(List<TimeCoord> timeCoords, Date baseDate) {

    Map<Double, Double> offsetMap = new HashMap<Double, Double>(256);
    for (TimeCoord tc : timeCoords) {
      double run_offset = FmrcInv.getOffsetInHours(baseDate, tc.getRunDate());
      for (double offset : tc.getOffsetHours()) {
        offsetMap.put(run_offset + offset, run_offset); // later ones override
      }
    }

    Set<Double> keys = offsetMap.keySet();
    int n = keys.size();
    List<Double> offsetList = Arrays.asList((Double[]) keys.toArray(new Double[n]));
    Collections.sort(offsetList);

    int counto = 0;
    double[] offs = new double[n];
    double[] runoffs = new double[n];
    for (Double key : offsetList) {
      offs[counto] = key;
      runoffs[counto] = offsetMap.get(key);
      counto++;
    }

    return new TimeResult( baseDate, offs, runoffs);
  }

  static class TimeResult {
    double[] offsets;
    double[] runOffsets;
    Date base;

    TimeResult(Date base, double[] offsets, double[] runOffsets) {
      this.base = base;
      this.offsets = offsets;
      this.runOffsets = runOffsets;
    }
  } */


}
