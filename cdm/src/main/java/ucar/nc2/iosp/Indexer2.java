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
import ucar.ma2.Index;
import ucar.ma2.Range;

import java.util.List;
import java.util.ArrayList;

/**
 * Iterator to read/write subsets of a multidimensional array, finding the contiguous chunks.
 * The iteration is monotonic in both src and dest positions.
 * <p/>
 * Example for Integers:
 * <pre>
  int[] read( Indexer2 index, int[] src) {
    int[] dest = new int[index.getTotalNelems()];
    while (index.hasNext()) {
      Indexer2.Chunk chunk = index.next();
      System.arraycopy(src, chunk.getSrcElem(), dest, chunk.getDestElem(), chunk.getNelems());
    }
    return dest;
  }

  int[] read( Indexer2 index, RandomAccessFile raf, long start_pos) {
    int[] dest = new int[index.getTotalNelems()];
    while (index.hasNext()) {
      Indexer2.Chunk chunk = index.next();
      raf.seek( start_pos + chunk.getSrcElem() * 4);
      raf.readInt(dest, chunk.getDestElem(), chunk.getNelems());
    }
    return dest;
  }

  void write( Indexer2 index, int[] src, RandomAccessFile raf, long start_pos) {
    while (index.hasNext()) {
      Indexer2.Chunk chunk = index.next();
      raf.seek( start_pos + chunk.getDestElem() * 4);
      raf.writeInt(src, chunk.getSrcElem(), chunk.getNelems());
    }
  }
 * </pre>
 *
 * @author caron
 * @since Jan 2, 2008
 */
public class Indexer2 {
  private List<Dim> dimList = new ArrayList<Dim>();
  private Index myIndex;

  private Chunk chunk; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private long start, total, done;

  private boolean debug = true, debugMerge = true, debugNext = false;

  public Indexer2(int[] varShape, Section wantSection) throws InvalidRangeException {

    wantSection = Section.fill(wantSection, varShape); // will throw InvalidRangeException if illegal section

    // compute total size of wanted section
    this.total = wantSection.computeSize();
    this.done = 0;

    // compute the layout

    // create the List<Dim>
    int varRank = varShape.length;
    int stride = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      dimList.add(new Dim(stride, varShape[ii], wantSection.getRange(ii))); // note reversed : fastest first
      stride *= varShape[ii];
    }

    // merge contiguous inner dimensions for efficiency
    if (debugMerge) System.out.println("merge= " + this);

    // count how many merge dimensions
    int merge = 0;
    for (int i = 0; i < dimList.size()-1; i++) {
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
    if ((varRank == 0) || (dimList.get(0).want.stride() > 1))
      this.nelems = 1;
    else {
      Dim innerDim = dimList.get(0);
      this.nelems = innerDim.wantSize;
      innerDim.wantSize = 1; // inner dimension has one element of length nelems
    }

    start = 0; // first wanted value
    for (Dim dim : dimList) {
      start += dim.stride * dim.want.first();
    }

    // we will use an Index object to keep track of the chunks, each index represents nelems
    int rank = dimList.size();
    int[] wstride = new int[rank];
    int[] shape = new int[rank];
    for (int i = 0; i < rank; i++) {
      Dim dim = dimList.get(i);
      wstride[rank - i - 1] = dim.stride * dim.want.stride(); // reverse to slowest first
      shape[rank - i - 1] = dim.wantSize;
    }
    if (debug) {
      System.out.println("  elemsPerChunk=" + nelems+ "  nchunks=" + Index.computeSize(shape));
      printa("  indexShape=", shape);
      printa("  indexStride=", wstride);
    }
    myIndex = new Index(shape, wstride);

    // sanity check
    assert Index.computeSize(shape) * nelems == total;

    if (debug) {
      System.out.println("Index2= " + this);
      System.out.println(" start= " + start + " varShape= " + printa(varShape) + " wantSection= " + wantSection);
    }
  }

  private class Dim {
    int stride;    // number of elements
    int maxSize;   // number of elements
    Range want;    // desired Range
    int wantSize;  // keep seperate from want so we can modify when merging

    Dim(int byteStride, int maxSize, Range want) {
      this.stride = byteStride;
      this.maxSize = maxSize;
      this.wantSize = want.length();
      this.want = want;
    }
  }

  public long getTotalNelems() {
    return total;
  }

  public boolean hasNext() {
    return done < total;
  }

  public Chunk next() {
    if (chunk == null) {
      chunk = new Chunk(start, nelems, 0);
    } else {
      myIndex.incr(); // increment one element, but it represents one chunk = nelems * sizeElem
      chunk.incrDestElem(nelems); // always read nelems at a time
    }

    // Get the current element's byte index from the start of the file
    chunk.setSrcElem(start + myIndex.currentElement());

    if (debugNext)
      System.out.println(" next chunk: " + chunk);

    done += nelems;
    return chunk;
  }


  /**
   * A chunk of data that is contiguous in both the source and destination.
   * Read nelems from src at filePos, store in destination at startElem.
   * (or) Write nelems to file at filePos, from array at startElem.
   */
  public class Chunk {
    private long srcElem;   // start reading/writing here in the file
    private int nelems;     // read these many contiguous elements
    private long destElem; // start writing/reading here in array

    public Chunk(long srcElem, int nelems, long destElem) {
      this.srcElem = srcElem;
      this.nelems = nelems;
      this.destElem = destElem;
    }

    /**
     * Get the position in source where to read or write
     * @return position as an element count
     */
    public long getSrcElem() {
      return srcElem;
    }

    public void setSrcElem(long srcElem) {
      this.srcElem = srcElem;
    }

    public void incrSrcElem(int incr) {
      this.srcElem += incr;
    }

    /**
     * @return number of elements to transfer contiguously (Note: elements, not bytes)
     */
    public int getNelems() {
      return nelems;
    }

    public void setNelems(int nelems) {
      this.nelems = nelems;
    }

    /**
     * Get the position in destination where to read or write
     * @return starting element in the array: "starting array element" (Note: elements, not bytes)
     */
    public long getDestElem() {
      return destElem;
    }

    public void setDestElem(long destElem) {
      this.destElem = destElem;
    }

    public void incrDestElem(int incr) {
      this.destElem += incr;
    }

    public String toString() {
      return " srcElem=" + srcElem + " nelems=" + nelems + " destElem=" + destElem;
    }
  }

  // debugging
  protected String printa(int[] a) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < a.length; i++) sbuff.append(a[i] + " ");
    return sbuff.toString();
  }

  protected void printa(String name, int[] a) {
    System.out.print(name + "= ");
    for (int i = 0; i < a.length; i++) System.out.print(a[i] + " ");
    System.out.println();
  }

  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("wantSize=");
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(elem.wantSize);
    }
    sbuff.append(" maxSize=");
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(elem.maxSize);
    }
    sbuff.append(" wantStride=");
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(elem.want.stride());
    }
    sbuff.append(" stride=");
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(elem.stride);
    }
    return sbuff.toString();
  }

}
