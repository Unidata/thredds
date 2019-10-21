package dap4.test;

import dap4.core.util.DapUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NotJenkins;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Category(NotJenkins.class)
public class TestNc4Iosp extends DapTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static protected final boolean DEBUG = false;

    static protected final boolean NCDUMP = true;

    static protected final Mode mode = Mode.BOTH;

    //////////////////////////////////////////////////
    // Constants

    static protected String DATADIR = "d4tests/src/test/data"; // relative to dap4 root
    static protected String TESTDATADIR = "/resources/";
    static protected String BASELINEDIR = "/resources/TestIosp/baseline";
    static protected String TESTINPUTDIR = "/resources/testfiles";

    static protected final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    //////////////////////////////////////////////////
    // Type Declarations

    static protected class Nc4IospTest
    {
        static String root = null;
        String title;
        String dataset;
        String testinputpath;
        String baselinepath;

        Nc4IospTest(String dataset)
        {
            this.title = dataset;
            this.dataset = dataset;
            this.testinputpath
                    = root + "/" + TESTINPUTDIR + "/" + dataset;
            this.baselinepath
                    = root + "/" + BASELINEDIR + "/" + dataset + ".nc4";
        }

        public String toString()
        {
            return dataset;
        }
    }

    static protected enum Mode
    {
        DMR, DATA, BOTH;
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Misc variables
    protected boolean isbigendian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    // Test cases

    protected List<Nc4IospTest> alltestcases = new ArrayList<>();

    protected List<Nc4IospTest> chosentests = new ArrayList<>();

    protected String datasetpath = null;

    protected String testroot = null;

    //////////////////////////////////////////////////
    @Before
    public void setup() throws Exception
    {
        this.testroot = getTestFilesDir();
        this.datasetpath = this.testroot + "/" + DATADIR;
        File f = new File(this.datasetpath + "/" + BASELINEDIR);
        if(!f.exists()) f.mkdir();
        defineAllTestcases(this.datasetpath);
        chooseTestcases();
    }

    protected String
    getTestFilesDir()
    {
        return getDAP4Root();
    }

    //////////////////////////////////////////////////
    // Define test cases

    void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate("test_one_var.nc");
            //chosentests.add(new Nc4IospTest("test_test.nc"));
        } else {
            for(Nc4IospTest tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    // Depending on which libnetcdf4-wrapper is used, the enum names will differ
    void defineAllTestcases(String root)
    {
        Nc4IospTest.root = root;
        this.alltestcases.add(new Nc4IospTest("test_one_var.nc"));
        this.alltestcases.add(new Nc4IospTest("test_one_vararray.nc"));
        this.alltestcases.add(new Nc4IospTest("test_atomic_types.nc"));
        this.alltestcases.add(new Nc4IospTest("test_atomic_array.nc"));
        this.alltestcases.add(new Nc4IospTest("test_enum.nc"));
        this.alltestcases.add(new Nc4IospTest("test_enum_array.nc"));
        this.alltestcases.add(new Nc4IospTest("test_struct_type.nc"));
        this.alltestcases.add(new Nc4IospTest("test_struct_array.nc"));
        this.alltestcases.add(new Nc4IospTest("test_struct_nested.nc"));
        this.alltestcases.add(new Nc4IospTest("test_vlen1.nc"));
        this.alltestcases.add(new Nc4IospTest("test_vlen2.nc"));
        this.alltestcases.add(new Nc4IospTest("test_vlen3.nc"));
        this.alltestcases.add(new Nc4IospTest("test_vlen4.nc"));
        this.alltestcases.add(new Nc4IospTest("test_vlen5.nc"));
    }


    //////////////////////////////////////////////////
    // Junit test methods

    @Test
    public void testNc4Iosp()
            throws Exception
    {
        for(Nc4IospTest testcase : chosentests) {
            if(!doOneTest(testcase)) {
                Assert.assertTrue(false);
            }
        }
    }

    //////////////////////////////////////////////////
    // Primary test method
    boolean
    doOneTest(Nc4IospTest testcase)
            throws Exception
    {
        boolean pass = true;

        System.out.println("Testcase: " + testcase.testinputpath);

        NetcdfDataset ncfile = openDataset(testcase.testinputpath);

        String metadata = null;
        String data = null;
        if(mode == Mode.DMR || mode == Mode.BOTH) {
            metadata = (NCDUMP ? ncdumpmetadata(ncfile,testcase.dataset) : null);
            if(prop_visual)
                visual("Meta Data: ", metadata);
        }
        if(mode == Mode.DATA || mode == Mode.BOTH) {
            data = (NCDUMP ? ncdumpdata(ncfile,testcase.dataset) : null);
            if(prop_visual)
                visual("Data: ", data);
        }

        String baselinefile = String.format("%s", testcase.baselinepath);
        if(prop_baseline) {
            if(mode == Mode.DMR || mode == Mode.BOTH)
                writefile(baselinefile + ".dmr", metadata);
            if(mode == Mode.DATA || mode == Mode.BOTH)
                writefile(baselinefile + ".dap", data);
        } else if(prop_diff) { //compare with baseline
            String baselinecontent = null;
            if(mode == Mode.DMR || mode == Mode.BOTH) {
                // Read the baseline file(s)
                System.out.println("DMR Comparison:");
                baselinecontent = readfile(baselinefile + ".dmr");
                pass = pass && same(getTitle(), baselinecontent, metadata);
                System.out.println(pass ? "Pass" : "Fail");
            }
            if(mode == Mode.DATA || mode == Mode.BOTH) {
                System.out.println("DATA Comparison:");
                baselinecontent = readfile(baselinefile + ".dap");
                pass = pass && same(getTitle(), baselinecontent, data);
                System.out.println(pass ? "Pass" : "Fail");
            }
        }
        return pass;
    }

    //////////////////////////////////////////////////
    // Utility methods

    boolean
    report(String msg)
    {
        System.err.println(msg);
        prop_generate = false;
        return false;
    }


    // Locate the test cases with given prefix
    List<Nc4IospTest>
    locate(String prefix)
    {
        List<Nc4IospTest> results = new ArrayList<Nc4IospTest>();
        for(Nc4IospTest ct : this.alltestcases) {
            if(ct.dataset.startsWith(prefix))
                results.add(ct);
        }
        return results;
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
        if(!ok) {
            System.err.println("NcdumpW failed");
            System.exit(1);
        }
        return shortenFileName(sw.toString(), ncfile.getLocation());
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
            System.exit(1);
        }
        return shortenFileName(sw.toString(), ncfile.getLocation());
    }

}

