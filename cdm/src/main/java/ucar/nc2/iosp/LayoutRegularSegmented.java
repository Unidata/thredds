/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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

  private static final boolean debugNext = false;

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
    IndexChunker.Chunk result;

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

