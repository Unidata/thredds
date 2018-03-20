/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package opendap.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.File;
import java.io.FileReader;
import java.lang.invoke.MethodHandles;

// WARNING: assumes we are operating inside module directory
// when invoked

abstract public class TestFiles extends UnitTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final boolean DEBUG = false;

    // Following are with respect to threddsRoot
    static final String TESTSUFFIX = "opendap/src/test/data";
    static final String RESULTSUFFIX = "opendap/target/results";

    // With respect to TESTSUFFIX
    static final String TESTDATA1DIR = "testdata1";
    static final String BASELINE1DIR = "baseline1";

    static enum TestPart
    {
        DAS, DDS, DATADDS;
    }

    //////////////////////////////////////////////////
    // Static variables

    //////////////////////////////////////////////////
    // Static methods

    // List all the base names from testdata1
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
        String fname = testprefix + "/" + basename + partext(part);

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

    protected String testdir = null;
    protected String baselinedir = null;
    protected String resultsdir = null;
    protected String test = null;
    protected String testname = null;
    protected String threddsRoot = null;

    public TestFiles()
    {
        this.threddsRoot = getThreddsroot();
        this.testdir = threddsRoot + "/" + TESTSUFFIX + "/" + TESTDATA1DIR;
        this.baselinedir = threddsRoot + "/" + TESTSUFFIX + "/" + BASELINE1DIR;
        this.resultsdir = threddsRoot + "/" + RESULTSUFFIX;

        File tmp = new File(testdir);
        if(!tmp.exists()) {
            System.err.printf("Cannot locate %s directory; path does not exist: %n",tmp.getAbsolutePath());
            System.exit(1);
        }
        tmp = new File(baselinedir);
        if(!tmp.exists()) {
            System.err.printf("Cannot locate %s directory; path does not exist: %n",tmp.getAbsolutePath());
            System.exit(1);
        }
        tmp = new File(resultsdir);
        try {
            // wipe out the results dir
            if(tmp.exists()) {
                clearDir(tmp, true);
                tmp.delete();
            }
            tmp.mkdirs(); // make sure it exists
        } catch (Exception e) {
            System.err.println("Cannot create: " + tmp.getAbsolutePath());
            System.exit(1);
        }
        if(!tmp.canWrite()) {
            System.err.printf("Cannot write %s directory%n",tmp.getAbsolutePath());
            System.exit(1);
        }
    }

    //////////////////////////////////////////////////
    // Define the test data basenames
    //////////////////////////////////////////////////

    static String[] dastestfiles = {
            "123.nc", "123bears.nc", "bears.nc", "1990-S1700101.HDF.WVC_Lat", "1998-6-avhrr.dat", "D1",
            "Drifters", "EOSDB", "NestedSeq", "NestedSeq2", "OverideExample",
            "SimpleDrdsExample", "b31", "b31a", "ber-2002-10-01.nc",
            "ceopL2AIRS2-2.nc", "ceopL2AIRS2.nc", "ingrid", "nestedDAS",
            "pbug0001b", "synth1", "synth3", "synth4", "synth5",
            "test.01", "test.02", "test.03", "test.04", "test.05",
            "test.06", "test.06a", "test.07", "test.07a", "test.21",
            "test.22", "test.23", "test.31", "test.32", "test.50",
            "test.53", "test.55", "test.56", "test.57", "test.66",
            "test.67", "test.68", "test.69", "test.PointFile", "test.SwathFile",
            "test.an1", "test.dfp1", "test.dfr1", "test.dfr2", "test.dfr3",
            "test.gr1", "test.gr2", "test.gr3", "test.gr4", "test.gr5",
            "test.sds1", "test.sds2", "test.sds3", "test.sds4", "test.sds5",
            "test.sds6", "test.sds7", "test.vs1", "test.vs2", "test.vs3",
            "test.vs4", "test.vs5", "test1", "test2", "test3",
            "whoi"
    };

    static String[] ddstestfiles = {
            "123.nc", "123bears.nc", "bears.nc", "1990-S1700101.HDF.WVC_Lat", "1998-6-avhrr.dat", "D1",
            "Drifters", "EOSDB", "NestedSeq", "NestedSeq2", "OverideExample",
            "SimpleDrdsExample", "b31", "b31a", "ber-2002-10-01.nc",
            "ce.test.01.1", "ce.test.02.1", "ce.test.03.1", "ce.test.04.1",
            "ce.test.05.1", "ce.test.06.1", "ce.test.07.1", "ce.test.07.2", "ceopL2AIRS2-2.nc",
            "ceopL2AIRS2.nc", "ingrid", "nestedDAS", "pbug0001b", "synth1",
            "synth2", "synth3", "synth4", "synth5", "synth6",
            "test.01", "test.02", "test.03", "test.04", "test.05",
            "test.06", "test.06a", "test.07", "test.07a", "test.21",
            "test.22", "test.23", "test.31", "test.32", "test.50",
            "test.53", "test.55", "test.56", "test.57", "test.66",
            "test.67", "test.68", "test.69", "test.PointFile", "test.SwathFile",
            "test.an1", "test.dfp1", "test.dfr1", "test.dfr2", "test.dfr3",
            "test.gr1", "test.gr2", "test.gr3", "test.gr4", "test.gr5",
            "test.sds1", "test.sds2", "test.sds3", "test.sds4", "test.sds5",
            "test.sds6", "test.sds7", "test.vs1", "test.vs2", "test.vs3",
            "test.vs4", "test.vs5", "test1", "test2", "test3",
            "test4", "whoi"
    };

    static String[] ddxtestfiles = {
            "test.01"
    };

    static String[] errtestfiles = {
            "test1"
    };

    // define the xfails
    static String[] dasxfails = {
    };

    static String[] ddsxfails = {
    };

    static String[] ddxxfails = {
    };

    static String[] errxfails = {
    };

    ////////////////////
    static String[] xtestfiles = {
    };

    /*  No longer available */
    static String[][] specialtests = {
            {".das",
                    "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200810",
                    "OS_M1_20081008_TS.nc"
            }
    };
}
