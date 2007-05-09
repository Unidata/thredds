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
 * <p>
 * Example for Integers:
 * <pre>
 *    int[] pa = new int[size];
 *    Indexer index;
 *    while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.readInt( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }

      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next(); // LOOK not using chunk.getIndexPos()
        raf.seek ( chunk.getFilePos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeInt( ii.getIntNext());
      }

   </pre>
 * @author caron
 */
abstract class Indexer {

  /** @return total number of elements in the wanted subset. */ // LOOK change to long ??
  abstract int getTotalNelems();

  /** @return  size of each element in bytes. */
  abstract int getElemSize();

  /** @return true if theres more to do */
  abstract boolean hasNext();

  /** @return next chunk */
  abstract Chunk next();

  /** A contiguous chunk of data in the file, that is wanted for this subset.
   *  Read nelems from file at filePos, store in array at indexPos.
   *  (or) Write nelems to file at filePos, from array at indexPos.
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

    /** @return position in file where to read or write: "file position" */
    public long getFilePos() { return filePos; }
    /** @return number of elements to transfer (Note: elements, not bytes) */
    public int getNelems() { return nelems; }
    /** @return position in the memory array: "memory position" */
    public int getIndexPos() { return indexPos; }

    public String toString() { return " filePos="+filePos+" nelems="+nelems+" indexPos="+indexPos; }
  }

}
