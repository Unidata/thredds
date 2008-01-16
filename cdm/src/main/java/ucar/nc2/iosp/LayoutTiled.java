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

/**
 * For datasets where the data are stored in chunks.
 * "Tiled" means that all chunks are assumed to be equal size.
 * Chunks have an offset into the complete array.
 * Chunks do not necessarily cover the array, missing data is possible.
 *
 * @author caron
 * @since Jan 9, 2008
 */
public class LayoutTiled implements Layout {
  private Section want;
  private int[] chunkSize; // all chunks assumed to be the same size
  private int elemSize;
  private long startSrcPos;

  private DataChunkIterator chunkIterator; // iterate across chunks
  private IndexChunkerTiled index = null; // iterate within a chunk

  // track the overall iteration
  private long totalNelems, totalNelemsDone; // total number of elemens

  private boolean debug = false, debugNext= false;

  /**
   * Constructor.
   *
   * @param chunkIterator iterator over all available data chunks
   * @param chunkSize     all chunks assumed to be the same size
   * @param elemSize      size of an element in bytes.
   * @param wantSection   the wanted section of data, contains a List of Range objects. Must be complete
   * @throws ucar.ma2.InvalidRangeException if section invalid for this variable
   * @throws java.io.IOException            on io error
   */
  public LayoutTiled(DataChunkIterator chunkIterator, int[] chunkSize, int elemSize, Section wantSection) throws InvalidRangeException, IOException {
    this.chunkIterator = chunkIterator;
    this.chunkSize = chunkSize;
    this.elemSize = elemSize;
    this.want = wantSection;

    this.totalNelems = this.want.computeSize();
    this.totalNelemsDone = 0;
  }

  public long getTotalNelems() {
    return totalNelems;
  }

  public int getElemSize() {
    return elemSize;
  }

  private Layout.Chunk next = null;

  public boolean hasNext() { // have to actually fetch the thing here
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
          if (dataSection.intersects(want)) // does it intersect ?
            break;
        }

        if (debug)
          System.out.println(" found intersecting section: " + dataSection + " for filePos " + dataChunk.filePos);
        index = new IndexChunkerTiled(dataSection, want);
        startSrcPos = dataChunk.filePos;

      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

    IndexChunker.Chunk chunk = index.next();
    totalNelemsDone += chunk.getNelems();
    chunk.setSrcPos(startSrcPos + chunk.getSrcElem() * elemSize);
    next = chunk;
    return true;
  }

  public Layout.Chunk next() throws IOException {
    if (debugNext) System.out.println("  next="+next);
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


  static public interface DataChunkIterator {
    public boolean hasNext();
    public DataChunk next() throws IOException;
  }

  static public class DataChunk {
    public int[] offset; // offset index of this chunk, reletive to entire array
    public long filePos; // filePos of a single raw data chunk

    public DataChunk(int[] offset, long filePos) {
      this.offset = offset;
      this.filePos = filePos;
    }
  }

}
