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

import java.io.IOException;

/**
 * Iterator to read/write subsets of an array.
 * The transfer is broken into "chunks" that are contiguous in both the source and destination.
 * Iterating over all the chunks should give the wanted section of data. However, there may be
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
 * @deprecated use Layout
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
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < a.length; i++) sbuff.append(a[i] + " ");
    return sbuff.toString();
  }

  protected void printa(String name, int[] a) {
    System.out.print(name + "= ");
    for (int i = 0; i < a.length; i++) System.out.print(a[i] + " ");
    System.out.println();
  }

}
