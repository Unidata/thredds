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

/**
 * Iterator to read/write subsets of an array.
 * <p>
 * Example for Integers:
 * <pre>
 * Reading:
     int[] pa = new int[size];
     Indexer index;
     while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.readInt( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }

   Writing:
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeInt( ii.getIntNext());
      }

   </pre>
 * @author caron
 */
public abstract class Indexer {

  /** @return total number of elements in the wanted subset. */
  public abstract long getTotalNelems();

  /** @return  size of each element in bytes. */
  public abstract int getElemSize();

  /** @return true if theres more to do */
  public abstract boolean hasNext();

  /** @return next chunk */
  public abstract Chunk next();

  /** A contiguous chunk of data in the file, that is wanted for this subset.
   *  Read nelems from file at filePos, store in array at indexPos.
   *  (or) Write nelems to file at filePos, from array at indexPos.
   */
  public class Chunk {
    private long filePos; // start reading here
    private int nelems; // read these many elements
    private int indexPos; // put them here in the result array

    public Chunk( long filePos, int nelems, int indexPos) {
      this.filePos = filePos;
      this.nelems = nelems;
      this.indexPos = indexPos;
    }

    /** @return position in file where to read or write: "file position" */
    public long getFilePos() { return filePos; }
    public void setFilePos(long filePos) { this.filePos = filePos; }
    public void incrFilePos(int incr) { this.filePos += incr; }

    /** @return number of elements to transfer (Note: elements, not bytes) */
    public int getNelems() { return nelems; }
    public void setNelems(int nelems) { this.nelems = nelems; }

    /** @return position in the output Array: "memory position" */
    public int getIndexPos() { return indexPos; }
    public void setIndexPos(int indexPos) { this.indexPos = indexPos; }
    public void incrIndexPos(int incr) { this.indexPos += incr; }

    public String toString() { return " filePos="+filePos+" nelems="+nelems+" indexPos="+indexPos; }
  }

  // used to keep track of which element we are on
  // need access to protected methods
  protected class MyIndex extends ucar.ma2.Index {
    MyIndex(int[] shape, int[] stride) {
      super(shape, stride);
    }

    protected int incr() {
      return super.incr();
    }
  }

}
