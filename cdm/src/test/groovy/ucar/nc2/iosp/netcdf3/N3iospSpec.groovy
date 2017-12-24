package ucar.nc2.iosp.netcdf3

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * @author cwardgar
 * @since 2015/09/16
 */
class N3iospSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(N3iospSpec)
    
    def "test invalid NetCDF object names: null or empty"() {
        expect: "null names are invalid"
        !N3iosp.isValidNetcdfObjectName(null)

        when:
        N3iosp.makeValidNetcdfObjectName(null)
        then:
        thrown(NullPointerException)

        expect: "empty names are invalid"
        !N3iosp.isValidNetcdfObjectName("")

        when:
        N3iosp.makeValidNetcdfObjectName("")
        then:
        IllegalArgumentException e = thrown()
        e.message == "Illegal NetCDF object name: ''"
    }

    def "test invalid NetCDF object names: first char"() {
        expect: "names with first chars not in ([a-zA-Z0-9_]|{UTF8}) are invalid"
        !N3iosp.isValidNetcdfObjectName(" blah")
        N3iosp.makeValidNetcdfObjectName(" blah") == "blah"

        !N3iosp.isValidNetcdfObjectName("\n/blah")
        N3iosp.makeValidNetcdfObjectName("\n/blah") == "blah"

        !N3iosp.isValidNetcdfObjectName("\u001F\u007F blah")  // Unit separator and DEL
        N3iosp.makeValidNetcdfObjectName("\u001F\u007F blah") == "blah"
    }

    def "test invalid NetCDF object names: remaining chars"() {
        expect: "names with remaining chars not in ([^\\x00-\\x1F\\x7F/]|{UTF8})* are invalid"
        !N3iosp.isValidNetcdfObjectName("1\u000F2\u007F3/4")
        N3iosp.makeValidNetcdfObjectName("1\u000F2\u007F3/4") == "1234"

        and: "names may not have trailing spaces"
        !N3iosp.isValidNetcdfObjectName("foo     ")
        N3iosp.makeValidNetcdfObjectName("foo     ") == "foo"
    }

    def "test valid NetCDF object names"() {
        expect: "valid names have syntax: ([a-zA-Z0-9_]|{UTF8})([^\\x00-\\x1F\\x7F/]|{UTF8})*"
        N3iosp.isValidNetcdfObjectName("_KfS9Jn_s9__")
        N3iosp.makeValidNetcdfObjectName("_KfS9Jn_s9__") == "_KfS9Jn_s9__"

        and: "unicode characters greater than 0x7F can appear anywhere"
        N3iosp.isValidNetcdfObjectName("\u0123\u1234\u2345\u3456")
        N3iosp.makeValidNetcdfObjectName("\u0123\u1234\u2345\u3456") == "\u0123\u1234\u2345\u3456"
    }
}
