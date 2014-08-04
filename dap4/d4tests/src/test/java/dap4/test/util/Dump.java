/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.test.util;

import dap4.core.util.DapUtil;
import dap4.core.util.Escape;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class Dump
{

    //////////////////////////////////////////////////
    // Constants

    static final String LBRACE = "{";
    static final String RBRACE = "}";

    static final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    //////////////////////////////////////////////////
    // Type decls

    // Place to insert the command list
    static public interface Commands
    {
        public void run(Dump printer) throws IOException;
    }

    //////////////////////////////////////////////////
    // Instance databuffer

    public InputStream reader = null;
    public boolean checksumming = true;
    public ByteOrder order = null;
    public StringBuilder buf = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Dump()
    {
    }

    //////////////////////////////////////////////////
    // Command processing

    public String
    dumpdata(InputStream reader, boolean checksumming, ByteOrder order, Commands commands)
        throws IOException
    {
        this.reader = reader;
        this.checksumming = checksumming;
        this.order = order;
        this.buf = new StringBuilder();
        commands.run(this);
        return this.buf.toString();
    }

    public int
    printcount()
        throws IOException
    {
        ByteBuffer bytes = ByteBuffer.allocate(8).order(order);
        for(int i = 0;i < 8;i++) {
            int c = reader.read();
            if(c < 0)
                throw new IOException("Short DATADMR");
            bytes.put((byte) (c & 0xFF));
        }
        bytes.flip();
        long l = bytes.getLong();
        buf.append(String.format("count=%d%n",l));
        return (int) l;
    }

    public void
    printvalue(char cmd, int typesize, int... indices)
        throws IOException
    {
        long l = 0;
        ByteBuffer bytes = null;
        // for strings and opaque, the typesize is zero
        if(typesize == 0) {
            bytes = readn(8);
            l = bytes.getLong();
            bytes = readn((int)l);
        } else
            bytes = readn(typesize);
        if(indices != null && indices.length > 0) {
            for(int index : indices)
                buf.append(" [" + Integer.toString(index) + "]");
        }
        buf.append(" ");
        int switcher = (((int) cmd) << 4) + typesize;
        switch (switcher) {
        case ('S' << 4) + 1:
            byte b = bytes.get();
            l = ((long) b);
            buf.append(String.format("%d", l));
            break;
        case ('U' << 4) + 1:
            b = bytes.get();
            l = ((long) b) & 0xFF;
            buf.append(String.format("%d", l));
            break;
        case ('C' << 4) + 0:
        case ('C' << 4) + 1:
            l = ((long) bytes.get()) & 0x7F;
            buf.append(String.format("'%c'", (char) (l & 0xFF)));
            break;
        case ('S' << 4) + 2:
            short s = bytes.getShort();
            l = ((long) s);
            buf.append(String.format("%d", l));
            break;
        case ('U' << 4) + 2:
            s = bytes.getShort();
            l = ((long) s) & 0xFFFFL;
            buf.append(String.format("%d", l));
            break;
        case ('S' << 4) + 4:
            int i = bytes.getInt();
            l = ((long) i);
            buf.append(String.format("%d", l));
            break;
        case ('U' << 4) + 4:
            i = bytes.getInt();
            l = ((long) i) & 0xFFFFFFFFL;
            buf.append(String.format("%d", l));
            break;
        case ('S' << 4) + 8:
            l = bytes.getLong();
            l = ((long) l);
            buf.append(String.format("%d", l));
            break;
        case ('U' << 4) + 8: // We have to convert to BigInteger to get this right
            l = bytes.getLong();
            l = ((long) l);
            BigInteger big = BigInteger.valueOf(l);
            big = big.and(MASK);
            buf.append(String.format("%s", big.toString()));
            break;
        case ('F' << 4) + 4:
            Float f = bytes.getFloat();
            buf.append(String.format("%g", f));
            break;
        case ('F' << 4) + 8:
            Double d = bytes.getDouble();
            buf.append(String.format("%g", d));
            break;
        case ('T' << 4) + 0:
            // Read the text
            buf.append('"' + Escape.backslashEscape(new String(bytes.array(), DapUtil.UTF8), "\"") + '"');
            break;
        case ('O' << 4) + 0:
            buf.append("0x");
            for(i = 0;i < bytes.limit();i++) {
                int uint8 = bytes.get();
                char c = hexchar((uint8 >> 4) & 0xF);
                buf.append(c);
                c = hexchar((uint8) & 0xF);
                buf.append(c);
            }
            break;
        default:
            assert false;
        }
    }

    public void
    printchecksum()
        throws IOException
    {
        if(!checksumming)
            return;
        buf.append("\tchecksum = ");
        ByteBuffer bbuf = readn(DapUtil.CHECKSUMSIZE);
        for(int i=0;i<bbuf.limit();i++)
            buf.append(String.format("%02x", (bbuf.get() & 0xFF)));
        buf.append("\n");
    }

    public void
    newline()
    {
        buf.append("\n");
    }

    ByteBuffer
    readn(int n)
        throws IOException
    {
        byte[] bytes = new byte[n];
        for(int i = 0;i < n;i++) {
            int c = reader.read();
            if(c < 0)
                throw new IOException("Short DATADMR");
            bytes[i] = (byte) (c & 0xFF);
        }
        ByteBuffer result = ByteBuffer.wrap(bytes).order(order);
        return result;
    }

    public void
    format(String s)
    {
        buf.append(String.format(s));
    }

    char
    hexchar(int i)
    {
        return "0123456789ABCDEF".charAt((i & 0xF));
    }

}
