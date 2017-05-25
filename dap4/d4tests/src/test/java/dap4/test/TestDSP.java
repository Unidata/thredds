package dap4.test;

import dap4.core.data.DSP;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.DMRPrinter;
import dap4.dap4lib.DSPPrinter;
import dap4.dap4lib.FileDSP;
import dap4.dap4lib.HttpDSP;
import dap4.dap4lib.netcdf.Nc4DSP;
import dap4.servlet.DapCache;
import dap4.servlet.SynDSP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test some of the DSP classes:
 * AbstractDSP: tested by all the other DSPs
 * CDMDSP: tested elsewhere (TestServlet)
 * HttpDSP: tested elsewhere (TestCDMClient)
 * FileDSP: tested here
 * Nc4DSP: tested here
 * SynDSP: tested here
 * D4DSP: tested because superclass of FileDSP, HttpDSP, and SynDSP
 * ThreddsDSP: not directly tested anywhere yet
 */
public class TestDSP extends DapTestCommon
{
    static final boolean DEBUG = false;
    static final boolean SHOWTESTCASES = true;

    static final String BASEEXTENSION = "txt";

    static final String DAP4TAG = "#protocol=dap4";

    //////////////////////////////////////////////////
    // Constants

    static final String DATADIR = "src/test/data/resources"; // relative to dap4 root
    static final String BASELINEDIR = "TestDSP/baseline";
    static final String TESTCDMINPUT = "TestCDMClient/testinput";
    static final String TESTDSPINPUT = "TestDSP/testinput";
    static final String TESTFILESINPUT = "testfiles";

    static final String[] EXCLUDEDFILETESTS = new String[] {
    };

    //////////////////////////////////////////////////
    // Type Declarations

    static class TestCase
    {
        static protected String root = null;

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
            this.title = dataset;
            this.checksumming = csum;
            this.url = url;
            try {
                URL u = new URL(url);
                this.testpath = DapUtil.canonicalpath(u.getPath());
                int i = this.testpath.lastIndexOf('/');
                assert i > 0;
                this.dataset = this.testpath.substring(i + 1, this.testpath.length());
                // strip off any raw extension
                this.baselinepath = root + "/" + BASELINEDIR + "/" + this.dataset + "." + BASEEXTENSION;
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
    // Static variables and methods

    protected DSP
    dspFor(String surl)
    {
        URL url;
        try {
            url = new URL(surl);
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException("Malformed url: " + surl);
        }
        String proto = url.getProtocol();
        String path = url.getPath();
        int dot = path.lastIndexOf('.');
        if(dot < 0) dot = path.length();
        String ext = path.substring(dot, path.length());
        DSP dsp = null;
        try {
            if("file".equals(proto)) {
                // discriminate on the extensions
                if(".raw".equals(ext)) {
                    dsp = new FileDSP();
                } else if(".syn".equals(ext)) {
                    dsp = new SynDSP();
                } if(".nc".equals(ext)) {
                    dsp = new Nc4DSP();
                }
            } else if("http".equals(proto)
                    || "https".equals(url.getProtocol())) {
                dsp = new HttpDSP();
            }  else
                throw new IllegalArgumentException("Cannot determine DSP class for: " + surl);
        } catch (DapException de) {
            throw new IllegalArgumentException("Cannot create DSP  for: " + surl);
        }
        if(DEBUG)
            System.err.printf("DSP: %s%n",dsp.getClass().getName());
        return dsp;
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
        this.resourceroot = getResourceRoot();
        this.resourceroot = DapUtil.absolutize(this.resourceroot); // handle problem of windows paths
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
            if(ext.equals("syn"))
                return "file:/"
                        + this.resourceroot
                        + "/"
                        + TESTDSPINPUT;
            if(ext.equals("nc"))
                return "file:/"
                        + this.resourceroot
                        + "/"
                        + TESTFILESINPUT;
        }
        throw new IllegalArgumentException();
    }

    //////////////////////////////////////////////////
    // Define test cases

    void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate("file:", "test_struct_nested3.hdf5.raw");
            prop_visual = true;
            prop_baseline = false;
        } else {
            prop_baseline = false;
            for(TestCase tc : alltestcases) {
                if(DEBUG)
                    System.err.printf("Test case: %s%n", tc.dataset);
                chosentests.add(tc);
            }
        }
    }

    void
    defineAllTestcases()
    {
        List<String> matches = new ArrayList<>();
        String dir = TestCase.root + "/" + TESTCDMINPUT;
        TestFilter.filterfiles(dir, matches, "raw");
        if(false) {
            dir = TestCase.root + "/" + TESTFILESINPUT;
            TestFilter.filterfiles(dir, matches, "nc", "syn");
        }
        for(String f : matches) {
            boolean excluded = false;
            for(String x: EXCLUDEDFILETESTS) {
                if(f.indexOf(x) >= 0) {excluded = true; break;}
            }
            if(!excluded)
                add("file:/" + f);
        }
        if(SHOWTESTCASES) {
            for(int i=0;i<this.alltestcases.size();i++) {
                TestCase tc = this.alltestcases.get(i);
                System.err.printf("ALLTESTS: %s%n",tc.getURL());
            }
        }
    }

    protected void
    add(String url)
    {
        try {
            URL u = new URL(url);
            File f = new File(u.getPath());
            if(!f.canRead()) {
                System.err.println("Unreadable file test case: " + url);
            }
        } catch (MalformedURLException e) {
            System.err.println("Malformed file test case: " + url);
        }
        String ext = url.substring(url.lastIndexOf('.'), url.length());
        TestCase tc = new TestCase(url);
        for(TestCase t : this.alltestcases) {
            assert !t.getURL().equals(tc.getURL()) : "Duplicate TestCases: " + t;
        }
        this.alltestcases.add(tc);
    }

    //////////////////////////////////////////////////
    // Junit test method
    @Test
    public void testDSP()
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

        DSP dsp = dspFor(testcase.getURL());

        dsp.setContext(new DapContext());
        dsp.open(testcase.getURL());

        String metadata = dumpmetadata(dsp);
        if(prop_visual)
            visual(testcase.getURL() + ".dmr", metadata);
        String data = dumpdata(dsp);
        if(prop_visual)
            visual(testcase.getURL() + ".dap", data);

        String baselinefile = testcase.getBaseline();

        String testoutput = metadata + data;

        if(prop_baseline)
            writefile(baselinefile, testoutput);
        else if(prop_diff) { //compare with baseline
            // Read the baseline file(s)
            String baselinecontent = readfile(baselinefile);
            System.err.println("Comparison: vs " + baselinefile);
            Assert.assertTrue("*** FAIL", same(getTitle(), baselinecontent, testoutput));
        }
    }

    String dumpmetadata(DSP dsp)
            throws Exception
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        // Print the meta-databuffer using these args to NcdumpW
        DMRPrinter p = new DMRPrinter(dsp.getDMR(), pw);
        p.testprint();
        pw.close();
        sw.close();
        return sw.toString();
    }

    String dumpdata(DSP dsp)
            throws Exception
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        // Print the meta-databuffer using these args to NcdumpW
        DSPPrinter p = new DSPPrinter(dsp, pw).flag(DSPPrinter.Flags.CONTROLCHAR);
        p.print();
        pw.close();
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
