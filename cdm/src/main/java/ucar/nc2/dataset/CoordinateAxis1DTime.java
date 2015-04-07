/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.time.*;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

import ucar.nc2.units.DateRange;

/**
 * A 1-dimensional Coordinate Axis representing Calendar time.
 * Its coordinate values can be represented as Dates.
 * <p/>
 * May use udunit dates, or ISO Strings.
 *
 * @author caron
 */
public class CoordinateAxis1DTime extends CoordinateAxis1D {

  static private final Logger logger = LoggerFactory.getLogger(CoordinateAxis1DTime.class);

  static public CoordinateAxis1DTime factory(NetcdfDataset ncd, VariableDS org, Formatter errMessages) throws IOException {
    if (org instanceof CoordinateAxis1DTime) return (CoordinateAxis1DTime) org;

    if (org.getDataType() == DataType.CHAR)
      return new CoordinateAxis1DTime(ncd, org, errMessages, org.getDimension(0).getShortName());

    else if (org.getDataType() == DataType.STRING)
      return new CoordinateAxis1DTime(ncd, org, errMessages, org.getDimensionsString());

    else
      return new CoordinateAxis1DTime(ncd, org, errMessages);
  }


  ////////////////////////////////////////////////////////////////
  private final CoordinateAxisTimeHelper helper;
  private List<CalendarDate> cdates = null;

  // for section and slice
  @Override
  protected Variable copy() {
    return new CoordinateAxis1DTime(this.ncd, this);
  }

  // copy constructor
  private CoordinateAxis1DTime(NetcdfDataset ncd, CoordinateAxis1DTime org) {
    super(ncd, org);
    helper = org.helper;
    this.cdates = org.cdates;
  }

  @Override
  public CoordinateAxis1DTime section(Range r) throws InvalidRangeException {
    CoordinateAxis1DTime s = (CoordinateAxis1DTime) super.section(r);
    List<CalendarDate> cdates = getCalendarDates();

    List<CalendarDate> cdateSection = new ArrayList<>(cdates.size());
    for (int i = r.first(), j = 0; i <= r.last(); i += r.stride(), ++j) {
      cdateSection.add(cdates.get(i));
    }
    s.cdates = cdateSection;
    return s;
  }

   /**
   * Get the the ith CalendarDate.
   * @param idx index
   * @return the ith CalendarDate
   */
   public CalendarDate getCalendarDate (int idx) {
     List<CalendarDate> cdates = getCalendarDates();  // in case we want to lazily evaluate
     return cdates.get(idx);
   }

  /**
   * Get calendar date range
   * @return calendar date range
   */
  public CalendarDateRange getCalendarDateRange() {
    List<CalendarDate> cd = getCalendarDates();
    int last = cd.size();
    return (last > 0) ? CalendarDateRange.of(cd.get(0), cd.get(last-1)) : null;
  }

  @Override
  public List<NamedObject> getNames() {
    List<CalendarDate> cdates = getCalendarDates();
    List<NamedObject> names = new ArrayList<>(cdates.size());
    for (CalendarDate cd : cdates)
      names.add(new NamedAnything(CalendarDateFormatter.toDateTimeString(cd), getShortName())); // "calendar date"));
    return names;
  }

  /**
   * only if isRegular() LOOK REDO
   *
   * @return time unit
   * @throws Exception on bad unit string
   */
  public TimeUnit getTimeResolution() throws Exception {
    String tUnits = getUnitsString();
    StringTokenizer stoker = new StringTokenizer(tUnits);
    double tResolution = getIncrement();
    return new TimeUnit(tResolution, stoker.nextToken());
  }

  /**
   * Given a Date, find the corresponding time index on the time coordinate axis.
   * Can only call this is hasDate() is true.
   * This will return
   * <ul>
   * <li> i, if time(i) <= d < time(i+1).
   * <li> 0, if d < time(0)
   * <li> n-1, if d > time(n-1),  where n is length of time coordinates
   * </ul>
   *
   * @param d date to look for
   * @return corresponding time index on the time coordinate axis
   * @throws UnsupportedOperationException is no time axis or isDate() false
   */
  public int findTimeIndexFromCalendarDate(CalendarDate d) {
    List<CalendarDate> cdates = getCalendarDates();         // LOOK linear search, switch to binary
    int index = 0;
    while (index < cdates.size()) {
      if (d.compareTo(cdates.get(index)) < 0)
        break;
      index++;
    }
    return Math.max(0, index - 1);
  }

  /**
   * See if the given CalendarDate appears as a coordinate
   *
   * @param date test this
   * @return true if equals a coordinate
   */
  public boolean hasCalendarDate(CalendarDate date) {
    List<CalendarDate> cdates = getCalendarDates();
    for (CalendarDate cd : cdates) {   // LOOK linear search, switch to binary
      if (date.equals(cd))
        return true;
    }
    return false;
  }

  /**
   * Get the list of datetimes in this coordinate as CalendarDate objects.
   * @return list of CalendarDates.
   */
  public List<CalendarDate> getCalendarDates() {
     return cdates;
  }

  public CalendarDate[] getCoordBoundsDate( int i) {
    double[] intv = getCoordBounds(i);
    CalendarDate[] e = new CalendarDate[2];
    e[0] = helper.makeCalendarDateFromOffset(intv[0]);
    e[1] = helper.makeCalendarDateFromOffset(intv[1]);
    return e;
  }

  ////////////////////////////////////////////////////////////////////////

  /**
   * Constructor for CHAR or STRING variables.
   * Must be ISO dates.
   *
   * @param ncd         the containing dataset
   * @param org         the underlying Variable
   * @param errMessages put error messages here; may be null
   * @param dims        list of dimensions
   * @throws IOException              on read error
   * @throws IllegalArgumentException if cant convert coordinate values to a Date
   */
  private CoordinateAxis1DTime(NetcdfDataset ncd, VariableDS org, Formatter errMessages, String dims) throws IOException {
    super(ncd, org.getParentGroup(), org.getShortName(), DataType.STRING, dims, org.getUnitsString(), org.getDescription());
    this.ncd = ncd;
    
    //Gotta set the original var. Otherwise it would be unable to read the values
    this.orgVar = org;
    
    this.orgName = org.orgName;
    this.helper = new CoordinateAxisTimeHelper(getCalendarFromAttribute(), null);

    if (org.getDataType() == DataType.CHAR)
      cdates = makeTimesFromChar(org, errMessages);
    else
      cdates = makeTimesFromStrings(org, errMessages);

    List<Attribute> atts = org.getAttributes();
    for (Attribute att : atts) {
      addAttribute(att);
    }
  }

  private List<CalendarDate> makeTimesFromChar(VariableDS org, Formatter errMessages) throws IOException {
    int ncoords = (int) org.getSize();
    int rank = org.getRank();
    int strlen = org.getShape(rank - 1);
    ncoords /= strlen;

    List<CalendarDate> result = new ArrayList<>(ncoords);

    ArrayChar data = (ArrayChar) org.read();
    ArrayChar.StringIterator ii = data.getStringIterator();
    ArrayObject.D1 sdata = new ArrayObject.D1(String.class, ncoords);

    for (int i = 0; i < ncoords; i++) {
      String coordValue = ii.next();
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      sdata.set(i, coordValue);
      result.add( cd);
    }
    setCachedData(sdata, true);
    return result;
  }

  private List<CalendarDate> makeTimesFromStrings( VariableDS org, Formatter errMessages) throws IOException {

    int ncoords = (int) org.getSize();
    List<CalendarDate> result = new ArrayList<>(ncoords);

    ArrayObject data = (ArrayObject) org.read();
    IndexIterator ii = data.getIndexIterator();
    for (int i = 0; i < ncoords; i++) {
      String coordValue = (String) ii.getObjectNext();
      CalendarDate cd = makeCalendarDateFromStringCoord( coordValue, org, errMessages);
      result.add(cd);
    }

    return result;
  }

  private CalendarDate makeCalendarDateFromStringCoord(String coordValue, VariableDS org, Formatter errMessages) throws IOException {
    CalendarDate cd =  helper.makeCalendarDateFromOffset(coordValue);
    if (cd == null) {
      if (errMessages != null) {
        errMessages.format("String time coordinate must be ISO formatted= %s%n", coordValue);
        logger.info("Char time coordinate must be ISO formatted= {} file = {}", coordValue, org.getDatasetLocation());
      }
      throw new IllegalArgumentException();
    }
    return cd;
  }


  /**
   * Constructor for numeric values - must have units
   * @param ncd         the containing dataset
   * @param org         the underlying Variable
   * @throws IOException on read error
   */
  private CoordinateAxis1DTime(NetcdfDataset ncd, VariableDS org, Formatter errMessages) throws IOException {
    super(ncd, org);
    this.helper= new CoordinateAxisTimeHelper(getCalendarFromAttribute(), getUnitsString());

    // make the coordinates
    int ncoords = (int) org.getSize();
    List<CalendarDate> result = new ArrayList<>(ncoords);

    Array data = org.read();

    int count = 0;
    IndexIterator ii = data.getIndexIterator();
    for (int i = 0; i < ncoords; i++) {
      double val = ii.getDoubleNext();
      if (Double.isNaN(val)) continue;  // WTF ??
      result.add( helper.makeCalendarDateFromOffset(val));
      count++;
    }

    // if we encountered NaNs, shorten it up
    if (count != ncoords) {
      Dimension localDim = new Dimension(getShortName(), count, false);
      setDimension(0, localDim);

      // set the shortened values
      Array shortData = Array.factory(data.getElementType(), new int[]{count});
      Index ima = shortData.getIndex();
      int count2 = 0;
      ii = data.getIndexIterator();
      for (int i = 0; i < ncoords; i++) {
        double val = ii.getDoubleNext();
        if (Double.isNaN(val)) continue;
        shortData.setDouble(ima.set0(count2), val);
        count2++;
      }

      // here we have to decouple from the original variable
      cache = new Cache();
      setCachedData(shortData, true);
    }

    cdates =  result;
  }

  ///////////////////////////////////////////////////////

  /**
   * Does not handle non-standard Calendars
   * @deprecated use getCalendarDates() to correctly interpret calendars
   */
  public java.util.Date[] getTimeDates() {
    List<CalendarDate> cdates = getCalendarDates();
    Date[] timeDates = new Date[cdates.size()];
    int index = 0;
    for (CalendarDate cd : cdates)
      timeDates[index++] = cd.toDate();
    return timeDates;
  }

  /**
   * Does not handle non-standard Calendars
    * @deprecated use getCalendarDate()
    */
   public java.util.Date getTimeDate (int idx) {
     return getCalendarDate(idx).toDate();
   }

   /**
    * Does not handle non-standard Calendars
   * @deprecated use getCalendarDateRange()
   */
  public DateRange getDateRange() {
    CalendarDateRange cdr = getCalendarDateRange();
    return cdr.toDateRange();
  }

  /**
   * Does not handle non-standard Calendars
   * @deprecated use findTimeIndexFromCalendarDate
   */
  public int findTimeIndexFromDate(java.util.Date d) {
    return findTimeIndexFromCalendarDate( CalendarDate.of(d));
  }

  /**
   * Does not handle non-standard Calendars
   * @deprecated  use hasCalendarDate
   */
  public boolean hasTime(Date date) {
    List<CalendarDate> cdates = getCalendarDates();
    for (CalendarDate cd : cdates) {
      if (date.equals(cd.toDate()))
        return true;
    }
    return false;
  }
}
