/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////



package opendap.servlet;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Deflater;


/**
 * The Servlet to exercise what's available.
 *
 * @author Nathan David Potter
 */

public class DummySink extends DeflaterOutputStream {

    int count = 0;

    /**
     * Creates a new output stream with the specified compressor and
     * buffer size.
     *
     * @param out the output stream
     * @param def the compressor ("deflater")
     * @throws IllegalArgumentException if size is <= 0
     */
    public DummySink(OutputStream out, Deflater def, int size) {
        super(out);
        count = 0;
    }

    /**
     * Creates a new output stream with the specified compressor and
     * a default buffer size.
     *
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
    public void close() {
    }


    public void flush() {
    }

    public void write(int b) throws IOException {
        count++;
        super.write(b);
    }

    /**
     * Writes an array of bytes to the compressed output stream. This
     * method will block until all the bytes are written.
     *
     * @param off the start offset of the data
     * @param len the length of the data
     * @throws IOException if an I/O error has occurred
     */
    public void write(byte[] b, int off, int len) throws IOException {

        count += len;
        super.write(b, off, len);

    }

    public int getCount() {
        return count;
    }

    public void setCount(int c) {
        count = c;
    }

    public void resetCount() {
        count = 0;
    }


}





