package thredds.crawlabledataset;

import java.io.File;

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

    private final AmazonS3Client s3Client;

    public ThreddsS3Client() {
        // Use HTTP, it's much faster
        s3Client = new AmazonS3Client();
        s3Client.setEndpoint("http://s3.amazonaws.com");
    }

    public ObjectMetadata getObjectMetadata(S3URI s3uri) {
        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(s3uri.getBucket(), s3uri.getKey());
            logger.info(String.format("S3 Downloaded metadata '%s'", s3uri));
            return metadata;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info(String.format("S3 No such key in bucket: '%s'", s3uri));
                return null;
            } else {
                throw e;
            }
        }
    }

    // Listing is limited to ~1000 results.
    // Will never be null.
    public ObjectListing listObjects(S3URI s3uri) {
        final ListObjectsRequest listObjectsRequest =
                new ListObjectsRequest().withBucketName(s3uri.getBucket()).withDelimiter(S3URI.S3_DELIMITER);

        if (s3uri.getKey() != null) {
            listObjectsRequest.setPrefix(s3uri.getKeyWithTrailingDelimiter());
        }

        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        logger.info(String.format("S3 Downloaded listing '%s'", s3uri));
        return objectListing;
    }

    public File saveObjectToFile(S3URI s3uri, File file) {
        try {
            s3Client.getObject(new GetObjectRequest(s3uri.getBucket(), s3uri.getKey()), file);
            logger.info(String.format("S3 Downloaded object '%s' to '%s'", s3uri, file));
            return file;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info(String.format("S3 No such key '%s' in bucket '%s'.", s3uri.getKey(), s3uri.getBucket()));
                return null;
            } else {
                throw e;
            }
        }
    }

    /////////////////////////////////////// Static util ///////////////////////////////////////

//    public static String basename(String uri) {
//        return new File(uri).getName();
//    }
//
//    public static String parent(String uri) {
//        int delim = uri.lastIndexOf(S3_DELIMITER);
//        return uri.substring(0, delim);
//    }
//
//    public static String concat(String parent, String child) {
//        if (child == null || child.isEmpty()) {
//            return parent;
//        } else {
//            return removeTrailingSlashes(parent) + S3_DELIMITER + removeTrailingSlashes(child);
//        }
//    }
//
//    public static String removeTrailingSlashes(String str) {
//        while (str.endsWith(S3_DELIMITER)) {
//            str = str.substring(0, str.length() - S3_DELIMITER.length());
//        }
//
//        return str;
//    }


    public static void main(String[] args) {
        String path1 = "s3://imos-data/IMOS/ANMN/AM/NRSYON/CO2/real-time/" +
                "IMOS_ANMN-AM_KST_20140709T033000Z_NRSYON_FV00_NRSYON-CO2-1407-realtime-raw_END-20140901T060000Z_C" +
                "-20150722T081042Z.nc";
        String path2 = "s3://imos-data/IMOS";
        ThreddsS3Client client = new ThreddsS3Client();

        ObjectMetadata metadata = client.getObjectMetadata(new S3URI(path1));
        if (metadata != null) {
            System.out.println(metadata.getLastModified());
        }

        ObjectListing listing = client.listObjects(new S3URI(path1));
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
