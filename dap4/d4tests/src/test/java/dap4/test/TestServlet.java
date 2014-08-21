package dap4.test;

import dap4.dap4shared.ChunkInputStream;
import dap4.core.util.DapDump;
import dap4.dap4shared.RequestMode;
import dap4.servlet.DapCache;
import dap4.servlet.Generator;
import dap4.test.servlet.*;
import dap4.test.util.*;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * TestServlet has multiple purposes.
 * 1. It test the d4tsservlet.
 * 2. It generates into files, the serialized databuffer
 * for datasets. These files are then used to
 * test client side deserialization.
 */

public class TestServlet extends DapTestCommon
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected String DATADIR = "d4tests/src/test/data"; // relative to dap4 root
    static protected String TESTDATADIR = DATADIR + "/resources";
    static protected String BASELINEDIR = DATADIR + "/resources/TestServlet/baseline";
    static protected String TESTINPUTDIR = DATADIR + "/resources/testfiles";

    static protected String GENERATEDIR = TESTDATADIR + "/TestCDMClient/testinput";

    // constants for Fake Request
    static protected String FAKEURLPREFIX = "http://localhost:8080/d4ts";

    static protected final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    // Define the file extensions of interest for generation
    static protected final String[] GENEXTENSIONS = new String[]{".raw.dap", ".raw.dmr"};

    //////////////////////////////////////////////////
    // Type Declarations

    static protected class ServletTest
    {
        static String root = null;

        String title;
        String dataset;
        String[] extensions;
        boolean checksumming;
        boolean xfail; // => template should be null
        Dump.Commands template;
        String testinputpath;
        String baselinepath;
        String generatepath;

        ServletTest(String dataset, String extensions, boolean checksumming,
                    Dump.Commands template)
        {
            this(dataset, extensions, checksumming, false, template);
        }

        ServletTest(String dataset, String extensions, boolean checksumming)
        {
            this(dataset, extensions, checksumming, true, null);
        }

        protected ServletTest(String dataset, String extensions,
                              boolean checksumming, boolean xfail,
                              Dump.Commands template)
        {
            this.title = dataset;
            this.dataset = dataset;
            this.extensions = extensions.split(",");
            this.template = template;
            this.xfail = xfail;
            this.checksumming = checksumming;
            this.testinputpath
                = root + "/" + TESTINPUTDIR + "/" + dataset;
            this.baselinepath
                = root + "/" + BASELINEDIR + "/" + dataset;
            this.generatepath
                = root + "/" + GENERATEDIR + "/" + dataset;
        }

        String makeurl(RequestMode ext)
        {
            return FAKEURLPREFIX + "/" + dataset + "." + ext.toString();
        }

        public String toString()
        {
            return dataset;
        }
    }

    static protected class GenerateFilter implements FileFilter
    {
        boolean debug;

        public GenerateFilter(boolean debug)
        {
            this.debug = debug;
        }

        public boolean accept(File file)
        {
            boolean ok = false;
            if(file.isFile() && file.canRead() && file.canWrite()) {
                // Check for proper extension
                String name = file.getName();
                for(String ext : GENEXTENSIONS) {
                    if(name != null && name.endsWith(ext))
                        ok = true;
                }
            }
            if(!ok && debug) {
                System.err.println("Ignoring: " + file.toString());
            }
            return ok;
        }

    }

    //////////////////////////////////////////////////
    // Instance variables

    // Test cases

    protected List<ServletTest> alltestcases = new ArrayList<ServletTest>();

    protected List<ServletTest> chosentests = new ArrayList<ServletTest>();

    protected String datasetpath = null;

    protected String root = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestServlet()
        throws Exception
    {
        this("TestServlet");
    }

    public TestServlet(String name)
        throws Exception
    {
        this(name, null);
    }

    public TestServlet(String name, String[] argv)
        throws Exception
    {
        super(name);
        setSystemProperties();
        if(prop_ascii)
            Generator.setASCII(true);

        this.root = getDAP4Root();
        if(this.root == null)
            throw new Exception("dap4 root not found");

        this.datasetpath = this.root + "/" + DATADIR;
        defineAllTestcases(this.root);
        chooseTestcases();
        generatesetup();
    }

    //////////////////////////////////////////////////
    // Define test cases

    protected void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate("test_struct_type.nc");
        } else {
            for(ServletTest tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    protected void
    defineAllTestcases(String root)
    {
        ServletTest.root = root;
        this.alltestcases.add(
            new ServletTest("test_sequence_1.syn", "dmr,dap", true,  //0
                // S4
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        int count = printer.printcount();
                        for(int j = 0;j < count;j++) {
                            printer.printvalue('S', 4);
                            printer.printvalue('S', 2);
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_sequence_2.syn", "dmr,dap", true,  //0
                // S4
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 2;i++) {
                            int count = printer.printcount();
                            for(int j = 0;j < count;j++) {
                                printer.printvalue('S', 4);
                                printer.printvalue('S', 2);
                            }
                            printer.newline();
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_one_var.nc", "dmr,dap", true,  //0
                // S4
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('S', 4);
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_opaque.nc", "dmr,dap", true,  //0
                // S4
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('O', 0);
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
                    new ServletTest("test_opaque_array.nc", "dmr,dap", true,  //0
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                for(int i = 0;i < 4;i++) {
                                    printer.printvalue('O', 0, i);
                                }
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
            new ServletTest("test_one_vararray.nc", "dmr,dap", true,  //1
                // S4
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('S', 4);
                        printer.printvalue('S', 4);
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_enum.nc", "dmr,dap", true,   //
                // S1
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('S', 1);
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
                    new ServletTest("test_enum_2.nc", "dmr,dap", true,   //
                        // S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.printvalue('S', 1);
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
            new ServletTest("test_enum_array.nc", "dmr,dap", true, //3
                // 5 S1
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 5;i++) {
                            printer.printvalue('U', 1, i);
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_atomic_types.nc", "dmr,dap", true, //4
                // S1 U1 S2 U2 S4 U4 S8 U8 F4 F8 C1 T O S1 S1
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('S', 1);
                        printer.printchecksum();
                        printer.printvalue('U', 1);
                        printer.printchecksum();
                        printer.printvalue('S', 2);
                        printer.printchecksum();
                        printer.printvalue('U', 2);
                        printer.printchecksum();
                        printer.printvalue('S', 4);
                        printer.printchecksum();
                        printer.printvalue('U', 4);
                        printer.printchecksum();
                        printer.printvalue('S', 8);
                        printer.printchecksum();
                        printer.printvalue('U', 8);
                        printer.printchecksum();
                        printer.printvalue('F', 4);
                        printer.printchecksum();
                        printer.printvalue('F', 8);
                        printer.printchecksum();
                        printer.printvalue('C', 1);
                        printer.printchecksum();
                        printer.printvalue('T', 0);
                        printer.printchecksum();
                        printer.printvalue('O', 0);
                        printer.printchecksum();
                        printer.printvalue('S', 1);
                        printer.printchecksum();
                        printer.printvalue('S', 1);
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_atomic_array.nc", "dmr,dap", true,  //5
                // 6 U1 4 S2 6 U4 2 F8 2 C1 4 T 2 O 5 S1
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 6;i++) {
                            printer.printvalue('U', 1, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 4;i++) {
                            printer.printvalue('S', 2, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 6;i++) {
                            printer.printvalue('U', 4, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 2;i++) {
                            printer.printvalue('F', 8, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 2;i++) {
                            printer.printvalue('C', 1, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 4;i++) {
                            printer.printvalue('T', 0, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 2;i++) {
                            printer.printvalue('O', 0, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 5;i++) {
                            printer.printvalue('S', 1, i);
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_groups1.nc", "dmr,dap", true,   //6
                //5 S4 3 F4 5 S4 7 F4",
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 5;i++) {
                            printer.printvalue('S', 4, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 3;i++) {
                            printer.printvalue('F', 4, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 5;i++) {
                            printer.printvalue('S', 4, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 7;i++) {
                            printer.printvalue('F', 4, i);
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_struct_type.nc", "dmr,dap", true,  //7
                // { S4 S4 }
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('S', 4);
                        printer.printvalue('S', 4);
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_utf8.nc", "dmr,dap", true,  //9
                // 2 { S4 S4 }
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 2;i++) {
                            printer.printvalue('T', 0, i);
                            printer.format("%n");
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_struct_nested.hdf5", "dmr,dap", true,    // 10
                // { { S4 S4 } { S4 S4 } }
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('S', 4);
                        printer.printvalue('S', 4);
                        printer.printvalue('S', 4);
                        printer.printvalue('S', 4);
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_struct_nested3.hdf5", "dmr,dap", true,
                // { { {S4 } } }
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('S', 4);
                        printer.printchecksum();
                    }
                }));
/*Not currently working
        this.alltestcases.add(
            new ServletTest("test_vlen1.nc", "dmr,dap", true,
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
            new ServletTest("test_vlen2.nc", "dmr,dap", true,
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
            new ServletTest("test_vlen3.hdf5", "dmr,dap", true,
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
            new ServletTest("test_vlen4.hdf5", "dmr,dap", true,
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
            new ServletTest("test_vlen5.hdf5", "dmr,dap", true,
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
            new ServletTest("test_anon_dim.syn", "dmr,dap", true,  //0
                // S4
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 6;i++) {
                            printer.printvalue('S', 4, i);
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_atomic_types.syn", "dmr,dap", true, //4
                // S1 U1 S2 U2 S4 U4 S8 U8 F4 F8 C1 T O S1 S1
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        printer.printvalue('S', 1);
                        printer.printchecksum();
                        printer.printvalue('U', 1);
                        printer.printchecksum();
                        printer.printvalue('S', 2);
                        printer.printchecksum();
                        printer.printvalue('U', 2);
                        printer.printchecksum();
                        printer.printvalue('S', 4);
                        printer.printchecksum();
                        printer.printvalue('U', 4);
                        printer.printchecksum();
                        printer.printvalue('S', 8);
                        printer.printchecksum();
                        printer.printvalue('U', 8);
                        printer.printchecksum();
                        printer.printvalue('F', 4);
                        printer.printchecksum();
                        printer.printvalue('F', 8);
                        printer.printchecksum();
                        printer.printvalue('C', 1);
                        printer.printchecksum();
                        printer.printvalue('T', 0);
                        printer.printchecksum();
                        printer.printvalue('O', 0);
                        printer.printchecksum();
                        printer.printvalue('S', 1);
                        printer.printchecksum();
                        printer.printvalue('S', 1);
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_atomic_array.syn", "dmr,dap", true,  //5
                // 6 U1 4 S2 6 U4 2 F8 2 C1 4 T 2 O 5 S1
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 6;i++) {
                            printer.printvalue('U', 1, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 4;i++) {
                            printer.printvalue('S', 2, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 6;i++) {
                            printer.printvalue('U', 4, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 2;i++) {
                            printer.printvalue('F', 8, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 2;i++) {
                            printer.printvalue('C', 1, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 4;i++) {
                            printer.printvalue('T', 0, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 2;i++) {
                            printer.printvalue('O', 0, i);
                        }
                        printer.printchecksum();
                        for(int i = 0;i < 5;i++) {
                            printer.printvalue('S', 1, i);
                        }
                        printer.printchecksum();
                    }
                }));
        this.alltestcases.add(
            new ServletTest("test_struct_array.syn", "dmr,dap", true,  //8
                // 12 { S4 S4 }
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        for(int i = 0;i < 4;i++) {
                            for(int j = 0;j < 3;j++) {
                                printer.printvalue('S', 4, i);
                                printer.format(" ");
                                printer.printvalue('S', 4);
                                printer.format("%n");
                            }
                        }
                        printer.printchecksum();
                    }
                }));
        // XFAIL tests
        this.alltestcases.add(
            new ServletTest("test_struct_array.nc", "dmr", true)
        );
    }


    //////////////////////////////////////////////////
    // Junit test methods

    public void testServlet()
        throws Exception
    {
        DapCache.flush();
        for(ServletTest testcase : chosentests) {
            assertTrue(doOneTest(testcase));
        }
    }

    //////////////////////////////////////////////////
    // Primary test method
    boolean
    doOneTest(ServletTest testcase)
        throws Exception
    {
        boolean pass = true;

        System.out.println("Testcase: " + testcase.testinputpath);

        for(String extension : testcase.extensions) {
            RequestMode ext = RequestMode.modeFor(extension);
            switch (ext) {
            case DMR:
                pass = dodmr(testcase);
                break;
            case DAP:
                pass = dodata(testcase);
                break;
            default:
                assert (false);
                if(!pass) break;
            }
            if(!pass) break;
        }
        return pass;
    }

    boolean
    dodmr(ServletTest testcase)
        throws Exception
    {
        boolean pass = true;

        // Create request and response objects
        FakeServlet servlet = new FakeServlet(this.datasetpath);
        FakeServletRequest req = new FakeServletRequest(testcase.makeurl(RequestMode.DMR), servlet);
        FakeServletResponse resp = new FakeServletResponse();
        servlet.init();

        // See if the servlet can process this
        try {
            servlet.doGet(req, resp);
        } catch (Throwable t) {
            System.out.println(testcase.xfail ? "XFail" : "Fail");
            t.printStackTrace();
            return testcase.xfail;
        }

        // Collect the output
        FakeServletOutputStream fakestream = (FakeServletOutputStream) resp.getOutputStream();
        byte[] byteresult = fakestream.toArray();

        // Test by converting the raw output to a string

        String sdmr = new String(byteresult, UTF8);
        if(prop_visual)
            visual(testcase.title + ".dmr", sdmr);
        if(!testcase.xfail && prop_baseline) {
            writefile(testcase.baselinepath + ".dmr", sdmr);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(testcase.baselinepath + ".dmr");
            System.out.println("DMR Comparison: vs " + testcase.baselinepath + ".dmr");
            pass = compare(baselinecontent, sdmr);
            System.out.println(pass ? "Pass" : "Fail");
        }
        return pass;
    }

    boolean
    dodata(ServletTest testcase)
        throws Exception
    {
        boolean pass = true;
        String baseline;
        RequestMode ext = RequestMode.DAP;

        // Create request and response objects
        FakeServlet servlet = new FakeServlet(this.datasetpath);
        String methodurl = testcase.makeurl(RequestMode.DAP);
        FakeServletRequest req = new FakeServletRequest(methodurl, servlet);
        FakeServletResponse resp = new FakeServletResponse();

        servlet.init();

        // See if the servlet can process this
        try {
            servlet.doGet(req, resp);
        } catch (Throwable t) {
            System.out.println(testcase.xfail ? "XFail" : "Fail");
            t.printStackTrace();
            return testcase.xfail;
        }

        // Collect the output
        FakeServletOutputStream fakestream
            = (FakeServletOutputStream) resp.getOutputStream();
        if(prop_debug || DEBUG) {
            DapDump.dumpbytestream(fakestream.byteStream(),ByteOrder.nativeOrder(),"TestServlet.dodata");
        }
        byte[] byteresult = fakestream.toArray();

        if(!testcase.xfail && prop_generate) {
            // Dump the serialization into a file; this also includes the dmr
            String target = testcase.generatepath + ".raw";
            writefile(target, byteresult);
        }

        if(DEBUG) {
            System.out.println("///////////////////");
            ByteBuffer datab = ByteBuffer.wrap(byteresult).order(ByteOrder.nativeOrder());
            DapDump.dumpbytes(datab,true);
            System.out.println("///////////////////");
            System.out.flush();
        }

        // Setup a ChunkInputStream
        ByteArrayInputStream bytestream = new ByteArrayInputStream(byteresult);

        ChunkInputStream reader = new ChunkInputStream(bytestream, RequestMode.DAP, ByteOrder.nativeOrder());

        String sdmr = reader.readDMR(); // Read the DMR
        if(prop_visual)
            visual(testcase.title + ".dmr.dap", sdmr);

        Dump printer = new Dump();
        String sdata = printer.dumpdata(reader, testcase.checksumming, reader.getByteOrder(), testcase.template);

        if(prop_visual)
            visual(testcase.title + ".dap", sdata);

        if(!testcase.xfail && prop_baseline)
            writefile(testcase.baselinepath + ".dap", sdata);

        if(prop_diff) {
            //compare with baseline
            // Read the baseline file
            System.out.println("Note Comparison:");
            String baselinecontent = readfile(testcase.baselinepath + ".dap");
            pass = compare(baselinecontent, sdata);
            System.out.println(pass ? "Pass" : "Fail");
        }

        return pass;
    }

    //////////////////////////////////////////////////
    // Utility methods

    boolean
    generatesetup()
    {
        if(!prop_generate)
            return false;
        File genpath = new File(root + "/" + GENERATEDIR);
        if(!genpath.exists()) {// create generate dir if it does not exist
            if(!genpath.mkdirs())
                return report("Cannot create: " + genpath.toString());
        } else if(!genpath.isDirectory())
            return report("Not a directory: " + genpath.toString());
        else if(!genpath.canWrite())
            return report("Directory not writeable: " + genpath.toString());
        // Clear the generate directory, but of files only
        clearDir(genpath, false);
        return true;
    }

    boolean
    report(String msg)
    {
        System.err.println(msg);
        prop_generate = false;
        return false;
    }


    // Locate the test cases with given prefix
    List<ServletTest>
    locate(String prefix)
    {
        List<ServletTest> results = new ArrayList<ServletTest>();
        for(ServletTest ct : this.alltestcases) {
            if(ct.dataset.startsWith(prefix))
                results.add(ct);
        }
        return results;
    }
    //////////////////////////////////////////////////
    // Stand alone

    static public void
    main(String[] argv)
    {
        try {
            new TestServlet().testServlet();
        } catch (Exception e) {
            System.err.println("*** FAIL");
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("*** PASS");
        System.exit(0);
    }// main

} // class TestServlet
