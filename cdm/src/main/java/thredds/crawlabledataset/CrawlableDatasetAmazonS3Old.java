package thredds.crawlabledataset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import org.apache.http.HttpStatus;

public class CrawlableDatasetAmazonS3Old extends CrawlableDatasetFile {
    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrawlableDatasetAmazonS3Old.class);

    private final String path;

    private static final String EHCACHE_S3_OBJECT_KEY = "S3Objects";
    private static final String EHCACHE_S3_OBJECT_METADATA_KEY = "S3ObjectMetadata";
    private static final String EHCACHE_S3_LISTING_KEY = "S3Listing";
    private static final int EHCACHE_MAX_OBJECTS = 1000;
    private static final int EHCACHE_TTL = 60;
    private static final int EHCACHE_TTI = 60;

    public CrawlableDatasetAmazonS3Old(String path, Object configObject) {
        super(path, configObject);
        this.path = path;
    }

    //////////////////////////////////////// Caching ////////////////////////////////////////

    public static Cache getS3ObjectMetadataCache() {
        return getS3Cache(EHCACHE_S3_OBJECT_METADATA_KEY);
    }

    public static Cache getS3ListingCache() {
        return getS3Cache(EHCACHE_S3_LISTING_KEY);
    }

    public static Cache getS3ObjectCache() {
        return getS3Cache(EHCACHE_S3_OBJECT_KEY, new S3CacheEventListener());
    }

    public static Cache getS3Cache(String cacheName) {
        return getS3Cache(cacheName, null);
    }

    public static Cache getS3Cache(String cacheName, CacheEventListener eventListener) {
        CacheManager cacheManager = CacheManager.create();

        if (!cacheManager.cacheExists(cacheName)) {
            Cache newCache = new Cache(cacheName, EHCACHE_MAX_OBJECTS, false, false, EHCACHE_TTL, EHCACHE_TTI);

            if (null != eventListener) {
                newCache.getCacheEventNotificationService().registerListener(eventListener);
            }

            cacheManager.addCache(newCache);
        }

        return cacheManager.getCache(cacheName);
    }

    //////////////////////////////////////// Instance methods ////////////////////////////////////////

    // Null means that the specified key does not exist in the bucket.
    private ThreddsS3Object getS3Object() {
        return S3Helper.getS3Metadata(path, getS3ObjectMetadataCache());
    }

    private List<ThreddsS3Object> getS3Listing() {
        return S3Helper.listS3Dir(path, getS3ListingCache());
    }



    //////////////////////////////////////// CrawlableDatasetFile ////////////////////////////////////////

    @Override
    public File getFile() {
        if (getS3Object() == null) {
            return null;
        } else {
            return S3Helper.getS3File(path, getS3ObjectCache());
        }
    }

    //////////////////////////////////////// CrawlableDataset ////////////////////////////////////////

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        return S3Helper.basename(path);
    }

    @Override
    public CrawlableDataset getParentDataset() {
        return new CrawlableDatasetAmazonS3Old(S3Helper.parent(path), getConfigObject());
    }

    @Override
    public boolean exists() {
        return getS3Object() != null || getS3Listing() != null;
    }

    @Override
    public boolean isCollection() {
        return !getS3Listing().isEmpty();
    }

    @Override
    public CrawlableDataset getDescendant(String relativePath) {
        if (relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Path must be relative <" + relativePath + ">.");
        }

        return new CrawlableDatasetAmazonS3Old(S3Helper.concat(path, relativePath), getConfigObject());
    }

    @Override
    public List<CrawlableDataset> listDatasets() throws IOException {
        if (!isCollection()) {
            String tmpMsg = String.format("'%s' is not a collection dataset.", path);
            log.error("listDatasets(): " + tmpMsg);
            throw new IllegalStateException(tmpMsg);
        }

        List<ThreddsS3Object> listing = S3Helper.listS3Dir(path, getS3ListingCache());
        assert !listing.isEmpty() : "In S3, virtual directories are never empty.";

        List<CrawlableDataset> list = new ArrayList<>();
        for (ThreddsS3Object s3Object : listing) {
            CrawlableDatasetAmazonS3Old crDs = new CrawlableDatasetAmazonS3Old(
                    S3Helper.concat(path, s3Object.key), getConfigObject());
            list.add(crDs);
        }

        return list;
    }

    @Override
    public long length() {
        ThreddsS3Object s3Object = getS3Object();
        if (s3Object != null) {
            return s3Object.size;
        } else {
            // "this" may be a collection or non-existent. In both cases, we return 0.
            return 0;
        }
    }

    @Override
    public Date lastModified() {
        ThreddsS3Object s3Object = getS3Object();
        if (s3Object != null) {
            return s3Object.lastModified;
        } else {
            // "this" may be a collection or non-existent. In both cases, we return null.
            return null;
        }
    }

    //////////////////////////////////////// Static Nested Classes ////////////////////////////////////////

    public static class ThreddsS3Object {
        public final String key;
        public final long size;
        public final Date lastModified;

        public enum Type {
            DIR,
            FILE
        }

        public final Type type;

        public ThreddsS3Object(String key, long size, Date lastModified, Type type) {
            this.key = key;
            this.size = size;
            this.lastModified = lastModified;
            this.type = type;
        }

        public ThreddsS3Object(String key, Type type) {
            this(key, -1, null, type);
        }
    }

    public static class S3Helper {
        private static String S3_PREFIX = "s3://";
        private static String S3_DELIMITER = "/";
        private static HashMap<String, File> fileStore = new HashMap<>();

        private static AmazonS3Client s3Client;

        public static String concat(String parent, String child) {
            if (child.isEmpty()) {
                return parent;
            } else {
                return parent + S3_DELIMITER + removeTrailingSlash(child);
            }
        }

        public static String parent(String uri) {
            int delim = uri.lastIndexOf(S3_DELIMITER);
            return uri.substring(0, delim);
        }

        public static String basename(String uri) {
            return new File(uri).getName();
        }

        public static String[] s3UriParts(String uri) {
            if (uri.startsWith(S3_PREFIX)) {
                uri = stripPrefix(uri, S3_PREFIX);
                String[] parts = new String[2];
                int delim = uri.indexOf(S3_DELIMITER);

                if (delim == -1) {  // Handle case where uri includes bucket but no key, e.g. "s3://bucket".
                    parts[0] = uri;
                    parts[1] = "";
                } else {
                    parts[0] = uri.substring(0, delim);
                    parts[1] = uri.substring(Math.min(delim + 1, uri.length()), uri.length());
                }

                return parts;
            } else {
                throw new IllegalArgumentException(String.format("Not a valid s3 uri: %s", uri));
            }
        }

        private static String stripPrefix(String key, String prefix) {
            return key.replaceFirst(prefix, "");
        }

        private static String removeTrailingSlash(String str) {
            if (str.endsWith(S3_DELIMITER)) {
                str = str.substring(0, str.length() - 1);
            }

            return str;
        }

        private static AmazonS3Client getS3Client() {
            if (s3Client == null) {
                // Use HTTP, it's much faster
                s3Client = new AmazonS3Client();
                s3Client.setEndpoint("http://s3.amazonaws.com");
                return s3Client;
            }

            return s3Client;
        }

        public static File createTempFile(String uri) throws IOException {
            // We have to save the key twice, as Ehcache will not provide
            // us with tmpFile when eviction happens, so we use fileStore
            Path tmpDir = Files.createTempDirectory("S3Download_");
            String fileBasename = basename(uri);
            File file = new File(tmpDir.toFile(), fileBasename);
            file.deleteOnExit();
            fileStore.put(uri, file);
            return file;
        }

        public static void deleteFileElement(Element element) {
            File file = fileStore.get(element.getObjectKey());
            if (null == file) {
                return;
            }

            // Should cleanup what createTempFile has created, meaning we have
            // to get rid of both the file and its containing directory
            file.delete();
            file.getParentFile().delete();

            fileStore.remove(element.getObjectKey());
        }

        public static ThreddsS3Object getS3Metadata(String uri, Cache cache) {
            Element element;
            if ((element = cache.get(uri)) != null) {
                return (ThreddsS3Object) element.getObjectValue();
            }

            String[] uriParts = s3UriParts(uri);
            String s3Bucket = uriParts[0];
            String s3Key = uriParts[1];

            try {
                ObjectMetadata metadata = getS3Client().getObjectMetadata(s3Bucket, s3Key);
                log.info(String.format("S3 Downloaded metadata '%s'", uri));

                ThreddsS3Object threddsS3Object = new ThreddsS3Object(
                        s3Key, metadata.getContentLength(), metadata.getLastModified(), ThreddsS3Object.Type.FILE);
                cache.put(new Element(uri, threddsS3Object));

                return threddsS3Object;
            } catch (AmazonServiceException e) {
                if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    log.info(String.format("S3 No such key in bucket: '%s'", uri));
                    return null;
                } else {
                    throw e;
                }
            }
        }

        public static List<ThreddsS3Object> listS3Dir(String uri, Cache cache) {
            Element element;
            if ((element = cache.get(uri)) != null) {
                return (List<ThreddsS3Object>) element.getObjectValue();
            }

            String[] uriParts = s3UriParts(uri);
            String s3Bucket = uriParts[0];
            String s3Key = uriParts[1];

            if (!s3Key.endsWith(S3_DELIMITER)) {
                s3Key += S3_DELIMITER;
            }

            final ListObjectsRequest listObjectsRequest =
                    new ListObjectsRequest().withBucketName(s3Bucket).withDelimiter(S3_DELIMITER);

            if (!s3Key.equals(S3_DELIMITER)) {
                // uri contains a bucket but no key, e.g. "s3://bucket".
                listObjectsRequest.setPrefix(s3Key);
            }

            final ObjectListing objectListing = getS3Client().listObjects(listObjectsRequest);
            log.info(String.format("S3 Downloaded listing '%s'", uri));

            List<ThreddsS3Object> listing = new ArrayList<>();

            for (final S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                listing.add(new ThreddsS3Object(
                        stripPrefix(objectSummary.getKey(), s3Key),
                        objectSummary.getSize(),
                        objectSummary.getLastModified(),
                        ThreddsS3Object.Type.FILE
                ));
            }

            for (final String commonPrefix : objectListing.getCommonPrefixes()) {
                String key = stripPrefix(commonPrefix, s3Key);
                key = removeTrailingSlash(key);

                listing.add(new ThreddsS3Object(key, ThreddsS3Object.Type.DIR));
            }

            if (listing.isEmpty()) {
                log.info(String.format("S3 Not a virtual directory: '%s'", uri));
            }

            cache.put(new Element(uri, listing));
            return listing;
        }

        public static File getS3File(String uri, Cache cache) {
            Element element;
            if ((element = cache.get(uri)) != null && ((File) element.getObjectValue()).exists()) {
                return (File) element.getObjectValue();
            }

            try {
                String[] uriParts = s3UriParts(uri);
                String s3Bucket = uriParts[0];
                String s3Key = uriParts[1];

                File tmpFile = createTempFile(uri);
                getS3Client().getObject(new GetObjectRequest(s3Bucket, s3Key), tmpFile);
                log.info(String.format("S3 Downloaded object '%s' to '%s'", uri, tmpFile.toString()));

                cache.put(new Element(uri, tmpFile));
                return tmpFile;
            } catch (Exception e) {
                log.error(String.format("S3 Error downloading object '%s'", uri));
                e.printStackTrace();
            }

            return null;
        }
    }

    public static class S3CacheEventListener extends CacheEventListenerAdapter {
        @Override
        public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
            S3Helper.deleteFileElement(element);
        }

        @Override
        public void notifyElementExpired(final Ehcache cache, final Element element) {
            S3Helper.deleteFileElement(element);
        }

        @Override
        public void notifyElementEvicted(Ehcache cache, Element element) {
            S3Helper.deleteFileElement(element);
        }
    }
}
