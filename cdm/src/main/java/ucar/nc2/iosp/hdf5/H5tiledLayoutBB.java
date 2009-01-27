/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.hdf5;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.nc2.iosp.LayoutBB;
import ucar.nc2.iosp.LayoutBBTiled;
import ucar.nc2.Variable;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Iterator to read/write subsets of an array.
 * This calculates byte offsets for HD5 chunked datasets.
 * Assumes that the data is stored in chunks, indexed by a Btree.
 * for unfiltered data only
 *
 * @author caron
 */
class H5tiledLayoutBB implements LayoutBB {
  private LayoutBBTiled delegate;

  private RandomAccessFile raf;
  private H5header.Filter[] filters;
  private ByteOrder byteOrder;
                                                                                                     
  private Section want;
  private int[] chunkSize; // from the StorageLayout message (exclude the elemSize)
  private int elemSize; // last dimension of the StorageLayout message
  private int nChunkDims;

  private boolean debug = false;

  /**
   * Constructor.
   * This is for HDF5 chunked data storage. The data is read by chunk, for efficency.
   *
   * @param v2          Variable to index over; assumes that vinfo is the data object
   * @param wantSection the wanted section of data, contains a List of Range objects. must be complete
   * @param raf the RandomAccessFile
   * @param filters set of filters that have been applied to the data
   * @throws InvalidRangeException if section invalid for this variable
   * @throws java.io.IOException   on io error
   */
  H5tiledLayoutBB(Variable v2, Section wantSection, RandomAccessFile raf, H5header.Filter[] filters, ByteOrder byteOrder) throws InvalidRangeException, IOException {
    wantSection = Section.fill(wantSection, v2.getShape());

    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    assert vinfo.isChunked;
    assert vinfo.btree != null;

    this.raf = raf;
    this.filters = filters;
    this.byteOrder = byteOrder;

    // we have to translate the want section into the same rank as the storageSize, in order to be able to call
    // Section.intersect(). It appears that storageSize (actually msl.chunkSize) may have an extra dimension, reletive
    // to the Variable.
    DataType dtype = v2.getDataType();
    if ((dtype == DataType.CHAR) && (wantSection.getRank() < vinfo.storageSize.length))
      this.want = new Section(wantSection).appendRange(1);
    else
      this.want = wantSection;

    // one less chunk dimension, except in the case of char
    nChunkDims = (dtype == DataType.CHAR) ? vinfo.storageSize.length : vinfo.storageSize.length - 1;
    this.chunkSize = new int[nChunkDims];
    System.arraycopy(vinfo.storageSize, 0, chunkSize, 0, nChunkDims);
    this.elemSize = vinfo.storageSize[vinfo.storageSize.length - 1]; // last one is always the elements size

    // create the data chunk iterator
    H5header.DataBTree.DataChunkIterator iter = vinfo.btree.getDataChunkIterator(this.want);
    DataChunkIterator dcIter = new DataChunkIterator(iter);
    delegate = new LayoutBBTiled(dcIter, chunkSize, elemSize, wantSection);
    
    if (debug) System.out.println(" H5tiledLayout: " + this);
  }

  public long getTotalNelems() {
    return delegate.getTotalNelems();
  }

  public int getElemSize() {
    return delegate.getElemSize();
  }

  public boolean hasNext() {
    return delegate.hasNext();
  }

  public Chunk next() throws IOException {
    return delegate.next();
  }

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("want=").append(want).append("; ");
    sbuff.append("chunkSize=[");
    for (int i = 0; i < chunkSize.length; i++) {
      if (i > 0) sbuff.append(",");
      sbuff.append(chunkSize[i]);
    }
    sbuff.append("] totalNelems=").append(getTotalNelems());
    sbuff.append(" elemSize=").append(elemSize);
    return sbuff.toString();
  }

  private class DataChunkIterator implements LayoutBBTiled.DataChunkIterator {
    H5header.DataBTree.DataChunkIterator delegate;

    DataChunkIterator(H5header.DataBTree.DataChunkIterator delegate) {
      this.delegate = delegate;
    }

    public boolean hasNext() {
      return delegate.hasNext();
    }

    public LayoutBBTiled.DataChunk next() throws IOException {
      return new DataChunk(delegate.next());
    }
  }

  private class DataChunk implements ucar.nc2.iosp.LayoutBBTiled.DataChunk {
    H5header.DataBTree.DataChunk delegate;

    DataChunk(H5header.DataBTree.DataChunk delegate) {
      this.delegate = delegate;
    }

    public int[] getOffset() {
      int[] offset = delegate.offset;
      if (offset.length > nChunkDims) { // may have to eliminate last offset
        offset = new int[nChunkDims];
        System.arraycopy(delegate.offset, 0, offset, 0, nChunkDims);
      }
      return offset;
    }

    public ByteBuffer getByteBuffer() throws IOException {
      // read the data
      byte[] data = new byte[delegate.size];
      raf.seek(delegate.filePos);
      raf.readFully(data);

      // apply filters backwards
      for (int i = filters.length - 1; i >= 0; i--) {
        H5header.Filter f = filters[i];
        if (isBitSet(delegate.filterMask, i)) {
          if (debug) System.out.println("skip for chunk " + delegate);
          continue;
        }
        if (f.id == 1) {
          data = inflate(data);
        } else if (f.id == 2) {
          data = shuffle(data, f.data[0]);
        } else if (f.id == 3) {
          data = checkfletcher32(data);
        } else
          throw new RuntimeException("Unknown filter type="+f.id);
      }

      ByteBuffer result = ByteBuffer.wrap(data);
      result.order(byteOrder);
      return result;
    }

    /**
     * inflate data
     *
     * @param compressed compressed data
     * @return ucompressed data
     * @throws IOException on I/O error
     */
    private byte[] inflate(byte[] compressed) throws IOException {
      // run it through the Inflator
      ByteArrayInputStream in = new ByteArrayInputStream(compressed);
      java.util.zip.InflaterInputStream inflater = new java.util.zip.InflaterInputStream(in);
      ByteArrayOutputStream out = new ByteArrayOutputStream(8 * compressed.length);
      IO.copy(inflater, out);

      byte[] uncomp = out.toByteArray();
      if (debug) System.out.println(" inflate bytes in= " + compressed.length + " bytes out= " + uncomp.length);
      return uncomp;
    }

    // LOOK fake
    private byte[] checkfletcher32(byte[] org) throws IOException {
      byte[] result = new byte[org.length-4];
      System.arraycopy(org, 0, result, 0, result.length);
      if (debug) System.out.println(" checkfletcher32 bytes in= " + org.length + " bytes out= " + result.length);
      return result;
    }

    private byte[] shuffle(byte[] data, int n) throws IOException {
      if (debug) System.out.println(" shuffle bytes in= " + data.length + " n= " + n);

      assert data.length % n == 0;
      if (n <= 1) return data;

      int m = data.length / n;
      int[] count = new int[n];
      for (int k = 0; k < n; k++) count[k] = k * m;

      byte[] result = new byte[data.length];
      /* for (int i = 0; i < data.length; i += n) {
        for (int k = 0; k < n; k++) {
          result[count[k]++] = data[i + k];
        }
      } */

      for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
          result[i*n+j] = data[i + count[j]];
        }
      }

      return result;
    }

    boolean isBitSet(int val, int bitno) {
      return ((val >>> bitno) & 1) != 0;
    }

  }


}