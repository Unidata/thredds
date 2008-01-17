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
package ucar.nc2.iosp.hdf5;

import ucar.nc2.Variable;
import ucar.nc2.util.IO;
import ucar.nc2.iosp.Indexer;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.*;

/**
 * @author caron
 * @since Jul 21, 2007
 * @deprecated use H5tiledLayoutBB
 */
class H5chunkFilterLayout extends H5chunkLayout {
  private RandomAccessFile raf;
  private H5header.Filter[] filters;
  private ByteBuffer bb;
  private ShortBuffer sb;
  private IntBuffer ib;
  private LongBuffer longb;
  private FloatBuffer fb;
  private DoubleBuffer db;

  private boolean debug = false;

  H5chunkFilterLayout(Variable v2, Section wantSection, RandomAccessFile raf, H5header.Filter[] filters) throws InvalidRangeException, IOException {
    super(v2, v2.getDataType(), wantSection);
    this.raf = raf;
    this.filters = filters;
  }

  public ByteBuffer getByteBuffer() { return bb; }
  public ShortBuffer getShortBuffer() {
    if (sb == null) sb = bb.asShortBuffer();
    return sb;
  }
  public IntBuffer getIntBuffer() {
    if (ib == null) ib = bb.asIntBuffer();
    return ib;
  }
  public LongBuffer getLongBuffer() {
    if (longb == null) longb = bb.asLongBuffer();
    return longb;
  }
  public FloatBuffer getFloatBuffer() {
    if (fb == null) fb = bb.asFloatBuffer();
    return fb;
  }
  public DoubleBuffer getDoubleBuffer() {
    if (db == null) db = bb.asDoubleBuffer();
    return db;
  }

  protected Indexer indexFactory(H5header.DataBTree.DataChunk dataChunk, long filePos, int elemSize, Section dataSection, Section want) throws IOException, InvalidRangeException {
    // read the data
    byte[] data = new byte[dataChunk.size];
    raf.seek(dataChunk.filePos);
    raf.readFully(data);

    // apply filters backwards
    for (int i=filters.length-1; i >=0; i--) {
      H5header.Filter f = filters[i];
      if (isBitSet(dataChunk.filterMask, i)) {
        if (debug) System.out.println("skip for chunk "+dataChunk);
        continue;
      }
      if (f.id == 1)
        data = inflate(data);
      if (f.id == 2)
        data = shuffle(data, f.data[0]);
    }

    bb = ByteBuffer.wrap(data);
    sb = null;
    ib = null;
    longb = null;
    fb = null;
    db = null;

    return super.indexFactory(null, 0, elemSize, dataSection, want);
  }

  /**
   * inflate data
   * @param compressed compressed data
   * @return ucompressed data
   * @throws IOException on I/O error
   */
  private byte[] inflate(byte[] compressed) throws IOException {
     // run it through the Inflator
     ByteArrayInputStream in = new ByteArrayInputStream(compressed);
     java.util.zip.InflaterInputStream inflater = new java.util.zip.InflaterInputStream( in);
     ByteArrayOutputStream out = new ByteArrayOutputStream(8 * compressed.length);
     IO.copy(inflater, out);

     byte[] uncomp = out.toByteArray();
     if (debug) System.out.println(" inflate bytes in= "+compressed.length+ " bytes out= "+uncomp.length);
     return uncomp;
   }

  private byte[] shuffle(byte[] data, int n) throws IOException {
    if (debug) System.out.println(" shuffle bytes in= "+data.length+ " n= "+n);

    assert data.length % n == 0;
    if (n <= 1) return data;

    int seglen = data.length/n;
    int[] count = new int[n];
    for (int k=0; k<n; k++) count[k] = k * seglen;

    byte[] result = new byte[data.length];
    for (int i=0; i<data.length; i += n) {
      for (int k=0; k<n; k++) {
        result[count[k]++] = data[i+k];
      }
    }

    return result;
  }

  static boolean isBitSet(int val, int bitno) {
    return ((val >>> bitno) & 1) != 0;
  }

  // test
  public static void main(String args[]) {
    assert isBitSet(1,0);
    assert !isBitSet(1,1);
    assert !isBitSet(2,0);
    assert isBitSet(2,1);
    assert isBitSet(1024,10);
    assert !isBitSet(1024,1);
  }


}
