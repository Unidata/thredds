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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Test CoordSys COnventions
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestConventions  {

  @Test
  public void testWRF() throws IOException {
    testWRF(TestDir.cdmUnitTestDir + "conventions/wrf/wrf-ver1.3.nc");
  }

  private void testWRF(String location) throws IOException {
    NetcdfDataset ds = NetcdfDataset.openDataset(location);
    ds.close();
  }

  @Test
  public void testCF() throws IOException {
    GridDataset ds = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/cf/twoGridMaps.nc");
    GeoGrid grid = ds.findGridByName("altitude");
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert 1 == gcs.getCoordinateTransforms().size();
    CoordinateTransform ct = gcs.getCoordinateTransforms().get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct.getName().equals("projection_stere");
    ds.close();
  }

  @Test
  public void testCOARDSdefaultCalendar() throws IOException {
    GridDataset ds = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/coards/olr.day.mean.nc");
    GeoGrid grid = ds.findGridByName("olr");
    assert grid != null;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1DTime time = gcs.getTimeAxis1D();
    assert time != null;

    CalendarDate first = time.getCalendarDate(0);
    CalendarDate cd = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, "2002-01-01T00:00:00Z");
    assert first.equals(cd) : first + " != " + cd;
    CalendarDate last = time.getCalendarDate((int)time.getSize()-1);
    CalendarDate cd2 = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, "2012-12-02T00:00:00Z");
    assert last.equals(cd2) : last + " != " + cd2;
  }
}
