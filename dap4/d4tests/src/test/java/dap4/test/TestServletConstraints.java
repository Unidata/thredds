package dap4.test;

import dap4.core.util.DapDump;
import dap4.core.util.Escape;
import dap4.dap4shared.ChunkInputStream;
import dap4.dap4shared.RequestMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TestServlet test server side
 * constraint processing.
 */


public class TestServletConstraints extends DapTestCommon
{
    static final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String TESTINPUTDIR = "/testfiles";
    static protected String BASELINEDIR = "/TestServletConstraints/baseline";
    static protected String GENERATEDIR = "/TestCDMClient/testinput";


    // constants for Fake Request
    static String FAKEURLPREFIX = "http://localhost:8080/dap4";

    static final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    //////////////////////////////////////////////////
    // Type Declarations

    static class ConstraintTest
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

        static ConstraintTest[] alltests;

        static {
            alltests = new ConstraintTest[2048];
            reset();
        }

        static public void reset()
        {
            Arrays.fill(alltests, null);
        }

        String title;
        String dataset;
        String constraint;
        boolean xfail;
        String[] extensions;
        Dump.Commands template;
        String testinputpath;
        String baselinepath;
        String generatepath;
        int id;

        ConstraintTest(int id, String dataset, String extensions, String ce)
        {
            this(id, dataset, extensions, ce, null, true);
        }

        ConstraintTest(int id, String dataset, String extensions, String ce,
                       Dump.Commands template)
        {
            this(id, dataset, extensions, ce, template, false);
        }

        ConstraintTest(int id, String dataset, String extensions, String ce,
                       Dump.Commands template, boolean xfail)
        {
            if(alltests[id] != null)
                throw new IllegalStateException("two tests with same id");
            this.id = id;
            this.title = dataset + (ce == null ? "" : ("?" + ce));
            this.dataset = dataset;
            this.constraint = ce;
            this.xfail = xfail;
            this.extensions = extensions.split(",");
            this.template = template;
            this.testinputpath = canonjoin(this.inputroot, dataset) + "." + id;
            this.baselinepath = canonjoin(this.baselineroot, dataset) + "." + id;
            this.generatepath = canonjoin(this.generateroot, dataset) + "." + id;
            alltests[id] = this;
        }

        String makeurl(RequestMode ext)
        {
            String url = canonjoin(FAKEURLPREFIX, canonjoin(TESTINPUTDIR, dataset));
            if(ext != null) url += "." + ext.toString();
            if(constraint != null) {
                url += "?" + CONSTRAINTTAG + "=";
                String ce = constraint;
                // Escape it
                ce = Escape.urlEncodeQuery(ce);
                url += ce;
            }
            return url;
        }

        public String toString()
        {
            return makeurl(null);
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Test cases

    List<ConstraintTest> alltestcases = new ArrayList<ConstraintTest>();

    List<ConstraintTest> chosentests = new ArrayList<ConstraintTest>();

    String root = null;
    String wardir = null;

    //////////////////////////////////////////////////

    @Before
    public void setup() {
        ConstraintTest.setRoots(canonjoin(getResourceDir(), TESTINPUTDIR),
                canonjoin(getResourceDir(), BASELINEDIR),
                canonjoin(getResourceDir(), GENERATEDIR));
        defineAllTestcases();
        chooseTestcases();
    }

    //////////////////////////////////////////////////
    // Define test cases

    void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate(5);
        } else {
            for(ConstraintTest tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    void defineAllTestcases()
    {
        ConstraintTest.reset();
        this.alltestcases.add(
                new ConstraintTest(1, "test_one_vararray.nc", "dmr,dap", "/t[1]",
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
                new ConstraintTest(2, "test_anon_dim.syn", "dmr,dap", "/vu32[0:3]",  // test for dimension inclusion
                        // S4
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                printer.printvalue('U', 4);
                                printer.printvalue('U', 4);
                                printer.printvalue('U', 4);
                                printer.printvalue('U', 4);
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
                new ConstraintTest(3, "test_one_vararray.nc", "dmr,dap", "/t",  // test for dimension inclusion
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
                new ConstraintTest(4, "test_enum_array.nc", "dmr,dap", "/primary_cloud[1:2:4]",
                        // 2 S1
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('U', 1, i);
                                }
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
                new ConstraintTest(5, "test_atomic_array.nc", "dmr,dap", "/vu8[1][0:2:2];/vd[1];/vs[1][0];/vo[0][1]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('U', 1, i);
                                }
                                printer.printchecksum();
                                for(int i = 0; i < 1; i++) {
                                    printer.printvalue('F', 8, i);
                                }
                                printer.printchecksum();
                                for(int i = 0; i < 1; i++) {
                                    printer.printvalue('T', 0, i);
                                }
                                printer.printchecksum();
                                for(int i = 0; i < 1; i++) {
                                    printer.printvalue('O', 0, i);
                                }
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
                new ConstraintTest(6, "test_struct_array.nc", "dmr,dap", "/s[0:2:3][0:1]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                for(int i = 0; i < 4; i++) {
                                    for(int j = 0; j < 2; j++) {
                                        printer.printvalue('S', 4);
                                    }
                                }
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
                new ConstraintTest(7, "test_opaque_array.nc", "dmr,dap", "/vo2[1][0:1]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                for(int i = 0; i < 2; i++) {
                                    printer.printvalue('O', 0, i);
                                }
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
                new ConstraintTest(8, "test_atomic_array.nc", "dmr,dap", "/v16[0:1,3]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                for(int i = 0; i < 3; i++) {
                                    printer.printvalue('S', 2, i);
                                }
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
                new ConstraintTest(9, "test_atomic_array.nc", "dmr,dap", "/v16[3,0:1]",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                for(int i = 0; i < 3; i++) {
                                    printer.printvalue('S', 2, i);
                                }
                                printer.printchecksum();
                            }
                        }));
    }

    //////////////////////////////////////////////////
    // Junit test methods
    @Test
    public void testServletConstraints()
            throws Exception
    {
        boolean pass = true;
        for(ConstraintTest testcase : chosentests) {
            if(!doOneTest(testcase))
                pass = false;
        }
        Assert.assertTrue("***Fail: TestServletConstraints", pass);
    }

    //////////////////////////////////////////////////
    // Primary test method
    boolean
    doOneTest(ConstraintTest testcase)
            throws Exception
    {
        boolean pass = true;
        System.out.println("Testcase: " + testcase.toString());

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
            if(!pass) {
                System.err.printf("TestServletConstraint: fail: %s ext=%s\n", testcase, extension);
                System.err.flush();
                break;
            }
        }
        return pass;
    }

    boolean
    dodmr(ConstraintTest testcase)
            throws Exception
    {
        boolean pass = true;
        String url = testcase.makeurl(RequestMode.DMR);
        // Create request and response objects
        Mocker mocker = new Mocker("dap4",url,this);
        byte[] byteresult = null;

        // See if the servlet can process this
        try {
            byteresult = mocker.execute();
        } catch (Throwable t) {
            System.out.println(testcase.xfail ? "XFail" : "Fail");
            t.printStackTrace();
            return testcase.xfail;
        }

        // Test by converting the raw output to a string
        String sdmr = new String(byteresult, UTF8);
        if(prop_visual)
            visual(url, sdmr);
        if(prop_baseline) {
            writefile(testcase.baselinepath + ".dmr", sdmr);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(testcase.baselinepath + ".dmr");
            System.out.println("DMR Comparison:");
            pass = compare(baselinecontent, sdmr);
            System.out.println(pass ? "Pass" : "Fail");
        }
        return pass;
    }

    boolean
    dodata(ConstraintTest testcase)
            throws Exception
    {
        boolean pass = true;
        String baseline;
        RequestMode mode = RequestMode.DAP;
        String methodurl = testcase.makeurl(mode);
        // Create request and response objects
        Mocker mocker = new Mocker("dap4",methodurl,this);
        byte[] byteresult = null;

        try {
            byteresult = mocker.execute();
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
        if(DEBUG) {
            DapDump.dumpbytes(ByteBuffer.wrap(byteresult).order(ByteOrder.nativeOrder()), true);
        }

        // Setup a ChunkInputStream
        ByteArrayInputStream bytestream = new ByteArrayInputStream(byteresult);

        ChunkInputStream reader = new ChunkInputStream(bytestream, RequestMode.DAP, ByteOrder.nativeOrder());

        String sdmr = reader.readDMR(); // Read the DMR
        if(prop_visual)
            visual(methodurl, sdmr);

        Dump printer = new Dump();
        String sdata = printer.dumpdata(reader, true, reader.getByteOrder(), testcase.template);

        if(prop_visual)
            visual(testcase.title + ".dap", sdata);

        if(prop_baseline)
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


    // Locate the test cases with given prefix
    List<ConstraintTest>
    locate(Object tag)
    {
        List<ConstraintTest> results = new ArrayList<ConstraintTest>();
        for(ConstraintTest ct : this.alltestcases) {
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
