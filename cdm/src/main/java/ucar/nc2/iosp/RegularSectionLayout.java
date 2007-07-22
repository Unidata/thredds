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
 * The user asks for some section, wantSection (may have strides).
 * For each dataSection that intersects wantSection, a RegularSectionLayout is created, which
 * figures out the optimal access pattern, based on reading contiguous runs of data. Each
 * RegularSectionLayout handles only one dataSection.
 * <p/>
 * Both dataSection and wantSection refer to the variable's overall shape.
 *
 * @author caron
 * @see Indexer
 */
public class RegularSectionLayout extends Indexer {
  private List<Dim> dimList = new ArrayList<Dim>();
  private MyIndex dataIndex, wantIndex;

  private int elemSize; // size of each element
  private long startPos; // starting address
  private int startElem;

  private Chunk chunk; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private long total, done;

  private boolean debug = true, debugMerge = false, debugDetail = false, debugNext = true;

  static public Indexer factory(long startFilePos, int elemSize, Section dataSection, Section wantSection) throws InvalidRangeException {
    if (dataSection.equals(wantSection)) // optimize the simple case
      return new SingleChunkIndexer(startFilePos, (int) dataSection.computeSize(), elemSize);

    return new RegularSectionLayout(startFilePos, elemSize, dataSection, wantSection);
  }

  /**
   * Constructor.
   * Assume varSection.intersects(wantSection).
   *
   * @param startFilePos starting address of the dataSection
   * @param elemSize     size of an element in bytes.
   * @param dataSection  the section of data we actually have. must have all ranges with stride = 1.
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
    // Section shifted = intersect.shiftOrigin(dataSection); // want reletive to dataSection
    int wantStride = 1;
    int dataStride = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      Range dr = dataSection.getRange(ii);
      Range wr = wantSection.getRange(ii);
      Range ir = intersect.getRange(ii);
      dimList.add(new Dim(dr, wr, ir, dataStride, wantStride)); // note reversed : fastest first
      dataStride *= dr.length();
      wantStride *= wr.length();
    }

    // the origin can be handled by adding to the startPos
     long fileOffset = 0; // offset in file
     for (Dim dim : dimList) {
       int d = dim.intersect.first() - dim.data.first();
       if (d > 0) fileOffset += elemSize * dim.dataStride * d;
     }
     this.startPos = startFilePos + fileOffset;

     startElem = 0; // offset in want
     for (Dim dim : dimList) {
       int d = dim.intersect.first() - dim.want.first();
       if (d > 0)  startElem += dim.wantStride * d;
     }


    /* merge contiguous inner dimensions for efficiency
    if (debugMerge) System.out.println("RegularSectionLayout= " + this);

    // count how many merge dimensions
    int merge = 0;
    for (int i = 0; i < dimList.size()-1; i++) {
      Dim elem = dimList.get(i);
      if (elem.want.stride() != 1) break;

      if (i > 0) {
        Dim prevElem = dimList.get(i-1);
        if (prevElem.want.length() != prevElem.intersect.length()) break;
        if (prevElem.data.length() != prevElem.intersect.length()) break;
      }
      merge++;
    }

    // merge the dimensions
    for (int i = 0; i < merge; i++) {
      Dim elem = dimList.get(i);
      Dim elem2 = dimList.get(i + 1);
      elem2.dataStride *= elem.dataStride;
      elem2.wantStride *= elem.wantStride;
      if (debugMerge) System.out.println(" ----" + this);
    }

    // delete merged
    dimList = dimList.subList(merge, varRank);   */

    // how many elements can we do at a time?
    if (varRank == 0)
      this.nelems = 1;
    else {
      Dim innerDim = dimList.get(0);
      this.nelems = innerDim.ncontigElements;
      if (innerDim.ncontigElements > 1) {
        innerDim.wantNelems = 1;   // 1 wantIndex increment = nelems
        innerDim.wantStride = innerDim.ncontigElements;
      }
    }

     // we will use an Index object to keep track of the chunks
    int rank = dimList.size();
    int[] dataStrides = new int[rank];
    int[] wantStrides = new int[rank];
    int[] shape = new int[rank];
    for (int i = 0; i < dimList.size(); i++) {  // reverse to slowest first
      Dim dim = dimList.get(i);
      dataStrides[rank - i - 1] = dim.dataStride * dim.want.stride();
      wantStrides[rank - i - 1] = dim.wantStride * dim.want.stride();
      shape[rank - i - 1] = dim.wantNelems;
    }
    if (debugDetail) {
      printa(" indexShape=", shape);
      printa(" dataStrides=", dataStrides);
      printa(" wantStride=", wantStrides);
      System.out.println(" indexChunks=" + Index.computeSize(shape));
    }
    dataIndex = new MyIndex(shape, dataStrides);
    wantIndex = new MyIndex(shape, wantStrides);

    // sanity checks
    long nchunks = Index.computeSize(shape);
    assert nchunks * nelems == total;

    if (debug) {
      System.out.println("RegularSectionLayout total = "+total+" nchunks= "+nchunks+" nelems= "+nelems+
          " dataSection= " + dataSection + " wantSection= " + wantSection+ " intersect= " + intersect+ this);
    }
  }

  private class Dim {
    //int byteStride; // bytes per element
    //int maxSize;    // number of elements
    Range data;       // Range we got
    Range want;       // Range we want
    Range intersect;  // Range we want
    int dataStride;   // stride in the data array
    int wantStride;   // stride in the want array
    int wantNelems;
    int ncontigElements;

    Dim(Range data, Range want, Range intersect, int dataStride, int wantStride) {
      //assert want.length() <= maxSize : want.last() +" > "+ maxSize ;
      this.data = data;
      this.want = want;
      this.intersect = intersect;
      this.dataStride = dataStride;
      this.wantStride = wantStride;
      this.ncontigElements = intersect.stride() == 1 ? intersect.length() : 1;
      this.wantNelems = intersect.length();
    }

    public String toString() { return "  data = "+data+ " want = "+want+ " intersect = "+intersect+ " ncontigElements = "+ncontigElements; }
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
      chunk = new Chunk(startPos, nelems, startElem);
    } else {
      dataIndex.incr();
      wantIndex.incr();
    }

    // Get the current element's byte index from the start of the file
    chunk.setFilePos(startPos + elemSize * dataIndex.currentElement());

    int w = wantIndex.currentElement();
    chunk.setStartElem(startElem + wantIndex.currentElement());

    if (debugNext)
      System.out.println(" chunk: " + chunk);
    if (debugDetail) {
      System.out.println(" dataIndex: " + dataIndex);
      System.out.println(" wantIndex: " + wantIndex);
    }

    done += nelems;
    return chunk;
  }

  ////////////////////

  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      sbuff.append("\n");
      sbuff.append(elem);
    }
    return sbuff.toString();
  }

}