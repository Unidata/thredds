/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.junit.Before;
import org.junit.Test;
import ucar.httpservices.*;
import ucar.nc2.util.UnitTestCommon;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.test.util.ThreddsServer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class TestAuth extends UnitTestCommon
{
    static final String BADPASSWORD = "bad";

    static protected final String MODULE = "httpclient";

    static protected final boolean IGNORE = true;

    /**
     * Temporary data directory (for writing temporary data).
     */
    static public String TEMPROOT = "target/test/tmp/"; // relative to module root

    // Add a temporary control for remote versus localhost
    static boolean remote = false;

    // TODO: add proxy and digest tests

    // Assuming we have thredds root, then the needed keystores
    // are located in this directory
    static final String KEYDIR = "/httpclient/src/test/resources";

    static final String CLIENTKEY = "clientkey.jks";
    static final String CLIENTPWD = "changeit";

    // Mnemonics for xfail
    static final boolean MUSTFAIL = true;
    static final boolean MUSTPASS = false;

    static String temppath = null;


    static {
        //todo: Register the 8843 protocol to test client side keys
        // HTTPSession.registerProtocol("https", 8843,
        //       new Protocol("https",
        //               new EasySSLProtocolSocketFactory(),
        //               8843));
        HTTPSession.TESTING = true;
        HTTPCachingProvider.TESTING = true;
        HTTPAuthStore.TESTING = true;

    }

    static public class Counter
    {
        protected int callcount = 0;// track # of times getCredentials is called

        synchronized public void incr()
        {
            callcount++;
        }

        synchronized public void clear()
        {
            callcount = 0;
        }

        synchronized public int counter()
        {
            int c = callcount;
            callcount = 0;
            return c;
        }
    }


    static protected class Result
    {
        int status = 0;
        int count = -1;
        byte[] contents = null;
    }


    //////////////////////////////////////////////////
    // Provide a non-interactive CredentialsProvider to hold
    // the user+pwd; used in several places
    static class TestProvider implements CredentialsProvider, Serializable
    {
        Counter counter = null;
        String username = null;
        String password = null;

        public TestProvider(Counter counter)
        {
            this.counter = counter;
        }

        public void setPWD(String username, String password)
        {
            this.username = username;
            this.password = password;
        }

        // Credentials Provider Interface
        public Credentials
        getCredentials(AuthScope scope) //AuthScheme authscheme, String host, int port, boolean isproxy)
        {
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
            System.err.printf("TestCredentials.getCredentials called: creds=|%s| host=%s port=%d%n",
                    creds.toString(), scope.getHost(), scope.getPort());
            this.counter.incr();
            return creds;
        }

        public void setCredentials(AuthScope scope, Credentials creds)
        {
            throw new UnsupportedOperationException();
        }

        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        // Serializable Interface
        private void writeObject(java.io.ObjectOutputStream oos)
                throws IOException
        {
            oos.writeObject(this.username);
            oos.writeObject(this.password);
        }

        private void readObject(java.io.ObjectInputStream ois)
                throws IOException, ClassNotFoundException
        {
            this.username = (String) ois.readObject();
            this.password = (String) ois.readObject();
        }
    }

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////

    // Define the test sets

    protected int passcount = 0;
    protected int xfailcount = 0;
    protected int failcount = 0;
    protected boolean verbose = true;

    protected String datadir = null;
    protected String threddsroot = null;

    protected Counter counter = new Counter();
    protected TestProvider provider = new TestProvider(counter);
    protected Result result = new Result();

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestAuth(String name, String testdir)
    {
        super(name);
        setTitle("DAP Authorization tests");
        // Make sure temp file exist
        temppath = getThreddsroot() + "/" + MODULE + "/" + TEMPROOT;
        new File(temppath).mkdirs();
        //HTTPSession.debugHeaders(true);
    }

    public TestAuth(String name)
    {
        this(name, null);
    }

    public TestAuth()
    {
        this("TestAuth", null);
    }

    @Before
    public void setUp()
    {
        ThreddsServer.REMOTETEST.assumeIsAvailable();
    }

    @Test
    public void
    testSSH() throws Exception
    {
        boolean pass = true;
        String[] sshurls = {
                "https://" + TestDir.dap2TestServer + "/dts/b31.dds"
        };

        System.out.println("*** Testing: Simple Https");
        for(String url : sshurls) {
            System.out.println("*** URL: " + url);
            try (HTTPMethod method = HTTPFactory.Get(url)) {
                int status = method.execute();
                System.out.printf("\tstatus code = %d\n", status);
                pass = (status == 200);
                assertTrue("testSSH", pass);
            }
        }
    }

    //////////////////////////////////////////////////

    static class AuthDataBasic
    {
        String url;
        String user = null;
        String password = null;

        public AuthDataBasic(String url, String usr, String pwd)
        {
            this.url = url;
            this.user = usr;
            this.password = pwd;
        }
    }
    protected AuthDataBasic[] basictests = {
            new AuthDataBasic("http://" + TestDir.threddsTestServer + "/thredds/dodsC/restrict/testData.nc.dds",
                    "tiggeUser", "tigge"),
    };

    @Test
    public void
    testBasic() throws Exception
    {
        System.out.println("*** Testing: Http Basic Password Authorization");
        boolean pass = true;
        for(AuthDataBasic data : basictests) {
            System.out.println("Test global credentials provider");
            System.out.println("*** URL: " + data.url);

            this.provider.setPWD(data.user, data.password);

            // Test global credentials provider
            HTTPSession.setGlobalCredentialsProvider(data.url, this.provider);
            HTTPCachingProvider.clearCache();
            try (HTTPSession session = HTTPFactory.newSession(data.url)) {
                this.result = invoke(session, data.url);
            }
            pass &= (this.result.status == 200 || this.result.status == 404); // non-existence is ok
            assertTrue("Credentials provider called: " + this.result.count, this.result.count == 1);
            assertTrue("no content", this.result.contents.length > 0);
        }

        for(AuthDataBasic data : basictests) {
            System.out.println("Test local credentials provider");
            System.out.println("*** URL: " + data.url);
            provider.setPWD(data.user, data.password);

            HTTPCachingProvider.clearCache();
            try (HTTPSession session = HTTPFactory.newSession(data.url)) {
                session.setCredentialsProvider(data.url, this.provider);
                this.result = invoke(session, data.url);
            }
            pass &= (this.result.status == 200 || this.result.status == 404); // non-existence is ok
            assertTrue("Credentials provider called: " + this.result.count, this.result.count == 1);
            assertTrue("no content", this.result.contents.length > 0);

        }
        assertTrue("testBasic", pass);
    }

    @Test
    public void
    testBasicDirect() throws Exception
    {
        System.out.println("*** Testing: Http Basic Password Authorization Using Constant Credentials");
        boolean pass = true;
        for(AuthDataBasic data : basictests) {
            Credentials creds = new UsernamePasswordCredentials(data.user, data.password);
            System.out.println("Test global credentials");
            System.out.println("*** URL: " + data.url);

            // Test global credentials provider
            HTTPSession.setGlobalCredentials(data.url, creds);
            HTTPCachingProvider.clearCache();
            try (HTTPSession session = HTTPFactory.newSession(data.url)) {
                this.result = invoke(session, data.url);
            }
            pass &= (this.result.status == 200 || this.result.status == 404); // non-existence is ok
            assertTrue("no content", this.result.contents.length > 0);
        }

        for(AuthDataBasic data : basictests) {
            Credentials creds = new UsernamePasswordCredentials(data.user, data.password);
            System.out.println("Test local credentials");
            System.out.println("*** URL: " + data.url);

            HTTPCachingProvider.clearCache();
            try (HTTPSession session = HTTPFactory.newSession(data.url)) {
                session.setCredentials(data.url, creds);
                this.result = invoke(session, data.url);

            }
            pass &= (this.result.status == 200 || this.result.status == 404); // non-existence is ok
            assertTrue("no content", this.result.contents.length > 0);
        }
        assertTrue("testBasic", pass);
    }


    @Test
    public void
    testCache() throws Exception
    {
        System.err.println("*** Testing: Cache Invalidation");
        boolean pass = true;
        for(AuthDataBasic data : basictests) {
            System.out.println("*** URL: " + data.url);

            // Do each test with a bad password to cause cache invalidation
            this.provider.setPWD(data.user, BADPASSWORD);

            HTTPCachingProvider.clearCache();
            try (HTTPSession session = HTTPFactory.newSession(data.url)) {
                session.setCredentialsProvider(data.url, this.provider);
                this.result = invoke(session, data.url);
            }
            pass &= (this.result.status == 401); // bad password should fail
            assertTrue("Credentials provider called: " + this.result.count, this.result.count == 1);
            // Look at the invalidation list
            List<HTTPCachingProvider.Auth> removed = HTTPCachingProvider.getTestList();
            if(removed.size() == 1) {
                HTTPCachingProvider.Auth triple = removed.get(0);
                pass &= (triple.scope.getScheme().equals(HTTPAuthSchemes.BASIC.toUpperCase())
                        && triple.creds instanceof UsernamePasswordCredentials);
            } else
                pass &= false;
            if(pass) {
                // retry with correct password
                this.provider.setPWD(data.user, data.password);
                try (HTTPSession session = HTTPFactory.newSession(data.url)) {
                    session.setCredentialsProvider(data.url, this.provider);
                    this.result = invoke(session, data.url);
                }
                assertTrue(result.status == 200);
            }
        }
        assertTrue("testBasic", pass);
    }

/*
    @Test
    public void
    testDigest() throws Exception
    {
        boolean pass = true;
        System.err.println("*** Testing: Digest Policy");
        // Clear the cache and the global authstore
        HTTPCachingProvider.clearCache();
        HTTPSession.debugHeaders(true);

        for(AuthDataBasic data : basictests) {
            Credentials cred = new UsernamePasswordCredentials(data.user, data.password);
            System.err.println("*** URL: " + data.url);
            try (HTTPSession session = HTTPFactory.newSession(data.url);
                 HTTPMethod method = HTTPFactory.Get(session, data.url)) {
                session.setCredentials(HTTPAuthSchemes.BASIC, cred);
                int status = method.execute();
                System.err.printf("status code = %d\n", status);
                System.err.flush();
                pass = (status == 200 || status == 404); // non-existence is ok
                assertTrue("testBasic", pass);
            }
        }
    }

    public void
    testRedirect() throws Exception  // not used except for special testing
    {
        if(IGNORE) return;
        boolean pass = true;
        System.err.println("*** Testing: Http Basic Password Authorization with redirect");
        HTTPSession.debugHeaders(true);

        for(AuthDataBasic data : basictests) {
            Credentials cred = new UsernamePasswordCredentials(data.user, data.password);
            System.err.println("*** URL: " + data.url);
            // Test local credentials provider
            try (HTTPSession session = HTTPFactory.newSession(data.url);
                 HTTPMethod method = HTTPFactory.Get(session, data.url)) {
                session.setCredentials(HTTPAuthSchemes.BASIC, cred);
                int status = method.execute();
                System.err.printf("\tlocal provider: status code = %d\n", status);
                switch (status) {
                case 200:
                case 404: // non-existence is ok
                    pass = true;
                    break;
                default:
                    System.err.println("Redirect: Unexpected status = " + status);
                    pass = false;
                    break;
                }
                session.clearState();
            }
            if(pass) {
                // Test global credentials provider
                HTTPSession.setGlobalCredentials(data.url, cred);
                try (HTTPSession session = HTTPFactory.newSession(data.url);
                     HTTPMethod method = HTTPFactory.Get(session, data.url)) {
                    int status = method.execute();
                    System.err.printf("\tglobal provider test: status code = %d\n", status);
                    System.err.flush();
                    switch (status) {
                    case 200:
                    case 404: // non-existence is ok
                        pass = true;
                        break;
                    default:
                        System.err.println("Redirect: Unexpected status = " + status);
                        pass = false;
                        break;
                    }
                }
            }
            if(pass)
                assertTrue("testBasic", true);
            else
                assertTrue("testBasic", false);
        }
    }
*/

/*
    // This test is turned off until such time as the server can handle it.
    @Test
    public void
    testKeystore() throws Exception
    {
        if(IGNORE) return; //ignore
        boolean pass = true;
        System.err.println("*** Testing: Client-side Key based Authorization");

        String server;
        String path;
        if(remote) {
            server = TestDir.dap2TestServer;
            path = "/dts/b31.dds";
        } else {
            server = "localhost:8843";
            path = "/thredds/dodsC/testStandardTdsScan/1day.nc.dds";
        }

        String url = "https://" + server + path;
        System.err.println("*** URL: " + url);

        // See if the client keystore exists
        String keystore = getThreddsroot() + KEYDIR + "/" + CLIENTKEY;
        File tmp = new File(keystore);
        if(!tmp.exists() || !tmp.canRead())
            throw new Exception("Cannot read client key store: " + keystore);

        CredentialsProvider provider = new HTTPSSLProvider(keystore, CLIENTPWD);
        AuthScope scope
                = HTTPAuthUtil.urlToScope(url, HTTPAuthSchemes.SSL);
        HTTPSession.setGlobalCredentialsProvider(scope, provider);

        try (HTTPSession session = HTTPFactory.newSession(url);
             HTTPMethod method = HTTPFactory.Get(session, url)) {
            int status = method.execute();
            System.err.printf("Execute: status code = %d\n", status);
            pass = (status == 200);
            if(pass)
                assertTrue("testKeystore", true);
            else
                assertTrue("testKeystore", false);
        }
    }

    // This test actually is does nothing because I have no way to test it
    // since it requires a firwall proxy that requires username+pwd
    @Test
    public void
    testFirewall() throws Exception
    {
        if(IGNORE) return; //ignore
        boolean pass = true;
        String user = null;
        String pwd = null;
        String host = null;
        int port = -1;
        String url = null;

        Counter counter = new Counter();
        System.err.println("*** Testing: Http Firewall Proxy (with authentication)");
        if(false) {
            this.provider.setPWD(user, pwd);
            System.err.println("*** URL: " + url);
            // Test local credentials provider
            try (HTTPSession session = HTTPFactory.newSession(url);
                 HTTPMethod method = HTTPFactory.Get(session, url)) {
                session.setProxy(host, port);
                session.setCredentialsProvider(url, provider);
                int status = method.execute();
                System.err.printf("\tlocal provider: status code = %d\n", status);
                System.err.flush();
                pass = (status == 200 || status == 404); // non-existence is ok
                String msg = pass ? "Local test passed" : "Local test failed";
                System.err.println("\t" + msg);
            }
            if(pass) {
                // Test global credentials provider
                HTTPSession.setGlobalProxy(host, port);
                HTTPSession.setGlobalCredentialsProvider(url, provider);
                try (HTTPSession session = HTTPFactory.newSession(url);
                     HTTPMethod method = HTTPFactory.Get(session, url)) {
                    int status = method.execute();
                    System.err.printf("\tglobal provider test: status code = %d\n", status);
                    System.err.flush();
                    pass = (status == 200 || status == 404); // non-existence is ok
                    String msg = pass ? "Local test passed" : "Local test failed";
                    System.err.println("\t" + msg);
                }
            }
            if(pass)
                assertTrue("testProxy", true);
            else
                assertTrue("testProxy", false);
        }
        assertTrue("testProxy", true);
    }
*/

    //////////////////////////////////////////////////

    protected Result
    invoke(HTTPSession session, String url)
            throws IOException
    {
        Result result = new Result();
        this.counter.clear();
        try {
            try (HTTPMethod method = HTTPFactory.Get(session, url)) {
                result.status = method.execute();
                System.err.printf("\tglobal provider: status code = %d\n", result.status);
                System.err.printf("\t|cache| = %d\n", HTTPCachingProvider.getCache().size());
                // Get the number of calls to the credentialer
                result.count = counter.counter();
                // try to read in the content
                result.contents = readbinaryfile(method.getResponseAsStream());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException(t);
        }
        return result;
    }
}

