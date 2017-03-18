/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.dmr.ErrorResponse;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.DapCodes;
import dap4.dap4lib.RequestMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ChunkWriter extends OutputStream
{
    //////////////////////////////////////////////////
    // Constants

    static public boolean DEBUG = false;
    static public boolean DUMPDATA = false;
    static public boolean DEBUGHEADER = false;

    static final int MAXCHUNKSIZE = 0xFFFF;

    static final long DEFAULTWRITELIMIT = 1 * 1000000;

    static final int SIZEOF_INTEGER = 4;

    static public final byte[] CRLF8 = DapUtil.extract(DapUtil.UTF8.encode(DapUtil.CRLF));

    static public final String XMLDOCUMENTHEADER
              = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    //////////////////////////////////////////////////
    // Type declarations

    protected enum State
    {
        ERROR,    // after error chunk
        END,     // after last data chunk
        DATA,    // after at least 1 data chunk
        DMR,     // After DMR
        INITIAL; // before anything is written
    }

    //////////////////////////////////////////////////
    // Instance variable

    protected OutputStream saveoutput = null; // save the target stream if we are debugging
    protected OutputStream output = null;
    protected State state = State.INITIAL;

    protected int maxbuffersize = MAXCHUNKSIZE;
    protected long writelimit = DEFAULTWRITELIMIT; // Max amount we will accept
    protected long writecount = 0; // actual amount written so far
    protected ByteBuffer chunk = null; // give caller a chance to set the max buffersize
    protected ByteBuffer header = null;
    protected ByteOrder writeorder = null;
    protected RequestMode mode = null;
    protected byte[] dmr8 = null; // dmr in utf-8 form
    protected boolean closed = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public ChunkWriter(OutputStream output, RequestMode mode, ByteOrder writeorder)
            throws IOException
    {
        if(DUMPDATA || DEBUG) {
            this.saveoutput = output;
            this.output = new ByteArrayOutputStream();
        } else
            this.output = output;
        setWriteOrder(writeorder);
        setMode(mode);
        // Header must always go out in network order (aka big-endian)
        header = ByteBuffer.allocate(SIZEOF_INTEGER).order(ByteOrder.BIG_ENDIAN);
    }

    //////////////////////////////////////////////////
    // Accessors

    public RequestMode getMode()
    {
        return this.mode;
    }

    public void setMode(RequestMode mode)
    {
        this.mode = mode;
    }

    public void setBufferSize(int maxsize)
    {
        if(maxsize < 0 || maxsize > MAXCHUNKSIZE)
            return;
        maxbuffersize = maxsize;
    }

    public ByteOrder getWriteOrder()
    {
        return this.writeorder;
    }

    public void setWriteOrder(ByteOrder writeorder)
    {
        this.writeorder = writeorder;
    }

    public void setWriteLimit(long limit)
    {
        writelimit = limit;
    }


    //////////////////////////////////////////////////

    /**
     * Write the DSR; do not bother to cache.
     *
     * @param dsr The DSR string
     * @throws IOException on IO related errors
     */

    public void
    writeDSR(String dsr)
            throws IOException
    {
        if(state != State.INITIAL)
            throw new DapException("Attempt to write DSR twice");

        if(dsr == null)
            throw new DapException("Attempt to write empty DSR");

        // Strip off any trailing sequence of CR or LF.
        int len = dsr.length();
        while(len > 0) {
            char c = dsr.charAt(len - 1);
            if(c != '\r' && c != '\n') break;
            len--;
        }
        if(dsr.length() == 0)
            throw new DapException("Attempt to write empty DSR");

        dsr = dsr.substring(0, len) + DapUtil.CRLF;

        // Add <?xml...?> prefix
        dsr = XMLDOCUMENTHEADER + "\n" + dsr;

        // Convert the dsr to UTF-8 and then to byte[]
        byte[] dsr8 = DapUtil.extract(DapUtil.UTF8.encode(dsr));
        sendDXR(dsr8);
        state = State.END;
    }

    /**
     * Cache the DMR. What it really does is
     * cache the DMR and write it at the point
     * where it is needed; either in close(), if writing
     * the DMR only, or in writeChunk() if writing data as well.
     *
     * @param dmr The DMR string
     * @throws IOException on IO related errors
     */

    public void
    cacheDMR(String dmr)
            throws IOException
    {
        if(state != State.INITIAL)
            throw new DapException("Attempt to write DMR twice");

        if(dmr == null)
            throw new DapException("Attempt to write empty DMR");

        // Strip off any trailing sequence of CR or LF.
        int len = dmr.length();
        while(len > 0) {
            char c = dmr.charAt(len - 1);
            if(c != '\r' && c != '\n') break;
            len--;
        }
        if(dmr.length() == 0)
            throw new DapException("Attempt to write empty DMR");

        dmr = dmr.substring(0, len) + DapUtil.CRLF;

        // Prepend the <?xml...?> prefix
        dmr = XMLDOCUMENTHEADER + "\n" + dmr;

        // Convert the dmr to UTF-8 and then to byte[]
        this.dmr8 = DapUtil.extract(DapUtil.UTF8.encode(dmr));

        state = State.DMR;
    }

    /**
     * Output the specifiedd DMR or DSR or..., but xml only.
     *
     * @throws IOException on IO related errors
     */

    void
    sendDXR(byte[] dxr8)
            throws IOException
    {
        if(dxr8 == null || dxr8.length == 0)
            return; // do nothing

        if(mode == RequestMode.DMR || mode == RequestMode.DSR) {
            state = State.END;
        } else {//mode == DATA
            // Prefix with chunk header
            int flags = DapUtil.CHUNK_DATA;
            if(this.writeorder == ByteOrder.LITTLE_ENDIAN)
                flags |= DapUtil.CHUNK_LITTLE_ENDIAN;
            chunkheader(dxr8.length, flags, this.header);
            // write the header
            output.write(DapUtil.extract(this.header));
            state = State.DATA;
        }
        // write the DXR
        output.write(dxr8);
        output.flush();
    }

    /**
     * Write an error chunk.
     * If mode == DMR then replaces the dmr
     * else reset the current chunk thus
     * losing any partial write.
     *
     * @param httpcode The httpcode, 0 => ignore
     * @param msg      The <Error> <Message>, null => ignore
     * @param cxt      The <Error> <Context>, null => ignore
     * @param other    The <Error> <OtherInformation>, null => ignore
     * @throws IOException on IO related errors
     */
    public void
    writeError(int httpcode,
               String msg,
               String cxt,
               String other)
            throws IOException
    {
        dmr8 = null;
        ErrorResponse response = new ErrorResponse(httpcode, msg, cxt, other);
        String errorbody = response.buildXML();
        // Convert the error body into utf8 then to byte[]
        byte[] errbody8 = DapUtil.extract(DapUtil.UTF8.encode(errorbody));
        if(mode == RequestMode.DMR) {
            sendDXR(errbody8);
        } else {//mode == DATA
            // clear any partial chunk
            chunk.clear();
            // create an error header
            int flags = DapUtil.CHUNK_ERROR | DapUtil.CHUNK_END;
            chunkheader(errbody8.length, flags, header);
            output.write(DapUtil.extract(header));
            output.write(errbody8);
            output.flush();
        }
        state = State.ERROR;
    }


    /**
     * Write out the current chunk (with given set of flags).
     *
     * @param flags The flags for the header
     * @throws IOException on IO related errors
     */
    void writeChunk(int flags)
            throws IOException
    {
        // If flags indicate CHUNK_END
        // and amount to write is zero,
        // go ahead and write the zero size chunk.
        if(chunk == null)
            chunk = ByteBuffer.allocate(maxbuffersize);

        int buffersize = chunk.position();
        chunkheader(buffersize, flags, header);
        // output the header followed by the data (if any)
        // Zero size chunk is ok.
        output.write(DapUtil.extract(header));
        if(buffersize > 0)
            output.write(chunk.array(), 0, buffersize);
        chunk.clear();// reset
    }


    static public void
    chunkheader(int length, int flags, ByteBuffer hdrbuf)
            throws DapException
    {
        if(length > MAXCHUNKSIZE || length < 0)
            throw new DapException("Illegal chunk size: " + length);
        int hdr = ((flags << 24) | length);
        hdrbuf.clear();
        hdrbuf.putInt(hdr);
        if(DEBUGHEADER)
            System.err.printf("header: flags=%x length=%d header=%08x%n",
                    flags, length, hdr);
    }

    void verifystate()
            throws DapException
    {
        // Verify that we are in a proper state to write data
        switch (state) {
        case INITIAL:
            throw new DapException("Attempt to write data before DMR");
        case END:
            throw new DapException("Attempt to write data after END");
        case ERROR:
            throw new DapException("Attempt to write data after ERROR");
        case DMR:
        case DATA:
            break;
        }
    }

    //////////////////////////////////////////////////
    // OutputStream API

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. Except, the underlying stream is not
     * actually closed; that is left to the servlet level
     *
     * @throws IOException on IO related errors
     */

    public void close()
            throws IOException
    {
        if(closed)
            return;
        closed = true;

        if(dmr8 != null) {
            sendDXR(dmr8);
            dmr8 = null;
        }

        if(mode == RequestMode.DMR)
            return; // only DMR should be sent

        // If there is no partial chunk to write then
        // we are done; else verify we can write and write the last
        // chunk; => multiple closes are ok.
        if(chunk == null || chunk.position() == 0)
            return;

        // There is data left to write.
        verifystate(); // are we in a state supporting data write?

        // Force out the current chunk (might be empty)
        // but do not close the underlying output stream
        state = State.DATA; // pretend

        int flags = DapUtil.CHUNK_END;
        writeChunk(flags);
        state = State.END;
        this.output.flush(); // Do not close
        if(this.saveoutput != null) {
            // write to true output target
            this.saveoutput.write(((ByteArrayOutputStream) this.output).toByteArray());
        }
    }

    public byte[]
    getDump()
    {
        if((DUMPDATA || DEBUG) && this.output != null)
            return ((ByteArrayOutputStream) this.output).toByteArray();
        else
            return null;
    }

    /**
     * Overload flush to also write out the DMR
     *
     * @throws IOException
     */
    @Override
    public void
    flush()
            throws IOException
    {
        if(mode == RequestMode.DMR)
            return; // leave to close() to do this
        if(dmr8 != null) {
            sendDXR(dmr8);
            dmr8 = null;
        }
    }

    /**
     * Writes b.length bytes from the specified byte array
     * to this output stream.
     *
     * @param b the data
     * @throws IOException if an I/O error occurs
     */

    @Override
    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }

    /**
     * Writes the specified byte to the chunk
     *
     * @param b the byte to write
     * @throws IOException if an I/O error occurs
     */

    @Override
    public void write(int b)
            throws IOException
    {
        byte[] buf = new byte[1];
        buf[0] = (byte) (b & 0xff);
        write(buf, 0, 1);
    }

    /**
     * Writes len bytes from the specified byte array starting at
     * offset off to this output stream.
     * <p>
     * If this write fills up the chunk buffer,
     * then write out the buffer and put
     * the remaining bytes into the reset buffer.
     *
     * @param b   the data
     * @param off start point in b from which to write data
     * @param len number of bytes to write
     * @throws IOException if an I/O error occurs
     */

    @Override
    public void write(byte[] b, int off, int len)
            throws IOException
    {
        verifystate();
        if(writecount + len >= writelimit)
            throw new DapException("Attempt to write too much data: limit=" + writecount + len)
                    .setCode(DapCodes.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        if(chunk == null)
            chunk = ByteBuffer.allocate(maxbuffersize).order(getWriteOrder());
        if(state == State.DMR) {
            chunk.clear(); // reset
            state = State.DATA;
        }
        assert (state == State.DATA);
        if(b.length < off + len)
            throw new BufferUnderflowException();
        int left = len;
        int offset = off;
        while(left > 0) {
            int avail = chunk.remaining();
            do {
                if(avail == 0) {
                    writeChunk(DapUtil.CHUNK_DATA);
                    avail = chunk.remaining();
                }
                int towrite = (left < avail ? left : avail);
                chunk.put(b, off, towrite);
                left -= towrite;
                avail -= towrite;
            } while(left > 0);
        }
        writecount += len;
    }
}
