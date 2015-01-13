/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.iosp.fysat;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.fysat.FysatHeader.Vinfo;
import ucar.nc2.iosp.fysat.util.EndianByteBuffer;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import java.io.*;
import java.awt.image.*;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

/**
 * FY satellite data stored in AWX format include both original observation and the derived dataset
 *
 * @author yuan
 * @see "http://www.nsmc.cma.gov.cn/en/NSMC/UploadFiles/files/AWX%20File%20Format%20Specification%20Version%202.1.pdf"
 */

public class Fysatiosp extends AbstractIOServiceProvider {

  protected FysatHeader headerParser;

  final static int Z_DEFLATED = 8;
  final static int DEF_WBITS = 15;

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    FysatHeader localHeader = new FysatHeader();
    return (localHeader.isValidFile(raf));
  }

  public String getFileTypeId() {
    return "FYSAT";
  }

  public String getFileTypeDescription() {
    return "Chinese FY-2 satellite image data in AWX format";
  }

  /////////////////////////////////////////////////////////////////////////////
  // reading

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {

    super.open(raf, ncfile, cancelTask);

    headerParser = new FysatHeader();
    headerParser.read(raf, ncfile);

    ncfile.finish();
  }

  public Array readData(ucar.nc2.Variable v2, Section section) throws IOException, InvalidRangeException {
    // subset
    //  Range[] section = Range.toArray( sectionList);
    int[] origin = section.getOrigin();
    int[] shape = section.getShape();
    int[] stride = section.getStride();

    FysatHeader.Vinfo vinfo = (FysatHeader.Vinfo) v2.getSPobject();

    if (headerParser.getCompressType() == 0)
      return readData(v2, vinfo.begin, origin, shape, stride);
    else if (headerParser.getCompressType() == 2)
      return readCompressedData(v2, vinfo.begin, origin, shape, stride);
    else if (headerParser.getCompressType() == 1)
      return readCompressedZlib(v2, vinfo.begin, vinfo.nx, vinfo.ny, origin, shape, stride);
    else
      return null;
  }

  // all the work is here, so can be called recursively
  private Array readData(ucar.nc2.Variable v2, long dataPos, int[] origin, int[] shape, int[] stride) throws IOException, InvalidRangeException {

    // long length = myRaf.length();
    raf.seek(dataPos);
    Vinfo vi = (Vinfo) v2.getSPobject();
    int data_size = vi.vsize;
    byte[] data = new byte[data_size];
    raf.readFully(data);

    Array array;
    if (vi.classType == DataType.BYTE.getPrimitiveClassType()) {

      array = Array.factory(DataType.BYTE.getPrimitiveClassType(), v2.getShape(), data);
    } else if (vi.classType == DataType.SHORT.getPrimitiveClassType()) {
      EndianByteBuffer byteBuff = new EndianByteBuffer(data, vi.byteOrder);
      short[] sdata = byteBuff.getShortArray();
      //for(int i=0; i<sdata.length; i++){
      //	System.out.println(sdata[i]);
      //}
      array = Array.factory(DataType.SHORT.getPrimitiveClassType(), v2.getShape(), sdata);
    } else if (vi.classType == DataType.INT.getPrimitiveClassType()) {
      EndianByteBuffer byteBuff = new EndianByteBuffer(data, vi.byteOrder);
      short[] idata = byteBuff.getShortArray();
      array = Array.factory(DataType.INT.getPrimitiveClassType(), v2.getShape(), idata);

    } else {
      throw new UnsupportedEncodingException();
    }

    return array.sectionNoReduce(origin, shape, stride);

  }

  public Array readDataOld(ucar.nc2.Variable v2, long dataPos, int[] origin, int[] shape, int[] stride) throws IOException, InvalidRangeException {
    int start_l, stride_l, stop_l;
    int start_p, stride_p, stop_p;
    if (origin == null) origin = new int[v2.getRank()];
    if (shape == null) shape = v2.getShape();

    FysatHeader.Vinfo vinfo = (FysatHeader.Vinfo) v2.getSPobject();
    //ucar.ma2.DataType dataType = v2.getDataType();

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


  // for the compressed data read all out into a array and then parse into requested

  public Array readCompressedData(ucar.nc2.Variable v2, long dataPos, int[] origin, int[] shape, int[] stride) throws IOException, InvalidRangeException {

    long length = raf.length();

    raf.seek(dataPos);

    int data_size = (int) (length - dataPos);
    byte[] data = new byte[data_size];
    raf.readFully(data);
    ByteArrayInputStream ios = new ByteArrayInputStream(data);

    BufferedImage image = javax.imageio.ImageIO.read(ios);
    Raster raster = image.getData();
    DataBuffer db = raster.getDataBuffer();

    if (db instanceof DataBufferByte) {
      DataBufferByte dbb = (DataBufferByte) db;
      byte[] udata = dbb.getData();

      Array array = Array.factory(DataType.BYTE.getPrimitiveClassType(), v2.getShape(), udata);
      v2.setCachedData(array, false);
      return array.sectionNoReduce(origin, shape, stride);
    }

    return null;
  }

  public Array readCompressedZlib(ucar.nc2.Variable v2, long dataPos, int nx, int ny, int[] origin, int[] shape, int[] stride) throws IOException, InvalidRangeException {

    long length = raf.length();

    raf.seek(dataPos);

    int data_size = (int) (length - dataPos);     //  or 5120 as read buffer size
    byte[] data = new byte[data_size];
    raf.readFully(data);

    // decompress the bytes
    int resultLength;
    int result = 0;
    byte[] tmp;
    int uncompLen;        /* length of decompress space    */
    byte[] uncomp = new byte[nx * (ny + 1) + 4000];
    Inflater inflater = new Inflater(false);

    inflater.setInput(data, 0, data_size);
    int offset = 0;
    int limit = nx * ny + nx;

    while (inflater.getRemaining() > 0) {
      try {
        resultLength = inflater.inflate(uncomp, offset, 4000);

      } catch (DataFormatException ex) {
        ex.printStackTrace();
        throw new IOException(ex.getMessage());
      }

      offset = offset + resultLength;
      result = result + resultLength;
      if ((result) > limit) {
        // when uncomp data larger then limit, the uncomp need to increase size
        tmp = new byte[result];
        System.arraycopy(uncomp, 0, tmp, 0, result);
        uncompLen = result + 4000;
        uncomp = new byte[uncompLen];
        System.arraycopy(tmp, 0, uncomp, 0, result);
      }

      if (resultLength == 0) {
        int tt = inflater.getRemaining();
        byte[] b2 = new byte[2];
        System.arraycopy(data, data_size - tt, b2, 0, 2);
        if (isZlibHed(b2) == 0) {
          System.arraycopy(data, data_size - tt, uncomp, result, tt);
          break;
        }
        inflater.reset();
        inflater.setInput(data, data_size - tt, tt);
      }

    }

    inflater.end();

    byte[] inflateData = new byte[nx * ny];
    System.arraycopy(uncomp, 0, inflateData, 0, nx * ny);
    Array array = Array.factory(DataType.BYTE.getPrimitiveClassType(), v2.getShape(), inflateData);
    if (array.getSize() < Variable.defaultSizeToCache)
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

  public short convertunsignedByte2Short(byte b) {
    return (short) ((b < 0) ? (short) b + 256 : (short) b);
  }

  int isZlibHed(byte[] buf) {
    short b0 = convertunsignedByte2Short(buf[0]);
    short b1 = convertunsignedByte2Short(buf[1]);

    if ((b0 & 0xf) == Z_DEFLATED) {
      if ((b0 >> 4) + 8 <= DEF_WBITS) {
        if ((((b0 << 8) + b1) % 31) == 0) {
          return 1;
        }
      }
    }

    return 0;
  }

}
