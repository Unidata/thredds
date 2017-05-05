package thredds.tds

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.Diff
import spock.lang.Specification
import thredds.TestWithLocalServer
import thredds.util.ContentType

import java.nio.charset.Charset

/**
 * Tests a fresh installation of TDS. An installation is considered "fresh" if the "tds.content.root.path" property:
 *   - points to a non-existent directory. In this case, TDS will create that directory, if it can.
 *   - points to an existing directory that does not contain a "catalog.xml" file.
 *
 * @author cwardgar
 * @since 2017-04-21
 */
class FreshTdsInstallSpec extends Specification {
    static Logger logger = LoggerFactory.getLogger(FreshTdsInstallSpec.name)
    
    String propName = "tds.content.root.path"
    
    def "TDS copies over default startup files"() {
        setup: "determine 'threddsDirectory', which is a sub-directory of '$propName'"
        String contentRootPath = System.properties[propName]
        assert contentRootPath : "'$propName' wasn't set"
        File threddsDirectory = new File(contentRootPath, "thredds")
        
        expect: "TDS created 'threddsDirectory'"
        threddsDirectory.exists()
        
        and: "it copied over the default startup files"
        // See TdsContext.afterPropertiesSet(), in the "Copy default startup files, if necessary" section.
        new File(threddsDirectory, "catalog.xml").exists()
        new File(threddsDirectory, "enhancedCatalog.xml").exists()
        new File(new File(threddsDirectory, "public"), "testdata").exists()
        new File(threddsDirectory, "threddsConfig.xml").exists()
        new File(threddsDirectory, "wmsConfig.xml").exists()
        
        and: "it created the 'logs' directory"
        new File(threddsDirectory, "logs").exists()
    }
    
    def "TDS returns expected client catalog"() {
        setup: "Identify control file for this test. It's located in src/freshInstallTest/resources/thredds/tds/"
        String controlFileName = 'enhancedCatalog.xml'
        
        and: "retrieve the sever base URI (set by Gretty) and construct catalog endpoint with it"
        String preferredBaseURI = System.properties["gretty.preferredBaseURI"]
        assert preferredBaseURI : "'$preferredBaseURI' wasn't set"
        String endpoint = "$preferredBaseURI/catalog/enhancedCatalog.xml"
        
        expect: "server responds with HTTP code 200 and XML content. method contains JUnit assertions"
        byte[] response = TestWithLocalServer.getContent(endpoint, 200, ContentType.xml)
        
        when: "compare expected XML (read from test resource) with server response, ignoring comments and whitespace"
        Diff diff = DiffBuilder.compare(Input.fromStream(getClass().getResourceAsStream(controlFileName)))
                               .withTest(Input.fromByteArray(response))
                               .ignoreComments().normalizeWhitespace().build()
    
        then: "there will be no difference between the two"
        boolean noDiffs = !diff.hasDifferences()
        if (!noDiffs) {
            logger.error("Actual XML:\n{}", new String(response, Charset.forName("UTF-8")))
        }
        noDiffs
    }
}
