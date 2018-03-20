/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.unidata.util.test.TestDir;

/**
 *  from (WUB-664639) (Didier Earith)
 *
 * @author caron
 */
public class TestSectionFillValue {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testExplicitFillValue() throws Exception {
    String filename = TestDir.cdmLocalTestDataDir +"standardVar.nc";
    try (NetcdfDataset ncfile = NetcdfDataset.openDataset(filename)) {
      VariableDS v = (VariableDS) ncfile.findVariable("t3");
      Assert.assertNotNull("t3", v);
      Assert.assertTrue(v.hasFillValue());
      Assert.assertNotNull(v.findAttribute("_FillValue"));

      int rank = v.getRank();
      List<Range> ranges = new ArrayList<>();
      ranges.add(null);
      for (int i = 1; i < rank; i++) {
        ranges.add(new Range(0, 1));
      }

      VariableDS v_section = (VariableDS) v.section(ranges);
      Assert.assertNotNull (v_section.findAttribute("_FillValue"));
      System.out.println(v_section.findAttribute("_FillValue"));
      Assert.assertTrue(v_section.hasFillValue());
    }
  }

  @Test
  public void testImplicitFillValue() throws Exception {
    String filename = TestDir.cdmLocalTestDataDir + "testWriteFill.nc";
    List<String> varWithFill = Lists.newArrayList("temperature", "rtemperature");
    try (NetcdfFile ncfile = NetcdfDataset.openFile(filename, null);
         NetcdfDataset ncd = NetcdfDataset.openDataset(filename)) {

      for (Variable v : ncfile.getVariables()) {
        if (!v.getDataType().isNumeric()) continue;
        System.out.printf("testImplicitFillValue for %s type=%s%n", v.getShortName(), v.getDataType());

        VariableDS ve = (VariableDS) ncd.findVariable(v.getFullName());
        if (varWithFill.contains(v.getShortName())) {
          Assert.assertNotNull(v.findAttribute("_FillValue"));
          Assert.assertTrue(ve.hasFillValue());
          Number fillValue = v.findAttribute("_FillValue").getNumericValue();

          Array data = v.read();
          Array dataE = ve.read();

          IndexIterator iter = data.getIndexIterator();
          IndexIterator iterE = dataE.getIndexIterator();
          while (iter.hasNext() && iterE.hasNext()) {
            Object val = iter.next();
            Object vale = iterE.next();
            double vald = ((Number) val).doubleValue();
            double valde = ((Number) vale).doubleValue();
            if (ve.isFillValue(vald)) {
              if (v.getDataType().isFloatingPoint())
                Assert.assertTrue(Double.toString(valde), Double.isNaN(valde));
              else
                Assert.assertTrue(vale.toString(), fillValue.equals(vale));
            }
          }
        } else {
          Assert.assertNull(v.findAttribute("_FillValue"));
          Assert.assertTrue(ve.hasFillValue());
          Number fillValue = N3iosp.getFillValueDefault(v.getDataType());
          Assert.assertNotNull(v.getDataType().toString(), fillValue);

          Array data = v.read();
          Array dataE = ve.read();

          IndexIterator iter = data.getIndexIterator();
          IndexIterator iterE = dataE.getIndexIterator();
          while (iter.hasNext() && iterE.hasNext()) {
            Object val = iter.next();
            Object vale = iterE.next();
            double vald = ((Number) val).doubleValue();
            double valde = ((Number) vale).doubleValue();
            if (val.equals(fillValue))
              Assert.assertTrue(ve.isFillValue(vald));

            if (ve.isFillValue(vald)) {
              if (v.getDataType().isFloatingPoint())
                Assert.assertTrue(Double.toString(valde), Double.isNaN(valde));
              else
                Assert.assertTrue(vale.toString(), fillValue.equals(vale));
            }
          }
        }


      }
    }
  }


}
