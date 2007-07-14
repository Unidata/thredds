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

import ucar.ma2.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Assume that the data is divided into sections, described by dataSection. All the data within a dataSection is
 * stored contiguously, in a regular layout. Assume dataSection strides must be = 1.
 * <p/>
 * The user asks for some section, wantSection.
 * For each dataSection that intersects wantSection, a RegularSectionLayout is created, which
 * figures out the optimal access pattern, based on reading contiguous runs of data.
 * Both dataSection and wantSection refer to the variable's overall shape.
 *
 * @author caron
 * @see Indexer
 */
public class RegularSectionLayout extends Indexer {
  private List<Dim> dimList = new ArrayList<Dim>();
  private MyIndex myIndex;

  private int elemSize; // size of each element
  private long startPos; // starting address

  private Chunk chunk; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private long total, done;

  private boolean debug = false, debugMerge = false, debugNext = false;

  static public Indexer factory(long startFilePos, int elemSize, Section dataSection, Section wantSection) throws InvalidRangeException {
    if (dataSection.computeSize() == wantSection.computeSize()) // optimize the simple case
      return new SingleChunkIndexer(startFilePos, (int) dataSection.computeSize(), elemSize);

    return new  RegularSectionLayout(startFilePos, elemSize, dataSection, wantSection);
  }

  /**
   * Constructor.
   * Assume varSection.intersects(wantSection).
   *
   * @param startFilePos starting address of the dataSection
   * @param elemSize     size of an element in bytes.
   * @param dataSection  section of the entire variable array. must have all ranges with stride = 1.
   * @param wantSection  the wanted section of data
   * @throws InvalidRangeException if ranges are misformed
   */
  public RegularSectionLayout(long startFilePos, int elemSize, Section dataSection, Section wantSection) throws InvalidRangeException {
    assert startFilePos >= 0;
    assert elemSize > 0;

    this.elemSize = elemSize;
    this.done = 0;

    // The actual wanted data we can get from this section
    Section intersect = dataSection.intersect(wantSection);
    this.total = intersect.computeSize();
    assert total > 0;
    int varRank = intersect.getRank();

    // create the List<Dim>
    Section shifted = intersect.shiftOrigin(dataSection); // want reletive to dataSection
    int stride = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      Range dr = dataSection.getRange(ii);
      dimList.add(new Dim(elemSize * stride, dr.length(), shifted.getRange(ii))); // note reversed : fastest first
      stride *= dr.length();
    }

    // merge contiguous inner dimensions for efficiency
    if (debugMerge) System.out.println("RegularSectionLayout merge= " + this);

    // count how many merge dimensions
    int merge = 0;
    for (int i = 0; i < dimList.size() - 1; i++) {
      Dim elem = dimList.get(i);
      Dim elem2 = dimList.get(i + 1);
      if ((elem.maxSize == elem.wantSize) && (elem2.want.stride() == 1)) {
        merge++;
      } else {
        break;
      }
    }

    // merge the dimensions
    for (int i = 0; i < merge; i++) {
      Dim elem = dimList.get(i);
      Dim elem2 = dimList.get(i + 1);
      elem2.maxSize *= elem.maxSize;
      elem2.wantSize *= elem.wantSize;
      if (debugMerge) System.out.println(" ----" + this);
    }

    // delete merged
    dimList = dimList.subList(merge, varRank);

    // how many elements can we do at a time?
    if ((varRank == 0) || (dimList.get(0).want.stride() > 1))
      this.nelems = 1;
    else {
      Dim innerDim = dimList.get(0);
      this.nelems = innerDim.wantSize;
      innerDim.wantSize = 1; // inner dimension has one element of length innerDim.wantSize
    }

    // the origin can be handled by adding to the startPos
    long offset = 0; // offset to start of first wanted value
    for (Dim dim : dimList) {
      offset += dim.byteStride * dim.want.first();
    }
    this.startPos = startFilePos + offset;

    // we will use an Index object to keep track of the chunks
    int rank = dimList.size();
    int[] byteStride = new int[rank];
    int[] shape = new int[rank];
    for (int i = 0; i < dimList.size(); i++) {
      Dim dim = dimList.get(i);
      byteStride[rank - i - 1] = dim.byteStride * dim.want.stride(); // reverse to slowest first
      shape[rank - i - 1] = dim.wantSize;
    }
    if (debug) {
      printa(" indexShape=", shape);
      printa(" indexStride=", byteStride);
      System.out.println(" indexChunks=" + Index.computeSize(shape));
    }
    myIndex = new MyIndex(shape, byteStride);

    // sanity checks
    long nchunks = Index.computeSize(shape);
    assert nchunks * nelems == total;

    if (debug) {
      System.out.println("RegularSectionLayout = " + this);
      System.out.println(" startFilePos= " + startFilePos + " elemSize= " + elemSize + " dataSection= " + dataSection + " section= " + wantSection);
    }
  }

  private class Dim {
    int byteStride; // bytes per element
    int maxSize;    // number of elements
    Range want;    // desired Range
    int wantSize;  // keep seperate from want so we can modify when merging

    Dim(int byteStride, int maxSize, Range want) {
      assert want.length() <= maxSize : want.last() +" > "+ maxSize ;

      this.byteStride = byteStride;
      this.maxSize = maxSize;
      this.wantSize = want.length();
      this.want = want;
    }
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

  public Chunk next() {
    if (chunk == null) {
      chunk = new Chunk(startPos, nelems, 0);
    } else {
      myIndex.incr(); // increment one element, but is represents one chunk = nelems * sizeElem
      chunk.incrIndexPos(nelems); // always read nelems at a time
    }

    // Get the current element's byte index from the start of the file
    chunk.setFilePos(startPos + myIndex.currentElement());

    if (debugNext)
      System.out.println(" next chunk: " + chunk);

    done += nelems;
    return chunk;
  }

  ////////////////////

  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("wantSize=");
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(elem.wantSize);
    }
    sbuff.append(" wantStride=");
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(elem.want.stride());
    }
    sbuff.append(" maxSize=");
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(elem.maxSize);
    }
    sbuff.append(" byteStride=");
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(elem.byteStride);
    }

    return sbuff.toString();
  }

  private String printa(int[] a) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < a.length; i++) sbuff.append(a[i] + " ");
    return sbuff.toString();
  }

  private void printa(String name, int[] a) {
    System.out.print(name + "= ");
    for (int i = 0; i < a.length; i++) System.out.print(a[i] + " ");
    System.out.println();
  }
}