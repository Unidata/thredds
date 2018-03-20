/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.radial;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dt.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;

/**
 * Test radial datasets in the JUnit framework.
 */

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestRadialDatasetNew {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static Collection params() {
    Object[][] data = new Object[][]{
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

  @Parameterized.Parameter(value = 1)
  public CalendarDate start;

  @Parameterized.Parameter(value = 2)
  public CalendarDate end;

  @Test
  public void testDates() throws IOException {
    String fullpath = TestDir.cdmUnitTestDir + filename;
    Formatter errlog = new Formatter();
    RadialDatasetSweep rds = (RadialDatasetSweep) FeatureDatasetFactoryManager.open(FeatureType.RADIAL, fullpath, null, errlog);

    Assert.assertEquals(start, rds.getCalendarDateStart());
    Assert.assertEquals(end, rds.getCalendarDateEnd());
  }
}
