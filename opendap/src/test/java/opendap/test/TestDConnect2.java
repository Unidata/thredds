package opendap.test;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;

import opendap.dap.DAS;
import opendap.dap.DConnect2;
import opendap.dap.DDS;
import opendap.dap.DataDDS;
import opendap.util.Getopts;
import opendap.util.InvalidSwitch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.Diff;
import ucar.unidata.util.test.category.NeedsExternalResource;

@Category(NeedsExternalResource.class)
public class TestDConnect2 extends TestSources {

  static boolean debug = false;
  static boolean createbaseline = true;

  final String TITLE = "DAP DConnect2 Tests";

// Define the test sets

  int passcount = 0;
  int xfailcount = 0;
  int failcount = 0;
  boolean verbose = true;
  String test = null;
  String testname = null;
  String testno = null;
  String ce = "";
  String testdataname = null;
  String url = null;
  boolean pass = false;
  TestSet currentTestSet = null;

  TestSetEnum[] whichtests = {
          TestSetEnum.Standard1, TestSetEnum.Constrained1, TestSetEnum.Remote2
          //TestSetEnum.Experimental
  };

  final String[] XFAIL = {
          "test.01.das", "test.07.das"
  };

  boolean isxfail(String testname, String extension) {
    // See if this is an xfail test
    for (String s : XFAIL) {
      if (!s.endsWith(extension)) continue;
      if (s.startsWith(testname)) return true;
    }
    return false;
  }

  public TestDConnect2() {
    setTitle("DAP DConnect2 Tests");
  }

  @Before
  public void setUp() {
    passcount = 0;
    xfailcount = 0;
    failcount = 0;
  }

  void dotest(String test) throws Exception {
    boolean constrained = false;
    this.test = test;
    this.testname = test;
    this.ce = "";
    // see if we are using constraints
    constrained = (test.indexOf(';') >= 0);
    if (constrained) {
      String[] pieces = test.split(";");
      this.testname = pieces[0];
      this.testno = pieces[1];
      this.ce = pieces[2];
      try {
        Integer.decode(this.testno);
      } catch (NumberFormatException nfe) {
        System.err.printf("Illegal constrained test testno: %s\n", test);
        return;
      }
    }

    if (constrained) {
      testdataname = "ce." + testname + "." + testno;
      url = currentTestSet.url + "/" + testname;
    } else {
      testdataname = testname;
      url = currentTestSet.url + "/" + testname;

    }
    if (verbose) System.out.println("*** Testing: " + testdataname);
    if (verbose) System.out.println("*** URL: " + url);


    if (!constrained) testpart(TestPart.DAS, ce);
    testpart(TestPart.DDS, ce);
    if (constrained) testpart(TestPart.DATADDS, ce);
    if (!pass)
      Assert.assertTrue(testname, pass);
  }

  void testpart(TestPart part, String ce) {
    ByteArrayOutputStream bytes = null;
    PrintStream output = null;
    bytes = new ByteArrayOutputStream();
    output = new PrintStream(bytes);
    try {
      DConnect2 dc2 = new DConnect2(url);

      switch (part) {
        case DAS:
          DAS das = dc2.getDAS();
          bytes.reset();
          das.print(output);
          break;

        case DDS:
          DDS dds = dc2.getDDS(ce);
          bytes.reset();
          dds.print(output);
          break;

        case DATADDS:
          DataDDS datadds = dc2.getData(ce);
          bytes.reset();
          datadds.print(output);
          datadds.printVal(output);
          break;

        default:
          break;
      }

      String result = new String(bytes.toByteArray());
      if (debug) {
        System.out.println("DEBUG: result: " + testname + partext(part) + ":");
        System.out.println(result);
      }

      String testdata = accessTestData(testdir, testdataname, part);
      if (testdata == null) {
        System.err.println("No comparison testdata found: " + testdataname + partext(part));
        System.err.println(result);
        if (createbaseline) {
          String fname = this.testdir + "/" + testdataname + partext(part);
          System.err.println("Creating: " + fname);
          FileWriter fw = new FileWriter(fname);
          fw.write(result);
          fw.close();
        }
        pass = createbaseline;
      } else {
        if (debug) {
          System.out.println("DEBUG: testdata: " + testname + "." + part.toString() + ":");
          System.out.println(testdata);
        }
        Diff diff = new Diff(test);
        pass = !diff.doDiff(result, testdata);
      }
      if (isxfail(testname, partext(part))) {
        System.out.println("XFail: " + test);
        xfailcount++;
        pass = true;
      } else if (!pass) {
        System.out.println("Fail: " + test);
        failcount++;
      } else {
        System.out.println("Pass: " + test);
        passcount++;
      }
    } catch (Exception hwe) {
      hwe.printStackTrace();
      pass = false;
      return;
    }
  }

  @Test
  public void testDConnect2() throws Exception {

    System.out.printf("*** Testing %s\n", TITLE);
    System.out.println("    Note: The remote tests may be slow or even fail if the server is overloaded");

    for (TestSetEnum e : whichtests) {
      currentTestSet = TestSets.get(e);
      if (currentTestSet == null) continue;
      System.out.printf("Base URL: %s\n", currentTestSet.url);

      for (String test : currentTestSet.tests) {
        dotest(test);
      }
    }

    int totalcount = passcount + failcount;
    int okcount = passcount;

    System.out.printf("*** PASSED: %d/%d; %d expected failures; %d unexpected failures\n", okcount, totalcount, xfailcount, failcount);
  }


  public static void main(String args[]) throws Exception {
    Getopts opts = null;
    try {
      opts = new Getopts("d", args);
      if (opts.getSwitch('d').set) {
        debug = true;
      }
    } catch (InvalidSwitch is) {
      throw new Exception(is);
    }
    String testdir = null;
    if (opts.argList().length > 0) testdir = opts.argList()[0];
    else testdir = ".";
    new TestDConnect2().testDConnect2();
  }

}
