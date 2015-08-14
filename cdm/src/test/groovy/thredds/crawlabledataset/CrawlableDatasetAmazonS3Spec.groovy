package thredds.crawlabledataset

import net.sf.ehcache.Element
import spock.lang.Specification

import static thredds.crawlabledataset.CrawlableDatasetAmazonS3.S3Helper.*

/**
 * @author cwardgar
 * @since 2015/08/14
 */
class CrawlableDatasetAmazonS3Spec extends Specification {
    def "S3Helper parent"() {
        expect:
        parent("s3://bucket/path/to/object") == "s3://bucket/path/to"
    }

    def "S3Helper concat"() {
        expect:
        concat("s3://bucket/path/to", "object") == "s3://bucket/path/to/object"
        concat("s3://bucket/path/to", "object/") == "s3://bucket/path/to/object"
        concat("s3://bucket/path/to", "") == "s3://bucket/path/to"
    }

    def "S3Helper s3UriParts"() {
        expect:
        s3UriParts("s3://bucket/path/to/object") as List == ["bucket", "path/to/object"]
    }

    def "S3Helper file creation and deletion"() {
        when:
        File tmpFile = createTempFile("s3://bucket/path/to/object")
        then:
        tmpFile.name == "object"

        expect:
        tmpFile.createNewFile()

        when:
        deleteFileElement(new Element("s3://bucket/path/to/object", null))
        then:
        !tmpFile.exists()
    }
}
