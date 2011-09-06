package ucar.nc2.util.net;

import junit.framework.TestCase;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.Credentials;
import org.junit.Test;

import java.io.*;
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


    @Test
    public void
    testSSH() throws Exception
    {
        junit.framework.Assert.assertTrue("testSSH", true);
    }

    @Test
    public void
    testBasic() throws Exception
    {
        String url = "http://www.giss.nasa.gov/staff/rschmunk/test/file1.nc";
        String user = "jcaron";
        String password = "boulder";
        CredentialsProvider provider = new HTTPBasicProvider(user, password);

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
        junit.framework.Assert.assertTrue("testBasic", pass);
    }

    public boolean
    testKeystore() throws Exception
    {
        String url = "http://ceda.ac.uk/dap/neodc/casix/seawifs_plankton/data/monthly/PSC_monthly_1998.nc.dds";
        String keystore = "c:/Users/dmh/IdeaProjects/MyProxyLogon/esgkeystore";
        String password = "anonymous";
        String truststore = "c:/Users/dmh/IdeaProjects/MyProxyLogon/esgtruststore";

        System.out.println("*** Testing: ESG Keystore-based Authorization");
        System.out.println("*** URL: " + url);


        HTTPSession session = new HTTPSession(url);
        HTTPMethod method = HTTPMethod.Get(session);
        CredentialsProvider provider = new HTTPSSLProvider(keystore,password,truststore,password);
        int status = method.execute();
        System.out.printf("Execute: status code = %d\n", status);
        pass = (status == 200);
        return pass;
    }

    @Test
    public void
    testSerialize() throws Exception
    {
        System.out.println("*** Testing: HTTPAuthStore (de-)serialization");

        boolean ok = true;
        CredentialsProvider creds1 = new HTTPBasicProvider("p1","pwd");
        CredentialsProvider creds2 = new HTTPSSLProvider("keystore","keystorepwd");
        CredentialsProvider creds3 = new HTTPBasicProvider("p3","pwd3");

        // Add some entries to HTTPAuthStore
        HTTPAuthStore.clear();
        HTTPAuthStore.insert(new HTTPAuthStore.Entry(
                HTTPAuthScheme.BASIC,
                "http://ceda.ac.uk/dap/neodc/casix/seawifs_plankton/data/monthly/PSC_monthly_1998.nc.dds",
                creds1)
        );
        HTTPAuthStore.insert(new HTTPAuthStore.Entry(
                HTTPAuthScheme.SSL,
                "http://ceda.ac.uk",
                creds2)
        );
        HTTPAuthStore.insert(new HTTPAuthStore.Entry(
                HTTPAuthScheme.BASIC,
                "http://ceda.ac.uk",
                creds3)
        );

        // Remove any old file
        File target1 = new File("./serial1");
        target1.delete();

        // serialize out
        OutputStream ostream = new FileOutputStream(target1);
        HTTPAuthStore.serialize(ostream, "password1");
        // Read in
        InputStream istream = new FileInputStream(target1);
        List<HTTPAuthStore.Entry> entries = HTTPAuthStore.getDeserializedEntries(istream, "password1");

        // compare
        List<HTTPAuthStore.Entry> rows = HTTPAuthStore.getAllRows();
        for (HTTPAuthStore.Entry row : rows) {
            HTTPAuthStore.Entry match = null;
            for (HTTPAuthStore.Entry e : entries) {
block:          {
                if (!HTTPAuthStore.Entry.identical(row,e)) break block;
                if(match == null)
                    match = e;
                else {System.out.println("ambigous match");  ok=false;}
                }
            }
            if (match == null) {
                System.out.println("no match for: " + row.toString());  ok=false;
            }
        }
        junit.framework.Assert.assertTrue("test(De-)Serialize", ok);
    }
}
