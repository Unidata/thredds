/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class wraps a chunked source of databuffer
 * as an InputStream. It is mostly pass-thru
 * in the sense that reads on this class are
 * turned into a series of reads of the underlying stream.
 */

public class ChunkInputStream extends InputStream
{
    //////////////////////////////////////////////////
    // Constants

    static final int DFALTCHUNKSIZE = 0x00FFFFFF;

    static final byte CR8 = DapUtil.extract(DapUtil.UTF8.encode("\r"))[0];
    static final byte LF8 = DapUtil.extract(DapUtil.UTF8.encode("\n"))[0];

    //////////////////////////////////////////////////
    // Type Decls

    //////////////////////////////////////////////////
    // static variables

    //////////////////////////////////////////////////
    // static methods

    //////////////////////////////////////////////////
    // Type declarations

    enum State
    {
        ERROR,    // after error chunk
        END,     // after last databuffer chunk
        DATA,    // when at start of a databuffer chunk
        INDATA,    // when reading a databuffer chunk
        INITIAL; // before anything is written
    }

    //////////////////////////////////////////////////
    // Instance variable

    protected InputStream input = null;
    protected State state = State.INITIAL;
    protected RequestMode requestmode = null;

    protected ByteOrder localorder = null;
    protected ByteOrder remoteorder = null;
    protected boolean nochecksum = false;

    // State info for current chunk
    protected int flags = 0;
    protected int chunksize = 0;
    protected int avail = 0;


    //////////////////////////////////////////////////
    // Constructor(s)

    public ChunkInputStream(InputStream input, RequestMode requestmode)
    {
        this(input, requestmode, ByteOrder.nativeOrder());
    }

    public ChunkInputStream(InputStream input, RequestMode requestmode, ByteOrder order)
    {
        this.input = input;
        this.requestmode = requestmode;
        this.localorder = order;
    }

    //////////////////////////////////////////////////
    // Accessors

    public ByteOrder getHostByteOrder()
    {
        return this.localorder;
    }

    public ByteOrder getRemoteByteOrder()
    {
        return this.remoteorder;
    }

    public boolean getNoChecksum()
        {
            return this.nochecksum;
        }

    //////////////////////////////////////////////////

    /**
     * Read the DMR, trimmed.
     *
     * @return the DMR as a Java String
     */

    public String
    readDMR()
            throws DapException
    {
        try {
            if(state != State.INITIAL)
                throw new DapException("Attempt to read DMR twice");

            byte[] dmr8 = null;

            if(requestmode == RequestMode.DMR) {
                // The whole buffer is the dmr;
                // but we do not know the length
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int c;
                while((c = input.read()) >= 0) {
                    baos.write(c);
                }
                baos.close();
                dmr8 = baos.toByteArray();
            } else if(requestmode == RequestMode.DAP) {
                // Pull in the DMR chunk header
                if(!readHeader(input))
                    throw new DapException("Malformed chunk count");
                // Read the DMR databuffer
                dmr8 = new byte[this.chunksize];
                int red = read(dmr8, 0, this.chunksize);
                if(red < this.chunksize)
                    throw new DapException("Short chunk");
            } else
                assert false : "Internal error";

            // Convert DMR to a string
            String dmr = new String(dmr8, DapUtil.UTF8);
            // Clean it up
            dmr = dmr.trim();
            // Make sure it has trailing \r\n"
            if(dmr.endsWith("\r\n")) {
                // do nothing
            } else if(dmr.endsWith("\n"))
                dmr = dmr.substring(0,dmr.length()-2) + "\r\n";
            else
                dmr = dmr + "\r\n";

            // Figure out the endian-ness of the response
            this.remoteorder = (flags & DapUtil.CHUNK_LITTLE_ENDIAN) == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN;
            this.nochecksum = (flags & DapUtil.CHUNK_NOCHECKSUM) != 0;

            // Set the state
            if((flags & DapUtil.CHUNK_ERROR) != 0)
                state = State.ERROR;
            else if((flags & DapUtil.CHUNK_END) != 0)
                state = State.END;
            else
                state = State.DATA;
            return dmr; //return the DMR

        } catch (IOException ioe) {
            throw new DapException(ioe.getMessage());
        }
    }

    /**
     * Convert an error chunk to an exception
     *
     * @param document XML representation of the error
     * @throws DapException containing the contents of the error chunk
     */
    public void
    throwError(String document)
            throws ErrorException
    {
        throw new ErrorException("Error chunk encountered")
                .setDocument(document);
    }

    /**
     * Read an error chunk
     *
     * @return the error document as a string
     */
    public String
    readError()
            throws IOException
    {
        state = State.ERROR;
        // Read the error body databuffer
        byte[] bytes = new byte[this.chunksize];
        try {
            if(read(bytes, 0, this.chunksize) < this.chunksize)
                throw new ErrorException("Short chunk");
        } catch (IOException ioe) {
            throw new ErrorException(ioe);
        }
        String document = new String(bytes, DapUtil.UTF8);
        return document;
    }

    //////////////////////////////////////////////////
    // InputStream abstract methods

    /**
     * Reads the next byte of databuffer from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input databuffer is available, the end of the stream is detected,
     * or an exception is thrown.
     * <p>
     * Operates by loading chunk by chunk. If an error chunk is detected,
     * then return ErrorException (which is a subclass of IOException).
     *
     * @return the next byte of databuffer, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if an I/O error occurs.
     */
    public int
    read()
            throws IOException
    {
        if(requestmode == RequestMode.DMR)
            throw new UnsupportedOperationException("Attempt to read databuffer when DMR only"); // Runtime
        if(avail <= 0) {
            if((flags & DapUtil.CHUNK_END) != 0)
                return -1; // Treat as EOF
            if(!readHeader(input))
                return -1; // EOF
            // See if we have an error chunk,
            // and if so, turn it into an exception
            if((flags & DapUtil.CHUNK_ERROR) != 0) {
                String document = readError();
                throwError(document);
            }
        }
        avail--;
        return input.read();
    }

    //////////////////////////////////////////////////
    // InputStream method overrides

    /**
     * Reads up to len databuffer of databuffer from the input stream into an
     * array of databuffer. An attempt is made to read as many as len
     * databuffer, but a smaller number may be read. The number of databuffer
     * actually read is returned as an integer.
     *
     * @param buf the byte array into which databuffer is read
     * @param off the offset in the byte array at which to write
     * @param len the amount to read
     * @return the actual number of databuffer read
     * @throws IOException
     */

    public int
    read(byte[] buf, int off, int len)
            throws IOException
    {
        // Sanity check
        if(off < 0 || len < 0)
            throw new IndexOutOfBoundsException();// Runtime
        if(off >= buf.length || buf.length < (off + len))
            throw new IndexOutOfBoundsException(); //Runtime
        if(requestmode == RequestMode.DMR)
            throw new UnsupportedOperationException("Attempt to read databuffer when DMR only"); // Runtime

        // Attempt to read len bytes out of a sequence of chunks
        int count = len;
        int pos = off;
        while(count > 0) {
            if(avail <= 0) {
                if((flags & DapUtil.CHUNK_END) != 0
                        || !readHeader(input))
                    return (len - count); // return # databuffer read
                // See if we have an error chunk,
                // and if so, turn it into an exception
                if((flags & DapUtil.CHUNK_ERROR) != 0) {
                    String document = readError();
                    throwError(document);
                }
            } else {
                int actual = (this.avail < count ? this.avail : count);
                int red = input.read(buf, pos, actual);
                if(red < 0)
                    throw new IOException("Unexpected EOF");
                pos += red;
                count -= red;
                this.avail -= red;
            }
        }
        return len;
    }

    /**
     * Returns an estimate of the number of databuffer that can be read
     * (or skipped over) from this input stream without
     * blocking by the next invocation of a method for this
     * input stream.
     * <p>
     * Repurposed here to do equivalent of peek().
     *
     * @return 0 if at eof, some number > 0 otherwise.
     */

    public int
    available()
    {
        if(this.avail > 0) return this.avail;
        if((flags & DapUtil.CHUNK_END) != 0)
            return 0;
        return 1; // should be some unknown amount left.
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. The underlying servlet stream is
     * not closed; that is left to the level above.
     */

    public void close()
            throws IOException
    {
        state = State.END;
    }

    //////////////////////////////////////////////////
    // Utilities

    /**
     * Read the size+flags header from the input stream and use it to
     * initialize the chunk state
     *
     * @param input The input streamfrom which to read
     * @return true if header read false if immediate eof encountered
     */

    boolean
    readHeader(InputStream input)
            throws IOException
    {
        byte[] bytehdr = new byte[4];
        int red = input.read(bytehdr);
        if(red == -1) return false;
        if(red < 4)
            throw new IOException("Short binary chunk count");
        this.flags = ((int) bytehdr[0]) & 0xFF; // Keep unsigned
        bytehdr[0] = 0;
        ByteBuffer buf = ByteBuffer.wrap(bytehdr).order(ByteOrder.BIG_ENDIAN);
        this.chunksize = buf.getInt();
        this.avail = this.chunksize;
        return true;
    }

}