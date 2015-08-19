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
        s3UriParts("s3://bucket/") as List == ["bucket", ""]
        s3UriParts("s3://bucket") as List == ["bucket", ""]
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

    def "goof around"() {
        setup:
        CrawlableDatasetAmazonS3 crawlableDset = new CrawlableDatasetAmazonS3("s3://imos-data/IMOS/", null)

//        expect:
//        println crawlableDset.path
//        println crawlableDset.name
//        println crawlableDset.file
//        println crawlableDset.parentDataset
//        println crawlableDset.exists()
//        println crawlableDset.collection
//        println crawlableDset.getDescendant("foo")
//        println crawlableDset.listDatasets()
//        println crawlableDset.length()
//        println crawlableDset.lastModified()

        expect:
        crawlableDset.listDatasets().each {
            println "\t$it"
        } || true
        println crawlableDset.file.text
    }
}
