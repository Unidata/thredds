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


package opendap.dap;

import java.io.*;

/**
 * The HeaderInputStream filters the input to only read lines of text until
 * the "Data:" line.  This is required because overzealous buffering in the
 * DDSParser will read the data as well as the DDS otherwise.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see DConnect
 */
public class HeaderInputStream extends FilterInputStream {
    /**
     * Each line is buffered here.
     */
    private byte lineBuf[];

    /**
     * Number of bytes remaining in buffer.
     */
    private int bytesRemaining;

    /**
     * Current buffer offset.
     */
    private int currentOffset;

    /**
     * End sequence to look for: "\nData:\n"
     */
    private byte[] endSequence = {(byte) '\n', (byte) 'D', (byte) 'a', (byte) 't',
            (byte) 'a', (byte) ':', (byte) '\n'};

    /**
     * Flag when end sequence has been found
     */
    private boolean endFound;

    /**
     * Construct a new HeaderInputStream.
     */
    public HeaderInputStream(InputStream in) {
        super(in);
        lineBuf = new byte[4096];
        bytesRemaining = currentOffset = 0;
        endFound = false;

        //System.out.print("EndSequence: ");
        //for(int i=0; i<endSequence.length ;i++)
        //    System.out.print(endSequence[i]+"  ");
        //System.out.print("\n");
    }

    /**
     * Return the number of bytes in the buffer.
     */
    public int available() {
        return bytesRemaining;
    }

    /**
     * Returns that we don't support the mark() and reset() methods.
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Reads a single byte of data
     */
    public int read() throws IOException {
        // if the buffer is empty, get more bytes
        if (bytesRemaining == 0 && !endFound)
            getMoreBytes();
        // if the buffer is still empty, return EOF
        if (bytesRemaining == 0)
            return -1;
        else {
            bytesRemaining--;
            return lineBuf[currentOffset++];
        }
    }


    /**
     * Get more bytes into buffer.  Stop when endSequence is found.
     */
    private void getMoreBytes() throws IOException {
        currentOffset = 0;   // reset current array offset to 0
        int bytesRead = 0;   // bytes read so far
        int lookingFor = 0;  // character in endSequence to look for
        for (; bytesRead < lineBuf.length; bytesRead++) {
            int c = in.read();
            if (c == -1)
                break;  // break on EOL and return what we have so far

            lineBuf[bytesRead] = (byte) c;
            if (lineBuf[bytesRead] == endSequence[lookingFor]) {
                lookingFor++;
                if (lookingFor == endSequence.length) {
                    endFound = true;
                    break;
                }
            } else if (lineBuf[bytesRead] == endSequence[0]) { // CHANGED JC
                lookingFor = 1;
            } else {
                lookingFor = 0;
            }
        }
        bytesRemaining = bytesRead;  // number of bytes we've read
    }


    /**
     * Reads up to len bytes of data from this input stream into an array of
     * bytes. This method blocks until some input is available.
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }

        int c = read();
        if (c == -1)
            return -1;
        b[off] = (byte) c;

        // We've read one byte successfully, let's try for more
        int i = 1;
        try {
            for (; i < len; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte) c;
            }
        } catch (IOException e) {
        }
        return i;
    }

    /**
     * Skips over and discards n bytes of data from the input stream.
     */
    public long skip(long n) {
        if (bytesRemaining >= n) {
            bytesRemaining -= n;
            return n;
        } else {
            int oldBytesRemaining = bytesRemaining;
            bytesRemaining = 0;
            return oldBytesRemaining;
        }
    }
}


