/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test;

import dap4.core.dmr.*;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.dmr.parser.ParseUtil;
import dap4.servlet.DMRPrint;
import dap4.test.util.UnitTestCommon;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class TestParserDMR extends UnitTestCommon
{
    static boolean PARSEDEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String DIR1 = "tests/src/test/data/resources/TestParsers/testinput"; // relative to opuls root
    static protected final String DIR2 = "tests/src/test/data/resources/TestServlet/baseline"; // relative to opuls root
    static protected final String DIR3 = "tests/src/test/data/resources/TestParsers/dmrset"; // relative to opuls root

    static protected final String BASELINEDIR = "tests/src/test/data/resources/TestParsers/baseline";

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

    // System properties

    protected boolean prop_diff = true;
    protected boolean prop_baseline = false;
    protected boolean prop_visual = false;
    protected String prop_controls = null;

    // Test cases
    protected List<TestCase> testcases = new ArrayList<TestCase>();
    protected int flags = ParseUtil.FLAG_NONE;
    protected boolean debug = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestParserDMR()
    {
        this("TestParserDMR");
    }

    public TestParserDMR(String name)
    {
        super(name);
        setSystemProperties();
        setControls();
        defineTestCases();
    }

    //////////////////////////////////////////////////
    // Misc. methods

    void defineTestCases()
    {
        String dirpath1 = getRoot() + "/" + DIR1;
        String dirpath2 = getRoot() + "/" + DIR2;
        String dirpath3 = getRoot() + "/" + DIR3;
        if(false) {
            testcases.add(new TestCase(dirpath1, "testx", "dmr"));
        } else {
            if(false) {
                testcases.add(new TestCase(dirpath1, "test_simple_1", "xml"));
                testcases.add(new TestCase(dirpath1, "test_simple_2", "xml"));
                testcases.add(new TestCase(dirpath1, "test_misc1", "xml"));
                testcases.add(new TestCase(dirpath1, "testall", "xml"));
            }
            if(true)
                loadDir(dirpath2);
            if(true)
                loadDir(dirpath3);
        }
    }

    void loadDir(String dirpath)
    {
        File dir = new File(dirpath);
        File[] filelist = dir.listFiles();
        for(int i = 0;i < filelist.length;i++) {
            File file = filelist[i];
            String name = file.getName();
            // check the extension
            if(!name.endsWith("dmr"))
                continue; // ignore
            String basename = name.substring(0, name.length() - ".dmr".length());
            TestCase ct = new TestCase(dirpath, basename, "dmr");
            this.testcases.add(ct);
        }
    }


    /**
     * Try to get the system properties
     */
    void setSystemProperties()
    {
        if(System.getProperty("nodiff") != null)
            prop_diff = false;
        if(System.getProperty("baseline") != null)
            prop_baseline = true;
        if(System.getProperty("visual") != null)
            prop_visual = true;
        prop_controls = System.getProperty("X");
    }

    void setControls()
    {
	if(prop_controls == null)
	    return;
        flags = ParseUtil.FLAG_NOCR; // always
	for(int i=0;i<prop_controls.length();i++) {
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
		System.err.println("unknown X option: "+c);
		break;
	    }
        }
    }

    //////////////////////////////////////////////////
    // Junit test method

    public void testParser()
        throws Exception
    {
            for(TestCase testcase : testcases) {
                if(!doOneTest(testcase)) {
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
        String baseline = getRoot() + "/" + BASELINEDIR + "/" + testcase.name + ".dmp";

        System.out.println("Testcase: " + testinput);

        document = readfile(testinput);

        // 1. push parser
        Dap4Parser pushparser = new Dap4Parser(new DapFactoryDMR());
        if(PARSEDEBUG || debug)
            pushparser.setDebugLevel(1);

        if(!pushparser.parse(document))
            throw new Exception("DMR Parse failed");
        DapDataset dmr = pushparser.getDMR();
        ErrorResponse err = pushparser.getErrorResponse();
        if(err != null)
            System.err.println("Error response:\n" + err.buildXML());
        if(dmr == null) {
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

        if(prop_visual)
            visual(testcase.name, testresult);

        boolean pass = true;
        if(prop_baseline) {
            writefile(baseline, testresult);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baseline);
            pass = compare(baselinecontent, testresult);
        }

        return pass;
    }

    //////////////////////////////////////////////////
    // Standalone main procedure

    static public void
    main(String[] argv)
        throws Exception
    {
        new TestParserDMR("TestParserDMR").testParser();
    }// main


}





