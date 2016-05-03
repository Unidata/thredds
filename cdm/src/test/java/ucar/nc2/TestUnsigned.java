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

package ucar.nc2;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.StringReader;

/**
 * Test that adding _Unsigned attribute in NcML works correctly
 *
 * @author caron
 * @since Aug 7, 2008
 */
public class TestUnsigned {

  @Test
  public void testSigned() throws IOException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testWrite.nc");

    Variable v = null;
    assert(null != (v = ncfile.findVariable("bvar")));
    assert !v.isUnsigned();
    assert v.getDataType() == DataType.BYTE;

    boolean hasSigned = false;
    Array data = v.read();
    while (data.hasNext()) {
      byte b = data.nextByte();
      if (b < 0) hasSigned = true;
    }
    assert hasSigned;

    ncfile.close();
  }

  @Test
  public void testUnsigned() throws IOException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testUnsignedByte.ncml");

    Variable v = null;
    assert(null != (v = ncfile.findVariable("bvar")));
    assert v.getDataType() == DataType.FLOAT;

    boolean hasSigned = false;
    Array data = v.read();
    while (data.hasNext()) {
      float b = data.nextFloat();
      if (b < 0) hasSigned = true;
    }
    assert !hasSigned;

    ncfile.close();
  }

  @Test
  public void testUnsignedWrap() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"+TestDir.cdmLocalTestDataDir +"testWrite.nc'>\n" +
        "  <variable name='bvar' shape='lat' type='byte'>\n" +
        "    <attribute name='_Unsigned' value='true' />\n" +
        "    <attribute name='scale_factor' type='float' value='2.0' />\n" +
        "   </variable>\n" +
        "</netcdf>";

    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
    NetcdfFile ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll());

    Variable v = ncd.findVariable("bvar");
    assert(null != v);
    assert v.getDataType() == DataType.FLOAT;

    boolean hasSigned = false;
    Array data = v.read();
    while (data.hasNext()) {
      float b = data.nextFloat();
      if (b < 0) hasSigned = true;
    }
    assert !hasSigned;

    ncd.close();
  }

}
