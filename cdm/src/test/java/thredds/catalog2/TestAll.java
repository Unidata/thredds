package thredds.catalog2;

import junit.framework.*;
import thredds.catalog2.simpleImpl.TestCatalogImpl;
import thredds.catalog2.simpleImpl.TestPropertyImpl;
import thredds.catalog2.simpleImpl.TestServiceImpl;
import thredds.catalog2.xml.parser.TestCatalogParser;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestAll extends TestCase
{
  public TestAll( String name )
  {
    super( name );
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTestSuite( TestCatalogImpl.class );
    suite.addTestSuite( TestPropertyImpl.class );
    suite.addTestSuite( TestServiceImpl.class );

    suite.addTestSuite( TestCatalogParser.class );

    return suite;
  }
}
