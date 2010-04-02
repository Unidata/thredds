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

package ucar.nc2.dataset;

import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
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
  private Date[] timeDates;
  private DateUnit dateUnit;

  static public CoordinateAxis1DTime factory(NetcdfDataset ncd, VariableDS org, Formatter errMessages) throws IOException {
    if (org.getDataType() == DataType.CHAR) {
      return new CoordinateAxis1DTime(ncd, org, errMessages, org.getDimension(0).getName());
    }

    return new CoordinateAxis1DTime(ncd, org, errMessages);
  }

  /**
   * Constructor for CHAR variables, turn into String
   *
   * @param ncd         the containing dataset
   * @param org         the underlying Variable
   * @param errMessages put error messages here; may be null
   * @param dims        list of dimensions
   * @throws IOException              on read error
   * @throws IllegalArgumentException if cant convert coordinate values to a Date
   */
  private CoordinateAxis1DTime(NetcdfDataset ncd, VariableDS org, Formatter errMessages, String dims) throws IOException {
    // NetcdfDataset ds, Group group, String shortName,  DataType dataType, String dims, String units, String desc
    super(ncd, org.getParentGroup(), org.getShortName(), DataType.STRING, dims, org.getUnitsString(), org.getDescription());
    this.ncd = ncd;
    this.orgName = org.orgName;

    List<Attribute> atts = org.getAttributes();
    for (Attribute att : atts) {
      addAttribute(att);
    }

    //named = new ArrayList<NamedObject>(); // declared in CoordinateAxis1D superclass

    int ncoords = (int) org.getSize();
    int rank = org.getRank();
    int strlen = org.getShape(rank - 1);
    ncoords /= strlen;

    timeDates = new Date[ncoords];

    ArrayChar data = (ArrayChar) org.read();
    ArrayChar.StringIterator ii = data.getStringIterator();
    ArrayObject.D1 sdata = new ArrayObject.D1(String.class, ncoords);

    for (int i = 0; i < ncoords; i++) {
      String coordValue = ii.next();
      Date d = DateUnit.getStandardOrISO(coordValue);
      if (d == null) {
        if (errMessages != null)
          errMessages.format("DateUnit cannot parse String= %s\n",coordValue);
        else
          System.out.println("DateUnit cannot parse String= " + coordValue + "\n");

        throw new IllegalArgumentException();
      } else {
        sdata.set(i, coordValue);
        timeDates[i] = d;
      }
    }
    setCachedData(sdata, true);
  }

  private CoordinateAxis1DTime(NetcdfDataset ncd, VariableDS org, Formatter errMessages) throws IOException {
    super(ncd, org);

    // named = new ArrayList<NamedObject>(); // declared in CoordinateAxis1D superclass

    int ncoords = (int) org.getSize();
    timeDates = new Date[ncoords];

    // see if it has a valid udunits unit
    DateUnit dateUnit = null;
    String units = org.getUnitsString();
    if (units != null) {
      try {
         dateUnit = new DateUnit(units);
       } catch (Exception e) {
        // not a date unit - ok to fall through
      }
    }

    // has a valid date unit - read data
    if (dateUnit != null) {
      Array data = org.read();

      int count = 0;
      IndexIterator ii = data.getIndexIterator();
      for (int i = 0; i < ncoords; i++) {
        double val = ii.getDoubleNext();
        if (Double.isNaN(val)) continue;
        Date d = dateUnit.makeDate(val);
        timeDates[count++] = d;
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

        // shorten up the timeDate array
        Date[] keep = timeDates;
        timeDates = new Date[count];
        System.arraycopy(keep, 0, timeDates, 0, timeDates.length);
      }

      return;
    } // has valid date unit

    //  see if its a String, and if we can parse the values as an ISO date
    if (org.getDataType() == DataType.STRING) {
      ArrayObject data = (ArrayObject) org.read();
      IndexIterator ii = data.getIndexIterator();
      for (int i = 0; i < ncoords; i++) {
        String coordValue = (String) ii.getObjectNext();
        Date d = DateUnit.getStandardOrISO(coordValue);
        if (d == null) {
          if (errMessages != null)
            errMessages.format("DateUnit cannot parse String= %s\n", coordValue);
          else
            System.out.println("DateUnit cannot parse String= " + coordValue + "\n");

          throw new IllegalArgumentException();
        } else {
          timeDates[i] = d;
        }
      }
      return;
    }

    // hack something in here so it doesnt fail
    if (units != null) {
      try {
        // if in time unit, use CF convention "since 1-1-1 0:0:0"
        dateUnit = new DateUnit(units+" since 0001-01-01 00:00:00");
      } catch (Exception e) {
        try {
          if (errMessages != null)
            errMessages.format("Time Coordinate must be udunits or ISO String: hack since 0001-01-01 00:00:00\n");
          else
            System.out.println("Time Coordinate must be udunits or ISO String: hack since 0001-01-01 00:00:00\n");
          dateUnit = new DateUnit("secs since 0001-01-01 00:00:00");
        } catch (Exception e1) {
          // cant happpen
        }
      }
    }

    Array data = org.read();
    IndexIterator ii = data.getIndexIterator();
    for (int i = 0; i < ncoords; i++) {
      double val = ii.getDoubleNext();
      Date d = dateUnit.makeDate(val);
      timeDates[i] = d;
    }
  }

  private CoordinateAxis1DTime(NetcdfDataset ncd, CoordinateAxis1DTime org, Date[] timeDates) {
    super(ncd, org);
    this.timeDates = timeDates;
    this.dateUnit = org.dateUnit;
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new CoordinateAxis1DTime(this.ncd, this, getTimeDates());
  }

  /**
   * Get the list of times as Dates.
   *
   * @return array of java.util.Date, or null.
   */
  public java.util.Date[] getTimeDates() {
    return timeDates;
  }

  public java.util.Date getTimeDate (int idx) {
    return timeDates[idx];
  }

  public DateRange getDateRange() {
    return new DateRange(timeDates[0], timeDates[timeDates.length - 1]);
  }

  @Override
  public List<NamedObject> getNames() {
    DateFormatter formatter = new DateFormatter();
    int n = (int) getSize();
    List<NamedObject> names = new ArrayList<NamedObject>(n);
    for (int i = 0; i < n; i++) {
      names.add(new NamedAnything(formatter.toDateTimeString(getTimeDate(i)), "date/time"));
    }
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
  public int findTimeIndexFromDate(java.util.Date d) {
    int n = timeDates.length;
    long m = d.getTime();
    int index = 0;
    while (index < n) {
      if (m < timeDates[index].getTime())
        break;
      index++;
    }
    return Math.max(0, index - 1);
  }

  /**
   * See if the given Date appears is a coordinate
   *
   * @param date test this
   * @return true if equals a coordinate
   */
  public boolean hasTime(Date date) {
    for (Date timeDate : timeDates) {
      if (date.equals(timeDate))
        return true;
    }
    return false;
  }

}
