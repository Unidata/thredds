/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;

import org.junit.Test;
import ucar.nc2.util.UnitTestCommon;
import ucar.nc2.util.net.HTTPSession;
import ucar.nc2.util.net.HTTPMethod;
import ucar.nc2.util.net.HTTPMethodStream;

import java.io.*;
import java.nio.charset.Charset;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Test the proper state transitions of HTTPSession
 * and HTTPMethod and HTTPMethodStream.
 * Specifically:
 * 1. make sure that closing a stream
 *    closes the method
 * 2. closing the method closes the stream
 * 3. closing the session closes the methods
 * 4. test local session handling.
 */

public class TestState extends UnitTestCommon
{
    //////////////////////////////////////////////////
    // Constants
    static final String SESSIONURL = "http://thredds-test.ucar.edu:8081/dts";
    static final String TESTSOURCE1 = SESSIONURL + "/test.01.dds";
    static final String TESTSOURCE2 = SESSIONURL + "/test.02.dds";

    static final public Charset UTF8 = Charset.forName("UTF-8");

    //////////////////////////////////////////////////
    boolean verbose = false;
    boolean pass = false;

    public TestState()
    {
        setTitle("HTTP Session and Method State tests");
    }


    @Test
    public void
    testState()
        throws Exception
    {

        HTTPSession session = new HTTPSession(SESSIONURL);
        assertFalse(session.isClosed());

        // Check state transitions for open and execute
        HTTPMethod method = HTTPMethod.Get(session, TESTSOURCE1);
        assertFalse(method.isClosed());
        int methodcount = session.getMethodcount();
        assertTrue(methodcount == 1);

        // Check that stream close causes method close
        method.execute();
        HTTPMethodStream stream = (HTTPMethodStream) method.getResponseBodyAsStream();
        assertTrue(method.hasStreamOpen());
        stream.close();
        assertTrue(method.isClosed());
        assertFalse(method.hasStreamOpen());
        methodcount = session.getMethodcount();
        assertTrue(methodcount == 0);

        // Check that method close forcibly closes streams
        method = HTTPMethod.Get(session, TESTSOURCE1);
        methodcount = session.getMethodcount();
        assertTrue(methodcount == 1);
        method.execute();
        stream = (HTTPMethodStream) method.getResponseBodyAsStream();
        method.close();
        assertTrue(stream.isClosed());
        assertFalse(method.hasStreamOpen());
        assertTrue(method.isClosed());

        // Check that session close closes methods and streams
        // and transitively until stream close
        method = HTTPMethod.Get(session);
        methodcount = session.getMethodcount();
        assertTrue(methodcount == 1);
        method.execute();
        stream = (HTTPMethodStream) method.getResponseBodyAsStream();
        session.close();
        assertTrue(stream.isClosed());
        assertTrue(method.isClosed());
        methodcount = session.getMethodcount();
        assertTrue(methodcount == 0);
        assertTrue(session.isClosed());

        // Test Local session
        method = HTTPMethod.Get();
        assertTrue(method.isSessionLocal());
        session = method.getSession();
        methodcount = session.getMethodcount();
        assertTrue(methodcount == 1);
        method.execute(TESTSOURCE1);
        String body = method.getResponseAsString();
        stream = (HTTPMethodStream) method.getResponseBodyAsStream();
        byte[] bbody = readbinaryfile(stream);
        method.close();
        assertTrue(stream.isClosed());
        assertFalse(method.hasStreamOpen());
        assertTrue(method.isClosed());
        methodcount = session.getMethodcount();
        assertTrue(methodcount == 0);
        assertTrue(session.isClosed());

        // Finally, do a content comparison
        String s = new String(bbody,UTF8);
        if(verbose) {
            System.err.println("Body as String:\n"+body);
            System.err.println("Body as Bytes:\n"+s);
        }

        String compare = compare("TestState",body,s);
        if(compare != null) {
            System.out.println(compare);
            pass = false;
        } else
            pass = true;
        assertTrue(pass);
    }

    static public byte[]
    readbinaryfile(InputStream stream)
        throws IOException
    {
        // Extract the stream into a bytebuffer
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] tmp = new byte[1 << 16];
        for(;;) {
            int cnt;
            cnt = stream.read(tmp);
            if(cnt <= 0) break;
            bytes.write(tmp, 0, cnt);
        }
        return bytes.toByteArray();
    }
}
