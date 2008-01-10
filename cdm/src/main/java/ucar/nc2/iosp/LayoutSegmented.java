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

import ucar.ma2.Index;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

/**
 * LayoutSegmented has data stored in segments, eg HDF4 linked lists.
 *
 * @author caron
 * @since Dec 31, 2007
 */
public class LayoutSegmented implements Layout {
  private long total, done;
  private int elemSize; // size of each element

  private long[] segPos;    // bytes
  private long[] segMax, segMin; // bytes

  // outer chunk
  private IndexChunker chunker;
  private IndexChunker.Chunk chunkOuter;

  // inner chunk = deal with segmentation
  private IndexChunker.Chunk chunkInner = new IndexChunker.Chunk(0,0,0);

  private boolean debugNext = false;

  /**
   * Constructor.
   *
   * @param segPos      starting address of each segment.
   * @param segSize     number of bytes in each segment.
   * @param elemSize    size of an element in bytes.
   * @param srcShape    shape of the entire data array.
   * @param wantSection the wanted section of data
   * @throws ucar.ma2.InvalidRangeException if ranges are misformed
   */
  public LayoutSegmented(long[] segPos, int[] segSize, int elemSize, int[] srcShape, Section wantSection) throws InvalidRangeException {
    assert segPos.length == segSize.length;
    this.segPos = segPos;

    int nsegs = segPos.length;
    segMin = new long[nsegs];
    segMax = new long[nsegs];
    long totalElems = 0;
    for (int i = 0; i < nsegs; i++) {
      assert segPos[i] >= 0;
      assert segSize[i] > 0;
      assert (segSize[i] % elemSize) == 0;

      segMin[i] = totalElems;
      totalElems += segSize[i];
      segMax[i] = totalElems;
    }
    assert totalElems >=  Index.computeSize(srcShape) * elemSize;

    chunker = new IndexChunker(srcShape, wantSection);
    this.total = chunker.getTotalNelems();
    this.done = 0;
    this.elemSize = elemSize;
  }

  public long getTotalNelems() {
    return total;
  }

  public int getElemSize() {
    return elemSize;
  }

  public boolean hasNext() {
    return done < total;
  }

  ///////////////////

  private long getFilePos(long elem) {
    int segno = 0;
    while (elem >= segMax[segno]) segno++;
    return segPos[segno] + elem - segMin[segno];
  }

  // how many more bytes are in this segment ?
  private int getMaxBytes(long start) {
    int segno = 0;
    while (start >= segMax[segno]) segno++;
    return (int) (segMax[segno] - start);
  }

  private int needInner = 0;
  private int doneInner = 0;

  public Chunk next() {
    Chunk result = null;

    if (needInner > 0) {
      result = nextInner(false, 0);

    } else {
      result = nextOuter();
      int nbytes = getMaxBytes( chunkOuter.getSrcElem() * elemSize);
      if (nbytes < result.getNelems() * elemSize)
        result = nextInner(true, nbytes);
    }

    done += result.getNelems();
    doneInner += result.getNelems();
    needInner -= result.getNelems();

    if (debugNext)
      System.out.println(" next chunk: " + result);

    return result;
  }

  private Chunk nextInner(boolean first, int nbytes) {
    if (first) {
      chunkInner.setNelems(nbytes / elemSize);
      chunkInner.setDestElem( chunkOuter.getDestElem());
      needInner = chunkOuter.getNelems();
      doneInner = 0;

    } else {
      chunkInner.incrDestElem( chunkInner.getNelems()); // increment using last chunks' value
      nbytes = getMaxBytes( (chunkOuter.getSrcElem() + doneInner) * elemSize);
      nbytes = Math.min(nbytes, needInner * elemSize);
      chunkInner.setNelems(nbytes / elemSize); // set this chunk's value
    }

    chunkInner.setSrcPos( getFilePos( (chunkOuter.getSrcElem() + doneInner) * elemSize));
    return chunkInner;
  }

  public Chunk nextOuter() {
    chunkOuter = chunker.next();
    long srcPos = getFilePos( chunkOuter.getSrcElem() * elemSize);
    chunkOuter.setSrcPos( srcPos);
    return chunkOuter;
  }

}

