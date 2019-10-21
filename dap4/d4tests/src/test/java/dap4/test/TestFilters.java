package dap4.test;

import dap4.dap4lib.ChunkInputStream;
import dap4.core.util.*;
import dap4.dap4lib.RequestMode;
import dap4.dap4lib.XURI;
import dap4.servlet.Generator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * TestFilter tests server side
 * filter processing.
 */

public class TestFilters extends DapTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    //////////////////////////////////////////////////
    // Constants
    static boolean DEBUGSERVER = true;
    static final boolean NCDUMP = true; // Use NcDumpW instead of D4Print

    static protected final String SERVLETPATH = "d4ts";
    static protected final String RESOURCEPATH = "/src/test/data/resources";
    static protected final String TESTINPUTPATH = "/testfiles";
    static protected final String BASELINEDIR = "/TestFilters/baseline";

    static final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    static final int DEFAULTROWCOUNT = 5;

    //////////////////////////////////////////////////
    // Type Declarations

    static class TestCase
    {
        static String servletpath = null;
        static String baselinedir = null;
        static String server = null;

        static public void
        setRoots(String servletpath, String baselinedir, String server)
        {
            TestCase.baselinedir = baselinedir;
            TestCase.servletpath = servletpath;
            TestCase.server = server;
        }

        static boolean[] idcheck = new boolean[2048];

        //////////////////////////////////////////////////
        String title;
        String dataset;
        String constraint;
        int id;
        boolean xfail;
        int rowcount = DEFAULTROWCOUNT;

        TestCase(int id, String dataset, String ce)
        {
            this(id, dataset, 0, ce, false);
        }

        TestCase(int id, String dataset, int rows, String ce, boolean xfail)
        {
	    if(idcheck[id])
                throw new IllegalStateException("two tests with same id");
            if(ce != null && ce.length() == 0)
                ce = null;
            this.id = id;
            idcheck[id] = true;
            this.title = dataset + (ce == null ? "" : ("?" + ce));
            this.dataset = dataset;
            this.constraint = ce;
            this.xfail = xfail;
            this.rowcount = rows == 0 ? DEFAULTROWCOUNT : rows;
        }
// this.testinputpath = root + "/" + TESTINPUTDIR + "/" + dataset;
// this.baselinepath = root + "/" + BASELINEDIR + "/" + dataset + "." + String.valueOf(this.id);

        String
        getbaseline(RequestMode mode)
        {
            return canonjoin(this.baselinedir, dataset) + "." + this.id + "." + mode.toString();
        }

        String makeurl(RequestMode ext)
        {
            StringBuilder url = new StringBuilder();
            url.append("http://");
            url.append(server);
            url.append("/");
            url.append(servletpath);
            url.append(TESTINPUTPATH);
            url.append("/");
            url.append(dataset);
            if(ext != null) {url.append("."); url.append(ext.toString());}
            if(constraint != null) {
                url.append("?");
                url.append(CONSTRAINTTAG);
                url.append("=");
                // Escape it
                //ce = Escape.urlEncodeQuery(ce);
                url.append(constraint);
            }
            url.append(DAP4MODE);
            return url.toString();
        }

        String makeName()
        {
            return this.dataset + "." + this.id;
        }

        public String toString()
        {
            return makeurl(null);
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Misc variables
    boolean isbigendian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    // Test cases

    List<TestCase> alltestcases = new ArrayList<>();
    List<TestCase> chosentests = new ArrayList<>();

    //////////////////////////////////////////////////

    @Before
    public void setup() throws Exception {
	String d4root = getDAP4Root();
        if(d4root == null)
            throw new Exception("dap4 root cannot be located");
	    testSetup();
        if(DEBUGSERVER)
            HTTPMethod.MOCKEXECUTOR = new MockExecutor(getResourceRoot());
        TestCase.setRoots(
                SERVLETPATH,
                canonjoin(getResourceRoot(), BASELINEDIR),
                TestDir.dap4TestServer);
        defineAllTestcases();
        chooseTestcases();
    }

    //////////////////////////////////////////////////
    // Define test cases

    protected void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate(2);
            prop_visual = true;
            prop_baseline = true;
        } else {
            prop_baseline = true;
            for(TestCase tc : alltestcases)
                chosentests.add(tc);
        }
    }

    protected void
    defineAllTestcases()
    {
        this.alltestcases.add(
                new TestCase(1, "test_sequence_1.syn", "/s"));
        this.alltestcases.add(
                new TestCase(2, "test_sequence_1.syn", "/s|i1<0"));
    }
    ///////////////////////////////////////////////
    @Test
    public void testFilters()
        throws Exception///
    // Junit test methods
    {
        for(TestCase testcase : chosentests) {
            doOneTest(testcase);
        }
    }

    //////////////////////////////////////////////////
    // Primary test method
    void
    doOneTest(TestCase testcase)
        throws Exception
    {
        System.err.println("Testcase: " + testcase.toString());

        String baselinepath = testcase.getbaseline(RequestMode.DAP);

        System.err.println("Baseline: " + baselinepath);
        Generator.setRowCount(testcase.rowcount);

        String url = testcase.makeurl(RequestMode.DAP);
        // Make sure url is escaped
        url = new XURI(url).assemble(XURI.URLALL);
        NetcdfDataset ncfile = null;
        try {
            ncfile = openDataset(url);
        } catch (Exception e) {
            throw e;
        }

        // Patch the ncfile to change dataset name
        String datasetname = extractDatasetname(url,Integer.toString(testcase.id));
        String metadata = (NCDUMP ? ncdumpmetadata(ncfile,datasetname) : null);
        String data = (NCDUMP ? ncdumpdata(ncfile,datasetname) : null);

        if(prop_visual) {
            visual("DMR: " + url, metadata);
            visual("DAP: " + url, data);
        }

        if(prop_debug) {
            ByteOrder order = (isbigendian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        }

        if(prop_baseline)
            writefile(baselinepath, data);

        if(prop_diff) { //compare with baseline
            // Read the baseline file(s)
            String baselinecontent = readfile(baselinepath);
            System.err.println("Comparison:");
            Assert.assertTrue("***Fail", same(getTitle(), baselinecontent, data));
        }
    }

    //////////////////////////////////////////////////
    // Dump methods

    String ncdumpmetadata(NetcdfDataset ncfile, String datasetname)
    {
        boolean ok = false;
        String metadata = null;
        StringWriter sw = new StringWriter();
        StringBuilder args = new StringBuilder("-strict");
        if(datasetname != null) {
            args.append(" -datasetname ");
            args.append(datasetname);
        }
        // Print the meta-databuffer using these args to NcdumpW
        ok = false;
        try {
            ok = ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null);
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
        }
        return sw.toString();
    }

    String ncdumpdata(NetcdfDataset ncfile, String datasetname)
    {
        boolean ok = false;
        StringWriter sw = new StringWriter();

        StringBuilder args = new StringBuilder("-strict -vall");
        if(datasetname != null) {
            args.append(" -datasetname ");
            args.append(datasetname);
        }

        // Dump the databuffer
        sw = new StringWriter();
        ok = false;
        try {
            ok = ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null);
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
        }
        return sw.toString();
    }

    //////////////////////////////////////////////////
    // Utility methods

    // Locate the test cases with given prefix
    List<TestCase>
    locate(Object pattern)
    {
        List<TestCase> results = new ArrayList<TestCase>();
        for(TestCase ct : this.alltestcases) {
            if(pattern instanceof String) {
            if(ct.title.equals(pattern.toString()))
                results.add(ct);
            } else if(pattern instanceof Integer) {
                if(ct.id == (Integer)pattern)
                    results.add(ct);
            }
        }
        return results;
    }

}

