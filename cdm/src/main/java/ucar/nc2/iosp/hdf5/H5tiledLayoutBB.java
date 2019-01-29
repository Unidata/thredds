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
package ucar.nc2.iosp.hdf5;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;
import ucar.nc2.iosp.LayoutBB;
import ucar.nc2.iosp.LayoutBBTiled;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Iterator to read/write subsets of an array.
 * This calculates byte offsets for HD5 chunked datasets.
 * Assumes that the data is stored in chunks, indexed by a Btree.
 * for filtered data
 *
 * @author caron
 */
class H5tiledLayoutBB implements LayoutBB {

  static final int DEFAULTZIPBUFFERSIZE = 512;
  // System property name for -D flag
  static final String INFLATEBUFFERSIZE = "unidata.h5iosp.inflate.buffersize";

  static public boolean debugFilter = false;

  private LayoutBBTiled delegate;

  private RandomAccessFile raf;
  private H5header.Filter[] filters;
  private ByteOrder byteOrder;
                                                                                                     
  private Section want;
  private int[] chunkSize; // from the StorageLayout message (exclude the elemSize)
  private int elemSize; // last dimension of the StorageLayout message
  private int nChunkDims;

  private boolean debug = false;

  private int inflatebuffersize = DEFAULTZIPBUFFERSIZE;

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
  H5tiledLayoutBB(Variable v2, Section wantSection, RandomAccessFile raf, H5header.Filter[] filters, ByteOrder byteOrder) throws InvalidRangeException, IOException
  {
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
    if((dtype == DataType.CHAR) && (wantSection.getRank() < vinfo.storageSize.length))
      this.want = new Section(wantSection).appendRange(1);
    else
      this.want = wantSection;

    // one less chunk dimension, except in the case of char
    nChunkDims = (dtype == DataType.CHAR) ? vinfo.storageSize.length : vinfo.storageSize.length - 1;
    this.chunkSize = new int[nChunkDims];
    System.arraycopy(vinfo.storageSize, 0, chunkSize, 0, nChunkDims);
    this.elemSize = vinfo.storageSize[vinfo.storageSize.length - 1]; // last one is always the elements size

    // create the data chunk iterator
    DataBTree.DataChunkIterator iter = vinfo.btree.getDataChunkIteratorFilter(this.want);
    DataChunkIterator dcIter = new DataChunkIterator(iter);
    delegate = new LayoutBBTiled(dcIter, chunkSize, elemSize, this.want);

    if(System.getProperty(INFLATEBUFFERSIZE) != null)  {
      try {
        int size = Integer.parseInt(System.getProperty(INFLATEBUFFERSIZE));
        if(size <= 0)
          H5iosp.log.warn(String.format("-D%s must be > 0",INFLATEBUFFERSIZE));
        else
          this.inflatebuffersize = size;
      } catch (NumberFormatException nfe) {
        H5iosp.log.warn(String.format("-D%s is not an integer",INFLATEBUFFERSIZE));
      }
    }
    if(debugFilter)
      System.out.printf("inflate buffer size -D%s = %d%n",INFLATEBUFFERSIZE,this.inflatebuffersize);

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
    DataBTree.DataChunkIterator delegate;

    DataChunkIterator(DataBTree.DataChunkIterator delegate) {
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
    // Copied from ArrayList.
    private static final int MAX_ARRAY_LEN = Integer.MAX_VALUE - 8;

    DataBTree.DataChunk delegate;

    DataChunk(DataBTree.DataChunk delegate) {
      this.delegate = delegate;

      // Check that the chunk length (delegate.size) isn't greater than the maximum array length that we can
      // allocate (MAX_ARRAY_LEN). This condition manifests in two ways.
      // 1) According to the HDF docs (https://www.hdfgroup.org/HDF5/doc/Advanced/Chunking/, "Chunk Maximum Limits"),
      //    max chunk length is 4GB (i.e. representable in an unsigned int). Java, however, only has signed ints.
      //    So, if we try to store a large unsigned int in a singed int, it'll overflow, and the signed int will come
      //    out negative. We're trusting here that the chunk size read from the HDF file is never negative.
      // 2) In most JVM implementations MAX_ARRAY_LEN is actually less than Integer.MAX_VALUE (see note in ArrayList).
      //    So, we could have: "MAX_ARRAY_LEN < chunkSize <= Integer.MAX_VALUE".
      if (delegate.size < 0 || delegate.size > MAX_ARRAY_LEN) {
        // We want to report the size of the chunk, but we may be in an arithmetic overflow situation. So to get the
        // correct value, we're going to reinterpet the integer's bytes as long bytes.
        byte[] intBytes = Ints.toByteArray(delegate.size);
        byte[] longBytes = new byte[8];
        System.arraycopy(intBytes, 0, longBytes, 4, 4);   // Copy int bytes to the lowest 4 positions.
        long chunkSize = Longs.fromByteArray(longBytes);  // Method requires an array of length 8.

        throw new IllegalArgumentException(String.format("Filtered data chunk is %s bytes and we must load it all " +
                "into memory. However the maximum length of a byte array in Java is %s.", chunkSize, MAX_ARRAY_LEN));
      }
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
      try {
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
      } catch (OutOfMemoryError e) {
        Error oom =  new OutOfMemoryError("Ran out of memory trying to read HDF5 filtered chunk. Either increase the " +
                "JVM's heap size (use the -Xmx switch) or reduce the size of the dataset's chunks (use nccopy -c).");
        oom.initCause(e);  // OutOfMemoryError lacks a constructor with a cause parameter.
        throw oom;
      }
    }

    /**
     * inflate data
     *
     * @param compressed compressed data
     * @return uncompressed data
     * @throws IOException on I/O error
     */
    private byte[] inflate(byte[] compressed) throws IOException {
      // run it through the Inflator
      ByteArrayInputStream in = new ByteArrayInputStream(compressed);
      java.util.zip.Inflater inflater = new java.util.zip.Inflater();
      java.util.zip.InflaterInputStream inflatestream
        = new java.util.zip.InflaterInputStream(in, inflater, inflatebuffersize);
      int len = Math.min(8 * compressed.length, MAX_ARRAY_LEN);
      ByteArrayOutputStream out = new ByteArrayOutputStream(len); // Fixes KXL-349288
      IO.copyB(inflatestream, out, len);

      byte[] uncomp = out.toByteArray();
      if (debug || debugFilter)
        System.out.println(" inflate bytes in= " + compressed.length + " bytes out= " + uncomp.length);
      return uncomp;
    }

     // just strip off the 4-byte fletcher32 checksum at the end
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
