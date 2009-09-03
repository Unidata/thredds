package thredds.catalog2.simpleImpl;

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
        thredds.catalog2.simpleImpl.TestAccessImpl.class,
        thredds.catalog2.simpleImpl.TestCatalogImpl.class,
        thredds.catalog2.simpleImpl.TestCatalogRefImpl.class,
        thredds.catalog2.simpleImpl.TestDatasetImpl.class,
        thredds.catalog2.simpleImpl.TestDatasetNodeImpl.class,
        thredds.catalog2.simpleImpl.TestMetadataImpl.class,
        thredds.catalog2.simpleImpl.TestPropertyImpl.class,
        thredds.catalog2.simpleImpl.TestPropertyContainer.class,
        thredds.catalog2.simpleImpl.TestServiceImpl.class,
        GlobalServiceContainerTest.class,
        thredds.catalog2.simpleImpl.TestServiceContainer.class,
        thredds.catalog2.simpleImpl.TestThreddsMetadataImpl.class
})
public class AllTests
{ }