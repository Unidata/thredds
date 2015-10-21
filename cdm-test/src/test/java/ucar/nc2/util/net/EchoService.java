/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package ucar.nc2.util.net;

import ucar.nc2.util.UnitTestCommon;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * ***************************************************************************
 * Compilation:  javac EchoService.java
 * Execution:    java EchoService port
 * Dependencies: In.java Out.java
 * <p>
 * Runs an echo server which listents for connections on port 4444,
 * and echoes back whatever is sent to it.
 * <p>
 * <p>
 * % java EchoService 4444
 * <p>
 * <p>
 * Limitations
 * -----------
 * The server is not multi-threaded, so at most one client can connect
 * at a time.
 * <p>
 * Source: http://introcs.cs.princeton.edu/java/84network/EchoService.java.html
 * <p>
 * ****************************************************************************
 */

public class EchoService implements Runnable, Closeable
{
    int port = 4444;

    public volatile boolean terminate = false;

    Thread instance = null;

    public EchoService(int port)
    {
        this.port = port;
    }

    public void run()
    {
        try {
            // create socket
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                //log.info(
                System.err.println(
                        "Started server on port " + port);
                System.err.flush();
                serverSocket.setSoTimeout(1000);
                // repeatedly wait for connections, and process
                while(!terminate) {

                    // a "blocking" call which waits until a connection is requested
                    Socket clientSocket = null;
                    try {
                        clientSocket = serverSocket.accept();
                    } catch (SocketTimeoutException ste) {
                        continue; // so we can check for terminate flag
                    }
                    //log.info(
                    System.err.println(
                            "Accepted connection from client");
                    System.err.flush();

                    InputStream is = clientSocket.getInputStream();
                    OutputStream os = clientSocket.getOutputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    // Send the headers
                    byte[] hdrs = "HTTP/1.0 200 OK\nContent-Type: application/octet-stream\n\n"
                            .getBytes("UTF-8");
                    os.write(hdrs);
                    while(!terminate) {
                        int c = is.read();
                        if(c <= 0) break;
                        os.write(c);
                        bos.write(c);
                        // available() on a socket appears to require
                        // an initial read in order to be non-zero.
                        // So we have to assume that at least one char is sent
                        int avail = is.available();
                        if(avail == 0) break;
                    }
                    byte[] in = bos.toByteArray();
                    if(UnitTestCommon.DEBUG) {
                        String body = new String(in, "UTF-8");
                        System.err.println("EchoService.RAW:\n" + body);
                    }
                    System.err.println(
                            "Closing connection with client");
                    System.err.flush();
                    clientSocket.close();
                }
            }

        } catch (Exception e) {
            //log.error(
            System.err.println(
                    "EchoService failed: " + e.getMessage());
            System.err.flush();
        }
        if(terminate)
            System.err.println("Echo Server terminated");
        System.err.flush();
    }

    public EchoService
    startecho()
    {
        instance = new Thread(this);
        instance.start();
        return this;
    }

    public void close()
    {
        System.err.println("Closing thread");
        System.err.flush();
        this.terminate = true;
        this.instance.interrupt();
        for(; ; ) {
            try {
                this.instance.join(1000);
                if(this.instance.isAlive()) {
                    System.err.println("thread timeout");
                    System.err.flush();
                } else break;
            } catch (InterruptedException ie) {
                continue;
            }
        }
    }
}
