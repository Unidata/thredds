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
package ucar.nc2.iosp.netcdf3;

import ucar.ma2.*;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.IospHelper;

import java.nio.channels.WritableByteChannel;


/**
 * Use our RandomAccessFile class to read and write.
 * @author caron
 */

public class N3raf extends N3iosp  {

  protected void _open(ucar.unidata.io.RandomAccessFile raf) throws java.io.IOException {
  }

  protected void _create(ucar.unidata.io.RandomAccessFile raf) throws java.io.IOException {
  }

  /**
   * Read data subset from file for a variable, create primitive array.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   * @return primitive array with data read in
   */
 protected Object readData( Layout index, DataType dataType) throws java.io.IOException {
   return IospHelper.readDataFill(raf, index, dataType, null, -1);
 }

  /**
   * Read data subset from file for a variable, to WritableByteChannel.
   * Will send as bigendian, since thats what the underlying file has.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   */
  protected long readData( Layout index, DataType dataType, WritableByteChannel out) throws java.io.IOException {
    long count = 0;
    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.ENUM1)) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        count += raf.readToByteChannel( out, chunk.getSrcPos(), chunk.getNelems());
      }

    } else if ((dataType == DataType.SHORT) || (dataType == DataType.ENUM2)) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        count += raf.readToByteChannel( out, chunk.getSrcPos(), 2 * chunk.getNelems());
      }

    } else if ((dataType == DataType.INT) || (dataType == DataType.FLOAT) || (dataType == DataType.ENUM4)) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        count += raf.readToByteChannel( out, chunk.getSrcPos(), 4 * chunk.getNelems());
      }

    } else if ((dataType == DataType.DOUBLE) || (dataType == DataType.LONG)) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        count += raf.readToByteChannel( out, chunk.getSrcPos(), 8 * chunk.getNelems());
      }
    }

    return count;
  }

   /**
    * write data to a file for a variable.
    * @param values write this data.
    * @param index handles skipping around in the file.
    * @param dataType dataType of the variable
    */
  protected void writeData( Array values, Layout index, DataType dataType) throws java.io.IOException {
    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek ( chunk.getSrcPos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.write( ii.getByteNext());
      }
      return;

    } else if (dataType == DataType.STRING) { // LOOK not legal
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek ( chunk.getSrcPos());
        for (int k=0; k<chunk.getNelems(); k++) {
          String val = (String) ii.getObjectNext();
          if (val != null) raf.write( val.getBytes("UTF-8")); // LOOK ??
        }
      }
      return;

    } else if (dataType == DataType.SHORT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek ( chunk.getSrcPos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeShort( ii.getShortNext());
      }
      return;

    } else if (dataType == DataType.INT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek ( chunk.getSrcPos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeInt( ii.getIntNext());
      }
      return;

    } else if (dataType == DataType.FLOAT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek ( chunk.getSrcPos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeFloat( ii.getFloatNext());
      }
      return;

    } else if (dataType == DataType.DOUBLE) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek ( chunk.getSrcPos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeDouble( ii.getDoubleNext());
      }
      return;
    }

    throw new IllegalStateException("dataType= "+dataType);
  }


}