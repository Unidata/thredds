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

    private static final long ENTRY_EXPIRATION_TIME = 1;  // In hours.
    private static final long MAX_METADATA_ENTRIES = 10000;
    private static final long MAX_FILE_ENTRIES = 100;

    private final LoadingCache<S3URI, Optional<ThreddsS3Metadata>> metadataCache;
    private final LoadingCache<S3URI, Optional<ThreddsS3Listing>> listingCache;
    private final LoadingCache<S3URI, Optional<File>> objectFileCache;

    public CachingThreddsS3Client(ThreddsS3Client threddsS3Client) {
        this(threddsS3Client, new ObjectFileCacheRemovalListener());
    }

    public CachingThreddsS3Client(
            final ThreddsS3Client threddsS3Client, RemovalListener<S3URI, Optional<File>> removalListener) {
        // We can't reuse the builder because each of the caches we're creating has different type parameters.
        this.metadataCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.HOURS)
                .maximumSize(MAX_METADATA_ENTRIES)
                .build(
                    new CacheLoader<S3URI, Optional<ThreddsS3Metadata>>() {
                        public Optional<ThreddsS3Metadata> load(S3URI s3uri) { // no checked exception
                            return Optional.fromNullable(threddsS3Client.getMetadata(s3uri));
                        }
                    }
                 );
        this.listingCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.HOURS)
                .maximumSize(MAX_METADATA_ENTRIES)
                .build(
                    new CacheLoader<S3URI, Optional<ThreddsS3Listing>>() {
                        public Optional<ThreddsS3Listing> load(S3URI s3uri) { // no checked exception
                            return Optional.fromNullable(threddsS3Client.listContents(s3uri));
                        }
                    }
                );
        this.objectFileCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.HOURS)
                .maximumSize(MAX_FILE_ENTRIES)
                .removalListener(removalListener)
                .build(
                    new CacheLoader<S3URI, Optional<File>>() {
                        public Optional<File> load(S3URI s3uri) throws IOException { // no checked exception
                            return Optional.fromNullable(threddsS3Client.getLocalCopy(s3uri));
                        }
                    }
                );
    }

    private static class ObjectFileCacheRemovalListener implements RemovalListener<S3URI, Optional<File>> {
        @Override
        public void onRemoval(RemovalNotification<S3URI, Optional<File>> notification) {
            Optional<File> file = notification.getValue();
            assert file != null : "Silence a silly IntelliJ warning. Of course the Optional isn't null.";

            if (file.isPresent()) {
                file.get().delete();
            }
        }
    }

    @Override
    public ThreddsS3Metadata getMetadata(S3URI s3uri) {
        try {
            return metadataCache.get(s3uri).orNull();
        } catch (ExecutionException e) {
            logger.error("Could not get metadata for %s", s3uri, e.getCause());
            return null;
        }
    }

    @Override
    public ThreddsS3Listing listContents(S3URI s3uri) {
        try {
            Optional<ThreddsS3Listing> listing = listingCache.get(s3uri);
            if (listing.isPresent()) {
                // add listing contents to metadataCache so we don't need to fetch them individually
                for (ThreddsS3Metadata metadata : listing.get().getContents()) {
                    metadataCache.put(metadata.getS3uri(), Optional.fromNullable(metadata));
                }
            }
            return listing.orNull();
        } catch (ExecutionException e) {
            logger.error("Could not get listing for %s", s3uri, e.getCause());
            return null;
        }
    }

    @Override
    public File getLocalCopy(S3URI s3uri) {
        try {
            File file = objectFileCache.get(s3uri).orNull();
            // check for deleted file
            if (file != null && !file.exists()) {
                objectFileCache.invalidate(s3uri);
                file = objectFileCache.get(s3uri).orNull();
            }
            return file;
        } catch (ExecutionException e) {
            logger.error("Could not get getLocalCopy for %s", s3uri, e.getCause());
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
