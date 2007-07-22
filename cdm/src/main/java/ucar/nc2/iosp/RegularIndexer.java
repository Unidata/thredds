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

import ucar.ma2.Index;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

import java.util.*;

/**
 * Indexer into a regular array.
 * This calculates byte lengths and offsets of the wanted data into the entire data.
 * Assumes that the data is stored "regularly", like netcdf arrays and hdf5 continuous storage.
 * Also handles netcdf3 record dimensions.
 *
 * @author caron
 * @deprecated use RegularLayout
 */
public class RegularIndexer extends Indexer {
  private int elemSize; // size of each element
  private long startPos; // starting address

  // calculated
  private int [] wantShape; // for N3iosp

  private FileIndex index; // file pos tracker
  private Chunk chunk; // gets returned on next().
  private int nelems; // number of elements to read at one time
  private int total, done;

  private boolean debug = false, debugNext = false;

  /**
   * Constructor.
   * @param varShape shape of the entire data array.
   * @param elemSize size of on element in bytes.
   * @param startPos starting address of the entire data array.
   * @param section the wanted section of data: List of Range objects,
   *    corresponding to each Dimension, else null means all.
   * @param recSize if > 0, then size of outer stride in bytes, else ignored
   * @throws InvalidRangeException is ranges are misformed
   */
  public RegularIndexer( int[] varShape, int elemSize, long startPos, List section, int recSize) throws InvalidRangeException {
    this.elemSize = elemSize;
    this.startPos = startPos;

    boolean isRecord = recSize > 0;

    // construct wantOrigin, wantShape, wantStride from section and defaults
    int varRank = varShape.length;
    int[] wantOrigin = new int[ varRank];
    int[] wantStride = new int[ varRank];
    for (int ii = 0; ii < varRank; ii++) wantStride[ii] = 1;

    if (section == null) {
      wantShape = new int[varShape.length];  // optimization over clone()
      System.arraycopy(varShape, 0, wantShape, 0, varShape.length);

    } else {
      // check ranges are valid
      if (section.size() != varRank)
        throw new InvalidRangeException("Bad section rank");
      wantShape = new int[ varRank];

      for (int ii = 0; ii < varRank; ii++) {
        Range r = (Range) section.get(ii);
        if (r == null) {
          wantShape[ii] = varShape[ii]; // all in this range

        } else {
          if (r.last() >= varShape[ii])
            throw new InvalidRangeException("Bad range for dimension "+ii+" = "+r.last());
          wantOrigin[ii] = r.first();
          wantShape[ii] = r.length();
          wantStride[ii] = r.stride();
        }
      }
    }

    // compute total size of wanted section
    this.total = (int) Index.computeSize( wantShape);
    this.done = 0;

    // deal with nonzero wantOrigin : need strides before userStrides are included
    int[] stride = new int[ varRank];
    int product = 1;
    for (int ii = varRank-1; ii >= 0; ii--) {
      stride[ii] = product;
      product *= varShape[ii];
    }
    long offset = 0;
    for (int ii = 0; ii < varRank; ii++) {
      long realStride = (isRecord && ii == 0) ? recSize : elemSize * stride[ii];
      long pos = realStride * wantOrigin[ii];
      offset += pos;
    }

    // merge contiguous inner dimensions for efficiency
    int[] mergeShape = new int[wantShape.length];  // cant munge wantShape : optimization over clone()
    System.arraycopy(wantShape, 0, mergeShape, 0, wantShape.length);
    int rank = varRank;
    int lastDim = isRecord ? 2 : 1; // cant merge record dimension
    while ((rank > lastDim) && (varShape[rank-1] == wantShape[rank-1]) && (wantStride[rank-2] == 1)) {
      mergeShape[rank-2] *= mergeShape[rank-1];
      rank--;
    }

    // how many elements at a time?
    if ((rank==0) || (isRecord && rank == 1) || (wantStride[rank-1] > 1))
      this.nelems = 1;
    else {
      this.nelems = mergeShape[rank - 1];
      mergeShape[rank - 1] = 1;
    }

    // compute final strides : include user stride
    int[] finalStride = new int[ varRank];
    product = 1;
    for (int ii = varRank-1; ii >= 0; ii--) {
      finalStride[ii] = product * wantStride[ii];
      product *= varShape[ii];
    }

    // make stride into bytes instead of elements
    for (int ii = 0; ii < rank; ii++)
      finalStride[ii] *= elemSize;
    if (isRecord && (rank > 0))
      finalStride[0] = recSize * wantStride[0];

    int[] finalShape = new int[ rank];
    for (int ii=0; ii<rank;ii++) finalShape[ii] = mergeShape[ii];

    index = new FileIndex( offset, finalShape, finalStride);

    if (debug) {
      System.out.println("*************************");
      printa(" varShape", varShape);
      printa(" wantOrigin", wantOrigin);
      printa(" wantShape", wantShape);
      printa(" wantStride", wantStride);

      printa(" shape", finalShape, rank);
      printa(" stride", finalStride, rank);
      System.out.println("offset= "+offset);

      System.out.println("total= "+total);
      System.out.println("nelems= "+nelems);
      System.out.println("rank= "+rank+" varRank= "+varRank);
      System.out.println("isRecord= "+isRecord);
    }
  }

  private void printa( String name, int[] a, int rank) {
    System.out.print(name+"= ");
    for (int i=0;i<rank; i++) System.out.print(a[i]+" ");
    System.out.println();
  }

  public int getChunkSize() { return nelems; }              // debug
  public int[] getWantShape() { return wantShape; }  // for N3iosp

  // Indexer abstract methods
  public long getTotalNelems() { return total; }
  public int getElemSize() { return elemSize; }
  public boolean hasNext() { return done < total; }

  public Chunk next() {

    if (chunk == null) {
      chunk = new Chunk(startPos, nelems, 0);

    } else {
      index.incr(); // increment file position
      chunk.incrStartElem(nelems); // always read nelems at a time
    }

    // Get the current element's byte index from the start
    chunk.setFilePos(startPos + index.currentPos());

    if (debugNext) {
      printa("-- current index= ", index.current);
      System.out.println(" pos= " + index.currentPos());
      System.out.println(" next chunk = " + chunk);
    }

    done += nelems;
    //if (debugNext) System.out.println(" done = "+done+" total = "+total);
    return chunk;
  }

  private class FileIndex {
    long startPos;
    int[] shape, stride, origin, current;
    int rank;

    FileIndex( long startPos, int[] shape, int[] stride) {
      this.startPos = startPos;
      this.shape = shape;
      this.stride = stride;
      this.rank = shape.length;
      this.current = new int[ rank];
    }

    void incr() {
      int digit = rank-1;
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
      for(int ii = 0; ii < rank; ii++)
        value += current[ii] * ((long) stride[ii]);
      return value;
    }

  }



}