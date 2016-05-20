/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.ft.fmrc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NCdumpW;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.util.Formatter;

/**
 * misc tests on Fmrc
 *
 * @author caron
 * @since 3/17/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestFmrcMisc {
  @Test
  public void testConventionsAttribute() throws Exception {
    String path = TestDir.cdmUnitTestDir + "ncml/AggForecastModel.ncml";
    Formatter errlog = new Formatter();
    Fmrc fmrc = Fmrc.open(path, errlog);
    assert (fmrc != null) : errlog;

    try (ucar.nc2.dt.GridDataset gridDs = fmrc.getDataset2D(null)) {
      NetcdfDataset ncd = (NetcdfDataset) gridDs.getNetcdfFile();
      Attribute att = ncd.findGlobalAttribute(CDM.CONVENTIONS);
      assert att != null;
      System.out.printf("%s%n", att);
    }
  }

  @Test
  public void testFloatingPointCompare() throws Exception {
    String spec = TestDir.cdmUnitTestDir+"ft/fmrc/fp_precision/sediment_thickness_#yyMMddHHmm#.*\\.nc$";
    System.out.printf("%n====================FMRC dataset %s%n", spec);
    Formatter errlog = new Formatter();
    Fmrc fmrc = Fmrc.open(spec, errlog);
    assert (fmrc != null) : errlog;

    try (ucar.nc2.dt.GridDataset gridDs = fmrc.getDatasetBest()) {
      GridDatatype v = gridDs.findGridByShortName("thickness_of_sediment");
      assert v != null;
      GridCoordSystem gcs = v.getCoordinateSystem();
      CoordinateAxis1DTime time = gcs.getTimeAxis1D();

      Assert.assertEquals("hours since 2015-03-08 12:51:00.000 UTC", time.getUnitsString());
      Assert.assertEquals(74, time.getSize());
      Array data = time.read();
      System.out.printf("%s%n", NCdumpW.toString(data));

      for (CalendarDate cd : time.getCalendarDates()) {
        assert cd.getFieldValue(CalendarPeriod.Field.Minute) == 0 : System.out.printf("%s%n", cd);
      }
    }
  }


}
