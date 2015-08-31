package thredds.crawlabledataset.s3

import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3ObjectSummary
import spock.lang.Specification
import thredds.crawlabledataset.CrawlableDataset

/**
 * Tests CrawlableDatasetAmazonS3
 *
 * @author cwardgar
 * @since 2015/08/14
 */
class CrawlableDatasetAmazonS3Spec extends Specification {
    def "listDatasets success"() {
        setup: "create mock client that returns a listing with two objects and two directories"
        S3URI s3uri = new S3URI("s3://bucket/parentDir")
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            listObjects(s3uri) >> Mock(ObjectListing) {
                getObjectSummaries() >> [
                    Mock(S3ObjectSummary) {
                        getBucketName() >> "bucket"
                        getKey() >> "parentDir/dataset1.nc"
                    }, Mock(S3ObjectSummary) {
                        getBucketName() >> "bucket"
                        getKey() >> "parentDir/dataset2.nc"
                    }
                ]
                getCommonPrefixes() >> ['parentDir/childDir1', 'parentDir/childDir2']
            }
        }

        and: "create CrawlableDatasetAmazonS3 with mock ThreddsS3Client"
        CrawlableDataset parentDataset = new CrawlableDatasetAmazonS3(s3uri, null, threddsS3Client)

        when:
        List<CrawlableDataset> childDatasets = parentDataset.listDatasets();

        then:
        childDatasets.size() == 4
        (childDatasets[0] as CrawlableDatasetAmazonS3).s3URI == new S3URI("s3://bucket/parentDir/dataset1.nc")
        (childDatasets[1] as CrawlableDatasetAmazonS3).s3URI == new S3URI("s3://bucket/parentDir/dataset2.nc")
        (childDatasets[2] as CrawlableDatasetAmazonS3).s3URI == new S3URI("s3://bucket/parentDir/childDir1")
        (childDatasets[3] as CrawlableDatasetAmazonS3).s3URI == new S3URI("s3://bucket/parentDir/childDir2")
    }

    def "listDatasets failure"() {
        setup: "create CrawlableDataset that will fail"
        S3URI s3uri = new S3URI("s3://bucket/dir")
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client)
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(s3uri, null, threddsS3Client)

        when:
        dataset.listDatasets()

        then:
        IllegalStateException e = thrown()
        e.message == "'$s3uri' is not a collection dataset."
    }

    def "length and lastModified missing"() {
        setup:
        S3URI s3uri = new S3URI("s3://bucket/foo/dataset1.nc")

        and: "create mock client that returns null for every method"
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client)
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(s3uri, null, threddsS3Client)

        expect:
        dataset.length() == 0
        dataset.lastModified() == null
    }

    def "length and lastModified found"() {
        setup:
        S3URI s3uri = new S3URI("s3://bucket/foo/dataset2.nc")
        Date lastModified = new Date()

        and: "create mock client whose ObjectMetadata returns real values"
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            getObjectMetadata(s3uri) >> Mock(ObjectMetadata) {
                getContentLength() >> 237
                getLastModified() >> lastModified
            }
        }
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(s3uri, null, threddsS3Client)

        expect:
        dataset.length() == 237
        dataset.lastModified() == lastModified
    }
}
