package thredds.crawlabledataset.s3

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
    RemovalListener<S3URI, File> mockRemovalListener = Mock(RemovalListener)
    ThreddsS3Client cachingThreddsS3Client = new CachingThreddsS3Client(mockThreddsS3Client, mockRemovalListener)

    def "download - missing key"() {
        setup: "create URI"
        S3URI s3uri = new S3URI("s3://bucket/missing-key")

        when: "caching client's getLocalCopy() is called twice"
        cachingThreddsS3Client.getLocalCopy(s3uri)
        cachingThreddsS3Client.getLocalCopy(s3uri)

        then: "mocking client's getLocalCopy() is called exactly twice (null is not cached). It is stubbed to return null"
        2 * mockThreddsS3Client.getLocalCopy(s3uri) >> null

        and: "caching client is returning null"
        cachingThreddsS3Client.getLocalCopy(s3uri) == null
    }

    def "download - redownloading cached file"() {
        setup: "create URI"
        S3URI s3uri = new S3URI("s3://bucket/dataset.nc")
        File file = createTempFile s3uri

        when: "caching client's saveObjectToFile() is called twice"
        cachingThreddsS3Client.getLocalCopy(s3uri)
        cachingThreddsS3Client.getLocalCopy(s3uri)

        and: "file is deleted"
        Files.delete(file.toPath())

        and: "caching client's saveObjectToFile() is called twice more"
        cachingThreddsS3Client.getLocalCopy(s3uri)
        cachingThreddsS3Client.getLocalCopy(s3uri)

        then: "mocking client's getLocalCopy() is called exactly twice. It is stubbed to return file"
        2 * mockThreddsS3Client.getLocalCopy(s3uri) >> {  // This is a closure that generates return values.
            if (!file.exists()) {
                file.createNewFile()
            }
            file  // Last statement is the (implicit) value of the closure.
        }

        and: "entry for non-existent file was evicted"
        1 * mockRemovalListener.onRemoval({ it.getValue() == file })

        and: "caching client is returning file"
        cachingThreddsS3Client.getLocalCopy(s3uri) is file

        cleanup: "delete temp file"
        Files.delete(file.toPath())
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
        mockThreddsS3Client.getLocalCopy(_) >>> [file1, file2, file3]

        expect: "save objects to files, adding them to cache"
        cachingThreddsS3Client.getLocalCopy(s3uri1) == file1
        cachingThreddsS3Client.getLocalCopy(s3uri2) == file2
        cachingThreddsS3Client.getLocalCopy(s3uri3) == file3

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

    def "getMetadata"() {
        setup: "create URI and mock return value"
        S3URI s3uri = new S3URI("s3://bucket/existing-key")
        ThreddsS3Metadata mockMetadata = Mock(ThreddsS3Metadata)

        when: "caching client's getMetadata() is called twice"
        cachingThreddsS3Client.getMetadata(s3uri)
        cachingThreddsS3Client.getMetadata(s3uri)

        then: "mocking client's getMetadata() is called exactly once. It is stubbed to return mockMetadata"
        1 * mockThreddsS3Client.getMetadata(s3uri) >> mockMetadata

        and: "caching client is returning mockMetadata"
        cachingThreddsS3Client.getMetadata(s3uri) is mockMetadata
    }

    def "getMetadata - missing key"() {
        setup: "create URI"
        S3URI s3uri = new S3URI("s3://bucket/missing-key")

        when: "caching client's getMetadata() is called twice"
        cachingThreddsS3Client.getMetadata(s3uri)
        cachingThreddsS3Client.getMetadata(s3uri)

        then: "mocking client's getMetadata() is called exactly twice (null is not cached). It is stubbed to return null"
        2 * mockThreddsS3Client.getMetadata(s3uri) >> null

        and: "caching client is returning null"
        cachingThreddsS3Client.getMetadata(s3uri) == null
    }

    def "listContents"() {
        setup: "create URIs and mock return values"
        S3URI s3uri = new S3URI("s3://bucket/parent_dir")
        S3URI objectUri = new S3URI("s3://bucket/parent_dir/object")
        S3URI childDirUri = new S3URI("s3://bucket/parent_dir/child_dir")

        ThreddsS3Object mockObject = Mock(ThreddsS3Object) {
            getS3uri() >> objectUri
        }
        ThreddsS3Directory mockDirectory = Mock(ThreddsS3Directory) {
            getS3uri() >> childDirUri
        }
        ThreddsS3Listing mockListing = Mock(ThreddsS3Listing) {
            getContents() >> [mockDirectory, mockObject]
        }

        when: "caching client's listContents() is called twice"
        cachingThreddsS3Client.listContents(s3uri)
        cachingThreddsS3Client.listContents(s3uri)

        and: "caching client's getMetadata() is called once for each entry returned by listContents"
        cachingThreddsS3Client.getMetadata(objectUri)
        cachingThreddsS3Client.getMetadata(childDirUri)

        then: "mocking client's listContents() is called exactly once. It is stubbed to return mockListing"
        1 * mockThreddsS3Client.listContents(s3uri) >> mockListing

        and: "mocking client's getMetadata() is never called for the entries returned by listContents"
        0 * mockThreddsS3Client.getMetadata(*_)

        and: "caching client is returning mockListing, mockDirectory and mockObject"
        cachingThreddsS3Client.listContents(s3uri) is mockListing
        cachingThreddsS3Client.getMetadata(objectUri) is mockObject
        cachingThreddsS3Client.getMetadata(childDirUri) is mockDirectory
    }

    def "listContents - missing key"() {
        setup: "create URI"
        S3URI s3uri = new S3URI("s3://bucket/missing-key")

        when: "caching client's listContents() is called twice"
        cachingThreddsS3Client.listContents(s3uri)
        cachingThreddsS3Client.listContents(s3uri)

        then: "mocking client's listContents() is called exactly twice (null is not cached). It is stubbed to return null"
        2 * mockThreddsS3Client.listContents(s3uri) >> null

        and: "caching client is returning null"
        cachingThreddsS3Client.listContents(s3uri) == null
    }

}
