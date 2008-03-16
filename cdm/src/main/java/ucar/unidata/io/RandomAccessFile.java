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


import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.nio.channels.WritableByteChannel;


/**
 * RandomAccessFile.java.  By Russ Rew, based on
 * BufferedRandomAccessFile by Alex McManus, based on Sun's source code
 * for java.io.RandomAccessFile.  For Alex McManus version from which
 * this derives, see his <a href="http://www.aber.ac.uk/~agm/Java.html">
 * Freeware Java Classes</a>.
 * <p/>
 * A buffered drop-in replacement for java.io.RandomAccessFile.
 * Instances of this class realise substantial speed increases over
 * java.io.RandomAccessFile through the use of buffering. This is a
 * subclass of Object, as it was not possible to subclass
 * java.io.RandomAccessFile because many of the methods are
 * final. However, if it is necessary to use RandomAccessFile and
 * java.io.RandomAccessFile interchangeably, both classes implement the
 * DataInput and DataOutput interfaces.
 *
 * @author Alex McManus
 * @author Russ Rew
 * @author john caron
 * @see DataInput
 * @see DataOutput
 * @see java.io.RandomAccessFile
 */

public class RandomAccessFile implements DataInput, DataOutput {

  static public final int BIG_ENDIAN = 0;
  static public final int LITTLE_ENDIAN = 1;

  // debug leaks - keep track of open files

  /**
   * Debugging, do not use.
   * @return true if debugLeaks is on
   */
  static public boolean getDebugLeaks() {
    return debugLeaks;
  }

  /**
   * Debugging, do not use.
   * @param b set true to track java.io.RandomAccessFile
   */
  static public void setDebugLeaks(boolean b) {
    debugLeaks = b;
  }

  /**
   * Debugging, do not use.
   * @param b to debug file reading
   */
  static public void setDebugAccess(boolean b) {
    debugAccess = b;
  }

  /**
   * Debugging, do not use.
   * @return list of open files.
   */
  static public List<String> getOpenFiles() {
    return openFiles;
  }

  static protected boolean debugLeaks = false;
  static protected boolean debugAccess = false;
  static protected List<String> openFiles = Collections.synchronizedList(new ArrayList<String>());

  /**
   * The default buffer size, in bytes.
   */
  protected static final int defaultBufferSize = 8092;

  /**
   * File location
   */
  protected String location;

  /**
   * The underlying java.io.RandomAccessFile.
   */
  protected java.io.RandomAccessFile file;
  protected java.nio.channels.FileChannel fileChannel;

  /**
   * The offset in bytes from the file start, of the next read or
   * write operation.
   */
  protected long filePosition;

  /**
   * The buffer used for reading the data.
   */
  protected byte buffer[];

  /**
   * The offset in bytes of the start of the buffer, from the start of the file.
   */
  protected long bufferStart;

  /**
   * The offset in bytes of the end of the data in the buffer, from
   * the start of the file. This can be calculated from
   * <code>bufferStart + dataSize</code>, but it is cached to speed
   * up the read( ) method.
   */
  protected long dataEnd;

  /**
   * The size of the data stored in the buffer, in bytes. This may be
   * less than the size of the buffer.
   */
  protected int dataSize;

  /**
   * True if we are at the end of the file.
   */
  protected boolean endOfFile;

  /**
   * The access mode of the file.
   */
  protected boolean readonly;

  /**
   * The current endian (big or little) mode of the file.
   */
  protected boolean bigEndian;

  /**
   * True if the data in the buffer has been modified.
   */
  boolean bufferModified = false;

  /**
   * make sure file is this long when closed
   */
  private long minLength = 0;

  /**
   * stupid extendMode for truncated, yet valid files - old code allowed NOFILL to do this
   */
  boolean extendMode = false;

  /**
   * Constructor, for subclasses
   *
   * @param bufferSize size of read buffer
   */
  protected RandomAccessFile(int bufferSize) {
    file = null;
    readonly = true;
    init(bufferSize);
  }

  /**
   * Constructor, default buffer size.
   *
   * @param location location of the file
   * @param mode     same as for java.io.RandomAccessFile
   * @throws IOException on open error
   */
  public RandomAccessFile(String location, String mode) throws IOException {
    this(location, mode, defaultBufferSize);
    this.location = location;
  }

  /**
   * Constructor.
   *
   * @param location   location of the file
   * @param mode       same as for java.io.RandomAccessFile
   * @param bufferSize size of buffer to use.
   * @throws IOException on open error
   */
  public RandomAccessFile(String location, String mode, int bufferSize)
      throws IOException {
    this.location = location;
    this.file = new java.io.RandomAccessFile(location, mode);
    this.readonly = mode.equals("r");
    init(bufferSize);
    if (debugLeaks) {
      openFiles.add(location);
    }
  }

  /**
   * Allow access to the underlying java.io.RandomAccessFile.
   * WARNING! BROKEN ENCAPSOLATION, DO NOT USE. May change implementation in the future.
   *
   * @return the underlying java.io.RandomAccessFile.
   */
  public java.io.RandomAccessFile getRandomAccessFile() {
    return this.file;
  }

  /**
   * _more_
   *
   * @param bufferSize _more_
   */
  private void init(int bufferSize) {
    // Initialise the buffer
    bufferStart = 0;
    dataEnd = 0;
    dataSize = 0;
    filePosition = 0;
    buffer = new byte[bufferSize];
    endOfFile = false;
  }

  /**
   * Close the file, and release any associated system resources.
   *
   * @throws IOException if an I/O error occurrs.
   */
  public void close() throws IOException {
    if (debugLeaks) {
      openFiles.remove(location);
    }

    if (file == null) {
      return;
    }

    // If we are writing and the buffer has been modified, flush the contents
    // of the buffer.
    if (!readonly && bufferModified) {
      file.seek(bufferStart);
      file.write(buffer, 0, dataSize);
    }

    /* may need to extend file to minLength
   if (!readonly &&  minLength > file.length()) {
     file.seek( minLength-1);
     file.writeByte(0);
   } */

    // may need to extend file, in case no fill is neing used
    // may need to truncate file in case overwriting a longer file
    // use only if minLength is set (by N3iosp)
    if (!readonly && (minLength != 0) && (minLength != file.length())) {
      file.setLength(minLength);
      // System.out.println("TRUNCATE!!! minlength="+minLength);
    }

    // Close the underlying file object.
    file.close();
    file = null;  // help the gc
  }

  /**
   * Return true if file pointer is at end of file.
   *
   * @return _more_
   */
  public boolean isAtEndOfFile() {
    return endOfFile;
  }

  /* Create channel from file
 public FileChannel getChannel() {
     if (file == null) {
         return null;
     }

     try {
         file.seek(0);
     } catch (IOException e) {
         e.printStackTrace();
     }
     return file.getChannel();
 } */

  /**
   * Set the position in the file for the next read or write.
   *
   * @param pos the offset (in bytes) from the start of the file.
   * @throws IOException if an I/O error occurrs.
   */
  public void seek(long pos) throws IOException {

    // If the seek is into the buffer, just update the file pointer.
    if ((pos >= bufferStart) && (pos < dataEnd)) {
      filePosition = pos;
      return;
    }

    // If the current buffer is modified, write it to disk.
    if (bufferModified) {
      flush();
    }

    // need new buffer
    bufferStart = pos;
    filePosition = pos;

    dataSize = read_(pos, buffer, 0, buffer.length);
    if (dataSize <= 0) {
      dataSize = 0;
      endOfFile = true;
    } else {
      endOfFile = false;
    }

    // Cache the position of the buffer end.
    dataEnd = bufferStart + dataSize;
  }

  /**
   * Returns the current position in the file, where the next read or
   * write will occur.
   *
   * @return the offset from the start of the file in bytes.
   * @throws IOException if an I/O error occurrs.
   */
  public long getFilePointer() throws IOException {
    return filePosition;
  }

  /**
   * Get the file location, or name.
   *
   * @return _more_
   */
  public String getLocation() {
    return location;
  }

  /**
   * Get the length of the file. The data in the buffer (which may not
   * have been written the disk yet) is taken into account.
   *
   * @return the length of the file in bytes.
   * @throws IOException if an I/O error occurrs.
   */
  public long length() throws IOException {
    long fileLength = file.length();
    if (fileLength < dataEnd) {
      return dataEnd;
    } else {
      return fileLength;
    }
  }

  /**
   * Change the current endian mode. Subsequent reads of short, int, float, double, long, char will
   * use this. Does not currently affect writes.
   * Default values is BIG_ENDIAN.
   *
   * @param endian RandomAccessFile.BIG_ENDIAN or RandomAccessFile.LITTLE_ENDIAN
   */
  public void order(int endian) {
    if (endian < 0) return;
    this.bigEndian = (endian == BIG_ENDIAN);
  }

  /**
   * Returns the opaque file descriptor object associated with this file.
   *
   * @return the file descriptor object associated with this file.
   * @throws IOException if an I/O error occurs.
   */
  public FileDescriptor getFD() throws IOException {
    return (file == null)
        ? null
        : file.getFD();
  }

  /**
   * Copy the contents of the buffer to the disk.
   *
   * @throws IOException if an I/O error occurrs.
   */
  public void flush() throws IOException {
    if (bufferModified) {
      file.seek(bufferStart);
      file.write(buffer, 0, dataSize);
      //System.out.println("--flush at "+bufferStart+" dataSize= "+dataSize+ " filePosition= "+filePosition);
      bufferModified = false;
    }

      // check min length
      if ( !readonly && (minLength != 0) && (minLength != file.length())) {
          file.setLength(minLength);
      }
  }

  /**
   * Make sure file is at least this long when its closed.
   * needed when not using fill mode, and not all data is written.
   *
   * @param minLength minimum length of the file.
   */
  public void setMinLength(long minLength) {
    this.minLength = minLength;
  }

  /**
   * Set extendMode for truncated, yet valid files - old NetCDF code allowed this
   * when NOFILL on, and user doesnt write all variables.
   */
  public void setExtendMode() {
    this.extendMode = true;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  // Read primitives.
  //

  /**
   * Read a byte of data from the file, blocking until data is
   * available.
   *
   * @return the next byte of data, or -1 if the end of the file is
   *         reached.
   * @throws IOException if an I/O error occurrs.
   */
  public int read() throws IOException {

    // If the file position is within the data, return the byte...
    if (filePosition < dataEnd) {
      int pos = (int) (filePosition - bufferStart);
      filePosition++;
      return (buffer[pos] & 0xff);

      // ...or should we indicate EOF...
    } else if (endOfFile) {
      return -1;

      // ...or seek to fill the buffer, and try again.
    } else {
      seek(filePosition);
      return read();
    }
  }

  /**
   * Read up to <code>len</code> bytes into an array, at a specified
   * offset. This will block until at least one byte has been read.
   *
   * @param b   the byte array to receive the bytes.
   * @param off the offset in the array where copying will start.
   * @param len the number of bytes to copy.
   * @return the actual number of bytes read, or -1 if there is not
   *         more data due to the end of the file being reached.
   * @throws IOException if an I/O error occurrs.
   */
  protected int readBytes(byte b[], int off, int len) throws IOException {

    // Check for end of file.
    if (endOfFile) {
      return -1;
    }

    // See how many bytes are available in the buffer - if none,
    // seek to the file position to update the buffer and try again.
    int bytesAvailable = (int) (dataEnd - filePosition);
    if (bytesAvailable < 1) {
      seek(filePosition);
      return readBytes(b, off, len);
    }

    // Copy as much as we can.
    int copyLength = (bytesAvailable >= len)
        ? len
        : bytesAvailable;
    System.arraycopy(buffer, (int) (filePosition - bufferStart), b, off,
        copyLength);
    filePosition += copyLength;

    // If there is more to copy...
    if (copyLength < len) {
      int extraCopy = len - copyLength;

      // If the amount remaining is more than a buffer's length, read it
      // directly from the file.
      if (extraCopy > buffer.length) {
        extraCopy = read_(filePosition, b, off + copyLength,
            len - copyLength);

        // ...or read a new buffer full, and copy as much as possible...
      } else {
        seek(filePosition);
        if (!endOfFile) {
          extraCopy = (extraCopy > dataSize)
              ? dataSize
              : extraCopy;
          System.arraycopy(buffer, 0, b, off + copyLength,
              extraCopy);
        } else {
          extraCopy = -1;
        }
      }

      // If we did manage to copy any more, update the file position and
      // return the amount copied.
      if (extraCopy > 0) {
        filePosition += extraCopy;
        return copyLength + extraCopy;
      }
    }

    // Return the amount copied.
    return copyLength;
  }

  /**
   * Read up to <code>nbytes</code> bytes, at a specified offset, send to a WritableByteChannel.
   * This will block until all bytes are read.
   * This uses the underlying file channel directly, bypassing all user buffers.
   *
   * @param dest write to this WritableByteChannel.
   * @param offset the offset in the file where copying will start.
   * @param nbytes the number of bytes to read.
   * @return the actual number of bytes read, or -1 if there is no
   *         more data due to the end of the file being reached.
   * @throws IOException if an I/O error occurs.
   */
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {

    if (fileChannel == null)
        fileChannel = file.getChannel();

    long done = 0;
    while (done < nbytes) {
      done += fileChannel.transferTo(offset, nbytes, dest);
      if (done <= 0) break;  // LOOK not sure what the EOF condition is
    }
    return done;
  }
  

  /**
   * Read directly from file, without going through the buffer.
   * All reading goes through here or readToByteChannel;
   *
   * @param pos    start here in the file
   * @param b      put data into this buffer
   * @param offset buffer offset
   * @param len    this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  protected int read_(long pos, byte[] b, int offset, int len)
      throws IOException {

    file.seek(pos);
    int n = file.read(b, offset, len);
    if (debugAccess)
      System.out.println(" **read_ " + location + " = " + len + " bytes at " + pos + "; block = " + (pos / buffer.length));

    if (extendMode && (n < len)) {
      //System.out.println(" read_ = "+len+" at "+pos+"; got = "+n);
      n = len;
    }
    return n;
  }

  /**
   * Read up to <code>len</code> bytes into an array, at a specified
   * offset. This will block until at least one byte has been read.
   *
   * @param b   the byte array to receive the bytes.
   * @param off the offset in the array where copying will start.
   * @param len the number of bytes to copy.
   * @return the actual number of bytes read, or -1 if there is not
   *         more data due to the end of the file being reached.
   * @throws IOException if an I/O error occurrs.
   */
  public int read(byte b[], int off, int len) throws IOException {
    return readBytes(b, off, len);
  }

  /**
   * Read up to <code>b.length( )</code> bytes into an array. This
   * will block until at least one byte has been read.
   *
   * @param b the byte array to receive the bytes.
   * @return the actual number of bytes read, or -1 if there is not
   *         more data due to the end of the file being reached.
   * @throws IOException if an I/O error occurrs.
   */
  public int read(byte b[]) throws IOException {
    return readBytes(b, 0, b.length);
  }

  /**
   * Read fully count bytes
   *
   * @param count how many bytes tp read
   * @return a byte array of length count, fully read in
   * @throws IOException if an I/O error occurrs.
   */
  public byte[] readBytes(int count) throws IOException {
    byte[] b = new byte[count];
    readFully(b);
    return b;
  }

  /**
   * Reads <code>b.length</code> bytes from this file into the byte
   * array. This method reads repeatedly from the file until all the
   * bytes are read. This method blocks until all the bytes are read,
   * the end of the stream is detected, or an exception is thrown.
   *
   * @param b the buffer into which the data is read.
   * @throws EOFException if this file reaches the end before reading
   *                      all the bytes.
   * @throws IOException  if an I/O error occurs.
   */
  public final void readFully(byte b[]) throws IOException {
    readFully(b, 0, b.length);
  }

  /**
   * Reads exactly <code>len</code> bytes from this file into the byte
   * array. This method reads repeatedly from the file until all the
   * bytes are read. This method blocks until all the bytes are read,
   * the end of the stream is detected, or an exception is thrown.
   *
   * @param b   the buffer into which the data is read.
   * @param off the start offset of the data.
   * @param len the number of bytes to read.
   * @throws EOFException if this file reaches the end before reading
   *                      all the bytes.
   * @throws IOException  if an I/O error occurs.
   */
  public final void readFully(byte b[], int off, int len)
      throws IOException {
    int n = 0;
    while (n < len) {
      int count = this.read(b, off + n, len - n);
      if (count < 0) {
        throw new EOFException();
      }
      n += count;
    }
  }

  /**
   * Skips exactly <code>n</code> bytes of input.
   * This method blocks until all the bytes are skipped, the end of
   * the stream is detected, or an exception is thrown.
   *
   * @param n the number of bytes to be skipped.
   * @return the number of bytes skipped, which is always <code>n</code>.
   * @throws EOFException if this file reaches the end before skipping
   *                      all the bytes.
   * @throws IOException  if an I/O error occurs.
   */
  public int skipBytes(int n) throws IOException {
    seek(getFilePointer() + n);
    return n;
  }

  /* public void skipToMultiple( int multipleOfBytes) throws IOException {
   long pos = getFilePointer();
   int pad = (int) (pos % multipleOfBytes);
   if (pad != 0) pad = multipleOfBytes - pad;
   if (pad > 0) skipBytes(pad);
 } */

  /**
   * Unread the last byte read.
   * This method should not be used more than once
   * between reading operations, or strange things might happen.
   */
  public void unread() {
    filePosition--;
  }

  //
  // Write primitives.
  //

  /**
   * Write a byte to the file. If the file has not been opened for
   * writing, an IOException will be raised only when an attempt is
   * made to write the buffer to the file.
   * <p/>
   * Caveat: the effects of seek( )ing beyond the end of the file are
   * undefined.
   *
   * @param b write this byte
   * @throws IOException if an I/O error occurrs.
   */
  public void write(int b) throws IOException {

    // If the file position is within the block of data...
    if (filePosition < dataEnd) {
      int pos = (int) (filePosition - bufferStart);
      buffer[pos] = (byte) b;
      bufferModified = true;
      filePosition++;

      // ...or (assuming that seek will not allow the file pointer
      // to move beyond the end of the file) get the correct block of
      // data...
    } else {

      // If there is room in the buffer, expand it...
      if (dataSize != buffer.length) {
        int pos = (int) (filePosition - bufferStart);
        buffer[pos] = (byte) b;
        bufferModified = true;
        filePosition++;
        dataSize++;
        dataEnd++;

        // ...or do another seek to get a new buffer, and start again...
      } else {
        seek(filePosition);
        write(b);
      }
    }
  }

  /**
   * Write <code>len</code> bytes from an array to the file.
   *
   * @param b   the array containing the data.
   * @param off the offset in the array to the data.
   * @param len the length of the data.
   * @throws IOException if an I/O error occurrs.
   */
  public void writeBytes(byte b[], int off, int len) throws IOException {
    // If the amount of data is small (less than a full buffer)...
    if (len < buffer.length) {

      // If any of the data fits within the buffer...
      int spaceInBuffer = 0;
      int copyLength = 0;
      if (filePosition >= bufferStart) {
        spaceInBuffer = (int) ((bufferStart + buffer.length) - filePosition);
      }

      if (spaceInBuffer > 0) {
        // Copy as much as possible to the buffer.
        copyLength = (spaceInBuffer > len) ? len : spaceInBuffer;
        System.arraycopy(b, off, buffer, (int) (filePosition - bufferStart), copyLength);
        bufferModified = true;
        long myDataEnd = filePosition + copyLength;
        dataEnd = (myDataEnd > dataEnd) ? myDataEnd : dataEnd;
        dataSize = (int) (dataEnd - bufferStart);
        filePosition += copyLength;
        ///System.out.println("--copy to buffer "+copyLength+" "+len);
      }

      // If there is any data remaining, move to the new position and copy to
      // the new buffer.
      if (copyLength < len) {
        //System.out.println("--need more "+copyLength+" "+len+" space= "+spaceInBuffer);
        seek(filePosition);   // triggers a flush
        System.arraycopy(b, off + copyLength, buffer, (int) (filePosition - bufferStart), len - copyLength);
        bufferModified = true;
        long myDataEnd = filePosition + (len - copyLength);
        dataEnd = (myDataEnd > dataEnd) ? myDataEnd : dataEnd;
        dataSize = (int) (dataEnd - bufferStart);
        filePosition += (len - copyLength);
      }

      // ...or write a lot of data...
    } else {

      // Flush the current buffer, and write this data to the file.
      if (bufferModified) {
        flush();
      }
      file.seek(filePosition);  // moved per Steve Cerruti; Jan 14, 2005
      file.write(b, off, len);
      //System.out.println("--write at "+filePosition+" "+len);

      filePosition += len;
      bufferStart = filePosition;  // an empty buffer
      dataSize = 0;
      dataEnd = bufferStart + dataSize;
    }
  }

  /**
   * Writes <code>b.length</code> bytes from the specified byte array
   * starting at offset <code>off</code> to this file.
   *
   * @param b the data.
   * @throws IOException if an I/O error occurs.
   */
  public void write(byte b[]) throws IOException {
    writeBytes(b, 0, b.length);
  }

  /**
   * Writes <code>len</code> bytes from the specified byte array
   * starting at offset <code>off</code> to this file.
   *
   * @param b   the data.
   * @param off the start offset in the data.
   * @param len the number of bytes to write.
   * @throws IOException if an I/O error occurs.
   */
  public void write(byte b[], int off, int len) throws IOException {
    writeBytes(b, off, len);
  }

  //
  // DataInput methods.
  //

  /**
   * Reads a <code>boolean</code> from this file. This method reads a
   * single byte from the file. A value of <code>0</code> represents
   * <code>false</code>. Any other value represents <code>true</code>.
   * This method blocks until the byte is read, the end of the stream
   * is detected, or an exception is thrown.
   *
   * @return the <code>boolean</code> value read.
   * @throws EOFException if this file has reached the end.
   * @throws IOException  if an I/O error occurs.
   */
  public final boolean readBoolean() throws IOException {
    int ch = this.read();
    if (ch < 0) {
      throw new EOFException();
    }
    return (ch != 0);
  }

  /**
   * Reads a signed 8-bit value from this file. This method reads a
   * byte from the file. If the byte read is <code>b</code>, where
   * <code>0&nbsp;&lt;=&nbsp;b&nbsp;&lt;=&nbsp;255</code>,
   * then the result is:
   * <ul><code>
   * (byte)(b)
   * </code></ul>
   * <p/>
   * This method blocks until the byte is read, the end of the stream
   * is detected, or an exception is thrown.
   *
   * @return the next byte of this file as a signed 8-bit
   *         <code>byte</code>.
   * @throws EOFException if this file has reached the end.
   * @throws IOException  if an I/O error occurs.
   */
  public final byte readByte() throws IOException {
    int ch = this.read();
    if (ch < 0) {
      throw new EOFException();
    }
    return (byte) (ch);
  }

  /**
   * Reads an unsigned 8-bit number from this file. This method reads
   * a byte from this file and returns that byte.
   * <p/>
   * This method blocks until the byte is read, the end of the stream
   * is detected, or an exception is thrown.
   *
   * @return the next byte of this file, interpreted as an unsigned
   *         8-bit number.
   * @throws EOFException if this file has reached the end.
   * @throws IOException  if an I/O error occurs.
   */
  public final int readUnsignedByte() throws IOException {
    int ch = this.read();
    if (ch < 0) {
      throw new EOFException();
    }
    return ch;
  }

  /**
   * Reads a signed 16-bit number from this file. The method reads 2
   * bytes from this file. If the two bytes read, in order, are
   * <code>b1</code> and <code>b2</code>, where each of the two values is
   * between <code>0</code> and <code>255</code>, inclusive, then the
   * result is equal to:
   * <ul><code>
   * (short)((b1 &lt;&lt; 8) | b2)
   * </code></ul>
   * <p/>
   * This method blocks until the two bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next two bytes of this file, interpreted as a signed
   *         16-bit number.
   * @throws EOFException if this file reaches the end before reading
   *                      two bytes.
   * @throws IOException  if an I/O error occurs.
   */
  public final short readShort() throws IOException {
    int ch1 = this.read();
    int ch2 = this.read();
    if ((ch1 | ch2) < 0) {
      throw new EOFException();
    }
    if (bigEndian) {
      return (short) ((ch1 << 8) + (ch2));
    } else {
      return (short) ((ch2 << 8) + (ch1));
    }
  }

  /**
   * Read an array of shorts
   *
   * @param pa    read into this array
   * @param start starting at pa[start]
   * @param n     read this many elements
   * @throws IOException on read error
   */
  public final void readShort(short[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      pa[start + i] = readShort();
    }
  }

  /**
   * Reads an unsigned 16-bit number from this file. This method reads
   * two bytes from the file. If the bytes read, in order, are
   * <code>b1</code> and <code>b2</code>, where
   * <code>0&nbsp;&lt;=&nbsp;b1, b2&nbsp;&lt;=&nbsp;255</code>,
   * then the result is equal to:
   * <ul><code>
   * (b1 &lt;&lt; 8) | b2
   * </code></ul>
   * <p/>
   * This method blocks until the two bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next two bytes of this file, interpreted as an unsigned
   *         16-bit integer.
   * @throws EOFException if this file reaches the end before reading
   *                      two bytes.
   * @throws IOException  if an I/O error occurs.
   */
  public final int readUnsignedShort() throws IOException {
    int ch1 = this.read();
    int ch2 = this.read();
    if ((ch1 | ch2) < 0) {
      throw new EOFException();
    }
    if (bigEndian) {
      return ((ch1 << 8) + (ch2));
    } else {
      return ((ch2 << 8) + (ch1));
    }
  }


  /*
   * Reads a signed 24-bit integer from this file. This method reads 3
   * bytes from the file. If the bytes read, in order, are <code>b1</code>,
   * <code>b2</code>, and <code>b3</code>, where
   * <code>0&nbsp;&lt;=&nbsp;b1, b2, b3&nbsp;&lt;=&nbsp;255</code>,
   * then the result is equal to:
   * <ul><code>
   * (b1 &lt;&lt; 16) | (b2 &lt;&lt; 8) + (b3 &lt;&lt; 0)
   * </code></ul>
   * <p/>
   * This method blocks until the three bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   */

  /**
   * Reads a Unicode character from this file. This method reads two
   * bytes from the file. If the bytes read, in order, are
   * <code>b1</code> and <code>b2</code>, where
   * <code>0&nbsp;&lt;=&nbsp;b1,&nbsp;b2&nbsp;&lt;=&nbsp;255</code>,
   * then the result is equal to:
   * <ul><code>
   * (char)((b1 &lt;&lt; 8) | b2)
   * </code></ul>
   * <p/>
   * This method blocks until the two bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next two bytes of this file as a Unicode character.
   * @throws EOFException if this file reaches the end before reading
   *                      two bytes.
   * @throws IOException  if an I/O error occurs.
   */
  public final char readChar() throws IOException {
    int ch1 = this.read();
    int ch2 = this.read();
    if ((ch1 | ch2) < 0) {
      throw new EOFException();
    }
    if (bigEndian) {
      return (char) ((ch1 << 8) + (ch2));
    } else {
      return (char) ((ch2 << 8) + (ch1));
    }
  }

  /**
   * Reads a signed 32-bit integer from this file. This method reads 4
   * bytes from the file. If the bytes read, in order, are <code>b1</code>,
   * <code>b2</code>, <code>b3</code>, and <code>b4</code>, where
   * <code>0&nbsp;&lt;=&nbsp;b1, b2, b3, b4&nbsp;&lt;=&nbsp;255</code>,
   * then the result is equal to:
   * <ul><code>
   * (b1 &lt;&lt; 24) | (b2 &lt;&lt; 16) + (b3 &lt;&lt; 8) + b4
   * </code></ul>
   * <p/>
   * This method blocks until the four bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next four bytes of this file, interpreted as an
   *         <code>int</code>.
   * @throws EOFException if this file reaches the end before reading
   *                      four bytes.
   * @throws IOException  if an I/O error occurs.
   */
  public final int readInt() throws IOException {
    int ch1 = this.read();
    int ch2 = this.read();
    int ch3 = this.read();
    int ch4 = this.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
      throw new EOFException();
    }

    if (bigEndian) {
      return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    } else {
      return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
    }
  }

  /**
   * Read an integer at the given position, bypassing all buffering.
   *
   * @param pos read a byte at this position
   * @return The int that was read
   * @throws IOException if an I/O error occurs.
   */
  public final int readIntUnbuffered(long pos) throws IOException {
    byte[] bb = new byte[4];
    read_(pos, bb, 0, 4);
    int ch1 = bb[0] & 0xff;
    int ch2 = bb[1] & 0xff;
    int ch3 = bb[2] & 0xff;
    int ch4 = bb[3] & 0xff;
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
      throw new EOFException();
    }

    if (bigEndian) {
      return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    } else {
      return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
    }
  }


  /**
   * Read an array of ints
   *
   * @param pa    read into this array
   * @param start starting at pa[start]
   * @param n     read this many elements
   * @throws IOException on read error
   */
  public final void readInt(int[] pa, int start, int n) throws IOException {
    for (int i = 0; i < n; i++) {
      pa[start + i] = readInt();
    }
  }

  /**
   * Reads a signed 64-bit integer from this file. This method reads eight
   * bytes from the file. If the bytes read, in order, are
   * <code>b1</code>, <code>b2</code>, <code>b3</code>,
   * <code>b4</code>, <code>b5</code>, <code>b6</code>,
   * <code>b7</code>, and <code>b8,</code> where:
   * <ul><code>
   * 0 &lt;= b1, b2, b3, b4, b5, b6, b7, b8 &lt;=255,
   * </code></ul>
   * <p/>
   * then the result is equal to:
   * <p><blockquote><pre>
   *     ((long)b1 &lt;&lt; 56) + ((long)b2 &lt;&lt; 48)
   *     + ((long)b3 &lt;&lt; 40) + ((long)b4 &lt;&lt; 32)
   *     + ((long)b5 &lt;&lt; 24) + ((long)b6 &lt;&lt; 16)
   *     + ((long)b7 &lt;&lt; 8) + b8
   * </pre></blockquote>
   * <p/>
   * This method blocks until the eight bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next eight bytes of this file, interpreted as a
   *         <code>long</code>.
   * @throws EOFException if this file reaches the end before reading
   *                      eight bytes.
   * @throws IOException  if an I/O error occurs.
   */
  public final long readLong() throws IOException {
    if (bigEndian) {
      return ((long) (readInt()) << 32) + (readInt() & 0xFFFFFFFFL);  // tested ok
    } else {
      return ( (readInt() & 0xFFFFFFFFL) + ((long) readInt() << 32)); // not tested yet ??
    }

    /*     int ch1 = this.read();
      int ch2 = this.read();
      int ch3 = this.read();
      int ch4 = this.read();
      int ch5 = this.read();
      int ch6 = this.read();
      int ch7 = this.read();
      int ch8 = this.read();
      if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0)
         throw new EOFException();

      if (bigEndian)
        return ((long)(ch1 << 56)) + (ch2 << 48) + (ch3 << 40) + (ch4 << 32) + (ch5 << 24) + (ch6 << 16) + (ch7 << 8) + (ch8 << 0));
      else
        return ((long)(ch8 << 56) + (ch7 << 48) + (ch6 << 40) + (ch5 << 32) + (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    */
  }

  /**
   * Read an array of longs
   *
   * @param pa    read into this array
   * @param start starting at pa[start]
   * @param n     read this many elements
   * @throws IOException on read error
   */
  public final void readLong(long[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      pa[start + i] = readLong();
    }
  }


  /**
   * Reads a <code>float</code> from this file. This method reads an
   * <code>int</code> value as if by the <code>readInt</code> method
   * and then converts that <code>int</code> to a <code>float</code>
   * using the <code>intBitsToFloat</code> method in class
   * <code>Float</code>.
   * <p/>
   * This method blocks until the four bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next four bytes of this file, interpreted as a
   *         <code>float</code>.
   * @throws EOFException if this file reaches the end before reading
   *                      four bytes.
   * @throws IOException  if an I/O error occurs.
   * @see java.io.RandomAccessFile#readInt()
   * @see java.lang.Float#intBitsToFloat(int)
   */
  public final float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  /**
   * Read an array of floats
   *
   * @param pa    read into this array
   * @param start starting at pa[start]
   * @param n     read this many elements
   * @throws IOException on read error
   */
  public final void readFloat(float[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      pa[start + i] = Float.intBitsToFloat(readInt());
    }
  }


  /**
   * Reads a <code>double</code> from this file. This method reads a
   * <code>long</code> value as if by the <code>readLong</code> method
   * and then converts that <code>long</code> to a <code>double</code>
   * using the <code>longBitsToDouble</code> method in
   * class <code>Double</code>.
   * <p/>
   * This method blocks until the eight bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next eight bytes of this file, interpreted as a
   *         <code>double</code>.
   * @throws EOFException if this file reaches the end before reading
   *                      eight bytes.
   * @throws IOException  if an I/O error occurs.
   * @see java.io.RandomAccessFile#readLong()
   * @see java.lang.Double#longBitsToDouble(long)
   */
  public final double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  /**
   * Read an array of doubles
   *
   * @param pa    read into this array
   * @param start starting at pa[start]
   * @param n     read this many elements
   * @throws IOException on read error
   */
  public final void readDouble(double[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      pa[start + i] = Double.longBitsToDouble(readLong());
    }
  }

  /**
   * Reads the next line of text from this file. This method
   * successively reads bytes from the file until it reaches the end of
   * a line of text.
   * <p/>
   * <p/>
   * A line of text is terminated by a carriage-return character
   * (<code>'&#92;r'</code>), a newline character (<code>'&#92;n'</code>), a
   * carriage-return character immediately followed by a newline
   * character, or the end of the input stream. The line-terminating
   * character(s), if any, are included as part of the string returned.
   * <p/>
   * <p/>
   * This method blocks until a newline character is read, a carriage
   * return and the byte following it are read (to see if it is a
   * newline), the end of the stream is detected, or an exception is thrown.
   *
   * @return the next line of text from this file.
   * @throws IOException if an I/O error occurs.
   */
  public final String readLine() throws IOException {
    StringBuffer input = new StringBuffer();
    int c;

    while (((c = read()) != -1) && (c != '\n')) {
      input.append((char) c);
    }
    if ((c == -1) && (input.length() == 0)) {
      return null;
    }
    return input.toString();
  }

  /**
   * Reads in a string from this file. The string has been encoded
   * using a modified UTF-8 format.
   * <p/>
   * The first two bytes are read as if by
   * <code>readUnsignedShort</code>. This value gives the number of
   * following bytes that are in the encoded string, not
   * the length of the resulting string. The following bytes are then
   * interpreted as bytes encoding characters in the UTF-8 format
   * and are converted into characters.
   * <p/>
   * This method blocks until all the bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return a Unicode string.
   * @throws EOFException           if this file reaches the end before
   *                                reading all the bytes.
   * @throws IOException            if an I/O error occurs.
   * @throws UTFDataFormatException if the bytes do not represent
   *                                valid UTF-8 encoding of a Unicode string.
   * @see java.io.RandomAccessFile#readUnsignedShort()
   */
  public final String readUTF() throws IOException {
    return DataInputStream.readUTF(this);
  }

  /**
   * Read a String of knoen length.
   *
   * @param nbytes number of bytes to read
   * @return String wrapping the bytes.
   * @throws IOException if an I/O error occurs.
   */
  public String readString(int nbytes) throws IOException {
    byte[] data = new byte[nbytes];
    readFully(data);
    return new String(data);
  }

  //
  // DataOutput methods.
  //

  /**
   * Writes a <code>boolean</code> to the file as a 1-byte value. The
   * value <code>true</code> is written out as the value
   * <code>(byte)1</code>; the value <code>false</code> is written out
   * as the value <code>(byte)0</code>.
   *
   * @param v a <code>boolean</code> value to be written.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeBoolean(boolean v) throws IOException {
    write(v
        ? 1
        : 0);
  }

  /**
   * _more_
   *
   * @param pa    _more_
   * @param start _more_
   * @param n     _more_
   * @throws IOException _more_
   */
  public final void writeBoolean(boolean[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      writeBoolean(pa[start + i]);
    }
  }

  /**
   * Writes a <code>byte</code> to the file as a 1-byte value.
   *
   * @param v a <code>byte</code> value to be written.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeByte(int v) throws IOException {
    write(v);
  }

  /**
   * Writes a <code>short</code> to the file as two bytes, high byte first.
   *
   * @param v a <code>short</code> to be written.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeShort(int v) throws IOException {
    write((v >>> 8) & 0xFF);
    write((v) & 0xFF);
  }

  /**
   * _more_
   *
   * @param pa    _more_
   * @param start _more_
   * @param n     _more_
   * @throws IOException _more_
   */
  public final void writeShort(short[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      writeShort(pa[start + i]);
    }
  }

  /**
   * Writes a <code>char</code> to the file as a 2-byte value, high
   * byte first.
   *
   * @param v a <code>char</code> value to be written.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeChar(int v) throws IOException {
    write((v >>> 8) & 0xFF);
    write((v) & 0xFF);
  }

  /**
   * _more_
   *
   * @param pa    _more_
   * @param start _more_
   * @param n     _more_
   * @throws IOException _more_
   */
  public final void writeChar(char[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      writeChar(pa[start + i]);
    }
  }


  /**
   * Writes an <code>int</code> to the file as four bytes, high byte first.
   *
   * @param v an <code>int</code> to be written.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeInt(int v) throws IOException {
    write((v >>> 24) & 0xFF);
    write((v >>> 16) & 0xFF);
    write((v >>> 8) & 0xFF);
    write((v) & 0xFF);
  }

  /**
   * _more_
   *
   * @param pa    _more_
   * @param start _more_
   * @param n     _more_
   * @throws IOException _more_
   */
  public final void writeInt(int[] pa, int start, int n) throws IOException {
    for (int i = 0; i < n; i++) {
      writeInt(pa[start + i]);
    }
  }

  /**
   * Writes a <code>long</code> to the file as eight bytes, high byte first.
   *
   * @param v a <code>long</code> to be written.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeLong(long v) throws IOException {
    write((int) (v >>> 56) & 0xFF);
    write((int) (v >>> 48) & 0xFF);
    write((int) (v >>> 40) & 0xFF);
    write((int) (v >>> 32) & 0xFF);
    write((int) (v >>> 24) & 0xFF);
    write((int) (v >>> 16) & 0xFF);
    write((int) (v >>> 8) & 0xFF);
    write((int) (v) & 0xFF);
  }

  /**
   * _more_
   *
   * @param pa    _more_
   * @param start _more_
   * @param n     _more_
   * @throws IOException _more_
   */
  public final void writeLong(long[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      writeLong(pa[start + i]);
    }
  }

  /**
   * Converts the float argument to an <code>int</code> using the
   * <code>floatToIntBits</code> method in class <code>Float</code>,
   * and then writes that <code>int</code> value to the file as a
   * 4-byte quantity, high byte first.
   *
   * @param v a <code>float</code> value to be written.
   * @throws IOException if an I/O error occurs.
   * @see java.lang.Float#floatToIntBits(float)
   */
  public final void writeFloat(float v) throws IOException {
    writeInt(Float.floatToIntBits(v));
  }

  /**
   * _more_
   *
   * @param pa    _more_
   * @param start _more_
   * @param n     _more_
   * @throws IOException _more_
   */
  public final void writeFloat(float[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      writeFloat(pa[start + i]);
    }
  }


  /**
   * Converts the double argument to a <code>long</code> using the
   * <code>doubleToLongBits</code> method in class <code>Double</code>,
   * and then writes that <code>long</code> value to the file as an
   * 8-byte quantity, high byte first.
   *
   * @param v a <code>double</code> value to be written.
   * @throws IOException if an I/O error occurs.
   * @see java.lang.Double#doubleToLongBits(double)
   */
  public final void writeDouble(double v) throws IOException {
    writeLong(Double.doubleToLongBits(v));
  }

  /**
   * _more_
   *
   * @param pa    _more_
   * @param start _more_
   * @param n     _more_
   * @throws IOException _more_
   */
  public final void writeDouble(double[] pa, int start, int n)
      throws IOException {
    for (int i = 0; i < n; i++) {
      writeDouble(pa[start + i]);
    }
  }

  /**
   * Writes the string to the file as a sequence of bytes. Each
   * character in the string is written out, in sequence, by discarding
   * its high eight bits.
   *
   * @param s a string of bytes to be written.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeBytes(String s) throws IOException {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      write((byte) s.charAt(i));
    }
  }

  /**
   * Writes the character array to the file as a sequence of bytes. Each
   * character in the string is written out, in sequence, by discarding
   * its high eight bits.
   *
   * @param b   a character array of bytes to be written.
   * @param off the index of the first character to write.
   * @param len the number of characters to write.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeBytes(char b[], int off, int len)
      throws IOException {
    for (int i = off; i < len; i++) {
      write((byte) b[i]);
    }
  }

  /**
   * Writes a string to the file as a sequence of characters. Each
   * character is written to the data output stream as if by the
   * <code>writeChar</code> method.
   *
   * @param s a <code>String</code> value to be written.
   * @throws IOException if an I/O error occurs.
   * @see java.io.RandomAccessFile#writeChar(int)
   */
  public final void writeChars(String s) throws IOException {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      int v = s.charAt(i);
      write((v >>> 8) & 0xFF);
      write((v) & 0xFF);
    }
  }

  /**
   * Writes a string to the file using UTF-8 encoding in a
   * machine-independent manner.
   * <p/>
   * First, two bytes are written to the file as if by the
   * <code>writeShort</code> method giving the number of bytes to
   * follow. This value is the number of bytes actually written out,
   * not the length of the string. Following the length, each character
   * of the string is output, in sequence, using the UTF-8 encoding
   * for each character.
   *
   * @param str a string to be written.
   * @throws IOException if an I/O error occurs.
   */
  public final void writeUTF(String str) throws IOException {
    int strlen = str.length();
    int utflen = 0;

    for (int i = 0; i < strlen; i++) {
      int c = str.charAt(i);
      if ((c >= 0x0001) && (c <= 0x007F)) {
        utflen++;
      } else if (c > 0x07FF) {
        utflen += 3;
      } else {
        utflen += 2;
      }
    }
    if (utflen > 65535) {
      throw new UTFDataFormatException();
    }

    write((utflen >>> 8) & 0xFF);
    write((utflen) & 0xFF);
    for (int i = 0; i < strlen; i++) {
      int c = str.charAt(i);
      if ((c >= 0x0001) && (c <= 0x007F)) {
        write(c);
      } else if (c > 0x07FF) {
        write(0xE0 | ((c >> 12) & 0x0F));
        write(0x80 | ((c >> 6) & 0x3F));
        write(0x80 | ((c) & 0x3F));
      } else {
        write(0xC0 | ((c >> 6) & 0x1F));
        write(0x80 | ((c) & 0x3F));
      }
    }
  }

  /**
   * Create a string representation of this object.
   *
   * @return a string representation of the state of the object.
   */
  public String toString() {
    return "fp=" + filePosition + ", bs=" + bufferStart + ", de="
        + dataEnd + ", ds=" + dataSize + ", bl=" + buffer.length
        + ", readonly=" + readonly + ", bm=" + bufferModified;
  }

  /**
   * Support for ucar.unidata.io.FileCache.
   */
  protected boolean cached;

  /**
   * Set whether this fie is in the cache.
   *
   * @param cached in the cache or not.
   * @see ucar.unidata.io.FileCache
   */
  public void setCached(boolean cached) {
    this.cached = cached;
  }

  /**
   * Find whether this fie is in the cache.
   *
   * @return true if in the cache.
   * @see ucar.unidata.io.FileCache
   */
  public boolean isCached() {
    return cached;
  }

  /**
   * _more_
   *
   * @throws IOException _more_
   */
  public void synch() throws IOException {
  }

  /**
   * Test the byte operations of the RandomAccessFile class. These are
   * the methods that read/write on a byte-by-byte basis. The following checks
   * are made:
   * <ul>
   * <li>Writing random bytes to a file.
   * <li>Checking the size of the file is correct.
   * <li>Checking that EOF is correctly raised.
   * <li>Reading the file back in and verifying its contents.
   * </ul>
   * The test file is 4.5 times the size of the buffer, in order to test
   * paging between buffers, and using files that end in the middle of a
   * buffer. A constant seed value is used for the random number generator,
   * to ensure any bugs are reproduceable.
   *
   * @param filename    the name of the test file to generate.
   * @param bufferSize  the size of the buffer to use.
   *
   * public static void testBytes( String filename, int bufferSize ) {
   *
   *  System.out.println( "\nTesting byte operations..." );
   *  int newFileSize = (int)(bufferSize * 4.5 );
   *
   *  try {
   *
   *     // Create a test file.
   *     RandomAccessFile outFile = new RandomAccessFile( filename,
   *        RandomAccessFile.WRITE |
   *        RandomAccessFile.CREATE, bufferSize );
   *     try {
   *        Random random = new Random( 0 );
   *        byte b = 0;
   *        for( int i = 0; i < newFileSize; i++ ) {
   *           b = (byte)(random.nextInt( ) % 256);
   *           outFile.writeByte( b );
   *        }
   *     } finally {
   *        outFile.close( );
   *     }
   *
   *     // Check that the file length is correct.
   *     if( (new File( filename )).length( ) == newFileSize )
   *        System.out.println( ". File size correct (" + newFileSize + ")." );
   *     else
   *        System.out.println( "X New file size incorrect (should be " + newFileSize +
   *                            ", but is " + (new File( filename )).length( ) + ")." );
   *
   *     // Read the file, verify and modify its contents.
   *     RandomAccessFile inoutFile = new RandomAccessFile( filename,
   *        RandomAccessFile.READ |
   *        RandomAccessFile.WRITE, bufferSize );
   *
   *     boolean verified = true;
   *     int byteNo = 0;
   *     try {
   *
   *        // Read each byte in the file.
   *        Random random = new Random( 0 );
   *        byte b = 0;
   *        for( byteNo = 0; byteNo < newFileSize; byteNo++ ) {
   *           b = (byte)(random.nextInt( ) % 256);
   *           byte currentByte = inoutFile.readByte( );
   *
   *           // Check the value is correct.
   *           if( currentByte != b )
   *              verified = false;
   *
   *           // Modify selected values.
   *           if( currentByte >=128 ) {
   *              inoutFile.seek( inoutFile.getFilePointer( ) - 1 );
   *              inoutFile.writeByte( 0 );
   *           }
   *        }
   *
   *        // Check the EOF is correctly trapped.
   *        boolean foundEOF = false;
   *        try {
   *           inoutFile.readByte( );
   *        } catch( EOFException e ) {
   *           foundEOF = true;
   *        }
   *        if( foundEOF )
   *           System.err.println( ". EOF found correctly" );
   *        else
   *           System.err.println( "X No EOF found." );
   *
   *     // Trace a premature EOF.
   *     } catch( EOFException e ) {
   *        e.printStackTrace( );
   *        System.err.println( "    At byte " + byteNo );
   *     } finally {
   *        inoutFile.close( );
   *     }
   *
   *     // Check that the read was verified.
   *     if( verified )
   *        System.out.println( ". Read/Write verified" );
   *     else
   *        System.out.println( "X Read/Write verification failed" );
   *
   *     // Read the file and verify contents.
   *     RandomAccessFile inFile = new RandomAccessFile( filename,
   *        RandomAccessFile.READ, bufferSize );
   *
   *     verified = true;
   *     byteNo = 0;
   *     try {
   *
   *        // Read each byte in the file.
   *        Random random = new Random( 0 );
   *        byte b = 0;
   *        for( byteNo = 0; byteNo < newFileSize; byteNo++ ) {
   *           b = (byte)(random.nextInt( ) % 256);
   *           byte currentByte = inFile.readByte( );
   *
   *           // Account for the modification.
   *           if( currentByte >= 128 )
   *              currentByte = 0;
   *
   *           // Check the byte's value.
   *           if( currentByte != b )
   *              verified = false;
   *        }
   *
   *     // Trap a premature EOF.
   *     } catch( EOFException e ) {
   *        e.printStackTrace( );
   *        System.err.println( "    At byte " + byteNo );
   *     } finally {
   *        inFile.close( );
   *     }
   *
   *     // Check that the read was verified.
   *     if( verified )
   *        System.out.println( ". Update verified" );
   *     else
   *        System.out.println( "X Update verification failed" );
   *
   *  } catch( Exception e ) {
   *     e.printStackTrace( );
   *  }
   * }
   *
   *
   * Test the block operations of the RandomAccessFile class. These
   * are the methods that read/write blocks of data. The following checks
   * are made:
   * <ul>
   * <li>Writing blocks of data that are smaller than the buffer size.
   * <li>Writing blocks of data that are larger than the buffer size.
   * <li>Checking the size of the file is correct.
   * <li>Reading small blocks of the file back in and verifying its contents.
   * <li>Reading large blocks of the file back in and verifying its contents.
   * </ul>
   *
   * @param filename    the name of the test file to generate.
   *
   * public static void testBlocks( String filename ) {
   *
   *  System.err.println( "\nTesting block operations..." );
   *
   *  // Generate the data.
   *  int bufferSize = 10;
   *  byte data[] = new byte[256];
   *  for( int i = 0; i < data.length; i++ )
   *     data[i] = (byte)(i % 256);
   *
   *  try {
   *
   *     // Write the data in small and large blocks.
   *     RandomAccessFile outFile = new RandomAccessFile(
   *                           filename, RandomAccessFile.WRITE |
   *                           RandomAccessFile.CREATE, bufferSize );
   *     for( int i = 0; i < data.length; ) {
   *        int blockSize = (i < data.length / 2) ? 3 :
   *                                                13 ;
   *        blockSize = (i + blockSize >= data.length) ? (data.length - i) :
   *                                                     blockSize;
   *        outFile.write( data, i, blockSize );
   *        i += blockSize;
   *     }
   *
   *     outFile.close( );
   *
   *     // Check that the file length is correct.
   *     if( (new File( filename )).length( ) != data.length )
   *        System.out.println( "X New file size incorrect (should be " + data.length +
   *                            ", but is " + (new File( filename )).length( ) + ")." );
   *     else
   *        System.out.println( ". File size correct (" + data.length + ")." );
   *
   *     // Reopen the file for reading.
   *     RandomAccessFile inFile = new RandomAccessFile(
   *                    filename, RandomAccessFile.READ, bufferSize );
   *
   *     // Read and check random small blocks of data.
   *     boolean verified = true;
   *     int firstFailure = 256;
   *     Random random = new Random( 0 );
   *     byte block[] = new byte[(int)(bufferSize * 0.5)];
   *     for( int i = 0; i < 100; i++ ) {
   *        int index = Math.abs( random.nextInt( ) ) % (data.length - block.length);
   *        inFile.seek( index );
   *        inFile.read( block );
   *
   *        // Verify the block of data.
   *        for( int j = 0; j < block.length; j++ ) {
   *           if( block[j] != data[index + j] ) {
   *              verified = false;
   *              if( index + j < firstFailure )
   *                 firstFailure = index + j;
   *           }
   *        }
   *     }
   *     if( verified )
   *        System.err.println( ". Reading small blocks verified." );
   *     else
   *        System.err.println( "X Reading small blocks failed (byte " + firstFailure + ")." );
   *
   *     // Read and check random large (bigger than the bufferSize) blocks
   *     // of data.
   *     verified = true;
   *     random = new Random( 0 );
   *     block = new byte[(int)(bufferSize * 1.5)];
   *     for( int i = 0; i < 100; i++ ) {
   *        int index = Math.abs( random.nextInt( ) ) % (data.length - block.length);
   *        inFile.seek( index );
   *        inFile.read( block );
   *
   *        // Verify the block of data.
   *        for( int j = 0; j < block.length; j++ ) {
   *           if( block[j] != data[j + index] )
   *              verified = false;
   *        }
   *     }
   *     if( verified )
   *        System.err.println( ". Reading large blocks verified." );
   *     else
   *        System.err.println( "X Reading large blocks failed." );
   *
   *     // Close the input file.
   *     inFile.close( );
   *
   *  } catch( Exception e ) {
   *     e.printStackTrace( );
   *  }
   *
   * }
   *
   *
   * Benchmark the performance of the new RandomAccessFile
   * class. Its speed is compared to that of a
   * java.io.RandomAccessFile, based on reading and writing a test
   * file, byte by byte.
   *
   * @param filename    the name of the test file.
   * @param bufferSize the buffer size to use.
   * public static void benchmark( String filename, int bufferSize ) {
   *  System.out.println( "\nBenchmarking..." );
   *
   *  // Start the clock, and open a file for reading and a file for writing.
   *  long time = (new Date( )).getTime( );
   *  try {
   *     RandomAccessFile inFile = new RandomAccessFile( filename,
   *        RandomAccessFile.READ, bufferSize );
   *     RandomAccessFile outFile = new RandomAccessFile( "temp.data",
   *        RandomAccessFile.WRITE |
   *        RandomAccessFile.CREATE, bufferSize );
   *
   *     // Copy one file to the other.
   *     try {
   *
   *        while( true ) {
   *           outFile.writeByte( inFile.readByte( ) );
   *        }
   *
   *     } catch( EOFException e ) {
   *     } catch( IOException e ) {
   *        e.printStackTrace( );
   *     } finally {
   *        inFile.close( );
   *        outFile.close( );
   *     }
   *     System.out.println( ". RandomAccessFile elapsed time=" +
   *                         ((new Date( )).getTime( ) - time) );
   *
   *     // Restart the clock, and open RandomAccessFiles for reading and writing.
   *     time = (new Date( )).getTime( );
   *     java.io.RandomAccessFile inFile2 = new java.io.RandomAccessFile( filename, "r" );
   *     java.io.RandomAccessFile outFile2 = new java.io.RandomAccessFile( "temp.data", "rw" );
   *
   *     // Copy one file to the other.
   *     try {
   *
   *        while( true ) {
   *           outFile2.writeByte( inFile2.readByte( ) );
   *        }
   *
   *     } catch( EOFException e ) {
   *     } catch( IOException e ) {
   *        e.printStackTrace( );
   *     } finally {
   *        inFile2.close( );
   *        outFile2.close( );
   *     }
   *
   *  } catch( Exception e ) {
   *     e.printStackTrace( );
   *  }
   *  System.out.println( ". java.io.RandomAccessFile elapsed time=" + ((new Date( )).getTime( ) - time) );
   * }
   *
   *
   * Test the RandomAccessFile class. This involves testing the byte
   * methods, the block methods, and benchmarking the performance. By appending
   * 'test' or 'benchmark' to the command-line, it can be limited to the tests
   * or benchmarking alone. The test filename is only used for the benchmarking,
   * the other tests create a file called "temp.data" in the current directory.
   * Note that the size of the buffer determines the size of the test file
   * (which is 4.5 times the size of the buffer).
   *
   * @param argv  Usage: <testFilename> [bufferSize] [test | benchmark]
   * @see #testBytes(Stringfilename,intbufferSize)
   * @see #testBlocks(Stringfilename)
   * @see #benchmark(Stringfilename,intbufferSize)
   *
   * public static void main( String argv[] ) {
   *
   *  int defaultPageSize = 4096;
   *
   *  // Parse the command-line arguments.
   *  String filename = null;
   *  int bufferSize = 0;
   *  boolean test = true;
   *  boolean benchmark = true;
   *  if( argv.length < 1 ) {
   *     System.err.println( "Usage: RandomAccessFile <filename> [buffer.length] [benchmark | test]" );
   *     System.exit( -1 );
   *  } else if( argv.length < 2 ) {
   *     filename = argv[0];
   *     bufferSize = defaultPageSize;
   *  } else if( argv.length < 3 ) {
   *     filename = argv[0];
   *     bufferSize = Integer.parseInt( argv[1] );
   *  } else {
   *     filename = argv[0];
   *     bufferSize = Integer.parseInt( argv[1] );
   *     if( argv[2].equals( "benchmark" ) )
   *        test = false;
   *     else if( argv[2].equals( "test" ) )
   *        benchmark = false;
   *  }
   *
   *  System.out.println( "\nRandomAccessFile\n" +
   *                        "========================" );
   *  System.out.println( "filename=" + filename +
   *                      ", bufferSize=" + bufferSize );
   *  System.out.println( "totalMemory=" +
   *     (Runtime.getRuntime( ).totalMemory( ) / 1000) + "k" +
   *     " freeMemory=" + (Runtime.getRuntime( ).freeMemory( ) / 1000) + "k" );
   *
   *  if( test ) {
   *     RandomAccessFile.testBytes( "temp.data", bufferSize );
   *     RandomAccessFile.testBlocks( "temp.data" );
   *  }
   *  if( benchmark ) {
   *     RandomAccessFile.benchmark( filename, bufferSize );
   *  }
   *
   *  System.out.println( "\nEND" );
   * }
   */

}

/* Change History:
   $Log: RandomAccessFile.java,v $
   Revision 1.33  2006/03/25 00:20:13  caron
   *** empty log message ***

   Revision 1.32  2006/03/09 22:18:47  caron
   bug fixes for sync, dods.

   Revision 1.31  2006/01/13 18:55:22  jeffmc
   fix javadoc errors

   Revision 1.30  2006/01/11 16:15:47  caron
   syncExtend
   N3iosp, FileWriter writes by record

   Revision 1.29  2005/10/16 20:46:39  caron
   remove debugging messages

   Revision 1.28  2005/10/15 23:59:50  caron
   fix bug in truncating non-netcdf3 files

   Revision 1.27  2005/10/11 19:39:52  caron
   RAF can be optionally cached
   HttpRAF detects if server supports range bytes

   Revision 1.26  2005/08/26 00:32:41  caron
   deal with NetCDF "non-canonical length" files

   Revision 1.25  2005/08/09 23:35:33  caron
   *** empty log message ***

   Revision 1.24  2005/07/29 00:30:54  caron
   null file when closing to help gc

   Revision 1.23  2005/07/25 00:07:06  caron
   cache debugging

   Revision 1.22  2005/04/18 23:45:57  caron
   _unsigned
   FileCache
   minFileLength

   Revision 1.21  2005/03/21 22:07:18  caron
   add setMinLength() method

   Revision 1.20  2005/01/14 23:20:48  caron
   seek to filePosition before write

   Revision 1.19  2005/01/14 21:41:37  caron
   *** empty log message ***

   Revision 1.18  2004/12/08 18:09:23  caron
   add isAtEndOfFile()

   Revision 1.17  2004/10/23 21:36:12  caron
   no message

   Revision 1.16  2004/10/22 00:50:46  caron
   fix long-standing bug in writeBytes()

   Revision 1.15  2004/10/20 23:23:15  caron
   add nexrad2 iosp

   Revision 1.14  2004/10/12 22:03:46  rkambic
   added   readInt3Bytes and readUint3Bytes

   Revision 1.13  2004/10/12 14:12:04  rkambic
   added getInt3Bytes()

   Revision 1.12  2004/10/12 02:57:06  caron
   refactor for grib1/grib2: move common functionality up to ucar.grib
   split GribServiceProvider

   Revision 1.11  2004/10/06 19:03:45  caron
   clean up javadoc
   change useV3 -> useRecordsAsStructure
   remove id, title, from NetcdfFile constructors
   add "in memory" NetcdfFile

   Revision 1.10  2004/10/02 20:56:03  caron
   keep track of location URL

   Revision 1.9  2004/09/24 02:32:02  caron
   grib2 reading

   Revision 1.8  2004/09/22 21:24:08  caron
   common io for nc22, grib, etc

   Revision 1.5  2004/08/16 21:41:36  caron
   *** empty log message ***

   Revision 1.3  2004/07/12 23:40:17  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:10  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

 */

