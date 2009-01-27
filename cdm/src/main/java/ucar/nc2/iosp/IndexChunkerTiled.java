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
 * Assume that the data is stored divided into sections, described by dataSection. All the data within a dataSection is
 * stored contiguously, in a regular layout. Assume dataSection strides must be = 1, that is, the stored data is not strided.
 * <p/>
 * The user asks for some section, wantSection (may have strides).
 * For each dataSection that intersects wantSection, a IndexChunkerTiled is created, which
 * figures out the optimal access pattern, based on reading contiguous runs of data. Each
 * IndexChunkerTiled handles only one dataSection. Typically the cllaing program loops over
 * all dataSections that intersect the wanted section.
 * <p/>
 * Both dataSection and wantSection refer to the variable's overall shape.
 *
 * @author caron
 * @since Jan 9, 2008
 */
public class IndexChunkerTiled {
  private List<Dim> dimList = new ArrayList<Dim>();
  private Index dataIndex;    // Index into the data source section - used to calculate chunk.filePos
  private Index resultIndex; // Index into the data result section - used to calculate chunk.startElem

  private IndexChunker.Chunk chunk; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private long total, done;
  private int startDestElem; // the offset in the result Array of this piece of it
  private int startSrcElem; // the offset in the source Array of this piece of it

  private boolean debug = false, debugMerge = false, debugDetail = false, debugNext = false, debugStartingElems = false;

  /**
   * Constructor.
   * Assume varSection.intersects(wantSection).
   *
   * @param dataSection  the section of data we actually have. must have all ranges with stride = 1.
   * @param wantSection  the wanted section of data, it will be intersected with dataSection.
   *   dataSection.intersects(wantSection) must be true
   * @throws InvalidRangeException if ranges are malformed
   */
  public IndexChunkerTiled(Section dataSection, Section wantSection) throws InvalidRangeException {
    this.done = 0;

    // LOOK - need test for "all" common case

    // The actual wanted data we can get from this section
    Section intersect = dataSection.intersect(wantSection);
    this.total = intersect.computeSize();
    if (total <= 0) {
      System.out.println("hey");
    }
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

    /* the origin can be handled by adding to the startPos
     long fileOffset = 0; // offset in file
     for (Dim dim : dimList) {
       int d = dim.intersect.first() - dim.data.first();
       if (d > 0) fileOffset += elemSize * dim.dataStride * d;
     }
     this.startPos = startFilePos + fileOffset; */

    // the offset in the result Array of this piece of it
    startDestElem = wantSection.offset( intersect);
    startSrcElem = dataSection.offset( intersect);
    if (debugStartingElems)
      System.out.println(" startDestElem="+ startDestElem + " startSrcElem="+ startSrcElem);

     /* for (Dim dim : dimList) {
       int d = dim.intersect.first() - dim.want.first();
       if (d > 0)  startElem += dim.wantStride * d;
     } */


    // LOOK : not merging inner dimensions
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

     // we will use Index objects to keep track of the chunks
    int rank = dimList.size();
    int[] dataStrides = new int[rank];
    int[] resultStrides = new int[rank];
    int[] shape = new int[rank];
    for (int i = 0; i < dimList.size(); i++) {  // reverse to slowest first
      Dim dim = dimList.get(i);
      dataStrides[rank - i - 1] = dim.dataStride * dim.want.stride();
      resultStrides[rank - i - 1] = dim.wantStride; // * dim.want.stride();
      shape[rank - i - 1] = dim.wantNelems;
    }
    if (debugDetail) {
      printa(" indexShape=", shape);
      printa(" dataStrides=", dataStrides);
      printa(" wantStride=", resultStrides);
      System.out.println(" indexChunks=" + Index.computeSize(shape));
    }
    dataIndex = new Index(shape, dataStrides);
    resultIndex = new Index(shape, resultStrides);

    if (debugDetail) {
      System.out.println(" dataIndex="+ dataIndex.toStringDebug());
      System.out.println(" resultIndex="+ resultIndex.toStringDebug());
    }

    // sanity checks
    long nchunks = Index.computeSize(shape);
    assert nchunks * nelems == total;

    if (debug) {
      System.out.println("RegularSectionLayout total = "+total+" nchunks= "+nchunks+" nelems= "+nelems+
          " dataSection= " + dataSection + " wantSection= " + wantSection+ " intersect= " + intersect+ this);
    }
  }

  private class Dim {
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

      if (debugMerge) System.out.println("Dim="+this);
    }

    public String toString() {
      return "  data = "+data+ " want = "+want+ " intersect = "+intersect+ " ncontigElements = "+ncontigElements;
    }
  } // Dim

  // Indexer methods
  public long getTotalNelems() {
    return total;
  }

  public boolean hasNext() {
    return done < total;
  }

  public IndexChunker.Chunk next() {
    if (chunk == null) {
      chunk = new IndexChunker.Chunk(0, nelems, startDestElem);
    } else {
      dataIndex.incr();
      resultIndex.incr();
    }

    // Set the current element's index from the start of the data array
    chunk.setSrcElem(startSrcElem + dataIndex.currentElement());

    // Set the current element's index from the start of the result array
    chunk.setDestElem(startDestElem + resultIndex.currentElement());

    if (debugNext)
      System.out.println(" chunk: " + chunk);
    if (debugDetail) {
      System.out.println(" dataIndex: " + dataIndex);
      System.out.println(" wantIndex: " + resultIndex);
    }

    done += nelems;
    return chunk;
  }

  ////////////////////

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < dimList.size(); i++) {
      Dim elem = dimList.get(i);
      sbuff.append("\n");
      sbuff.append(elem);
    }
    return sbuff.toString();
  }

  private void printa(String name, int[] a) {
    System.out.print(name + "= ");
    for (int i = 0; i < a.length; i++) System.out.print(a[i] + " ");
    System.out.println();
  }

}
