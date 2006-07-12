// $Id: TestAllCatGen.java,v 1.4 2006/01/20 20:42:06 caron Exp $
package thredds.cataloggen;

import junit.framework.*;

/**
 * A description
 *
 * @author edavis
 * @since Mar 30, 2005T4:26:37 PM
 */
public class TestAllCatGen extends TestCase
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TestAllCatGen.class );

  public TestAllCatGen( String name )
  {
    super( name );
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTestSuite( thredds.cataloggen.config.TestResultService.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetFilterType.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetFilter.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetNamerType.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetNamer.class );
    suite.addTestSuite( thredds.cataloggen.config.TestCatGenConfigMetadataFactory.class );
    suite.addTestSuite( thredds.cataloggen.config.TestCatalogRefExpander.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetSource.class );

    suite.addTestSuite( thredds.cataloggen.datasetenhancer.TestRegExpAndDurationTimeCoverageEnhancer.class );

    suite.addTestSuite( thredds.cataloggen.TestDatasetEnhancer1.class );
    suite.addTestSuite( thredds.cataloggen.TestDirectoryScanner.class );
    suite.addTestSuite( thredds.cataloggen.TestCollectionLevelScanner.class );
    suite.addTestSuite( thredds.cataloggen.TestSimpleCatalogBuilder.class );
    suite.addTestSuite( thredds.cataloggen.TestCatalogGen.class );

    return suite;
  }
}

/*
 * $Log: TestAllCatGen.java,v $
 * Revision 1.4  2006/01/20 20:42:06  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.3  2005/12/16 23:19:38  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/08/16 21:47:51  edavis
 * Switch from Log4j to commons logging.
 *
 * Revision 1.1  2005/03/30 23:56:07  edavis
 * Fix tests.
 *
 */