/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

  private static final boolean debug = false, debugNext= false;

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
    if(this.want.isVariableLength()) {
        // remove the varlen
        List<Range> newrange = new ArrayList<>(this.want.getRanges());
        newrange.remove(newrange.size()-1);
        this.want = new Section(newrange);
    }

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
          if (debug) System.out.printf(" dataChunk: %s%n", dataSection);
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
    public int[] offset; // offset index of this chunk, relative to entire array
    public long filePos; // filePos of a single raw data chunk

    public DataChunk(int[] offset, long filePos) {
      this.offset = offset;
      this.filePos = filePos;
    }
  }

}
