package thredds.catalog2;

import junit.framework.*;
import thredds.catalog2.simpleImpl.*;
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
    // Tests in thredds.catalog2.simpleImpl
    suite.addTestSuite( TestPropertyImpl.class );
    suite.addTestSuite( TestPropertyContainer.class );
    suite.addTestSuite( TestServiceImpl.class );
    suite.addTestSuite( TestServiceContainer.class );
    suite.addTestSuite( TestAccessImpl.class );
    suite.addTestSuite( TestDatasetNodeImpl.class );
    suite.addTestSuite( TestDatasetImpl.class );
    suite.addTestSuite( TestCatalogRefImpl.class );
    suite.addTestSuite( TestCatalogImpl.class );

    // Tests in thredds.catalog2.xml
    suite.addTestSuite( TestCatalogParser.class );

    return suite;
  }
}
