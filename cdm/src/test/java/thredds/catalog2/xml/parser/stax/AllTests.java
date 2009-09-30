package thredds.catalog2.xml.parser.stax;

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
        DateTypeParserTest.class,
        KeyphraseParserTest.class,
        ParseCatalogTest.class,
        ParseMetadataTest.class,
        ParseThreddsMetadataCreatedDate.class,
        ProjectParserTest.class,
        TimeCoverageElementParserTest.class,
        XMLEvent_WriteAsEncodedUnicodeMethodTest.class
})
public class AllTests
{ }