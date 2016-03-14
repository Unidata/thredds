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

import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;
import java.util.ArrayList;
import java.util.List;

import ucar.nc2.Variable;
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
    String filename = TestDir.cdmLocalTestDataDir +"testWriteFill.nc";
    String varWithFill = "temperature";
    try (NetcdfDataset ncfile = NetcdfDataset.openDataset(filename)) {
      VariableDS v = (VariableDS) ncfile.findVariable(varWithFill);
      Assert.assertNotNull(varWithFill, v);
      Assert.assertTrue(v.hasFillValue());
      Assert.assertNotNull(v.findAttribute("_FillValue"));

      Array data = v.read();
      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext()) {
        double val = iter.getDoubleNext();
        Assert.assertTrue( Double.toString(val), Double.isNaN(val));
      }

      // all other variables are using the default fill values
      for (Variable v2 : ncfile.getVariables()) {
        if (v2.getShortName().equals(varWithFill)) continue;
        VariableDS ve = (VariableDS) v2;
        Assert.assertFalse(ve.getShortName(), ve.hasFillValue());
        Assert.assertNull(ve.getShortName(), ve.findAttribute("_FillValue"));

        Array data2 = v2.read();
        IndexIterator iter2 = data2.getIndexIterator();
        while (iter2.hasNext()) {
          double val = iter2.getDoubleNext();
          Assert.assertFalse(Double.toString(val), Double.isNaN(val));
        }
      }

    }
  }


}
