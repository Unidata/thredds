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
package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.StringReader;

/**
 * Class Description
 *
 * @author caron
 * @since Nov 6, 2009
 */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggFmrcMisc {
  String location = TestDir.cdmUnitTestDir +"ft/fmrc/efine/";

  @Test
  public void testScaling() throws Exception {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='true' >\n" +
      "  <aggregation dimName='runtime' type='forecastModelRunCollection' timeUnitsChange='true'>\n" +
      "    <scan location='"+location+"' suffix='.nc' dateFormatMark='#yyyyMMddHH' enhance='true' />" +
      "  </aggregation>\n" +
      "</netcdf>";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(xml), location, null);

    // make sure that scaling is applied
    VariableDS vs = (VariableDS) ncfile.findVariable("hs");
    Array data = vs.read("0,1,:,:)");
    while (data.hasNext()) {
      float val = data.nextFloat();
      if (!vs.isMissing(val))
        assert (val < 10.0) : val;
      //System.out.printf("%f %n",val);
    }

    ncfile.close();
  }

  @Test
  public void testScaling2() throws Exception {
    NetcdfFile ncfile = NetcdfDataset.acquireFile(location+"fine.ncml", null);

    // make sure that scaling is applied
    VariableDS vs = (VariableDS) ncfile.findVariable("hs");
    Array data = vs.read("0,1,:,:)");
    while (data.hasNext()) {
      float val = data.nextFloat();
      if (!vs.isMissing(val))
        assert (val < 10.0) : val;
      //System.out.printf("%f %n",val);
    }

    ncfile.close();
  }

}

