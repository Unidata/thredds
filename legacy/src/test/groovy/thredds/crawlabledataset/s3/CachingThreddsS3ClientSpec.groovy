package thredds.crawlabledataset.s3

import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.common.base.Optional
import com.google.common.cache.RemovalListener
import spock.lang.Specification

import java.nio.file.Files
/**
 * Tests the caching behavior of CachingThreddsS3Client.
 *
 * @author cwardgar
 * @since 2015/08/27
 */
class CachingThreddsS3ClientSpec extends Specification {
    // create a CachingThreddsS3Client that wraps our mock ThreddsS3Client
    ThreddsS3Client mockThreddsS3Client = Mock(ThreddsS3Client)
    RemovalListener<S3URI, Optional<File>> mockRemovalListener = Mock(RemovalListener)
    ThreddsS3Client cachingThreddsS3Client = new CachingThreddsS3Client(mockThreddsS3Client, mockRemovalListener)

    def "getObjectMetadata"() {
        setup: "create URI and mock return value"
        S3URI s3uri = new S3URI("s3://bucket/existing-key")
        ObjectMetadata mockObjectData = Mock(ObjectMetadata)

        when: "caching client's getObjectMetadata() is called twice"
        cachingThreddsS3Client.getObjectMetadata(s3uri)
        cachingThreddsS3Client.getObjectMetadata(s3uri)

        then: "mocking client's getObjectMetadata() is called exactly once. It is stubbed to return mockObjectData"
        1 * mockThreddsS3Client.getObjectMetadata(s3uri) >> mockObjectData

        and: "caching client is returning mockObjectData"
        cachingThreddsS3Client.getObjectMetadata(s3uri) is mockObjectData
    }

    def "listObjects"() {
        setup: "create URI and mock return value"
        S3URI s3uri = new S3URI("s3://bucket/existing-key")
        ObjectListing mockObjectListing = Mock(ObjectListing)

        when: "caching client's listObjects() is called twice"
        cachingThreddsS3Client.listObjects(s3uri)
        cachingThreddsS3Client.listObjects(s3uri)

        then: "mocking client's listObjects() is called exactly once. It is stubbed to return mockObjectListing"
        1 * mockThreddsS3Client.listObjects(s3uri) >> mockObjectListing

        and: "caching client is returning mockObjectListing"
        cachingThreddsS3Client.listObjects(s3uri) is mockObjectListing
    }

    def "saveObjectToFile - missing key"() {
        setup: "create URI"
        S3URI s3uri = new S3URI("s3://bucket/missing-key")
        File file = s3uri.getTempFile();

        when: "caching client's saveObjectToFile() is called twice"
        cachingThreddsS3Client.saveObjectToFile(s3uri, file)
        cachingThreddsS3Client.saveObjectToFile(s3uri, file)

        then: "mocking client's saveObjectToFile() is called exactly once. It is stubbed to return null"
        1 * mockThreddsS3Client.saveObjectToFile(s3uri, file) >> null

        and: "caching client is returning null"
        cachingThreddsS3Client.saveObjectToFile(s3uri, file) == null

        cleanup: "delete temp file"
        file?.delete()
    }

    def "saveObjectToFile - redownloading cached file"() {
        setup: "create URI and File"
        S3URI s3uri = new S3URI("s3://bucket/dataset.nc")
        File file = createTempFile s3uri

        when: "caching client's saveObjectToFile() is called twice"
        cachingThreddsS3Client.saveObjectToFile(s3uri, file)
        cachingThreddsS3Client.saveObjectToFile(s3uri, file)

        and: "the saved file is deleted"
        Files.delete(file.toPath())

        and: "caching client's saveObjectToFile() is called twice more"
        cachingThreddsS3Client.saveObjectToFile(s3uri, file)
        cachingThreddsS3Client.saveObjectToFile(s3uri, file)

        then: "mocking client's saveObjectToFile() is called exactly twice. It is stubbed to return file"
        2 * mockThreddsS3Client.saveObjectToFile(s3uri, file) >> {  // This is a closure that generates return values.
            if (!file.exists()) {
                // Before the 2nd call to this method, the file will have been deleted. We must re-create.
                file.createNewFile()
            }
            file  // Last statement is the (implicit) value of the closure.
        }

        and: "entry for non-existent file was evicted"
        1 * mockRemovalListener.onRemoval({ it.getValue().get() == file })

        and: "caching client is returning file"
        cachingThreddsS3Client.saveObjectToFile(s3uri, file) is file

        cleanup: "delete temp file"
        Files.delete(file.toPath())
    }

    def "saveObjectToFile - download object to 2 different files"() {
        setup: "create URI and Files"
        S3URI s3uri = new S3URI("s3://bucket/dataset.nc")
        File file1 = File.createTempFile("file1", ".nc")
        File file2 = File.createTempFile("file2", ".nc")

        when: "caching client's saveObjectToFile() is called once with file1 and once with file2"
        cachingThreddsS3Client.saveObjectToFile(s3uri, file1)
        cachingThreddsS3Client.saveObjectToFile(s3uri, file2)

        then: "mocking client's saveObjectToFile() is called exactly once. It's stubbed to return file1"
        1 * mockThreddsS3Client.saveObjectToFile(s3uri, file1) >> file1

        and: "old entry for s3uri was evicted"
        1 * mockRemovalListener.onRemoval({ it.getKey() == s3uri && it.getValue().get() == file1 })

        and: "caching client is returning file2"
        cachingThreddsS3Client.saveObjectToFile(s3uri, file2) is file2

        cleanup: "delete temp files"
        Files.delete(file1.toPath())
        Files.delete(file2.toPath())
    }

    def "clear"() {
        setup: "create caching client that uses real RemovalListener"
        cachingThreddsS3Client = new CachingThreddsS3Client(mockThreddsS3Client)  // Deletes files evicted from cache.

        and: "create URIs"
        S3URI s3uri1 = new S3URI("s3://bucket/dataset1.nc")
        S3URI s3uri2 = new S3URI("s3://bucket/dataset2.nc")
        S3URI s3uri3 = new S3URI("s3://bucket/dataset3.nc")

        and: "create temp files"
        File file1 = createTempFile s3uri1
        File file2 = createTempFile s3uri2
        File file3 = createTempFile s3uri3

        and: "mocking client's saveObjectToFile() is stubbed to return file1, file2, and file3 in order"
        mockThreddsS3Client.saveObjectToFile(_, _) >>> [file1, file2, file3]

        expect: "save objects to files, adding them to cache"
        cachingThreddsS3Client.saveObjectToFile(s3uri1, file1) == file1
        cachingThreddsS3Client.saveObjectToFile(s3uri2, file2) == file2
        cachingThreddsS3Client.saveObjectToFile(s3uri3, file3) == file3

        and: "files exist"
        file1.exists()
        file2.exists()
        file3.exists()

        when: "cache is cleared"
        cachingThreddsS3Client.clear()

        then: "files no longer exist"
        !file1.exists()
        !file2.exists()
        !file3.exists()
    }

    File createTempFile(S3URI s3URI) {
        File file = s3URI.tempFile
        file.parentFile.mkdirs()
        file.createNewFile()
        file
    }
}
