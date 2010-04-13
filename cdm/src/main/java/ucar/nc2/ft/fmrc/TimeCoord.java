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
  private List<GridDatasetInv.Grid> gridInv; // all use this coord
  private int id; // unique id for serialization
  private double[] offset; // hours since runDate
  private String axisName; // time coordinate axis

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
    this.id = from.id;
  }

  TimeCoord(Date runDate, CoordinateAxis1DTime axis) {
    this.runDate = runDate;
    this.axisName = axis.getName();

    DateUnit unit = null;
    try {
      unit = new DateUnit(axis.getUnitsString());
    } catch (Exception e) {
      throw new IllegalArgumentException("Not a unit of time " + axis.getUnitsString());
    }

    int n = (int) axis.getSize();
    offset = new double[n];
    for (int i = 0; i < axis.getSize(); i++) {
      Date d = unit.makeDate(axis.getCoordValue(i));
      offset[i] = FmrcInv.getOffsetInHours(runDate, d);
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

   /**
   * The list of valid times, in units of hours since the run time
   * @return list of valid times, in units of hours since the run time
   */
  public double[] getOffsetHours() {
    return offset;
  }

  public void setOffsetHours(double[] offset) {
    this.offset = offset;
  }

  @Override
  public String toString() {
    DateFormatter df = new DateFormatter();
    Formatter out = new Formatter();
    out.format("%-10s %-26s offsets=", getName(), df.toDateTimeString(runDate));
    for (double val : offset) out.format("%3.1f, ", val);
    return out.toString();
  }

  /**
   * Instances that have the same offsetHours and runtime are equal
   *
   * @param tother compare this TomCoord's data
   * @return true if data is equal
   */
  public boolean equalsData(TimeCoord tother) {
    if (getRunDate() != null) {
      if (!getRunDate().equals(tother.getRunDate())) return false;
    }

    if (offset.length != tother.offset.length)
      return false;

    for (int i = 0; i < offset.length; i++) {
      if (!ucar.nc2.util.Misc.closeEnough(offset[i], tother.offset[i]))
        return false;
    }
    return true;
  }

  public int findIndex(double offsetHour) {
    for (int i = 0; i < offset.length; i++)
      if (Misc.closeEnough(offset[i], offsetHour))
        return i;
    return -1;
  }

  public int compareTo(Object o) {
    TimeCoord ot = (TimeCoord) o;
    return id - ot.id;
  }

  /////////////////////////////////////////////

  /**
   * Look through timeCoords to see if equivilent to want exists.
   * Equiv means runDate is the same, and offsets match.
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
    // put into a set for uniqueness
    Set<Double> offsets = new HashSet<Double>();
    for (TimeCoord tc : timeCoords) {
      for (double off : tc.getOffsetHours())
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
    result.setOffsetHours(offset);
    return result;
  }
  
  /**
   * Create the union of all the values in the list of TimeCoord, converting all to a common baseDate
   * @param timeCoords list of TimeCoord
   * @param baseDate resulting union timeCoord uses this as a base date
   * @return union TimeCoord
   */
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
  }


}
