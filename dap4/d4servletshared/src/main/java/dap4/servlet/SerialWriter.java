/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.dmr.*;
import dap4.core.util.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * The SerialWriter class
 * is intended to provide the API
 * through which various kinds of
 * data are written into the ChunkWriter.
 * <p/>
 * Ideally, this class should be completely
 * independent of CDM code so that
 * Non-NetcdfDataset writers can be accomodated.
 */

public class SerialWriter
{
    static public boolean DEBUG = false; // make it mutable

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
    // Type declarations

    static enum State
    {
        INITIAL, DATASET, VARIABLE;
    }

    //////////////////////////////////////////////////
    // Instance variables

    ByteOrder order = null;
    OutputStream output = null;
    int depth = 0;
    State state = State.INITIAL;

    java.util.zip.Checksum checksum;
    boolean checksumming = true;
    boolean serialize = true; // false=>we do not need to actually serialize
    StringBuilder lastchecksum = new StringBuilder(); // checksum from last variable

    ByteBuffer longbuffer = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public SerialWriter(OutputStream output, ByteOrder order)
    {
        this.output = output;
        this.order = order;
        this.longbuffer = ByteBuffer.allocate(8) //8==sizeof(long)
            .order(order);
        if("CRC32".equalsIgnoreCase(DapUtil.DIGESTER)) {
            // use the one from java.util.zip.CRC32
            checksum = new java.util.zip.CRC32();
        } else
            assert (false) : "No such checksum algorithm: " + DapUtil.DIGESTER;

    }

    //////////////////////////////////////////////////
    // Accessors

    public void computeChecksums(boolean tf)
    {
        this.checksumming = tf;
    }

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
     * @param vtype The type of the object
     * @param value The value
     * @return bytebuffer encoding of the value using the
     *         platform's native encoding.
     */
    public ByteBuffer
    encodeObject(DapType vtype, Object value)
        throws IOException
    {
        AtomicType atomtype = vtype.getPrimitiveType();
        int total = (int) AtomicType.getSize(atomtype);
        ByteBuffer buf = null;
        if(atomtype.isFixedSize())
            buf = ByteBuffer.allocate(total).order(order);
        switch (atomtype) {
        case Char:
            byte b = (byte) (0xFFL & (long) ((Character) value).charValue());
            buf.put(b);
            break;
        case UInt8:
        case Int8:
            buf.put((byte) (Byte) value);
            break;
        case Int16:
        case UInt16:
            buf.putShort((Short) value);
            break;
        case Int32:
        case UInt32:
            buf.putInt((Integer) value);
            break;
        case Int64:
        case UInt64:
            buf.putLong((Long) value);
            break;
        case Float32:
            buf.putFloat((Float) value);
            break;
        case Float64:
            buf.putDouble((Double) value);
            break;

        case URL:
        case String:
            // Convert the string to a counted UTF-8 bytestring
            String content = value.toString();
            byte[] bytes = content.getBytes(DapUtil.UTF8);
            buf = ByteBuffer.allocate(bytes.length + COUNTSIZE)
                .order(order);
            buf.putLong(bytes.length);
            buf.put(bytes);
            break;

        case Opaque:
            ByteBuffer opaquedata = (ByteBuffer) value;
            // the data may be at an offset in the buffer
            int size = opaquedata.remaining(); // should be limit - pos
            buf = ByteBuffer.allocate(size + COUNTSIZE)
                .order(order);
            buf.putLong(size);
            buf.put(opaquedata);
            break;

        case Enum: // handled by getPrimitiveType() above
            assert false : "Unexpected ENUM type";
        default:
            throw new DapException("Unknown type: " + vtype.getTypeName());
        }
        return buf;
    }


    /**
     * Encode an array of primitive values.
     *
     * @param vtype    The type of the object
     * @param values   The value array
     * @return bytebuffer encoding of the array using the
     *         platform's native encoding.
     */

    public ByteBuffer
    encodeArray(DapType vtype, Object values)
        throws IOException
    {
        AtomicType atomtype = vtype.getPrimitiveType();
        int count = Array.getLength(values);
        int total = (int) AtomicType.getSize(atomtype) * count;
        ByteBuffer buf = null;
        if(atomtype.isFixedSize())
            buf = ByteBuffer.allocate(total).order(order);
        switch (atomtype) {
        case Char:
            char[] datac = (char[]) values;
            for(int i = 0;i < datac.length;i++) {
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
            for(int i = 0;i < datas.length;i++) {
                String content = datas[i];
                byte[] bytes = content.getBytes(DapUtil.UTF8);
                total += (bytes.length + COUNTSIZE);
            }
            buf = ByteBuffer.allocate(total).order(order);
            // Pass 2: write the strings
            for(int i = 0;i < datas.length;i++) {
                String content = datas[i];
                byte[] bytes = content.getBytes(DapUtil.UTF8);
                buf.putLong(bytes.length);
                buf.put(bytes);
            }
            break;

        case Opaque:
            ByteBuffer[] datao = (ByteBuffer[]) values;
            // Pass 1: get total size
            total = 0;
            int size = 0;
            ByteBuffer opaquedata = null;
            for(int i = 0;i < datao.length;i++) {
                opaquedata = datao[i];
                // the data may be at an offset in the buffer
                size = opaquedata.remaining(); // should be limit - pos
                total += (size + COUNTSIZE);
            }
            buf = ByteBuffer.allocate(total).order(order);
            // Pass 2: write the opaque elements
            for(int i = 0;i < datao.length;i++) {
                opaquedata = datao[i];
                size = opaquedata.remaining(); // should be limit - pos
                buf.putLong(size);
                buf.put(opaquedata);
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
    startDataset()
    {
        assert (state == State.INITIAL) : "Internal Error";
        state = State.DATASET;
    }

    public void
    endDataset()
    {
        assert (state == State.DATASET && depth == 0) : "Internal Error";
        state = State.INITIAL;
    }

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
        assert ((state == State.DATASET && depth == 0)
            || (state == State.VARIABLE && depth > 0)) : "Internal Error";
        state = State.VARIABLE;
        if(depth == 0)
            checksum.reset();
        depth++;
    }

    public void
    endVariable()
        throws IOException
    {
        assert (state == State.VARIABLE) : "Internal Error";
        depth--;
        if(depth == 0 && checksumming) {
            long digest = checksum.getValue(); // get the digest value
            longbuffer.clear();
            longbuffer.putLong(digest);
            byte[] csum = longbuffer.array();
            // convert to a string
            this.lastchecksum.setLength(0);
            // by experiment; checksum leads in buffer
            for(int i = 0;i < DapUtil.CHECKSUMSIZE;i++)
                this.lastchecksum.append(Escape.toHex((int) (csum[i] & 0xff)));
            if(DEBUG) {
                System.err.print("checksum = " + this.lastchecksum.toString());
                System.err.println();
            }
            // Write out the digest in binary form
            output.write(csum, 0, DapUtil.CHECKSUMSIZE);
            state = State.DATASET;
        }
    }

    //////////////////////////////////////////////////     
    // Write API

    /**
     * Write out a single object
     *
     * @param daptype the type of the object
     * @param value   the object to write out
     * @throws IOException
     */
    public void
    writeObject(DapType daptype, Object value)
        throws IOException
    {
        ByteBuffer buf = encodeObject(daptype, value);
        byte[] bytes = buf.array();
        int len = buf.position();
        if(checksumming)
            checksum.update(bytes, 0, len);
        output.write(bytes, 0, len);
        if(DEBUG) {
            System.err.printf("%s: ", daptype.getShortName());
            for(int i = 0;i < len;i++) {
                int x = (int) (order == ByteOrder.BIG_ENDIAN ? bytes[i] : bytes[(len - 1) - i]);
                System.err.printf("%02x", (int) (x & 0xff));
            }
            System.err.println();
        }
    }

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
        longbuffer.clear();
        longbuffer.putLong(count);
        byte[] countbuf = longbuffer.array();
        int len = longbuffer.position();
        output.write(countbuf, 0, len);
        if(DEBUG) {
            System.err.printf("count: %d\n", count);
        }
    }

    /**
     * Write out an array of values
     *
     * @param daptype  the type of the object
     * @throws IOException
     */
    public void
    writeArray(DapType daptype, Object values)
        throws IOException
    {
        int count = Array.getLength(values);
        ByteBuffer buf = encodeArray(daptype, values);
        byte[] bytes = buf.array();
        int len = buf.position();
        if(checksumming)
            checksum.update(bytes, 0, len);
        output.write(bytes, 0, len);
        if(DEBUG) {
            System.err.printf("%s: ", daptype.getShortName());
            for(int i = 0;i < len;i++) {
                int x = (int) (order == ByteOrder.BIG_ENDIAN ? bytes[i] : bytes[(len - 1) - i]);
                System.err.printf("%02x", (int) (x & 0xff));
            }
            System.err.println();
        }
    }

}//SerialWriter
