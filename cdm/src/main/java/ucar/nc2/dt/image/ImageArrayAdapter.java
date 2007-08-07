/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt.image;

import ucar.ma2.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.color.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import javax.swing.*;

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
  public static java.awt.image.BufferedImage makeGrayscaleImage( Array ma) {

    if (ma.getRank() == 3)
      ma = ma.reduce();

    if (ma.getRank() == 3)
      ma = ma.slice( 0, 0); // we need 2D

    int h = ma.getShape()[0];
    int w = ma.getShape()[1];
    DataBuffer dataBuffer = makeDataBuffer( ma);

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

  private static DataBuffer makeDataBuffer( Array ma) {
    if (ma instanceof ArrayByte)
      return makeByteDataBuffer( (ArrayByte) ma);

    int h = ma.getShape()[0];
    int w = ma.getShape()[1];

    double min = MAMath.getMinimum(ma); // LOOK we need missing values to be removed !!
    double max = MAMath.getMaximum(ma);
    double scale = (max - min);
    if (scale > 0.0)
      scale = 255.0 / scale;

    IndexIterator ii = ma.getIndexIterator();
    byte[] byteData = new byte[h*w];
    for (int i = 0; i < byteData.length; i++) {
      double val = ii.getDoubleNext();
      double sval = ((val - min) * scale);
      byteData[i] = (byte) (sval); //  < 128.0 ? sval : sval - 255.0);
    }

    return new DataBufferByte(byteData, byteData.length);
  }

  private static DataBuffer makeByteDataBuffer( ArrayByte ma) {
    byte[] byteData = (byte []) ma.copyTo1DJavaArray();
    return new DataBufferByte(byteData, byteData.length);
  }

    /* ComponentColorModel colorModel = new  ComponentColorModel (cs,
        new int[] {16}, // int[] bits,
        false, false, //  boolean hasAlpha,  boolean isAlphaPremultiplied,
        Transparency.OPAQUE,DataBuffer.TYPE_BYTE); // int transparency,  int transferType)

/*
    // create a sample model
    int bitMasks[] = new int[]{0xffff, 0xffff, 0xffff};
    SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, nelems, nlines, bitMasks);
      new MultiPixelPackedSampleModel(DataBuffer.TYPE_USHORT, nelems, nlines, 16);
    WritableRaster raster = Raster.createWritableRaster( sampleModel, dataBuffer, null);

    // create a clor model
    ColorModel colorModel = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB),
                        16, // int bits,
                        0xffff, // int rmask,
                        0xffff, //int gmask,
                        0xffff, // int bmask,
                        0, // amask
                        false,
                        Transparency.OPAQUE);


    //new ComponentColorModel( ColorSpace.getInstance(ColorSpace.CS_GRAY),
    //    false, false, Transparency.OPAQUE, java.awt.image.DataBuffer.TYPE_USHORT); */



    // long timeEnd = System.currentTimeMillis();
    // if (Debug.isSet("ADDE/AddeImage/createTiming")) System.out.println("ADDE/AddeImage/createTiming AddeImage = "+ .001*(timeEnd - timeStart)+" sec");


  private static JLabel test() {
    byte[] tmp2 = new byte[900*641];
    for (int i = 0; i < tmp2.length; i++) {
      int row = i / 640;
      int col = i % 640;
      tmp2[i] = (byte) ( row * 255.0 / 900.0);
    }

    DataBuffer db = new DataBufferByte(tmp2,tmp2.length);

    WritableRaster raster = WritableRaster.createInterleavedRaster
    (db,900,640,900,1,new int[]{0},null);

    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    ComponentColorModel cm = new ComponentColorModel(cs,new int[]
    {8},false,false,Transparency.OPAQUE,DataBuffer.TYPE_BYTE);

    BufferedImage image = new BufferedImage(cm, raster,true,null);

    Graphics2D g2d = image.createGraphics();

    // Draw on the image
    g2d.setColor(Color.red);
    g2d.draw(new Ellipse2D.Float(0, 0, 200, 100));
    g2d.dispose();

    // Make all filled pixels transparent
    Color transparent = new Color(0, 0, 0, 0);
    g2d.setColor(transparent);
    g2d.setComposite(AlphaComposite.Src);
    g2d.fill(new Rectangle2D.Float(320, 20, 100, 20));
    g2d.dispose();

    return new JLabel(new ImageIcon(image));
  }

  static public void main( String[] args) {

    Array data = Array.factory(int.class, new int[] {255, 100} );
    int count = 0;
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      int row = count/100;
      ii.setIntNext(row*255);
      count++;
    }

    BufferedImage image = ImageArrayAdapter.makeGrayscaleImage( data);

    Raster raster = image.getRaster();
    DataBuffer ds = raster.getDataBuffer();
    SampleModel sm = raster.getSampleModel();

    System.out.println(" image type = "+image.getType());
    System.out.println(" transfer type = "+sm.getTransferType());

    // see if we can operate on it
    AffineTransform at = new AffineTransform();  // identity transform
    AffineTransformOp op = new AffineTransformOp( at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR );
    BufferedImage targetImage = new BufferedImage(
             image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR );

    BufferedImage image2 = op.filter( image, targetImage );

    System.out.println("ok!");

    boolean display = true;
    if (!display) return;

    // show it !!
    JFrame frame = new JFrame("Test");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    ImageIcon icon = new ImageIcon(image2);
    JLabel lab = new JLabel( icon);

    JPanel main = new JPanel();
    main.add(lab);

    frame.getContentPane().add(main);

    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);
  }

}

