// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2;

import ucar.ma2.*;


/**
 * Use our RandomAccessFile class to read and write.
 * @author caron
 * @version $Revision$ $Date$
 */

class N3raf extends N3iosp  {

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
  protected Object readData( Indexer index, DataType dataType) throws java.io.IOException {
    int size = index.getTotalNelems();

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      byte[] pa = new byte[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.read( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return (dataType == DataType.BYTE) ? pa : (Object) convertByteToChar( pa);

    } else if (dataType == DataType.SHORT) {
      short[] pa = new short[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.readShort( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = new int[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.readInt( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = new float[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.readFloat( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = new double[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.readDouble( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;
    }

    throw new IllegalStateException();
  }

   /**
    * write data to a file for a variable.
    * @param values write this data.
    * @param index handles skipping around in the file.
    * @param dataType dataType of the variable
    */
  protected void writeData( Array values, Indexer index, DataType dataType) throws java.io.IOException {
    // LOOK - could do an optimization here
    // if (fastOk) writeDataFast( values, index, dataType);

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.write( ii.getByteNext()); // LOOK what about chunk.getIndexPos() ??
      }
      return;

    } else if (dataType == DataType.STRING) { // LOOK not really legal
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        for (int k=0; k<chunk.getNelems(); k++) {
          String val = (String) ii.getObjectNext();
          raf.write( val.getBytes()); // ??
        }
      }
      return;

    } else if (dataType == DataType.SHORT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeShort( ii.getShortNext());
      }
      return;

    } else if (dataType == DataType.INT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeInt( ii.getIntNext());
      }
      return;

    } else if (dataType == DataType.FLOAT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeFloat( ii.getFloatNext());
      }
      return;

    } else if (dataType == DataType.DOUBLE) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeDouble( ii.getDoubleNext());
      }
      return;
    }

    throw new IllegalStateException("dataType= "+dataType);
  }

   /**
    * write data to a file for a variable.
    * @param values write this data.
    * @param index handles skipping around in the file.
    * @param dataType dataType of the variable
    */
  private void writeDataFast( Array values, Indexer index, DataType dataType) throws java.io.IOException {
    //int size = index.getTotalNelems();

    if (dataType == DataType.BYTE) {
      byte[] pa = (byte []) values.getStorage();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.write( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return;

    } else if (dataType == DataType.CHAR) {
      byte[] pa = convertCharToByte( (char []) values.getStorage());
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.write( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return;

    } else if (dataType == DataType.STRING) {
      byte[] pa = (byte []) values.getStorage(); // ??
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.write( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short []) values.getStorage();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.writeShort( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return;

    } else if (dataType == DataType.INT) {
      int[] pa = (int []) values.getStorage();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.writeInt( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float []) values.getStorage();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.writeFloat( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive arr
      }
      return;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double []) values.getStorage();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.writeDouble( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return;
    }

    throw new IllegalStateException("dataType= "+dataType);
  }

   /*
    * Read data subset from file for a variable, into a primitive array.
    * @param beginOffset variable's begining byte offset in file.
    * @param index handles skipping around in the file.
    * @param dataType dataType of the variable
    * @param dataArray primitive array
    * @param offset start reading into primitive array here
    * @return number of elements read
    *
  protected int readData( int beginOffset, Indexer index, DataType dataType, Object dataArray, int offset) throws java.io.IOException {
    int size = index.getTotalNelems();

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      byte[] pa = (byte[]) dataArray;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.read( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }

      byte[] pa = (byte[]) dataArray;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.read( pa, offset, chunk); // copy into primitive array
        offset += chunk;
      }

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) dataArray;
      while (index.hasNext()) {
        raf.seek ((long) beginOffset + index.next());
        raf.readShort( pa, offset, chunk); // copy into primitive array
        offset += chunk;
      }

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) dataArray;
      while (index.hasNext()) {
        raf.seek ((long) beginOffset + index.next());
        raf.readInt( pa, offset, chunk); // copy into primitive array
        offset += chunk;
      }

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) dataArray;
      while (index.hasNext()) {
       raf.seek ((long) beginOffset + index.next());
       raf.readFloat( pa, offset, chunk); // copy into primitive array
       offset += chunk;
      }

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) dataArray;
      while (index.hasNext()) {
        raf.seek ((long) beginOffset + index.next());
        raf.readDouble( pa, offset, chunk); // copy into primitive array
        offset += chunk;
      }
    } else {
      throw new IllegalStateException();
    }

    return size;
  } */

   /*
    * Write data subset to file for a variable, create primitive array.
    * @param beginOffset: variable's beginning byte offset in file.
    * @param index handles skipping around in the file.
    * @param source from this buye buffer
    * @param dataType dataType of the variable
    * @return primitive array with data read in
    *
  protected void writeData( Array aa, long beginOffset, Indexer index, DataType dataType) throws java.io.IOException {
    /* int offset = 0;
    int chunk = index.getChunkSize();
    int size = index.getTotalSize();

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      byte[] pa;
      if (dataType == DataType.BYTE)
         pa = (byte[]) aa.getStorage();
      else {
        char[] cbuff = (char[]) aa.getStorage();
        pa = convertCharToByte( cbuff);
      }
      ByteBuffer bbuff = ByteBuffer.allocateDirect( chunk);
      while (index.hasNext()) {
        bbuff.clear();
        bbuff.put(pa, offset, chunk); // copy from primitive array to buffer
        bbuff.flip();
        channel.write(bbuff, beginOffset + index.next());
        offset += chunk;
      }
      return;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) aa.getStorage();
      ByteBuffer bbuff = ByteBuffer.allocateDirect( chunk*2);
      ShortBuffer tbuff = bbuff.asShortBuffer(); // typed buffer
      while (index.hasNext()) {
        tbuff.clear();
        tbuff.put(pa, offset, chunk); // copy from primitive array to typed buffer
        bbuff.clear();
        channel.write(bbuff, beginOffset + index.next());
        offset += chunk;
      }
      return;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) aa.getStorage();
      ByteBuffer bbuff = ByteBuffer.allocateDirect( chunk*4);
      IntBuffer tbuff = bbuff.asIntBuffer(); // typed buffer
      while (index.hasNext()) {
        tbuff.clear();
        tbuff.put(pa, offset, chunk); // copy from primitive array to typed buffer
        bbuff.clear();
        channel.write(bbuff, beginOffset + index.next());
        offset += chunk;
      }
      return;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) aa.getStorage();
      ByteBuffer bbuff = ByteBuffer.allocateDirect( chunk*4);
      FloatBuffer tbuff = bbuff.asFloatBuffer(); // typed buffer
      while (index.hasNext()) {
        tbuff.clear();
        tbuff.put(pa, offset, chunk); // copy from primitive array to typed buffer
        bbuff.clear();
        channel.write(bbuff, beginOffset + index.next());
        offset += chunk;
      }
      return;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) aa.getStorage();
      ByteBuffer bbuff = ByteBuffer.allocateDirect( chunk*8);
      DoubleBuffer tbuff = bbuff.asDoubleBuffer(); // typed buffer
      while (index.hasNext()) {
        tbuff.clear();
        tbuff.put(pa, offset, chunk); // copy from primitive array to typed buffer
        bbuff.clear();
        channel.write(bbuff, beginOffset + index.next());
        offset += chunk;
      }
      return;
    }

    throw new IllegalStateException();
  } */

}