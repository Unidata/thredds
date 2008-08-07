/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2;

import ucar.ma2.Index;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.DataBuffer;
import java.awt.*;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 7, 2008
 */
public class TestUnsigned {

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
