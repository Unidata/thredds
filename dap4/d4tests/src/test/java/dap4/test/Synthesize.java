/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.test;

import dap4.core.dmr.DapType;
import dap4.core.util.DapException;
import dap4.servlet.SerialWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Synthesize
{

    //////////////////////////////////////////////////
    // Constants

    static final int MAXSIZE = 8;

    static final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    //////////////////////////////////////////////////
    // Type decls

    // Place to insert the command list
    static public interface Commands
    {
        public void run(Synthesize reader) throws IOException;
    }

    //////////////////////////////////////////////////
    // Instance databuffer

    public boolean checksumming = true;
    public ByteOrder order = null;
    public byte[] tmp = new byte[MAXSIZE];
    public ByteBuffer bbtmp = ByteBuffer.wrap(tmp);
    public ByteArrayOutputStream writer = new ByteArrayOutputStream();

    //////////////////////////////////////////////////
    // Constructor(s)

    public Synthesize()
    {
    }

    //////////////////////////////////////////////////
    // Accessors

    public byte[]
    getBytes()
    {
        return writer.toByteArray();
    }


    //////////////////////////////////////////////////
    // Command processing

    public void
    synthesize(boolean checksumming, ByteOrder order, Commands commands)
        throws IOException
    {
        this.checksumming = checksumming;
        this.order = order;
        this.bbtmp.order(order);
        commands.run(this);
    }

    //////////////////////////////////////////////////
    // Commands

    public void
    putcount(long count)
        throws IOException
    {
        bbtmp.position(0);
        bbtmp.putLong(count);
        writer.write(tmp, 0, bbtmp.position());
    }

    public void
    putvalue(DapType daptype, Object value)
        throws IOException
    {
        int typesize = daptype.getSize();
        if(daptype.isFixedSize()) {
            ByteBuffer tmp = SerialWriter.encodeArray(daptype, value, this.order);
        } else if(daptype == DapType.STRING) {
            String s = (String) value;
            putcount(s.length());
        } else if(daptype == DapType.OPAQUE) {
            ByteBuffer b = (ByteBuffer) value;
            int len = b.position();
            putcount(len);
            b.position(0);
            for(int i = 0;i < len;i++)
                writer.write(b.get());
            b.position(len);
        } else
            throw new DapException("Synthesize.putvalue: illegal type: " + daptype);
    }

    public void
    putchecksum(ByteBuffer b)
        throws IOException
    {
        if(!checksumming)
            return;
        int len = b.position();
        b.position(0);
        for(int i = 0;i < len;i++)
            writer.write(b.get());
        b.position(len);
    }

}
