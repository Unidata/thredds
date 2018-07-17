package thredds.crawlabledataset.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A basic implementation of {@link ThreddsS3Client}.
 *
 * @author cwardgar
 * @since 2015/08/22
 */
public class ThreddsS3ClientImpl implements ThreddsS3Client {
    private static final Logger logger = LoggerFactory.getLogger(ThreddsS3ClientImpl.class);

    private static String s3ServiceEndpoint = "http://s3.amazonaws.com";  // http is much faster
    private static int maxListingPages = Integer.MAX_VALUE;
    private static int connectTimeOut = 50*1000; //Default sdk timeout
    private static int socketTimeOut = 50*1000;  //Default sdk timeout

    private final AmazonS3Client s3Client;

    public static void setS3ServiceEndpoint(String s3ServiceEndpoint) {
        ThreddsS3ClientImpl.s3ServiceEndpoint = s3ServiceEndpoint;
    }

    public static void setMaxListingPages(int i) {
        maxListingPages = i;
    };

    public static void setConnectTimeOut(int connectTimeOut) {
        ThreddsS3ClientImpl.connectTimeOut = connectTimeOut;
    }

    public static void setSocketTimeOut(int socketTimeOut) {
        ThreddsS3ClientImpl.socketTimeOut = socketTimeOut;
    }

    public ThreddsS3ClientImpl() {
        ClientConfiguration clientConfiguration = new ClientConfiguration()
            .withConnectionTimeout(connectTimeOut)
            .withSocketTimeout(socketTimeOut);
        s3Client = new AmazonS3Client(defaultCredentialsProvider(), clientConfiguration);
        s3Client.setEndpoint(s3ServiceEndpoint);
    }

    public ThreddsS3ClientImpl(AmazonS3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public ThreddsS3Metadata getMetadata(S3URI s3uri) {
        ObjectMetadata metadata = getObjectMetadata(s3uri);

        if (metadata != null) {
            return new ThreddsS3Object(s3uri, metadata.getLastModified(), metadata.getContentLength());
        } else if (isDirectory(s3uri)) {
            return new ThreddsS3Directory(s3uri);
        } else {
            return null;
        }
    }

    @Override
    public ThreddsS3Listing listContents(S3URI s3uri) {
        ThreddsS3Listing listing = new ThreddsS3Listing(s3uri);
        ObjectListing page = listObjects(s3uri);

        if (page == null) {
            return null;
        }

        listing.add(page);
        int pageNo = 2;

        while (page.isTruncated() && pageNo <= maxListingPages) {
            page = s3Client.listNextBatchOfObjects(page);
            logger.info(String.format("Downloaded page %d of S3 listing '%s'", pageNo, s3uri));
            listing.add(page);
            pageNo++;
        }

        if (page.isTruncated()) {
            logger.warn(
                    String.format("Maximum number of S3 listing pages (%d) exceeded. " +
                            "Not all content for %s retrieved", maxListingPages, s3uri));
        }

        return listing;
    }

    @Override
    public File getLocalCopy(S3URI s3uri) throws IOException {
        return saveObjectToFile(s3uri, s3uri.getTempFile());
    }

    private AWSCredentialsProvider defaultCredentialsProvider() {
        return new DefaultAWSCredentialsProviderChain() {
            public AWSCredentials getCredentials() {
                try {
                    return super.getCredentials();
                } catch (AmazonClientException var2) {
                    logger.debug("No credentials available; falling back to anonymous access");
                    return null;
                }
            }
        };
    }

    private ObjectMetadata getObjectMetadata(S3URI s3uri) {
        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(s3uri.getBucket(), s3uri.getKey());
            logger.info(String.format("Downloaded S3 metadata '%s'", s3uri));
            return metadata;
        } catch (IllegalArgumentException e) {  // Thrown by getObjectMetadata() when key == null.
            logger.debug(e.getMessage());
            return null;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.debug(String.format(
                        "There is no S3 bucket '%s' that has key '%s'.", s3uri.getBucket(), s3uri.getKey()));
                return null;
            } else {
                throw e;
            }
        }
    }

    private ObjectListing listObjects(S3URI s3uri) {
        ListObjectsRequest listObjectsRequest =
                new ListObjectsRequest().withBucketName(s3uri.getBucket()).withDelimiter(S3URI.S3_DELIMITER);

        if (s3uri.getKey() != null) {
            listObjectsRequest.setPrefix(s3uri.getKeyWithTrailingDelimiter());
        }

        try {
            ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
            logger.info(String.format("Downloaded S3 listing '%s'", s3uri));

            // On S3 it is possible for a prefix to be a valid object (unlike a
            // filesystem where a node is either a file OR a directory). We
            // exclude self here so that the "directory" isn't treated as a file
            // by some of the caching mechanism.
            S3ObjectSummary self = null;
            for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
                if (Objects.equals(objectSummary.getKey(), s3uri.getKeyWithTrailingDelimiter())) {
                    self = objectSummary;
                    break;
                }
            }
            objectListing.getObjectSummaries().remove(self);

            // S3 Objects may contain a double slash in the key
            // similar to an empty folder in a path. Thredds can't handle
            // these so any key containing a double-slash needs to be exluded
            List<String> commonPrefixesCopy = new ArrayList<String>(objectListing.getCommonPrefixes());

            for (String commonPrefix: commonPrefixesCopy) {
                if (commonPrefix.contains("//")) {
                    objectListing.getCommonPrefixes().remove(commonPrefix);
                }
            }

            if (objectListing.getObjectSummaries().isEmpty() && objectListing.getCommonPrefixes().isEmpty()) {
                // There are no empty directories in a S3 hierarchy.
                logger.debug(String.format("In bucket '%s', the key '%s' does not denote an existing virtual directory.",
                        s3uri.getBucket(), s3uri.getKey()));
                return null;
            } else {
                return objectListing;
            }
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.debug(String.format("No S3 bucket named '%s' exists.", s3uri.getBucket()));
                return null;
            } else {
                throw e;
            }
        }
    }

    private File saveObjectToFile(S3URI s3uri, File file) throws IOException {
        try {
            s3Client.getObject(new GetObjectRequest(s3uri.getBucket(), s3uri.getKey()), file);
            logger.info(String.format("Downloaded S3 object '%s' to '%s'", s3uri, file));
            file.deleteOnExit();
            return file;
        } catch (IllegalArgumentException e) {  // Thrown by getObject() when key == null.
            logger.debug(e.getMessage());
            return null;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.debug(String.format(
                        "There is no S3 bucket '%s' that has key '%s'.", s3uri.getBucket(), s3uri.getKey()));
                return null;
            } else {
                throw e;
            }
        }
    }

    private boolean isDirectory(S3URI s3uri) {
        try {
            ListObjectsRequest listObjectsRequest =
                    new ListObjectsRequest().withBucketName(s3uri.getBucket()).withDelimiter(S3URI.S3_DELIMITER);

            listObjectsRequest.setPrefix(s3uri.getKeyWithTrailingDelimiter());
            listObjectsRequest.setMaxKeys(1);

            ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

            return !objectListing.getCommonPrefixes().isEmpty() || !objectListing.getObjectSummaries().isEmpty();
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.debug(String.format("There is no S3 bucket '%s'.", s3uri.getBucket()));
                return false;
            } else {
                throw e;
            }
        }
    }

}
