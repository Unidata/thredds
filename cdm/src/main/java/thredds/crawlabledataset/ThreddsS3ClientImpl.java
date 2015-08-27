package thredds.crawlabledataset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cwardgar
 * @since 2015/08/22
 */
public class ThreddsS3ClientImpl implements ThreddsS3Client {
    private static final Logger logger = LoggerFactory.getLogger(ThreddsS3ClientImpl.class);

    private final AmazonS3Client s3Client;

    public ThreddsS3ClientImpl() {
        // Use HTTP, it's much faster
        s3Client = new AmazonS3Client();
        s3Client.setEndpoint("http://s3.amazonaws.com");
    }

    @Override
    public ObjectMetadata getObjectMetadata(S3URI s3uri) {
        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(s3uri.getBucket(), s3uri.getKey());
            logger.info(String.format("S3 Downloaded metadata '%s'", s3uri));
            return metadata;
        } catch (IllegalArgumentException e) {  // Thrown by getObjectMetadata() when key == null.
            logger.info(e.getMessage());
            return null;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info(String.format(
                        "There is no S3 bucket '%s' that has key '%s'.", s3uri.getBucket(), s3uri.getKey()), e);
                return null;
            } else {
                throw e;
            }
        }
    }

    // Listing is limited to ~1000 results.
    @Override
    public ObjectListing listObjects(S3URI s3uri) {
        final ListObjectsRequest listObjectsRequest =
                new ListObjectsRequest().withBucketName(s3uri.getBucket()).withDelimiter(S3URI.S3_DELIMITER);

        if (s3uri.getKey() != null) {
            listObjectsRequest.setPrefix(s3uri.getKeyWithTrailingDelimiter());
        }

        try {
            ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
            logger.info(String.format("S3 Downloaded listing '%s'", s3uri));

            if (objectListing.getObjectSummaries().isEmpty() && objectListing.getCommonPrefixes().isEmpty()) {
                // There are no empty directories in a S3 hierarchy.
                logger.info(String.format("In bucket '%s', the key '%s' does not denote an existing virtual directory.",
                        s3uri.getBucket(), s3uri.getKey()));
                return null;
            } else {
                return objectListing;
            }
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info(String.format("No S3 bucket named '%s' exists.", s3uri.getBucket()), e);
                return null;
            } else {
                throw e;
            }
        }
    }

    @Override
    public File saveObjectToFile(S3URI s3uri, File file) throws IOException {
        try {
            s3Client.getObject(new GetObjectRequest(s3uri.getBucket(), s3uri.getKey()), file);
            logger.info(String.format("S3 Downloaded object '%s' to '%s'", s3uri, file));
            return file;
        } catch (IllegalArgumentException e) {  // Thrown by getObject() when key == null.
            logger.info(e.getMessage());
            return null;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info(String.format(
                        "There is no S3 bucket '%s' that has key '%s'.", s3uri.getBucket(), s3uri.getKey()), e);
                return null;
            } else {
                throw e;
            }
        }
    }

    public static File createTempFile(S3URI s3uri) throws IOException {
        File file = Files.createTempFile("S3Object", s3uri.getBaseName()).toFile();
        file.deleteOnExit();
        return file;
    }


    public static void main(String[] args) throws IOException {
        String path1 = "s3://imos-data";                    // no key
        String path2 = "s3://non-existent-bucket/blah";     // non-existent bucket
        String path3 = "s3://imos-data/non-existent-key";   // non-existent key
        String path4 = "s3://imos-data/IMOS/ACORN/radial";  // directory path, but not an actual existent key.
        String path5 = "s3://imos-data/index.html";
        String path6 = "s3://imos-data/IMOS/ANMN/AM/NRSYON/CO2/real-time/" +
                "IMOS_ANMN-AM_KST_20140709T033000Z_NRSYON_FV00_NRSYON-CO2-1407-realtime-raw_END-20140901T060000Z_C" +
                "-20150722T081042Z.nc";

        S3URI s3uri = new S3URI(path4);
        ThreddsS3ClientImpl client = new ThreddsS3ClientImpl();

//        System.out.println("--------------Metadata--------------");
//        ObjectMetadata metadata = client.getObjectMetadata(s3uri);
//        if (metadata != null) {
//            System.out.println("\t" + metadata.getLastModified());
//        } else {
//            System.out.println("\t" + "null");
//        }

        System.out.println("--------------Listing--------------");
        ObjectListing listing = client.listObjects(s3uri);
        if (listing != null) {
            System.out.println("\t--------------Common Prefixes--------------");
            for (String prefix : listing.getCommonPrefixes()) {
                System.out.println("\t\t" + prefix);
            }

            System.out.println("\t--------------Object Summaries--------------");
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                System.out.println("\t\t" + summary.getKey());
            }
        } else {
            System.out.println("\t" + "null");
        }

//        System.out.println("--------------File--------------");
//        File file = client.saveObjectToFile(s3uri, createTempFile(s3uri));
//        if (file != null) {
//            System.out.println(file.toString());
//        } else {
//            System.out.println("\t" + "null");
//        }
    }
}
