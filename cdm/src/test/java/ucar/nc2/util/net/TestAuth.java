package ucar.nc2.util.net;

import junit.framework.TestCase;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.Credentials;
import org.junit.Test;
import ucar.nc2.iosp.uamiv.UAMIVServiceProvider;

import java.io.*;
import java.util.List;

public class TestAuth extends TestCase
{
    // TODO: add proxy and digest tests

    static boolean debug = false;

    final String TITLE = "DAP Authorization Mechanism Tests";

//////////////////////////////////////////////////
//////////////////////////////////////////////////

// Define the test sets

    int passcount = 0;
    int xfailcount = 0;
    int failcount = 0;
    boolean verbose = true;
    boolean pass = false;

    public TestAuth(String name, String testdir)
    {
        super(name);
    }

    public TestAuth(String name)
    {
        this(name, null);
    }


    static String[] sshurls = {
            "https://motherlode.ucar.edu:8443/thredds/fileServer/station/profiler/wind/1hr/20110919/PROFILER_wind_01hr_20110919_2000.nc"
    };

    @Test
    public void
    testSSH() throws Exception
    {
        System.out.println("*** Testing: Simple Https");
        for(String url: sshurls) {
            System.out.println("*** URL: " + url);
            HTTPSession session = new HTTPSession(url);
            HTTPMethod method = HTTPMethod.Get(session);
            int status = method.execute();
            System.out.printf("\tstatus code = %d\n", status);
            pass = (status == 200);
            junit.framework.Assert.assertTrue("testSSH", pass);
        }
    }

    static class AuthDataBasic
    {
        String url;
        String user;
        String password;
        public AuthDataBasic(String url, String usr, String pwd)
        {
            this.url = url;
            this.user = usr;
            this.password = pwd;
        }
    }

    static AuthDataBasic[] basictests = {
            new AuthDataBasic("http://www.giss.nasa.gov/staff/rschmunk/test/file1.nc","jcaron","boulder"),
            //new AuthDataBasic("http://motherlode.ucar.edu/thredds/dodsC/restrict/testdata/testData.nc.html","tiggeUser","tigge")
    };

    @Test
    public void
    testBasic() throws Exception
    {
        System.out.println("*** Testing: Http Basic Password Authorization");

        for(AuthDataBasic data: basictests) {
            CredentialsProvider provider = new HTTPBasicProvider(data.user, data.password);
            System.out.println("*** URL: " + data.url);
            // Test local credentials provider
            HTTPSession session = new HTTPSession(data.url);
            session.setCredentialsProvider(provider);
            HTTPMethod method = HTTPMethod.Get(session);
            int status = method.execute();
            System.out.printf("\tlocal provider: status code = %d\n", status);  System.out.flush();
            session.setCredentialsProvider(null);
            pass = (status == 200);
            if(pass) System.out.println("\tLocal test passed");

            if(pass) {
                // Test global credentials provider
                HTTPSession.setGlobalCredentialsProvider(provider);
                session = new HTTPSession(data.url);
                method = HTTPMethod.Get(session);
                status = method.execute();
                System.out.printf("\tglobal provider test: status code = %d\n", status);  System.out.flush();
                pass = (status == 200);
                if(pass) System.out.println("\tGlobal test passed");

            }
            if(pass)
                junit.framework.Assert.assertTrue("testBasic", true);
            else
                junit.framework.Assert.assertTrue("testBasic", false);
        }
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
