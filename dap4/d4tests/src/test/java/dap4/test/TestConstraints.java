package dap4.test;

import dap4.dap4lib.XURI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test at the NetcdfDataset level
 */
public class TestConstraints extends DapTestCommon
{
    static final boolean DEBUG = false;

    static final public boolean DEBUGSERVER = true;

    //////////////////////////////////////////////////
    // Constants

    static final boolean NCDUMP = true; // Use NcDumpW instead of D4Print

    static final String BASEEXTENSION = "raw.txt";
    static final String TESTEXTENSION = ".raw";

    static final String DAP4TAG = "protocol=dap4";

    static protected final String SERVLETPATH = "d4ts";
    static protected final String RESOURCEPATH = "/src/test/data/resources";
    static protected final String TESTINPUTPATH = "/testfiles";
    static protected final String BASELINEDIR = "/TestConstraints/baseline";

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

        TestCase(int id, String dataset, String constraint)
        {
            if(idcheck[id])
                throw new IllegalStateException("two tests with same id");
            if(constraint != null && constraint.length() == 0)
                constraint = null;
            this.constraint = constraint;
            this.title = dataset + (constraint == null ? "" : "?" + constraint);
            this.dataset = dataset;
            this.id = id;
            idcheck[id] = true;
        }

        String
        getbaseline()
        {
            return canonjoin(this.baselinedir, dataset) + "." + id + "." + BASEEXTENSION;
        }

        String makeurl()
        {
            StringBuilder url = new StringBuilder();
            url.append("dap4://");
            url.append(server);
            url.append("/");
            url.append(servletpath);
            url.append(TESTINPUTPATH);
            url.append("/");
            url.append(dataset);
            if(constraint != null) {
                url.append("?");
                url.append(CONSTRAINTTAG);
                url.append("=");
                url.append(constraint);
            }
            url.append("#");
            url.append(DAP4TAG);
            return url.toString();
        }

        String makeName()
        {
            return this.dataset + "." + this.id;
        }

        public String toString()
        {
            return makeurl();
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Test cases

    protected List<TestCase> alltestcases = new ArrayList<TestCase>();
    protected List<TestCase> chosentests = new ArrayList<TestCase>();

    //////////////////////////////////////////////////

    @Before
    public void setup() throws Exception
    {
        String root = getDAP4Root();
        if(root == null)
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

    void
    chooseTestcases()
    {
        if(false) {
            chosentests.add(locate1(4));
            prop_visual = true;
        } else {
            prop_baseline = false;
            prop_visual = false;
            for(TestCase tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    void
    defineAllTestcases()
    {
        alltestcases.add(new TestCase(1, "test_one_vararray.nc", "t[1]"));
        alltestcases.add(new TestCase(2, "test_anon_dim.syn", "vu32[0:3]"));
        alltestcases.add(new TestCase(3, "test_one_vararray.nc", "t"));
        alltestcases.add(new TestCase(4, "test_enum_array.nc", "primary_cloud[1:2:4]"));
        alltestcases.add(new TestCase(5, "test_atomic_array.nc", "vu8[1][0:2:2];vd[1];vs[1][0];vo[0][1]"));
        alltestcases.add(new TestCase(6, "test_struct_array.nc", "s[0:2:3][0:1]"));
        alltestcases.add(new TestCase(7, "test_opaque_array.nc", "vo2[1:1][0,0]"));
        alltestcases.add(new TestCase(8, "test_atomic_array.nc", "v16[1:2,2]"));
        alltestcases.add(new TestCase(9, "test_atomic_array.nc", "v16[2,1:2]"));
    }

    //////////////////////////////////////////////////
    // Junit test method
    @Test
    public void testConstraints()
            throws Exception
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
        System.err.println("Testcase: " + testcase.makeurl());
        System.err.println("Baseline: " + testcase.getbaseline());

        String url = testcase.makeurl();
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

        String testoutput = (NCDUMP ? data : metadata + data);

        if(prop_baseline)
            writefile(testcase.getbaseline(), testoutput);

        if(prop_diff) { //compare with baseline
            // Read the baseline file(s)
            String baselinecontent = readfile(testcase.getbaseline());
            System.err.println("Comparison:");
            Assert.assertTrue("***Fail", same(getTitle(), baselinecontent, testoutput));
        }
    }

    //////////////////////////////////////////////////
    // Dump methods

    String ncdumpmetadata(NetcdfDataset ncfile, String datasetname)
    {
        boolean ok = false;
        String metadata = null;
        StringWriter sw = new StringWriter();
        StringBuilder args = new StringBuilder("-strict -unsigned");
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

        StringBuilder args = new StringBuilder("-strict -unsigned -vall");
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

    // Locate the test cases with given index
    TestCase
    locate1(int index)
    {
        List<TestCase> results = new ArrayList<TestCase>();
        for(TestCase ct : this.alltestcases) {
            if(ct.id == index)
                return ct;
        }
        return null;
    }

    // Locate the test cases with given prefix
    TestCase
    locate1(String prefix)
    {
        List<TestCase> tests = locate(prefix);
        assert tests.size() > 0;
        return tests.get(0);
    }

    //Locate the test cases with given prefix and optional constraint
    List<TestCase>
    locate(String prefix)
    {
        List<TestCase> results = new ArrayList<TestCase>();
        for(TestCase ct : this.alltestcases) {
            if(!ct.title.equals(prefix))
                continue;
            results.add(ct);
        }
        return results;
    }

    static boolean
    report(String msg)
    {
        System.err.println(msg);
        return false;
    }


}
