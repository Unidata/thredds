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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DDS;
import opendap.dap.parsers.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.Diff;
import ucar.unidata.util.test.category.NeedsExternalResource;

// Test that the dap2.y parsing is correct

abstract public class TestDapParser extends TestFiles
{
    static protected boolean VISUAL = false;

    static protected final int ISUNKNOWN = 0;
    static protected final int ISDAS = 1;
    static protected final int ISDDS = 2;
    static protected final int ISDDX = 3;
    static protected final int ISERR = 4;

    protected String extension = null;
    protected String baseextension = null;

    protected boolean isddx = false;

    protected String[] xfailtests = null;

    public void setExtensions(String extension, String baseextension)
    {
        this.extension = extension;
        this.baseextension = baseextension;
    }

    public TestDapParser()
    {
        setTitle("DAP Parser Tests");
    }

    @Test
    public void
    testDapParser() throws Exception
    {
        runDapParser();
    }

    @Test
    @Category(NeedsExternalResource.class)
    public void
    testDapParserSpecial() throws Exception {
        runDapParserSpecial();
    }

    protected void
    runDapParser() throws Exception
    {
        // Check that resultsdir exists and is writeable
        File resultsfile = new File(resultsdir);
        if(!resultsfile.exists() || !resultsfile.canWrite()) {
            resultsfile.mkdirs();
            if(!resultsfile.exists() || !resultsfile.canWrite()) {
                System.err.println("TestDapParser: cannot write: " + resultsdir);
                return;
            }
        }

        String[] testfilenames = null;

        if(extension.equals(".das")) {
            testfilenames = dastestfiles;
            xfailtests = dasxfails;
        } else if(extension.equals(".dds")) {
            testfilenames = ddstestfiles;
            xfailtests = ddsxfails;
        } else if(extension.equals(".ddx")) {
            testfilenames = ddxtestfiles;
            xfailtests = ddxxfails;
            isddx = true;
        } else if(extension.equals(".err")) {
            testfilenames = errtestfiles;
            xfailtests = errxfails;
        } else
            throw new Exception("TestDapParser: Unknown extension: " + extension);
        // override the test cases
        if(xtestfiles.length > 0) {
            testfilenames = xtestfiles;
        }

        for(int i = 0; i < testfilenames.length; i++) {
            String test = testfilenames[i];
            System.out.flush();
            this.test = test;
            this.testname = test;
            System.out.println("Testing file: " + test + extension);
            boolean isxfail = false;
            for(String s : xfailtests) {
                if(s.equals(test)) {
                    isxfail = true;
                    break;
                }
            }
            Test1(test, testdir, resultsdir, baselinedir, extension, baseextension, true);
        }
    }

    protected void
    runDapParserSpecial() throws Exception {
        // Test special cases
        for (int i = 0; i < specialtests.length; i++) {
            String thisext = specialtests[i][0];
            if (!extension.equals(thisext)) continue;
            String url = specialtests[i][1];
            String test = specialtests[i][2];
            System.out.flush();
            this.test = test;
            this.testname = test;
            System.out.println("Testing file: " + url + "/" + test + extension);
            Test1(test, url, resultsdir, baselinedir, extension, baseextension, false);
        }
    }

    void
    Test1(String test, String testdir, String resultsdir, String baselinedir,
          String extension, String baseextension, boolean isfile)
            throws Exception
    {
        int kind = ISUNKNOWN;
        if(extension.equals(".das")) kind = ISDAS;
        else if(extension.equals(".dds")) kind = ISDDS;
        else if(extension.equals(".ddx")) kind = ISDDX;
        else if(extension.equals(".err")) kind = ISERR;
        else
            throw new Exception("TestDapParser: Unknown extension: " + extension);
        String testfilepath = testdir + "/" + test + extension;
        String resultfilepath = resultsdir + "/" + test + extension;
        File testfile = null;
        if(isfile) {
            testfile = new File(testfilepath);
            if(!testfile.canRead())
                throw new Exception("TestDapParser: cannot read: " + testfile.toString());
        }
        try (
                InputStream teststream = (isfile ? new FileInputStream(testfile)
                        : new URL(testfilepath).openConnection().getInputStream());
                FileOutputStream resultstream = new FileOutputStream(resultfilepath)
        ) {

        /* try parsing .dds | .das | error | .ddx */

            switch (kind) {
            case ISDAS:
                DAS das = new DAS();
                das.parse(teststream);
                das.print(resultstream);
                break;
            case ISDDS:
                DDS dds = new DDS();
                dds.parse(teststream);// Do not validate
                dds.print(resultstream);
                break;
            case ISDDX:
                DDS ddx = new DDS();
                ddx.parseXML(teststream, false);
                ddx.print(resultstream);
                break;
            case ISERR:
                DAP2Exception err = new DAP2Exception();
                err.parse(teststream);
                err.print(resultstream);
                break;
            default:
                throw new ParseException("Unparseable file: " + testfilepath);
            }

        }

        // Open the baseline file
        String basefilepath = baselinedir + "/" + test + baseextension;
        String result = readfile(resultfilepath);
        String baseline = readfile(basefilepath);
        if(VISUAL) {
            System.err.println("TestDapParser: result:");
            System.err.println(result);
        }
        // Diff the two files
        Diff diff = new Diff(test);
        boolean pass = !diff.doDiff(baseline, result);
        if(!pass)
            Assert.assertTrue(testname, pass);
        System.out.flush();
        System.err.flush();
    }


}

