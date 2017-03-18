/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.data.ChecksumMode;
import dap4.core.data.DataCursor;
import dap4.core.dmr.DapType;
import dap4.core.dmr.DapVariable;
import dap4.core.dmr.TypeSort;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The SerialWriter class
 * is intended to provide the API
 * through which various kinds of
 * data are written into the ChunkWriter.
 * <p>
 * Ideally, this class should be completely
 * independent of CDM code so that
 * Non-NetcdfDataset writers can be accomodated.
 */

public class SerialWriter
{
    static public boolean DEBUG = false; // make it mutable
    static public boolean DUMPDATA = false; // make it mutable
    static public boolean DUMPCSUM = false; // make it mutable

    //////////////////////////////////////////////////
    // Constants

    //Define max size of a single integer type size
    static final int MAXINTOBJECTSIZE = 8;
    //Define max size of a single float type size
    static final int MAXFLOATOBJECTSIZE = 4;
    //Define max size of a single double type size
    static final int MAXDOUBLEOBJECTSIZE = 8;

    static final ByteOrder NETWORK_ORDER = ByteOrder.BIG_ENDIAN;
    static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();

    static final int COUNTSIZE = 8;

    //////////////////////////////////////////////////
    // Instance variables

    protected ByteOrder order = null;
    protected OutputStream output = null;
    protected int depth = 0;

    protected java.util.zip.Checksum checksum;
    protected ChecksumMode checksummode = null;
    protected boolean serialize = true; // false=>we do not need to actually serialize
    protected String lastchecksum = null; // checksum from last variable

    protected ByteBuffer crcbuffer = null;
    protected ByteBuffer countbuffer = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public SerialWriter(OutputStream output, ByteOrder order, ChecksumMode mode)
    {
        this.output = output;
        this.order = order;
        this.checksummode = mode;
        this.countbuffer = ByteBuffer.allocate(8) //8==sizeof(long)
                .order(order);
        this.crcbuffer = ByteBuffer.allocate(4) //4==sizeof(crc32 digest)
                .order(order);
        if("CRC32".equalsIgnoreCase(DapUtil.DIGESTER)) {
            // use the one from java.util.zip.CRC32
            this.checksum = new java.util.zip.CRC32();
        } else
            assert (false) : "No such checksum algorithm: " + DapUtil.DIGESTER;

    }

    //////////////////////////////////////////////////
    // Accessors

    public void noSerialize(boolean tf)
    {
        this.serialize = !tf;
    }

    public String getLastChecksum()
    {
        return this.lastchecksum.toString();
    }

    //////////////////////////////////////////////////
    // Encoding functions to encode into a bytebuffer

    /**
     * Encode an array of primitive values.
     *
     * @param vtype  The type of the object
     * @param values The value array
     * @return bytebuffer encoding of the array using the
     * platform's native encoding.
     */

    static public ByteBuffer
    encodeArray(DapType vtype, Object values, ByteOrder order)
            throws IOException
    {
        TypeSort atomtype = vtype.getAtomicType();
        assert values != null && values.getClass().isArray();
        int count = Array.getLength(values);
        int total = (int) TypeSort.getSize(atomtype) * count;
        ByteBuffer buf = ByteBuffer.allocate(total).order(order);
        switch (atomtype) {
        case Char:
            char[] datac = (char[]) values;
            for(int i = 0; i < datac.length; i++) {
                byte b = (byte) (0xFFL & (long) (datac[i]));
                buf.put(b);
            }
            break;
        case UInt8:
        case Int8:
            byte[] data8 = (byte[]) values;
            buf.put(data8);
            break;
        case Int16:
        case UInt16:
            short[] data16 = (short[]) values;
            buf.asShortBuffer().put(data16);
            buf.position(total); // because we are using asXXXBuffer
            break;
        case Int32:
        case UInt32:
            int[] data32 = (int[]) values;
            buf.asIntBuffer().put(data32);
            buf.position(total); // because we are using asXXXBuffer
            break;
        case Int64:
        case UInt64:
            long[] data64 = (long[]) values;
            buf.asLongBuffer().put(data64);
            buf.position(total); // because we are using asXXXBuffer
            break;
        case Float32:
            float[] dataf = (float[]) values;
            buf.asFloatBuffer().put(dataf);
            buf.position(total); // because we are using asXXXBuffer
            break;
        case Float64:
            double[] datad = (double[]) values;
            buf.asDoubleBuffer().put(datad);
            buf.position(total); // because we are using asXXXBuffer
            break;
        case URL:
        case String:
            // Convert the string to a counted UTF-8 bytestring
            String[] datas = (String[]) values;
            // Pass 1: get total size
            total = 0;
            for(int i = 0; i < datas.length; i++) {
                String content = datas[i];
                byte[] bytes = content.getBytes(DapUtil.UTF8);
                total += (bytes.length + COUNTSIZE);
            }
            buf = ByteBuffer.allocate(total).order(order);
            // Pass 2: write the strings
            for(int i = 0; i < datas.length; i++) {
                String content = datas[i];
                byte[] bytes = content.getBytes(DapUtil.UTF8);
                buf.putLong(bytes.length);
                buf.put(bytes);
            }
            break;

        case Opaque:
            // Unfortunately, Array.get1d does not produce
            // a ByteBuffer[].
            Object[] datao = (Object[]) values;
            // Pass 1: get total size
            total = 0;
            int size = 0;
            for(int i = 0; i < datao.length; i++) {
                ByteBuffer opaquedata = (ByteBuffer) datao[i];
                // the data may be at an offset in the buffer
                size = opaquedata.remaining(); // should be limit - pos
                total += (size + COUNTSIZE);
            }
            buf = ByteBuffer.allocate(total).order(order);
            // Pass 2: write the opaque elements
            for(int i = 0; i < datao.length; i++) {
                ByteBuffer opaquedata = (ByteBuffer) datao[i];
                size = opaquedata.remaining(); // should be limit - pos
                buf.putLong(size);
                int savepos = opaquedata.position();
                buf.put(opaquedata);
                opaquedata.position(savepos);
            }
            break;

        case Enum: // handled by getPrimitiveType() above
            assert false : "Unexpected ENUM type";
        default:
            throw new DapException("Unknown type: " + vtype.getTypeName());
        }
        return buf;
    }

    //////////////////////////////////////////////////
    // Dataset oriented writes

    public void
    startGroup()
    {
    }

    public void
    endGroup()
    {
    }

    //////////////////////////////////////////////////
    // Variable oriented writes

    public void
    startVariable()
    {
        if(depth == 0)
            this.checksum.reset();
        depth++;
    }

    public void
    endVariable()
            throws IOException
    {
        depth--;
        if(depth == 0 && this.checksummode.enabled(ChecksumMode.DAP)) {
            long crc = this.checksum.getValue(); // get the digest value
            crc = (crc & 0x00000000FFFFFFFFL); /* crc is 32 bits */
            crcbuffer.clear();
            crcbuffer.putInt((int) crc);
            byte[] csum = crcbuffer.array();
            assert csum.length == 4;
            // convert to a string; write as a signed integer
            this.lastchecksum = String.format("%08x", crc);
            if(DEBUG) {
                System.err.print("checksum = " + this.lastchecksum);
                System.err.println();
            }
            // Write out the digest in binary form
            // Do not use writeBytes because checksum is not itself checksummed
            outputBytes(csum, 0, DapUtil.CHECKSUMSIZE);
        }
    }

    //////////////////////////////////////////////////     
    // Write API

    /**
     * Write out a prefix count
     *
     * @param count the count to write out
     * @throws IOException
     */
    public void
    writeCount(long count)
            throws IOException
    {
        countbuffer.clear();
        countbuffer.putLong(count);
        byte[] countbuf = countbuffer.array();
        int len = countbuffer.position();
        writeBytes(countbuf, len);
        if(DEBUG) {
            System.err.printf("count: %d%n", count);
        }
    }

    /**
     * Write out an array of atomic values
     *
     * @param daptype type of the values
     * @param values the array of values
     * @throws IOException
     */
    public void
    writeAtomicArray(DapType daptype, Object values)
            throws IOException
    {
        assert values != null && values.getClass().isArray();
        ByteBuffer buf = SerialWriter.encodeArray(daptype, values, this.order);
        byte[] bytes = buf.array();
        int len = buf.position();
        writeBytes(bytes, len);
        if(DEBUG) {
            System.err.printf("%s: ", daptype.getShortName());
            for(int i = 0; i < len; i++) {
                int x = (int) (order == ByteOrder.BIG_ENDIAN ? bytes[i] : bytes[(len - 1) - i]);
                System.err.printf("%02x", (int) (x & 0xff));
            }
            System.err.println();
        }
    }

    /**
     * Write out a set of bytes
     *
     * @param bytes
     * @param len
     * @throws IOException
     */
    public void
    writeBytes(byte[] bytes, int len)
            throws IOException
    {
        outputBytes(bytes, 0, len);
        if(this.checksummode.enabled(ChecksumMode.DAP)) {
            this.checksum.update(bytes, 0, len);
            if(DUMPCSUM) {
                System.err.print("SSS ");
                for(int i = 0; i < len; i++) {
                    System.err.printf("%02x", bytes[i]);
                }
                System.err.println();
            }
        }
    }

    /**
     * Deliberate choke point for debugging
     *
     * @param bytes
     * @param start
     * @param count
     * @throws IOException
     */
    public void
    outputBytes(byte[] bytes, int start, int count)
            throws IOException
    {
        if(DUMPDATA) {
            System.err.printf("output %d/%d:", start, count);
            for(int i = 0; i < count; i++) {
                System.err.printf(" %02x", bytes[i]);
            }
            System.err.println("");
            System.err.flush();
        }
        output.write(bytes, start, count);
    }

    public void
    flush()
            throws IOException
    {
        output.flush();
    }


}//SerialWriter
