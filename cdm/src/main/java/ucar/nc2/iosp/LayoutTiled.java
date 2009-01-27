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
    StringBuilder sbuff = new StringBuilder();
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
