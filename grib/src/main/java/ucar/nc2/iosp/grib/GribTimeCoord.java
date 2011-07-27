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

import ucar.grib.GribGridRecord;
import ucar.grib.GribPds;

import ucar.nc2.iosp.grid.GridRecord;
import ucar.nc2.iosp.grid.GridTimeCoord;

import java.util.*;

/**
 * A Time Coordinate for a Grib dataset.
 *
 * @author caron
 */
public class GribTimeCoord extends GridTimeCoord {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribTimeCoord.class);

  /**
   * Create a new GridTimeCoord from the list of GridRecord
   *
   * @param records records to use
   * @param where file location for warn message
   */
  GribTimeCoord(List<GridRecord> records, String where) {
    super(records, where);

    // LOOK time coords get filled out by superclass

    // interval case - only GRIB
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

  /**
   * match time values - can this list of GridRecords use this coordinate?
   *
   * @param records list of records
   * @return true if they are the same as this
   */
  @Override
  protected boolean matchTimes(List<GridRecord> records) {
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
   * Find the index of a GridRecord in the list of times
   *
   * @param record the GridRecord
   * @return the index in the list of time values, or -1 if not found
   */
  @Override
  public int findIndex(GridRecord record) {
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

}
