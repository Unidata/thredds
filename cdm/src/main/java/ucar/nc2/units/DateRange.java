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

package ucar.nc2.units;

import java.util.Date;
import java.text.ParseException;

/**
 * Implements a range of dates, using DateType and/or TimeDuration.
 * You can use a DateType = "present" and a time duration to specify "real time" intervals, eg
 * "last 3 days" uses endDate = "present" and duration = "3 days".
 *
 * @author john caron
 */

public class DateRange {
  private DateType start, end;
  private TimeDuration duration, resolution;
  private boolean isEmpty, isMoving, useStart, useEnd, useDuration, useResolution;

  /**
   * default Constructor
   *
   * @throws java.text.ParseException artifact, cant happen
   */
  public DateRange() throws ParseException {
    this(null, new DateType(false, new Date()), new TimeDuration("1 day"), new TimeDuration("15 min"));
  }

  /**
    * Create Date Range from a start and end date
    *
    * @param start start of range
    * @param end   end of range
    */
   public DateRange(Date start, Date end) {
     this(new DateType(false, start), new DateType(false, end), null, null);
   }

  /**
    * Create Date Range from a start date and duration
    *
    * @param start start of range
    * @param duration   duration of range
    */
   public DateRange(Date start, TimeDuration duration) {
     this(new DateType(false, start), null, duration, null);
   }

   /**
   * Create DateRange from another DateRange, with a different units of resolution.
   *
   * @param range     copy start and end from here
   * @param timeUnits make resolution using new TimeDuration( timeUnits)
   * @throws Exception is units are not valid time units
   */
  public DateRange(DateRange range, String timeUnits) throws Exception {
    this(new DateType(false, range.getStart().getDate()), new DateType(false, range.getEnd().getDate()), null, new TimeDuration(timeUnits));
  }

  /**
   * Encapsolates a range of dates, using DateType start/end, and/or a TimeDuration.
   * A DateRange can be specified in any of the following ways:
   * <ol>
   * <li> a start date and end date
   * <li> a start date and duration
   * <li> an end date and duration
   * </ol>
   *
   * @param start      starting date
   * @param end        ending date
   * @param duration   time duration
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

    boolean invalid = true;
    if (useStart && useEnd) {
      invalid = false;
      this.isMoving = this.start.isPresent() || this.end.isPresent();
      useDuration = false;
      recalcDuration();

    } else if (useStart && useDuration) {
      invalid = false;
      this.isMoving = this.start.isPresent();
      this.end = this.start.add(duration);

    } else if (useEnd && useDuration) {
      invalid = false;
      this.isMoving = this.end.isPresent();
      this.start = this.end.subtract(duration);
    }

    if (invalid)
      throw new IllegalArgumentException("DateRange must have 2 of start, end, duration");

    checkIfEmpty();
    hashCode = 0;
  }

  private void checkIfEmpty() {
    if (this.start.isPresent() && this.end.isPresent())
      isEmpty = true;
    else if (this.start.isPresent() || this.end.isPresent())
      isEmpty = duration.getValue() <= 0;
    else
      isEmpty = this.end.before(this.start);

    if (isEmpty)
      duration.setValueInSeconds(0);
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

  /**
   * Determine if the given date is included in this date range.
   * The date range includes the start and end dates.
   *
   * @param d date to check
   * @return true if date in inside this range
   */
  public boolean included(Date d) {
    if (isEmpty) return false;

    if (getStart().after(d)) return false;
    if (getEnd().before(d)) return false;

    return true;
  }

  /**
   * Same as included()
   * @param d date to check
   * @return true if date in inside this range
   */
  public boolean contains(Date d) {
    return included(d);
  }


  /**
   * Determine if the given range intersects this date range.
   *
   * @param start_want range starts here
   * @param end_want   range ends here
   * @return true if ranges intersect
   */
  public boolean intersects(Date start_want, Date end_want) {
    if (isEmpty) return false;

    if (getStart().after(end_want)) return false;
    if (getEnd().before(start_want)) return false;

    return true;
  }

  /**
   * Determine if the given range intersects this date range.
   *
   * @param other date range
   * @return true if ranges intersect
   */
  public boolean intersects(DateRange other) {
    return intersects(other.getStart().getDate(), other.getEnd().getDate());
  }

  /**
   * Intersect with another date range
   *
   * @param clip intersect with this date range
   * @return new date range that is the intersection
   */
  public DateRange intersect(DateRange clip) {
    if (isEmpty) return this;
    if (clip.isEmpty) return clip;

    DateType ss = getStart();
    DateType s = ss.before(clip.getStart()) ? clip.getStart() : ss;

    DateType ee = getEnd();
    DateType e = ee.before(clip.getEnd()) ? ee : clip.getEnd();

    return new DateRange(s, e, null, resolution);
  }

  /**
   * Extend this date range by the given one.
   *
   * @param dr given DateRange
   */
  public void extend(DateRange dr) {
    boolean localEmpty = isEmpty;
    if (localEmpty || dr.getStart().before(getStart()))
      setStart(dr.getStart());
    if (localEmpty || getEnd().before(dr.getEnd()))
      setEnd(dr.getEnd());
  }

  /**
   * Extend this date range by the given Date.
   *
   * @param d given Date
   */
  public void extend(Date d) {
    if (d.before(getStart().getDate()))
      setStart( new DateType(false, d));
    if (getEnd().before(d))
      setEnd(new DateType(false, d));
  }

  /**
   * Get the starting Date.
   *
   * @return starting Date
   */
  public DateType getStart() {
    return (isMoving && !useStart) ? this.end.subtract(duration) : start;
  }

  /**
   * Set the starting Date. Makes useStart true.
   * If useEnd, recalculate the duration, else recalculate end.
   *
   * @param start starting Date
   */
  public void setStart(DateType start) {
    this.start = start;
    useStart = true;

    if (useEnd) {
      this.isMoving = this.start.isPresent() || this.end.isPresent();
      useDuration = false;
      recalcDuration();

    } else {
      this.isMoving = this.start.isPresent();
      this.end = this.start.add(duration);
    }
    checkIfEmpty();
  }

  /**
   * Get the ending Date.
   *
   * @return ending Date
   */
  public DateType getEnd() {
    return (isMoving && !useEnd) ? this.start.add(duration) : end;
  }

  /**
   * Set the ending Date. Makes useEnd true.
   * If useStart, recalculate the duration, else recalculate start.
   *
   * @param end ending Date
   */
  public void setEnd(DateType end) {
    this.end = end;
    useEnd = true;

    if (useStart) {
      this.isMoving = this.start.isPresent() || this.end.isPresent();
      useDuration = false;
      recalcDuration();

    } else {
      this.isMoving = this.end.isPresent();
      this.start = this.end.subtract(duration);
    }
    checkIfEmpty();
  }

  /**
   * Get the duration of the interval
   *
   * @return duration of the interval
   */
  public TimeDuration getDuration() {
    if (isMoving && !useDuration)
      recalcDuration();
    return duration;
  }

  /**
   * Set the duration of the interval. Makes useDuration true.
   * If useStart, recalculate end, else recalculate start.
   *
   * @param duration duration of the interval
   */
  public void setDuration(TimeDuration duration) {
    this.duration = duration;
    useDuration = true;

    if (useStart) {
      this.isMoving = this.start.isPresent();
      this.end = this.start.add(duration);
      useEnd = false;

    } else {
      this.isMoving = this.end.isPresent();
      this.start = this.end.subtract(duration);
    }
    checkIfEmpty();
  }

  // assumes not moving
  private void recalcDuration() {
    long min = getStart().getDate().getTime();
    long max = getEnd().getDate().getTime();
    double secs = .001 * (max - min);
    if (secs < 0)
      secs = 0;

    if (duration == null) {
      try {
        duration = new TimeDuration(chooseResolution(secs));
      } catch (ParseException e) {
        // cant happen
      }
    }

    if (resolution == null) {
      duration.setValueInSeconds(secs);
    } else {
      // make it a multiple of resolution
      double resSecs = resolution.getValueInSeconds();
      double closest = Math.round(secs / resSecs);
      secs = closest * resSecs;
      duration.setValueInSeconds(secs);
    }

    hashCode = 0;
  }

  /**
   * Get the time resolution.
   *
   * @return time resolution as a duration
   */
  public TimeDuration getResolution() {
    return resolution;
  }

  /**
   * Set the time resolution.
   *
   * @param resolution the time resolution
   */
  public void setResolution(TimeDuration resolution) {
    this.resolution = resolution;
    useResolution = true;
  }

  /**
   * Get if the start is fixed.
   * @return if start is fixed
   */
  public boolean useStart() {
    return useStart;
  }

  /**
   * Get if the end is fixed.
   * @return if end is fixed
   */
  public boolean useEnd() {
    return useEnd;
  }

  /**
   * Get if the duration is fixed.
   * @return if duration is fixed
   */
  public boolean useDuration() {
    return useDuration;
  }

  /**
   * Get if the resolution is set.
   * @return if resolution is fixed
   */
  public boolean useResolution() {
    return useResolution;
  }

  /**
   * Return true if start date equals end date, so date range is a point.
   *
   * @return true if start = end
   */
  public boolean isPoint() {
    return !isEmpty && start.equals(end);
  }

  /**
   * If the range is empty
   *
   * @return if the range is empty
   */
  public boolean isEmpty() {
    return isEmpty;
  }

  @Override
  public String toString() {
    return "start= " + start + " end= " + end + " duration= " + duration
        + " resolution= " + resolution;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DateRange)) return false;
    DateRange oo = (DateRange) o;
    if (useStart && !start.equals(oo.start)) return false;
    if (useEnd && !end.equals(oo.end)) return false;
    if (useDuration && !duration.equals(oo.duration)) return false;
    if (useResolution && !resolution.equals(oo.resolution)) return false;
    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (useStart)
        result = 37 * result + start.hashCode();
      if (useEnd)
        result = 37 * result + end.hashCode();
      if (useDuration)
        result = 37 * result + duration.hashCode();
      if (useResolution)
        result = 37 * result + resolution.hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private int hashCode = 0; // Bloch, item 8

}