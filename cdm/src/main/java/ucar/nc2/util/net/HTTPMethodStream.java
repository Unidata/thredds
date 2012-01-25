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

package ucar.nc2.util.net;

import java.io.IOException;
import java.io.InputStream;

/**
 * THe goal of this class is to allow
 * other classes to access the data stream
 * associated with a method response.
 * It tracks the method and the session
 * to allow them to be closed
 * when the stream hits eof.
 */

public class HTTPMethodStream extends InputStream
{
    HTTPSession session = null;
    HTTPMethod method = null;
    InputStream methodstream = null;

    public HTTPMethodStream() {}

    public HTTPMethodStream(HTTPSession session, HTTPMethod method, InputStream methodstream)
    {
	this.session = session;
	this.method = method;
	this.methodstream = methodstream;
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * <p> A subclass must provide an implementation of this method.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if an I/O error occurs.
     */
    
    public int read() throws IOException
    {
	try {

	if(methodstream == null) return -1;
	int ch = methodstream.read();
	if(ch >= 0) return ch;
	// EOF
	close();
    return -1;

	} catch (IOException ioe) {
	    try {close(); } catch(IOException ie) {};
	    throw ioe;
	}
    }

    /**
       * Closes this input stream and releases any system resources associated
       * with the stream.
       *
       * <p> The <code>close</code> method of <code>InputStream</code> does
       * nothing.
       *
       * @exception  IOException  if an I/O error occurs.
       */
    @Override
    public void close() throws IOException
    {
        if(methodstream == null) methodstream.close();
        if(method != null) method.close();
        if(session != null) session.close();
    }


}
