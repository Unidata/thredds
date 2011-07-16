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
