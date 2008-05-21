package thredds.server.config;

import junit.framework.TestSuite;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestAll
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TestAll.class );

  public static junit.framework.Test suite()
  {

    TestSuite suite = new TestSuite();
    suite.addTestSuite( TestChainedFileSource.class );
    suite.addTestSuite( TestBasicDescendantFileSource.class );
    suite.addTestSuite( TestBasicWithExclusionsDescendantFileSource.class );

    return suite;
  }

}
