// $Id: $
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

package ucar.nc2.dataset;

import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.ma2.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.io.IOException;

import thredds.datatype.DateRange;

/**
 * A 1-dimensional Coordinate Axis representing Calendar time.
 * Its coordinate values can be represented as Dates.
 *
 * May use udunit dates, or ISO Strings.
 *
 * @author caron
 * @version $Revision: 70 $ $Date: 2006-07-13 15:16:05Z $
 */
public class CoordinateAxis1DTime extends CoordinateAxis1D {
  private Date[] timeDates;
  private DateUnit dateUnit;

  /**
   * Constructor.
   *
   * @param org the underlying Variable
   * @param errMessages put error messages here; may be null
   * @throws IOException
   * @throws IllegalArgumentException if cant convert coordinate values to a Date
   */
  public CoordinateAxis1DTime( VariableDS org, StringBuffer errMessages) throws IOException {
    super( org);

    named = new ArrayList(); // declared in CoordinateAxis1D superclass

    int ncoords = (int) org.getSize();

    // deal with type char- basically make it into a string
    if (org.getDataType() == DataType.CHAR) {
      int rank = org.getRank();
      int strlen = org.getShape()[rank-1];
      ncoords /= strlen;

      /* setDataType( DataType.STRING);
      List dims = org.getDimensions();
      dims.remove( dims.size()-1); // remove the last one.
      setDimensions(dims); */
    }
    timeDates = new Date[ncoords];

    // see if it has a valid udunits unit
    SimpleUnit su = null;
    String units = org.getUnitsString();
    if (units != null)
      su = SimpleUnit.factory( units);

    if ((su != null) && (su instanceof DateUnit)) {
      Array data = org.read();
      int count = 0;
      IndexIterator ii = data.getIndexIterator();
      DateFormatter formatter = new DateFormatter();
      dateUnit = (DateUnit) su;
      for (int i = 0; i < ncoords; i++) {
        double val = ii.getDoubleNext();
        if (Double.isNaN(val)) continue;
        Date d = dateUnit.makeDate( val);
        String name = formatter.toDateTimeString(d);
        named.add(new NamedAnything(name, "date/time"));
        timeDates[count++] = d;
      }

      // if we encountered NaNs, shorten it up
      if (count != ncoords) {
        Dimension localDim = new Dimension( getName(), count, false);
        setDimension(0, localDim);

        // set the shortened values
        Array shortData = Array.factory( data.getElementType(), new int[] {count});
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
        orgVar = null;
        isSlice = false;
        isSection = false;
        cache = new Cache();
        setCachedData( shortData, true);

        // shorten up the timeDate array
        Date[] keep = timeDates;
        timeDates = new Date[count];
        for (int i = 0; i < timeDates.length; i++) {
          timeDates[i] = keep[i];
        }
      }

      return;
    }

    //  see if its a String, and if we can parse the values as an ISO date
    if (org.getDataType() == DataType.STRING) {
      ArrayObject data = (ArrayObject) org.read();
      IndexIterator ii = data.getIndexIterator();
      DateFormatter formatter = new DateFormatter();
      for (int i=0; i<ncoords; i++) {
        String coordValue = (String) ii.getObjectNext();
        Date d = DateUnit.getStandardOrISO( coordValue);
        if (d == null) {
          if (errMessages != null)
            errMessages.append("DateUnit cannot parse String= "+coordValue+"\n");
          else
            System.out.println("DateUnit cannot parse String= "+coordValue+"\n");

          throw new IllegalArgumentException();
        } else {
          named.add(new NamedAnything(formatter.toDateTimeString(d), "date/time"));
          timeDates[i] = d;
        }
      }
      return;
    }

    // otherwise, see if its a CHAR, and if we can parse the values as an ISO date
    if (org.getDataType() == DataType.CHAR) {
      ArrayChar data = (ArrayChar) org.read();
      ArrayChar.StringIterator ii = data.getStringIterator();
      DateFormatter formatter = new DateFormatter();
      for (int i=0; i<ncoords; i++) {
        String coordValue = ii.next();
        Date d = DateUnit.getStandardOrISO( coordValue);
        if (d == null) {
          if (errMessages != null)
            errMessages.append("DateUnit cannot parse String= "+coordValue+"\n");
          else
            System.out.println("DateUnit cannot parse String= "+coordValue+"\n");

          throw new IllegalArgumentException();
        } else {
          named.add(new NamedAnything(formatter.toDateTimeString(d), "date/time"));
          timeDates[i] = d;
        }
      }
      return;
    }

    // hack something in here so it doesnt fail

    // if in time unit, use CF convention "since 1-1-1 0:0:0"
    if ((su != null) && (su instanceof TimeUnit)) {
      su = SimpleUnit.factory( units+" since 0001-01-01 00:00:00");
    } else {
      su = SimpleUnit.factory("secs since 0001-01-01 00:00:00");
    }
    if (errMessages != null)
      errMessages.append("Time Coordinate must be udunits or ISO String: hack since 0001-01-01 00:00:00\n");
    else
      System.out.println("Time Coordinate must be udunits or ISO String: hack since 0001-01-01 00:00:00\n");

    Array data = org.read();
    IndexIterator ii = data.getIndexIterator();
    DateFormatter formatter = new DateFormatter();
    dateUnit = (DateUnit) su;
    for (int i = 0; i < ncoords; i++) {
      double val = ii.getDoubleNext();
      Date d = dateUnit.makeDate( val);
      String name = formatter.toDateTimeString(d);
      named.add(new NamedAnything(name, "date/time"));
      timeDates[i] = d;
    }
  }

  /**
   * Get the list of times as Dates.
   * @return array of java.util.Date, or null.
   */
  public java.util.Date[] getTimeDates() { return timeDates; }

  public DateRange getDateRange() {
    return new DateRange(timeDates[0], timeDates[timeDates.length - 1]);
  }

  /** only if isRegular() LOOK REDO */
  public TimeUnit getTimeResolution() throws Exception {
    String tUnits = getUnitsString();
    StringTokenizer stoker = new StringTokenizer( tUnits);
    double tResolution = getIncrement();
    return new TimeUnit( tResolution, stoker.nextToken());
  }

  /**
   * Given a Date, find the corresponding time index on the time coordinate axis.
   * Can only call this is hasDate() is true.
   * This will return
   * <ul>
   * <li> i, if time(i) <= d < time(i+1).
   * <li> -1, if d < time(0)
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
    return index - 1;
  }

  // override to keep section a CoordinateAxis1DTime
  public Variable section(List section) throws InvalidRangeException {
    Variable vs = null;
    try {
      vs = new CoordinateAxis1DTime(this, null);
    } catch (IOException e) {
      e.printStackTrace();  // cant happen - haha
    }
    makeSection(vs, section);
    return vs;
  }
}
