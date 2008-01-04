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