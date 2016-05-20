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

import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

import java.util.List;

import static ucar.httpservices.HTTPSession.*;

@Category(NeedsExternalResource.class)
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
            HTTPMethod method = HTTPFactory.Get(session, TESTURL1);
            method.execute();
            // Use special interface to access the request
            // Look for the user agent header
            List<Header> agents = HTTPSession.debugRequestInterceptor().getHeaders(HTTPSession.HEADER_USERAGENT);
            Assert.assertFalse("User-Agent Header not found", agents.size() == 0);
            Assert.assertFalse("Multiple User-Agent Headers", agents.size() > 1);
            Assert.assertTrue(String.format("User-Agent mismatch: expected %s found:%s",
                    GLOBALAGENT, agents.get(0).getValue()),
                    GLOBALAGENT.equals(agents.get(0).getValue()));
            System.out.println("*** Pass: set global agent");

            System.out.println("Test: HTTPSession.setUserAgent(" + SESSIONAGENT + ")");
            HTTPSession.debugReset();
            session.setUserAgent(SESSIONAGENT);
            method = HTTPFactory.Get(session, TESTURL1);
            method.execute();

            // Use special interface to access the request
            agents = HTTPSession.debugRequestInterceptor().getHeaders(HTTPSession.HEADER_USERAGENT);
            Assert.assertFalse("User-Agent Header not found", agents.size() == 0);
            Assert.assertFalse("Multiple User-Agent Headers", agents.size() > 1);
            Assert.assertTrue(String.format("User-Agent mismatch: expected %s found:%s",
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
            Credentials bp = new UsernamePasswordCredentials("anyuser", "password");
            session.setCredentials(bp);
            //session.setAuthorizationPreemptive(true); not implemented

            HTTPMethod method = HTTPFactory.Get(session, TESTURL1);
            method.execute();

            // Use special interface to access the config
            RequestConfig dbgcfg = method.getDebugConfig();

            boolean b = dbgcfg.isCircularRedirectsAllowed();
            System.out.println("Test: Circular Redirects");
            Assert.assertTrue("*** Fail: Circular Redirects", b);
            System.out.println("*** Pass: Circular Redirects");

            System.out.println("Test: Max Redirects");
            int n = dbgcfg.getMaxRedirects();
            Assert.assertTrue("*** Fail: Max Redirects", n == 111);
            System.out.println("*** Pass: Max Redirects");

            System.out.println("Test: SO Timeout");
            n = dbgcfg.getSocketTimeout();
            Assert.assertTrue("*** Fail: SO Timeout", n == 17777);
            System.out.println("*** Pass: SO Timeout");

            System.out.println("Test: Connection Timeout");
            n = dbgcfg.getConnectTimeout();
            Assert.assertTrue("*** Fail: Connection Timeout", n == 37777);
            System.out.println("*** Pass: SO Timeout");

        }
    }
}
