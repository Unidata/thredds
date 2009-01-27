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
 * LayoutRegularSegmented has data stored in segments that are regularly spaced.
 * This is now Netcdf-3 "record variables" are laid out.
 *
 * @author caron
 * @since Dec 31, 2007
 */
public class LayoutRegularSegmented implements Layout {
  private long total, done, innerNelems;
  private long startPos;
  private long recSize;
  private int elemSize;

  // outer chunk
  private IndexChunker chunker;
  private IndexChunker.Chunk chunkOuter;

  // inner chunk = deal with segmentation
  private IndexChunker.Chunk chunkInner = new IndexChunker.Chunk(0,0,0);

  private boolean debugNext = false;

  /**
   * Constructor.
   *
   * @param startPos starting address of the entire data array.
   * @param elemSize size of an element in bytes.
   * @param recSize  size of outer stride in bytes
   * @param srcShape    shape of the entire data array. must have rank > 0
   * @param wantSection the wanted section of data
   * @throws ucar.ma2.InvalidRangeException if ranges are misformed
   */
  public LayoutRegularSegmented(long startPos, int elemSize, long recSize, int[] srcShape, Section wantSection) throws InvalidRangeException {
    assert startPos > 0;
    assert elemSize > 0;
    assert recSize > 0;
    assert srcShape.length > 0;

    this.startPos = startPos;
    this.elemSize = elemSize;
    this.recSize = recSize;

    chunker = new IndexChunker(srcShape, wantSection);
    this.total = chunker.getTotalNelems();
    this.innerNelems = (srcShape[0] == 0) ? 0 : Index.computeSize(srcShape) / srcShape[0];
    this.done = 0;
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
    long segno = elem / innerNelems;
    long offset = elem % innerNelems;
    return startPos + segno * recSize + offset * elemSize;
  }

  // how many more elements are in this segment ?
  private int getMaxElem(long startElem) {
    return (int) (innerNelems - startElem % innerNelems);
  }

  private int needInner = 0;
  private int doneInner = 0;

  public Chunk next() {
    IndexChunker.Chunk result = null;

    if (needInner > 0) {
      result = nextInner(false, 0);

    } else {
      result = nextOuter();
      int nelems = getMaxElem( result.getSrcElem());
      if (nelems < result.getNelems())
        result = nextInner(true, nelems);
    }

    done += result.getNelems();
    doneInner += result.getNelems();
    needInner -= result.getNelems();

    if (debugNext)
      System.out.println(" next chunk: " + result);

    return result;
  }

  private IndexChunker.Chunk nextInner(boolean first, int nelems) {
    if (first) {
      chunkInner.setNelems(nelems);
      chunkInner.setDestElem( chunkOuter.getDestElem());
      needInner = chunkOuter.getNelems();
      doneInner = 0;

    } else {
      chunkInner.incrDestElem( chunkInner.getNelems()); // increment using last chunks' value
      nelems = getMaxElem( chunkOuter.getSrcElem() + doneInner);
      nelems = Math.min(nelems, needInner);
      chunkInner.setNelems(nelems); // set this chunk's value
    }

    chunkInner.setSrcElem( chunkOuter.getSrcElem() + doneInner);
    chunkInner.setSrcPos( getFilePos( chunkOuter.getSrcElem() + doneInner));
    return chunkInner;
  }

  public IndexChunker.Chunk nextOuter() {
    chunkOuter = chunker.next();
    chunkOuter.setSrcPos( getFilePos( chunkOuter.getSrcElem()));
    return chunkOuter;
  }

}

