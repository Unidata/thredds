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

import ucar.ma2.Index;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

/**
 * LayoutSegmented has data stored in segments.
 * Assume that each segment size is a multiple of elemSize.
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
   * @param segSize     number of bytes in each segment. Asume multiple of elemSize
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

