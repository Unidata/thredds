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

package ucar.httpservices;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The goal of this class is to allow
 * other classes to access the data stream
 * associated with a method response.
 * It tracks the method and the session
 * to allow them to be closed
 * when the stream hits eof.
 * It also guarantees that any remaining unconsumed
 * input is consumed.
 *
 * Note that this class is not public in the package.
 *
 * Note that this code now includes the equivalent of
 * Tom Kunicki's proposed pull request, but with the HTTPMethod
 * close extension he proposes but does not implement.
 * Pull request: https://github.com/tkunicki-usgs/thredds/commit/3b750ec0016a137db66336adeac421a9202b9d30
 * Is this class needed in httpclient 4.5+ any more?
 *
 */

public class HTTPMethodStream extends FilterInputStream implements Closeable
{
    //////////////////////////////////////////////////////////////////////////
    static public org.slf4j.Logger log = HTTPSession.log;

    //////////////////////////////////////////////////////////////////////////
    HTTPMethod method = null;
    InputStream stream = null; // in case someone wants to retrieve it
    boolean closed = false;

    HTTPMethodStream(InputStream stream, HTTPMethod method)
    {
	super(stream);
	this.method = method;
	this.stream = stream;
    }

    boolean getClosed() {return closed;}

    InputStream getStream() {return stream;}

    /**
       * Closes this input stream and releases any system resources associated
       * with the stream; closes the method also.
       *
       * @exception  IOException  if an I/O error occurs; but not if close
       *                          is called twice.
       */
    @Override
    public void close() throws IOException
    {
        if(closed)
            return; /* Allow multiple close calls */
        closed = true;
        try {
            consume();
        } finally {
            super.close();
        }
        if(method != null) method.close();
    }

    void consume()
    {
        try {
            long consumed = 0;
            long available;
            while ((available = available()) > 0) {
            consumed += skip(available);
            }
            if (consumed > 0) {
                log.debug("HTTPMethodStream: unconsumed data");
            }
        } catch (IOException ioe) {/*ignore*/};
      }

    public boolean isClosed() {return closed;}
}
