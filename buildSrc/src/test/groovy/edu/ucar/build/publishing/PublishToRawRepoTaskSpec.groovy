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
    
    def "stripLeadingAndTrailingSlashes"() {
        expect: 'empty string produces empty string'
        PublishToRawRepoTask.stripLeadingAndTrailingSlashes('') == ''
    
        and: 'before and after stripped'
        PublishToRawRepoTask.stripLeadingAndTrailingSlashes('/some/path/') == 'some/path'
    
        and: 'multiple stripped before, none stripped in middle '
        PublishToRawRepoTask.stripLeadingAndTrailingSlashes('//bunch//of//slashes') == 'bunch//of//slashes'
    
        and: 'slash-only string produces empty string'
        PublishToRawRepoTask.stripLeadingAndTrailingSlashes('/////') == ''
    }
    
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
    
    def "makeResourcePath"() {
        expect: 'common case'
        PublishToRawRepoTask.makeResourcePath("thredds-doc", "/5.0.0", new File("/thredds/docs/website"),
                                              new File("/thredds/docs/website/netcdf-java/CDM/index.adoc")) ==
                "/repository/thredds-doc/5.0.0/netcdf-java/CDM/index.adoc"
    
        and: 'empty destPath'
        PublishToRawRepoTask.makeResourcePath("thredds-doc", "/", new File("/thredds/docs/website"),
                                              new File("/thredds/docs/website/netcdf-java/CDM/index.adoc")) ==
                "/repository/thredds-doc/netcdf-java/CDM/index.adoc"

        and: 'srcBase == srcFile'
        PublishToRawRepoTask.makeResourcePath("cdm-unit-test", "/some/dumb/prefix/",
                                              new File("/thredds/docs/website/netcdf-java/CDM/index.adoc"),
                                              new File("/thredds/docs/website/netcdf-java/CDM/index.adoc")) ==
                "/repository/cdm-unit-test/some/dumb/prefix/index.adoc"
        
        when: 'repoName is empty'
        PublishToRawRepoTask.makeResourcePath("", "5.0.0", new File("/one/two/bar.txt"), new File("/one/two/bar.txt"))
    
        then: 'an exception is thrown'
        thrown(IllegalArgumentException)
    }
}
