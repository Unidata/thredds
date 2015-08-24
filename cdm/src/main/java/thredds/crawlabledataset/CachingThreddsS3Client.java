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
public class CachingThreddsS3Client {
    private static final long ENTRY_EXPIRATION_TIME = 60 * 10;  // In seconds.
    private static final long MAX_ENTRIES = 100;

    private final ThreddsS3Client threddsS3Client;

    private final Cache<String, ObjectMetadata> objectMetadataCache;
    private final Cache<String, ObjectListing> objectListingCache;
    private final Cache<String, File> objectFileCache;

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

    private static class ObjectFileCacheRemovalListener implements RemovalListener<String, File> {
        @Override
        public void onRemoval(RemovalNotification<String, File> notification) {
            notification.getValue().delete();
        }
    }


    public ObjectMetadata getObjectMetadata(String uri) {
        ObjectMetadata metadata;
        if ((metadata = objectMetadataCache.getIfPresent(uri)) != null) {
            return metadata;
        }

        if ((metadata = threddsS3Client.getObjectMetadata(uri)) != null) {
            objectMetadataCache.put(uri, metadata);
        }

        return metadata;
    }

    public ObjectListing listObjects(String uri) {
        ObjectListing objectListing;
        if ((objectListing = objectListingCache.getIfPresent(uri)) != null) {
            return objectListing;
        }

        if ((objectListing = threddsS3Client.listObjects(uri)) != null) {
            objectListingCache.put(uri, objectListing);
        }

        return objectListing;
    }

    public File saveObjectToFile(String uri, File file) throws IOException {
        File cachedFile;
        if ((cachedFile = objectFileCache.getIfPresent(uri)) != null && cachedFile.exists()) {
            if (file.equals(cachedFile)) {
                return file;
            } else {
                Files.copy(cachedFile, file);
                objectFileCache.put(uri, file);  // cachedFile will be evicted from the cache.
                return file;
            }
        }

        if ((file = threddsS3Client.saveObjectToFile(uri, file)) != null) {
            objectFileCache.put(uri, file);
        }

        return file;
    }
}
