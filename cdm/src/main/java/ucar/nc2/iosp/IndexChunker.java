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
import ucar.ma2.Index;
import ucar.ma2.Range;

import java.util.List;
import java.util.ArrayList;

/**
 * Iterator to read/write subsets of a multidimensional array, finding the contiguous chunks.
 * The iteration is monotonic in both src and dest positions.
 *
 * <p/>
 * Example for Integers:
 * <pre>
  int[] read( IndexChunker index, int[] src) {
    int[] dest = new int[index.getTotalNelems()];
    while (index.hasNext()) {
      Indexer2.Chunk chunk = index.next();
      System.arraycopy(src, chunk.getSrcElem(), dest, chunk.getDestElem(), chunk.getNelems());
    }
    return dest;
  }

  int[] read( IndexChunker index, RandomAccessFile raf, long start_pos) {
    int[] dest = new int[index.getTotalNelems()];
    while (index.hasNext()) {
      Indexer2.Chunk chunk = index.next();
      raf.seek( start_pos + chunk.getSrcElem() * 4);
      raf.readInt(dest, chunk.getDestElem(), chunk.getNelems());
    }
    return dest;
  }

 // note src and dest misnamed
  void write( IndexChunker index, int[] src, RandomAccessFile raf, long start_pos) {
    while (index.hasNext()) {
      Indexer2.Chunk chunk = index.next();
      raf.seek( start_pos + chunk.getSrcElem() * 4);
      raf.writeInt(src, chunk.getDestElem(), chunk.getNelems());
    }
  }
 * </pre>
 *
 * @author caron
 * @since Jan 2, 2008
 */
public class IndexChunker {
  private static final boolean debug = false, debugMerge = false, debugNext = false;

  private List<Dim> dimList = new ArrayList<Dim>();
  private Index chunkIndex; // each element is one chunk; strides track position in source

  private Chunk chunk; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private long start, total, done;


  /**
   * Constructor
   * @param srcShape the shape of the source, eg Variable.getSHape()
   * @param wantSection the wanted section in srcShape, ie must be sibset of srcShape.
   * @throws InvalidRangeException if wantSection is incorrect
   */
  public IndexChunker(int[] srcShape, Section wantSection) throws InvalidRangeException {

    wantSection = Section.fill(wantSection, srcShape); // will throw InvalidRangeException if illegal section

    // compute total size of wanted section
    this.total = wantSection.computeSize();
    this.done = 0;
    this.start = 0;

    // see if this is a "want all of it" single chunk
    if (wantSection.equivalent(srcShape)) {
      this.nelems = (int) this.total;
      chunkIndex = Index.factory(new int[] {1});
      return;
    }

    // create the List<Dim> tracking each dimension
    int varRank = srcShape.length;
    int stride = 1;
    for (int ii = varRank - 1; ii >= 0; ii--) {
      dimList.add(new Dim(stride, srcShape[ii], wantSection.getRange(ii))); // note reversed : fastest first
      stride *= srcShape[ii];
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
      if (elem2.wantSize < 0)
        throw new IllegalArgumentException("array size may not exceed 2^31");
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
      innerDim.wantSize = 1; // inner dimension has one element of length nelems (we dont actually need this here)
    }

    start = 0; // first wanted value
    for (Dim dim : dimList) {
      start += ((long) dim.stride) * dim.want.first();  // watch for overflow on large files
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
    chunkIndex = new Index(shape, wstride);

    // sanity check
    assert Index.computeSize(shape) * nelems == total;

    if (debug) {
      System.out.println("Index2= " + this);
      System.out.println(" start= " + start + " varShape= " + printa(srcShape) + " wantSection= " + wantSection);
    }
  }

  private class Dim {
    int stride;    // number of elements
    long maxSize;   // number of elements - must be a long since we may merge
    Range want;    // desired Range
    int wantSize;  // keep seperate from want so we can modify when merging

    Dim(int byteStride, int maxSize, Range want) {
      this.stride = byteStride;
      this.maxSize = maxSize;
      this.wantSize = want.length();
      this.want = want;
    }
  }

  /**
   * Get total number of elements in wantSection
   * @return total number of elements in wantSection
   */
  public long getTotalNelems() {
    return total;
  }

  /**
   * If there are  more chunks to process
   * @return true if there are  more chunks to process
   */
  public boolean hasNext() {
    return done < total;
  }

  /**
   * Get the next chunk
   * @return the next chunk
   */
  public Chunk next() {
    if (chunk == null) {
      chunk = new Chunk(start, nelems, 0);
    } else {
      chunkIndex.incr(); // increment one element, which represents one chunk = nelems * sizeElem
      chunk.incrDestElem(nelems); // always read nelems at a time
    }

    // Get the current element's  index from the start of the file
    chunk.setSrcElem(start + chunkIndex.currentElement());

    if (debugNext)
      System.out.println(" next chunk: " + chunk);

    done += nelems;
    return chunk;
  }


  /**
   * A chunk of data that is contiguous in both the source and destination.
   * Everything is done in elements, not bytes.
   * Read nelems from src at srcPos, store in destination at destPos.
   */
  static public class Chunk implements Layout.Chunk {
    private long srcElem;   // start reading/writing here in the file
    private int nelems;     // read these many contiguous elements
    private long destElem; // start writing/reading here in array
    private long srcPos;

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
      return  " srcPos=" + srcPos + " srcElem=" + srcElem + " nelems=" + nelems + " destElem=" + destElem;
    }

    // must be set by controlling Layout class - not used here
    public long getSrcPos() { return srcPos; }
    public void setSrcPos(long srcPos) {
      this.srcPos = srcPos;
    }
    public void incrSrcPos(int incr) { this.srcPos += incr; }
  }

  // debugging
  protected String printa(int[] a) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < a.length; i++) sbuff.append(a[i] + " ");
    return sbuff.toString();
  }

  protected void printa(String name, int[] a) {
    System.out.print(name + "= ");
    for (int i = 0; i < a.length; i++) System.out.print(a[i] + " ");
    System.out.println();
  }

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
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
