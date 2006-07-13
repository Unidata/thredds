/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@oce.orst.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//         
/////////////////////////////////////////////////////////////////////////////


/* $Id: DummySink.java 51 2006-07-12 17:13:13Z caron $
*
*/


package dods.servlet;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Deflater;


/**
* The Servlet to exercise what's available.
*
* @author Nathan David Potter
*/

public class DummySink extends DeflaterOutputStream  {

	int count = 0;
	
	    /**
     * Creates a new output stream with the specified compressor and
     * buffer size.
     * @param out the output stream
     * @param def the compressor ("deflater")
     * @param len the output buffer size
     * @exception IllegalArgumentException if size is <= 0
     */
    public DummySink(OutputStream out, Deflater def, int size) {
        super(out);
		count = 0;
      }

    /**
     * Creates a new output stream with the specified compressor and
     * a default buffer size.
     * @param out the output stream
     * @param def the compressor ("deflater")
     */
    public DummySink(OutputStream out, Deflater def) {
        this(out, def, 512);
    }
    
       /**
     * Creates a new output stream with a defaul compressor and buffer size.
     */
    public DummySink(OutputStream out) {
        this(out, new Deflater());
    }
	
	//Closes this output stream and releases any system resources associated with this stream.
	public void close() {}

	 
	public void flush(){}

	public void  write(int b) throws IOException{
		count++;
		super.write(b);
	}
    /**
     * Writes an array of bytes to the compressed output stream. This
     * method will block until all the bytes are written.
     * @param buf the data to be written
     * @param off the start offset of the data
     * @param len the length of the data
     * @exception IOException if an I/O error has occurred
     */
    public void write(byte[] b, int off, int len)throws IOException{
    
    	count += len;
		super.write(b,off,len);
    
    }
	public int getCount(){
		return count;
	}

	public void setCount(int c){
		count = c;
	}
	public void resetCount(){
		count = 0;
	}


}



