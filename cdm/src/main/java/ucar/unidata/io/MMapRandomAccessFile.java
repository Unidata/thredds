/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;

/**
 * Use NIO MappedByteBuffer to implement a RandomAccessFile.
 * Limited to 2G size. Not currently used
 * @author john
 */
public class MMapRandomAccessFile extends RandomAccessFile {

  private MappedByteBuffer source;
  private boolean debug = false;

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

    if (debug)
      System.out.println ("MMapRandomAccessFile opened file to read:'" + location+ "', size=" + dataEnd);
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