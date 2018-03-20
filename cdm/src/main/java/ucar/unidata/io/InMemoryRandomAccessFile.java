/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
    // do nothing
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