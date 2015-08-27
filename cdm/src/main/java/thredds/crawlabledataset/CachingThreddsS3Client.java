package thredds.crawlabledataset;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.io.Files;

/**
 * @author cwardgar
 * @since 2015/08/22
 */
public class CachingThreddsS3Client implements ThreddsS3Client {
    private static final long ENTRY_EXPIRATION_TIME = 60 * 10;  // In seconds.
    private static final long MAX_ENTRIES = 100;

    private final ThreddsS3Client threddsS3Client;

    private final Cache<S3URI, ObjectMetadata> objectMetadataCache;
    private final Cache<S3URI, ObjectListing> objectListingCache;
    private final Cache<S3URI, File> objectFileCache;

    public CachingThreddsS3Client(ThreddsS3Client threddsS3Client) {
        this.threddsS3Client = threddsS3Client;

        // We can't reuse the builder because each of the caches we're creating has different type parameters.
        this.objectMetadataCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.SECONDS)
                .maximumSize(MAX_ENTRIES)
                .build();
        this.objectListingCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.SECONDS)
                .maximumSize(MAX_ENTRIES)
                .build();
        this.objectFileCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ENTRY_EXPIRATION_TIME, TimeUnit.SECONDS)
                .maximumSize(MAX_ENTRIES)
                .removalListener(new ObjectFileCacheRemovalListener())
                .build();
    }

    private static class ObjectFileCacheRemovalListener implements RemovalListener<S3URI, File> {
        @Override
        public void onRemoval(RemovalNotification<S3URI, File> notification) {
            notification.getValue().delete();
        }
    }


    @Override
    public ObjectMetadata getObjectMetadata(S3URI s3uri) {
        ObjectMetadata metadata;
        if ((metadata = objectMetadataCache.getIfPresent(s3uri)) != null) {
            return metadata;
        }

        if ((metadata = threddsS3Client.getObjectMetadata(s3uri)) != null) {
            objectMetadataCache.put(s3uri, metadata);
        }

        return metadata;
    }

    @Override
    public ObjectListing listObjects(S3URI s3uri) {
        ObjectListing objectListing;
        if ((objectListing = objectListingCache.getIfPresent(s3uri)) != null) {
            return objectListing;
        }

        if ((objectListing = threddsS3Client.listObjects(s3uri)) != null) {
            objectListingCache.put(s3uri, objectListing);
        }

        return objectListing;
    }

    @Override
    public File saveObjectToFile(S3URI s3uri, File file) throws IOException {
        File cachedFile;
        if ((cachedFile = objectFileCache.getIfPresent(s3uri)) != null && cachedFile.exists()) {
            if (file.equals(cachedFile)) {
                return file;
            } else {
                Files.copy(cachedFile, file);
                objectFileCache.put(s3uri, file);  // cachedFile will be evicted from the cache.
                return file;
            }
        }

        if ((file = threddsS3Client.saveObjectToFile(s3uri, file)) != null) {
            objectFileCache.put(s3uri, file);
        }

        return file;
    }
}
