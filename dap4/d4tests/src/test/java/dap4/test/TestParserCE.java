/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test;

import dap4.ce.CEConstraint;
import dap4.ce.parser.CECompiler;
import dap4.ce.parser.CEParser;
import dap4.core.dmr.DapDataset;
import dap4.core.dmr.DapFactoryDMR;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.test.util.UnitTestCommon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestParserCE extends UnitTestCommon
{

    //////////////////////////////////////////////////
    // Constants
    static final boolean PARSEDEBUG = false;
    static final String TESTCASEDIR = "tests/src/test/data/resources/TestParsers"; // relative to opuls root

    //////////////////////////////////////////////////
    // Type decls
    static class TestSet
    {
        static public String rootdir = null;

        public String title;
        public String baseline;
        public String dmr;
        public String[] constraints;
        public String[] debug = null;

        public TestSet(String title)
            throws IOException
        {
            this.title = title;
            this.baseline = makepath(title+".dmp","baseline");
            String dmrfile = makepath(title+"_dmr.txt","testinput");
            String cefile = makepath(title+".txt","testinput");
            this.dmr = readfile(dmrfile);
            this.constraints = readfile(cefile).split("[\n]");
        }

        public TestSet setdebug(String[] debug) {this.debug = debug; return this;}
        public TestSet setdebug(String debug) {return setdebug(new String[]{debug});}

        String makepath(String file, String parent)
        {
            return getRoot() + "/" + TESTCASEDIR + "/" + parent +"/" + file;
        }

    }

    //////////////////////////////////////////////////
    // Instance methods

    // System properties

    boolean prop_diff = true;
    boolean prop_baseline = false;
    boolean prop_visual = false;

    // All test cases
    List<TestSet> alltestsets = new ArrayList<TestSet>();

    DapDataset dmr = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestParserCE()
    {
        this("TestParserCE");
    }

    public TestParserCE(String name)
    {
        super(name);
        setSystemProperties();
        try {
            defineTestCases();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    //////////////////////////////////////////////////
    // Misc. methods


    void defineTestCases()
        throws IOException
    {
        TestSet.rootdir = getRoot();
        TestSet set = new TestSet("ce1"); // take the constraints from this.txt
        //set = set.setdebug("b[10:16]");
        alltestsets.add(set);
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
    }

    //////////////////////////////////////////////////
    // Junit test method

    public void testParser()
        throws Exception
    {
        for(TestSet testset : alltestsets ) {
            if(!doOneTest(testset)) {
                assertTrue(false);
                System.exit(1);
            }
        }
    }

    boolean
    doOneTest(TestSet testset)
        throws Exception
    {
        boolean pass = true;

        System.out.println("Test Set: " + testset.title);

        // Create the DMR tree
        Dap4Parser pushparser = new Dap4Parser(new DapFactoryDMR());
        if(false)
            pushparser.setDebugLevel(1);
        boolean parseok = pushparser.parse(testset.dmr);
        if(parseok)
            dmr = pushparser.getDMR();
        if(dmr == null)
            parseok = false;
        if(!parseok)
            throw new Exception("DMR Parse failed");

        // Iterate over the constraints
        String results = "";
        CEConstraint ceroot = null;
        String[] tests = (testset.debug != null ? testset.debug : testset.constraints);
        for(String ce: tests) {
            System.out.println("constraint: " + ce);
            CEParser ceparser = null;
            try {
                ceparser = new CEParser(dmr);
                if(PARSEDEBUG)
                    ceparser.setDebugLevel(1);
                parseok = ceparser.parse(ce);
                CECompiler compiler = new CECompiler();
                ceroot = compiler.compile(dmr,ceparser.getConstraint());
            } catch (Exception e) {
                e.printStackTrace();
                parseok = false;
            }
            if(ceroot == null)
                parseok = false;
            if(!parseok)
                throw new Exception("CE Parse failed");

            // Dump the parsed CE for comparison purposes
            String cedump = ceroot.toConstraintString();
            if(prop_visual)
                visual(testset.title+" |"+ce+"|", cedump);
            results += (cedump+"\n");
        }

        if(prop_baseline) {
            writefile(testset.baseline, results);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(testset.baseline);
            pass = compare(baselinecontent, results);
        }

        return pass;
    }

    //////////////////////////////////////////////////
    // Standalone main procecedure

    static public void
    main(String[] argv)
        throws Exception
    {
        new TestParserCE("TestParserCE").testParser();
    }// main


}





