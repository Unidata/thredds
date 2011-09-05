package ucar.nc2.util.net;

import junit.framework.TestCase;
import opendap.dap.*;
import opendap.util.InvalidSwitch;
import ucar.nc2.util.net.HTTPAuthStore;
import ucar.nc2.util.net.HTTPMethod;
import ucar.nc2.util.net.HTTPSession;
import opendap.dap.parsers.*;
import opendap.util.Getopts;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.params.HttpParams;
import sun.net.www.protocol.http.HttpAuthenticator;

import java.io.*;
import java.nio.CharBuffer;
import java.util.List;

public class TestAuth extends TestCase
{

    static boolean debug = false;
    static boolean createbaseline = true;

    final String TITLE = "DAP Authorization Mechanism Tests";

//////////////////////////////////////////////////
//////////////////////////////////////////////////

// Define the test sets

    int passcount = 0;
    int xfailcount = 0;
    int failcount = 0;
    boolean verbose = true;
    String test = null;
    String testname = null;
    String testno = null;
    String testdataname = null;
    String url = null;
    boolean pass = false;

    public TestAuth(String name, String testdir)
    {
        super(name);
    }

    public TestAuth(String name)
    {
        this(name, null);
    }

    protected void
    setUp()
    {
        passcount = 0;
        xfailcount = 0;
        failcount = 0;
    }

    // HTTP basic password-based authorization
    static public class BasicProviderTest implements CredentialsProvider, Serializable
    {
        String user;
        String pwd;

        public BasicProviderTest() {}

        public BasicProviderTest(String user, String pwd)
        {
            setUserNamePassword(user,pwd);
        }

        public void setUserNamePassword(String user, String pwd)
        {
            this.user = user;
            this.pwd = pwd;
        }

        public Credentials getCredentials(AuthScheme authScheme, String s, int i, boolean b)
                throws CredentialsNotAvailableException
        {
            return new UsernamePasswordCredentials(user, pwd);

        }
    }

    public boolean
    testSSH() throws Exception
    {
        return true;
    }

    public boolean
    testBasic() throws Exception
    {
        String url = "http://www.giss.nasa.gov/staff/rschmunk/test/file1.nc";
        String user = "jcaron";
        String password = "boulder";
        CredentialsProvider provider = new BasicProviderTest(user, password);

        System.out.println("*** Testing: Http Basic Password Authorization");
        System.out.println("*** URL: " + url);

        // Test local credentials provider
        HTTPSession session = new HTTPSession(url);
        session.setCredentialsProvider(provider);
        HTTPMethod method = HTTPMethod.Get(session);
        int status = method.execute();
        System.out.printf("Local provider test: status code = %d\n", status);
        pass = (status == 200);
        if(pass) {
            // Test global credentials provider
            HTTPSession.setGlobalCredentialsProvider(provider);
            session = new HTTPSession(url);
            method = HTTPMethod.Get(session);
            status = method.execute();
            System.out.printf("Global provider test: status code = %d\n", status);
            pass = (status == 200);
        }
        return pass;
    }

    public boolean
    testESG() throws Exception
    {
        String url = "http://ceda.ac.uk/dap/neodc/casix/seawifs_plankton/data/monthly/PSC_monthly_1998.nc.dds";
        String keystore = "c:/Users/dmh/IdeaProjects/MyProxyLogon/esgkeystore";
        String password = "anonymous";
        String truststore = "c:/Users/dmh/IdeaProjects/MyProxyLogon/esgtruststore";

        System.out.println("*** Testing: ESG Keystore-based Authorization");
        System.out.println("*** URL: " + url);


        HTTPSession session = new HTTPSession(url);
        HTTPMethod method = HTTPMethod.Get();
        CredentialsProvider provider = new
        int status = method.execute();
        System.out.printf("Execute: status code = %d\n", status);
        pass = (status == 200);
        return pass;
    }

    public boolean
    testSerialize() throws Exception
    {
        System.out.println("*** Testing: HTTPAuthStore (de-)serialization");

        boolean ok = true;
        HTTPCreds creds1 = new HTTPCreds();
        HTTPCreds creds2 = new HTTPCreds();
        HTTPCreds creds3 = new HTTPCreds();

        creds1.schemeHttpBasic(new BasicProviderTest("p1","pwd"));
        creds2.schemeKeystore("keystore","keystorepwd");
        creds3.schemeOther("p3@pwd");

        // Add some entries to HTTPAuthStore
        HTTPAuthStore.clear();
        HTTPAuthStore.insert(
                "pr1",
                "http://ceda.ac.uk/dap/neodc/casix/seawifs_plankton/data/monthly/PSC_monthly_1998.nc.dds",
                HTTPAuthStore.Scheme.BASIC,
                creds1
        );
        HTTPAuthStore.insert(
                "pr2",
                "http://ceda.ac.uk",
                HTTPAuthStore.Scheme.OTHER,
                creds2
        );
        HTTPAuthStore.insert(
                "pr3",
                "http://ceda.ac.uk",
                HTTPAuthStore.Scheme.KEYSTORE,
                creds3
        );
        // Remove any old file
        File target1 = new File("./serial1");
        target1.delete();

        // serialize out
        OutputStream ostream = new FileOutputStream(target1);
        HTTPAuthStore.serializeAll(ostream, "password1");
        // Read in
        InputStream istream = new FileInputStream(target1);
        List<HTTPAuthStore.Entry> entries = HTTPAuthStore.getDeserializedEntries(istream, "password1");

        // compare
        List<HTTPAuthStore.Entry> rows = HTTPAuthStore.getAllRows();
        for (HTTPAuthStore.Entry row : rows) {
            HTTPAuthStore.Entry match = null;
            for (HTTPAuthStore.Entry e : entries) {
block:          {
                if (!row.host.equals(e.host)) break block;
                else if(row.port != e.port) break block;
                //else if(!row.path.equals(e.path)) break block;
                else if(row.scheme != e.scheme) break block;
                if(match == null)
                    match = e;
                else {System.out.println("ambigous match");  ok=false;}
                }
            }
            if (match == null)
            {System.out.println("no match for: " + row.toString());  ok=false;}
        }
        return ok;
    }

    public void testAuth() throws Exception
    {
        //System.setProperty("javax.net.debug","all");
        System.out.printf("*** Testing %s\n", TITLE);

        if(!testSSH()) failcount++ ; else passcount++;
        if(!testBasic()) failcount++ ; else passcount++;
        if (!testESG()) failcount++; else passcount++;
        if (!testSerialize()) failcount++;
        else passcount++;

//    testProxy();

        int totalcount = passcount + failcount;
        int okcount = passcount;

        System.out.printf("*** PASSED: %d/%d; %d expected failures; %d unexpected failures\n", okcount, totalcount, xfailcount, failcount);
        if (failcount > 0)
            junit.framework.Assert.assertTrue(testname, false);

    }

    public static void main(String args[]) throws Exception
    {
        Getopts opts = null;
        try {
            opts = new Getopts("d", args);
            if (opts.getSwitch('d').set) {
                debug = true;
            }
        } catch (InvalidSwitch is) {
            throw new Exception(is);
        }
        String testdir = null;
        if (opts.argList().length > 0) testdir = opts.argList()[0];
        else testdir = ".";
        new TestAuth("TestAuth", testdir).testAuth();
    }

}
