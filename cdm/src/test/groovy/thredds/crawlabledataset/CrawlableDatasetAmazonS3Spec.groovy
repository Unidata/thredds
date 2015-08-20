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
        String path = "s3://imos-data/index.html";
//        String path = "s3://imos-data/IMOS/ANMN/AM/NRSYON/CO2/real-time/" +
//                "IMOS_ANMN-AM_KST_20140709T033000Z_NRSYON_FV00_NRSYON-CO2-1407-realtime-raw_END-20140901T060000Z_C-20150722T081042Z.nc"
        CrawlableDatasetAmazonS3 crawlableDset = new CrawlableDatasetAmazonS3(path, null)
//        NetcdfFile ncFile = NetcdfFile.open(crawlableDset.file.absolutePath);
//        println ncFile.toString()

        expect:
        println crawlableDset.path
        println crawlableDset.name
        println crawlableDset.file?.text
        println crawlableDset.parentDataset
        println crawlableDset.exists()
        println crawlableDset.collection
        println crawlableDset.getDescendant("foo")
//        println crawlableDset.listDatasets()
        println crawlableDset.length()
        println crawlableDset.lastModified()

//        and:
//        crawlableDset.listDatasets().each {
//            println "\t$it"
//        }
//        println crawlableDset.file?.text
    }
}
