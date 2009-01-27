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

package ucar.unidata.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import java.net.URL;


/**
 * Big-endian little-endian data input stream, from which numbers in
 * both big- and little-endian representations can be read.  This is
 * needed for ESRI shapefiles, for example, since they contain both
 * big- and little-endian representations.
 *
 * @author Russ Rew
 * @version $Id: BeLeDataInputStream.java 64 2006-07-12 22:30:50Z edavis $
 */
public class BeLeDataInputStream extends DataInputStream {

    /** work array for buffering bytes */
    private byte w[] = new byte[8]; 

    /** arbitrary, probably big enough */
    private static final int kLongs = 128;

    /** work space for longs */
    private long[] longWorkSpace = new long[kLongs];

    /** work space for bytes */
    private byte[] byteWorkSpace = new byte[8 * kLongs];

    /**
     *  Construct a bigEndian-littleEndian input stream from an input stream.
     *
     *  @param inputStream from which to read
     *
     * @throws IOException on read error
     */
    public BeLeDataInputStream(InputStream inputStream) throws IOException {
        super(inputStream);
    }


    /**
     *  Construct a bigEndian-littleEndian input stream from a file.
     *
     * @param filename of file
     *
     * @throws IOException on read error
     */
    public BeLeDataInputStream(String filename) throws IOException {
        this(new BufferedInputStream(new FileInputStream(filename)));
    }



    /**
     *  Construct a bigEndian-littleEndian input stream from a URL.
     *
     * @param url of remote data
     * @exception IOException if there was a problem reading the file.
     */
    public BeLeDataInputStream(URL url) throws IOException {
        this(new BufferedInputStream(new DataInputStream(url.openStream())));
    }


    /**
     * read an int in little endian format
     * @return int created from next 4 bytes in stream, in littleEndian order
     *
     * @throws IOException on read error
     */
    public int readLEInt() throws IOException {
        readFully(w, 0, 4);
        return (w[3] & 0xff) << 24 | (w[2] & 0xff) << 16 | (w[1] & 0xff) << 8
               | (w[0] & 0xff);
    }


    /**
     * read an int in little endian format
     * @return int created from next 4 bytes in stream, in littleEndian order
     *
     * @throws IOException on read error
     */
    public float readLEFloat() throws IOException {
        return Float.intBitsToFloat(readLEInt());
    }


    /**
     *  read a double in little endian format
     * @return double from next 8 bytes in stream, littleEndian order
     *
     * @throws IOException on read error
     */
    public double readLEDouble() throws IOException {
        return Double.longBitsToDouble(readLELong());
    }

    /**
     * Reads <code>n</code> little-endian doubles from a random access file.
     *
     * <p> This method is provided for speed when accessing a number
     * of consecutive values of the same type.
     *
     * @param d the buffer into which the doubles are read
     * @param n number of little-endian doubles to read
     *
     * @throws IOException on read error
     */
    public final void readLEDoubles(double[] d, int n) throws IOException {
        int nLeft   = n;
        int dCount  = 0;
        int nToRead = kLongs;
        while (nLeft > 0) {
            if (nToRead > nLeft) {
                nToRead = nLeft;
            }
            readLELongs(longWorkSpace, nToRead);
            for (int i = 0; i < nToRead; i++) {
                d[dCount++] = Double.longBitsToDouble(longWorkSpace[i]);
            }
            nLeft -= nToRead;
        }
    }


    /**
     *  read a long in little endian format
     * @return long from next 8 bytes in stream, littleEndian order
     *
     * @throws IOException on read error
     */
    public long readLELong() throws IOException {
        readFully(w, 0, 8);
        return (long) (w[7] & 0xff) << 56 | (long) (w[6] & 0xff) << 48
               | (long) (w[5] & 0xff) << 40 | (long) (w[4] & 0xff) << 32
               | (long) (w[3] & 0xff) << 24 | (long) (w[2] & 0xff) << 16
               | (long) (w[1] & 0xff) << 8 | (long) (w[0] & 0xff);
    }


    /**
     * Reads <code>n</code> little-endian longs from a random access file.
     *
     * <p> This method is provided for speed when accessing a number
     * of consecutive values of the same type.
     *
     * @param lbuf the buffer into which the longs are read
     * @param n the number of little-endian longs to read
     *
     * @throws IOException on read error
     */
    public final void readLELongs(long lbuf[], int n) throws IOException {
        int nLeft   = n;
        int lCount  = 0;
        int nToRead = kLongs;
        while (nLeft > 0) {
            if (nToRead > nLeft) {
                nToRead = nLeft;
            }
            readFully(byteWorkSpace, 0, 8 * nToRead);
            int j = 0;
            for (int i = 0; i < nToRead; i++) {
                lbuf[lCount++] = (long) (byteWorkSpace[j] & 0xff)
                                 | (long) (byteWorkSpace[j + 1] & 0xff) << 8
                                 | (long) (byteWorkSpace[j + 2] & 0xff) << 16
                                 | (long) (byteWorkSpace[j + 3] & 0xff) << 24
                                 | (long) (byteWorkSpace[j + 4] & 0xff) << 32
                                 | (long) (byteWorkSpace[j + 5] & 0xff) << 40
                                 | (long) (byteWorkSpace[j + 6] & 0xff) << 48
                                 | (long) (byteWorkSpace[j + 7] & 0xff) << 56;
                j += 8;
            }
            nLeft -= nToRead;
        }
    }
}
