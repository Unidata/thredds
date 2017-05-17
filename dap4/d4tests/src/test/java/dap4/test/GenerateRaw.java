package dap4.test;

import dap4.core.ce.parser.CEParserImpl;
import dap4.core.util.DapDump;
import dap4.core.util.Escape;
import dap4.dap4lib.ChunkInputStream;
import dap4.dap4lib.DSPPrinter;
import dap4.dap4lib.FileDSP;
import dap4.dap4lib.RequestMode;
import dap4.servlet.DapController;
import dap4.servlet.Generator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import thredds.server.dap4.Dap4Controller;
import ucar.unidata.util.test.category.NotJenkins;
import ucar.unidata.util.test.category.NotTravis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;


/**
 * This program is actually a feeder to provide
 * testdata for use by the netcdf-c library.
 * It operates by capturing the on-wire data
 * that would be produced by a request to thredds
 * to fufill a request for a dataset. This is
 * referred to as <i>raw</i> data.
 * <p>
 * In operation, and for each test dataset,
 * a .dmr file is placed in
 * .../dap4/d4tests/src/test/data/resources/dmrtestfiles
 * and a .dap file is placed in
 * .../dap4/d4tests/src/test/data/resources/daptestfiles
 * The <i>.dap</i> file is the complete captured
 * on-the-wire data. The other, with the extension <i>.dmr</i>,
 * is just the dmr for that dataset.
 * <p>
 * This program is setup as a Junit Test.
 * It is a modified version of TestServletConstraints.
 * As a rule, this program should only be invoked when new
 * test cases are available (see GenerateRaw#defineAlltestcases).
 * No harm will occur if it is accidentally run.
 * <p>
 * On the netcdf-c side, there is a program -- dap4_test/maketests.sh --
 * that extracts files from the {dmr,dap}testfiles directories and from the
 * testfiles directory (for .cdl files). It stores the dmr files in dmrtestfiles,
 * the dap files in daptestfiles, and the cdl (if available) in
 * cdltestfiles.
 */

@Category({NotJenkins.class, NotTravis.class}) // must call explicitly in intellij
public class GenerateRaw extends DapTestCommon
{
    static public boolean GENERATE = false;
    static public boolean DEBUG = false;
    static public boolean DEBUGDATA = false;
    static public boolean PARSEDEBUG = false;
    static public boolean CEPARSEDEBUG = false;
    static public boolean SHOWTESTCASES = false;

    static public boolean USEBIG = false;
    static public boolean NOCSUM = false;

    static public boolean USED4TS = false;

    static public boolean USEDAPDMR = true;

    //////////////////////////////////////////////////
    // Constants

    //    static protected final String RESOURCEPATH = "/src/test/data/resources"; // wrt getTestInputFilesDir
    static protected final String RAWDIR = "/rawtestfiles";
    static protected final String DAPDIR = "/daptestfiles";
    static protected final String DMRDIR = "/dmrtestfiles";

    // constants for Fake Request
    static protected final String FAKEURLPREFIX = (USED4TS ? "/d4ts" : "/dap4");

    static protected final String RESOURCEPATH = "/src/test/data/resources"; // wrt getTestInputFilesDir

    //////////////////////////////////////////////////
    // Type Declarations

    static protected class TestCase
    {
        static String resourceroot = null;

        static public void
        setRoots(String resource)
        {
            resourceroot = canonicalpath(resource);
        }

        static public String resourcepath()
        {
            return resourceroot;
        }

        public String dataset;
        public String prefix;
        public String suffix; // extensions
        public String ce;
        public boolean bigendian;
        public boolean nochecksum;
        public int id;

        public TestCase(String dataset)
        {
            this(0, dataset, null);
        }

        public TestCase(int id, String dataset, String ce)
        {
            dataset = canonicalpath(dataset);
            // The prefix is everything before the final file name
            int index = dataset.lastIndexOf("/");
            if(index < 0) {
                prefix = null;
            } else {
                prefix = canonicalpath(dataset.substring(0, index));
                dataset = dataset.substring(index + 1);
            }
            // pull off the extensions
            index = dataset.indexOf(".");
            if(index < 0) {
                suffix = "";
            } else {
                suffix = dataset.substring(index);
                dataset = dataset.substring(0, index);
            }
            this.dataset = dataset;
            this.ce = (ce == null ? null : Escape.urlEncodeQuery(ce));
            this.id = id;
            this.bigendian = false;
            this.nochecksum = false;
        }

        public TestCase setBigEndian(boolean tf)
        {
            this.bigendian = tf;
            return this;
        }

        public TestCase setNoChecksum(boolean tf)
        {
            this.nochecksum = tf;
            return this;
        }

        public String inputpath()
        {
            return canonjoin(resourceroot, this.prefix);
        }

        public String generatepath(String dir)
        {
            String s = canonjoin(this.resourcepath(), dir, this.dataset);
            if(this.id > 0)
                s = s + "." + String.format("%d", this.id);
            return s + this.suffix;
        }

        public String makeurl()
        {
            String url = canonjoin(
                    FAKEURLPREFIX,
                    this.prefix,
                    this.dataset)
                    + this.suffix
                    + ".dap";
            return url;
        }

        public String makequery()
        {
            if(this.ce == null)
                return null;
            return this.ce;
        }

        public boolean matches(String name)
        {
            String thisname = this.dataset + this.suffix;
            return thisname.equals(name);
        }

    }

    //////////////////////////////////////////////////
    // Instance variables

    protected MockMvc mockMvc;

    protected List<TestCase> alltestcases = new ArrayList<>();

    protected List<TestCase> chosentests = new ArrayList<>();

    //////////////////////////////////////////////////

    @Before
    public void setup()
            throws Exception
    {
        StandaloneMockMvcBuilder mvcbuilder = USED4TS
                ? MockMvcBuilders.standaloneSetup(new D4TSController())
                : MockMvcBuilders.standaloneSetup(new Dap4Controller());
        mvcbuilder.setValidator(new TestServlet.NullValidator());
        this.mockMvc = mvcbuilder.build();
        testSetup();
        if(prop_ascii)
            Generator.setASCII(true);
        TestCase.setRoots(getResourceRoot());
        defineAlltestcases();
        choosetests();
        if(USEDAPDMR) {
            String dapdir = canonjoin(TestCase.resourcepath(), DAPDIR);
            String dmrdir = canonjoin(TestCase.resourcepath(), DMRDIR);
            File dapfile = new File(dapdir);
            if(!dapfile.exists()) dapfile.mkdirs();
            File dmrfile = new File(dmrdir);
            if(!dmrfile.exists()) dmrfile.mkdirs();
        } else {
            String rawdir = canonjoin(TestCase.resourcepath(), RAWDIR);
            File rawfile = new File(rawdir);
            if(!rawfile.exists()) rawfile.mkdirs();
        }

    }

    //////////////////////////////////////////////////
    // Define inputs

    protected void
    choosetests()
    {
        if(false) {
            chosentests = locate("test_atomic_array.nc");
            //chosentests = locate(5);
            prop_visual = true;
            prop_generate = false;
            DapController.DUMPDMR = true;
        } else {
            prop_generate = true;
            for(TestCase input : alltestcases) {
                chosentests.add(input);
            }
        }
    }

    //////////////////////////////////////////////////
    // Junit test methods

    @Test
    public void generate()
            throws Exception
    {
        Assert.assertFalse("No test cases specified", chosentests.size() == 0);
        for(TestCase tc : chosentests) {
            doOne(tc);
        }
    }

    //////////////////////////////////////////////////
    // Primary test method

    protected void
    doOne(TestCase tc)
            throws Exception
    {
        String inputpath = tc.inputpath();
        String dappath;
        String dmrpath;
        if(USEDAPDMR) {
            dappath = tc.generatepath(DAPDIR) + ".dap";
            dmrpath = tc.generatepath(DMRDIR) + ".dmr";
        } else {
            dappath = tc.generatepath(RAWDIR) + ".dap";
            dmrpath = tc.generatepath(RAWDIR) + ".dmr";
        }

        String url = tc.makeurl();

        String ce = tc.makequery();

        System.err.println("Input: " + inputpath);
        System.err.println("Generated (DMR):" + dmrpath);
        System.err.println("Generated (DAP):" + dappath);
        System.err.println("URL: " + url);
        if(ce != null)
            System.err.println("CE: " + ce);

        if(CEPARSEDEBUG)
            CEParserImpl.setGlobalDebugLevel(1);

        String little = tc.bigendian ? "0" : "1";
        String nocsum = tc.nochecksum ? "1" : "0";
        MvcResult result;
        if(ce == null) {
            result = perform(url, this.mockMvc,
                    RESOURCEPATH,
                    DapTestCommon.ORDERTAG, little,
                    DapTestCommon.NOCSUMTAG, nocsum,
                    DapTestCommon.TRANSLATETAG, "nc4"
            );
        } else {
            result = perform(url, this.mockMvc,
                    RESOURCEPATH,
                    DapTestCommon.CONSTRAINTTAG, ce,
                    DapTestCommon.ORDERTAG, little,
                    DapTestCommon.NOCSUMTAG, nocsum,
                    DapTestCommon.TRANSLATETAG, "nc4"
            );
        }

        // Collect the output
        MockHttpServletResponse res = result.getResponse();
        byte[] byteresult = res.getContentAsByteArray();

        if(prop_debug || DEBUGDATA) {
            DapDump.dumpbytestream(byteresult, ByteOrder.nativeOrder(), "GenerateRaw");
        }

        // Dump the dap serialization into a file
        if(prop_generate || GENERATE)
            writefile(dappath, byteresult);

        // Dump the dmr into a file  by extracting from the dap serialization
        ByteArrayInputStream bytestream = new ByteArrayInputStream(byteresult);
        ChunkInputStream reader = new ChunkInputStream(bytestream, RequestMode.DAP, ByteOrder.nativeOrder());
        String sdmr = reader.readDMR(); // Read the DMR
        if(prop_generate || GENERATE)
            writefile(dmrpath, sdmr);

        if(prop_visual) {
            visual(tc.dataset + ".dmr", sdmr);
            FileDSP src = new FileDSP();
            src.open(byteresult);
            StringWriter writer = new StringWriter();
            DSPPrinter printer = new DSPPrinter(src, writer);
            printer.print();
            printer.close();
            writer.close();
            String sdata = writer.toString();
            visual(tc.dataset + ".dap", sdata);
        }
    }

    //////////////////////////////////////////////////

    protected void
    defineAlltestcases()
    {
        add(
                new TestCase("testfiles/test_atomic_array.nc"),
                new TestCase("testfiles/test_atomic_array.syn"),
                new TestCase("testfiles/test_atomic_types.nc"),
                new TestCase("testfiles/test_atomic_types.syn"),
                new TestCase("testfiles/test_enum.nc"),
                new TestCase("testfiles/test_enum_2.nc"),
                new TestCase("testfiles/test_enum_array.nc"),
                new TestCase("testfiles/test_fill.nc"),
                new TestCase("testfiles/test_groups1.nc"),
                new TestCase("testfiles/test_one_var.nc"),
                new TestCase("testfiles/test_one_vararray.nc"),
                new TestCase("testfiles/test_anon_dim.syn"),
                new TestCase("testfiles/test_unlim1.nc"),
                new TestCase("testfiles/test_utf8.nc"),
                new TestCase("testfiles/test_opaque.nc"),
                new TestCase("testfiles/test_opaque_array.nc"),
                new TestCase("testfiles/test_sequence_1.syn"),
                new TestCase("testfiles/test_sequence_2.syn"),
                new TestCase("testfiles/test_struct_array.nc"),
                new TestCase("testfiles/test_struct_array.syn"),
                new TestCase("testfiles/test_struct_nested.nc"),
                new TestCase("testfiles/test_struct_nested3.nc"),
                new TestCase("testfiles/test_struct_nested3.nc"),
                new TestCase("testfiles/test_struct1.nc"),
                new TestCase("testfiles/test_struct_type.nc"),
                new TestCase("testfiles/test_vlen1.nc"),
                new TestCase("testfiles/test_vlen2.nc"),
                new TestCase("testfiles/test_vlen3.nc"),
                new TestCase("testfiles/test_vlen4.nc"), // vlen inside a compound
                new TestCase("testfiles/test_vlen5.nc"),
                new TestCase("testfiles/test_vlen6.nc"),
                new TestCase("testfiles/test_vlen7.nc"),
                new TestCase("testfiles/test_vlen8.nc"),
                (USED4TS ? new TestCase("testfiles/test_vlen9.nc") : null), // vlen of compound not handled
                (USED4TS ? new TestCase("testfiles/test_vlen10.nc") : null), // CDM cannot handle nesting
                (USED4TS ? new TestCase("testfiles/test_vlen11.nc") : null), // CDM cannot handle vlen of vlen

                // Constrained generation
                new TestCase(1, "testfiles/test_one_vararray.nc", "/t[1]"),
                new TestCase(2, "testfiles/test_anon_dim.syn", "/vu32[0:3]"),
                new TestCase(3, "testfiles/test_one_vararray.nc", "/t"),
                new TestCase(4, "testfiles/test_enum_array.nc", "/primary_cloud[1:2:4]"),
                new TestCase(5, "testfiles/test_atomic_array.nc", "/vu8[1][0:2:2];/vd[1];/vs[1][0];/vo[0][1]"),
                new TestCase(6, "testfiles/test_struct_array.nc", "/s[0:2:3][0:1]"),
                new TestCase(7, "testfiles/test_opaque_array.nc", "/vo2[1][0:1]"),
                new TestCase(8, "testfiles/test_atomic_array.nc", "/v16[0:1,3]"),
                new TestCase(9, "testfiles/test_atomic_array.nc", "/v16[3,0:1]")
        );
        if(SHOWTESTCASES) {
            for(TestCase tc : alltestcases) {
                System.err.println(tc.dataset);
            }
        }
    }

    //////////////////////////////////////////////////
    // Utility methods

    protected void
    add(TestCase... cases)
    {
        for(TestCase tc : cases) {
            if(tc != null)
                this.alltestcases.add(tc);
        }
    }

    // Locate the test cases with given prefix or id
    List<TestCase>
    locate(Object tag)
    {
        List<TestCase> results = new ArrayList<TestCase>();
        if(tag instanceof Integer) {
            for(TestCase ct : this.alltestcases) {
                if(ct.id == (Integer) tag) {
                    results.add(ct);
                    break;
                }
            }
        } else if(tag instanceof String) {
            for(TestCase ct : this.alltestcases) {
                if(ct.matches((String) tag)) {
                    results.add(ct);
                    break;
                }
            }
        }
        return results;
    }
}
