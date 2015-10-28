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

    def setup() {
        emptyMockObjectListing = Mock(ObjectListing) {
            getObjectSummaries() >> []
            getCommonPrefixes() >> []
        }

        nonEmptyMockObjectListing = Mock(ObjectListing) {
            getObjectSummaries() >> [Mock(S3ObjectSummary)]
            getCommonPrefixes() >> ['fake']
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
        threddsS3Client.listObjects(s3uri).commonPrefixes == ['fake']  // doesn't need a key.
        threddsS3Client.saveObjectToFile(s3uri, new File("some file")) == null
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
    }

    def "existent bucket and key"() {
        setup: "create mock ObjectMetadata"
        ObjectMetadata mockObjectMetadata = Mock(ObjectMetadata) {
            getContentType() >> 'fake'
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
    }
}
