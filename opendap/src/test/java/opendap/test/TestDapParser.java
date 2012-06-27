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
import ucar.unidata.test.Diff;

import java.io.*;
import java.net.URL;

// Test that the dap.y parsing is correct

public class TestDapParser extends TestFiles {

  static final int ISUNKNOWN = 0;
  static final int ISDAS = 1;
  static final int ISDDS = 2;
  static final int ISERR = 3;


  String extension = null;

  String[] xfailtests = null;

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public TestDapParser() {
    setTitle("DAP Parser Tests");
  }

  public void parse() throws Exception {
    // Check that resultsdir exists and is writeable
    File resultsfile = new File(resultsdir);
    if (!resultsfile.exists() || !resultsfile.canWrite()) {
      resultsfile.mkdirs();
      if (!resultsfile.exists() || !resultsfile.canWrite()) {
        System.err.println("TestDapParser: cannot write: " + resultsdir);
        return;
      }
    }

    String[] testfilenames = null;

    if (extension.equals(".das")) {
      testfilenames = dastestfiles;
      xfailtests = dasxfails;
    } else if (extension.equals(".dds")) {
      testfilenames = ddstestfiles;
      xfailtests = ddsxfails;
    } else if (extension.equals(".err")) {
      testfilenames = errtestfiles;
      xfailtests = errxfails;
    } else
      throw new Exception("TestDapParser: Unknown extension: " + extension);
    // override the test cases
    if (xtestfiles.length > 0) {
      testfilenames = xtestfiles;
    }

    for (int i = 0; i < testfilenames.length; i++) {
      String test = testfilenames[i];
      System.out.flush();
      this.test = test;
      this.testname = test;
      System.out.println("Testing file: " + test);
      boolean isxfail = false;
      for (String s : xfailtests) {
        if (s.equals(test)) {
          isxfail = true;
          break;
        }
      }
      if (false)
        Test1(test, testdir, resultsdir, baselinedir, extension);
    }

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
      Test1(test, url, resultsdir, baselinedir, extension);
    }
  }

  void
  Test1(String test, String testdir, String resultsdir, String baselinedir,
        String extension)
          throws Exception {
    int kind = ISUNKNOWN;
    if (extension.equals(".das")) kind = ISDAS;
    else if (extension.equals(".dds")) kind = ISDDS;
    else if (extension.equals(".err")) kind = ISERR;
    else
      throw new Exception("TestDapParser: Unknown extension: " + extension);
    boolean isfile = testdir.startsWith("file:");

    InputStream teststream = null;
    FileOutputStream resultstream = null;

    String testfilepath = testdir + "/" + test + extension;
    String resultfilepath = resultsdir + "/" + test + extension;
    if (isfile) {
      File testfile = new File(testfilepath);
      if (!testfile.canRead())
        throw new Exception("TestDapParser: cannot read: " + testfile.toString());
      teststream = new FileInputStream(testfile);
    } else
      teststream = new URL(testfilepath).openConnection().getInputStream();

    File resultfile = new File(resultfilepath);
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
        throw new ParseException("Unparseable file: " + testfilepath);
    }

    if (isfile) try {
      teststream.close();
    } catch (IOException ioe) {
    }
    ;

    switch (kind) {
      case ISDDS:
        dds.print(resultstream);
        break;
      case ISDAS:
        das.print(resultstream);
        break;
      case ISERR:
        err.print(resultstream);
        break;
    }

    try {
      resultstream.close();
      // Open the baseline file
      String basefilepath = baselinedir + "/" + test + extension;
      File basefile = new File(basefilepath);
      FileInputStream basestream = new FileInputStream(basefile);
      // Diff the two files
      Diff diff = new Diff(test);
      FileReader resultrdr = new FileReader(resultfile);
      FileReader baserdr = new FileReader(basefile);
      boolean pass = !diff.doDiff(baserdr, resultrdr);

      baserdr.close();
      resultrdr.close();
      if (!pass) {
        assertTrue(testname, pass);
      }
    } catch (IOException ioe) {
      System.err.println("Close failure");
    }
    System.out.flush();
    System.err.flush();
  }


}

