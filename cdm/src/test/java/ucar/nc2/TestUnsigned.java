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

import ucar.ma2.Index;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.DataBuffer;
import java.awt.*;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import junit.framework.TestCase;

/**
 * Test that adding _Unsigned attribute in NcML works correctly
 *
 * @author caron
 * @since Aug 7, 2008
 */
public class TestUnsigned extends TestCase {

  public TestUnsigned( String name) {
    super(name);
  }

  public void testSigned() throws IOException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(TestAll.cdmLocalTestDataDir + "testWrite.nc");

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

  public void testUnsigned() throws IOException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(TestAll.cdmLocalTestDataDir + "testUnsignedByte.ncml");

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

  public void testUnsignedWrap() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"+TestAll.cdmLocalTestDataDir +"testWrite.nc'>\n" +
        "  <variable name='bvar' shape='lat' type='byte'>\n" +
        "    <attribute name='_Unsigned' value='true' />\n" +
        "    <attribute name='scale_factor' type='float' value='2.0' />\n" +
        "   </variable>\n" +
        "</netcdf>";

    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
    NetcdfFile ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll());

    Variable v = null;
    assert(null != (v = ncd.findVariable("bvar")));
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

  public static byte[] convert(String srcPath, double a, double b) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(srcPath);
    try {
      Variable v = ncfile.findVariable("image1/image_data");
      Array array = v.read();

      int[] cmap = new int[256]; // palette
      cmap[0] = 0x00FFFFFF; // transparent and white
      for (int i = 1; i != 256; i++) {
        // 1 to 255 renders as (almost) white to black
        cmap[i] = 0xFF000000 | ((0xFF - i) * 0x010101);
      }
      IndexColorModel colorModel = new IndexColorModel(8,
              cmap.length, cmap, 0, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

      int[] shape = array.getShape();
      BufferedImage bi = new BufferedImage(shape[1], shape[0],BufferedImage.TYPE_BYTE_INDEXED, colorModel);

      Index index = array.getIndex();
      for (int y = 0; y < shape[0]; y++) {
        for (int x = 0; x < shape[1]; x++) {
          index.set(y, x);

          byte bval = array.getByte(index);
          double dval = v.isUnsigned() ? (double) DataType.unsignedByteToShort(bval) : (double) bval;

          //double dval = array.getDouble(index);
          // Fix for NetCDF returning all values larger than 127 as (value - 256):
          //if (dval < -1) {
          //  dval += 256;
          //}
          int pval = (int) Math.round(a * dval + b);
          pval = Math.min(Math.max(pval, 0), 255);
          bi.getRaster().setSample(x, y, 0, pval);
        }
      }

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ImageIO.write(bi, "png", os);
      return os.toByteArray();

    } finally {
      ncfile.close();
    }
  }

  public static void main(String args[]) throws IOException {
    convert("C:/data/test/RAD_NL25_PCP_NA_200808070810.h5", 1.0, 0.0);
  }

}
