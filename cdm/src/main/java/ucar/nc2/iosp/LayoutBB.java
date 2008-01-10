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
import java.nio.*;

/**
 * A Layout that supplies the "source" ByteBuffer.
 * This is used when the data must be massaged after being read, eg uncompresed or filtered.
 * The modified data is placed in a ByteBuffer, which may change for different chunks, and
 * so is supplied by each chunk.
 * 
 * <p/>
 * Example for Integers:
 * <pre>

  int[] read( LayoutBB index, int[] pa) {
      while (index.hasNext()) {
        LayoutBB.Chunk chunk = index.next();
        IntBuffer buff = chunk.getIntBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;
  }

 * </pre>
 *
 * @author caron
 * @since Jan 9, 2008
 */

public interface LayoutBB {

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
   * Read nelems from ByteBuffer at filePos, store in destination at startElem.
   */
  public interface Chunk {

    /**
     * Get the position in source <Type>Buffer where to read or write: "file position"
     * @return position as a element index into the <Type>Buffer
     */
    public int getSrcElem();

    public ByteBuffer getByteBuffer();
    public ShortBuffer getShortBuffer();
    public IntBuffer getIntBuffer();
    public FloatBuffer getFloatBuffer();
    public DoubleBuffer getDoubleBuffer();
    public LongBuffer getLongBuffer();

    /**
     * Get number of elements to transfer contiguously (Note: elements, not bytes)
     *
     * @return number of elements to transfer
     */
    public int getNelems();

    /**
     * Get starting element position as a 1D element index into the destination, eg the requested array with shape "wantSection".
     *
     * @return starting element in the array (Note: elements, not bytes)
     */
    public int getDestElem();
  }
}
