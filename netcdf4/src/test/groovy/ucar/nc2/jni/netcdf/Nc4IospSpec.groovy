package ucar.nc2.jni.netcdf

import org.junit.Assume
import spock.lang.Specification
import ucar.nc2.Attribute

/**
 * Test various aspects of Nc4Iosp.
 *
 * @author cwardgar
 * @since 2016-12-27
 */
class Nc4IospSpec extends Specification {
    def setup() {
        // Ignore this class's tests if NetCDF-4 isn't present.
        // We're using setup() because it shows these tests as being ignored.
        // setupSpec() shows them as *non-existent*, which is not what we want.
        Assume.assumeTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
    }
    
    def "flush in define mode, with C lib loaded"() {
        setup:
        Nc4Iosp nc4Iosp = new Nc4Iosp()
        
        when: "flush while still in define mode"
        nc4Iosp.flush()
        
        then: "no IOException is thrown"
        notThrown IOException  // Would fail before the bug fix in this commit.
    }
    
    def "updateAttribute in define mode, with C lib loaded"() {
        setup:
        Nc4Iosp nc4Iosp = new Nc4Iosp()
    
        when: "updateAttribute while still in define mode"
        nc4Iosp.updateAttribute(null, new Attribute("foo", "bar"))
    
        then: "no IOException is thrown"
        notThrown IOException  // Would fail before the bug fix in this commit.
    }
}
