package thredds.crawlabledataset.s3

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.apache.http.HttpStatus
import spock.lang.Specification

/**
 * Tests that ThreddsS3ClientImpl implements the contract of ThreddsS3Client, particularly with respect to unhappy
 * code paths. Makes heavy use of mocking to avoid actually connecting to Amazon S3.
 * <p>
 * TODO: These unit tests are nice and fast, but we need integration tests too. For that, we'll need an AWS instance
 * under our control that we can host test data on.
 *
 * @author cwardgar
 * @since 2015/08/26
 */
class ThreddsS3ClientImplSpec extends Specification {
    ObjectListing emptyMockObjectListing
    ObjectListing nonEmptyMockObjectListing
    AmazonServiceException amazonServiceException

    def setupSpec() {
        AmazonS3ClientOptions clientOptions = new AmazonS3ClientOptions()
        clientOptions.setMaxListingPages(2)
        ThreddsS3ClientImpl.setClientOptions(clientOptions)
    }

    def setup() {
        emptyMockObjectListing = Mock(ObjectListing) {
            getObjectSummaries() >> []
            getCommonPrefixes() >> []
            isTruncated() >> false
        }

        nonEmptyMockObjectListing = Mock(ObjectListing) {
            getObjectSummaries() >> [Mock(S3ObjectSummary)]
            getCommonPrefixes() >> ['fake/']
            isTruncated() >> false
        }

        // Create exception that stubbed methods will throw.
        amazonServiceException = new AmazonServiceException("error")
        amazonServiceException.setStatusCode(HttpStatus.SC_NOT_FOUND)
    }

    def "null key"() {
        setup: "create mock to avoid actually connecting to S3"
        AmazonS3Client amazonS3Client = Mock(AmazonS3Client) {
            // This is the behavior of the actual AmazonS3Client for these 3 methods when key is null.
            getObjectMetadata(*_) >> { throw new IllegalArgumentException("null key") }
            listObjects(*_) >> nonEmptyMockObjectListing
            getObject(*_) >> { throw new IllegalArgumentException("null key") }
        }

        and: "create ThreddsS3Client that uses the mock AmazonS3Client"
        ThreddsS3Client threddsS3Client = new ThreddsS3ClientImpl(amazonS3Client)

        and: "create URI with null key (not that it matters with the mocking we've done)"
        S3URI s3uri = new S3URI("s3://imos-data")

        expect: "null key"
        threddsS3Client.getObjectMetadata(s3uri) == null
        threddsS3Client.listObjects(s3uri).commonPrefixes == ['fake/']  // doesn't need a key.
        threddsS3Client.saveObjectToFile(s3uri, new File("some file")) == null
        threddsS3Client.getMetadata(s3uri) == threddsS3Directory(s3uri)
        threddsS3Client.listContents(s3uri) == threddsS3Listing(s3uri, ['fake/'], [])
    }

    def "non-existent bucket"() {
        setup: "create mock to avoid actually connecting to S3"
        AmazonS3Client amazonS3Client = Mock(AmazonS3Client) {
            // This is the behavior of the actual AmazonS3Client for these 3 methods when bucket is non-existent.
            getObjectMetadata(*_) >> { throw amazonServiceException }
            listObjects(*_) >> { throw amazonServiceException }
            getObject(*_) >> { throw amazonServiceException }
        }

        and: "create ThreddsS3Client that uses the mock AmazonS3Client"
        ThreddsS3Client threddsS3Client = new ThreddsS3ClientImpl(amazonS3Client)

        and: "create URI with non-existent bucket (not that it matters with the mocking we've done)"
        S3URI s3uri = new S3URI("s3://non-existent-bucket/blah")

        expect: "non-existent bucket"
        threddsS3Client.getObjectMetadata(s3uri) == null
        threddsS3Client.listObjects(s3uri) == null
        threddsS3Client.saveObjectToFile(s3uri, new File("some file")) == null
        threddsS3Client.getMetadata(s3uri) == null
        threddsS3Client.listContents(s3uri) == null
    }

    def "non-existent key"() {
        setup: "create mock to avoid actually connecting to S3"
        AmazonS3Client amazonS3Client = Mock(AmazonS3Client) {
            // This is the behavior of the actual AmazonS3Client for these 3 methods when key is non-existent.
            getObjectMetadata(*_) >> { throw amazonServiceException }
            listObjects(*_) >> emptyMockObjectListing
            getObject(*_) >> { throw amazonServiceException }
        }

        and: "create ThreddsS3Client that uses the mock AmazonS3Client"
        ThreddsS3Client threddsS3Client = new ThreddsS3ClientImpl(amazonS3Client)

        and: "create URI with non-existent key (not that it matters with the mocking we've done)"
        S3URI s3uri = new S3URI("s3://imos-data/non-existent-key")

        expect: "non-existent key"
        threddsS3Client.getObjectMetadata(s3uri) == null
        threddsS3Client.listObjects(s3uri) == null
        threddsS3Client.saveObjectToFile(s3uri, new File("some file")) == null
        threddsS3Client.getMetadata(s3uri) == null
        threddsS3Client.listContents(s3uri) == null
    }

    def "existent bucket and key"() {
        setup: "create mock ObjectMetadata"
        ObjectMetadata mockObjectMetadata = Mock(ObjectMetadata) {
            getContentType() >> 'fake'
            getContentLength() >> 873264L
            getLastModified() >> new Date(1941, 11, 7)
        }

        and: "create mock to avoid actually connecting to S3"
        AmazonS3Client amazonS3Client = Mock(AmazonS3Client) {
            // This is the behavior of the actual AmazonS3Client for these 3 methods when bucket and key exist.
            getObjectMetadata(*_) >> mockObjectMetadata  // Non-null ObjectMetadata
            listObjects(*_) >> emptyMockObjectListing    // Empty ObjectListing
            getObject(*_) >> mockObjectMetadata          // Non-null ObjectMetadata
        }

        and: "create ThreddsS3Client that uses the mock AmazonS3Client"
        ThreddsS3Client threddsS3Client = new ThreddsS3ClientImpl(amazonS3Client)

        and: "create URI with existent bucket and key (not that it matters with the mocking we've done)"
        S3URI s3uri = new S3URI("s3://bucket/dataset.nc")

        expect: "existent bucket and key"
        threddsS3Client.getObjectMetadata(s3uri).contentType == 'fake'
        threddsS3Client.listObjects(s3uri) == null
        threddsS3Client.saveObjectToFile(s3uri, s3uri.getTempFile()).name.endsWith('dataset.nc')
        threddsS3Client.getMetadata(s3uri) == threddsS3Object(s3uri, new Date(1941, 11, 7), 873264L)
        threddsS3Client.listContents(s3uri) == null
    }

    def "key is common prefix"() {
        setup: "create mock object listing for common prefix"
        ObjectListing commonPrefixListing = Mock(ObjectListing) {
            getObjectSummaries() >> [
                mockSummary("imos-data", 'parent_dir/some_object.nc', new Date(1941, 11, 7), 9879871L)
            ]
            getCommonPrefixes() >> ['parent_dir/fake/']
        }

        and: "create mock to avoid actually connecting to S3"
        AmazonS3Client amazonS3Client = Mock(AmazonS3Client) {
            // mock actual amazonS3Client interaction for this scenario
            listObjects(*_) >> commonPrefixListing
        }

        and: "create ThreddsS3Client that uses the mock AmazonS3Client"
        ThreddsS3Client threddsS3Client = new ThreddsS3ClientImpl(amazonS3Client)

        and: "create URI with non-existent key (not that it matters with the mocking we've done)"
        S3URI s3uri = new S3URI("s3://imos-data/parent_dir")

        expect: "key is common prefix"
        threddsS3Client.listContents(s3uri) == threddsS3Listing(s3uri, ['parent_dir/fake/'],
            [threddsS3Object('s3://imos-data/parent_dir/some_object.nc', new Date(1941, 11, 7), 9879871L)])
    }

    def "multiple page common prefix"() {
        setup: "create mock object listings for each page"
        ObjectListing commonPrefixListingPage1 = Mock(ObjectListing) {
            getObjectSummaries() >> [
                    mockSummary("imos-data", 'parent_dir/some_object.nc', new Date(1941, 11, 7), 9879871L)
            ]
            getCommonPrefixes() >> ['parent_dir/fake/']
            isTruncated() >> true
        }

        ObjectListing commonPrefixListingPage2 = Mock(ObjectListing) {
            getObjectSummaries() >> [
                    mockSummary("imos-data", 'parent_dir/some_object_2.nc', new Date(1941, 11, 8), 98771L)
            ]
            getCommonPrefixes() >> []
            isTruncated() >> false
        }

        and: "create mock to avoid actually connecting to S3"
        AmazonS3Client amazonS3Client = Mock(AmazonS3Client) {
            // mock actual amazonS3Client interaction for this scenario
            listObjects(*_) >> commonPrefixListingPage1
            listNextBatchOfObjects(*_) >> commonPrefixListingPage2
        }

        and: "create ThreddsS3Client that uses the mock AmazonS3Client"
        ThreddsS3Client threddsS3Client = new ThreddsS3ClientImpl(amazonS3Client)

        and: "create URI with non-existent key (not that it matters with the mocking we've done)"
        S3URI s3uri = new S3URI("s3://imos-data/parent_dir")

        expect: "all pages returned"
        threddsS3Client.listContents(s3uri) == threddsS3Listing(s3uri, ['parent_dir/fake/'], [
            threddsS3Object('s3://imos-data/parent_dir/some_object.nc', new Date(1941, 11, 7), 9879871L),
            threddsS3Object('s3://imos-data/parent_dir/some_object_2.nc', new Date(1941, 11, 8), 98771L)
        ])
    }

    def "multiple page common prefix exceeding maxListingPages"() {
        setup: "create mock object listings for each page"
        ObjectListing commonPrefixListingPage1 = Mock(ObjectListing) {
            getObjectSummaries() >> [
                mockSummary("imos-data", 'parent_dir/some_object.nc', new Date(1941, 11, 7), 9879871L)
            ]
            getCommonPrefixes() >> ['parent_dir/fake/']
            isTruncated() >> true
        }

        ObjectListing commonPrefixListingPage2 = Mock(ObjectListing) {
            getObjectSummaries() >> [
                mockSummary("imos-data", 'parent_dir/some_object_2.nc', new Date(1941, 11, 8), 98771L)
            ]
            getCommonPrefixes() >> []
            isTruncated() >> true
        }

        ObjectListing commonPrefixListingPage3 = Mock(ObjectListing) {
            getObjectSummaries() >> [
                mockSummary("imos-data",'parent_dir/some_object_3.nc', new Date(1941, 11, 9), 987771L)
            ]
            getCommonPrefixes() >> []
            isTruncated() >> false
        }

        and: "create mock to avoid actually connecting to S3"
        AmazonS3Client amazonS3Client = Mock(AmazonS3Client) {
            // mock actual amazon s3 interactions
            listObjects(*_) >> commonPrefixListingPage1
            listNextBatchOfObjects(commonPrefixListingPage1) >> commonPrefixListingPage2
            listNextBatchOfObjects(commonPrefixListingPage2) >> commonPrefixListingPage3
        }

        and: "create ThreddsS3Client that uses the mock AmazonS3Client"
        ThreddsS3Client threddsS3Client = new ThreddsS3ClientImpl(amazonS3Client)

        and: "create URI for common prefix"
        S3URI s3uri = new S3URI("s3://imos-data/parent_dir")

        expect: "only pages up to maxListingPages returned"
        threddsS3Client.listContents(s3uri) == threddsS3Listing(s3uri, ['parent_dir/fake/'], [
                threddsS3Object('s3://imos-data/parent_dir/some_object.nc', new Date(1941, 11, 7), 9879871L),
                threddsS3Object('s3://imos-data/parent_dir/some_object_2.nc', new Date(1941, 11, 8), 98771L)
        ])
    }

    /////////////////////////// Helper Methods (improve readability) /////////////////////////

    private S3ObjectSummary mockSummary(String bucket, String key, Date date, long size) {
        Mock(S3ObjectSummary) {
            getBucketName() >> bucket
            getKey() >> key
            getLastModified() >> date
            getSize() >> size
        }
    }

    private ThreddsS3Directory threddsS3Directory(S3URI s3URI) {
        new ThreddsS3Directory(s3URI)
    }

    private ThreddsS3Object threddsS3Object(S3URI s3URI, Date date, long l) {
        new ThreddsS3Object(s3URI, date, l)
    }

    private ThreddsS3Object threddsS3Object(String s3URI, Date date, long l) {
        threddsS3Object(new S3URI(s3URI), date, l)
    }

    private ThreddsS3Listing  threddsS3Listing(S3URI s3URI, List<String> commonPrefixes, List<ThreddsS3Object> objects) {
        ThreddsS3Listing listing = new ThreddsS3Listing(s3URI)

        for (String commonPrefix: commonPrefixes) {
            S3URI childUri = new S3URI(s3URI.getBucket(), commonPrefix)
            listing.add(new ThreddsS3Directory(childUri))
        }

        for (ThreddsS3Object object: objects) {
            listing.add(object)
        }

        return listing
    }

}
