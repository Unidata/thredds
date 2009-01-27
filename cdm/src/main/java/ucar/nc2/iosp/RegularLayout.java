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

import ucar.ma2.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Indexer into data that has a "regular" layout, like netcdf-3 and hdf5 compact and contiguous storage.
 * The data is contiguous, with outer dimension varying fastest.
 * Given a Section, this calculates the set of contiguous "chunks" of the wanted data into the stored data.
 * Also handles netcdf3 record dimensions.
 * The wanted section is always a subset of the data section (see RegularSectionLayout where thats not the case).
 *
 * @author caron
 * @deprecated use LayoutRegular
 */
public class RegularLayout extends Indexer {
  private List<Dim> dimList = new ArrayList<Dim>();
  private Index myIndex;

  private int elemSize; // size of each element
  private long startPos; // starting address

  private Chunk chunk; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private long total, done;

  private boolean debug = false, debugMerge = false, debugNext = false;

  static public Indexer factory(long startPos, int elemSize, int recSize, int[] varShape, Section wantSection) throws InvalidRangeException {
    if ((recSize <= 0) && wantSection.equivalent(varShape)) // optimize the simple case
      return new SingleChunkIndexer(startPos, (int) wantSection.computeSize(), elemSize);

    return new RegularLayout(startPos, elemSize, recSize, varShape, wantSection);
  }

  /**
   * Constructor: drop in replacement for old RegularIndexer.
   * @param varShape shape of the entire data array.
   * @param elemSize size of on element in bytes.
   * @param startPos starting address of the entire data array.
   * @param ranges the wanted section of data: List of Range objects,
   *    corresponding to each Dimension, else null means all.
   * @param recSize if > 0, then size of outer stride in bytes, else ignored
   * @throws InvalidRangeException is ranges are misformed
   */
  public RegularLayout( int[] varShape, int elemSize, long startPos, List<Range> ranges, int recSize) throws InvalidRangeException {
    this(startPos, elemSize, recSize, varShape, new Section(ranges));
  }

  /**
   * Constructor.
   *
   * @param startPos starting address of the entire data array.
   * @param elemSize size of an element in bytes.
   * @param recSize  if > 0, then size of outer stride in bytes, else ignored
   * @param varShape shape of the entire data array.
   * @param wantSection the wanted section of data, contains a List of Range objects.
   * @throws InvalidRangeException if ranges are misformed
   */
  public RegularLayout(long startPos, int elemSize, int recSize, int[] varShape, Section wantSection) throws InvalidRangeException {
    assert startPos >= 0;
    assert elemSize > 0;

    this.elemSize = elemSize;
    wantSection =  Section.fill(wantSection, varShape); // will throw InvalidRangeException if illegal section

    // compute total size of wanted section
    this.total = wantSection.computeSize();
    this.done = 0;

    // compute the layout

    // create the List<Dim>
    boolean isRecord = recSize > 0;
    int varRank = varShape.length;
    int stride = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      int realStride = (isRecord && ii == 0) ? recSize : elemSize * stride;
      dimList.add( new Dim(realStride, varShape[ii], wantSection.getRange(ii))); // note reversed : fastest first
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
    myIndex = new Index(shape, byteStride);

    // sanity checks
    long nchunks = Index.computeSize(shape);
    assert nchunks * nelems == total;

    if (debug) {
      System.out.println("RegularLayout = "+this);
      System.out.println("startPos= "+ startPos+" elemSize= "+elemSize+" recSize= "+recSize+" varShape= "+printa(varShape)+" section= "+wantSection);
    }
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

  // debug
  public int getChunkSize() {
    return nelems;
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
      myIndex.incr(); // increment one element, but it represents one chunk = nelems * sizeElem
      chunk.incrStartElem( nelems); // always read nelems at a time
    }

    // Get the current element's byte index from the start of the file
    chunk.setFilePos( startPos + myIndex.currentElement());

    if (debugNext)
      System.out.println(" next chunk: " + chunk);

    done += nelems;
    return chunk;
  }

  ////////////////////

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
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

}