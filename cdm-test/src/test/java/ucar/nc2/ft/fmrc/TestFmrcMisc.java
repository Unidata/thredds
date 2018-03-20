/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.fmrc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/**
 * misc tests on Fmrc
 *
 * @author caron
 * @since 3/17/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestFmrcMisc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
      logger.debug("{}", NCdumpW.toString(data));

      for (CalendarDate cd : time.getCalendarDates()) {
        assert cd.getFieldValue(CalendarPeriod.Field.Minute) == 0 : System.out.printf("%s%n", cd);
      }
    }
  }
}
