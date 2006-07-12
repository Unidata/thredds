// $Id:DateRange.java 63 2006-07-12 21:50:51Z edavis $
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

package thredds.datatype;

import java.util.Date;
import java.text.ParseException;

/**
 * Implements a range of dates, using DateType and/or TimeDuration.
 * You can use a DateType = "present" and a time duration to specify "real time" intervals, eg
 *   "last 3 days" uses endDate = "present" and duration = "3 days".
 *
 * @author john caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */

public class DateRange {
  private DateType start, end;
  private TimeDuration duration, resolution;
  private boolean invalid, useStart, useEnd, useDuration, useResolution;

  public DateRange() throws Exception {
      this( null, new DateType(false, new Date()), new TimeDuration("1 day"), new TimeDuration("15 min"));
  }

  public DateRange(Date start, Date end) {
    this( new DateType(false, start), new DateType(false, end), null, null);
  }

  public DateRange(DateRange range, String units) throws Exception {
    this( new DateType(false, range.getStart().getDate()), new DateType(false, range.getEnd().getDate()), null,
        new TimeDuration( units));
  }

  /**
   * Encapsolates a range of dates, using DateType start/end, and/or a TimeDuration.
   *  A DateRange can be specified in any of the following ways:
    <ol>
     <li> a start date and end date
     <li> a start date and duration
     <li> an end date and duration
    </ol>
   *
   * @param start starting date
   * @param end ending date
   * @param duration time duration
   * @param resolution time resolution; optional
   */
  public DateRange(DateType start, DateType end, TimeDuration duration, TimeDuration resolution) {
    this.start = start;
    this.end = end;
    this.duration = duration;
    this.resolution = resolution;

    useStart = (start != null) && !start.isBlank();
    useEnd = (end != null) && !end.isBlank();
    useDuration = (duration != null);
    useResolution = (resolution != null);

    invalid = true;
    if (useStart && useEnd) {
      invalid = false;
      recalcDuration();

    } else if (useStart && useDuration) {
      invalid = false;
      this.end = start.add( duration);

    } else if (useEnd && useDuration) {
      invalid = false;
      this.start = end.subtract( duration);
    }
    if (invalid)
      throw new IllegalArgumentException("DateRange must have 2 of start, end, duration");
    hashCode = 0;
  }

    // choose a resolution based on # seconds
  private String chooseResolution(double time) {
    if (time < 180) // 3 minutes
      return "secs";
    time /= 60; // minutes
    if (time < 180) // 3 hours
      return "minutes";
    time /= 60; // hours
    if (time < 72) // 3 days
      return "hours";
    time /= 24; // days
    if (time < 90) // 3 months
      return "days";
    time /= 30; // months
    if (time < 36) // 3 years
      return "months";
    return "years";
  }

  private void recalcDuration() {

    long min = getStart().getDate().getTime();
    long max = getEnd().getDate().getTime();
    double secs = .001 * (max - min);
    if (secs < 0)
      secs = 0;

    if (duration == null) {
      try {
        duration = new TimeDuration( chooseResolution(secs));
      } catch (ParseException e) {
        // cant happen
      }
    }

    if (resolution == null) {
      duration.setValueInSeconds( secs);
    } else {
       // make it a multiple of resolution
      double resSecs = resolution.getValueInSeconds();
      double closest = Math.round(secs / resSecs);
      secs = closest * resSecs;
      duration.setValueInSeconds( secs);
    }

    hashCode = 0;
  }

  /**
   * Determine if the given date is included in this date range.
   * The date range includes the start and end dates.
   */
  public boolean included( Date d) {
    if (invalid) return false;

    if (start.after( d)) return false;
    if (end.before( d)) return false;

    return true;
  }

  public DateType getStart() { return start; }
  public void setStart(DateType start) {
    this.start = start;
    useStart = true;

    if (start.isPresent()) {
      this.end = start.add( duration);
      useEnd = false;
    } else if (end.isPresent()) {
      recalcDuration();
      this.start = end.subtract( duration);
    } else {
      recalcDuration();
      this.end = start.add( duration);
    }
  }

  public DateType getEnd() { return end; }
  public void setEnd(DateType end) {
    this.end = end;
    useEnd = true;

    if (end.isPresent()) {
      this.start = end.subtract( duration);
      useStart = false;
    } else if (start.isPresent()) {
      recalcDuration();
      this.end = start.add( duration);
    } else {
      recalcDuration();
      this.start = end.subtract( duration);
    }
  }
  
  /** Extend this date range by the given one, if needed */
  public void extend( DateRange dr) {
    if (dr.getStart().getDate().before( start.getDate()))
      setStart( dr.getStart());
    if (dr.getEnd().getDate().after( end.getDate()))
      setEnd( dr.getEnd());
  }

  public TimeDuration getDuration() { return duration; }
  public void setDuration(TimeDuration duration) {
    this.duration = duration;
    useDuration = true;

    if (this.end.isPresent()) {
      this.start = end.subtract( duration);
      useStart = false;
    } else {
      this.end = start.add( duration);
      useEnd = false;
    }
  }

  public TimeDuration getResolution() { return resolution; }
  public void setResolution(TimeDuration resolution) {
    this.resolution = resolution;
    useResolution = true;
  }

  public boolean useStart() { return useStart; }
  public boolean useEnd() { return useEnd; }
  public boolean useDuration() { return useDuration; }
  public boolean useResolution() { return useResolution; }

  /**
   * Return true if start equals end date, so date range is a point.
   */
  public boolean isPoint() {
    return start.equals( end);
  }

  public String toString() { return "start= "+start +" end= "+end+ " duration= "+ duration
        + " resolution= "+ resolution; }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DateRange)) return false;
    return o.hashCode() == this.hashCode();
  }

 /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if ( useStart )
        result = 37*result + start.hashCode();
      if ( useEnd )
        result = 37*result + end.hashCode();
      if ( useDuration )
        result = 37*result + duration.hashCode();
      if ( useResolution )
        result = 37*result + resolution.hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}