package thredds.catalog2;

import org.junit.runners.Suite;
import org.junit.runner.RunWith;
import thredds.catalog2.xml.parser.stax.XMLEvent_WriteAsEncodedUnicodeMethodTest;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        thredds.catalog2.builder.util.AllTests.class,
        thredds.catalog2.simpleImpl.AllTests.class,
        thredds.catalog2.xml.parser.AllTests.class
})
public class AllTests { }