package dap4.test;

import dap4.cdm.dsp.CDMDSP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class TestNc4Iosp extends DapTestCommon
{
    static protected final boolean DEBUG = false;

    static protected final boolean NCDUMP = true;

    static protected final Mode mode = Mode.BOTH;

    //////////////////////////////////////////////////
    // Constants

    static protected final String RESOURCEPATH = "/src/test/data/resources"; // wrt getTestInputFilesDIr
    static protected final String TESTINPUTDIR = "/testfiles";
    static protected final String BASELINEDIR = "/TestIosp/baseline";

    static protected final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    //////////////////////////////////////////////////
    // Type Declarations

    static protected class Nc4IospTest
    {
        static String inputroot = null;
        static String baselineroot = null;

        static public void
        setRoots(String input, String baseline)
        {
            inputroot = input;
            baselineroot = baseline;
        }

        String title;
        String dataset;
        String testinputpath;
        String baselinepath;

        Nc4IospTest(String dataset)
        {
            this.title = dataset;
            this.dataset = dataset;
            this.testinputpath = canonjoin(this.inputroot, dataset);
            this.baselinepath = canonjoin(this.baselineroot, dataset) + ".nc4";
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

    protected List<Nc4IospTest> alltestcases = new ArrayList<Nc4IospTest>();

    protected List<Nc4IospTest> chosentests = new ArrayList<Nc4IospTest>();

    protected String datasetpath = null;

    protected String root = null;

    //////////////////////////////////////////////////
    @Before
    public void setup() throws Exception
    {
        this.root = getResourceRoot();
        testSetup();
        Nc4IospTest.setRoots(canonjoin(getResourceRoot(), TESTINPUTDIR),
                canonjoin(getResourceRoot(), BASELINEDIR));
        defineAllTestcases();
        chooseTestcases();
    }

    protected String
    getTestFilesDir()
    {
        return "";
    }

    //////////////////////////////////////////////////
    // Define test cases

    void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate("test_struct_array.nc");
            prop_visual = true;
            prop_debug = true;
            //chosentests.add(new Nc4IospTest("test_test.nc"));
        } else {
            prop_baseline = false;
            for(Nc4IospTest tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    void defineAllTestcases()
    {
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
            doOneTest(testcase);
        }
    }

    //////////////////////////////////////////////////
    // Primary test method
    void
    doOneTest(Nc4IospTest testcase)
            throws Exception
    {
        System.err.println("Testcase: " + testcase.testinputpath);

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
        System.err.println("Testpath: " + testcase.testinputpath);
        System.err.println("Baseline: " + baselinefile);
        if(prop_baseline) {
            if(mode == Mode.DMR || mode == Mode.BOTH)
                writefile(baselinefile + ".dmr", metadata);
            if(mode == Mode.DATA || mode == Mode.BOTH)
                writefile(baselinefile + ".dap", data);
        } else if(prop_diff) { //compare with baseline
            String baselinecontent = null;
            if(mode == Mode.DMR || mode == Mode.BOTH) {
                // Read the baseline file(s)
                System.err.println("DMR Comparison:");
                try {
                    baselinecontent = readfile(baselinefile + ".dmr");
                    boolean pass = same(getTitle(), baselinecontent, metadata);
                    Assert.assertTrue("***Fail", pass);
                } catch (IOException ioe) {
                    Assert.assertTrue("baselinefile" + ".dmr: " + ioe.getMessage(), false);
                }
            }
            if(mode == Mode.DATA || mode == Mode.BOTH) {
                System.err.println("DATA Comparison:");
                try {
                    baselinecontent = readfile(baselinefile + ".dap");
                    Assert.assertTrue("***Data Fail", same(getTitle(), baselinecontent, data));

                } catch (IOException ioe) {
                    Assert.assertTrue("baselinefile" + ".dap: " + ioe.getMessage(), false);
                }
            }
        }
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

        if(!ok) {
            System.err.println("NcdumpW failed");
            System.exit(1);
        }
        //return shortenFileName(sw.toString(), ncfile.getLocation());
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
            System.exit(1);
        }
        //return shortenFileName(sw.toString(), ncfile.getLocation());
        return sw.toString();
    }


}

