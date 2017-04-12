/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package dap4.test;

import com.sun.org.apache.xpath.internal.axes.ReverseAxesWalker;
import dap4.core.util.DapUtil;
import dap4.servlet.DapCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Client side access
 */

public class TestCDMClient extends DapTestCommon
{
    static final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static final String BASEEXTENSION = ".txt";
    static final String INPUTEXTENSION = ".raw";

    static final String DAP4TAG = "#protocol=dap4";

    static final String DATADIR = "src/test/data/resources"; // relative to dap4 root
    static final String BASELINEDIR = "TestCDMClient/baseline";
    static final String TESTCDMINPUT = "TestCDMClient/testinput";
    static final String TESTFILESINPUT = "testfiles";

    static final String[] EXCLUDEDFILETESTS = new String[]{
            "test_sequence_2.syn.raw"
    };

    //////////////////////////////////////////////////
    // Type Declarations

    static class TestCase
    {
        static private String root = null;

        static void setRoot(String r)
        {
            root = r;
        }

        static String getRoot()
        {
            return root;
        }

        /////////////////////////

        private String title;
        private String dataset;
        private String ext;
        private boolean checksumming;
        private String testpath;
        private String baselinepath;
        private String url;

        TestCase(String url)
        {
            this(url, true);
        }

        TestCase(String url, boolean csum)
        {
            try {
                URL u = new URL(url);
                this.title = u.getPath();
            } catch (MalformedURLException e) {
                this.title = "unknown";
            }
            this.checksumming = csum;
            this.url = url;
            try {
                URL u = new URL(url);
                this.testpath = DapUtil.canonicalpath(u.getPath());
                int i = this.testpath.lastIndexOf('/');
                assert i > 0;
                this.dataset = this.testpath.substring(i + 1, this.testpath.length());
                // strip off any raw extension
                if(this.dataset.endsWith(INPUTEXTENSION))
                    this.dataset = this.dataset.substring(0,this.dataset.length() - INPUTEXTENSION.length());
                this.baselinepath = root + "/" + BASELINEDIR + "/" + this.dataset + INPUTEXTENSION + BASEEXTENSION;
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(url);
            }
        }

        public String getURL()
        {
            return this.url + DAP4TAG;
        }

        public String getPath()
        {
            return this.testpath;
        }

        public String getDataset()
        {
            return this.dataset;
        }

        public String getBaseline()
        {
            return this.baselinepath;
        }

        public String getTitle()
        {
            return this.title;
        }

        public String toString()
        {
            return this.url;
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Test cases

    protected List<TestCase> alltestcases = new ArrayList<TestCase>();
    protected List<TestCase> chosentests = new ArrayList<TestCase>();

    protected String resourceroot = null;

    //////////////////////////////////////////////////

    @Before
    public void setup() throws Exception
    {
        DapCache.flush();
        testSetup();
        this.resourceroot = getResourceRoot();
        TestCase.setRoot(resourceroot);
        defineAllTestcases();
        chooseTestcases();
    }

    // convert an extension to a file or url prefix
    String
    prefix(String scheme, String ext)
    {
        if(ext.charAt(0) == '.') ext = ext.substring(1);
        if(scheme.startsWith("http")) {
            return "http://"
                    + TestDir.dap4TestServer
                    + "/d4ts";
        } else if(scheme.equals("file")) {
            if(ext.equals("raw"))
                return "file:/"
                        + this.resourceroot
                        + "/"
                        + TESTCDMINPUT;
        }
        throw new IllegalArgumentException();
    }

    //////////////////////////////////////////////////
    // Define test cases

    void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate("file:", "test_atomic_array.nc.raw");
            prop_visual = true;
            prop_baseline = false;
        } else {
            prop_baseline = false;
            for(TestCase tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    void
    defineAllTestcases()
    {
        System.err.printf("pwd=%s%n", System.getProperty("user.dir"));
        List<String> matches = new ArrayList<>();
        String dir = TestCase.getRoot() + "/" + TESTCDMINPUT;
        TestFilter.filterfiles(dir, matches, "raw");
        for(String f : matches) {
            boolean excluded = false;
            for(String x : EXCLUDEDFILETESTS) {
                if(f.endsWith(x)) {
                    excluded = true;
                    break;
                }
            }
            if(!excluded) {
                add(f);
            }
        }
    }

    protected void
    add(String path)
    {
        File f = new File(path);
        if(!f.exists())
            System.err.println("Non existent file test case: " + path);
        else if(!f.canRead())
            System.err.println("Unreadable file test case: " + path);
        String ext = path.substring(path.lastIndexOf('.'), path.length());
        String url = "file://"+path;
        try {
            URL u = new URL(url);
            System.err.printf("Testcase: add: %s  path=%s%n",u.toString(),u.getPath());
        } catch (MalformedURLException e) {
            System.err.println("Malformed file test case: " + url);
        }
        TestCase tc = new TestCase(url);
        for(TestCase t: this.alltestcases) {
            assert !t.getURL().equals(tc.getURL()) : "Duplicate TestCases: " + t;
        }
        this.alltestcases.add(tc);
    }

    //////////////////////////////////////////////////
    // Junit test method
    @Test
    public void testCDMClient()
            throws Exception
    {
        for(TestCase testcase : chosentests) {
            doOneTest(testcase);
        }
        System.err.println("*** PASS");
    }

    //////////////////////////////////////////////////
    // Primary test method
    void
    doOneTest(TestCase testcase)
            throws Exception
    {
        System.err.println("Testcase: " + testcase.getURL());
        System.err.println("Baseline: " + testcase.getBaseline());

        NetcdfDataset ncfile;
        try {
            ncfile = openDataset(testcase.getURL());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("File open failed: " + testcase.getURL(), e);
        }
        assert ncfile != null;

        String datasetname = testcase.getDataset();
        String data = dumpdata(ncfile, datasetname);

        if(prop_visual) {
            visual(testcase.getTitle() + ".dap", data);
        }
        String baselinefile = testcase.getBaseline();

        if(prop_baseline)
            writefile(baselinefile, data);
        else if(prop_diff) { //compare with baseline
            // Read the baseline file(s)
            String baselinecontent = readfile(baselinefile);
            System.err.println("Comparison: vs " + baselinefile);
            Assert.assertTrue("*** FAIL", same(getTitle(), baselinecontent, data));
        }
    }

    String dumpmetadata(NetcdfDataset ncfile, String datasetname)
            throws Exception
    {
        StringWriter sw = new StringWriter();
        StringBuilder args = new StringBuilder("-strict -unsigned");
        if(datasetname != null) {
            args.append(" -datasetname ");
            args.append(datasetname);
        }
        // Print the meta-databuffer using these args to NcdumpW
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
                throw new Exception("NcdumpW failed");
        } catch (IOException ioe) {
            throw new Exception("NcdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    String dumpdata(NetcdfDataset ncfile, String datasetname)
            throws Exception
    {
        StringBuilder args = new StringBuilder("-strict -unsigned -vall");
        if(datasetname != null) {
            args.append(" -datasetname ");
            args.append(datasetname);
        }
        StringWriter sw = new StringWriter();
        // Dump the databuffer
        sw = new StringWriter();
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
                throw new Exception("NCdumpW failed");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new Exception("NCdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    //////////////////////////////////////////////////
    // Utility methods

    //Locate the test cases with given prefix
    List<TestCase>
    locate(String scheme, String s)
    {
        return locate(scheme, s, null);
    }

    List<TestCase>
    locate(String scheme, String s, List<TestCase> list)
    {
        if(list == null) list = new ArrayList<>();
        int matches = 0;
        for(TestCase ct : this.alltestcases) {
            if(!ct.getURL().startsWith(scheme)) continue;
            if(ct.getPath().endsWith(s)) {
                matches++;
                list.add(ct);
            }
        }
        assert matches > 0 : "No such testcase: " + s;
        return list;
    }

    static boolean
    report(String msg)
    {
        System.err.println(msg);
        return false;
    }

}
