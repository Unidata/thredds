package ucar.nc2.util.net;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TestSocketFactory extends PlainSocketFactory
{

    public TestSocketFactory()
    {
        super();
    }

    @Override
    public Socket
    connectSocket(Socket socket,
                  InetSocketAddress remote,
                  InetSocketAddress local,
                  HttpParams params)
        throws IOException
    {
        // See if our test parameter is present
        Object o = params.getParameter("testtesttest");
        return super.connectSocket(socket, remote, local, params);
    }

    @Override
    public Socket
    createSocket(HttpParams params)
    {
        // See if our test parameter is present
        Object o = params.getParameter("testtesttest");
        return super.createSocket(params);
    }

}
