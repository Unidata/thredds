/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.unidata.io.hdfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;


/**
 * @author James McClain, based on work by John Caron and Donald Denbo
 */

public class HDFSRandomAccessFile extends ucar.unidata.io.RandomAccessFile {
  static public final int defaultHDFSBufferSize = 1<<12;

  private Configuration conf = null;
  private Path path = null;
  private FileSystem fs = null;
  private FSDataInputStream inputStream = null;


  public HDFSRandomAccessFile(String url) throws IOException {
    this(url, defaultHDFSBufferSize);
  }

  public HDFSRandomAccessFile(String url, int bufferSize) throws IOException {
    super(bufferSize);
    file = null;
    location = url;

    conf = new Configuration();
    path = new Path(url);
    fs = FileSystem.get(path.toUri(), conf);
    inputStream = fs.open(path);
  }

  public void close() throws IOException {
    inputStream.close();
    fs.close();
  }

  /**
   * Read directly from HDFS, without going through the buffer.
   * All reading goes through here or readToByteChannel;
   *
   * @param pos    start here in the file
   * @param buff   put data into this buffer
   * @param offset buffer offset
   * @param len    this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  @Override
  protected int read_(long pos, byte[] buff, int offset, int len) throws IOException {
    int bytes = 0;
    int totalBytes = 0;

    bytes = inputStream.read(pos, buff, offset + totalBytes, len - totalBytes);
    while ((bytes > 0) && ((len - totalBytes) > 0)) {
        totalBytes += bytes;
        bytes = inputStream.read(pos, buff, offset + totalBytes, len - totalBytes);
    }

    return totalBytes;
  }

  @Override
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    int n = (int) nbytes;
    byte[] buff = new byte[n];
    int done = read_(offset, buff, 0, n);
    dest.write(ByteBuffer.wrap(buff));
    return done;
  }

  @Override
  public long length() throws IOException {
    return fs.getFileStatus(path).getLen();
  }

  @Override
  public long getLastModified() {
    long time;

    try {
        time = fs.getFileStatus(path).getModificationTime();
    }
    catch (IOException e) {
        time = 0;
    }

    return time;
  }
}
