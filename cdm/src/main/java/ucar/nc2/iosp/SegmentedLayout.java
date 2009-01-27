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
import ucar.ma2.Range;

import java.util.List;
import java.util.ArrayList;

/**
 * SegmentedLayout has data stored in segments.
 *
 * @author caron
 * @since Dec 31, 2007
 * @deprecated use LayoutSegmented
 */
public class SegmentedLayout extends Indexer {
  private List<Dim> dimList = new ArrayList<Dim>();
  private Index myIndex;

  private long total, done;
  private int elemSize; // size of each element

  int nsegs;
  private long[] segPos;
  private long[] segMax, segMin;

  // outer chunk
  private Chunk chunkOuter;
  private int contigElemsOuter; // number of elements to read at one time
  private long origin;

  // inner chunk = deal with segmentation
  private Chunk chunkInner;

  private boolean debug = false, debugDetail = false, debugMerge = false, debugNext = false;


  /**
   * Constructor.
   *
   * @param segPos      starting address of each segment.
   * @param segSize     number of bytes in each segment.
   * @param elemSize    size of an element in bytes.
   * @param varShape    shape of the entire data array.
   * @param wantSection the wanted section of data
   * @throws InvalidRangeException if ranges are misformed
   */
  public SegmentedLayout(long[] segPos, int[] segSize, int elemSize, int[] varShape, Section wantSection) throws InvalidRangeException {
    assert segPos.length == segSize.length;
    this.nsegs = segPos.length;
    this.segPos = segPos;
    this.elemSize = elemSize;

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
    assert totalElems >=  Index.computeSize(varShape) * elemSize;

    wantSection = Section.fill(wantSection, varShape); // will throw InvalidRangeException if illegal section

    // compute total size of wanted section
    this.total = wantSection.computeSize();
    this.done = 0;
    this.origin = 0;
    this.contigElemsOuter = 1;

    // compute the layout

    // create the List<Dim>
    int varRank = varShape.length;
    int stride = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      int realStride = elemSize * stride;
      dimList.add(new Dim(realStride, varShape[ii], wantSection.getRange(ii))); // note reversed : fastest first
      stride *= varShape[ii];
    }

    // merge contiguous inner dimensions for efficiency
    int lastDim = 1;
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
    if (debugDetail) System.out.println(" final= " + this);

    // how many elements can we do at a time?
    if ((varRank == 0) || (dimList.get(0).want.stride() > 1))
      this.contigElemsOuter = 1;
    else {
      Dim innerDim = dimList.get(0);
      this.contigElemsOuter = innerDim.wantSize;
      innerDim.wantSize = 1; // inner dimension has one element of length innerDim.wantSize
    }

    // the origin can be handled by adding to the startPos
    for (Dim dim : dimList) {
      origin += dim.byteStride * dim.want.first();
    }

    // we will use an Index object to keep track of the chunks
    int rank = dimList.size();
    int[] byteStride = new int[rank];
    int[] shape = new int[rank];
    for (int i = 0; i < dimList.size(); i++) {
      Dim dim = dimList.get(i);
      byteStride[rank - i - 1] = dim.byteStride * dim.want.stride(); // reverse to slowest first
      shape[rank - i - 1] = dim.wantSize;
    }
    if (debugDetail) {
      printa(" indexShape=", shape);
      printa(" indexStride=", byteStride);
      System.out.println(" indexChunks=" + Index.computeSize(shape));
    }
    myIndex = new Index(shape, byteStride);

    // sanity checks
    long nchunks = Index.computeSize(shape);
    assert nchunks * contigElemsOuter == total;

    if (debug) {
      System.out.println("SegmentedLayout = " + this);
      System.out.println("origin= " + origin + " elemSize= " + elemSize + " varShape= " + printa(varShape) + " section= " + wantSection);
    }
  }

  private long getFilePos(long elem) {
    elem += origin;
    int segno = 0;
    while (elem >= segMax[segno]) segno++;
    return segPos[segno] + elem - segMin[segno];
  }

  // how many more elements are in this segment ?
  private int getMaxElems(long start) {
    start += origin;
    int segno = 0;
    while (start >= segMax[segno]) segno++;
    return (int) (segMax[segno] - start);
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

  public long getTotalNelems() {
    return total;
  }

  public int getElemSize() {
    return elemSize;
  }

  public boolean hasNext() {
    return done < total;
  }

  private int needInner = 0;

  public Chunk next() {
    Chunk result = null;

    if (needInner > 0) {
      result = nextInner(false, 0);

    } else {
      result = nextOuter();
      int n = getMaxElems(done * elemSize);
      if (n < contigElemsOuter * elemSize) {
        result = nextInner(true, n);
      }
    }

    done += result.getNelems();
    needInner -= result.getNelems();

    if (debugNext)
      System.out.println(" next chunk: " + result);

    return result;
  }

  private Chunk nextInner(boolean first, int n) {
    if (first) {
      chunkInner.setFilePos(chunkOuter.getFilePos());
      chunkInner.setNelems(n / elemSize);
      chunkInner.setStartElem(chunkOuter.getStartElem());
      needInner = contigElemsOuter;

    } else {
      n = getMaxElems(done * elemSize);
      n = Math.min(n, needInner * elemSize);
      chunkInner.setFilePos(getFilePos(done * elemSize));
      chunkInner.incrStartElem(chunkInner.getNelems());
      chunkInner.setNelems(n / elemSize);
    }

    return chunkInner;
  }

  public Chunk nextOuter() {
    if (chunkOuter == null) {
      chunkOuter = new Chunk(getFilePos(0), contigElemsOuter, 0);
      chunkInner = new Chunk(0, 0, 0);

    } else {
      myIndex.incr(); // increment one element, but it represents one chunk = nelems * sizeElem
      chunkOuter.incrStartElem(contigElemsOuter);
    }

    chunkOuter.setFilePos( getFilePos( myIndex.currentElement()));

    return chunkOuter;
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
