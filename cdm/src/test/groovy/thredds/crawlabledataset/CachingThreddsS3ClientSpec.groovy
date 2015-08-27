package thredds.crawlabledataset

import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata

import spock.lang.Specification

/**
 * Tests the caching behavior of CachingThreddsS3Client.
 *
 * @author cwardgar
 * @since 2015/08/27
 */
class CachingThreddsS3ClientSpec extends Specification {
    // create a CachingThreddsS3Client that wraps our mock ThreddsS3Client
    ThreddsS3Client mockThreddsS3Client = Mock(ThreddsS3Client)
    ThreddsS3Client cachingThreddsS3Client = new CachingThreddsS3Client(mockThreddsS3Client)

    def "getObjectMetadata - existing key"() {
        setup: "create URI and mock return value"
        S3URI s3uri = new S3URI("s3://bucket/existing-key")
        ObjectMetadata mockObjectData = Mock(ObjectMetadata)

        when: "caching client's getObjectMetadata() is called multiple times"
        cachingThreddsS3Client.getObjectMetadata(s3uri)
        cachingThreddsS3Client.getObjectMetadata(s3uri)
        cachingThreddsS3Client.getObjectMetadata(s3uri)

        then: "mocking client's getObjectMetadata() is called exactly once. It is stubbed to return mockObjectData"
        1 * mockThreddsS3Client.getObjectMetadata(s3uri) >> mockObjectData

        and: "caching client is returning mockObjectData"
        cachingThreddsS3Client.getObjectMetadata(s3uri) is mockObjectData
    }

    def "getObjectMetadata - missing key"() {
        setup: "create URI"
        S3URI s3uri = new S3URI("s3://bucket/missing-key")

        when: "caching client's getObjectMetadata() is called multiple times"
        cachingThreddsS3Client.getObjectMetadata(s3uri)
        cachingThreddsS3Client.getObjectMetadata(s3uri)
        cachingThreddsS3Client.getObjectMetadata(s3uri)

        then: "mocking client's getObjectMetadata() is called exactly once. It is stubbed to return null"
        1 * mockThreddsS3Client.getObjectMetadata(s3uri) >> null

        and: "caching client is returning null"
        cachingThreddsS3Client.getObjectMetadata(s3uri) == null
    }

    def "listObjects - existing key"() {
        setup: "create URI and mock return value"
        S3URI s3uri = new S3URI("s3://bucket/existing-key")
        ObjectListing mockObjectListing = Mock(ObjectListing)

        when: "caching client's listObjects() is called multiple times"
        cachingThreddsS3Client.listObjects(s3uri)
        cachingThreddsS3Client.listObjects(s3uri)
        cachingThreddsS3Client.listObjects(s3uri)

        then: "mocking client's listObjects() is called exactly once. It is stubbed to return mockObjectListing"
        1 * mockThreddsS3Client.listObjects(s3uri) >> mockObjectListing

        and: "caching client is returning mockObjectListing"
        cachingThreddsS3Client.listObjects(s3uri) is mockObjectListing
    }

    def "listObjects - missing key"() {
        setup: "create URI"
        S3URI s3uri = new S3URI("s3://bucket/missing-key")

        when: "caching client's listObjects() is called multiple times"
        cachingThreddsS3Client.listObjects(s3uri)
        cachingThreddsS3Client.listObjects(s3uri)
        cachingThreddsS3Client.listObjects(s3uri)

        then: "mocking client's listObjects() is called exactly once. It is stubbed to return null"
        1 * mockThreddsS3Client.listObjects(s3uri) >> null

        and: "caching client is returning null"
        cachingThreddsS3Client.listObjects(s3uri) == null
    }

    // LOOK: Should I test what happens when I invalidate cache entries?
    // I'd first have to expose a method in CachingThreddsS3Client that performs that.
}
