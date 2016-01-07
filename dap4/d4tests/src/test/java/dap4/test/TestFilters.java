package dap4.test;

import dap4.dap4shared.ChunkInputStream;
import dap4.core.util.*;
import dap4.dap4shared.RequestMode;
import dap4.servlet.Generator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    //////////////////////////////////////////////////
    // Constants

    static String DATADIR = "d4tests/src/test/data"; // relative to dap4 root
    static String TESTDATADIR = DATADIR + "/resources/";
    static String BASELINEDIR = DATADIR + "/resources/TestFilters/baseline";
    static String TESTINPUTDIR = DATADIR + "/resources/testfiles";

    // constants for Fake Request
    static String FAKEURLPREFIX = "http://localhost:8080/d4ts";

    static final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    static protected final int DEFAULTROWCOUNT = 5;

    //////////////////////////////////////////////////
    // Type Declarations

    static class ConstraintTest
    {
        static String root = null;

        static ConstraintTest[] alltests;

        static {
            alltests = new ConstraintTest[2048];
            Arrays.fill(alltests, null);
        }

        String title;
        String dataset;
        String constraint;
        boolean xfail;
        Dump.Commands template;
        String testinputpath;
        String baselinepath;
        int id;
        int rowcount = DEFAULTROWCOUNT;

        ConstraintTest(int id, String dataset, String ce)
        {
            this(id, dataset, 0, ce, null, true);
        }

        ConstraintTest(int id, String dataset, String ce,
                       Dump.Commands template)
        {
            this(id, dataset, 0, ce, template, false);
        }

        ConstraintTest(int id, String dataset, int rows, String ce,
                       Dump.Commands template)
        {
            this(id, dataset, rows, ce, template, false);
        }

        ConstraintTest(int id, String dataset, int rows, String ce,
                       Dump.Commands template, boolean xfail)
        {
            if(alltests[id] != null)
                throw new IllegalStateException("two tests with same id");
            this.id = id;
            this.title = dataset + (ce == null ? "" : ("?" + ce));
            this.dataset = dataset;
            this.constraint = ce;
            this.xfail = xfail;
            this.template = template;
            this.testinputpath
                = root + "/" + TESTINPUTDIR + "/" + dataset;
            this.baselinepath
                = root + "/" + BASELINEDIR + "/" + dataset + "." + String.valueOf(this.id);
            this.rowcount = rows == 0 ? DEFAULTROWCOUNT : rows;
            alltests[id] = this;
        }

        String makeurl(RequestMode ext)
        {
            String url = FAKEURLPREFIX + "/" + dataset;
            if(ext != null) url += "." + ext.toString();
            if(constraint != null) {
                url += "?" + CONSTRAINTTAG + "=";
                String ce = constraint;
                // Escape it
                //ce = Escape.urlEncodeQuery(ce);
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

    // Misc variables
    boolean isbigendian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    // Test cases

    List<ConstraintTest> alltestcases = new ArrayList<ConstraintTest>();

    List<ConstraintTest> chosentests = new ArrayList<ConstraintTest>();

    String datasetpath = null;

    String root = null;

    //////////////////////////////////////////////////

    @Before
    public void setup() throws Exception {
        this.root = getDAP4Root();
        if(this.root == null)
            throw new Exception("dap4 root not found");
        this.datasetpath = this.root + "/" + DATADIR;
        defineAllTestcases(this.root);
        chooseTestcases();
    }

    //////////////////////////////////////////////////
    // Define test cases

    protected void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate(1);
        } else {
            for(ConstraintTest tc : alltestcases)
                chosentests.add(tc);
        }
    }

    protected void
    defineAllTestcases(String root)
    {
        ConstraintTest.root = root;
        this.alltestcases.add(
                    new ConstraintTest(1, "test_sequence_1.syn", "/s",
                        new Dump.Commands()
                        {
                            public void run(Dump printer) throws IOException
                            {
                                int count = printer.printcount();
                                for(int j = 0;j < count;j++) {
                                    printer.printvalue('S', 4);
                                    printer.printvalue('S', 2);
                                }
                                printer.newline();
                                printer.printchecksum();
                            }
                        }));
        this.alltestcases.add(
            new ConstraintTest(2, "test_sequence_1.syn", "/s|i1<0",
                new Dump.Commands()
                {
                    public void run(Dump printer) throws IOException
                    {
                        int count = printer.printcount();
                        for(int j = 0;j < count;j++) {
                            printer.printvalue('S', 4);
                            printer.printvalue('S', 2);
                        }
                        printer.newline();
                        printer.printchecksum();
                    }
                }));

    }

    //////////////////////////////////////////////////
    // Junit test methods
    @Test
    public void testFilters()
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
        Generator.setRowCount(testcase.rowcount);
        pass = dodata(testcase);
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

	Mocker mocker = new Mocker("dap4",methodurl,this);
	byte[] byteresult = null;

        try {
	    byteresult = mocker.execute();
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }

        if(prop_debug) {
            ByteOrder order = (isbigendian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            DapDump.dumpbytes(ByteBuffer.wrap(byteresult).order(order), true);
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
    locate(Object pattern)
    {
        List<ConstraintTest> results = new ArrayList<ConstraintTest>();
        for(ConstraintTest ct : this.alltestcases) {
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

}

