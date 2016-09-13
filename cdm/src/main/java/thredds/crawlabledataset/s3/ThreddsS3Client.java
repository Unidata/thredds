package thredds.crawlabledataset.s3;

import java.io.File;
import java.io.IOException;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * Provides an interface for accessing the Amazon S3 web service. It is a basic version of the
 * {@link com.amazonaws.services.s3.AmazonS3} interface that is much simpler to use from within THREDDS.
 *
 * @author cwardgar
 * @since 2015/08/26
 */
public interface ThreddsS3Client {
    /**
     * Gets the metadata for the specified Amazon S3 object without actually fetching the object itself.
     * The object metadata contains information such as content type, content disposition, etc.,
     * as well as custom user metadata that can be associated with an object in Amazon S3.
     *
     * @param s3uri  the Amazon S3 URI of the object whose metadata is being retrieved.
     * @return all Amazon S3 object metadata for the specified object.
     *         Will be {@code null} if there is no S3 bucket with the specified name that has the specified key.
     */
    ObjectMetadata getObjectMetadata(S3URI s3uri);

    /**
     * Returns a listing of the objects located in the "virtual filesystem" at the specified URI.
     * {@code s3uri.getKey()} is interpreted as a path, and only objects (accessible via
     * {@link ObjectListing#getObjectSummaries}) or "directories" (accessible via
     * {@link ObjectListing#getCommonPrefixes}) that have that prefix will be part of the returned listing.
     * <p>
     * NOTE: To manage large result sets, Amazon S3 uses pagination to split them into multiple responses.
     * As a consequence, this listing will only include the first 1000-or-so results.
     * <p>
     * TODO: Extend this API so that it allows the retrieval of more than 1000 results.
     *
     * @param s3uri  the Amazon S3 URI of the "virtual directory" whose objects are being retrieved.
     * @return  a listing of objects. Will be {@code null} if the specified URI does not denote an existing virtual
     *          directory. Otherwise, the result will be non-null and non-empty. That is, it will have at least one
     *          object summary or at least one common prefix.
     * @see  com.amazonaws.services.s3.AmazonS3#listObjects(String)
     */
    ObjectListing listObjects(S3URI s3uri);

    /**
     * Gets the object metadata for the object stored in Amazon S3 under the specified bucket and key,
     * and saves the object contents to the specified file.
     *
     * @param s3uri  the Amazon S3 URI of the object whose content is being saved.
     * @param file  indicates the file (which might already exist) where to save the object content being downloading
     *              from Amazon S3.
     * @return  the file in which the Amazon S3 object's content was saved.
     *          Returns {@code null} if there is no S3 bucket with the specified name that has the specified key.
     * @throws IOException  if some I/O error occurred on the local filesystem while writing to file.
     */

    File saveObjectToFile(S3URI s3uri, File file) throws IOException;

    /**
     * Returns metadata for the "virtual directory" or S3 object at the specified Amazon S3 URI.
     *
     * @param s3uri  the Amazon S3 URI of the "virtual directory" or S3 object whose metadata is being retrieved.
     * @return metadata for the "virtual directory" or S3 object at the specified Amazon S3 URI.
     *         Will be {@code null} if there is no S3 object or "virtual directory" at the specified Amazon S3 URI.
     */
    ThreddsS3Metadata getMetadata(S3URI s3uri);

    /**
     * Returns a listing of the "virtual directories" and S3 objects contained in the "virtual directory" at the
     * specified Amazon S3 URI.
     *
     * @param s3uri  the Amazon S3 URI of the "virtual directory" whose contents are to be listed.
     * @return a listing of the "virtual directories" and/or S3 objects at the specified Amazon S3 URI.
     *         Will be {@code null} if there is no "virtual directory" at the specified Amazon S3 URI.
     */
    ThreddsS3Listing listContents(S3URI s3uri);

}
