// $Id$
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
package ucar.nc2;

/**
 * Iterator to read/write subsets of an array.
 * <pre>
 *
 *    Indexer index;
 *    while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.read( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
   </pre>
 * @author caron
 * @version $Revision$ $Date$
 */
abstract class Indexer {

  /** total number of elements in the wanted subset. */
  abstract int getTotalNelems();

  /**  size of each element in bytes. */
  abstract int getElemSize();

  /** if theres more to do */
  abstract boolean hasNext();

  /** get next chunk */
  abstract Chunk next();

  /** a contiguous chunk of data in the file, that is wanted for this subset. */
  class Chunk {
    long filePos; // start reading here
    int nelems; // read these many elements
    int indexPos; // put them here in the result array

    Chunk( long filePos, int nelems, int indexPos) {
      this.filePos = filePos;
      this.nelems = nelems;
      this.indexPos = indexPos;
    }

    /** where to read in the file "source position" */
    public long getFilePos() { return filePos; }
    /** number of elements to read/transfer */
    public int getNelems() { return nelems; }
    /** where to place in the result array "destination position" */
    public int getIndexPos() { return indexPos; }
    /** for debugging */
    public String toString() { return " filePos="+filePos+" nelems="+nelems+" indexPos="+indexPos; }
  }

  class FileIndex {
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
