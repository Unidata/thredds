/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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





