package thredds.crawlabledataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cwardgar
 * @since 2015/08/23
 */
public class CrawlableDatasetAmazonS3 extends CrawlableDatasetFile {
    private static final Logger logger = LoggerFactory.getLogger(CrawlableDatasetAmazonS3.class);

    private final String path;
    private final CachingThreddsS3Client threddsS3Client;

    public CrawlableDatasetAmazonS3(String path, Object configObject) {
        super(path, configObject);
        this.path = ThreddsS3Client.removeTrailingSlashes(path);
        this.threddsS3Client = new CachingThreddsS3Client(new ThreddsS3Client());
    }

    //////////////////////////////////////// CrawlableDatasetFile ////////////////////////////////////////

    @Override
    public File getFile() {
        try {
            File tempFile = ThreddsS3Client.createTempFile(path);
            return threddsS3Client.saveObjectToFile(path, tempFile);
        } catch (IOException e) {
            logger.error("Could not save S3 object to file.", e);
            return null;
        }
    }

    //////////////////////////////////////// CrawlableDataset ////////////////////////////////////////

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        return new File(path).getName();
    }

    @Override
    public CrawlableDataset getParentDataset() {
        return new CrawlableDatasetAmazonS3(ThreddsS3Client.parent(path), getConfigObject());
    }

    @Override
    public boolean exists() {
        return threddsS3Client.getObjectMetadata(path) != null || isCollection();
    }

    @Override
    public boolean isCollection() {
        ObjectListing listing = threddsS3Client.listObjects(path);
        return !listing.getCommonPrefixes().isEmpty() || !listing.getObjectSummaries().isEmpty();
    }

    @Override
    public CrawlableDataset getDescendant(String relativePath) {
        if (relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Path must be relative <" + relativePath + ">.");
        }

        return new CrawlableDatasetAmazonS3Old(ThreddsS3Client.concat(path, relativePath), getConfigObject());
    }

    @Override
    public List<CrawlableDataset> listDatasets() throws IOException {
        if (!isCollection()) {
            String tmpMsg = String.format("'%s' is not a collection dataset.", path);
            logger.error("listDatasets(): " + tmpMsg);
            throw new IllegalStateException(tmpMsg);
        }

        ObjectListing objectListing = threddsS3Client.listObjects(path);

//        List<ThreddsS3Object> listing = new ArrayList<>();
//
//        for (final S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
//            listing.add(new ThreddsS3Object(
//                    stripPrefix(objectSummary.getKey(), s3Key),
//                    objectSummary.getSize(),
//                    objectSummary.getLastModified(),
//                    ThreddsS3Object.Type.FILE
//            ));
//        }
//
//        for (final String commonPrefix : objectListing.getCommonPrefixes()) {
//            String key = stripPrefix(commonPrefix, s3Key);
//            key = removeTrailingSlash(key);
//
//            listing.add(new ThreddsS3Object(key, ThreddsS3Object.Type.DIR));
//        }

        List<CrawlableDataset> list = new ArrayList<>();
//        for (ThreddsS3Object s3Object : listing) {
//            CrawlableDatasetAmazonS3Old crDs = new CrawlableDatasetAmazonS3Old(
//                    S3Helper.concat(path, s3Object.key), getConfigObject());
//            list.add(crDs);
//        }
//
//        return list;

        ThreddsS3Client.S3Uri s3Uri = new ThreddsS3Client.S3Uri(path);

        for (final S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            String key = ThreddsS3Client.stripPrefix(objectSummary.getKey(), s3Uri.key);
            String listingPath = ThreddsS3Client.concat(path, key);

            CrawlableDatasetAmazonS3 crDs = new CrawlableDatasetAmazonS3(listingPath, getConfigObject());
            list.add(crDs);
        }

        for (String commonPrefix : objectListing.getCommonPrefixes()) {
            String prefix = s3Uri.key.endsWith(ThreddsS3Client.S3_DELIMITER) ?
                    s3Uri.key : s3Uri.key + ThreddsS3Client.S3_DELIMITER;
            String key = ThreddsS3Client.stripPrefix(commonPrefix, prefix);
            String listingPath = ThreddsS3Client.concat(path, key);

            CrawlableDatasetAmazonS3 crDs = new CrawlableDatasetAmazonS3(listingPath, getConfigObject());
            list.add(crDs);
        }

        assert !list.isEmpty() : "But this is a collection!";
        return list;
    }

    public static String makeS3Uri(String first, String last, String sep) {
        int numInCommon = 0;
        while (numInCommon < first.length() && numInCommon < last.length() &&
                first.charAt(first.length() - numInCommon - 1) == last.charAt(numInCommon)) {
            ++numInCommon;
        }

        return first.substring(0, first.length() - numInCommon) + sep + last.substring(numInCommon, last.length());
    }

    @Override
    public long length() {
//        ThreddsS3Object s3Object = getS3Object();
//        if (s3Object != null) {
//            return s3Object.size;
//        } else {
//            // "this" may be a collection or non-existent. In both cases, we return 0.
//            return 0;
//        }

        ObjectMetadata metadata = threddsS3Client.getObjectMetadata(path);
        if (metadata != null) {
            return metadata.getContentLength();
        } else {
            // "this" may be a collection or non-existent. In both cases, we return 0.
            return 0;
        }
    }

    @Override
    public Date lastModified() {
//        ThreddsS3Object s3Object = getS3Object();
//        if (s3Object != null) {
//            return s3Object.lastModified;
//        } else {
//            // "this" may be a collection or non-existent. In both cases, we return null.
//            return null;
//        }

        ObjectMetadata metadata = threddsS3Client.getObjectMetadata(path);
        if (metadata != null) {
            return metadata.getLastModified();
        } else {
            // "this" may be a collection or non-existent. In both cases, we return null.
            return null;
        }
    }
}
