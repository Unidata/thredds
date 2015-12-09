/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.iosp.gini;

import ucar.nc2.constants.DataFormatType;
import ucar.ma2.*;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import java.io.*;
import java.awt.image.*;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

/**
 * IOServiceProvider for GINI files.
 */

public class Giniiosp extends AbstractIOServiceProvider {

  protected Giniheader headerParser;

  final static int Z_DEFLATED = 8;
  final static int DEF_WBITS = 15;

  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, java.util.List section)
          throws java.io.IOException, ucar.ma2.InvalidRangeException {

    throw new UnsupportedOperationException("Gini IOSP does not support nested variables");
  }

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    Giniheader localHeader = new Giniheader();
    return (localHeader.isValidFile(raf));
  }

  /////////////////////////////////////////////////////////////////////////////
  // reading

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {

    super.open(raf, ncfile, cancelTask);

    headerParser = new Giniheader();
    headerParser.read(raf, ncfile);

    ncfile.finish();
  }

  public Array readData(ucar.nc2.Variable v2, Section section) throws IOException, InvalidRangeException {
    // subset
    int[] origin = section.getOrigin();
    int[] shape = section.getShape();
    int[] stride = section.getStride();
    Giniheader.Vinfo vinfo = (Giniheader.Vinfo) v2.getSPobject();
    int[] levels = vinfo.levels;

    if (headerParser.gini_GetCompressType() == 0)
      return readData(v2, vinfo.begin, origin, shape, stride, levels);
    else if (headerParser.gini_GetCompressType() == 2)
      return readCompressedData(v2, vinfo.begin, origin, shape, stride, levels);
    else if (headerParser.gini_GetCompressType() == 1)
      return readCompressedZlib(v2, vinfo.begin, vinfo.nx, vinfo.ny, origin, shape, stride, levels);
    else
      return null;
  }

  private float[] handleLevels(byte[] data,  int[] levels) {
    int level = levels[0];
    float[] a = new float[level];
    float[] b = new float[level];
    float[] fdata = new float[data.length];
    int scale = 1;

    for (int i = 0; i < level; i++) {
      int numer = levels[1 + 5 * i] - levels[2 + 5 * i];
      int denom = levels[3 + 5 * i] - levels[4 + 5 * i];
      a[i] = (numer * 1.f) / (1.f * denom);
      b[i] = levels[1 + 5 * i] - a[i] * levels[3 + 5 * i];
    }

    for (int i = 0; i < data.length; i++) {
      int ival = DataType.unsignedByteToShort(data[i]);
      int k = -1;
      for (int j = 0; j < level; j++) {
        if (levels[3 + (j * 5)] <= ival && ival <= levels[4 + (j * 5)]) {
          k = j;
          scale = levels[5 + j * 5];
        }
      }

      if (k >= 0)
        fdata[i] = (a[k] * ival + b[k]) / scale;
      else
        fdata[i] = 0;

    }
    return fdata;
  }

  private Array makeArray(byte[] data, int[] levels, int[] shape)
  {
    // Default (if no level data) is to just return an array from the bytes.
    Object store = data;
    Class dt = DataType.BYTE.getPrimitiveClassType();

    // If have levels, convert data to float and set-up to use that for array
    if (levels != null) {
      store = handleLevels(data, levels);
      dt = DataType.FLOAT.getPrimitiveClassType();
    }

    // Create array and return
    return Array.factory(dt, shape, store);
  }

  // all the work is here, so can be called recursively
  private Array readData(ucar.nc2.Variable v2, long dataPos, int[] origin, int[] shape, int[] stride,
                         int[] levels) throws IOException, InvalidRangeException {
    // Get to the proper offset and read in the data
    raf.seek(dataPos);
    int data_size = (int) (raf.length() - dataPos);
    byte[] data = new byte[data_size];
    raf.readFully(data);

    // Turn it into an array
    Array array = makeArray(data, levels, v2.getShape());
    return array.sectionNoReduce(origin, shape, stride);
  }

  public Array readDataOld(ucar.nc2.Variable v2, long dataPos, int[] origin, int[] shape, int[] stride) throws IOException, InvalidRangeException {
    int start_l, stride_l, stop_l;
    int start_p, stride_p, stop_p;
    if (origin == null) origin = new int[v2.getRank()];
    if (shape == null) shape = v2.getShape();

    Giniheader.Vinfo vinfo = (Giniheader.Vinfo) v2.getSPobject();

    int nx = vinfo.nx;
    int ny = vinfo.ny;
    start_l = origin[0];
    stride_l = stride[0];
    stop_l = origin[0] + shape[0] - 1;
    // Get data values from GINI
    // Loop over number of lines (slower dimension) for actual data Array
    start_p = origin[1];
    stride_p = stride[1];
    stop_p = origin[1] + shape[1] - 1;

    if (start_l + stop_l + stride_l == 0) { //default lines
      start_l = 0;
      stride_l = 1;
      stop_l = ny - 1;
    }
    if (start_p + stop_p + stride_p == 0) { //default pixels
      start_p = 0;
      stride_p = 1;
    }

    int Len = shape[1]; // length of pixels read each line
    ArrayByte adata = new ArrayByte(new int[]{shape[0], shape[1]});
    Index indx = adata.getIndex();
    long doff = dataPos + start_p;
    // initially no data conversion is needed.
    for (int iline = start_l; iline <= stop_l; iline += stride_l) {
      /* read 1D byte[] */
      byte[] buf = getGiniLine(nx, ny, doff, iline, Len, stride_p);
      /* write into 2D array */
      for (int i = 0; i < Len; i++) {
        adata.setByte(indx.set(iline - start_l, i), buf[i]);
      }
    }
    return adata;
  }

  // for the compressed data read all out into a array and then parse into requested
  public Array readCompressedData(ucar.nc2.Variable v2, long dataPos, int[] origin,
                                  int[] shape, int[] stride, int[] levels) throws IOException, InvalidRangeException {
    // Get to the proper offset and read in the rest of the compressed data
    raf.seek(dataPos);
    int data_size = (int) (raf.length() - dataPos);
    byte[] data = new byte[data_size];
    raf.readFully(data);

    // Send the compressed data to ImageIO (to handle PNG)
    ByteArrayInputStream ios = new ByteArrayInputStream(data);
    BufferedImage image = javax.imageio.ImageIO.read(ios); // LOOK why ImageIO ??
    DataBuffer db = image.getData().getDataBuffer();

    // If the image had byte data, turn into an array
    if (db instanceof DataBufferByte) {
      DataBufferByte dbb = (DataBufferByte) db;
      Array array = makeArray(dbb.getData(), levels, v2.getShape());
      if (levels == null)
        v2.setCachedData(array, false);
      return array.sectionNoReduce(origin, shape, stride);
    }

    return null;
  }

  public Array readCompressedZlib(ucar.nc2.Variable v2, long dataPos, int nx, int ny, int[] origin,
                                  int[] shape, int[] stride, int[] levels) throws IOException, InvalidRangeException {
    // Get to the proper offset and read in the rest of the compressed data
    raf.seek(dataPos);
    int data_size = (int) (raf.length() - dataPos);     //  or 5120 as read buffer size
    byte[] data = new byte[data_size];
    raf.readFully(data);

    // Buffer for decompressing data
    byte[] uncomp = new byte[nx * ny];
    int offset = 0;

    // Set-up zlib decompression (inflation)
    Inflater inflater = new Inflater(false);
    inflater.setInput(data);

    // Loop while the inflater has data and we have space in final buffer
    // This will end up ignoring the last few compressed bytes, which
    // correspond to the end of file marker, which is a single row of pixels
    // of alternating 0/255.
    while (inflater.getRemaining() > 0 && offset < uncomp.length) {
      // Try to decompress what's left, which ends up decompressing one block
      try {
        offset += inflater.inflate(uncomp, offset, uncomp.length - offset);
      } catch (DataFormatException ex) {
        System.out.println("ERROR on inflation " + ex.getMessage());
        ex.printStackTrace();
        throw new IOException(ex.getMessage());
      }

      // If the last block finished...
      if (inflater.finished()) {
        // See if anything's left
        int bytesLeft = inflater.getRemaining();
        if (bytesLeft > 0) {
          // Figure out where we are in the input data
          int inputOffset = data_size - bytesLeft;

          // Check if remaining data are zlib--if not copy out and bail
          byte[] b2 = new byte[2];
          System.arraycopy(data, inputOffset, b2, 0, b2.length);
          if (!isZlibHed(b2)) {
            System.arraycopy(data, inputOffset, uncomp, offset, bytesLeft);
            break;
          }

          // Otherwise, set up for decompressing next block
          inflater.reset();
          inflater.setInput(data, inputOffset, bytesLeft);
        }
      }
    }
    inflater.end();

    // Turn the decompressed data into an array, caching as appropriate
    Array array = makeArray(uncomp, levels, v2.getShape());
    if (levels == null && array.getSize() < Variable.defaultSizeToCache)
      v2.setCachedData(array, false);
    return array.sectionNoReduce(origin, shape, stride);
  }

  /*
  ** Name:       GetGiniLine
  **
  ** Purpose:    Extract a line of data from a GINI image
  **
  ** Parameters:
  **             buf     - buffer containing image data
  **
  ** Returns:
  **             SUCCESS == 1
  **             FAILURE == 0
  **
  **
  */
  private byte[] getGiniLine(int nx, int ny, long doff, int lineNumber, int len, int stride) throws IOException {

    byte[] data = new byte[len];

    /*
    ** checking image file and set location of first line in file
    */
    raf.seek(doff);

    if (lineNumber >= ny)
      throw new IOException("Try to access the file at line number= " + lineNumber + " larger then last line number = " + ny);

    /*
    ** Read in the requested line
    */

    int offset = lineNumber * nx + (int) doff;

    //myRaf.seek ( offset );
    for (int i = 0; i < len; i++) {
      raf.seek(offset);
      data[i] = raf.readByte();
      offset = offset + stride;
      //myRaf.seek(offset);
    }
    //myRaf.read( data, 0, len);

    return data;

  }

  static boolean isZlibHed(byte[] buf) {
    short b0 = DataType.unsignedByteToShort(buf[0]);
    short b1 = DataType.unsignedByteToShort(buf[1]);

    if ((b0 & 0xf) == Z_DEFLATED) {
      if ((b0 >> 4) + 8 <= DEF_WBITS) {
        if ((((b0 << 8) + b1) % 31) == 0) {
          return true;
        }
      }
    }

    return false;
  }

  public String getFileTypeId() {
    return DataFormatType.GINI.getDescription();
  }

  public String getFileTypeDescription() {
    return "GOES Ingest and NOAAPORT Interface";
  }

}
