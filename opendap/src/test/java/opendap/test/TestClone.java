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
import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.util.test.Diff;

import java.io.*;

// Test that the DDS parsing is correct

public class TestClone extends TestFiles {
  static final int ISUNKNOWN = 0;
  static final int ISDAS = 1;
  static final int ISDDS = 2;
  static final int ISERR = 3;

  int kind = ISUNKNOWN;

  String[] testfilenames = null;

  boolean debug = false;
  static final String TITLE = "DAP DDS  and DAS Clone Tests";

  String extension = null;

  String[] xfailtests = null;

  public TestClone() {
    setTitle(TITLE);
  }

  @Test
  public void test() throws Exception {
    compare(dastestfiles, dasxfails, ISDAS, ".das");
    compare(ddstestfiles, ddsxfails, ISDDS, ".dds");
  }


  public void compare(String[] testfilenames, String[] xfails, int kind, String extension) throws Exception {

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
      if (xfailtests != null)
        for (String s : xfailtests) {
          if (s.equals(test)) {
            isxfail = true;
            break;
          }
        }
      // Do not try xfail tests
      if (isxfail) continue;

      FileInputStream teststream;
      String testfilepath = testdir + "/" + test + extension;
      File testfile = new File(testfilepath);
      if (!testfile.canRead()) {
        System.err.println("TestClone: cannot read: " + testfile.toString());
        continue;
      }
      teststream = new FileInputStream(testfile);

      DAS das = new DAS();
      DDS dds = new DDS();
      dds.setURL(testfilepath);
      DAS dasclone = null;
      DDS ddsclone = null;

      /* try parsing .dds | .das */

      switch (kind) {
        case ISDAS:
          das.parse(teststream);
          dasclone = (DAS) das.clone();
          break;
        case ISDDS:
          dds.parse(teststream);
          ddsclone = (DDS) dds.clone();
          break;
        default:
          throw new ParseException("Unparseable file: " + testfile);
      }

      try {
        teststream.close();
      } catch (IOException ioe) {
      }

      StringWriter resultwriter = new StringWriter();
      StringWriter clonewriter = new StringWriter();
      PrintWriter writer0 = new PrintWriter(resultwriter);
      PrintWriter writer1 = new PrintWriter(clonewriter);

      if (kind == ISDDS) {
        dds.print(writer0);
        ddsclone.print(writer1);
      } else if (kind == ISDAS) {
        das.print(writer0);
        dasclone.print(writer1);
      }
      writer0.flush();
      writer1.flush();
      String result = resultwriter.toString();
      String cloneresult = clonewriter.toString();

      try {
        // Diff the two print results
        Diff diff = new Diff(test);
        StringReader resultrdr = new StringReader(result);
        StringReader clonerdr = new StringReader(cloneresult);
        boolean pass = !diff.doDiff(resultrdr, clonerdr);
        clonerdr.close();
        resultrdr.close();
        if (!pass)
          Assert.assertTrue(testname, pass);
      } catch (IOException ioe) {
        System.err.println("Close failure");
      }
      System.out.flush();
      System.err.flush();
    }
    System.out.flush();
  }

  public static void main(String args[]) throws Exception {
    new TestClone().test();
  }

}

