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

import java.io.IOException;

/**
 * @author caron
 * @since Jan 3, 2008
 */
public class IospHelper {

  /**
   * Read data subset from file for a variable, create primitive array, the size of the Indexer.
   * @param raf read from here.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readData(RandomAccessFile raf, Layout index, DataType dataType) throws java.io.IOException {
    Object arr = makePrimitiveArray((int) index.getTotalNelems(), dataType);
    return readData(raf, index, dataType, arr);
  }

  /**
   *  Create 1D primitive array of the given size and type
   *
   * @param size the size of the array to create
   * @param dataType dataType of the variable
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object makePrimitiveArray(int size, DataType dataType) throws java.io.IOException {
    Object arr = null;

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
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

  static public Object readData(PositioningDataInputStream raf, Layout index, DataType dataType) throws java.io.IOException {
    Object arr = makePrimitiveArray((int) index.getTotalNelems(), dataType);
    return readData(raf, index, dataType, arr);
  }


  /**
   * Read data subset from file for a variable, create primitive array, the size of the Indexer.
   * @param raf read from here.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param arr primitive array to read data into
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readData(PositioningDataInputStream raf, Layout index, DataType dataType, Object arr) throws java.io.IOException {

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      byte[] pa = (byte []) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.read(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short []) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readShort(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readInt(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa =(float []) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readFloat(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double []) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readDouble(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;
    }

    throw new IllegalStateException();
  }

  /**
   * Read data subset from file for a variable, create primitive array, the size of the Indexer.
   * @param raf read from here.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param arr primitive array to read data into
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  static public Object readData(RandomAccessFile raf, Layout index, DataType dataType, Object arr) throws java.io.IOException {

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      byte[] pa = (byte []) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        raf.read(pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short []) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        raf.readShort(pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        raf.readInt(pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa =(float []) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        raf.readFloat(pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double []) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        raf.readDouble(pa, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;
    }

    throw new IllegalStateException();
  }

  // convert byte array to char array
  static public char[] convertByteToChar(byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i = 0; i < size; i++)
      cbuff[i] = (char) DataType.unsignedByteToShort(byteArray[i]); // NOTE: not Unicode ! // LOOK (char) byteArray[i]
    return cbuff;
  }

  // convert char array to byte array
  static public byte[] convertCharToByte(char[] from) {
    int size = from.length;
    byte[] to = new byte[size];
    for (int i = 0; i < size; i++)
      to[i] = (byte) from[i]; // LOOK wrong, convert back to unsigned byte
    return to;
  }

}
