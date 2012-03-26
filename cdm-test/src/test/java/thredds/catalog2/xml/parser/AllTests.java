package thredds.catalog2.xml.parser;

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
        thredds.catalog2.xml.parser.stax.AllTests.class,
        IdAuthorityInheritanceTest.class,
        ServiceNameInheritanceTest.class,
        TestCatalogParser.class
})
public class AllTests
{ }