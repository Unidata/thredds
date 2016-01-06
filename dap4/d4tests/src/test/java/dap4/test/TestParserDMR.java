/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test;

import dap4.core.dmr.*;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.dmr.parser.ParseUtil;
import dap4.servlet.DMRPrint;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class TestParserDMR extends DapTestCommon
{
    static boolean PARSEDEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String DIR1 = "d4tests/src/test/data/resources/TestParsers/testinput"; // relative to dap4 root
    static protected final String DIR2 = "d4tests/src/test/data/resources/TestServlet/baseline"; // relative to dap4 root
    static protected final String DIR3 = "d4tests/src/test/data/resources/TestParsers/dmrset"; // relative to dap4  root

    static protected final String BASELINEDIR = "d4tests/src/test/data/resources/TestParsers/baseline";

    //////////////////////////////////////////////////

    static protected class TestCase
    {
        String name;
        String dir;
        String ext;

        public TestCase(String dir, String name, String ext)
        {
            this.name = name;
            this.dir = dir;
            this.ext = ext;
        }
    }
    //////////////////////////////////////////////////
    // Instance methods

    // Test cases
    protected List<TestCase> alltestcases = new ArrayList<TestCase>();
    protected List<TestCase> chosentests = new ArrayList<TestCase>();
    protected int flags = ParseUtil.FLAG_NONE;
    protected boolean debug = false;

    //////////////////////////////////////////////////

    @Before
    public void setup() {
        setControls();
        defineTestCases();
        chooseTestcases();
    }

    //////////////////////////////////////////////////
    // Misc. methods

    protected void
    chooseTestcases()
    {
        if (true) {
            chosentests = locate("test_atomic_array.syn");
        } else {
            for (TestCase tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    // Locate the test cases with given prefix
    List<TestCase>
    locate(String prefix)
    {
        List<TestCase> results = new ArrayList<TestCase>();
        for (TestCase ct : this.alltestcases) {
            if (ct.name.startsWith(prefix))
                results.add(ct);
        }
        return results;
    }

    protected void defineTestCases()
    {
        String dirpath1 = getDAP4Root() + "/" + DIR1;
        String dirpath2 = getDAP4Root() + "/" + DIR2;
        String dirpath3 = getDAP4Root() + "/" + DIR3;

        if (false) {
            alltestcases.add(new TestCase(dirpath1, "test_simple_1", "xml"));
            alltestcases.add(new TestCase(dirpath1, "test_simple_2", "xml"));
            alltestcases.add(new TestCase(dirpath1, "test_misc1", "xml"));
            alltestcases.add(new TestCase(dirpath1, "testall", "xml"));
        }
        if (true)
            loadDir(dirpath2);
        if (true)
            loadDir(dirpath3);
    }

    void loadDir(String dirpath)
    {
        File dir = new File(dirpath);
        File[] filelist = dir.listFiles();
        for (int i = 0; i < filelist.length; i++) {
            File file = filelist[i];
            String name = file.getName();
            // check the extension
            if (!name.endsWith("dmr"))
                continue; // ignore
            String basename = name.substring(0, name.length() - ".dmr".length());
            TestCase ct = new TestCase(dirpath, basename, "dmr");
            this.alltestcases.add(ct);
        }
    }

    void setControls()
    {
        if (prop_controls == null)
            return;
        flags = ParseUtil.FLAG_NOCR; // always
        for (int i = 0; i < prop_controls.length(); i++) {
            char c = prop_controls.charAt(i);
            switch (c) {
            case 'w':
                flags |= ParseUtil.FLAG_TRIMTEXT;
                break;
            case 'l':
                flags |= ParseUtil.FLAG_ELIDETEXT;
                break;
            case 'e':
                flags |= ParseUtil.FLAG_ESCAPE;
                break;
            case 'T':
                flags |= ParseUtil.FLAG_TRACE;
                break;
            case 'd':
                debug = true;
                break;
            default:
                System.err.println("unknown X option: " + c);
                break;
            }
        }
    }

    //////////////////////////////////////////////////
    // Junit test method

    @Test
    public void testParser()
            throws Exception
    {
        for (TestCase testcase : chosentests) {
            if (!doOneTest(testcase)) {
                assertTrue(false);
                System.exit(1);
            }
        }
    }

    boolean
    doOneTest(TestCase testcase)
            throws Exception
    {
        String document;
        int i, c;

        String testinput = testcase.dir + "/" + testcase.name + "." + testcase.ext;
        String baseline = getDAP4Root() + "/" + BASELINEDIR + "/" + testcase.name + ".dmp";

        System.out.println("Testcase: " + testinput); System.out.flush();

        document = readfile(testinput);

        // 1. push parser
        Dap4Parser pushparser = new Dap4Parser(new DapFactoryDMR());
        if (PARSEDEBUG || debug)
            pushparser.setDebugLevel(1);

        if (!pushparser.parse(document))
            throw new Exception("DMR Parse failed: "+testinput);
        DapDataset dmr = pushparser.getDMR();
        ErrorResponse err = pushparser.getErrorResponse();
        if (err != null)
            System.err.println("Error response:\n" + err.buildXML());
        if (dmr == null) {
            System.err.println("No dataset created");
            return false;
        }

        // Dump the parsed DMR for comparison purposes
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DMRPrint dapprinter = new DMRPrint(pw);
        dapprinter.printDMR(dmr);
        pw.close();
        sw.close();
        String testresult = sw.toString();

        if (prop_visual)
            visual(testcase.name, testresult);

        boolean pass = true;
        if (prop_baseline) {
            writefile(baseline, testresult);
        } else if (prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baseline);
            pass = compare(baselinecontent, testresult);
        }

        return pass;
    }
}
