// $Id: RegularIndexer.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
public class RegularLayoutTest extends Indexer {
  private int elemSize; // size of each element
  private long startPos; // starting address

  Layout layout;

  private FileIndex index; // file pos tracker
  private Chunk chunk, chunk2; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private long total, done, done2;

  private boolean debug = false, debugMerge = false, debugNext = false;

  /**
   * Constructor.
   *
   * @param startPos starting address of the entire data array.
   * @param elemSize size of on element in bytes.
   * @param recSize  if > 0, then size of outer stride in bytes, else ignored
   * @param varShape shape of the entire data array.
   * @param want     the wanted section of data, contains a List of Range objects,
   *                 corresponding to each Dimension, else null means all.
   * @throws InvalidRangeException is ranges are misformed
   */
  private RegularLayoutTest(long startPos, int elemSize, int recSize, int[] varShape, Section want) throws InvalidRangeException {
    this.elemSize = elemSize;
    this.startPos = startPos;

    boolean isRecord = recSize > 0;

    // clean up and check want Section
    if (want == null)
      want = new Section(varShape);
    else
      want.setDefaults(varShape);
    String err = want.checkInRange(varShape);
    if (err != null)
      throw new InvalidRangeException(err);

    // compute the layout
    layout = new Layout(startPos, elemSize, recSize, varShape, want);

    // compute total size of wanted section
    this.total = want.computeSize();
    this.done = 0;
    this.done2 = 0;

    // deal with nonzero wantOrigin : need strides before userStrides are included
    int varRank = varShape.length;
    int[] stride = new int[varRank]; // variable strides, slowest = 0
    int product = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      stride[ii] = product;
      product *= varShape[ii];
    }
    long offset = 0; // offset to start of first wanted value
    for (int ii = 0; ii < varRank; ii++) {
      long realStride = (isRecord && ii == 0) ? recSize : elemSize * stride[ii];
      offset += realStride * want.getOrigin(ii);
    }

    // merge contiguous inner dimensions for efficiency
    int[] mergeShape = want.getShape();
    if (debugMerge) printa("mergeShape=", mergeShape);
    int rank = varRank;
    int lastDim = isRecord ? 2 : 1; // cant merge record dimension
    while ((rank > lastDim) && (varShape[rank - 1] == want.getShape(rank - 1)) && (want.getStride(rank - 2) == 1)) {
      mergeShape[rank - 2] *= mergeShape[rank - 1];
      rank--;
      if (debugMerge) printa("mergeShape=", mergeShape, rank);
    }

    // how many elements at a time?
    if ((varRank == 0) || (isRecord && varRank == 1) || (want.getStride(rank - 1) > 1)) // LOOK was rank, not varRank
      this.nelems = 1;
    else {
      this.nelems = mergeShape[rank - 1];
      mergeShape[rank - 1] = 1;
    }

    // compute final strides : include user stride
    int[] finalStride = new int[varRank];
    product = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      finalStride[ii] = product * want.getStride(ii);
      product *= varShape[ii];
    }

    // make stride into bytes instead of elements
    for (int ii = 0; ii < rank; ii++)
      finalStride[ii] *= elemSize;
    if (isRecord && varRank > 0)
      finalStride[0] = recSize * want.getStride(0);

    int[] finalShape = new int[rank];
    for (int ii = 0; ii < rank; ii++)
      finalShape[ii] = mergeShape[ii];

    index = new FileIndex(offset, finalShape, finalStride);

    if (debug) {
      System.out.println("*************************");
      System.out.println("elemSize= "+elemSize+" startPos= "+startPos+" recSize= "+recSize);
      printa(" varShape=", varShape);
      System.out.println(" varShape total= " + Index.computeSize(varShape));
      System.out.println(" want= " + want);
      System.out.println(" want total= " + total);
      System.out.println(" isRecord= " + isRecord);
      System.out.println();

      System.out.println(" mergeRank= " + rank + " varRank= " + varRank);
      System.out.println(" nelems read at a time= " + nelems);
      printa(" finalShape=", finalShape);
      printa(" finalStride", finalStride, rank);
      System.out.println(" offset= " + offset);

    }
  }

  /**
   * Constructor.
   *
   * @param startPos starting address of the entire data array.
   * @param elemSize size of on element in bytes.
   * @param recSize  if > 0, then size of outer stride in bytes, else ignored
   * @param varShape shape of the entire data array.
   * @param want     the wanted section of data, contains a List of Range objects,
   *                 corresponding to each Dimension, else null means all.
   * @throws InvalidRangeException is ranges are misformed
   */
  RegularLayoutTest(long startPos, int elemSize, int recSize, int[] varShape, Section want, boolean dummy) throws InvalidRangeException {
    this.elemSize = elemSize;

    // clean up and check want Section
    if (want == null)
      want = new Section(varShape);
    else
      want.setDefaults(varShape);
    String err = want.checkInRange(varShape);
    if (err != null)
      throw new InvalidRangeException(err);

    // compute the layout
    layout = new Layout(startPos, elemSize, recSize, varShape, want);

    // compute total size of wanted section
    this.total = want.computeSize();
    this.done2 = 0;
  }


  // this knows how to map an array index to a file position.
  // it merges inner dimensions if it can
  private class Layout {
    long startPos;
    int nelems, nchunks;
    List<Dim> dimList = new ArrayList<Dim>();
    MyIndex myIndex;

    Layout(long startPos, int elemSize, int recSize, int[] varShape, Section wantSection) {

      List<Range> wantr = wantSection.getRanges();

      // create the List<Dim>
      boolean isRecord = recSize > 0;
      int varRank = varShape.length;
      int stride = 1;
      for (int ii = varRank - 1; ii >= 0; ii--) {
        int realStride = (isRecord && ii == 0) ? recSize : elemSize * stride;
        dimList.add(new Dim(realStride, varShape[ii], wantr.get(ii))); // note reversed : fastest first
        stride *= varShape[ii];
      }

      // merge contiguous inner dimensions for efficiency
      int lastDim = isRecord ? 2 : 1; // cant merge record dimension
      if (debugMerge) System.out.println("merge= " + this);

      // count how many merge dimensions
      int merge = 0;
      for (int i = 0; i < dimList.size() - lastDim; i++) {
        Dim elem = dimList.get(i);
        Dim elem2 = dimList.get(i+1);
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
      for (int i=0; i < merge; i++)
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
      for (int i = 0; i < dimList.size(); i++) {
        Dim dim = dimList.get(i);
        offset += dim.byteStride * dim.want.first();
      }
      this.startPos = startPos + offset;

      // we will use an Index object to keep track of the chunks
      int rank = dimList.size();
      int[] byteStride = new int[rank];
      int[] shape = new int[rank];
      for (int i = 0; i < dimList.size(); i++) {
        Dim dim = dimList.get(i);
        byteStride[rank-i-1] = dim.byteStride * dim.want.stride();
        shape[rank-i-1] = dim.wantSize;
      }
      if (debug) {
        printa(" indexShape=", shape);
        printa(" indexStride=", byteStride);
        System.out.println(" indexChunks="+Index.computeSize(shape));
      }
      myIndex = new MyIndex(shape, byteStride);

      // sanity checks
      nchunks = (int) Index.computeSize(shape);
    }

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
      sbuff.append(" startPos="+startPos);

      return sbuff.toString();
    }

    long currentPos() {
      return startPos + myIndex.currentElement();
    }

    void incr() {
      myIndex.incr();
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

    /** constructor for subclasses only. */
  private class MyIndex extends Index {
    MyIndex( int[] shape, int[] stride) {
      super(shape,stride);
    }
    protected int incr() { return super.incr(); }
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

  public int getChunkSize() {
    return nelems;
  }              // debug

  //public int[] getWantShape() {
    //return want.getShape();
  //}  // for N3iosp

  // Indexer abstract methods
  public int getTotalNelems() {
    return (int) total;
  } // LOOK int not long

  public int getElemSize() {
    return elemSize;
  }

  public boolean hasNext() {
    return done < total;
  }

  public boolean hasNext2() {
    return done2 < total;
  }

  public Chunk next2() {

    if (chunk2 == null) {
      chunk2 = new Chunk(startPos, nelems, 0);

    } else {
      layout.incr(); // increment indexer
      chunk2.indexPos += nelems; // always read nelems at a time
    }

    // Get the current element's byte index from the start
    chunk2.filePos = layout.currentPos();

    if (debugNext) {
      System.out.println(" next chunk2: " + chunk2);
    }

    done2 += nelems;
    //if (debugNext) System.out.println(" done = "+done+" total = "+total);
    return chunk2;
  }

  public Chunk next() {

    if (chunk == null) {
      chunk = new Chunk(startPos, nelems, 0);

    } else {
      index.incr(); // increment file position
      chunk.indexPos += nelems; // always read nelems at a time
    }

    // Get the current element's byte index from the start
    chunk.filePos = startPos + index.currentPos();

    if (debugNext) {
      //printa("-- current index= ", index.current);
      //System.out.println(" pos= " + index.currentPos());
      System.out.println(" next chunk: " + chunk);
    }

    done += nelems;
    //if (debugNext) System.out.println(" done = "+done+" total = "+total);
    return chunk;
  }


  private class FileIndex {
    private long startPos;
    private int[] shape, stride, current;
    private int rank;

    FileIndex(long startPos, int[] shape, int[] stride) {
      this.startPos = startPos;
      this.shape = shape;
      this.stride = stride;
      this.rank = shape.length;
      this.current = new int[rank];
    }

    void incr() {
      int digit = rank - 1;
      while (digit >= 0) {
        current[digit]++;
        if (current[digit] < shape[digit])
          break;                        // normal exit
        current[digit] = 0;               // else, carry
        digit--;
      }
    }

    long currentPos() {
      long value = startPos;
      for (int ii = 0; ii < rank; ii++)
        value += current[ii] * ((long) stride[ii]);
      return value;
    }

  }


  static public void compare(RegularLayoutTest rlayout) throws InvalidRangeException {
    assert rlayout.layout.startPos == rlayout.startPos +rlayout.index.startPos : "startPos= "+rlayout.layout.startPos+" != "+ rlayout.startPos+rlayout.index.startPos;
    assert rlayout.layout.nelems == rlayout.nelems : " nelems= "+rlayout.layout.nelems +" != "+ rlayout.nelems;
    assert rlayout.layout.nchunks * rlayout.layout.nelems == rlayout.getTotalNelems();

    while (rlayout.hasNext() && rlayout.hasNext2()) {
      Indexer.Chunk chunk = rlayout.next();
      Indexer.Chunk chunk2 = rlayout.next2();
      assert chunk.getFilePos() == chunk2.getFilePos();
      assert chunk.getIndexPos() == chunk2.getIndexPos();
      assert chunk.getNelems() == chunk2.getNelems();
    }
    assert !rlayout.hasNext();
    assert !rlayout.hasNext2();

  }


  static public void main(String args[]) throws InvalidRangeException {
    // RegularLayoutTest(long startPos, int elemSize, int recSize, int[] varShape, Section want) throws InvalidRangeException {

    // 4D
    Section var = new Section().appendRange(10).appendRange(20).appendRange(30).appendRange(4);
    Section want = new Section().appendRange(2).appendRange(10).appendRange(30).appendRange(4);
    compare(new RegularLayoutTest(0, 1, 0, var.getShape(), want));
    compare(new RegularLayoutTest(1000, 8, 4000, var.getShape(), want));

    want = new Section().appendRange(5,5).appendRange(0,900,20).appendRange(3,3).appendRange(0,16);
    compare(new RegularLayoutTest(1000, 4, 888, new int[] {13,1000,4,17}, want));
    compare(new RegularLayoutTest(1000, 4, 0, new int[] {13,1000,4,17}, want));

    // 3D
    want = new Section().appendRange(5,5).appendRange().appendRange();
    compare(new RegularLayoutTest(1000, 4, 888, new int[] {13,40,5}, want));
    compare(new RegularLayoutTest(1000, 4, 0, new int[] {13,40,5}, want));

    want = new Section().appendRange(2).appendRange(0,55,11);
    compare(new RegularLayoutTest(200000, 3, 123999, new int[] {5,55}, want));
    compare(new RegularLayoutTest(200000, 3, 0, new int[] {5,55}, want));

    want = new Section().appendRange(0,55,19).appendRange();
    compare(new RegularLayoutTest(4000, 4000, 4000, new int[] {60,5}, want));
    compare(new RegularLayoutTest(4000, 4000, 0, new int[] {60,5}, want));

    // 2D
    want = new Section().appendRange(6,55,2).appendRange();
    compare(new RegularLayoutTest(4000, 4000, 4000, new int[] {60,5}, want));
    compare(new RegularLayoutTest(4000, 4000, 0, new int[] {60,5}, want));

    // 1D
    want = new Section().appendRange(0,55,9);
    compare(new RegularLayoutTest(4000, 4000, 4000, new int[] {60}, want));
    compare(new RegularLayoutTest(4000, 4000, 0, new int[] {60}, want));

    want = new Section().appendRange(19,55,3);
    compare(new RegularLayoutTest(4000, 4000, 4000, new int[] {60}, want));
    compare(new RegularLayoutTest(4000, 4000, 0, new int[] {60}, want));

    // scaler */
    want = new Section();
    compare(new RegularLayoutTest(4000, 4000, 4000, new int[] {}, want));  // */
    compare(new RegularLayoutTest(4000, 4000, 0, new int[] {}, want));

  }


}