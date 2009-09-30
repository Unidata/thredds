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
        TestAccessImpl.class,
        TestCatalogImpl.class,
        TestCatalogRefImpl.class,
        TestDatasetImpl.class,
        TestDatasetNodeImpl.class,
        TestMetadataImpl.class,
        TestPropertyImpl.class,
        TestPropertyContainer.class,
        TestServiceImpl.class,
        GlobalServiceContainerTest.class,
        TestServiceContainer.class,
        TestThreddsMetadataImpl.class,
        ProjectNameImplTest.class,
        VariableTest.class,
        VariableGroupTest.class
})
public class AllTests
{ }