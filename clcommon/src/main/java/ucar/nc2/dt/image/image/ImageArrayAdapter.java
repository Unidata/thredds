/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.image.image;

import ucar.ma2.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.color.*;

/**
 * Makes a 2D Array into a java.awt.image.BufferedImage
 *
 * @author caron
 */
public class ImageArrayAdapter {

  /**
   * Adapt a rank 2 array into a java.awt.image.BufferedImage.
   * If passed a rank 3 array, take first 2D slice.
   * @param ma rank 2 or 3 array.
   * @return BufferedImage
   */
  public static java.awt.image.BufferedImage makeGrayscaleImage( Array ma, IsMissingEvaluator missEval) {
    if (ma.getRank() < 2) return null;

    if (ma.getRank() == 3)
      ma = ma.reduce();

    if (ma.getRank() == 3)
      ma = ma.slice( 0, 0); // we need 2D

    int h = ma.getShape()[0];
    int w = ma.getShape()[1];
    DataBuffer dataBuffer = makeDataBuffer(ma, missEval);

    WritableRaster raster = WritableRaster.createInterleavedRaster(dataBuffer,
        w, h, //   int w, int h,
        w, //   int scanlineStride,
        1,      //    int pixelStride,
        new int[]{0}, //   int bandOffsets[],
        null);     //   Point location)

    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    ComponentColorModel colorModel = new ComponentColorModel(cs,new int[] {8},
        false,false,Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    return new BufferedImage( colorModel, raster, false, null);
  }

  private static DataBuffer makeDataBuffer( Array ma, IsMissingEvaluator missEval) {
    if (ma instanceof ArrayByte)
      return makeByteDataBuffer( (ArrayByte) ma);

    MAMath.MinMax minmax = MAMath.getMinMaxSkipMissingData(ma, missEval);
    double diff = (minmax.max - minmax.min);
    boolean hasMissing = (missEval != null) && missEval.hasMissing();
    int n = hasMissing ? 254 : 255;
    double scale = (diff > 0.0) ? n / diff : 1.0;

    IndexIterator ii = ma.getIndexIterator();
    int h = ma.getShape()[0];
    int w = ma.getShape()[1];
    byte[] byteData = new byte[h*w];
    for (int i = 0; i < byteData.length; i++) {
      double val = ii.getDoubleNext();
      if (missEval != null && missEval.isMissing(val)) {
        byteData[i] = (byte) 255; // missing
      } else {
        double sval = ((val - minmax.min) * scale);
        byteData[i] = (byte) (sval); //  < 128.0 ? sval : sval - 255.0);
      }
    }

    return new DataBufferByte(byteData, byteData.length);
  }

  private static DataBuffer makeByteDataBuffer( ArrayByte ma) {
    byte[] byteData = (byte []) ma.copyTo1DJavaArray();
    return new DataBufferByte(byteData, byteData.length);
  }

}

