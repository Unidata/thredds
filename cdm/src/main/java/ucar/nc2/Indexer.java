// $Id:Indexer.java 51 2006-07-12 17:13:13Z caron $
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
 *    int[] pa = new int[size];
 *    Indexer index;
 *    while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.readInt( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
   </pre>
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
abstract class Indexer {

  /** @return total number of elements in the wanted subset. */
  abstract int getTotalNelems();

  /** @return  size of each element in bytes. */
  abstract int getElemSize();

  /** @return true if theres more to do */
  abstract boolean hasNext();

  /** @return next chunk */
  abstract Chunk next();

  /** A contiguous chunk of data in the file, that is wanted for this subset.
   *  Read nelems from file at filePos, store in array at indexPos.
   */
  class Chunk {
    long filePos; // start reading here
    int nelems; // read these many elements
    int indexPos; // put them here in the result array

    Chunk( long filePos, int nelems, int indexPos) {
      this.filePos = filePos;
      this.nelems = nelems;
      this.indexPos = indexPos;
    }

    /** @return position in file where to start reading: "source position" */
    public long getFilePos() { return filePos; }
    /** @return number of elements to read/transfer */
    public int getNelems() { return nelems; }
    /** @return where to place in the result array: "destination position" */
    public int getIndexPos() { return indexPos; }

    public String toString() { return " filePos="+filePos+" nelems="+nelems+" indexPos="+indexPos; }
  }

}
