package thredds.crawlabledataset.s3

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
    // Shared resources are initialized in setupSpec()
    @Shared S3URI parentDirUri, childDir1Uri, childDir2Uri, dataset1Uri, dataset2Uri, nonExistentUri
    @Shared long dataset1Length, dataset2Length
    @Shared Date dataset1LastModified, dataset2LastModified

    @Shared ThreddsS3Listing parentDirListing, childDir1Listing, childDir2Listing
    @Shared ThreddsS3Metadata parentDirMetadata, childDir1Metadata, childDir2Metadata, dataset1Metadata, dataset2Metadata

    // create mock client that returns a listing with two objects and two directories
    def setupSpec() {
        parentDirUri = new S3URI("s3://bucket/parentDir")
        childDir1Uri = new S3URI("s3://bucket/parentDir/childDir1")
        childDir2Uri = new S3URI("s3://bucket/parentDir/childDir2")
        dataset1Uri  = new S3URI("s3://bucket/parentDir/childDir1/dataset1.nc")
        dataset2Uri  = new S3URI("s3://bucket/parentDir/childDir2/dataset2.nc")

        nonExistentUri = new S3URI("s3://bucket/parentDir/no-such-dataset.nc")

        dataset1Length = 1337
        dataset2Length = 42

        dataset1LastModified = new Date(1941, 11, 7)
        dataset2LastModified = new Date(1952, 2, 11)

        parentDirMetadata = new ThreddsS3Directory(parentDirUri)
        childDir1Metadata = new ThreddsS3Directory(childDir1Uri)
        childDir2Metadata = new ThreddsS3Directory(childDir2Uri)

        dataset1Metadata = new ThreddsS3Object(dataset1Uri, dataset1LastModified, dataset1Length)
        dataset2Metadata = new ThreddsS3Object(dataset2Uri, dataset2LastModified, dataset2Length)

        /*
         * These are return values from a mocked ThreddsS3Client. Together, they describe the following file collection:
         *   parentDir/
         *     childDir1/
         *       dataset1.nc
         *     childDir2/
         *       dataset2.nc
         */

        // To be returned by: threddsS3Client.listContents(parentDirUri)
        parentDirListing = new ThreddsS3Listing()
        parentDirListing.add(childDir1Metadata)
        parentDirListing.add(childDir2Metadata)

        // To be returned by: threddsS3Client.listContents(childDir1Uri)

        childDir1Listing = new ThreddsS3Listing()
        childDir1Listing.add(dataset1Metadata)

        // To be returned by: threddsS3Client.listContents(childDir2Uri)
        childDir2Listing = new ThreddsS3Listing()
        childDir2Listing.add(dataset2Metadata)

        // The default client is a mock ThreddsS3Client that returns default values from all its methods.
        CrawlableDatasetAmazonS3.defaultThreddsS3Client = Mock(ThreddsS3Client)
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
            1 * getMetadata(childDir1Uri) >> childDir1Metadata
            1 * getMetadata(dataset1Uri) >> dataset1Metadata
        }

        expect:
        new CrawlableDatasetAmazonS3(childDir1Uri, null, threddsS3Client).exists()
        new CrawlableDatasetAmazonS3(dataset1Uri, null, threddsS3Client).exists()
        !new CrawlableDatasetAmazonS3(nonExistentUri, null, threddsS3Client).exists()
    }

    def "isCollection"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            1 * getMetadata(parentDirUri) >> parentDirMetadata
            1 * getMetadata(childDir2Uri) >> childDir2Metadata
            1 * getMetadata(dataset1Uri) >> dataset1Metadata
        }

        expect:
        new CrawlableDatasetAmazonS3(parentDirUri, null, threddsS3Client).isCollection()
        new CrawlableDatasetAmazonS3(childDir2Uri, null, threddsS3Client).isCollection()
        !new CrawlableDatasetAmazonS3(dataset1Uri, null, threddsS3Client).isCollection()
    }

    def "listDatasets success"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            1 * listContents(parentDirUri) >> parentDirListing
            1 * listContents(childDir1Uri) >> childDir1Listing
            1 * listContents(childDir2Uri) >> childDir2Listing
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
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            1 * listContents(dataset1Uri) >> null
        }
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(dataset1Uri, null, threddsS3Client)

        when:
        dataset.listDatasets()

        then:
        IllegalStateException e = thrown()
        e.message == "'$dataset1Uri' is not a collection dataset."
    }

    def "length and lastModified non-existent S3URI"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client)
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(nonExistentUri, null, threddsS3Client)

        when:
        def length = dataset.length()
        def lastModified = dataset.lastModified()

        then:
        length == 0L

        and:
        lastModified == null
    }

    def "length and lastModified collection"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            2 * getMetadata(childDir1Uri) >> childDir1Metadata
        }
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(childDir1Uri, null, threddsS3Client)

        when:
        def length = dataset.length()
        def lastModified = dataset.lastModified()

        then:
        length == 0L

        and:
        lastModified == null
    }

    def "length and lastModified object"() {
        setup:
        ThreddsS3Client threddsS3Client = Mock(ThreddsS3Client) {
            2 * getMetadata(dataset1Uri) >> dataset1Metadata
        }
        CrawlableDataset dataset = new CrawlableDatasetAmazonS3(dataset1Uri, null, threddsS3Client)

        when:
        def length = dataset.length()
        def lastModified = dataset.lastModified()

        then:
        length == dataset1Length

        and:
        lastModified == dataset1LastModified
    }
}
