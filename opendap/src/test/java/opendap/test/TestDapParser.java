/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package opendap.test;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DDS;
import opendap.dap.parsers.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Diff;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;

@RunWith(JUnitParamsRunner.class)
public class TestDapParser extends TestFiles
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static protected boolean VISUAL = false;

    static protected final int ISUNKNOWN = 0;
    static protected final int ISDAS = 1;
    static protected final int ISDDS = 2;
    static protected final int ISDDX = 3;
    static protected final int ISERR = 4;

    protected boolean isddx = false;

    protected String[] xfailtests = null;

    public TestDapParser()
    {
        setTitle("DAP Parser Tests");
    }

    private Object[] extensionValues() {
        return new Object[] {
                new Object[] { ".das", ".das" },  // Test that the DAS parsing is correct
                new Object[] { ".dds", ".dds" },  // Test that the DDS parsing is correct
                new Object[] { ".err", ".err" }   // Test that the error body parsing is correct
        };
    }

    @Test
    @Parameters(method = "extensionValues")
    public void testDapParser(String extension, String baseextension) throws Exception
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

    @Test
    @Parameters(method = "extensionValues")
    @Category(NeedsExternalResource.class)
    public void testDapParserSpecial(String extension, String baseextension) throws Exception {
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
