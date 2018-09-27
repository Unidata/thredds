/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package dap4.test;

import dap4.core.dmr.parser.DOM4Parser;
import dap4.core.util.DapDump;
import dap4.dap4lib.ChunkInputStream;
import dap4.dap4lib.RequestMode;
import dap4.servlet.DapCache;
import dap4.servlet.Generator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import thredds.server.dap4.Dap4Controller;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.jni.netcdf.Nc4wrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * TestServlet has multiple purposes.
 * 1. It tests the d4tsservlet.
 * 2. It (optionally) stores the serialized raw databuffer
 * for datasets into files., These files are then used to
 * test client side deserialization (see TestCDMClient).
 */

/*
Normally, we would like to use Spring applicationContext
and autowiring for this class.
This can work under, say, Jenkins or Travis, but
it fails under Intellij at the moment because of Mocking.
I have managed to get it to work partly, but currently it
crashes trying to initialize the ChronicleMap cache.
It should be noted that AFAIK none of the Mocking tests will
work under Intellij; TestServlet is just one example.

I have included the necessary changes marked with the tag
USESPRING to remind me of what needs to be done someday.
*/

/* USESPRING
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(
        locations = {"/WEB-INF/applicationContext.xml", "/WEB-INF/spring-servlet.xml"},
        loader = MockTdsContextLoader.class)
*/

public class TestServlet extends DapTestCommon
{
    static final boolean USESPRING = false;

    static public boolean DEBUG = false;
    static public boolean DEBUGDATA = false;
    static public boolean PARSEDEBUG = false;

    static public boolean USEBIG = false;
    static public boolean NOCSUM = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String RESOURCEPATH = "/src/test/data/resources"; // wrt getTestInputFilesDir
    static protected final String TESTINPUTDIR = "/testfiles";
    static protected final String BASELINEDIR = "/TestServlet/baseline";
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

        public TestCase(String dataset, String extensions, boolean checksumming)
        {
            this(dataset, extensions, checksumming, null);
        }

        public TestCase(String dataset, String extensions,
                        boolean checksumming,
                        Dump.Commands template)
        {
            this.title = dataset;
            this.dataset = dataset;
            this.extensions = extensions.split(",");
            this.template = template;
            this.checksumming = checksumming;
            this.testinputpath = canonjoin(this.inputroot, dataset);
            this.baselinepath = canonjoin(this.baselineroot, dataset);
            this.generatepath = canonjoin(this.generateroot, dataset);
        }

        String makeurl(RequestMode ext)
        {
            String u = canonjoin(FAKEURLPREFIX, canonjoin(TESTINPUTDIR, dataset)) + "." + ext.toString();
            return u;
        }

        public String toString()
        {
            return dataset;
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected MockMvc mockMvc;

    // Test cases

    protected List<TestCase> alltestcases = new ArrayList<TestCase>();

    protected List<TestCase> chosentests = new ArrayList<TestCase>();

    /* USESPRING
    @Autowired
	private WebApplicationContext wac;
    */

    //////////////////////////////////////////////////

    @Before
    public void setup()
            throws Exception
    {
        super.bindstd();
        Nc4wrapper.TRACE = false;
        //if(DEBUGDATA) DapController.DUMPDATA = true;
        /*USESPRING
          this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	else */
        {
            StandaloneMockMvcBuilder mvcbuilder =
                    MockMvcBuilders.standaloneSetup(new Dap4Controller());
            mvcbuilder.setValidator(new TestServlet.NullValidator());
            this.mockMvc = mvcbuilder.build();
        }
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
            throws Exception
    {
        super.unbindstd();
        Nc4wrapper.TRACE = false;
    }

    //////////////////////////////////////////////////
    // Define test cases

    protected void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate("test_struct_type.nc");
            prop_visual = true;
            prop_generate = false;
            prop_baseline = false;
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
    public void testServlet()
            throws Exception
    {
        Nc4Iosp.setLogLevel(5);
        try {
            DapCache.flush();
            for(TestCase testcase : chosentests) {
                doOneTest(testcase);
            }
        } finally {
            Nc4Iosp.setLogLevel(0);
        }
    }

    //////////////////////////////////////////////////
    // Primary test method

    void
    doOneTest(TestCase testcase)
            throws Exception
    {
        System.err.println("Testcase: " + testcase.testinputpath);
        System.err.println("Baseline: " + testcase.baselinepath);
        if(PARSEDEBUG) DOM4Parser.setGlobalDebugLevel(1);
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
        String little = (USEBIG ? "0" : "1");
        String nocsum = (NOCSUM ? "1" : "0");
        MvcResult result = perform(url, this.mockMvc,
                RESOURCEPATH,
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
            writefile(testcase.baselinepath + ".dmr", sdmr);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(testcase.baselinepath + ".dmr");
            System.err.println("DMR Comparison");
            Assert.assertTrue("***Fail", same(getTitle(), baselinecontent, sdmr));
        }
    }

    void
    dodata(TestCase testcase)
            throws Exception
    {
        String url = testcase.makeurl(RequestMode.DAP);
        String little = (USEBIG ? "0" : "1");
        String nocsum = (NOCSUM ? "1" : "0");
        MvcResult result = perform(url, this.mockMvc,
                RESOURCEPATH,
                DapTestCommon.ORDERTAG, little,
                DapTestCommon.NOCSUMTAG, nocsum,
                DapTestCommon.TESTTAG, "true"
        );
        // Collect the output
        MockHttpServletResponse res = result.getResponse();
        byte[] byteresult = res.getContentAsByteArray();

        if(DEBUGDATA) {
            DapDump.dumpbytestream(byteresult, ByteOrder.nativeOrder(), "TestServlet.dodata");
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
            writefile(testcase.baselinepath + ".dap", sdata);

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
                new TestCase("test_fill.nc", "dmr,dap", true,  //0
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('U', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 2);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 4);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_one_var.nc", "dmr,dap", true,  //0
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
                new TestCase("test_opaque.nc", "dmr,dap", true,  //0
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('O', 0);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_opaque_array.nc", "dmr,dap", true,  //0
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 4; i++) {
                                    printer.printvalue('O', 0, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_one_vararray.nc", "dmr,dap", true,  //1
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
                new TestCase("test_enum.nc", "dmr,dap", true,   //
                        // S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('S', 1);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_enum_2.nc", "dmr,dap", true,   //
                        // S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('S', 1);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_enum_array.nc", "dmr,dap", true, //3
                        // 5 S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 5; i++) {
                                    printer.printvalue('U', 1, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_atomic_types.nc", "dmr,dap", true, //4
                        // S1 U1 S2 U2 S4 U4 S8 U8 F4 F8 C1 T O S1 S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('S', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 2);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 2);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 4);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 4);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 8);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 8);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('F', 4);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('F', 8);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('C', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('T', 0);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('O', 0);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 1);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_atomic_array.nc", "dmr,dap", true,  //5
                        // 6 U1 4 S2 6 U4 2 F8 2 C1 4 T 2 O 5 S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 6; i++) {
                                    printer.printvalue('U', 1, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 4; i++) {
                                    printer.printvalue('S', 2, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 6; i++) {
                                    printer.printvalue('U', 4, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('F', 8, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('C', 1, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 4; i++) {
                                    printer.printvalue('T', 0, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('O', 0, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 5; i++) {
                                    printer.printvalue('S', 1, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_groups1.nc", "dmr,dap", true,   //6
                        //5 S4 3 F4 5 S4 7 F4",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 5; i++) {
                                    printer.printvalue('S', 4, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 3; i++) {
                                    printer.printvalue('F', 4, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 5; i++) {
                                    printer.printvalue('S', 4, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 7; i++) {
                                    printer.printvalue('F', 4, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_struct_type.nc", "dmr,dap", true,  //7
                        // { S4 S4 }
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
                new TestCase("test_utf8.nc", "dmr,dap", true,  //9
                        // 2 { S4 S4 }
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('T', 0, i);
                                    printer.format("%n");
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_struct_nested.hdf5", "dmr,dap", true,    // 10
                        // { { S4 S4 } { S4 S4 } }
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('S', 4);
                                printer.printvalue('S', 4);
                                printer.printvalue('S', 4);
                                printer.printvalue('S', 4);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_struct_nested3.hdf5", "dmr,dap", true,
                        // { { {S4 } } }
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
                new TestCase("test_sequence_1.syn", "dmr,dap", true,  //0
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                int count = printer.printcount();
                                for(int j = 0; j < count; j++) {
                                    printer.printvalue('S', 4);
                                    printer.printvalue('S', 2);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_sequence_2.syn", "dmr,dap", true,  //0
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    int count = printer.printcount();
                                    for(int j = 0; j < count; j++) {
                                        printer.printvalue('S', 4);
                                        printer.printvalue('S', 2);
                                    }
                                    printer.newline();
                                }
                                printer.verifychecksum();
                            }
                        }));
/*Not currently working
        this.alltestcases.add(
            new TestCase("test_vlen1.nc", "dmr,dap", true,
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        int count = printer.printcount();
                        for(int i = 0;i < count;i++) {
                            printer.printvalue('S', 4, i);
                            printer.format("\n");
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new TestCase("test_vlen2.nc", "dmr,dap", true,
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        //{1, 3, 5, 7}, {100,200}, {-1,-2},{1, 3, 5, 7}, {100,200}, {-1,-2};
                        for(int d3 = 0;d3 < 3;d3++) {
                            for(int d2 = 0;d2 < 2;d2++) {
                                int count = printer.printcount();
                                for(int i = 0;i < count;i++) {
                                    printer.printvalue('S', 4, d3, d2, i);
                                    printer.format("\n");
                                }
                            }
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new TestCase("test_vlen3.hdf5", "dmr,dap", true,
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        int count = printer.printcount();
                        for(int i = 0;i < count;i++) {
                            printer.printvalue('S', 4, i);
                            printer.format("\n");
                        }
                        printer.printchecksum();
                    }
                }));
        //*hdf5 iosp is not doing this correctly
            this.alltestcases.add(
            new TestCase("test_vlen4.hdf5", "dmr,dap", true,
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i=0;i<2;i++) {
                            int count = printer.printcount();
                            for(int j = 0;j < count;j++) {
                                printer.printvalue('S', 4, i, j);
                                printer.format("\n");
                            }
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new TestCase("test_vlen5.hdf5", "dmr,dap", true,
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 2;i++) {
                            int count = printer.printcount();
                            for(int j = 0;j < count;j++) {
                                printer.printvalue('S', 4, i, j);
                                printer.format("\n");
                            }
                        }
                        printer.printchecksum();
                    }
                }));
*/
        this.alltestcases.add(
                new TestCase("test_anon_dim.syn", "dmr,dap", true,  //0
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 6; i++) {
                                    printer.printvalue('S', 4, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_atomic_types.syn", "dmr,dap", true, //4
                        // S1 U1 S2 U2 S4 U4 S8 U8 F4 F8 C1 T O S1 S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                printer.printvalue('S', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 2);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 2);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 4);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 4);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 8);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('U', 8);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('F', 4);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('F', 8);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('C', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('T', 0);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('O', 0);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 1);
                                printer.verifychecksum();
                                printer.startchecksum();
                                printer.printvalue('S', 1);
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_atomic_array.syn", "dmr,dap", true,  //5
                        // 6 U1 4 S2 6 U4 2 F8 2 C1 4 T 2 O 5 S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 6; i++) {
                                    printer.printvalue('U', 1, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 4; i++) {
                                    printer.printvalue('S', 2, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 6; i++) {
                                    printer.printvalue('U', 4, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('F', 8, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('C', 1, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 4; i++) {
                                    printer.printvalue('T', 0, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('O', 0, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 5; i++) {
                                    printer.printvalue('S', 1, i);
                                }
                                printer.verifychecksum();
                            }
                        }));
        this.alltestcases.add(
                new TestCase("test_struct_array.syn", "dmr,dap", true,  //8
                        // 12 { S4 S4 }
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.startchecksum();
                                for(int i = 0; i < 4; i++) {
                                    printer.printvalue('F', 4, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 3; i++) {
                                    printer.printvalue('F', 4, i);
                                }
                                printer.verifychecksum();
                                printer.startchecksum();
                                for(int i = 0; i < 4; i++) {
                                    for(int j = 0; j < 3; j++) {
                                        printer.printvalue('S', 4, i);
                                        printer.format(" ");
                                        printer.printvalue('S', 4);
                                        printer.format("%n");
                                    }
                                }
                                printer.verifychecksum();
                            }
                        }));
        // XFAIL tests
        this.alltestcases.add(
                new TestCase("test_struct_array.nc", "dmr", true)
        );
    }

    //////////////////////////////////////////////////
    // Utility methods


    // Locate the test cases with given prefix
    List<TestCase>
    locate(String prefix)
    {
        List<TestCase> results = new ArrayList<TestCase>();
        for(TestCase ct : this.alltestcases) {
            if(ct.dataset.startsWith(prefix))
                results.add(ct);
        }
        return results;
    }


    //////////////////////////////////////////////////
    // Support classes

    static /*package*/ class NullValidator implements Validator
    {
        public boolean supports(Class<?> clazz)
        {
            return true;
        }

        public void validate(Object target, Errors errors)
        {
            return;
        }
    }

}

