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

import org.apache.http.Header;
import org.junit.Ignore;

import ucar.httpclient.*;
import org.apache.http.Header;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.junit.Test;
import ucar.nc2.util.EscapeStrings;
import ucar.nc2.util.UnitTestCommon;

import java.io.*;
import java.util.List;
import java.util.Map;

public class TestAuth extends UnitTestCommon
{
    static final String BADPASSWORD = "bad";

    static protected final String MODULE = "httpclient";

    static protected final boolean IGNORE = true;

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
        // Make sure temp file exist
        temppath = threddsRoot + "/" + MODULE + "/" + TEMPROOT;
        new File(temppath).mkdirs();
    }

    //////////////////////////////////////////////////
    // Provide a non-interactive CredentialsProvider to hold
    // the user+pwd; used in several places
    static class TestProvider implements CredentialsProvider, Serializable
    {
        protected int callcount = 0;// track # of times getCredentials is called

        String username = null;
        String password = null;

        public TestProvider(String username, String password)
        {
            this.username = username;
            this.password = password;
            this.callcount = 0;
        }

        public int getCallCount()
        {
            int count = this.callcount;
            this.callcount = 0;
            return count;
        }

        // Credentials Provider Interface
        public Credentials
        getCredentials(AuthScope scope) //AuthScheme authscheme, String host, int port, boolean isproxy)
        {
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
            System.err.printf("TestCredentials.getCredentials called: creds=|%s| host=%s port=%d%n",
                creds.toString(), scope.getHost(), scope.getPort());
            this.callcount++;
            return creds;
        }

        public void setCredentials(AuthScope scope, Credentials creds)
        {
        }

        public void clear()
        {
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
    protected boolean pass = false;

    protected String datadir = null;
    protected String threddsroot = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestAuth(String name, String testdir)
    {
        super(name);
        setTitle("DAP Authorization tests");
    }

    public TestAuth(String name)
    {
        this(name, null);
    }

    public TestAuth()
    {
        this("TestAuth", null);
    }

    @Test
    public void
    testSSH() throws Exception
    {
        String[] sshurls = {
            "https://" + REMOTESERVER + "/dts/b31.dds"
        };

        System.out.println("*** Testing: Simple Https");
        for(String url : sshurls) {
            System.out.println("*** URL: " + url);
            HTTPSession session = HTTPFactory.newSession(url);
            HTTPMethod method = HTTPFactory.Get(session);
            int status = method.execute();
            System.out.printf("\tstatus code = %d\n", status);
            pass = (status == 200);
            assertTrue("testSSH", pass);
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

    static AuthDataBasic[] basictests = {
        new AuthDataBasic("http://" + REMOTESERVER + "/thredds/restricted/basicAuth",
            "remoteUser", "remotePassword"),
    };

    static AuthDataBasic[] redirecttests = {
        new AuthDataBasic("http://" + REMOTESERVER + "/thredds/dodsC/restrict/testData.nc.dds",
            "tiggUser", "tigge"),
    };

    @Test
    public void
    testBasic() throws Exception
    {
        if(IGNORE) return;
        // System.out.println("*** Testing: Http Basic Password Authorization");
        HTTPSession.debugHeaders(true);

        for(AuthDataBasic data : basictests) {
            HTTPCachingProvider.clearCache();
            TestProvider provider = new TestProvider(data.user, data.password);
            System.out.println("*** URL: " + data.url);
            // Test local credentials provider
            HTTPSession session = HTTPFactory.newSession(data.url);
            session.setCredentialsProvider(provider);
            HTTPMethod method = HTTPFactory.Get(session);
            int status = method.execute();
            System.err.printf("\tlocal provider: status code = %d\n", status);
            pass = (status == 200 || status == 404); // non-existence is ok
            // Verify that getCredentials was called only once
            int count = provider.getCallCount();
            assertTrue("Credentials provider called: " + count, count == 1);

            // try to read in the content
            byte[] contents = readbinaryfile(method.getResponseAsStream());
            assertTrue("no content", contents.length > 0);

            if(pass) {
                // Test global credentials provider
                HTTPSession.setGlobalCredentialsProvider(provider);
                session = HTTPFactory.newSession(data.url);
                method = HTTPFactory.Get(session);
                status = method.execute();
                System.err.printf("\tglobal provider test: status code = %d\n", status);
                System.err.flush();
                pass = (status == 200 || status == 404); // non-existence is ok
            }
            assertTrue("testBasic", pass);
        }
    }

    @Test
    public void
    testBasicDirect() throws Exception
    {
        if(IGNORE) return;
        System.err.println("*** Testing: Http Basic Password Authorization Using direct credentials");
        HTTPSession.debugHeaders(true);

        for(AuthDataBasic data : basictests) {
            Credentials cred = new UsernamePasswordCredentials(data.user, data.password);
            System.err.println("*** URL: " + data.url);
            // Test local credentials provider
            HTTPSession session = HTTPFactory.newSession(data.url);
            session.setCredentials(HTTPAuthPolicy.BASIC, cred);

            HTTPMethod method = HTTPFactory.Get(session);
            int status = method.execute();
            System.err.printf("\tlocal provider: status code = %d\n", status);
            System.err.flush();
            pass = (status == 200 || status == 404); // non-existence is ok
            if(pass) {
                session.clearState();
                // Test global credentials provider
                AuthScope scope
                    = HTTPAuthScope.urlToScope(data.url, HTTPAuthPolicy.BASIC, null);
                HTTPSession.setGlobalCredentials(scope, cred);
                session = HTTPFactory.newSession(data.url);
                method = HTTPFactory.Get(session);
                status = method.execute();
                System.err.printf("\tglobal provider test: status code = %d\n", status);
                System.err.flush();
                pass = (status == 200 || status == 404); // non-existence is ok
            }
            if(pass)
                assertTrue("testBasic", true);
            else
                assertTrue("testBasic", false);
        }
    }

    @Test
    public void
    testCache() throws Exception
    {
        if(IGNORE) return;

        System.err.println("*** Testing: Cache Invalidation");
        // Clear the cache and the global authstore
        HTTPAuthStore.DEFAULTS.clear();
        HTTPCachingProvider.clearCache();
        for(AuthDataBasic data : basictests) {
            // Do each test with a bad password to cause cache invalidation
            TestProvider provider = new TestProvider(data.user, BADPASSWORD);
            System.err.println("*** URL: " + data.url);

            HTTPSession session = HTTPFactory.newSession(data.url);
            session.setCredentialsProvider(provider);
            HTTPMethod method = HTTPFactory.Get(session);
            int status = method.execute();

            System.err.printf("\tlocal provider: status code = %d\n", status);

            assertTrue(status == 200);

            int count = provider.getCallCount();
            // Verify that getCredentials was called only once
            assertTrue("Credentials provider call count = " + count, count == 1);

            // Look at the invalidation list
            List<HTTPCachingProvider.Triple> removed = HTTPCachingProvider.getTestList();
            if(removed.size() == 1) {
                HTTPCachingProvider.Triple triple = removed.get(0);
                pass = (triple.scope.getScheme().equals(HTTPAuthPolicy.BASIC.toUpperCase())
                    && triple.creds instanceof UsernamePasswordCredentials);
            } else
                pass = false;

            if(pass) {
                // retry with correct password
                provider = new TestProvider(data.user, data.password);
                session.setCredentialsProvider(provider);
                method = HTTPFactory.Get(session);
                status = method.execute();
                assertTrue(status == 200);
            }

            if(pass)
                assertTrue("testBasic", true);
            else
                assertTrue("testBasic", false);
        }
    }

    @Ignore
    @Test
    public void
    testDigest() throws Exception
    {
        if(IGNORE) return; //ignore
        System.err.println("*** Testing: Digest Policy");
        // Clear the cache and the global authstore
        HTTPAuthStore.DEFAULTS.clear();
        HTTPCachingProvider.clearCache();
        HTTPSession.debugHeaders(true);

        for(AuthDataBasic data : basictests) {
            Credentials cred = new UsernamePasswordCredentials(data.user, data.password);
            System.err.println("*** URL: " + data.url);
            HTTPSession session = HTTPFactory.newSession(data.url);
            session.setCredentials(HTTPAuthPolicy.BASIC, cred);
            HTTPMethod method = HTTPFactory.Get(session);
            int status = method.execute();
            System.err.printf("status code = %d\n", status);
            System.err.flush();
            pass = (status == 200 || status == 404); // non-existence is ok
            assertTrue("testBasic", pass);
        }
    }

    public void
    testRedirect() throws Exception  // not used except for special testing
    {
        if(IGNORE) return;

        System.err.println("*** Testing: Http Basic Password Authorization with redirect");
        HTTPSession.debugHeaders(true);

        for(AuthDataBasic data : redirecttests) {
            Credentials cred = new UsernamePasswordCredentials(data.user, data.password);
            System.err.println("*** URL: " + data.url);
            // Test local credentials provider
            HTTPSession session = HTTPFactory.newSession(data.url);
            session.setCredentials(HTTPAuthPolicy.BASIC, cred);
            HTTPMethod method = HTTPFactory.Get(session);
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
            if(pass) {
                session.clearState();
                // Test global credentials provider
                AuthScope scope
                    = HTTPAuthScope.urlToScope(data.url, HTTPAuthPolicy.BASIC, null);
                HTTPSession.setGlobalCredentials(scope, cred);
                session = HTTPFactory.newSession(data.url);
                method = HTTPFactory.Get(session);
                status = method.execute();
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
            if(pass)
                assertTrue("testBasic", true);
            else
                assertTrue("testBasic", false);
        }
    }

    // This test is turned off until such time as the server can handle it.
    @Ignore
    @Test
    public void
    testKeystore() throws Exception
    {
        if(IGNORE) return; //ignore
        System.err.println("*** Testing: Client-side Key based Authorization");

        String server;
        String path;
        if(remote) {
            server = REMOTESERVER;
            path = "/dts/b31.dds";
        } else {
            server = "localhost:8843";
            path = "/thredds/dodsC/testStandardTdsScan/1day.nc.dds";
        }

        String url = "https://" + server + path;
        System.err.println("*** URL: " + url);

        // See if the client keystore exists
        String keystore = threddsRoot + KEYDIR + "/" + CLIENTKEY;
        File tmp = new File(keystore);
        if(!tmp.exists() || !tmp.canRead())
            throw new Exception("Cannot read client key store: " + keystore);

        CredentialsProvider provider = new HTTPSSLProvider(keystore, CLIENTPWD);
        AuthScope scope
            = HTTPAuthScope.urlToScope(url, HTTPAuthPolicy.SSL, null);
        HTTPSession.setGlobalCredentialsProvider(scope, provider);

        HTTPSession session = HTTPFactory.newSession(url);

        //session.setCredentialsProvider(provider);

        HTTPMethod method = HTTPFactory.Get(session);

        int status = method.execute();
        System.err.printf("Execute: status code = %d\n", status);
        pass = (status == 200);
        if(pass)
            assertTrue("testKeystore", true);
        else
            assertTrue("testKeystore", false);

    }

    @Test
    public void
    testSerialize() throws Exception
    {
        System.err.println("*** Testing: HTTPAuthStore (de-)serialization");

        boolean ok = true;
        CredentialsProvider credp1 = new TestProvider("p1", "pwd");
        CredentialsProvider credp2 = new HTTPSSLProvider("keystore", "keystorepwd");
        CredentialsProvider credp3 = new TestProvider("p3", "pwd3");
        Credentials cred1 = new UsernamePasswordCredentials("u1", "pwd1");
        Credentials cred2 = new UsernamePasswordCredentials("u2", "pwd2");
        AuthScope scope;
        scope = new AuthScope(
            "http://ceda.ac.uk/dap/neodc/casix/seawifs_plankton/data/monthly/PSC_monthly_1998.nc.dds",
            AuthScope.ANY_PORT, AuthScope.ANY_REALM,
            HTTPAuthPolicy.BASIC);
        // Add some entries to an HTTPAuthStore
        HTTPAuthStore store = new HTTPAuthStore();

        scope = HTTPAuthScope.urlToScope(
            "http://ceda.ac.uk/dap/neodc/casix/seawifs_plankton/data/monthly/PSC_monthly_1998.nc.dds",
            HTTPAuthPolicy.BASIC, null);
        store.insert(HTTPAuthScope.ANY_PRINCIPAL, scope, credp1);

        scope = HTTPAuthScope.urlToScope("http://ceda.ac.uk",
            HTTPAuthPolicy.SSL, null);

        store.insert(HTTPAuthScope.ANY_PRINCIPAL, scope, credp2);

        scope = HTTPAuthScope.urlToScope("http://ceda.ac.uk",
            HTTPAuthPolicy.BASIC, null);
        store.insert(HTTPAuthScope.ANY_PRINCIPAL, scope, credp3);

        // Remove any old file
        File target1 = new File(temppath + "/serial1");
        target1.delete();

        // serialize out
        OutputStream ostream = new FileOutputStream(target1);
        store.serialize(ostream, "password1");
        // Read in auth store
        InputStream istream = new FileInputStream(target1);
        ObjectInputStream ois = HTTPAuthStore.openobjectstream(istream, "password1");
        HTTPAuthStore newstore = HTTPAuthStore.getDeserializedStore(ois); // - cache

        // compare
        List<HTTPAuthStore.Entry> rows = store.getAllRows();
        List<HTTPAuthStore.Entry> newrows = newstore.getAllRows();
        for(HTTPAuthStore.Entry row : rows) {
            HTTPAuthStore.Entry match = null;
            for(HTTPAuthStore.Entry e : newrows) {
                block:
                {
                    if(!HTTPAuthScope.equivalent(row.scope, e.scope)
                        && row.provider.getClass() == e.provider.getClass())
                        break block;
                    if(match == null)
                        match = e;
                    else {
                        System.err.println("ambigous match");
                        ok = false;
                    }
                }
            }
            if(match == null) {
                System.err.println("no match for: " + row.toString());
                ok = false;
            }
        }
        assertTrue("test(De-)Serialize", ok);
    }


    // This test actually is does nothing because I have no way to test it
    // since it requires a firwall proxy that requires username+pwd
    @Ignore
    @Test
    public void
    testFirewall() throws Exception
    {
        if(IGNORE) return; //ignore
        String user = null;
        String pwd = null;
        String host = null;
        int port = -1;
        String url = null;

        System.err.println("*** Testing: Http Firewall Proxy (with authentication)");
        if(false) {
            CredentialsProvider provider = new TestProvider(user, pwd);
            System.err.println("*** URL: " + url);
            // Test local credentials provider
            HTTPSession session = HTTPFactory.newSession(url);
            session.setProxy(host, port);
            session.setCredentialsProvider(provider);
            HTTPMethod method = HTTPFactory.Get(session);
            int status = method.execute();
            System.err.printf("\tlocal provider: status code = %d\n", status);
            System.err.flush();
            pass = (status == 200 || status == 404); // non-existence is ok
            String msg = pass ? "Local test passed" : "Local test failed";
            System.err.println("\t" + msg);
            if(pass) {
                // Test global credentials provider
                HTTPSession.setGlobalCredentialsProvider(provider);
                HTTPSession.setGlobalProxy(host, port);
                session = HTTPFactory.newSession(url);
                method = HTTPFactory.Get(session);
                status = method.execute();
                System.err.printf("\tglobal provider test: status code = %d\n", status);
                System.err.flush();
                pass = (status == 200 || status == 404); // non-existence is ok
                msg = pass ? "Local test passed" : "Local test failed";
                System.err.println("\t" + msg);
            }
            if(pass)
                assertTrue("testProxy", true);
            else
                assertTrue("testProxy", false);
        }
        assertTrue("testProxy", true);
    }


}

