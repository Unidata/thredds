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
import opendap.dap.BaseType;
import opendap.dap.Server.*;
import opendap.servlet.AsciiWriter;
import opendap.servlet.GuardedDataset;
import thredds.server.opendap.GuardedDatasetImpl;
import thredds.server.opendap.NcDDS;
import thredds.servlet.DataRootHandler;
import thredds.servlet.DatasetHandler;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;


import java.io.*;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Vector;

// Test that the Constraint parsing is correct

public class TestCEEvaluator extends TestCase
{
    // Location on motherlode of the .nc files of interest:
    //  /data/ldm/pub/decoded/netcdf/grid/NCEP/NAM
    //  /opt/webroot/htdocs/motherlode/threddsCats/8088/dodsC
    //  /data/testdata/pub/decoded/netcdf/grid/NCEP/NAM


    // All .nc files are stored here
    //static final String testdir = "src/test/data/testdata2";
    //static final String testdir = "//fileserver/share/testdata/cdmUnitTest/conventions/mars";
    static final String DFALTTESTDIR = "src/test/data/testdata2";

    // Test case list
    static final String[][] testsetsx = new String[][]{
            new String[]{
                    "temp_air_01082000.nc",
                    "?time",
                    "?longitude,latitude",
                    "?time[1:2]",
                    "?t[0:2:3][3:4][4:5][0:2:6]",
            },
            new String[]{
		    "tst-PROFILER.nc",
		    "?wmoStaNum",
		    "staName"
            },
    };

    static final String[][] testsets = new String[][]{
            new String[]{
                    "temp_air_01082000.nc",
                    "?t[0:2:3][3:4][4:5][0:2:6]",
            }
    };

    //////////////////////////////////////////////////

    static final String TITLE = "DAP CEEvaluator Tests";

    //////////////////////////////////////////////////
    boolean debug = true;

    boolean generate = false;

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

        for(int i = 0; i < ntestsets && pass; i++) {
            String[] testset = testsets[i];
            int ntests = testset.length;
            String basename = testset[0];
            String path = testdir + "/" + basename;

            for(int j = 1; j < ntests && pass; j++) {
                String constraint = testset[j];
                String testname = path + constraint;
                System.out.println("Testing (" + i + "): " + testname);

                try {
                    file = new File(path);
                    ncfile = NetcdfDataset.openFile(file.getPath(), null);
                    if(ncfile == null) throw new FileNotFoundException(path);

                    ds = new GuardedDatasetImpl(path, ncfile, false);
                    dds = ds.getDDS();
                    // force the name
                    dds.setName(basename);

                    CEEvaluator ce = new CEEvaluator(dds);
                    ce.parseConstraint(constraint);
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
                    String expectedfile = String.format("%s.%02d.asc", path, j);
                    if(debug) {
                        try {
                            StringReader dresult = new StringReader(result);
                            FileReader dfile = new FileReader(expectedfile);
                            BufferedReader lnf = new BufferedReader(dfile);
                            BufferedReader lns = new BufferedReader(dresult);
                            System.err.println("-----\nresult:\n-----\n"+result);
                            String line = null;
                            System.err.println("-----\nexpected:\n-----");
                            System.err.flush();
                            while( (line = lnf.readLine()) != null) {
                                System.err.println(line);
                                System.err.flush();
                            }
                            System.err.flush();
                            lnf.close(); lns.close(); dfile.close(); dresult.close();
                        } catch (IOException ioe) {
                            System.err.println("debug failure:"+ioe);
                        }
                    }   else {
                        // Compare with expected result
                        Diff diff = new Diff(basename + constraint);
                        StringReader resultrdr = new StringReader(result);
                        FileReader expectedrdr = new FileReader(expectedfile);
                        pass = !diff.doDiff(resultrdr, expectedrdr);
                        try {
                            resultrdr.close();
                            expectedrdr.close();
                        } catch(IOException ioe) {
                            // ignore
                        }
                        junit.framework.Assert.assertTrue(testname, pass);
                    }
                } catch(Exception e) {
                    System.out.println("Fail: TestCEEvaluator: " + e.toString());
                    pass = false;
                }
                if(!pass) {
                    System.out.println("***Fail: " + path + constraint);
                } else
                    System.out.println("***Pass: " + path + constraint);
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
            ds = new GuardedDatasetImpl(path, ncfile, false);
            dds = ds.getDDS();
            // force the name
            dds.setName(basename);
            CEEvaluator ce = new CEEvaluator(dds);
            ce.parseConstraint("");
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
                ce.parseConstraint(constraint);
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

