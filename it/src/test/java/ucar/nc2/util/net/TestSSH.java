/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.net;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;

/**
 * This test is to check ssh authorization.
 * As a rule, this needs to run against localhost:8443
 * using a pure tomcat server.
 */
public class TestSSH extends UnitTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static protected final boolean IGNORE = true;

    static protected String SERVER = "localhost:8443";

    static protected String Dkeystore = null;
    static protected String Dkeystorepassword = null;

    static final String[] sshurls = {
            "https://" + SERVER + "/thredds/dodsC/localContent/testData.nc.dds"
    };

    static {
        // Get the keystore properties
        Dkeystore = System.getProperty("keystore");
        Dkeystorepassword = System.getProperty("keystorepassword");
        // Set testing output
        HTTPSession.TESTING = true;
    }

    //////////////////////////////////////////////////
    static public class Result
    {
        public int status = 0;
        public byte[] contents = null;

        public String toString()
        {
            return String.format("{status=%d |contents|=%d}",
                    status, (contents == null ? 0 : contents.length));
        }
    }

    static public void
    report(Result result)
    {
        report(result, null);
    }

    static public void
    report(Result result, Integer counter)
    {
        System.err.printf("Result: code=%d content?=%b provider-calls=%d%n",
                result.status, result.contents.length, counter);
        System.err.flush();
    }

    // Provide a non-interactive CredentialsProvider to hold
    // the user+pwd; used in several places

    static class TestProvider implements CredentialsProvider, Serializable
    {
        String username = null;
        String password = null;

        public TestProvider()
        {
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

    // Define the test sets

    protected String datadir = null;
    protected String threddsroot = null;

    protected Result result = new Result();

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestSSH()
    {
        super("TestSSH");
        setTitle("SSH Authorization tests");
        //HTTPSession.debugHeaders(true);
    }

    @Test
    public void
    testSSH() throws Exception
    {
        String version = System.getProperty("java.version");
        Assume.assumeTrue("Version must be 1.8 (temporary), not: " + version,
                version.startsWith("1.8"));

        System.out.println("*** Testing: Simple Https");

        // Reset the ssl stores
        HTTPSession.clearkeystore();

        for(String url : sshurls) {
            System.out.println("*** URL: " + url);
            try (HTTPSession session = HTTPFactory.newSession(url)) {
                this.result = invoke(session, url);
                Assert.assertTrue("Incorrect return code: " + this.result.status, this.result.status == 200);
            }
        }
    }

    /**
     * Client-side keys are difficult to test.
     * To properly test, the clientAuth attribute
     * in server.xml needs to be changed from
     * "want" to "true". In that case, testSSH will fail
     * but testMutualSSH will pass.
     *
     * @throws Exception
     */
    @Test
    public void
    testMutualSSH() throws Exception
    {
        String version = System.getProperty("java.version");
        Assume.assumeTrue("Version must be 1.8 (temporary), not: " + version,
                version.startsWith("1.8"));

        System.out.println("*** Testing: Mutual Https");

        // (Re-)Establish the client key store and password
        // Reset the ssl stores
        HTTPSession.rebuildkeystore(Dkeystore, Dkeystorepassword);

        for(String url : sshurls) {
            System.out.println("*** URL: " + url);
            try (HTTPSession session = HTTPFactory.newSession(url)) {
                this.result = invoke(session, url);
                Assert.assertTrue("Incorrect return code: " + this.result.status, this.result.status == 200);
            }
        }
    }

    //////////////////////////////////////////////////

    protected Result
    invoke(HTTPSession session, String url)
            throws IOException
    {
        Result result = new Result();
        try {
            try (HTTPMethod method = HTTPFactory.Get(session, url)) {
                result.status = method.execute();
                System.err.printf("\tglobal provider: status code = %d\n", result.status);
                //System.err.printf("\t|cache| = %d\n", HTTPCachingProvider.getCache().size());
                // Get the number of calls to the credentialer
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

/*
javax.net.debug=cmd

where cmd can be:

all            turn on all debugging
ssl            turn on ssl debugging

or

javax.net.debug=ssl:<option>:<option>...

where <option> is taken from the following:

The following can be used with ssl:
    record       enable per-record tracing
    handshake    print each handshake message
    keygen       print key generation data
    session      print session activity
    defaultctx   print default SSL initialization
    sslctx       print SSLContext tracing
    sessioncache print session cache tracing
    keymanager   print key manager tracing
    trustmanager print trust manager tracing
    pluggability print pluggability tracing

    handshake debugging can be widened with:
    data         hex dump of each handshake message
    verbose      verbose handshake message printing

    record debugging can be widened with:
    plaintext    hex dump of record plaintext
    packet       print raw SSL/TLS packets


Process finished with exit code 0
Empty test suite.
*/
