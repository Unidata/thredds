package thredds.util.filesource;

import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * Run all tests in thredds.server.config.
 *
 * @author edavis
 * @since 4.0
 */
public class TestAll extends TestCase
{
  public static junit.framework.Test suite()
  {

    TestSuite suite = new TestSuite();
    suite.addTestSuite( TestChainedFileSource.class );
    suite.addTestSuite( TestBasicDescendantFileSource.class );
    suite.addTestSuite( TestBasicWithExclusionsDescendantFileSource.class );

    return suite;
  }
}
