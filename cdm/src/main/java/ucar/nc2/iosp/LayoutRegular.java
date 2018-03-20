/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

/**
 * Indexer into data that has a "regular" layout, like netcdf-3 and hdf5 compact and contiguous storage.
 * The data is contiguous, with outer dimension varying fastest.
 * Given a Section, this calculates the set of contiguous "chunks" of the wanted data into the stored data.
 * The wanted section is always a subset of the data section (see RegularSectionLayout where thats not the case).
 *
 * @author caron
 * @since Jan 3, 2008
 */
public class LayoutRegular implements Layout {
  private IndexChunker chunker;
  private long startPos; // starting position
  private int elemSize; // size of each element

  /**
   * Constructor.
   *
   * @param startPos starting address of the entire data array.
   * @param elemSize size of an element in bytes.
   * @param varShape shape of the entire data array.
   * @param wantSection the wanted section of data, contains a List of Range objects.
   * @throws InvalidRangeException if ranges are misformed
   */
  public LayoutRegular(long startPos, int elemSize, int[] varShape, Section wantSection) throws InvalidRangeException {
    assert startPos >= 0;
    assert elemSize > 0;

    this.startPos = startPos;
    this.elemSize = elemSize;

    chunker = new IndexChunker(varShape, wantSection);
  }

  public long getTotalNelems() {
    return chunker.getTotalNelems();
  }

  public int getElemSize() {
    return elemSize;
  }

  public boolean hasNext() {
    return chunker.hasNext();
  }

  public Chunk next() {
    IndexChunker.Chunk chunk = chunker.next();
    chunk.setSrcPos( startPos + chunk.getSrcElem() * elemSize);
    return chunk;
  }
}