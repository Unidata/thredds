// $Id: InMemoryRandomAccessFile.java 64 2006-07-12 22:30:50Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

/**
 * @author john
 */
public class InMemoryRandomAccessFile extends RandomAccessFile {

   /**
    * Constructor for in-memory "files"
    * @param location used as a name
    * @param data the complete file
    */
   public InMemoryRandomAccessFile(String location, byte[] data)  {
     super(1);
     this.location = location;
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
       openFiles.add( location);
   }

   public long length( ) {
     return dataEnd;
   }

}