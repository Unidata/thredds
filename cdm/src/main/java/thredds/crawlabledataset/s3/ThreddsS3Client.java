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

    /**
     * Get a local copy of the object at the specified Amazon S3 URI.
     *
     * @param s3uri  the Amazon S3 URI of the object for which a local copy is to be returned.
     * @return  the file in which the the local copy of the Amazon S3 object's content is saved.
     *          Returns {@code null} if there is no S3 bucket with the specified name that has the specified key.
     * @throws IOException  if some I/O error occurred on the local filesystem while writing to file.
     */
    File getLocalCopy(S3URI s3uri) throws IOException;
}
