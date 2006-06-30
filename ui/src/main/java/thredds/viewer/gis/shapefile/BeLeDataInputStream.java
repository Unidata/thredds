// $Id: BeLeDataInputStream.java,v 1.2 2004/09/24 03:26:37 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.viewer.gis.shapefile;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Big-endian little-endian data input stream, from which numbers in
 * both big- and little-endian representations can be read.  This is
 * needed for ESRI shapefiles, for example, since they contain both
 * big- and little-endian representations.
 *
 * @author Russ Rew
 * @version $Id: BeLeDataInputStream.java,v 1.2 2004/09/24 03:26:37 caron Exp $ */
public class BeLeDataInputStream extends DataInputStream
{
    private byte w[] = new byte[8]; // work array for buffering bytes
    private static final int kLongs = 128; // arbitrary, probably big enough
    private long[] longWorkSpace = new long[kLongs];
    private byte[] byteWorkSpace = new byte[8*kLongs];

    /**
     *  Construct a bigEndian-littleEndian input stream from an input stream.
     *
     *  @param inputStream from which to read
     **/
    public BeLeDataInputStream(InputStream inputStream)
        throws IOException
    {
        super(inputStream);
    }


    /**
     *  Construct a bigEndian-littleEndian input stream from a file.
     *
     * @param filename of file
     **/
    public BeLeDataInputStream(String filename)
        throws IOException
    {
        this(new BufferedInputStream(new FileInputStream(filename)));
    }



    /**
     *  Construct a bigEndian-littleEndian input stream from a URL.
     *
     * @param URL of remote data
     * @exception IOException if there was a problem reading the file.
     **/
    public BeLeDataInputStream(URL url)
        throws IOException
    {
      this(new BufferedInputStream(new DataInputStream(url.openStream())));
    }


    /**
     * read an int in little endian format
     * @return int created from next 4 bytes in stream, in littleEndian order
     **/
    public int readLEInt()
        throws IOException
    {
        readFully(w, 0, 4);
        return
            (w[3]&0xff) << 24 |
            (w[2]&0xff) << 16 |
            (w[1]&0xff) <<  8 |
            (w[0]&0xff);
    }

    /**
     *  read a double in little endian format
     * @return double from next 8 bytes in stream, littleEndian order
     **/
    public double readLEDouble()
        throws IOException
    {
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
     */
    public final void readLEDoubles(double[] d, int n) throws IOException
    {
        int nLeft = n;
        int dCount = 0;
        int nToRead = kLongs;
        while (nLeft > 0) {
            if (nToRead > nLeft)
                nToRead = nLeft;
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
     **/
    public long readLELong()
        throws IOException
    {
        readFully(w, 0, 8);
        return
            (long)(w[7]&0xff) << 56 |
            (long)(w[6]&0xff) << 48 |
            (long)(w[5]&0xff) << 40 |
            (long)(w[4]&0xff) << 32 |
            (long)(w[3]&0xff) << 24 |
            (long)(w[2]&0xff) << 16 |
            (long)(w[1]&0xff) <<  8 |
            (long)(w[0]&0xff);
    }


    /**
     * Reads <code>n</code> little-endian longs from a random access file.
     *
     * <p> This method is provided for speed when accessing a number
     * of consecutive values of the same type.
     *
     * @param lbuf the buffer into which the longs are read
     * @param n the number of little-endian longs to read
     */
    public final void readLELongs(long lbuf[], int n) throws IOException
    {
        int nLeft = n;
        int lCount = 0;
        int nToRead = kLongs;
        while (nLeft > 0) {
            if (nToRead > nLeft)
                nToRead = nLeft;
            readFully(byteWorkSpace, 0, 8*nToRead);
            int j = 0;
            for(int i=0; i < nToRead; i++) {
                lbuf[lCount++] =
                    (long)(byteWorkSpace[j    ] & 0xff)       |
                    (long)(byteWorkSpace[j + 1] & 0xff) <<  8 |
                    (long)(byteWorkSpace[j + 2] & 0xff) << 16 |
                    (long)(byteWorkSpace[j + 3] & 0xff) << 24 |
                    (long)(byteWorkSpace[j + 4] & 0xff) << 32 |
                    (long)(byteWorkSpace[j + 5] & 0xff) << 40 |
                    (long)(byteWorkSpace[j + 6] & 0xff) << 48 |
                    (long)(byteWorkSpace[j + 7] & 0xff) << 56;
                j += 8;
            }
            nLeft -= nToRead;
        }
    }
}
