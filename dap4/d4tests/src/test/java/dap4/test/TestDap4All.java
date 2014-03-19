package dap4.test;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import dap4.test.util.UnitTestCommon;

public class TestDap4All extends UnitTestCommon
{


    //////////////////////////////////////////////////

    static String getClassName(Class c)
    {
        String[] pieces = c.getName().split("[.]");
        return pieces[pieces.length-1];
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected Class[] testclasses = new Class[]{
        TestParserDMR.class,
        TestParserCE.class,
        TestH5Iosp.class,
        TestNc4Iosp.class,
        TestServlet.class,
        TestServletConstraints.class,
        TestCDMClient.class,
        TestDSR.class,
/*    not yet testable
TestFrontPage.java
TestHyrax.java
TestSerial.java
*/
    };

    protected int testmax = testclasses.length;

    protected TestSuite[] tests = new TestSuite[testmax];
    protected TestResult[] results = new TestResult[testmax];

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestDap4All()
        throws Exception
    {
        this("TestDap4All");
    }

    public TestDap4All(String name)
        throws Exception
    {
        super(name);
    }

    //////////////////////////////////////////////////


    public void testAll()
    {
        for(int i = 0;i < testmax;i++) {
            tests[i] = new TestSuite(testclasses[i]);
            results[i] = new TestResult();
        }
        boolean pass = true;
        int nfailures = 0;
        for(int i = 0;i < testmax;i++) {
            setTitle(getClassName(testclasses[i]));
            System.out.println("Test Case: " + getTitle());
            tests[i].run(results[i]);
            if(results[i].failureCount() > 0) {
                pass = false;
                nfailures++;
                System.out.println("***Fail: " + getTitle());
            } else {
                System.out.println("***Pass: " + getTitle());
            }
        }
        assertTrue(nfailures + " Tests Failed",pass);
    }
}


