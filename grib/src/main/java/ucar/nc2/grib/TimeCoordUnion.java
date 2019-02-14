/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;

import java.util.*;

/**
 * A union of TimeCoords that tracks where each coord came from in the original List<TimeCoord>.
 * Unique by CalendarDate or Tinv.
 * Used only by Time Partitions
 *
 * @author caron
 * @since 6/16/11
 */
public class TimeCoordUnion extends TimeCoord {
  private final List<Val> values;

  /*
   * Create the union of all the values in the list of TimeCoord. keep track of which partition and index it came from.
   * Later ones override earlier ones.
   * constructor when we write.
   *
   * @param timeCoords list of TimeCoord
   * @param canon use baseDate, units, isInterval from here
   * @return List<TimeCoordUval>
   */
  public TimeCoordUnion(int code, List<TimeCoord> timeCoords, TimeCoord canon){
    super(code, canon.getRunDate(), canon.getTimeUnit(), null);
    if (canon.isInterval()) {
      this.values =  makeUnionIntv(timeCoords);
      this.intervals = new ArrayList<>(this.values.size());
      for (Val val : values) {
        this.intervals.add(val.tinv);
      }

    } else {
      this.values =  makeUnionReg(timeCoords);
      this.coords = new ArrayList<>(this.values.size());
      for (Val val : values) {
        int offset = TimeCoord.getOffset(getRunDate(), val.val, getTimeUnit());
        this.coords.add(offset);
      }
    }

  }

  // constructor when we read
  public TimeCoordUnion(int code, String units, List coords, int[] partition, int[] index){
    super(code, units, coords);

    values = new ArrayList<>(partition.length);
    for (int i=0; i<partition.length; i++) {
      values.add( new Val((CalendarDate) null, partition[i], index[i])); // LOOK null coord
    }
  }

  public Val getVal(int idx) {
    return values.get(idx);
  }

  public List<Val> getValues() {
    return values;
  }

  private List<Val> makeUnionReg(List<TimeCoord> timeCoords) {
    Map<CalendarDate, Val> values = new HashMap<>();

    // loop over partitions
    for (int partno = 0; partno < timeCoords.size(); partno++) {
      TimeCoord tc = timeCoords.get(partno);
      if (tc == null) continue;

      // loop over coordinates
      CalendarDateUnit thisUnit = CalendarDateUnit.of(null, tc.getUnits());
      List<Integer> vals = tc.getCoords();
      for (int idx = 0; idx < vals.size(); idx++) {
        CalendarDate d = thisUnit.makeCalendarDate(vals.get(idx)); // convert to CalendarDate

        // add or override
        Val uval = values.get(d);
        if (uval == null) {
          uval = new Val(d, partno, idx);
          values.put(d, uval);
        } else {
          uval.partition = partno;  // later ones override
          uval.index = idx;
        }
      }
    }

    // extract into a List and sort
    List<Val> valuesList = new ArrayList<>(values.values());
    Collections.sort(valuesList);  // sort by CalendarDate

    return valuesList;
  }

  private List<Val> makeUnionIntv(List<TimeCoord> timeCoords) {
    Map<TimeCoord.Tinv, Val> values = new HashMap<>();

    // loop over partitions
    for (int partno = 0; partno < timeCoords.size(); partno++) {
      TimeCoord tc = timeCoords.get(partno);
      if (tc == null) continue; // missing data

      // loop over coordinates
      List<TimeCoord.Tinv> vals = tc.getIntervals();
      for (int idx = 0; idx < vals.size(); idx++) {
        TimeCoord.Tinv org = vals.get(idx);

        // canonicalize
        TimeCoord.Tinv convert = org.convertReferenceDate(tc.getRunDate(), tc.getTimeUnit(), getRunDate(), getTimeUnit());

        // add or override
        Val uval = values.get(convert);
        if (uval == null) {
          uval = new Val(convert, partno, idx);
          values.put(convert, uval);
        } else {
          uval.partition = partno;  // later ones override
          uval.index = idx;
        }
      }
    }

    // extract into a List and sort
    List<Val> valuesList = new ArrayList<>(values.values());
    Collections.sort(valuesList);  // sort by Tinv

    return valuesList;
  }

  @Override
  public boolean equalsData(TimeCoord tother) {
    if (!super.equalsData(tother)) return false;
    TimeCoordUnion o = (TimeCoordUnion) tother;
    for (int i = 0; i < values.size(); i++) {
      Val val1 = values.get(i);
      Val val2 = o.getVal(i);
      if (val1.partition != val2.partition) return false;
      if (val1.index != val2.index) return false;
    }
    return true;
  }

  static public int findUnique(List<TimeCoordUnion> timeIndexList, TimeCoordUnion want) {
    if (want == null) return -1;

    for (int i = 0; i < timeIndexList.size(); i++) {
      if (want.equalsData(timeIndexList.get(i)))  // LOOK probably wrong
        return i;
    }

    timeIndexList.add(want);
    return timeIndexList.size() - 1;
  }

  static public class Val implements Comparable<Val> {
    TimeCoord.Tinv tinv; // not available on read
    CalendarDate val;// not available on read

    int partition;
    int index;

    Val(TimeCoord.Tinv tinv, int partition, int index) {
      this.tinv = tinv;
      this.partition = partition;
      this.index = index;
    }

    Val(CalendarDate val, int partition, int index) {
      this.val = val;
      this.partition = partition;
      this.index = index;
    }

    public Tinv getTinv() {
      return tinv;
    }

    public CalendarDate getVal() {
      return val;
    }

    public int getPartition() {
      return partition;
    }

    /**
     * Get time index in this partition
     * @return time index in this partition
     */
    public int getIndex() {
      return index;
    }

    @Override
    public int compareTo(Val o) {
      if (val != null)
        return val.compareTo(o.val);
      else
        return tinv.compareTo(o.tinv);
    }
  }



}
