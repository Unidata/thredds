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

  protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
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