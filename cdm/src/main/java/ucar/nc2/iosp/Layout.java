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
 * Iterator to read/write subsets of a multidimensional array, finding the contiguous chunks.
 * The iteration is monotonic in both src and dest positions.
 * <p/>
 * Example for Integers:
 * <pre>
  int[] read( Layout index, int[] src) {
    int[] dest = new int[index.getTotalNelems()];
    while (index.hasNext()) {
      Layout.Chunk chunk = index.next();
      System.arraycopy(src, chunk.getSrcElem(), dest, chunk.getDestElem(), chunk.getNelems());
    }
    return dest;
  }

  int[] read( Layout index, RandomAccessFile raf) {
    int[] dest = new int[index.getTotalNelems()];
    while (index.hasNext()) {
      Layout.Chunk chunk = index.next();
      raf.seek( chunk.getSrcPos());
      raf.readInt(dest, chunk.getDestElem(), chunk.getNelems());
    }
    return dest;
  }

   // note src and dest misnamed
    void write( Layout index, int[] src, RandomAccessFile raf) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek ( chunk.getSrcPos());
        for (int k=0; k<chunk.getNelems(); k++)
          raf.writeInt(src, chunk.getDestElem(), chunk.getNelems());
          raf.writeInt( ii.getByteNext());
      }

 * </pre>
 *
 * @author caron
 * @since Jan 2, 2008
 */

public interface Layout {

  /**
   * Get total number of elements in the wanted subset.
   *
   * @return total number of elements in the wanted subset.
   */
  public long getTotalNelems();

  /**
   * Get size of each element in bytes.
   *
   * @return size of each element in bytes.
   */
  public int getElemSize();

  /**
   * Is there more to do
   *
   * @return true if theres more to do
   */
  public boolean hasNext();

  /**
   * Get the next chunk
   *
   * @return next chunk, or null if !hasNext()
   * @throws java.io.IOException on i/o error
   */
  public Chunk next() throws IOException;

  /**
   * A chunk of data that is contiguous in both the source and destination.
   * Read nelems from src at filePos, store in destination at startElem.
   * (or) Write nelems to file at filePos, from array at startElem.
   */
  public interface Chunk {

    /**
     * Get the position in source where to read or write: "file position"
     * @return position as a byte count into the source, eg a file
     */
    public long getSrcPos();

    /**
     * Get number of elements to transfer contiguously (Note: elements, not bytes)
     * @return number of elements to transfer
     */
    public int getNelems();

    /**
     * Get starting element position as a 1D element index into the destination, eg the requested array with shape "wantSection".
     * @return starting element in the array (Note: elements, not bytes)
     */
    public long getDestElem();
  }
}
