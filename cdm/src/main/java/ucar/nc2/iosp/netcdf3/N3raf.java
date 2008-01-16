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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
   * Read data subset from file for a variable, to WritableByteChannel .
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   */
 protected long readData( Layout index, DataType dataType, WritableByteChannel out) throws java.io.IOException {
   long count = 0;
   if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
     while (index.hasNext()) {
       Layout.Chunk chunk = index.next();
       count += raf.readToByteChannel( out, chunk.getSrcPos(), chunk.getNelems());
     }

   } else if (dataType == DataType.SHORT) {
     while (index.hasNext()) {
       Layout.Chunk chunk = index.next();
       count += raf.readToByteChannel( out, chunk.getSrcPos(), 2 * chunk.getNelems());
     }

   } else if ((dataType == DataType.INT) || (dataType == DataType.FLOAT)) {
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

    } else if (dataType == DataType.STRING) { // LOOK not really legal
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