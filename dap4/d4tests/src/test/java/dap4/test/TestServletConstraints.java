package dap4.test;

import dap4.core.ce.parser.CEParserImpl;
import dap4.core.data.DSPRegistry;
import dap4.core.util.DapDump;
import dap4.core.util.Escape;
import dap4.dap4lib.ChunkInputStream;
import dap4.dap4lib.FileDSP;
import dap4.dap4lib.RequestMode;
import dap4.servlet.DapCache;
import dap4.servlet.Generator;
import dap4.servlet.SynDSP;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import thredds.server.dap4.Dap4Controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;


/**
 * TestServletConstraints has multiple purposes.
  * 1. It tests the d4tsservlet ability to serve up constrained data.
  * 2. It (optionally) stores the serialized raw databuffer
  * for datasets into files., These files are then used to
  * test client side deserialization (see TestCDMClient).
 */


public class TestServletConstraints extends DapTestCommon
{
    static public boolean DEBUG = false;
    static public boolean DEBUGDATA = false;
    static public boolean PARSEDEBUG = false;
    static public boolean CEPARSEDEBUG = false;

    static public boolean USEBIG = false;
    static public boolean NOCSUM = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String RESOURCEPATH = "/src/test/data/resources"; // wrt getTestInputFilesDir
    static protected final String TESTINPUTDIR = "/testfiles";
    static protected final String BASELINEDIR = "/TestServletConstraints/baseline";
    static protected final String GENERATEDIR = "/TestCDMClient/testinput";

    // constants for Fake Request
    static protected final String FAKEURLPREFIX = "/dap4";

    static protected final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    // Define the file extensions of interest for generation
    static protected final String[] GENEXTENSIONS = new String[]{".raw.dap", ".raw.dmr"};

    //////////////////////////////////////////////////
    // Type Declarations

    static protected class TestCase
    {
        static String inputroot = null;
        static String baselineroot = null;
        static String generateroot = null;

        static public void
        setRoots(String input, String baseline, String generate)
        {
            inputroot = input;
            baselineroot = baseline;
            generateroot = generate;
        }

        protected String title;
        protected String dataset;
        protected String[] extensions;
        protected boolean checksumming;
        protected Dump.Commands template;
        protected String testinputpath;
        protected String baselinepath;
        protected String generatepath;

        protected String constraint = null;
        protected int id;

        protected TestCase(int id, String dataset, String extensions, String ce,
                           Dump.Commands template)
        {
            this(id, dataset, extensions, ce, true, template);
        }

        protected TestCase(int id, String dataset, String extensions, String ce,
                           boolean checksumming,
                           Dump.Commands template)
        {
            this.id = id;
            this.title = dataset + (ce == null ? "" : ("?" + ce));
            this.constraint = ce;
            this.dataset = dataset;
            this.extensions = extensions.split(",");
            this.template = template;
            this.checksumming = checksumming;
            this.testinputpath = canonjoin(this.inputroot, dataset);
            this.baselinepath = canonjoin(this.baselineroot, dataset) + "." + id;
            this.generatepath = canonjoin(this.generateroot, dataset) + "." + id;
        }

        String makeurl(RequestMode ext)
        {
            String u = canonjoin(FAKEURLPREFIX, canonjoin(TESTINPUTDIR, dataset)) + "." + ext.toString();
            return u;
        }

        String makequery()
        {
            String query = "";
            if(this.constraint != null) {
                String ce = this.constraint;
                // Escape it
                ce = Escape.urlEncodeQuery(ce);
                query = ce;
            }
            return query;
        }

        public String makeBasepath(RequestMode mode)
        {
            String ext;
            switch (mode) {
            case DMR:
                return this.baselinepath + ".dmr";
            case DAP:
                return this.baselinepath + ".dap";
            default:
                break;
            }
            throw new UnsupportedOperationException("illegal mode: " + mode);
        }

        public String toString()
        {
            return dataset + "." + id;
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected MockMvc mockMvc;

    // Test cases

    protected List<TestCase> alltestcases = new ArrayList<TestCase>();

    protected List<TestCase> chosentests = new ArrayList<TestCase>();

    //////////////////////////////////////////////////

    @Before
    public void setup()
            throws Exception
    {
        super.bindstd();
        StandaloneMockMvcBuilder mvcbuilder =
                MockMvcBuilders.standaloneSetup(new Dap4Controller());
        mvcbuilder.setValidator(new TestServlet.NullValidator());
        this.mockMvc = mvcbuilder.build();
        testSetup();
        if(prop_ascii)
            Generator.setASCII(true);
        TestCase.setRoots(canonjoin(getResourceRoot(), TESTINPUTDIR),
                canonjoin(getResourceRoot(), BASELINEDIR),
                canonjoin(getResourceRoot(), GENERATEDIR));
        defineAllTestcases();
        chooseTestcases();
    }

    @After
    public void cleanup()
    {
        super.unbindstd();
    }


    //////////////////////////////////////////////////
    // Define test cases

    protected void
    chooseTestcases()
    {
        if(false) {
            if(false) {
                chosentests = locate(8);
            } else {
                List<TestCase> choose1 = locate(8);
                chosentests = locate(5);
                chosentests.addAll(choose1);
            }
            prop_visual = true;
            prop_debug = false;
            prop_generate = false;
            prop_baseline = true;
        } else {
            prop_baseline = false;
            prop_generate = false;
            for(TestCase tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    //////////////////////////////////////////////////
    // Junit test methods

    @Test
    public void testServletConstraints()
            throws Exception
    {
        DapCache.flush();
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
        System.err.println("Baseline: " + testcase.baselinepath);
        if(CEPARSEDEBUG) CEParserImpl.setGlobalDebugLevel(1);

        for(String extension : testcase.extensions) {
            RequestMode ext = RequestMode.modeFor(extension);
            switch (ext) {
            case DMR:
                dodmr(testcase);
                break;
            case DAP:
                dodata(testcase);
                break;
            default:
                Assert.assertTrue("Unknown extension", false);
            }
        }
    }

    void
    dodmr(TestCase testcase)
            throws Exception
    {
        String url = testcase.makeurl(RequestMode.DMR);
        String query = testcase.makequery();
        String basepath = testcase.makeBasepath(RequestMode.DMR);

        String little = (USEBIG ? "0" : "1");
        String nocsum = (NOCSUM ? "1" : "0");
        MvcResult result = perform(url, this.mockMvc,
                RESOURCEPATH,
                DapTestCommon.CONSTRAINTTAG, query,
                DapTestCommon.ORDERTAG, little,
                DapTestCommon.NOCSUMTAG, nocsum,
                DapTestCommon.TESTTAG, "true"
                );

        // Collect the output
        MockHttpServletResponse res = result.getResponse();
        byte[] byteresult = res.getContentAsByteArray();

        // Test by converting the raw output to a string
        String sdmr = new String(byteresult, UTF8);

        if(prop_visual)
            visual(testcase.title + ".dmr", sdmr);
        if(prop_baseline) {
            writefile(basepath, sdmr);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(basepath);
            System.err.println("DMR Comparison");
            Assert.assertTrue("***Fail", same(getTitle(), baselinecontent, sdmr));
        }
    }

    void
    dodata(TestCase testcase)
            throws Exception
    {
        String url = testcase.makeurl(RequestMode.DAP);
        String query = testcase.makequery();
        String basepath = testcase.makeBasepath(RequestMode.DAP);
        String little = (USEBIG ? "0" : "1");
        String nocsum = (NOCSUM ? "1" : "0");
        MvcResult result = perform(url, this.mockMvc,
                RESOURCEPATH,
                DapTestCommon.CONSTRAINTTAG, query,
                DapTestCommon.ORDERTAG, little,
                DapTestCommon.NOCSUMTAG, nocsum,
                DapTestCommon.TESTTAG, "true"
        );

        // Collect the output
        MockHttpServletResponse res = result.getResponse();
        byte[] byteresult = res.getContentAsByteArray();

        if(prop_debug || DEBUG) {
            DapDump.dumpbytestream(byteresult, ByteOrder.nativeOrder(), "TestServletConstraint.dodata");
        }

        if(prop_generate) {
            // Dump the serialization into a file; this also includes the dmr
            String target = testcase.generatepath + ".raw";
            writefile(target, byteresult);
        }

        // Setup a ChunkInputStream
        ByteArrayInputStream bytestream = new ByteArrayInputStream(byteresult);

        ChunkInputStream reader = new ChunkInputStream(bytestream, RequestMode.DAP, ByteOrder.nativeOrder());

        String sdmr = reader.readDMR(); // Read the DMR
        if(prop_visual)
            visual(testcase.title + ".dmr.dap", sdmr);

        Dump printer = new Dump();
        String sdata = printer.dumpdata(reader, testcase.checksumming, reader.getRemoteByteOrder(), testcase.template);

        if(prop_visual)
            visual(testcase.title + ".dap", sdata);

        if(prop_baseline)
            writefile(basepath, sdata);

        if(prop_diff) {
            //compare with baseline
            // Read the baseline file
            System.err.println("Data Comparison:");
            String baselinecontent = readfile(testcase.baselinepath + ".dap");
            Assert.assertTrue("***Fail", same(getTitle(), baselinecontent, sdata));
        }
    }

    //////////////////////////////////////////////////

    protected void
    defineAllTestcases()
    {
        this.alltestcases.add(
                new TestCase(1, "test_one_vararray.nc", "dmr,dap", "/t[1]",
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('S', 4);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase(2, "test_anon_dim.syn", "dmr,dap", "/vu32[0:3]",  // test for dimension inclusion
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('U', 4);
                                printer.printvalue('U', 4);
                                printer.printvalue('U', 4);
                                printer.printvalue('U', 4);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase(3, "test_one_vararray.nc", "dmr,dap", "/t",  // test for dimension inclusion
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('S', 4);
                                printer.printvalue('S', 4);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase(4, "test_enum_array.nc", "dmr,dap", "/primary_cloud[1:2:4]",
                        // 2 S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('U', 1, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase(5, "test_atomic_array.nc", "dmr,dap", "/vu8[1][0:2:2];/vd[1];/vs[1][0];/vo[0][1]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('U', 1, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 1; i++) {
                                    printer.printvalue('F', 8, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 1; i++) {
                                    printer.printvalue('T', 0, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 1; i++) {
                                    printer.printvalue('O', 0, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase(6, "test_struct_array.nc", "dmr,dap", "/s[0:2:3][0:1]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 4; i++) {
                                    for(int j = 0; j < 2; j++) {
                                        printer.printvalue('S', 4);
                                    }
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase(7, "test_opaque_array.nc", "dmr,dap", "/vo2[1][0:1]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('O', 0, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase(8, "test_atomic_array.nc", "dmr,dap", "/v16[0:1,3]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 3; i++) {
                                    printer.printvalue('S', 2, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase(9, "test_atomic_array.nc", "dmr,dap", "/v16[3,0:1]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 3; i++) {
                                    printer.printvalue('S', 2, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
    }

    //////////////////////////////////////////////////
    // Utility methods


    // Locate the test cases with given prefix
    List<TestCase>
    locate(Object tag)
    {
        List<TestCase> results = new ArrayList<TestCase>();
        for(TestCase ct : this.alltestcases) {
            if(tag instanceof Integer && ct.id == (Integer) tag) {
                results.add(ct);
                break;
            } else if(tag instanceof String && ct.title.equals((String) tag)) {
                results.add(ct);
                break;
            }
        }
        return results;
    }
}
