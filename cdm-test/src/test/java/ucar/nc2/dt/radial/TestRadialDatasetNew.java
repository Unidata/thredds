/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.dt.radial;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ucar.nc2.dt.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/** Test radial datasets in the JUnit framework. */

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestRadialDatasetNew {

    @Parameterized.Parameters(name="{0}")
    public static Collection params() {
        Object[][] data = new Object[][] {
                {"formats/nexrad/level3/N0R_20041119_2147",
                        CalendarDate.of(null, 2004, 11, 19, 21, 47, 44),
                        CalendarDate.of(null, 2004, 11, 19, 21, 47, 44)},
                {"formats/dorade/swp.1020511015815.SP0L.573.1.2_SUR_v1",
                        CalendarDate.of(null, 2002, 5, 11, 1, 58, 15).add(573, CalendarPeriod.Field.Millisec),
                        CalendarDate.of(null, 2002, 5, 11, 1, 59, 5).add(687, CalendarPeriod.Field.Millisec)}
        };
        return Arrays.asList(data);
    }

    @Parameterized.Parameter
    public String filename;

    @Parameterized.Parameter(value=1)
    public CalendarDate start;

    @Parameterized.Parameter(value=2)
    public CalendarDate end;

    @Test
    public void testDates() throws IOException {
        String fullpath = TestDir.cdmUnitTestDir + filename;
        RadialDatasetSweep rds = (RadialDatasetSweep) TypedDatasetFactory.open(
                FeatureType.RADIAL, fullpath, null, new StringBuilder());

        Assert.assertEquals(start, rds.getCalendarDateStart());
        Assert.assertEquals(end, rds.getCalendarDateEnd());
    }
}
