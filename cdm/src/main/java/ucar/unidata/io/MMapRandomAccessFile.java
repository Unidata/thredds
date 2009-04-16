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

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;

/**
 * Use NIO MappedByteBuffer to implement a RandomAccessFile.
 * Limited to 2G size. Not currently used - NOT UP TO DATE DO NOT USE
 * @author john
 */
class MMapRandomAccessFile extends RandomAccessFile {

  private MappedByteBuffer source;

  /**
    * Constructor for in-memory "files"
    * @param location used as a name
    * @param mode the open mode
    * @throws java.io.IOException on error
    */
  public MMapRandomAccessFile(String location, String mode ) throws IOException {
    super(location, mode, 1);
    FileChannel channel = file.getChannel();
    source = channel.map( readonly ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE, (long) 0, channel.size());
    channel.close();

    bufferStart = 0;
	  dataSize = (int) channel.size();
	  dataEnd = channel.size();
    filePosition = 0;
    buffer = null;
    endOfFile = false;
  }


  public void flush() throws IOException {
    if (bufferModified) {
      source.force();
      bufferModified = false;
    }
  }

  public void close() throws IOException {
    if (!readonly) flush();
    source = null;
  }

  public long length( ) {
    return dataEnd;
  }

  public void seek( long pos ) {
    filePosition = pos;
    if( filePosition < dataEnd )
      source.position((int) filePosition);
    else
      endOfFile = true;
  }

   public void unread( ) {
     seek( filePosition-1);
   }

  public int read() throws IOException {
    // If the file position is within the data, return the byte...
    if( filePosition < dataEnd ) {
      filePosition++;
      return source.get() & 0xff;

    } else {
       return -1;
    }
  }

   protected int readBytes( byte dst[], int offset, int length ) throws IOException {
     if( endOfFile ) return -1;

     length = (int) Math.min( length, dataEnd - filePosition);
     if (length > 0) {
       source.get(dst, offset, length);
       filePosition += length;
     }
     return length;
   }

  public void write( int b ) throws IOException {
     source.put( (byte)b);
     filePosition++;
     bufferModified = true;
  }

  public void writeBytes( byte dst[], int offset, int length ) throws IOException {
    source.put(dst, offset, length);
    filePosition += length;
  }

}