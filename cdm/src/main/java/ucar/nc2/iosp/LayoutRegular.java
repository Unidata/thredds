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