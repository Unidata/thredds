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
package ucar.unidata.io;

import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * A RandomAccessFile stored entirely in memory as a byte array.
 * @author john
 */
public class InMemoryRandomAccessFile extends RandomAccessFile {

  /**
   * Constructor for in-memory "files"
   *
   * @param name used as a name
   * @param data     the complete file
   */
  public InMemoryRandomAccessFile(String name, byte[] data) {
    super(1);
    this.location = name;
    this.file = null;
    if (data == null)
      throw new IllegalArgumentException("data array is null");

    buffer = data;
    bufferStart = 0;
    dataSize = buffer.length;
    dataEnd = buffer.length;
    filePosition = 0;
    endOfFile = false;

    if (debugLeaks)
      openFiles.add(location);
  }

  public long length() {
    return dataEnd;
  }

  @Override
  protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
    len = Math.min(len, (int) (buffer.length - pos));
    // copy out of buffer
    System.arraycopy(buffer, (int) pos, b, offset, len);
    return len;
  }

  /**
   * Read up to <code>nbytes</code> bytes, at a specified offset, send to a WritableByteChannel.
   * This will block until all bytes are read.
   * This uses the underlying file channel directly, bypassing all user buffers.
   *
   * @param dest   write to this WritableByteChannel.
   * @param offset the offset in the file where copying will start.
   * @param nbytes the number of bytes to read.
   * @return the actual number of bytes read, or -1 if there is no
   *         more data due to the end of the file being reached.
   * @throws java.io.IOException if an I/O error occurs.
   */
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    return dest.write(ByteBuffer.wrap(buffer, (int) offset, (int) nbytes));
  }

}