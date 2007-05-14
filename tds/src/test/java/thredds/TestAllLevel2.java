package thredds;

import junit.framework.*;

/**
 * _more_
 *
 * @author edavis
 * @since May 7, 2007 10:58:38 AM
 */
public class TestAllLevel2 extends TestCase
{

  public TestAllLevel2( String name )
  {
    super( name );
  }

  public static junit.framework.Test suite()
  {

    TestSuite suite = new TestSuite();
    suite.addTestSuite( thredds.servlet.TestDataRootHandlerLevel2.class );

    return suite;
  }

}
