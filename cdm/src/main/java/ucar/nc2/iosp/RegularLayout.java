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
 * Indexer into a regular array.
 * This calculates byte lengths and offsets of the wanted data into the entire data.
 * Assumes that the data is stored "regularly", like netcdf arrays and hdf5 continuous storage.
 * Also handles netcdf3 record dimensions.
 *
 * @author caron
 */
public class RegularLayout extends Indexer {
  private List<Dim> dimList = new ArrayList<Dim>();
  private MyIndex myIndex;
  private Section want;

  private int elemSize; // size of each element
  private long startPos; // starting address

  private Chunk chunk; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private long total, done;

  private boolean debug = false, debugMerge = false, debugNext = false;

  /**
   * Constructor.
   *
   * @param startPos starting address of the entire data array.
   * @param elemSize size of on element in bytes.
   * @param recSize  if > 0, then size of outer stride in bytes, else ignored
   * @param varShape shape of the entire data array.
   * @param wantSection the wanted section of data, contains a List of Range objects,
   *                 corresponding to each Dimension, else null means all.
   * @throws InvalidRangeException if ranges are misformed
   */
  public RegularLayout(long startPos, int elemSize, int recSize, int[] varShape, Section wantSection) throws InvalidRangeException {
    this.elemSize = elemSize;
    this.want = (wantSection == null) ?  new Section(varShape) :  new Section(wantSection.getRanges(), varShape);
    String err = want.checkInRange(varShape);
    if (err != null)
      throw new InvalidRangeException(err);

    // compute total size of wanted section
    this.total = want.computeSize();
    this.done = 0;

    // compute the layout

    // create the List<Dim>
    List<Range> wantr = want.getRanges();
    boolean isRecord = recSize > 0;
    int varRank = varShape.length;
    int stride = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      int realStride = (isRecord && ii == 0) ? recSize : elemSize * stride;
      dimList.add( new Dim(realStride, varShape[ii], wantr.get(ii))); // note reversed : fastest first
      stride *= varShape[ii];
    }

    // merge contiguous inner dimensions for efficiency
    int lastDim = isRecord ? 2 : 1; // cant merge record dimension
    if (debugMerge) System.out.println("merge= " + this);

    // count how many merge dimensions
    int merge = 0;
    for (int i = 0; i < dimList.size() - lastDim; i++) {
      Dim elem = dimList.get(i);
      Dim elem2 = dimList.get(i + 1);
      if (elem.maxSize == elem.wantSize && (elem2.want.stride() == 1)) {
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
    for (int i = 0; i < merge; i++)
      dimList.remove(0);
    if (debug) System.out.println(" final= " + this);

    // how many elements can we do at a time?
    if ((varRank == 0) || (isRecord && varRank == 1) || (dimList.get(0).want.stride() > 1))
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
    this.startPos = startPos + offset;

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
  }

  private class Dim {
    int byteStride; // bytes per element
    int maxSize;    // number of elements
    Range want;    // desired Range
    int wantSize;  // keep seperate from want so we can modify when merging

    Dim(int byteStride, int maxSize, Range want) {
      this.byteStride = byteStride;
      this.maxSize = maxSize;
      this.wantSize = want.length();
      this.want = want;
    }
  }

  private class MyIndex extends Index {
    MyIndex(int[] shape, int[] stride) {
      super(shape, stride);
    }

    protected int incr() {
      return super.incr();
    }
  }

  // debug
  public int getChunkSize() {
    return nelems;
  }

  public int[] getWantShape() {
    return want.getShape();
  }  // for N3iosp

  public int getTotalNelems() {
    return (int) total;
  } // LOOK int should be long

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
      chunk.indexPos += nelems; // always read nelems at a time
    }

    // Get the current element's byte index from the start of the file
    chunk.filePos = startPos + myIndex.currentElement();

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

  private void printa(String name, int[] a, int rank) {
    System.out.print(name + "= ");
    for (int i = 0; i < rank; i++) System.out.print(a[i] + " ");
    System.out.println();
  }

  private void printa(String name, int[] a) {
    System.out.print(name + "= ");
    for (int i = 0; i < a.length; i++) System.out.print(a[i] + " ");
    System.out.println();
  }
}