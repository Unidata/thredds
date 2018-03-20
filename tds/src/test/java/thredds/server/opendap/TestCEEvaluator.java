/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap;

import opendap.dap.BaseType;
import opendap.servers.*;
import opendap.servlet.AsciiWriter;
import opendap.servlet.GuardedDataset;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.Diff;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;


import java.io.*;
import java.io.FileWriter;
import java.lang.invoke.MethodHandles;
import java.util.Enumeration;

// Test that the Constraint parsing is correct

public class TestCEEvaluator extends UnitTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static boolean DEBUG = false;

    static  boolean generate = false;

    // Location on motherlode of the .nc files of interest:
    //  /data/ldm/pub/decoded/netcdf/grid/NCEP/NAM
    //  /opt/webroot/htdocs/motherlode/threddsCats/8088/dodsC
    //  /data/testdata/pub/decoded/netcdf/grid/NCEP/NAM


    // All .nc files are stored here
    //static final String testdir = "src/test/data/testdata2";
    //static final String testdir = "//fileserver/share/testdata/cdmUnitTest/conventions/mars";
    static final String DFALTTESTDIR = "src/test/data/testdata2";


    static final String[][] testsets = new String[][]{
            new String[]{
                    "0","temp_air_01082000.nc",
                    //"1","?time",
                    //"2","?longitude,latitude",
                    //"3","?time[1:2]",
                    "4","?t[0:2:3][3:4][4:5][0:2:6]",
            },
            new String[]{
		    "0","tst-PROFILER.nc",
		    "1","?wmoStaNum",
		    "2","staName"
            },
    };

    static final String[][] testsetsx = new String[][]{
            new String[]{
                    "0","temp_air_01082000.nc",
                    "4","?t[0:2:3][3:4][4:5][0:2:6]",
            }
    };

    //////////////////////////////////////////////////

    static final String TITLE = "DAP CEEvaluator Tests";

    //////////////////////////////////////////////////

    String testdir = DFALTTESTDIR;

    //////////////////////////////////////////////////
    // Constructors + etc.

    public TestCEEvaluator()
    {
        super("TestCEEvaluator");
        // Check to see if we are in the correct working directory
        String userdir = System.getProperty( "user.dir" );
        //if(userdir.endsWith("cdm")) {
        if(userdir.endsWith("tds")) {
            // we are being run under TestAll
            this.testdir = "../opendap/" +  this.testdir;
        }
    }

    //////////////////////////////////////////////////

    @Test
    public void testCEEvaluator() throws Exception
    {
        if(generate) dogenerate();
        else dotests();
    }

    void dotests() throws Exception
    {
        File file;
        NetcdfFile ncfile;
        GuardedDataset ds;
        ServerDDS dds;
        StringWriter content;
        PrintWriter pw;
        int ntestsets = testsets.length;
        AsciiWriter writer = new AsciiWriter(); // could be static
        boolean pass = true;
        String expectedfile = null;

loop:        for(int i = 0; i < ntestsets && pass; i++) {
            String[] testset = testsets[i];
            int ntests = (testset.length);
            String basename = testset[1];
            String path = testdir + "/" + basename;

            for(int j = 2; j < ntests && pass; j+=2) {
                String constraint = testset[j+1];
                String testname = path + constraint;
                System.err.println("Testing (" + i + "): " + testname);
                int caseno = 0;
                caseno = Integer.parseInt(testset[j]);

                try {
                    file = new File(path);
                    ncfile = NetcdfDataset.openFile(file.getPath(), null);
                    if(ncfile == null) throw new FileNotFoundException(path);
                    if(DEBUG)
                        visual("cdm file",ncdumpmetadata(ncfile,UnitTestCommon.extractDatasetname(path,null)));
                    ds = new GuardedDatasetCacheAndClone(path, ncfile, false);
                    dds = ds.getDDS();
                    // force the name
                    dds.setEncodedName(basename);
                    if(DEBUG) {
                        System.err.println("initial dds:\n");
                        dds.print(System.err);
                    }

                    CEEvaluator ce = new CEEvaluator(dds);
                    ce.parseConstraint(constraint,null);
                    if(DEBUG) {
                        PrintWriter w = new PrintWriter(System.err);
                        Enumeration venum = dds.getVariables();
                        boolean first = true;
                        w.print("projections: ");
                        while(venum.hasMoreElements()) {
                            ServerMethods sm = (ServerMethods)venum.nextElement();
                            if(!sm.isProject()) continue;
                            if(!first) w.print(",");
                            w.print(((BaseType)sm).getLongName());
                            first = false;
                        }
                        w.println();
                        w.print("selections: ");
                        ce.printConstraint(w);
                        w.println();
                        w.flush();
                        System.err.flush();
                    }
                    content = new StringWriter();
                    pw = new PrintWriter(content);
                    dds.printConstrained(pw);
                    writer.toASCII(pw, dds, null);
                    pw.close();
                    String result = content.toString();
                    expectedfile = String.format("%s.%02d.asc", path, caseno);
                    System.err.println("expected file: "+expectedfile);
                    if(DEBUG) {
                            StringReader dresult = new StringReader(result);
                            BufferedReader lns = new BufferedReader(dresult);
                            System.err.println("-----\nresult:\n-----\n"+result);
                            System.err.println("-----");
                            System.err.flush();
                    }
                    // Compare with expected result
                    Diff diff = new Diff(basename + constraint);
                    StringReader resultrdr = new StringReader(result);
                    FileReader expectedrdr = new FileReader(expectedfile);
                    pass = !diff.doDiff(resultrdr, expectedrdr, new OutputStreamWriter(System.err));

                    try {
                        resultrdr.close();
                        expectedrdr.close();
                    } catch(IOException ioe) {
                        // ignore
                    }
                } catch(Exception e) {
                    System.err.println("Fail: TestCEEvaluator: " + e.toString());
                    pass = false;
                }
                String passmark = (pass?"PASS":"FAIL");
                System.err.printf("***%s: %s%s  (%s)", passmark, path, constraint, expectedfile);
                System.err.flush();
                if(!pass) break loop;
            }
        }
        if(!pass)
            org.junit.Assert.assertTrue("TestCeParser", pass);
    }


    // Generate the expected results rather than testing
    void dogenerate() throws Exception
    {
        File file;
        NetcdfFile ncfile;
        GuardedDataset ds;
        ServerDDS dds;
        FileWriter content;
        PrintWriter pw;
        int ntestsets = testsets.length;
        AsciiWriter writer = new AsciiWriter(); // could be static

        for(int i = 0; i < ntestsets; i++) {
            String[] testset = testsets[i];
            int ntests = testset.length;
            String basename = testset[0];
            String path = testdir + "/" + basename;

            // generate the complete unconstrained data set
            file = new File(path);
            ncfile = NetcdfDataset.openFile(file.getPath(), null);
            if(ncfile == null) throw new FileNotFoundException(path);
            ds = new GuardedDatasetCacheAndClone(path, ncfile, false);
            dds = ds.getDDS();
            // force the name
            dds.setEncodedName(basename);
            CEEvaluator ce = new CEEvaluator(dds);
            ce.parseConstraint("",null);
            content = new FileWriter(path+".asc");
            pw = new PrintWriter(content);
            dds.print(pw);
            writer.toASCII(pw, dds, null);
            pw.close();
            content.close();

            //Generate the constrained test outputs
            for(int j = 1; j < ntests; j++) {
                String constraint = testset[j];
                String testname = path + constraint;
                dds = ds.getDDS();
                ce = new CEEvaluator(dds);
                ce.parseConstraint(constraint,null);
                content = new FileWriter(String.format("%s.%02d.asc", path, j));
                pw = new PrintWriter(content);
                dds.printConstrained(pw);
                writer.toASCII(pw, dds, null);
                pw.close();
                content.close();
            }
        }
    }
}

