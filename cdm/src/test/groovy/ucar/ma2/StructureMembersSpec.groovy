package ucar.ma2

import nl.jqno.equalsverifier.EqualsVerifier
import spock.lang.Specification

/**
 * @author cwardgar
 * @since 2015/09/25
 */
class StructureMembersSpec extends Specification {
    def "equals contract"() {
        EqualsVerifier.forClass(StructureMembers.class).verify();
    }
}
