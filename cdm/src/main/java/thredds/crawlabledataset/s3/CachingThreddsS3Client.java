package thredds.crawlabledataset.s3;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.io.Files;
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

    private final ThreddsS3Client threddsS3Client;

    private final Cache<S3URI, Optional<ObjectMetadata>> objectMetadataCache;
    private final Cache<S3URI, Optional<ObjectListing>> objectListingCache;
    private final Cache<S3URI, Optional<File>> objectFileCache;
    private final Cache<S3URI, Optional<ThreddsS3Metadata>> metadataCache;
    private final Cache<S3URI, Optional<ThreddsS3Listing>> listingCache;

    public CachingThreddsS3Client(ThreddsS3Client threddsS3Client) {
        this(threddsS3Client, new ObjectFileCacheRemovalListener());
    }

    public CachingThreddsS3Client(
            ThreddsS3Client threddsS3Client, RemovalListener<S3URI, Optional<File>> removalListener) {
        this.threddsS3Client = threddsS3Client;

        // We can't reuse the builder because each of the caches we're creating has different type parameters.
        this.objectMetadataCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.HOURS)
                .maximumSize(MAX_METADATA_ENTRIES)
                .build();
        this.objectListingCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.HOURS)
                .maximumSize(MAX_METADATA_ENTRIES)
                .build();
        this.objectFileCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.HOURS)
                .maximumSize(MAX_FILE_ENTRIES)
                .removalListener(removalListener)
                .build();
        this.metadataCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.HOURS)
                .maximumSize(MAX_METADATA_ENTRIES)
                .build();
        this.listingCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.HOURS)
                .maximumSize(MAX_METADATA_ENTRIES)
                .build();
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
    public ObjectMetadata getObjectMetadata(S3URI s3uri) {
        Optional<ObjectMetadata> metadata = objectMetadataCache.getIfPresent(s3uri);
        if (metadata == null) {
            logger.debug(String.format("ObjectMetadata cache MISS: '%s'", s3uri));
            metadata = Optional.fromNullable(threddsS3Client.getObjectMetadata(s3uri));
            objectMetadataCache.put(s3uri, metadata);
        } else {
            logger.debug(String.format("ObjectMetadata cache hit: '%s'", s3uri));
        }

        return metadata.orNull();
    }

    @Override
    public ObjectListing listObjects(S3URI s3uri) {
        Optional<ObjectListing> objectListing = objectListingCache.getIfPresent(s3uri);
        if (objectListing == null) {
            logger.debug(String.format("ObjectListing cache MISS: '%s'", s3uri));
            objectListing = Optional.fromNullable(threddsS3Client.listObjects(s3uri));
            objectListingCache.put(s3uri, objectListing);
        } else {
            logger.debug(String.format("ObjectListing cache hit: '%s'", s3uri));
        }

        return objectListing.orNull();
    }

    @Override
    public ThreddsS3Metadata getMetadata(S3URI s3uri) {
        Optional<ThreddsS3Metadata> metadata = metadataCache.getIfPresent(s3uri);
        if (metadata == null) {
            logger.debug(String.format("ThreddsS3Metadata cache MISS: '%s'", s3uri));
            metadata = Optional.fromNullable(threddsS3Client.getMetadata(s3uri));
            metadataCache.put(s3uri, metadata);
        } else {
            logger.debug(String.format("ThreddsS3Metadata cache hit: '%s'", s3uri));
        }

        return metadata.orNull();
    }

    @Override
    public ThreddsS3Listing listContents(S3URI s3uri) {
        Optional<ThreddsS3Listing> listing = listingCache.getIfPresent(s3uri);
        if (listing == null) {
            logger.debug(String.format("ThreddsS3Listing cache MISS: '%s'", s3uri));
            listing = Optional.fromNullable(threddsS3Client.listContents(s3uri));
            listingCache.put(s3uri, listing);
            if (listing.isPresent()) {
                // add contents of listing to metadata cache as well so additional requests
                // do not need to be made to get them later
                for (ThreddsS3Metadata metadata : listing.get().getContents()) {
                    metadataCache.put(metadata.getS3uri(), Optional.fromNullable(metadata));
                }
            }
        } else {
            logger.debug(String.format("ThreddsS3Listing cache hit: '%s'", s3uri));
        }

        return listing.orNull();
    }

    /**
     * {@inheritDoc}
     * <p>
     * WARNING: If the content at {@code s3uri} was previously saved using this method, and the old file to which it was
     * saved is <b>not</b> the same as {@code file}, the object content will be copied to the new file and the old file
     * will be deleted.
     */
    @Override
    public File saveObjectToFile(S3URI s3uri, File file) throws IOException {
        Optional<File> cachedFile = objectFileCache.getIfPresent(s3uri);

        if (cachedFile == null) {
            logger.debug("Object cache MISS: '%s'", s3uri);
            // Do download below.
        } else {
            logger.debug("Object cache hit: '%s'", s3uri);

            if (!cachedFile.isPresent()) {
                return null;
            } else if (!cachedFile.get().exists()) {
                logger.info(String.format("Found cache entry {'%s'-->'%s'}, but local file doesn't exist. " +
                        "Was it deleted? Re-downloading.", s3uri, cachedFile.get()));
                objectFileCache.invalidate(s3uri);  // Evict old entry. Re-download below.
            } else if (!cachedFile.get().equals(file)) {
                // Copy content of cachedFile to file. Evict cachedFile from the cache.
                Files.copy(cachedFile.get(), file);
                objectFileCache.put(s3uri, Optional.of(file));
                return file;
            } else {
                return file;  // File already contains the content of the object at s3uri.
            }
        }

        cachedFile = Optional.fromNullable(threddsS3Client.saveObjectToFile(s3uri, file));
        objectFileCache.put(s3uri, cachedFile);
        return cachedFile.orNull();
    }

    /**
     * Discards all entries from all caches. Any files created by {@link #saveObjectToFile} will be deleted.
     */
    public void clear() {
        objectMetadataCache.invalidateAll();
        objectListingCache.invalidateAll();
        objectFileCache.invalidateAll();
        metadataCache.invalidateAll();
        listingCache.invalidateAll();
    }
}
