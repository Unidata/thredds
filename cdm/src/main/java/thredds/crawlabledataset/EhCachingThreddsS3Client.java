package thredds.crawlabledataset;

import java.io.File;
import java.io.IOException;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.io.Files;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerAdapter;

/**
 * @author cwardgar
 * @since 2015/08/22
 */
public class EhCachingThreddsS3Client {
    private static final String EHCACHE_S3_OBJECT_KEY = "S3Objects";
    private static final String EHCACHE_S3_OBJECT_METADATA_KEY = "S3ObjectMetadata";
    private static final String EHCACHE_S3_LISTING_KEY = "S3Listing";
    private static final int EHCACHE_MAX_OBJECTS = 1000;
    private static final int EHCACHE_TTL = 60;
    private static final int EHCACHE_TTI = 60;

    private final ThreddsS3ClientImpl threddsS3Client;

    public EhCachingThreddsS3Client(ThreddsS3ClientImpl threddsS3Client) {
        this.threddsS3Client = threddsS3Client;
    }

    public Cache getS3ObjectMetadataCache() {
        return getS3Cache(EHCACHE_S3_OBJECT_METADATA_KEY);
    }

    public Cache getS3ListingCache() {
        return getS3Cache(EHCACHE_S3_LISTING_KEY);
    }

    public Cache getS3ObjectCache() {
        return getS3Cache(EHCACHE_S3_OBJECT_KEY, new S3CacheEventListener());
    }

    private Cache getS3Cache(String cacheName) {
        return getS3Cache(cacheName, null);
    }

    private Cache getS3Cache(String cacheName, CacheEventListener eventListener) {
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

    private void deleteFileElement(Element element) {
        // TODO
    }

    private class S3CacheEventListener extends CacheEventListenerAdapter {
        @Override
        public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
            deleteFileElement(element);
        }

        @Override
        public void notifyElementExpired(final Ehcache cache, final Element element) {
            deleteFileElement(element);
        }

        @Override
        public void notifyElementEvicted(Ehcache cache, Element element) {
            deleteFileElement(element);
        }
    }


    public ObjectMetadata getObjectMetadata(S3URI s3uri) {
        Element element;
        if ((element = getS3ObjectMetadataCache().get(s3uri)) != null) {
            return (ObjectMetadata) element.getObjectValue();
        }

        ObjectMetadata metadata = threddsS3Client.getObjectMetadata(s3uri);
        getS3ObjectMetadataCache().put(new Element(s3uri, metadata));
        return metadata;
    }

    public ObjectListing listObjects(S3URI s3uri) {
        Element element;
        if ((element = getS3ListingCache().get(s3uri)) != null) {
            return (ObjectListing) element.getObjectValue();
        }

        ObjectListing objectListing = threddsS3Client.listObjects(s3uri);
        getS3ListingCache().put(new Element(s3uri, objectListing));
        return objectListing;
    }

    public ObjectMetadata saveObjectToFile(S3URI s3uri, File file) throws IOException {
        Element element;
        if ((element = getS3ObjectCache().get(s3uri)) != null/* && ((File) element.getObjectValue()).exists()*/) {
            File cachedFile = (File) element.getObjectValue();

            if (!file.equals(cachedFile)) {
                Files.copy(cachedFile, file);
                getS3ObjectCache().put(new Element(s3uri, file));
            }
        }

        return null;
    }
}
