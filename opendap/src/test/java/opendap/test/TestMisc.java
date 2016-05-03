/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.Diff;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

public class TestMisc extends UnitTestCommon
{
    // Collect testcases locally
    static public class Testcase
    {
        String title;
        String url;
        String cdl;

        public Testcase(String title, String url, String cdl)
        {
            this.title = title;
            this.url = url;
            this.cdl = cdl;
        }
    }

    String testserver = null;
    List<Testcase> testcases = null;

    public TestMisc()
    {
        setTitle("DAP Misc tests");
        // Check if we are running against remote or localhost, or what.
        testserver = TestDir.dap2TestServer;
        definetestcases();
    }

    void
    definetestcases()
    {
        String threddsRoot = getThreddsroot();
        testcases = new ArrayList<Testcase>();
        if(false) { // use this arm for debugging individual cases
            testcases.add(new Testcase("TestDODSArrayPrimitiveExample",
                "dods://" + testserver + "/dts/test.02",
                "file://" + threddsRoot + "/opendap/src/test/data/baselinemisc/test.02.cdl")
            );
        } else {
            // This test changes too often and I no longer remember why it is here.
            // testcases.add(new Testcase("TestBennoGrid Example",
            //"dods://iridl.ldeo.columbia.edu/SOURCES/.NOAA/.NCEP/.CPC/.GLOBAL/.daily/dods",
            //"file://"+threddsRoot + "/opendap/src/test/data/baselinemisc/dods.cdl")
            //);
            testcases.add(new Testcase("Constrained access",
                "dods://" + testserver + "/dts/test.22?exp.ThreeD[5:1:7][5:8][1:3]",
                "file://" + threddsRoot + "/opendap/src/test/data/baselinemisc/test.22ce.cdl")
            );
            testcases.add(new Testcase("TestDODSArrayPrimitiveExample",
                "dods://" + testserver + "/dts/test.02",
                "file://" + threddsRoot + "/opendap/src/test/data/baselinemisc/test.02.cdl")
            );

        }
    }


    @Test
    @Category(NeedsExternalResource.class)
    public void
    testMisc() throws Exception
    {
        System.out.println("TestMisc:");
        for(Testcase testcase : testcases) {
            System.out.println("url: " + testcase.url);
            boolean pass = process1(testcase);
            if(!pass) {
                Assert.assertTrue("Testing " + testcase.title, pass);
            }
        }
    }

    boolean process1(Testcase testcase)
        throws Exception
    {
        DODSNetcdfFile ncfile = new DODSNetcdfFile(testcase.url);
        if(ncfile == null)
            throw new Exception("Cannot read: " + testcase.url);
        StringWriter ow = new StringWriter();
        PrintWriter pw = new PrintWriter(ow);
        ncfile.writeCDL(pw, false);
        pw.close();
        ow.close();
        String captured = ow.toString();
        boolean pass = true;
        visual(testcase.title, captured);
        if(System.getProperty("baseline") != null) {
            baseline(testcase, captured);
        } else
            pass = diff(testcase, captured);
        return pass;
    }

    boolean diff(Testcase testcase, String captured)
        throws Exception
    {
        // See if the cdl is in a file or a string.
        if(System.getProperty("nodiff") != null)
            return true;
        Reader baserdr = null;
        if(testcase.cdl.startsWith("file://")) {
            File f = new File(testcase.cdl.substring("file://".length(), testcase.cdl.length()));
            baserdr = new FileReader(f);
        } else
            baserdr = new StringReader(testcase.cdl);
        StringReader resultrdr = new StringReader(captured);
        // Diff the two files
        Diff diff = new Diff("Testing " + testcase.title);
        boolean pass = !diff.doDiff(baserdr, resultrdr);
        baserdr.close();
        resultrdr.close();
        return pass;
    }

    protected void
    baseline(Testcase testcase, String output)
    {
        try {
            // See if the cdl is in a file or a string.
            if(!testcase.cdl.startsWith("file://"))
                return;
            File f = new File(testcase.cdl.substring("file://".length(), testcase.cdl.length()));
            Writer w = new FileWriter(f);
            w.write(output);
            w.close();
        } catch (IOException ioe) {
            System.err.println("IOException");
        }

    }

}
