/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package opendap.test;

import ucar.unidata.util.test.UnitTestCommon;

import java.io.File;
import java.io.FileReader;

// WARNING: assumes we are operating inside module directory
// when invoked

abstract public class TestFiles extends UnitTestCommon
{
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
            /*
            {".das",
                    "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200810",
                    "OS_M1_20081008_TS.nc"
            }  */
    };

}
