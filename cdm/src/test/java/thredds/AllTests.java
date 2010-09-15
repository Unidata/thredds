package thredds;

import org.junit.runners.Suite;
import org.junit.runner.RunWith;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        thredds.catalog.TestCatalogAll.class,
        //thredds.catalog2.AllTests.class,
        thredds.cataloggen.TestAllCatGen.class,
        thredds.crawlabledataset.TestAllCrawlableDataset.class,
        thredds.util.TestUriResolver.class,
        thredds.wcs.TestWcsServer.class
})
public class AllTests
{
}