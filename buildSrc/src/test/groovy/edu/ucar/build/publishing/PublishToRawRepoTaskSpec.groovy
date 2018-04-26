package edu.ucar.build.publishing

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * Test static methods of PublishToRawRepoTask.
 *
 * @author cwardgar
 * @since 2017-09-30
 */
class PublishToRawRepoTaskSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(PublishToRawRepoTaskSpec)
    
    def "relativize"() {
        expect: 'common case'
        PublishToRawRepoTask.relativize(new File("/one/two/three/"), new File("/one/two/three/four/foo.txt")) ==
                "four/foo.txt"
        
        and: 'when parent and child are the same, an empty string is returned'
        PublishToRawRepoTask.relativize(new File("/one/two/bar.txt"), new File("/one/two/bar.txt")) == ""
        
        when: 'parent is not a prefix of child'
        PublishToRawRepoTask.relativize(new File("/one/two/"), new File("/three/four/baz.txt"))
        
        then: 'an exception is thrown'
        thrown(IllegalArgumentException)
    }
}
