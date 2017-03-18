/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test;

import dap4.core.dmr.DMRFactory;
import dap4.core.dmr.DapDataset;
import dap4.core.dmr.ErrorResponse;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.dmr.parser.Dap4ParserImpl;
import dap4.core.dmr.parser.ParseUtil;
import dap4.dap4lib.DMRPrinter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


public class TestParserDMR extends DapTestCommon
{
    static final boolean PARSEDEBUG = false;

    // Do a special test to compare the dmr parser print output
    // to the original input. This will often fail in non-essential
    // ways, so it must be verified by hand.
    static final boolean BACKCOMPARE = false;

    //////////////////////////////////////////////////
    // Constants
    // Define the input set(s)
    static protected final String DIR1 = "/TestParsers/dmrset"; // relative to dap4  root
    static protected final String DIR2 = "/TestServlet/baseline"; // relative to dap4  root
    static protected final String BASELINE = "/TestParsers/baseline"; // relative to dap4 root

    //////////////////////////////////////////////////

    static protected class TestCase
    {
        static public String resourceroot = null;

        String name;
        String dir;
        String ext;
        String input;
        String baseline;

        public TestCase(String dir, String name, String ext)
        {
            this.name = name;
            this.dir = dir;
            this.ext = ext;
            this.input = resourceroot + dir + "/" + name + "." + ext;
            this.baseline = resourceroot + BASELINE + "/" + name + "." + "dmp";
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

    public TestParserDMR()
    {
        super();
        setControls();
        defineTestCases();
        chooseTestcases();
    }

    //////////////////////////////////////////////////
    // Misc. methods

    protected void
    chooseTestcases()
    {
        if(false) {
            chosentests = locate("test_atomic_array.nc");
            prop_visual = true;
            assert chosentests.size() > 0 : "Not tests chosen";
        } else {
            for(TestCase tc : alltestcases) {
                chosentests.add(tc);
            }
        }
    }

    // Locate the test cases with given prefix
    List<TestCase>
    locate(String prefix)
    {
        List<TestCase> results = new ArrayList<TestCase>();
        for(TestCase ct : this.alltestcases) {
            if(ct.name.startsWith(prefix))
                results.add(ct);
        }
        return results;
    }

    protected void defineTestCases()
    {
        String root = getResourceRoot();
        TestCase.resourceroot = root;
        loadDir(DIR1, "dmr");
        loadDir(DIR2, "dmr");
    }

    void loadDir(String dirsuffix, String... extensions)
    {
        File dir = new File(TestCase.resourceroot + dirsuffix);
        File[] filelist = dir.listFiles();
        for(int i = 0; i < filelist.length; i++) {
            File file = filelist[i];
            String name = file.getName();
            // check the extension
            String match = null;
            for(String ext : extensions) {
                if(name.endsWith(ext)) {
                    match = ext;
                    break;
                }
            }
            if(match != null) {
                String basename = name.substring(0, name.length() - (match.length() + 1));
                TestCase ct = new TestCase(dirsuffix, basename, match);
                addtestcase(ct);
            }
        }
    }

    protected void addtestcase(TestCase ct)
    {
        if(DEBUG) {
            System.err.printf("Adding Test: input=%s%n", ct.input);
            if(!new File(ct.input).exists())
                System.err.printf("             +++%s does not exist%n", ct.input);
            System.err.printf("             baseline=%s%n", ct.baseline);
            if(!new File(ct.baseline).exists())
                System.err.printf("             ***%s does not exist%n", ct.baseline);
            System.err.flush();
        }
        this.alltestcases.add(ct);
    }

    void setControls()
    {
        if(prop_controls == null)
            return;
        flags = ParseUtil.FLAG_NOCR; // always
        for(int i = 0; i < prop_controls.length(); i++) {
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
        int ntests = 0;
        for(TestCase testcase : chosentests) {
            ntests++;
            doOneTest(testcase);
        }
        Assert.assertTrue("***Pass ", true);
    }

    void
    doOneTest(TestCase testcase)
            throws Exception
    {
        String document;
        int i, c;

        String testinput = testcase.input;
        String baseline = testcase.baseline;

        System.err.println("Testcase: " + testinput);
        System.err.println("Baseline: " + baseline);
        System.err.flush();

        document = readfile(testinput);

        Dap4Parser parser = new Dap4ParserImpl(new DMRFactory());
        if(PARSEDEBUG || debug)
            parser.setDebugLevel(1);

        if(!parser.parse(document))
            throw new Exception("DMR Parse failed: " + testinput);
        DapDataset dmr = parser.getDMR();
        ErrorResponse err = parser.getErrorResponse();
        if(err != null)
            System.err.println("Error response:\n" + err.buildXML());
        if(dmr == null) {
            System.err.println("No dataset created");
            return;
        }

        // Dump the parsed DMR for comparison purposes
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DMRPrinter dapprinter = new DMRPrinter(dmr, pw);
        dapprinter.testprint();
        pw.close();
        sw.close();
        String testresult = sw.toString();

        if(prop_visual)
            visual(testcase.name, testresult);

        if(prop_baseline) {
            writefile(baseline, testresult);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent;
            if(BACKCOMPARE)
                baselinecontent = document;
            else
                baselinecontent = readfile(baseline);
            Assert.assertTrue("Files are different",
                    same(getTitle(), baselinecontent, testresult));
        }
    }
}
