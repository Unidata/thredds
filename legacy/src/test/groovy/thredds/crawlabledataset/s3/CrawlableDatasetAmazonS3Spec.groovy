package thredds.crawlabledataset.s3

import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification
import thredds.crawlabledataset.CrawlableDataset

/**
 * Tests CrawlableDatasetAmazonS3
 *
 * @author cwardgar
 * @since 2015/08/14
 */
class CrawlableDatasetAmazonS3Spec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(CrawlableDatasetAmazonS3Spec)
    
    // Shared resources are initialized in setupSpec()
    @Shared S3URI parentDirUri, childDir1Uri, childDir2Uri, dataset1Uri, dataset2Uri
    @Shared long dataset1Length, dataset2Length
    @Shared Date dataset1LastModified, dataset2LastModified

    @Shared ObjectListing parentDirObjectListing, childDir1ObjectListing, childDir2ObjectListing
    @Shared ObjectMetadata dataset1ObjectMetadata, dataset2ObjectMetadata

    // create mock client that returns a listing with two objects and two directories
    def setupSpec() {
        parentDirUri = new S3URI("s3://bucket/parentDir")
        childDir1Uri = new S3URI("s3://bucket/parentDir/childDir1")
        childDir2Uri = new S3URI("s3://bucket/parentDir/childDir2")
        dataset1Uri  = new S3URI("s3://bucket/parentDir/childDir1/dataset1.nc")
        dataset2Uri  = new S3URI("s3://bucket/parentDir/childDir2/dataset2.nc")

        dataset1Length = 1337
        dataset2Length = 42

        dataset1LastModified = new Date(1941, 11, 7)
        dataset2LastModified = new Date(1952, 2, 11)

        /*
         * These are return values from a mocked ThreddsS3Client. Together, they describe the following file collection:
         *   parentDir/
         *     childDir1/
         *       dataset1.nc
         *     childDir2/
         *       dataset2.nc
         */

        // To be returned by: threddsS3Client.listObjects(parentDirUri)
        parentDirObjectListing = Mock(ObjectListing) {
            getObjectSummaries() >> []
            getCommonPrefixes() >> [childDir1Uri.key, childDir2Uri.key]
        }

        // To be returned by: threddsS3Client.listObjects(childDir1Uri)
        childDir1ObjectListing = Mock(ObjectListing) {
            getObjectSummaries() >> [
                    Mock(S3ObjectSummary) {
                        getBucketName() >> dataset1Uri.bucket
                        getKey() >> dataset1Uri.key
                        getSize() >> dataset1Length
                        getLastModified() >> dataset1LastModified
                    }
            ]
            getCommonPrefixes() >> []
        }

        // To be returned by: threddsS3Client.listObjects(childDir2Uri)
        childDir2ObjectListing = Mock(ObjectListing) {
            getObjectSummaries() >> [
                    Mock(S3ObjectSummary) {
                        getBucketName() >> dataset2Uri.bucket
                        getKey() >> dataset2Uri.key
                        getSize() >> dataset2Length
                        getLastModified() >> dataset2LastModified
                    }
            ]
            getCommonPrefixes() >> []
        }

        // To be returned by: threddsS3Client.getObjectMetadata(dataset1Uri)
        dataset1ObjectMetadata = Mock(ObjectMetadata) {
            getContentLength() >> dataset1Length
            getLastModified() >> dataset1LastModified
        }

        // To be returned by: threddsS3Client.getObjectMetadata(dataset2Uri)
        dataset2ObjectMetadata = Mock(ObjectMetadata) {
            getContentLength() >> dataset2Length
            getLastModified() >> dataset2LastModified
        }

        // The default client is a mock ThreddsS3Client that returns default values from all its methods.
        CrawlableDatasetAmazonS3.defaultThreddsS3Client = Mock(ThreddsS3Client)
    }

    // Clear the object summary cache before each feature method runs.
    def setup() {
        CrawlableDatasetAmazonS3.clearCache()
    }


    // These getter methods rely heavily on S3URI functionality, which is already tested thoroughly in S3URISpec.
    def "getPath"() {
        expect:
        new CrawlableDatasetAmazonS3("s3://bucket/some/key").path == "s3://bucket/some/key"
    }

    def "getName"() {
        expect:
        new CrawlableDatasetAmazonS3("s3://bucket/some/other-key").name == "other-key"
    }

    def "getParentDataset"() {
        setup: "use defaultThreddsS3Client"
        CrawlableDatasetAmazonS3 parent = new CrawlableDatasetAmazonS3("s3://bucket/one/two")
        CrawlableDatasetAmazonS3 child  = new CrawlableDatasetAmazonS3("s3://bucket/one/two/three")

        expect:
        child.getParentDataset() == parent
    }

    def "getDescendant"() {
        setup: "use defaultThreddsS3Client"
        CrawlableDatasetAmazonS3 parent = new CrawlableDatasetAmazonS3("s3://bucket/one/two")
        CrawlableDatasetAmazonS3 child = new CrawlableDatasetAmazonS3("s3://bucket/one/two/three")

        expect:
        parent.getDescendant("three") == child
    }

    def "exists"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            1 * listObjects(childDir1Uri) >> childDir1ObjectListing
            1 * getObjectMetadata(dataset1Uri) >> dataset1ObjectMetadata
        }

        expect:
        new CrawlableDatasetAmazonS3(childDir1Uri, null, threddsS3Client).exists()
        new CrawlableDatasetAmazonS3(dataset1Uri, null, threddsS3Client).exists()
        !new CrawlableDatasetAmazonS3(parentDirUri.getChild("non-existent-dataset.nc"), null, threddsS3Client).exists()
    }

    def "isCollection"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            1 * listObjects(parentDirUri) >> parentDirObjectListing
            1 * listObjects(childDir2Uri) >> childDir2ObjectListing
        }

        expect:
        new CrawlableDatasetAmazonS3(parentDirUri, null, threddsS3Client).isCollection()
        new CrawlableDatasetAmazonS3(childDir2Uri, null, threddsS3Client).isCollection()
        !new CrawlableDatasetAmazonS3(dataset1Uri, null, threddsS3Client).isCollection()
    }

    def "listDatasets success"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            1 * listObjects(parentDirUri) >> parentDirObjectListing
            1 * listObjects(childDir1Uri) >> childDir1ObjectListing
            1 * listObjects(childDir2Uri) >> childDir2ObjectListing
        }

        and: "s3://bucket/parentDir"
        CrawlableDatasetAmazonS3 parentDir = new CrawlableDatasetAmazonS3(parentDirUri, null, threddsS3Client)

        when:
        List<CrawlableDataset> childDirs = parentDir.listDatasets();

        then: "there are two datasets"
        childDirs.size() == 2

        and: "s3://bucket/parentDir/childDir1"
        CrawlableDatasetAmazonS3 childDir1 = childDirs[0] as CrawlableDatasetAmazonS3
        childDir1.s3URI == childDir1Uri

        and: "s3://bucket/parentDir/childDir2"
        CrawlableDatasetAmazonS3 childDir2 = childDirs[1] as CrawlableDatasetAmazonS3
        childDir2.s3URI == childDir2Uri

        when:
        List<CrawlableDataset> childDir1Datasets = childDir1.listDatasets()

        then: "s3://bucket/parentDir/childDir1/dataset1.nc"
        childDir1Datasets.size() == 1
        (childDir1Datasets[0] as CrawlableDatasetAmazonS3).s3URI == dataset1Uri

        when:
        List<CrawlableDataset> childDir2Datasets = childDir2.listDatasets()

        then: "s3://bucket/parentDir/childDir1/dataset2.nc"
        childDir2Datasets.size() == 1
        (childDir2Datasets[0] as CrawlableDatasetAmazonS3).s3URI == dataset2Uri
    }

    def "listDatasets failure"() {
        setup: "use defaultThreddsS3Client"
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(dataset1Uri)

        when: "listObjects(dataset1Uri) will return null"
        dataset.listDatasets()

        then:
        IllegalStateException e = thrown()
        e.message == "'$dataset1Uri' is not a collection dataset."
    }

    def "length and lastModified success (missing cache)"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client)
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(dataset1Uri, null, threddsS3Client)

        when: "call length() and lastModified() without first doing listDatasets() on parent"
        def length = dataset.length()
        def lastModified = dataset.lastModified()

        then: "get the metadata directly from threddsS3Client because it wasn't in the cache"
        2 * threddsS3Client.getObjectMetadata(dataset1Uri) >> dataset1ObjectMetadata

        and: "length() is returning the stubbed value"
        length == dataset1ObjectMetadata.contentLength

        and: "lastModified() is returning the stubbed value"
        lastModified == dataset1ObjectMetadata.lastModified
    }

    def "length and lastModified success (hitting cache)"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client)
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(dataset2Uri, null, threddsS3Client)

        when: "we listDatasets() on the parent directory, filling the cache with object summaries"
        dataset.getParentDataset().listDatasets()

        and: "call length() and lastModified() with object summaries in the cache"
        def length = dataset.length()
        def lastModified = dataset.lastModified()

        then: "listObjects() is called once and getObjectMetadata never gets called"
        1 * threddsS3Client.listObjects(childDir2Uri) >> childDir2ObjectListing
        0 * threddsS3Client.getObjectMetadata(_)

        and: "length() is returning the stubbed value"
        length == dataset2ObjectMetadata.contentLength

        and: "lastModified() is returning the stubbed value"
        lastModified == dataset2ObjectMetadata.lastModified
    }

    def "length and lastModified failure (missing cache)"() {
        setup:
        S3URI nonExistentUri = parentDirUri.getChild("non-existent-dataset.nc")
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client)
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(nonExistentUri, null, threddsS3Client)

        when: "we listDatasets() on the parent directory, there will be no summary for nonExistentUri"
        dataset.getParentDataset().listDatasets()

        and: "call length() and lastModified() with object summaries in the cache"
        def length = dataset.length()
        def lastModified = dataset.lastModified()

        then: "getObjectMetadata() will get called due to cache misses"
        1 * threddsS3Client.listObjects(nonExistentUri.parent) >> parentDirObjectListing
        2 * threddsS3Client.getObjectMetadata(nonExistentUri) >> null

        and: "length() is returning the missing value: 0"
        length == 0L

        and: "lastModified() is returning the missing value: null"
        lastModified == null
    }
}
