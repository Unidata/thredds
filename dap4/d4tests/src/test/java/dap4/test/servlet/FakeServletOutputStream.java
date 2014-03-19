package dap4.test.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    //////////////////////////////////////////////////
    // Get/Set

    public byte[] toArray()
    {
        return stream.toByteArray();
    }


    //////////////////////////////////////////////////
    // Interface

    public void write(int b)
        throws IOException
    {
        stream.write(b);
    }


    public void close()
        throws IOException
    {
        super.close();
        stream.close();
    }
}
