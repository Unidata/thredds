package thredds.tds.idd;

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
        TestMotherlodeGribVarNames.class,
        PingMotherlode8080Test.class
})
public class TestMotherlode8080 { }