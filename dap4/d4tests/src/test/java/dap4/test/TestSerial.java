package dap4.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Test at the NetcdfDataset level; access .ser files on server.
 */
public class TestSerial extends DapTestCommon
{
    static protected final boolean DEBUG = false;

    static protected final boolean NCDUMP = true; // Use NcDumpW instead of D4Print

    static protected final String EXTENSION = (NCDUMP ? "ncdump" : "dmp");

    static protected final String SERIALEXTENSION = "ser";

    static protected final String[] EMPTY = new String[]{""};

    //////////////////////////////////////////////////
    // Constants

    static protected final String DATADIR = "d4tests/src/test/data"; // relative to dap4 root
    static protected final String TESTDATADIR = DATADIR + "/resources/TestCDMClient";
    static protected final String BASELINEDIR = TESTDATADIR + "/baseline";

    static protected final String alpha = "abcdefghijklmnopqrstuvwxyz"
        + "abcdefghijklmnopqrstuvwxyz".toUpperCase();

    //////////////////////////////////////////////////
    // Type Declarations

    static protected class ClientTest
    {
        static protected String root = null;
        static protected String server = null;

        String title;
        String dataset;
        String baselinepath;
        String[] constraints;

        ClientTest(String dataset)
        {
            this(dataset, EMPTY);
        }

        ClientTest(String dataset, String[] constraints)
        {
            this.title = dataset;
            this.dataset = dataset;
            this.baselinepath
                = root + "/" + BASELINEDIR + "/" + dataset;
            assert constraints != null && constraints.length > 0;
            this.constraints = constraints;
        }

        String makeurl(String ce)
        {
            String url = server + "/" + dataset + "." + SERIALEXTENSION;
            if(ce != null && ce.length() > 0) url += "?"+ DapTestCommon.CONSTRAINTTAG+"=" + ce;
            return url;
        }

        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(dataset);
            buf.append("{");
            if(constraints != null)
                for(int i = 0;i < constraints.length;i++) {
                    if(i > 0) buf.append(",");
                    String ce = constraints[i];
                    buf.append(ce == null ? "all" : ce);
                }
            buf.append("}");
            return buf.toString();
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Test cases

    protected List<ClientTest> alltestcases = new ArrayList<ClientTest>();
    protected List<ClientTest> chosentests = new ArrayList<ClientTest>();

    protected String root = null;
    protected String datasetpath = null;

    protected String sourceurl = null;

    //////////////////////////////////////////////////

    @Before
    public void setup() throws Exception {
        this.root = getDAP4Root();
        if(this.root == null)
            throw new Exception("dap4 root cannot be located");
        // Check for windows path
        if(alpha.indexOf(this.root.charAt(0)) >= 0 && this.root.charAt(1) == ':') {
        } else if(this.root.charAt(0) != '/')
            this.root = "/" + this.root;
        this.datasetpath = this.root + "/" + BASELINEDIR;
        findServer(this.datasetpath);
        this.sourceurl = d4tsServer;
        System.out.println("Using source url " + this.sourceurl);
        defineAllTestcases(this.root, this.sourceurl);
        chooseTestcases();
    }

    //////////////////////////////////////////////////
    // Define test cases

    void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate("test_atomic_array");
        } else {
            for(ClientTest tc : alltestcases)
                chosentests.add(tc);
        }
    }

    void
    defineAllTestcases(String root, String server)
    {
        ClientTest.root = root;
        ClientTest.server = server;
        alltestcases.add(new ClientTest("test_one_var"));
        alltestcases.add(new ClientTest("test_atomic_types"));
        alltestcases.add(new ClientTest("test_atomic_array"));

    }

    //////////////////////////////////////////////////
    // Junit test method

    @Category(NeedsExternalResource.class)
    @Test
    public void testSerial()
        throws Exception
    {
	org.junit.Assume.assumeTrue(usingIntellij);
        for(ClientTest testcase : chosentests) {
            if(!doOneTest(testcase)) {
                Assert.assertTrue(false);
            }
        }
    }

    //////////////////////////////////////////////////
    // Primary test method
    boolean
    doOneTest(ClientTest testcase)
        throws Exception
    {
        boolean pass = true;
        int testcounter = 0;

        System.out.println("Testcase: " + testcase.dataset);

        String[] constraints = testcase.constraints;
        for(int i = 0;i < constraints.length;i++) {
            String url = testcase.makeurl(constraints[i]);
            NetcdfDataset ncfile = null;
            try {
	        ncfile = openDataset(url);
            } catch (Exception e) {
                throw e;
            }

            String metadata = (NCDUMP ? ncdumpmetadata(ncfile) : null);
            String data = (NCDUMP ? ncdumpdata(ncfile) : null);

            if(prop_visual) {
                visual("DMR: " + url, metadata);
                visual("DAP: " + url, data);
            }

            String testoutput = (NCDUMP ? data : metadata + data);

            String baselinefile = String.format("%s.ser.%s",
                testcase.baselinepath,
                EXTENSION);
            if(prop_baseline)
                writefile(baselinefile, testoutput);

            if(prop_diff) { //compare with baseline
                // Read the baseline file(s)
                String baselinecontent = readfile(baselinefile);
                System.out.println("Comparison:");
                pass = pass && compare(baselinecontent, testoutput);
                System.out.println(pass ? "Pass" : "Fail");
            }
        }
        return pass;
    }

    //////////////////////////////////////////////////
    // Dump methods

    String ncdumpmetadata(NetcdfDataset ncfile)
    {
        boolean ok = false;
        String metadata = null;
        StringWriter sw = new StringWriter();

        // Print the meta-databuffer using these args to NcdumpW
        ok = false;
        try {
            ok = ucar.nc2.NCdumpW.print(ncfile, "-unsigned", sw, null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ok = false;
        }
        try {
            sw.close();
        } catch (IOException e) {
        }
        ;
        if(!ok) {
            System.err.println("NcdumpW failed");
            System.exit(1);
        }
        return sw.toString();
    }

    String ncdumpdata(NetcdfDataset ncfile)
    {
        boolean ok = false;
        StringWriter sw = new StringWriter();

        // Dump the databuffer
        sw = new StringWriter();
        ok = false;
        try {
            ok = ucar.nc2.NCdumpW.print(ncfile, "-vall -unsigned", sw, null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ok = false;
        }
        try {
            sw.close();
        } catch (IOException e) {
        }
        ;
        if(!ok) {
            System.err.println("NcdumpW failed");
            System.exit(1);
        }
        return sw.toString();
    }

    //////////////////////////////////////////////////
    // Utility methods


    // Locate the test cases with given prefix
    ClientTest
    locate1(String prefix)
    {
        List<ClientTest> tests = locate(prefix);
        assert tests.size() > 0;
        return tests.get(0);
    }

    //Locate the test cases with given prefix and optional constraint
    List<ClientTest>
    locate(String prefix)
    {
        List<ClientTest> results = new ArrayList<ClientTest>();
        for(ClientTest ct : this.alltestcases) {
            if(!ct.dataset.startsWith(prefix))
                continue;
            results.add(ct);
        }
        return results;
    }

    static protected boolean
    report(String msg)
    {
        System.err.println(msg);
        return false;
    }
////////////////////////////////////////
    // Stand alone

    static public void
    main(String[] argv)
    {
        try {
            new TestConstraints().testConstraints();
        } catch (Exception e) {
            System.err.println("*** FAIL");
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("*** PASS");
        System.exit(0);
    }// main

} // class TestConstraints
