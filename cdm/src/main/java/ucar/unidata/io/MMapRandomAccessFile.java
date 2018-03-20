/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
public class MMapRandomAccessFile extends RandomAccessFile {

  private MappedByteBuffer source;

  /**
    * Constructor for in-memory "files"
    * @param location used as a name
    * @param mode the open mode
    * @throws java.io.IOException on error
    */
  public MMapRandomAccessFile(String location, String mode) throws IOException {
    super(location, mode, 1);
    FileChannel channel = file.getChannel();
    source = channel.map( readonly ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE, (long) 0, channel.size());
    //channel.close();

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

   public int readBytes( byte dst[], int offset, int length ) throws IOException {
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