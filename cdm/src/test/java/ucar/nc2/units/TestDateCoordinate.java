/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.ncml.TestNcML;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;

/**
 * Describe
 *
 * @author caron
 * @since Jul 1, 2009
 */
public class TestDateCoordinate extends TestCase {

  public TestDateCoordinate(String name) {
    super(name);
  }

  NetcdfDataset ncfile = null;

  public void setUp() {
    String filename = "file:" + TestNcML.topDir + "testDateUnits.ncml";

    try {
      ncfile = NetcdfDataset.openDataset(filename);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = " + e);
    } catch (IOException e) {
      System.out.println("IO error = " + e);
      e.printStackTrace();
    }
  }

  protected void tearDown() throws IOException {
    ncfile.close();
  }

  public void utestCoordinateAxis1DTime() {

    Variable time = ncfile.findVariable("time");
    assert time instanceof CoordinateAxis;
    assert time instanceof CoordinateAxis1DTime;

    CoordinateAxis1DTime timeAxis = (CoordinateAxis1DTime) time;

    for (Date d : timeAxis.getTimeDates()) {
      System.out.printf(" %s%n", d);
    }

  }

  public void testDateCoordinates() throws Exception {

    Variable time = ncfile.findVariable("time");
    Date first = null;

    DateFormatter format = new DateFormatter();
    String units = time.getUnitsString();
    DateUnit du = new DateUnit(time.getUnitsString());
    System.out.printf("units= %s%n", units);
    System.out.printf("DateUnit= %s (%s)%n", du, du.getUnitsString());

    Array data = time.read();
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      double val = ii.getDoubleNext();
      Date date = du.makeDate(val);
      System.out.printf(" %f %s ==  %s%n", val, units, format.toDateTimeStringISO(date));
      if (first == null) first = date;
    }

    assert (format.toDateTimeStringISO(first).equals("2009-06-15T04:00:00Z"));

  }


}