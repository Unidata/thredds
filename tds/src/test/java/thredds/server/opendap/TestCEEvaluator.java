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

package thredds.server.opendap;

import junit.framework.TestCase;
import opendap.dap.BaseType;
import opendap.Server.*;
import opendap.servlet.AsciiWriter;
import opendap.servlet.GuardedDataset;
import opendap.test.Diff;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;


import java.io.*;
import java.io.FileWriter;
import java.util.Enumeration;

// Test that the Constraint parsing is correct

public class TestCEEvaluator extends TestCase
{
    static boolean debug = true;

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
                    "1","?time",
                    "2","?longitude,latitude",
                    "3","?time[1:2]",
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

    public TestCEEvaluator(String name)
    {
        super(name);
        // Check to see if we are in the correct working directory
        String userdir = System.getProperty( "user.dir" );
        if(userdir.endsWith("cdm")) {
            // we are being run under TestAll
            this.testdir = "../opendap/" +  this.testdir;
        }
    }

    protected void setUp()
    {
    }


    //////////////////////////////////////////////////

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

                    ds = new GuardedDatasetCacheAndClone(path, ncfile, false);
                    dds = ds.getDDS();
                    // force the name
                    dds.setEncodedName(basename);
                    if(debug) {System.err.println("initial dds:\n");dds.printDecl(System.err);}

                    CEEvaluator ce = new CEEvaluator(dds);
                    ce.parseConstraint(constraint,null);
                    if(debug) {
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
                    if(debug) {
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
                    pass = !diff.doDiff(resultrdr, expectedrdr, System.err);

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
            junit.framework.Assert.assertTrue("TestCeParser", pass);
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

