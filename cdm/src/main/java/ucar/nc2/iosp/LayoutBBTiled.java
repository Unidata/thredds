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

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;

import java.io.IOException;
import java.nio.*;

/**
 * For datasets where the data are stored in chunks, and must be processed, eg compressed or filtered.
 * The data is read, processed, and placed in a ByteBuffer. Chunks have an offset into the ByteBuffer.
 * "Tiled" means that all chunks are assumed to be equal size.
 * Chunks do not necessarily cover the array, missing data is possible.
 *
 * @author caron
 * @since Jan 9, 2008
 */
public class LayoutBBTiled implements LayoutBB {
  private Section want;
  private int[] chunkSize; // all chunks assumed to be the same size
  private int elemSize;
  //private long startSrcPos;

  private DataChunkIterator chunkIterator; // iterate across chunks
  private IndexChunkerTiled index = null; // iterate within a chunk
  private ByteBuffer bb = null;

  // track the overall iteration
  private long totalNelems, totalNelemsDone; // total number of elemens

  private boolean debug = false, debugIntersection = false;

  /**
   * Constructor.
   *
   * @param chunkIterator iterator over all data chunks
   * @param chunkSize     all chunks assumed to be the same size
   * @param elemSize      size of an element in bytes.
   * @param srcShape      shape of the entire data array.
   * @param wantSection   the wanted section of data, contains a List of Range objects.
   * @throws ucar.ma2.InvalidRangeException if section invalid for this variable
   * @throws java.io.IOException            on io error
   */
  public LayoutBBTiled(DataChunkIterator chunkIterator, int[] chunkSize, int elemSize, int[] srcShape, Section wantSection) throws InvalidRangeException, IOException {
    this.chunkIterator = chunkIterator;
    this.chunkSize = chunkSize;
    this.elemSize = elemSize;
    this.want = Section.fill(wantSection, srcShape);

    this.totalNelems = this.want.computeSize();
    this.totalNelemsDone = 0;
  }

  public long getTotalNelems() {
    return totalNelems;
  }

  public int getElemSize() {
    return elemSize;
  }

  private LayoutBB.Chunk next;
  public boolean hasNext() { // have to actually fetch the thing
    if (totalNelemsDone >= totalNelems) return false;

    if ((index == null) || !index.hasNext()) { // get new data node
      try {
        Section dataSection;
        DataChunk dataChunk;

        while (true) { // look for intersecting sections
          if (!chunkIterator.hasNext()) {
            next = null;
            return false;
          }

          // get next dataChunk
          try {
            dataChunk = chunkIterator.next();
          } catch (IOException e) {
            e.printStackTrace();
            next = null;
            return false;
          }

          // make the dataSection for this chunk
          dataSection = new Section(dataChunk.offset, chunkSize);
          if (debugIntersection)
            System.out.println(" test intersecting: " + dataSection + " want: " + want);
          if (dataSection.intersects(want)) // does it intersect ?
            break;
        }

        if (debug)
          System.out.println(" found intersecting section: " + dataSection);
        index = new IndexChunkerTiled(dataSection, want);
        //startSrcPos = dataChunk.filePos;
        bb = dataChunk.bb;

      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

    IndexChunker.Chunk chunk = index.next();
    totalNelemsDone += chunk.getNelems();
    //chunk.setSrcPos(startSrcPos + chunk.getSrcElem() * elemSize);
    next = new Chunk( chunk, bb);
    return true;
  }

  public LayoutBB.Chunk next() throws IOException {
    return next;
  }
  
  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("want=").append(want).append("; ");
    sbuff.append("chunkSize=[");
    for (int i = 0; i < chunkSize.length; i++) {
      if (i > 0) sbuff.append(",");
      sbuff.append(chunkSize[i]);
    }
    sbuff.append("] totalNelems=").append(totalNelems);
    sbuff.append(" elemSize=").append(elemSize);
    return sbuff.toString();
  }

  /**
   * An iterator over the data chunks.
   */
  static public interface DataChunkIterator {
    public boolean hasNext();
    public DataChunk next() throws IOException;
  }

  /**
   * The Data Chunks.
   */
  static public class DataChunk {
    int[] offset; // offset index of this chunk, reletive to entire array
    //long filePos; // filePos of a single raw data chunk
    ByteBuffer bb;  // the data is placed into here

    public DataChunk(int[] offset, /*long filePos, */ ByteBuffer bb) {
      this.offset = offset;
      //this.filePos = filePos;
      this.bb = bb;
    }
  }

  /**
   * A chunk of data that is contiguous in both the source and destination.
   * Everything is done in elements, not bytes.
   * Read nelems from src at srcPos, store in destination at destPos.
   */
  static private class Chunk implements LayoutBB.Chunk {
    IndexChunker.Chunk delegate;

    private ByteBuffer bb;
    private ShortBuffer sb;
    private IntBuffer ib;
    private LongBuffer longb;
    private FloatBuffer fb;
    private DoubleBuffer db;

    private boolean debug = false;

    public Chunk(IndexChunker.Chunk delegate, ByteBuffer bb) {
      this.delegate = delegate;
      this.bb = bb;
    }

    public int getSrcElem() {
      return (int) delegate.getSrcElem(); // LOOK this is the problem ????
    }
    public int getNelems() {
      return delegate.getNelems();
    }
    public long getDestElem() {
      return delegate.getDestElem();
    }

    public ByteBuffer getByteBuffer() {
      return bb;
    }

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
  }

}
