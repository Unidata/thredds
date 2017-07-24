package thredds.crawlabledataset.s3;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ThreddsS3Client that wraps another ThreddsS3Client and caches its methods' return values for efficiency.
 *
 * @author cwardgar
 * @since 2015/08/22
 */
public class CachingThreddsS3Client implements ThreddsS3Client {
    private static final Logger logger = LoggerFactory.getLogger(CachingThreddsS3Client.class);

    private static int entryExpirationTime = 5*60;  // In seconds.
    private static int maxMetadataEntries = 10000;
    private static int maxFileEntries = 100;

    private final LoadingCache<S3URI, ThreddsS3Metadata> metadataCache;
    private final LoadingCache<S3URI, ThreddsS3Listing> listingCache;
    private final LoadingCache<S3URI, File> objectFileCache;

    public static void setEntryExpirationTime(int i) {
        entryExpirationTime = i;
    };

    public static void setMaxMetadataEntries(int i) {
        maxMetadataEntries = i;
    }

    public static void setMaxFileEntries(int i) {
        maxFileEntries = i;
    }

    public CachingThreddsS3Client(ThreddsS3Client threddsS3Client) {
        this(threddsS3Client, new ObjectFileCacheRemovalListener());
    }

    public CachingThreddsS3Client(
            final ThreddsS3Client threddsS3Client, RemovalListener<S3URI, File> removalListener) {
        // We can't reuse the builder because each of the caches we're creating has different type parameters.
        this.metadataCache = CacheBuilder.newBuilder()
                .expireAfterWrite(entryExpirationTime, TimeUnit.SECONDS)
                .maximumSize(maxMetadataEntries)
                .build(
                    new CacheLoader<S3URI, ThreddsS3Metadata>() {
                        public ThreddsS3Metadata load(S3URI s3uri) throws UriNotFoundException {
                            ThreddsS3Metadata metadata = threddsS3Client.getMetadata(s3uri);
                            if (metadata == null) {
                                throw new UriNotFoundException(s3uri);
                            }
                            return metadata;
                        }
                    }
                 );
        this.listingCache = CacheBuilder.newBuilder()
                .expireAfterWrite(entryExpirationTime, TimeUnit.SECONDS)
                .maximumSize(maxMetadataEntries)
                .build(
                    new CacheLoader<S3URI, ThreddsS3Listing>() {
                        public ThreddsS3Listing load(S3URI s3uri) throws UriNotFoundException {
                            ThreddsS3Listing listing = threddsS3Client.listContents(s3uri);
                            if (listing == null) {
                                throw new UriNotFoundException(s3uri);
                            }
                            return listing;
                        }
                    }
                );
        this.objectFileCache = CacheBuilder.newBuilder()
                .expireAfterWrite(entryExpirationTime, TimeUnit.SECONDS)
                .maximumSize(maxFileEntries)
                .removalListener(removalListener)
                .build(
                    new CacheLoader<S3URI, File>() {
                        public File load(S3URI s3uri) throws IOException, UriNotFoundException {
                            File localCopy = threddsS3Client.getLocalCopy(s3uri);
                            if (localCopy == null) {
                                throw new UriNotFoundException(s3uri);
                            }
                            return localCopy;
                        }
                    }
                );
    }

    private static class ObjectFileCacheRemovalListener implements RemovalListener<S3URI, File> {
        @Override
        public void onRemoval(RemovalNotification<S3URI, File> notification) {
            File file = notification.getValue();

            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    public ThreddsS3Metadata getMetadata(S3URI s3uri) {
        try {
            return metadataCache.get(s3uri);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UriNotFoundException) {
                logger.debug("{} not found", s3uri);
            } else {
                logger.error("Could not get metadata for {}", s3uri, e.getCause());
            }
            return null;
        }
    }

    @Override
    public ThreddsS3Listing listContents(S3URI s3uri) {
        try {
            ThreddsS3Listing listing = listingCache.get(s3uri);
            // add listing contents to metadataCache so we don't need to fetch them individually
            for (ThreddsS3Metadata metadata : listing.getContents()) {
                metadataCache.put(metadata.getS3uri(), metadata);
            }
            return listing;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UriNotFoundException) {
                logger.debug("{} not found", s3uri);
            } else {
                logger.error("Could not get listing for {}", s3uri, e.getCause());
            }
            return null;
        }
    }

    @Override
    public File getLocalCopy(S3URI s3uri) {
        try {
            File file = objectFileCache.get(s3uri);
            // check for deleted file
            if (!file.exists()) {
                objectFileCache.invalidate(s3uri);
                file = objectFileCache.get(s3uri);
            }
            return file;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UriNotFoundException) {
                logger.debug("{} not found", s3uri);
            } else {
                logger.error("Could not get getLocalCopy for {}", s3uri, e.getCause());
            }
            return null;
        }
    }

    /**
     * Discards all entries from all caches. Any files created by {@link #getLocalCopy} will be deleted.
     */
    public void clear() {
        metadataCache.invalidateAll();
        listingCache.invalidateAll();
        objectFileCache.invalidateAll();
    }
}
