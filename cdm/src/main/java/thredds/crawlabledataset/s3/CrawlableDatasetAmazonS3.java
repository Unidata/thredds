package thredds.crawlabledataset.s3;

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
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;

/**
 * CrawlableDataset implementation that allows THREDDS to interact with datasets stored on Amazon S3.
 *
 * @author danfruehauf
 * @author cwardgar
 * @since 2015/08/23
 */
public class CrawlableDatasetAmazonS3 extends CrawlableDatasetFile {
    private static final Logger logger = LoggerFactory.getLogger(CrawlableDatasetAmazonS3.class);
    private static ThreddsS3Client defaultThreddsS3Client;

    private final S3URI s3uri;
    private final ThreddsS3Client threddsS3Client;

    public CrawlableDatasetAmazonS3(String path, Object configObject) {  // Used reflectively by CrawlableDatasetFactory
        this(new S3URI(path), configObject);
    }

    public CrawlableDatasetAmazonS3(S3URI s3uri, Object configObject) {
        this(s3uri, configObject, getDefaultThreddsS3Client());
    }

    public CrawlableDatasetAmazonS3(S3URI s3uri, Object configObject, ThreddsS3Client threddsS3Client) {
        super(s3uri.toString(), configObject);
        this.s3uri = s3uri;
        this.threddsS3Client = threddsS3Client;
    }

    //////////////////////////////////////// Static ////////////////////////////////////////

    public static ThreddsS3Client getDefaultThreddsS3Client() {
        if (defaultThreddsS3Client == null) {
            defaultThreddsS3Client = new CachingThreddsS3Client(new ThreddsS3ClientImpl());
        }
        return defaultThreddsS3Client;
    }

    public static void setDefaultThreddsS3Client(ThreddsS3Client threddsS3Client) {
        defaultThreddsS3Client = threddsS3Client;
    }

    //////////////////////////////////////// Instance ////////////////////////////////////////

    public S3URI getS3URI() {
        return s3uri;
    }

    //////////////////////////////////////// CrawlableDatasetFile ////////////////////////////////////////

    @Override
    public File getFile() {
        try {
            return threddsS3Client.saveObjectToFile(s3uri, s3uri.createTempFile());
        } catch (IOException e) {
            logger.error(String.format("Could not save S3 object '%s' to file.", s3uri), e);
            return null;
        }
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
        return threddsS3Client.listObjects(s3uri) != null;
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
        assert objectListing != null : "We checked this in the isCollection() call above.";
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

    /**
     * Returns the size of the dataset, in bytes. Will be zero if this dataset is a collection or non-existent.
     *
     * @return the size of the dataset
     */
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

    /**
     * Returns the date that the dataset was last modified. Will be null if the dataset is a collection or non-existent.
     *
     * @return the date that the dataset was last modified
     */
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
