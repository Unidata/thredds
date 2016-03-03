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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by rmay on 8/10/15.
 */
public class NcStreamCompression {
    NcStreamProto.Compress type;
    Object compressInfo;

    private NcStreamCompression(NcStreamProto.Compress type, Object info) {
        this.type = type;
        this.compressInfo = info;
    }

    private NcStreamCompression(NcStreamProto.Compress type) {
        this(type, null);
    }

    public static NcStreamCompression none() {
        return new NcStreamCompression(NcStreamProto.Compress.NONE);
    }

    public static NcStreamCompression deflate() {
        return deflate(-1);
    }

    public static NcStreamCompression deflate(int level) {
        return new NcStreamCompression(NcStreamProto.Compress.DEFLATE, level);
    }

    public OutputStream setupStream(OutputStream out, int size)
            throws IOException
    {
        switch (type) {
            // For compression (currently deflate) we compress the data, then
            // will write the block size, and then data, when the stream is closed.
            case DEFLATE:
                // limit level to range [-1, 9], where -1 is default deflate setting.
                int level = Math.min(Math.max((Integer)compressInfo, -1), 9);
                int bufferSize = Math.min(size / 2, 512 * 1024 * 1024);
                return new NcStreamCompressedOutputStream(out, bufferSize, level);

            default:
                System.out.printf(" Unknown compression type %s. Defaulting to none.%n", type);

            // In the case of no compression, go ahead and write the block
            // size so that the stream is ready for data
            case NONE:
                NcStream.writeVInt(out, size);
                return out;
        }
    }
}