/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
