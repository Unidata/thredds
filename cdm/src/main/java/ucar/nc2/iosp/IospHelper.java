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
package ucar.nc2.iosp;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.PositioningDataInputStream;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.StructureMembers;

import java.nio.*;

/**
 * Helper methods for IOSP's
 *
 * @author caron
 * @since Jan 3, 2008
 */
public class IospHelper {
   static private boolean showLayoutTypes = false;

  /**
   * Read data subset from RandomAccessFile, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param raf      read from here.
   * @param index    handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param fillValue must Byte, Short, Integer, Long, Float, Double, or String, matching dataType, or null for none
   * @param byteOrder if equal to RandomAccessFile.ORDER_XXXX, set the byte order just before reading
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readDataFill(RandomAccessFile raf, Layout index, DataType dataType, Object fillValue, int byteOrder) throws java.io.IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), dataType) :
        makePrimitiveArray((int) index.getTotalNelems(), dataType, fillValue);
    return readData(raf, index, dataType, arr, byteOrder);
  } 

  /**
   * Read data subset from RandomAccessFile, place in given primitive array.
   * Reading is controlled by the Layout object.
   *
   * @param raf      read from here.
   * @param index    handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param arr      primitive array to read data into
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readData(RandomAccessFile raf, Layout index, DataType dataType, Object arr, int byteOrder) throws java.io.IOException {
    if (showLayoutTypes) System.out.println("***RAF LayoutType="+index.getClass().getName());

   if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)) {
      byte[] pa = (byte[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.read(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      //return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;
      if (dataType == DataType.CHAR) return convertByteToChar(pa); else return pa;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readShort(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readInt(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readFloat(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readDouble(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.LONG) {
      long[] pa = (long[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readLong(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.STRUCTURE) {
      byte[] pa = (byte[]) arr;
      int recsize = index.getElemSize();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.read(pa, (int) chunk.getDestElem()*recsize, chunk.getNelems()*recsize);
      }
      return pa;
    }  

    throw new IllegalStateException();
  }

  /**
   * Read data subset from PositioningDataInputStream, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param is      read from here.
   * @param index    handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param fillValue must Byte, Short, Integer, Long, Float, Double, or String, matching dataType, or null for none
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readDataFill(PositioningDataInputStream is, Layout index, DataType dataType, Object fillValue) throws java.io.IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), dataType) :
        makePrimitiveArray((int) index.getTotalNelems(), dataType, fillValue);
    return readData(is, index, dataType, arr);
  }


  /**
   * Read data subset from PositioningDataInputStream, place in given primitive array.
   * Reading is controlled by the Layout object.
   *
   * @param raf      read from here.
   * @param index    handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param arr      primitive array to read data into
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readData(PositioningDataInputStream raf, Layout index, DataType dataType, Object arr) throws java.io.IOException {
    if (showLayoutTypes) System.out.println("***PositioningDataInputStream LayoutType="+index.getClass().getName());

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)) {
      byte[] pa = (byte[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.read(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      //return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;
      if (dataType == DataType.CHAR) return convertByteToChar(pa); else return pa;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readShort(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readInt(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readFloat(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readDouble(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.LONG) {
      long[] pa = (long[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readLong(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.STRUCTURE) {
      int recsize = index.getElemSize();
      byte[] pa = (byte[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.read(chunk.getSrcPos(), pa, (int) chunk.getDestElem()*recsize, chunk.getNelems()*recsize);
      }
    }

    throw new IllegalStateException();
  } //

  /**
   * Read data subset from PositioningDataInputStream, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param layout    handles skipping around in the file, privide ByteBuffer to read from
   * @param dataType dataType of the variable
   * @param fillValue must Byte, Short, Integer, Long, Float, Double, or String, matching dataType, or null for none
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readDataFill(LayoutBB layout, DataType dataType, Object fillValue) throws java.io.IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) layout.getTotalNelems(), dataType) :
        makePrimitiveArray((int) layout.getTotalNelems(), dataType, fillValue);
    return readData(layout, dataType, arr);
  }

  /**
   * Read data subset from ByteBuffer, place in given primitive array.
   * Reading is controlled by the LayoutBB object.
   *
   * @param layout    handles skipping around in the file, privide ByteBuffer to read from
   * @param dataType dataType of the variable
   * @param arr      primitive array to read data into
   * @return the primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readData(LayoutBB layout, DataType dataType, Object arr) throws java.io.IOException {
    if (showLayoutTypes) System.out.println("***BB LayoutType="+layout.getClass().getName());

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)) {
      byte[] pa = (byte[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ByteBuffer bb = chunk.getByteBuffer();
        bb.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = bb.get();
      }
      //return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;
      if (dataType == DataType.CHAR) return convertByteToChar(pa); else return pa;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ShortBuffer buff = chunk.getShortBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        IntBuffer buff = chunk.getIntBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        FloatBuffer buff = chunk.getFloatBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        DoubleBuffer buff = chunk.getDoubleBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.LONG) {
      long [] pa = (long[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        LongBuffer buff = chunk.getLongBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.STRUCTURE) {
      byte[] pa = (byte[]) arr;
      int recsize = layout.getElemSize();
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ByteBuffer bb = chunk.getByteBuffer();
        bb.position(chunk.getSrcElem()*recsize);
        int pos = (int) chunk.getDestElem()*recsize;
        for (int i = 0; i < chunk.getNelems()*recsize; i++)
          pa[pos++] = bb.get();
      }
    }

    throw new IllegalStateException();
  } // */

  static public void copyFromByteBuffer(ByteBuffer bb, StructureMembers.Member m, IndexIterator result) {
    int offset = m.getDataParam();
    int count = m.getSize();
    DataType dtype = m.getDataType();

    if (dtype == DataType.FLOAT) {
      for (int i = 0; i < count; i++)
        result.setFloatNext( bb.getFloat(offset + i*4));

    } else if (dtype == DataType.DOUBLE) {
      for (int i = 0; i < count; i++)
        result.setDoubleNext( bb.getDouble(offset + i*8));

    } else if (dtype == DataType.INT) {
      for (int i = 0; i < count; i++)
        result.setIntNext( bb.getInt(offset + i*4));

    } else if (dtype == DataType.SHORT) {
      for (int i = 0; i < count; i++)
        result.setShortNext( bb.getShort(offset + i*2));

    } else if ((dtype == DataType.BYTE) || (dtype == DataType.OPAQUE)) {
      for (int i = 0; i < count; i++)
        result.setByteNext( bb.get(offset + i));

    } else if (dtype == DataType.CHAR) {
      for (int i = 0; i < count; i++)
        result.setCharNext( (char) bb.get(offset + i));

    } else if (dtype == DataType.LONG) {
      for (int i = 0; i < count; i++)
        result.setLongNext( bb.get(offset + i*8));

    } else
      throw new IllegalStateException();
  }

  /**
   * Create 1D primitive array of the given size and type
   *
   * @param size     the size of the array to create
   * @param dataType dataType of the variable
   * @return primitive array with data read in
   */
  static public Object makePrimitiveArray(int size, DataType dataType) {
    Object arr = null;

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)) {
      arr = new byte[size];

    } else if (dataType == DataType.SHORT) {
      arr = new short[size];

    } else if (dataType == DataType.INT) {
      arr = new int[size];

    } else if (dataType == DataType.FLOAT) {
      arr = new float[size];

    } else if (dataType == DataType.DOUBLE) {
      arr = new double[size];
    }

    return arr;
  }


  /**
   * Create 1D primitive array of the given size and type, fill it with the given value
   *
   * @param size     the size of the array to create
   * @param dataType dataType of the variable
   * @param fillValue must be Byte, Short, Integer, Long, Float, Double, or String, matching dataType
   * @return primitive array with data read in
   */
  static public Object makePrimitiveArray(int size, DataType dataType, Object fillValue) {

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE))  {
      byte[] pa = new byte[size];
      byte val = (Byte) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.OPAQUE) {
      return new byte[size];

    } else if (dataType == DataType.SHORT) {
      short[] pa = new short[size];
      short val = (Short) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = new int[size];
      int val = (Integer) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.LONG) {
      long[] pa = new long[size];
      long val = (Long) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = new float[size];
      float val = (Float) fillValue;
      if (val != 0.0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = new double[size];
      double val = (Double) fillValue;
      if (val != 0.0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if ((dataType == DataType.STRING) || (dataType == DataType.ENUM)) {
      String[] pa = new String[size];
      for (int i = 0; i < size; i++) pa[i] = (String) fillValue;
      return pa;
    }

    throw new IllegalStateException();
  }

  // convert byte array to char array
  static public char[] convertByteToChar(byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i = 0; i < size; i++)
      cbuff[i] = (char) DataType.unsignedByteToShort(byteArray[i]); // NOTE: not Unicode !
    return cbuff;
  }

  // convert char array to byte array
  static public byte[] convertCharToByte(char[] from) {
    int size = from.length;
    byte[] to = new byte[size];
    for (int i = 0; i < size; i++)
      to[i] = (byte) from[i]; // LOOK wrong, convert back to unsigned byte ???
    return to;
  }

}
