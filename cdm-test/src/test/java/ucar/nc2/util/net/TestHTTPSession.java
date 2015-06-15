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

import org.junit.Before;
import ucar.httpservices.*;

import org.apache.http.*;
import org.junit.Test;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.message.AbstractHttpMessage;
import ucar.nc2.util.UnitTestCommon;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.test.util.ThreddsServer;

import java.util.List;

import static ucar.httpservices.HTTPSession.*;

public class TestHTTPSession extends UnitTestCommon
{
    //////////////////////////////////////////////////
    // Constants

    protected final String TESTURL1 = "http://" + TestDir.dap2TestServer + "/dts/test.01.dds";
    static final String GLOBALAGENT = "TestUserAgent123global";
    static final String SESSIONAGENT = "TestUserAgent123session";

    //////////////////////////////////////////////////
    // Define the test sets

    int passcount = 0;
    int xfailcount = 0;
    int failcount = 0;
    boolean verbose = true;
    boolean pass = false;

    String datadir = null;
    String threddsroot = null;

    public TestHTTPSession()
    {
        super();
        setTitle("HTTP Session tests");
        HTTPSession.TESTING = true;
    }

    @Before
    public void setUp() {
        ThreddsServer.REMOTETEST.assumeIsAvailable();
    }

    @Test
    public void
    testAgent() throws Exception
    {
        System.out.println("*** Testing: User Agent");
        System.out.println("*** URL: " + TESTURL1);
        System.out.println("Test: HTTPSession.setGlobalUserAgent(" + GLOBALAGENT + ")");

        HTTPSession.debugHeaders(false);

        HTTPSession.setGlobalUserAgent(GLOBALAGENT);
        try (HTTPSession session = HTTPFactory.newSession(TESTURL1)) {
            HTTPMethod method = HTTPFactory.Get(session);
            method.execute();

            // Use special interface to access the request
            // Look for the user agent header
            List<Header> agents = HTTPSession.debugRequestInterceptor().getHeaders(HTTPSession.HEADER_USERAGENT);
            assertFalse("User-Agent Header not found", agents.size() == 0);
            assertFalse("Multiple User-Agent Headers", agents.size() > 1);
            assertTrue(String.format("User-Agent mismatch: expected %s found:%s",
                    GLOBALAGENT, agents.get(0).getValue()),
                GLOBALAGENT.equals(agents.get(0).getValue()));
            System.out.println("*** Pass: set global agent");

            System.out.println("Test: HTTPSession.setUserAgent(" + SESSIONAGENT + ")");
            HTTPSession.debugReset();
            session.setUserAgent(SESSIONAGENT);
            method = HTTPFactory.Get(session);
            method.execute();

            // Use special interface to access the request
            agents = HTTPSession.debugRequestInterceptor().getHeaders(HTTPSession.HEADER_USERAGENT);
            assertFalse("User-Agent Header not found", agents.size() == 0);
            assertFalse("Multiple User-Agent Headers", agents.size() > 1);
            assertTrue(String.format("User-Agent mismatch: expected %s found:%s",
                    SESSIONAGENT, agents.get(0).getValue()),
                SESSIONAGENT.equals(agents.get(0).getValue()));
            System.out.println("*** Pass: set session agent");
        }
    }

    // Verify that other configuration parameters
    // Can at least be set.
    @Test
    public void
    testConfigure() throws Exception
    {
        try (HTTPSession session = HTTPFactory.newSession(TESTURL1)) {

            System.out.println("Test: HTTPSession: Configuration");
            session.setSoTimeout(17777);
            session.setConnectionTimeout(37777);
            session.setMaxRedirects(111);
            CredentialsProvider bp = new HTTPBasicProvider("anyuser", "password");
            session.setCredentialsProvider(HTTPAuthPolicy.BASIC, bp);
            //session.setAuthorizationPreemptive(true); not implemented

            HTTPMethod method = HTTPFactory.Get(session);
            method.execute();

            // Use special interface to access the request
            AbstractHttpMessage dbgreq = (AbstractHttpMessage) method.debugRequest();

        /*  no longer used
        System.out.println("Test: Redirects Handled");
        b = dbgreq.getParams().getBooleanParameter(HANDLE_REDIRECTS, false);
        assertTrue("*** Fail: Redirects Handled", b);
        System.out.println("*** Pass: Redirects Handled");
        */

            boolean b = dbgreq.getParams().getBooleanParameter(HTTPSession.ALLOW_CIRCULAR_REDIRECTS, true);
            System.out.println("Test: Circular Redirects");
            assertTrue("*** Fail: Circular Redirects", b);
            System.out.println("*** Pass: Circular Redirects");

            System.out.println("Test: Max Redirects");
            int n = dbgreq.getParams().getIntParameter(MAX_REDIRECTS, -1);
            assertTrue("*** Fail: Max Redirects", n == 111);
            System.out.println("*** Pass: Max Redirects");

            System.out.println("Test: SO Timeout");
            n = dbgreq.getParams().getIntParameter(SO_TIMEOUT, -1);
            assertTrue("*** Fail: SO Timeout", n == 17777);
            System.out.println("*** Pass: SO Timeout");

            System.out.println("Test: Connection Timeout");
            n = dbgreq.getParams().getIntParameter(CONN_TIMEOUT, -1);
            assertTrue("*** Fail: Connection Timeout", n == 37777);
            System.out.println("*** Pass: SO Timeout");

        /* no longer used
        System.out.println("Test: Authentication Handled");
        b = dbgreq.getParams().getBooleanParameter(HANDLE_AUTHENTICATION, false);
        assertTrue("*** Fail: Authentication Handled", b);
        System.out.println("*** Pass: Authentication Handled");
         */


        }
    }
}
