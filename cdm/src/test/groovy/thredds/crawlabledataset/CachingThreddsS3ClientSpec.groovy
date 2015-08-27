package thredds.crawlabledataset

import com.amazonaws.services.s3.model.ObjectMetadata
import spock.lang.Specification

/**
 * @author cwardgar
 * @since 2015/08/27
 */
class CachingThreddsS3ClientSpec extends Specification {
    def "cache hit"() {
        setup: "create a mock ThreddsS3Client whose getObjectMetadata() method returns a mock ObjectMetadata"
        S3URI s3uri = new S3URI("s3://bucket/key")
        ObjectMetadata mockObjectData = Mock(ObjectMetadata)
        ThreddsS3Client mockThreddsS3Client = Mock(ThreddsS3Client) {  // Why isn't this working?
            getObjectMetadata(_) >> mockObjectData
        }

        and: "create a CachingThreddsS3Client that wraps mockThreddsS3Client"
        ThreddsS3Client cachingThreddsS3Client = new CachingThreddsS3Client(mockThreddsS3Client)

        when: "getObjectMetadata() is called multiple times on the caching client"
        cachingThreddsS3Client.getObjectMetadata(s3uri)
        cachingThreddsS3Client.getObjectMetadata(s3uri)
        cachingThreddsS3Client.getObjectMetadata(s3uri)

        then: "only 1 getObjectMetadata() call happens on the underlying client"
        1 * mockThreddsS3Client.getObjectMetadata(s3uri)

        and: "caching client is returning our mock ObjectData"
        cachingThreddsS3Client.getObjectMetadata(s3uri) is mockObjectData
    }
}
