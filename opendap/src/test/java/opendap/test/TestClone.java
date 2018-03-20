/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package opendap.test;

import opendap.dap.parsers.*;
import opendap.dap.*;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Diff;

import java.io.*;
import java.lang.invoke.MethodHandles;

// Test that the DDS parsing is correct

public class TestClone extends TestFiles {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

