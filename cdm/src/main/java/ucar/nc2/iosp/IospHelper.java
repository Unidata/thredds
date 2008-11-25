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
import ucar.ma2.*;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Variable;
import ucar.nc2.Structure;

import java.nio.*;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

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
  static public Object readDataFill(RandomAccessFile raf, Layout index, DataType dataType, Object fillValue,
          int byteOrder) throws java.io.IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), dataType) :
        makePrimitiveArray((int) index.getTotalNelems(), dataType, fillValue);
    return readData(raf, index, dataType, arr, byteOrder, true);
  }

  static public Object readDataFill(RandomAccessFile raf, Layout index, DataType dataType, Object fillValue,
          int byteOrder, boolean convertChar) throws java.io.IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), dataType) :
        makePrimitiveArray((int) index.getTotalNelems(), dataType, fillValue);
    return readData(raf, index, dataType, arr, byteOrder, convertChar);
  }

  /**
   * Read data subset from RandomAccessFile, place in given primitive array.
   * Reading is controlled by the Layout object.
   *
   * @param raf      read from here.
   * @param layout    handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param arr      primitive array to read data into
   * @param byteOrder if equal to RandomAccessFile.ORDER_XXXX, set the byte order just before reading
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readData(RandomAccessFile raf, Layout layout, DataType dataType, Object arr, int byteOrder, boolean convertChar) throws java.io.IOException {
    if (showLayoutTypes) System.out.println("***RAF LayoutType="+layout.getClass().getName());

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.ENUM1) ) {
       byte[] pa = (byte[]) arr;
       while (layout.hasNext()) {
         Layout.Chunk chunk = layout.next();
         raf.order(byteOrder);
         raf.seek(chunk.getSrcPos());
         raf.read(pa, (int) chunk.getDestElem(), chunk.getNelems());
       }
      //return (convertChar && dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;
      if (convertChar && dataType == DataType.CHAR) return convertByteToChar(pa); else return pa; // javac ternary compile error

    } else if ((dataType == DataType.SHORT) || (dataType == DataType.ENUM2)) {
      short[] pa = (short[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readShort(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if ((dataType == DataType.INT) || (dataType == DataType.ENUM4)) {
      int[] pa = (int[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readInt(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readFloat(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readDouble(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.LONG) {
      long[] pa = (long[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readLong(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

   } else if (dataType == DataType.STRUCTURE) {
     byte[] pa = (byte[]) arr;
     int recsize = layout.getElemSize();
     while (layout.hasNext()) {
       Layout.Chunk chunk = layout.next();
       raf.order(byteOrder);
       raf.seek(chunk.getSrcPos());
       raf.read(pa, (int) chunk.getDestElem()*recsize, chunk.getNelems()*recsize);
     }
     return pa;
   }

    throw new IllegalStateException("unknown type= "+dataType);
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

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE) || (dataType == DataType.ENUM1)) {
      byte[] pa = (byte[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.read(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      //return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;
      if (dataType == DataType.CHAR) return convertByteToChar(pa); else return pa;

    } else if ((dataType == DataType.SHORT) || (dataType == DataType.ENUM2)) {
      short[] pa = (short[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readShort(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if ((dataType == DataType.INT) || (dataType == DataType.ENUM4)) {
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
      return pa;
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

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.ENUM1)) {
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

    } else if ((dataType == DataType.SHORT) || (dataType == DataType.ENUM2)) {
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

    } else if ((dataType == DataType.INT) || (dataType == DataType.ENUM4)) {
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

    } else if ((dtype == DataType.INT) || (dtype == DataType.ENUM4)) {
      for (int i = 0; i < count; i++)
        result.setIntNext( bb.getInt(offset + i*4));

    } else if ((dtype == DataType.SHORT) || (dtype == DataType.ENUM2)) {
      for (int i = 0; i < count; i++)
        result.setShortNext( bb.getShort(offset + i*2));

    } else if ((dtype == DataType.BYTE) || (dtype == DataType.ENUM1)) {
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

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.ENUM1)  || (dataType == DataType.OPAQUE)) {
      arr = new byte[size];

    } else if ((dataType == DataType.SHORT) || (dataType == DataType.ENUM2)) {
      arr = new short[size];

    } else if ((dataType == DataType.INT) || (dataType == DataType.ENUM4)) {
      arr = new int[size];

    } else if (dataType == DataType.LONG) {
      arr = new long[size];

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

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.ENUM1))  {
      byte[] pa = new byte[size];
      byte val = (Byte) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.OPAQUE) {
      return new byte[size];

    } else if ((dataType == DataType.SHORT) || (dataType == DataType.ENUM2)) {
      short[] pa = new short[size];
      short val = (Short) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if ((dataType == DataType.INT) || (dataType == DataType.ENUM4)) {
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

    } else if (dataType == DataType.STRING) {
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

  static public long transferData(Array result, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    // LOOK should we buffer ??
    DataOutputStream outStream = new DataOutputStream( Channels.newOutputStream( channel));

    IndexIterator iterA = result.getIndexIterator();
    Class classType = result.getElementType();

    if (classType == double.class) {
      while (iterA.hasNext())
        outStream.writeDouble(iterA.getDoubleNext());

    } else if (classType == float.class) {
      while (iterA.hasNext())
        outStream.writeFloat(iterA.getFloatNext());

    } else if (classType == long.class) {
      while (iterA.hasNext())
        outStream.writeLong(iterA.getLongNext());

    } else if (classType == int.class) {
      while (iterA.hasNext())
        outStream.writeInt(iterA.getIntNext());

    } else if (classType == short.class) {
      while (iterA.hasNext())
        outStream.writeShort(iterA.getShortNext());

    } else if (classType == char.class) {
      while (iterA.hasNext())
        outStream.writeChar(iterA.getCharNext());

    } else if (classType == byte.class) {
      while (iterA.hasNext())
        outStream.writeByte(iterA.getByteNext());

    } else if (classType == boolean.class) {
      while (iterA.hasNext())
        outStream.writeBoolean(iterA.getBooleanNext());

    } else
      throw new UnsupportedOperationException("Class type = " + classType.getName());

    return 0;
  }

  // section reading for member data
  static public ucar.ma2.Array readSection(ParsedSectionSpec cer) throws IOException, InvalidRangeException {
    Variable inner = null;
    List<Range> totalRanges = new ArrayList<Range>();
    ParsedSectionSpec current = cer;
    while (current != null) {
      totalRanges.addAll( current.section.getRanges());
      inner = current.v;
      current = current.child;
    }

    Section total = new Section( totalRanges);
    Array result = Array.factory(inner.getDataType(), total.getShape());

    // must be a Structure
    Structure outer = (Structure) cer.v;
    Structure outerSubset = outer.select( cer.child.v); // allows IOSPs to optimize for  this case
    ArrayStructure outerData = (ArrayStructure) outerSubset.read(cer.section);
    extractSection( cer.child, outerData, result.getIndexIterator());

    result.setUnsigned(cer.v.isUnsigned());
    return result;
  }

  static private void extractSection(ParsedSectionSpec child, ArrayStructure outerData, IndexIterator to) throws IOException, InvalidRangeException {
    long wantNelems = child.section.computeSize();

    StructureMembers.Member m = outerData.findMember( child.v.getShortName());
    for (int recno = 0; recno < outerData.getSize(); recno++) {
      Array innerData = outerData.getArray(recno, m);

      if (child.child == null) {  // inner variable
        if (wantNelems != innerData.getSize())
          innerData = innerData.section(child.section.getRanges());
        MAMath.copy(child.v.getDataType(), innerData.getIndexIterator(), to);

      } else {                   // not an inner variable - must be an ArrayStructure

        if (innerData instanceof ArraySequence)
          extractSectionFromSequence(child.child, (ArraySequence) innerData, to);
        else {
          if (wantNelems != innerData.getSize())
            innerData = sectionArrayStructure(child, (ArrayStructure) innerData, m);
          extractSection(child.child, (ArrayStructure) innerData, to);
        }
      }
    }
  }

  static private void extractSectionFromSequence(ParsedSectionSpec child, ArraySequence outerData, IndexIterator to) throws IOException, InvalidRangeException {
    StructureDataIterator sdataIter = outerData.getStructureDataIterator();
    while (sdataIter.hasNext()) {
      StructureData sdata = sdataIter.next();
      StructureMembers.Member m = outerData.findMember( child.v.getShortName());
      Array innerData = sdata.getArray( child.v.getShortName());
      MAMath.copy(m.getDataType(), innerData.getIndexIterator(), to);
    }
  }

  // LOOK could be used in createView ??
  static private ArrayStructure sectionArrayStructure(ParsedSectionSpec child, ArrayStructure innerData, StructureMembers.Member m) throws IOException, InvalidRangeException {
    StructureMembers membersw = new StructureMembers(m.getStructureMembers()); // no data arrays get propagated
    ArrayStructureW result = new ArrayStructureW(membersw, child.section.getShape());

    int count =0;
    Section.Iterator iter = child.section.getIterator( child.v.getShape());
    while (iter.hasNext()) {
      int recno = iter.next();
      StructureData sd = innerData.getStructureData(recno);
      result.setStructureData(sd, count++);
    }

    return result;
  }

}
