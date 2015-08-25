/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.stream;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by rmay on 8/13/15.
 */
public class NcStreamCompressedOutputStream extends DataOutputStream {
    private OutputStream writer;
    private DeflaterOutputStream dout;
    private ByteArrayOutputStream buffer;

    public NcStreamCompressedOutputStream(OutputStream out, int bufferSize,
                                          int level) {
        super(out);

        // Save the original out for use when we flush
        writer = out;

        // write to an internal buffer, so we can find out the size when
        // compressed
        buffer = new ByteArrayOutputStream(bufferSize);
        dout = new DeflaterOutputStream(buffer, new Deflater(level), 4 * 1024);

        // Override out to point to our compressed stream
        this.out = new BufferedOutputStream(dout, 1024 * 1024);
    }

    @Override
    public void flush() throws IOException {
        // Make sure we flush out our stream
        out.flush();

        // Have to finish the deflater in order to get proper block.
        dout.finish();

        // Grab size of compressed data, write this out and then the block
        // of compressed data. Set number of bytes written to this value.
        int compressedSize = buffer.size();
        written = compressedSize;
        written += NcStream.writeVInt(writer, compressedSize);
        buffer.writeTo(writer);

        // Reset buffer so that in theory we could continue to write to this
        // stream.
        buffer.reset();
    }
}
