package thredds.crawlabledataset.s3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    private static ThreddsS3Client defaultThreddsS3Client = new CachingThreddsS3Client(new ThreddsS3ClientImpl());

    private final S3URI s3uri;
    private final ThreddsS3Client threddsS3Client;

    // A configObject is required by the superclass constructor (CrawlableDatasetFile).
    // However, it is ignored; in fact, a warning is emitted if it ISN'T null.
    // So, for convenience, we're providing constructors without the configObject parameter.

    public CrawlableDatasetAmazonS3(String path) {
        this(path, null);
    }

    public CrawlableDatasetAmazonS3(String path, Object configObject) {  // Used reflectively by CrawlableDatasetFactory
        this(new S3URI(path), configObject);
    }

    public CrawlableDatasetAmazonS3(S3URI s3uri) {
        this(s3uri, null);
    }

    public CrawlableDatasetAmazonS3(S3URI s3uri, Object configObject) {
        this(s3uri, configObject, defaultThreddsS3Client);
    }

    public CrawlableDatasetAmazonS3(S3URI s3uri, Object configObject, ThreddsS3Client threddsS3Client) {
        super(s3uri.toString(), configObject);
        this.s3uri = s3uri;
        this.threddsS3Client = threddsS3Client;
    }

    //////////////////////////////////////// Static ////////////////////////////////////////

    public static void setDefaultThreddsS3Client(ThreddsS3Client threddsS3Client) {
        defaultThreddsS3Client = threddsS3Client;
    }

    //////////////////////////////////////// Getters ////////////////////////////////////////

    public S3URI getS3URI() {
        return s3uri;
    }

    public ThreddsS3Client getThreddsS3Client() {
        return threddsS3Client;
    }

    //////////////////////////////////////// CrawlableDatasetFile ////////////////////////////////////////

    @Override
    public File getFile() {
        try {
            return threddsS3Client.saveObjectToFile(s3uri, s3uri.getTempFile());
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
        return new CrawlableDatasetAmazonS3(s3uri.getParent(), getConfigObject(), threddsS3Client);
    }

    @Override
    public CrawlableDataset getDescendant(String relativePath) {
        return new CrawlableDatasetAmazonS3(s3uri.getChild(relativePath), getConfigObject(), threddsS3Client);
    }

    @Override
    public boolean exists() {
        return threddsS3Client.getMetadata(s3uri) != null;
    }

    @Override
    public boolean isCollection() {
        return threddsS3Client.getMetadata(s3uri) instanceof ThreddsS3Directory;
    }

    @Override
    public List<CrawlableDataset> listDatasets() throws IOException {
        ThreddsS3Listing listing = threddsS3Client.listContents(s3uri);

        if (listing == null) {
            String tmpMsg = String.format("'%s' is not a collection dataset.", s3uri);
            logger.error("listContents(): " + tmpMsg);
            throw new IllegalStateException(tmpMsg);
        }

        List<CrawlableDataset> crawlableDsets = new ArrayList<>();

        for (final ThreddsS3Metadata metadata: listing.getContents()) {
            CrawlableDatasetAmazonS3 crawlableDset =
                    new CrawlableDatasetAmazonS3(metadata.getS3uri(), getConfigObject(), threddsS3Client);
            crawlableDsets.add(crawlableDset);

        }

        return crawlableDsets;
    }

    /**
     * Returns the size of the dataset, in bytes. Will be zero if this dataset is a collection or non-existent.
     *
     * @return the size of the dataset
     */
    @Override
    public long length() {
        ThreddsS3Metadata metadata = threddsS3Client.getMetadata(s3uri);

        if (metadata instanceof ThreddsS3Object) {
            return ((ThreddsS3Object)metadata).getLength();
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
        ThreddsS3Metadata metadata = threddsS3Client.getMetadata(s3uri);

        if (metadata instanceof ThreddsS3Object) {
            return ((ThreddsS3Object)metadata).getLastModified();
        } else {
            // "this" may be a collection or non-existent. In both cases, we return null.
            return null;
        }
    }

    //////////////////////////////////////// Object ////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("CrawlableDatasetAmazonS3{'%s'}", s3uri);
    }

    // Not considering threddsS3Client in either of these becaue ThreddsS3Client doesn't implement equals or hashCode.
    // It's very hard to provide those methods because ThreddsS3Client contains a AmazonS3Client data member, and that
    // class doesn't implement those methods either.
    // Still, it's probably okay because we almost always only care about the S3URI.

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        }

        CrawlableDatasetAmazonS3 that = (CrawlableDatasetAmazonS3) other;
        return Objects.equals(this.getS3URI(), that.getS3URI()) &&
               Objects.equals(this.getConfigObject(), that.getConfigObject());
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] { this.getS3URI(), this.getConfigObject() });
    }
}
