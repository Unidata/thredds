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

public interface LayoutBB extends Layout {

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
  public interface Chunk extends Layout.Chunk {

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
    public long getDestElem();
  }
}
