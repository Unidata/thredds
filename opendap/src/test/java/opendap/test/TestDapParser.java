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

import opendap.dap.parsers.*;
import opendap.dap.*;

import java.io.*;

// Test that the DDS parsing is correct

public class TestDapParser extends TestFiles
{

    static final int ISUNKNOWN = 0;
    static final int ISDAS = 1;
    static final int ISDDS = 2;
    static final int ISERR = 3;

    boolean debug = false;
    String TITLE = "DAP DDS Parser Tests";

    String extension = null;
    int kind = ISUNKNOWN;

    String[] xfailtests = null;

    public TestDapParser(String name, String testdir, String ext) {
        super(name, testdir);
	this.extension = ext;
 
    }

    public void test() throws Exception
    {
	// Check that resultsdir exists and is writeable
	File resultsdir = new File(resultspath);
        if(!resultsdir.exists() || !resultsdir.canWrite()) {
	    resultsdir.mkdirs();
            if(!resultsdir.exists() || !resultsdir.canWrite()) {
	        System.err.println("TestDapParser: cannot write: "+resultsdir);
	        return;
	    }
	}

	String[] testfilenames = null;


    if(extension.equals(".das")) {
        testfilenames = dastestfiles;
        xfailtests = dasxfails;
        kind = ISDAS;
    } else if(extension.equals(".dds")) {
        testfilenames = ddstestfiles;
        xfailtests = ddsxfails;
        kind = ISDDS;
    } else if(extension.equals(".err")) {
        testfilenames = errtestfiles;
        xfailtests = errxfails;
        kind = ISERR;
    } else
	    throw new Exception("TestDapParser: Unknown extension: "+extension);
    // override the test cases
    if(xtestfiles.length > 0) {
        testfilenames = xtestfiles;
    }

    for(int i=0;i<testfilenames.length;i++) {
        String test = testfilenames[i];   System.out.flush();
            this.test = test;
            this.testname = test;
        System.out.println("Testing file: "+test);
        boolean isxfail = false;
        for(String s: xfailtests) {
            if(s.equals(test)) {isxfail = true; break;}
        }
	    FileInputStream teststream;
	    FileOutputStream resultstream;
	    String testfilepath = testdir     + "/" + test + extension;
	    String resultpath   = resultspath + "/" + test + extension;
	    File testfile = new File(testfilepath);
	    File resultfile = new File(resultpath);
	    if(!testfile.canRead()) {
		    System.err.println("TestDapParser: cannot read: "+testfile.toString());
	        continue;
	    }
	    teststream = new FileInputStream(testfile);
	    resultstream = new FileOutputStream(resultfile);

        DAS das = new DAS();
        DDS dds = new DDS();
        DAP2Exception err = new DAP2Exception();

        /* try parsing .dds | .das | error */

        switch (kind) {
        case ISDAS:
	        das.parse(teststream);
            break;
        case ISDDS:
	        dds.parse(teststream);
            break;
        case ISERR:
	        err.parse(teststream);
            break;
        default:
            throw new ParseException("Unparseable file: "+testfile);
        }


	    try {teststream.close();} catch (IOException ioe) {};

	    if(extension.equals(".dds")) {
                dds.print(resultstream);
	    } else if(extension.equals(".das")) {
                das.print(resultstream);
	    } else if(extension.equals(".err")) {
                err.print(resultstream);	
	    }

	    try {
	        resultstream.close();
	        // Diff the two files
            Diff diff = new Diff(test);
	        FileReader resultrdr = new FileReader(resultfile);
	        FileReader testrdr = new FileReader(testfile);
                boolean pass = !diff.doDiff(testrdr,resultrdr);
                if(isxfail) {
                    pass = true;
                    System.err.println("***XFAIL: "+test);
                }
		        testrdr.close(); resultrdr.close();
                if(!pass) {
                    junit.framework.Assert.assertTrue(testname, pass);
                }
            } catch (IOException ioe) {
	        System.err.println("Close failure");
        }
    System.out.flush(); System.err.flush();
    }
    System.out.flush();
    }
}

