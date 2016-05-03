package opendap.test;

import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class TestSources extends TestFiles
{

    //////////////////////////////////////////////////
    // Remote test info

    /* Use this for experimenting with new URLS */
    static public final String XURL1 = "http://" + TestDir.dap2TestServer + "/dts";

    //"http://testremote.unidata.ucar.edu/thredds/dodsC/fmrc/NCEP/NAM/CONUS_12km/files";
    static final String[] X1 = {
            "test.01;1;f64"
    };

    // These shorter tests are always run
    protected final String REMOTEURLS1 = "http://" + TestDir.dap2TestServer + "/dts";
    static final String[] REMOTETESTSS1 = {
            "test.01", "test.02", "test.04", "test.05", "test.06a", "test.07a", "test.07",
            "test.21", "test.22", "test.23",
            "test.31",
            "test.50", "test.53", "test.55", "test.56", "test.57",
            "test.66", "test.67", "test.68", "test.69"
    };

    // These longer tests are optional
    protected final String REMOTEURLL1 = TestDir.dap2TestServer;
    static final String[] REMOTETESTSL1 = {
            "test.03", "b31", "b31a", "D1", "Drifters", "EOSDB", "ingrid", "nestedDAS", "NestedSeq", "NestedSeq2",
            "OverideExample", "SimpleDrdsExample",
            "test.an1", "test.dfp1", "test.gr1", "test.gr2", "test.gr3", "test.gr4", "test.gr5",
            "test.sds1", "test.sds2", "test.sds3", "test.sds4", "test.sds5",
            "test.vs1", "test.vs2", "test.vs3", "test.vs4", "test.vs5",
            "whoi"
    };

    // Following tests are to check constraint handling
    protected final String REMOTEURLC1 = "http://" + TestDir.dap2TestServer + "/dts";
    static final String[] REMOTETESTSC1 = {
            "test.01;1;f64",
            "test.02;1;b[1:2:10]",
            "test.03;1;i32[0:1][1:2][0:2]",
            "test.04;1;types.i32",
            "test.05;1;types.floats.f32",
            "test.06;1;ThreeD",
            "test.07;1;person.age",
            "test.07;2;person"
    };


    protected final String REMOTEURLC2 = "http://" + TestDir.remoteTestServer + "/thredds/dodsC/fmrc/NCEP/NAM/CONUS_12km/files";
    static final String[] REMOTEC2 = {
            "NAM_CONUS_12km_20100628_1200.grib2;01;Wind_speed[0][0][0][0]"
    };


    static enum TestSetEnum
    {
        Standard1, Long1, Constrained1, Constrained2, Remote2, Experimental;
    }

    static enum TestPart
    {
        DAS, DDS, DATADDS;
    }

    static class TestSet
    {
        public String name;
        public String url;
        public String[] tests;
    }

    static Map<TestSetEnum, TestSet> TestSets;

    static String partext(TestPart part)
    {
        switch (part) {
        case DAS:
            return ".das";
        case DDS:
            return ".dds";
        case DATADDS:
            return ".dods";
        default:
            break;
        }
        return ".dds";
    }

    static String
    accessTestData(String testprefix, String basename, TestPart part) throws Exception
    {

        String fname = testprefix + File.separator + basename + partext(part);

        String result = null;
        try {
            File f = new File(fname);
            if(!f.canRead()) return null;
            FileReader fr = new FileReader(fname);
            StringBuffer cbuf = new StringBuffer();
            int c;
            while((c = fr.read()) != -1) {
                cbuf.append((char) c);
            }
            return cbuf.toString();
        } catch (Exception e) {
            System.err.println("File io failure: " + e.toString());
            e.printStackTrace();
            throw e;
        }

    }

    public TestSources()
    {
	super();
        setup();
    }

    protected void setup()
    {
        TestSets = new HashMap<TestSetEnum, TestSet>();
        TestSet set;
        set = new TestSet();
        set.name = "Standard1";
        set.url = REMOTEURLS1;
        set.tests = REMOTETESTSS1;
        TestSets.put(TestSetEnum.Standard1, set);
        set = new TestSet();
        set.name = "Long1";
        set.url = REMOTEURLL1;
        set.tests = REMOTETESTSL1;
        TestSets.put(TestSetEnum.Long1, set);
        set = new TestSet();
        set.name = "constrained1";
        set.url = REMOTEURLC1;
        set.tests = REMOTETESTSC1;
        set = new TestSet();
        set.name = "remote2";
        set.url = REMOTEURLC2;
        set.tests = REMOTEC2;
        set = new TestSet();
        set.name = "Experimental";
        set.url = XURL1;
        set.tests = X1;
        TestSets.put(TestSetEnum.Experimental, set);
    }

}
