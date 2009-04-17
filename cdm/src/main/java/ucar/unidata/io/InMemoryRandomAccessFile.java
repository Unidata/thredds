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
public class InMemoryRandomAccessFile extends ucar.unidata.io.RandomAccessFile {

  /**
   * A RandomAccessFile stored entirely in memory as a byte array.
   *
   * @param name used as the location
   * @param data the complete data file
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

  @Override
  public long length() {
    return dataEnd;
  }

  // @Override  LOOK weird error
  public void setBufferSize(int bufferSize) {
    ; // do nothing
  }

  @Override
  protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
    len = Math.min(len, (int) (buffer.length - pos));
    // copy out of buffer
    System.arraycopy(buffer, (int) pos, b, offset, len);
    return len;
  }

  @Override
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    return dest.write(ByteBuffer.wrap(buffer, (int) offset, (int) nbytes));
  }

}