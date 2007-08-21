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

import java.io.IOException;

/**
 * Iterator to read/write subsets of an array.
 * The transfer is broken into "chunks" that are contiguous in both the source and destination.
 * The sum over all the chunks should give the the wanted section. However, there may be
 * missing values, so the caller cant count on all elements getting touched.
 *
 * <p>
 * Example for Integers:
 * <pre>
 * Reading:
     int[] pa = new int[size];
     Indexer index;
     while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        raf.seek ( chunk.getFilePos());
        raf.readInt( pa, chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
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

  /**
   * Get the next chunk
   * @return next chunk, or null if !hasNext()
   * @throws java.io.IOException on i/o error
   */
  public abstract Chunk next() throws IOException;

  /** A chunk of data that is contiguous in both the source and destination.
   *  Read nelems from src at filePos, store in destination at startElem.
   *  (or) Write nelems to file at filePos, from array at startElem.
   */
  public class Chunk {
    private long filePos;   // start reading/writing here in the file
    private int nelems;     // read these many contiguous elements
    private long startElem; // start writing/reading here in array

    public Chunk( long filePos, int nelems, long startElem) {
      this.filePos = filePos;
      this.nelems = nelems;
      this.startElem = startElem;
    }

    /**
     * Get the position in source where to read or write: "file position"
     * @return position as a byte count
     */
    public long getFilePos() { return filePos; }
    public void setFilePos(long filePos) { this.filePos = filePos; }
    public void incrFilePos(int incr) { this.filePos += incr; }

    /** @return number of elements to transfer contiguously (Note: elements, not bytes) */
    public int getNelems() { return nelems; }
    public void setNelems(int nelems) { this.nelems = nelems; }

    /**
     * Get starting element position as a 1D element index in the array with shape "wantSection". 
     * @return starting element in the array: "starting array element" (Note: elements, not bytes)
     */
    public long getStartElem() { return startElem; }
    public void setStartElem(long startElem) { this.startElem = startElem; }
    public void incrStartElem(int incr) { this.startElem += incr; }

    public String toString() { return " filePos="+ filePos +" nelems="+nelems+" startElem="+ startElem; }
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

}
