package dap4.test.servlet;

import dap4.core.util.DapDump;

import javax.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

public class FakeServletOutputStream extends javax.servlet.ServletOutputStream
{
    //////////////////////////////////////////////////
    // Instance Variables

    ByteArrayOutputStream stream;

    //////////////////////////////////////////////////
    // Constructor(s)

    public FakeServletOutputStream()
    {
        stream = new ByteArrayOutputStream();
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new UnsupportedOperationException();
    }

    //////////////////////////////////////////////////
    // Accessors

    public byte[] toArray()
    {
        return stream.toByteArray();
    }

    public ByteArrayOutputStream byteStream()
    {
        return stream;
    }

    //////////////////////////////////////////////////
    // Interface

    @Override
    public void write(int b)
        throws IOException
    {
        stream.write(b);
    }

    /**
    * @param      b     the data.
    * @param      off   the start offset in the data.
    * @param      len   the number of bytes to write.
    * @exception  IOException  if an I/O error occurs. In particular,
    *             an <code>IOException</code> is thrown if the output
    *             stream is closed.
    */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        super.write(b, off, len);
    }

    public void close()
        throws IOException
    {
        super.close();
        stream.close();
    }
}
