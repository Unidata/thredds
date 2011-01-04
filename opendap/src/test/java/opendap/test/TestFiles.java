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

import junit.framework.TestCase;

import java.io.*;

// WARNING: assumes we are operating inside cdm directory
// when invoked

public class TestFiles extends TestCase
{

    static int debug = 0;

    // Path from cdm to opendap directory
    static String opendappath = "../opendap";

    static String DFALTPREFIX = "src/test/data";
    static String testdata1dir = "testdata1";

    // Storage of test case outputs relative to root opendap directory
    static String resultspath = "target/results";

    static enum TestPart
    {
        DAS, DDS, DATADDS;
    }

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
            if (!f.canRead()) return null;
            FileReader fr = new FileReader(fname);
            StringBuffer cbuf = new StringBuffer();
            int c;
            while ((c = fr.read()) != -1) cbuf.append((char) c);
            return cbuf.toString();
        } catch (Exception e) {
            System.err.println("File io failure: " + e.toString());
            e.printStackTrace();
            throw e;
        }

    }

    String testdir = null;
    String test = null;
    String testname = null;

    public TestFiles(String name, String testdir)
    {
        super(name);
        if (testdir == null) {
            // Figure out if we are being run in cdm directory or opendap directory
            if (testdir == null) testdir = DFALTPREFIX + "/" + testdata1dir;
            // Check to see if we are in the correct working directory
            String userdir = System.getProperty( "user.dir" );
            if(userdir.endsWith("cdm")) {
                // we are being run under TestAll or TestOpenDap
                testdir = opendappath +"/" + testdir;
            }
            File tmp = new File(testdir);
            if(!tmp.exists()) {
                    System.err.println("Cannot locate testdata1 directory; path does not exist: "+tmp.getAbsolutePath());
                    System.exit(1);
            }
        }
        this.testdir = testdir;
    }

    //////////////////////////////////////////////////
    // Define the test data basenames
    //////////////////////////////////////////////////
    static String[] dastestfiles = {
            "bears.nc", "1990-S1700101.HDF.WVC_Lat", "1998-6-avhrr.dat",
            "b31", "b31a", "ber-2002-10-01.nc",
            "ceopL2AIRS2-2.nc", "ceopL2AIRS2.nc", "D1", "data.nc",
            "Drifters", "EOSDB", "fnoc1.nc", "in1.nc",
            "in_2.nc", "ingrid", "in.nc", "in_no_three_double_dmn.nc",
            "in_v.nc", "nestedDAS", "NestedSeq", "NestedSeq2",
            "OverideExample", "pbug0001b", "saco1.nc", "SimpleDrdsExample",
            "synth1", "synth2", "synth3", "synth4",
            "synth5", "synth6", "synth7", "synth8", "synth9",
            "test1",
            "test.01", "test.02", "test.03", "test.04",
            "test.05", "test.06", "test.06a", "test.07", "test.07a",
            "test.21", "test.22", "test.23",
            "test.31", "test.32",
            "test.50", "test.53", "test.55", "test.56", "test.57",
            "test.66", "test.67", "test.68", "test.69",
            "test.an1",
            "test.dfp1", "test.dfr1", "test.dfr2", "test.dfr3",
            "testfile.nc",
            "test.gr1", "test.gr2", "test.gr3", "test.gr4", "test.gr5",
            "test.nc",
            "test.PointFile",
            "test.sds1", "test.sds2", "test.sds3", "test.sds4",
            "test.sds5", "test.sds6", "test.sds7",
            "test.SwathFile",
            "test.vs1", "test.vs2", "test.vs3", "test.vs4", "test.vs5",
            "text.nc",
            "whoi",
    };

    static String[] ddstestfiles = {
            "bears.nc", "1990-S1700101.HDF.WVC_Lat", "1998-6-avhrr.dat",
            "b31", "b31a", "ber-2002-10-01.nc",
            "ceopL2AIRS2-2.nc", "ceopL2AIRS2.nc", "D1", "data.nc",
            "Drifters", "EOSDB", "fnoc1.nc", "in1.nc",
            "in_2.nc", "ingrid", "in.nc", "in_no_three_double_dmn.nc",
            "in_v.nc", "nestedDAS", "NestedSeq", "NestedSeq2",
            "OverideExample", "pbug0001b", "saco1.nc", "SimpleDrdsExample",
            "synth1", "synth2", "synth3", "synth4",
            "synth5", "synth6", "synth7", "synth8", "synth9",
            "test1",
            "test.01", "test.02", "test.03", "test.04",
            "test.05", "test.06", "test.06a", "test.07", "test.07a",
            "test.21", "test.22", "test.23",
            "test.31", "test.32",
            "test.50", "test.53", "test.55", "test.56", "test.57",
            "test.66", "test.67", "test.68", "test.69",
            "test.an1",
            "test.dfp1", "test.dfr1", "test.dfr2", "test.dfr3",
            "testfile.nc",
            "test.gr1", "test.gr2", "test.gr3", "test.gr4", "test.gr5",
            "test.nc",
            "test.PointFile",
            "test.sds1", "test.sds2", "test.sds3", "test.sds4",
            "test.sds5", "test.sds6", "test.sds7",
            "test.SwathFile",
            "test.vs1", "test.vs2", "test.vs3", "test.vs4", "test.vs5",
            "text.nc",
            "whoi",
    };

    static String[] errtestfiles = {
            "test1"
    };


    // define the xfails
    static String[] dasxfails = {
                // These failures come from way Printwriter handles escapes
                "pbug0001b",
                "bears.nc",
                "1990-S1700101.HDF.WVC_Lat",                  
                 "in1.nc", "in_2.nc", "in.nc", "in_v.nc", "in_no_three_double_dmn.nc",
                "test.nc", "text.nc",
                // These failures come from way Printwriter handles Attribute {}
                "synth2","synth6", "synth7", "synth8",
                "1998-6-avhrr.dat"

    };

    static String[] ddsxfails = {
            // Mostly due to url % escaping
            "bears.nc",
            "1990-S1700101.HDF.WVC_Lat",
            "test.dfr1", "test.dfr2", "test.dfr3",
            "test.PointFile",
            "test.sds6", "test.sds7",
            "test.SwathFile",
    };

    static String[] errxfails = {
    };

    ////////////////////
    static String[] xtestfiles = {
    };

}
