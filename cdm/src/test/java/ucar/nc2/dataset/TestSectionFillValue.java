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

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;
import java.util.ArrayList;
import java.util.List;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.unidata.test.util.TestDir;

/**
 *  from (WUB-664639) (Didier Earith)
 *
 * @author caron
 */
public class TestSectionFillValue {

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
