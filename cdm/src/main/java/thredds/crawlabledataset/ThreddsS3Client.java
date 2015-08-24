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
public class ThreddsS3Client {
    private static final Logger logger = LoggerFactory.getLogger(ThreddsS3Client.class);
    public static final String S3_PREFIX = "s3://";
    public static final String S3_DELIMITER = "/";

    private final AmazonS3Client s3Client;

    public ThreddsS3Client() {
        // Use HTTP, it's much faster
        s3Client = new AmazonS3Client();
        s3Client.setEndpoint("http://s3.amazonaws.com");
    }

    public ObjectMetadata getObjectMetadata(String uri) {
        S3Uri s3Uri = new S3Uri(uri);
        if (s3Uri.key.isEmpty()) {
            throw new IllegalArgumentException(String.format("S3 URI contains no key: '%s'", uri));
        }

        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(s3Uri.bucket, s3Uri.key);
            logger.info(String.format("S3 Downloaded metadata '%s'", uri));
            return metadata;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info(String.format("S3 No such key in bucket: '%s'", uri));
                return null;
            } else {
                throw e;
            }
        }
    }

    // Listing is limited to ~1000 results.
    // Will never be null.
    public ObjectListing listObjects(String uri) {
        S3Uri s3Uri = new S3Uri(uri);

        final ListObjectsRequest listObjectsRequest =
                new ListObjectsRequest().withBucketName(s3Uri.bucket).withDelimiter(S3_DELIMITER);

        if (!s3Uri.key.isEmpty()) {
            String prefix = s3Uri.key.endsWith(S3_DELIMITER) ? s3Uri.key : s3Uri.key + S3_DELIMITER;
            listObjectsRequest.setPrefix(prefix);
        }

        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        logger.info(String.format("S3 Downloaded listing '%s'", uri));
        return objectListing;
    }

    public File saveObjectToFile(String uri, File file) {
        S3Uri s3Uri = new S3Uri(uri);
        if (s3Uri.key.isEmpty()) {
            throw new IllegalArgumentException(String.format("S3 URI contains no key: '%s'", uri));
        }

        try {
            s3Client.getObject(new GetObjectRequest(s3Uri.bucket, s3Uri.key), file);
            logger.info(String.format("S3 Downloaded object '%s' to '%s'", uri, file));
            return file;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info(String.format("S3 No such key in bucket: '%s'", uri));
                return null;
            } else {
                throw e;
            }
        }
    }

    public static class S3Uri {
        public final String bucket, key;

        public S3Uri(String uri) {
            if (uri.startsWith(S3_PREFIX)) {
                uri = stripPrefix(uri, S3_PREFIX);
                int delim = uri.indexOf(S3_DELIMITER);

                if (delim == -1) {  // Handle case where uri includes bucket but no key, e.g. "s3://bucket".
                    this.bucket = uri;
                    this.key = "";
                } else {
                    this.bucket = uri.substring(0, delim);
                    this.key = uri.substring(Math.min(delim + 1, uri.length()), uri.length());
                }
            } else {
                throw new IllegalArgumentException(String.format("Not a valid s3 URI: '%s'", uri));
            }
        }
    }

    /////////////////////////////////////// Static util ///////////////////////////////////////

    public static File createTempFile(String uri) throws IOException {
        String fileBasename = basename(uri);
        File file = Files.createTempFile("S3Object", fileBasename).toFile();
        file.deleteOnExit();
        return file;
    }

    public static String stripPrefix(String key, String prefix) {
        return key.replaceFirst(prefix, "");
    }

    public static String basename(String uri) {
        return new File(uri).getName();
    }

    public static String parent(String uri) {
        int delim = uri.lastIndexOf(S3_DELIMITER);
        return uri.substring(0, delim);
    }

    public static String concat(String parent, String child) {
        if (child == null || child.isEmpty()) {
            return parent;
        } else {
            return removeTrailingSlashes(parent) + S3_DELIMITER + removeTrailingSlashes(child);
        }
    }

    public static String removeTrailingSlashes(String str) {
        while (str.endsWith(S3_DELIMITER)) {
            str = str.substring(0, str.length() - S3_DELIMITER.length());
        }

        return str;
    }


    public static void main(String[] args) {
        String path1 = "s3://imos-data/IMOS/ANMN/AM/NRSYON/CO2/real-time/" +
                "IMOS_ANMN-AM_KST_20140709T033000Z_NRSYON_FV00_NRSYON-CO2-1407-realtime-raw_END-20140901T060000Z_C" +
                "-20150722T081042Z.nc";
        String path2 = "s3://imos-data/IMOS";
        ThreddsS3Client client = new ThreddsS3Client();

        ObjectMetadata metadata = client.getObjectMetadata(path1);
        if (metadata != null) {
            System.out.println(metadata.getLastModified());
        }

        ObjectListing listing = client.listObjects(path1);
        if (listing != null) {
            System.out.println("--------------Common Prefixes--------------");
            for (String prefix : listing.getCommonPrefixes()) {
                System.out.println("\t" + prefix);
            }

            System.out.println("--------------Object Summaries--------------");
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                System.out.println("\t" + summary.getKey());
            }
        }
    }
}
