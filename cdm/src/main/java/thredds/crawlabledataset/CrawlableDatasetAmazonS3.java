package thredds.crawlabledataset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    private final S3URI s3uri;
    private final CachingThreddsS3Client threddsS3Client;

    public CrawlableDatasetAmazonS3(String path, Object configObject) {
        this(new S3URI(path), configObject);
    }

    public CrawlableDatasetAmazonS3(S3URI s3uri, Object configObject) {
        super(s3uri.toString(), configObject);
        this.s3uri = s3uri;
        this.threddsS3Client = new CachingThreddsS3Client(new ThreddsS3Client());
    }

    //////////////////////////////////////// CrawlableDatasetFile ////////////////////////////////////////

    @Override
    public File getFile() {
        try {
            File tempFile = createTempFile(s3uri);
            return threddsS3Client.saveObjectToFile(s3uri, tempFile);
        } catch (IOException e) {
            logger.error(String.format("Could not save S3 object '%s' to file.", s3uri), e);
            return null;
        }
    }

    public static File createTempFile(S3URI s3uri) throws IOException {
        File file = Files.createTempFile("S3Object", s3uri.getBaseName()).toFile();
        file.deleteOnExit();
        return file;
    }

    //////////////////////////////////////// CrawlableDataset ////////////////////////////////////////

    @Override
    public String getPath() {
        return s3uri.toString();
    }

    @Override
    public String getName() {
        return s3uri.getBaseName();
    }

    @Override
    public CrawlableDataset getParentDataset() {
        return new CrawlableDatasetAmazonS3(s3uri.getParent(), getConfigObject());
    }

    @Override
    public boolean exists() {
        return threddsS3Client.getObjectMetadata(s3uri) != null || isCollection();
    }

    @Override
    public boolean isCollection() {
        ObjectListing listing = threddsS3Client.listObjects(s3uri);
        return !listing.getCommonPrefixes().isEmpty() || !listing.getObjectSummaries().isEmpty();
    }

    @Override
    public CrawlableDataset getDescendant(String relativePath) {
        return new CrawlableDatasetAmazonS3(s3uri.getChild(relativePath), getConfigObject());
    }

    @Override
    public List<CrawlableDataset> listDatasets() throws IOException {
        if (!isCollection()) {
            String tmpMsg = String.format("'%s' is not a collection dataset.", s3uri);
            logger.error("listDatasets(): " + tmpMsg);
            throw new IllegalStateException(tmpMsg);
        }

        ObjectListing objectListing = threddsS3Client.listObjects(s3uri);
        List<CrawlableDataset> list = new ArrayList<>();

        for (final S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            S3URI childS3uri = new S3URI(objectSummary.getBucketName(), objectSummary.getKey());
            CrawlableDatasetAmazonS3 crawlableDset = new CrawlableDatasetAmazonS3(childS3uri, getConfigObject());
            list.add(crawlableDset);
        }

        for (String commonPrefix : objectListing.getCommonPrefixes()) {
            S3URI childS3uri = new S3URI(s3uri.getBucket(), commonPrefix);
            CrawlableDatasetAmazonS3 crawlableDset = new CrawlableDatasetAmazonS3(childS3uri, getConfigObject());
            list.add(crawlableDset);
        }

        assert !list.isEmpty() : "This is a collection and collections shouldn't be empty.";
        return list;
    }

    @Override
    public long length() {
        ObjectMetadata metadata = threddsS3Client.getObjectMetadata(s3uri);
        if (metadata != null) {
            return metadata.getContentLength();
        } else {
            // "this" may be a collection or non-existent. In both cases, we return 0.
            return 0;
        }
    }

    @Override
    public Date lastModified() {
        ObjectMetadata metadata = threddsS3Client.getObjectMetadata(s3uri);
        if (metadata != null) {
            return metadata.getLastModified();
        } else {
            // "this" may be a collection or non-existent. In both cases, we return null.
            return null;
        }
    }
}
