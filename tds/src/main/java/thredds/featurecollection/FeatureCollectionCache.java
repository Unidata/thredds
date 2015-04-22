/* Copyright */
package thredds.featurecollection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.Dataset;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.config.TdsContext;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Keep cache of InvDatasetFeatureCollection
 * Using guava cache.
 *
 * @author caron
 * @since 4/2/2015
 */
@Component("FeatureCollectionCache")
public class FeatureCollectionCache implements InitializingBean {
  static private final Logger logger = LoggerFactory.getLogger(FeatureCollectionCache.class);

  @Autowired
  private TdsContext tdsContext;
  private Cache<String, InvDatasetFeatureCollection> cache;

  public FeatureCollectionCache() {
  }

  public FeatureCollectionCache(int maxSize) {
    this.cache = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .recordStats()
            .build();
  }

  @Override
  public void afterPropertiesSet() {
    this.cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .recordStats()
            .build();
  }

  public void put(String catKey, InvDatasetFeatureCollection cat) throws IOException {
    cache.put(catKey, cat);
  }

  public void invalidate(String catKey) throws IOException {
    cache.invalidate(catKey);
  }

  public InvDatasetFeatureCollection getIfPresent(String catKey) throws IOException {
    return cache.getIfPresent(catKey);
  }

  public InvDatasetFeatureCollection get(final FeatureCollectionRef fcr) throws IOException {
    try {
      return cache.get(fcr.getCollectionName(), new Callable<InvDatasetFeatureCollection>() {
        @Override
        public InvDatasetFeatureCollection call() throws IOException {
          return makeFeatureCollection(fcr);
        }
      });

    } catch (ExecutionException e) {
      Throwable c = e.getCause();
      if (c instanceof IOException) throw (IOException) c;
      throw new RuntimeException(e.getCause());
    }
  }

  private InvDatasetFeatureCollection makeFeatureCollection(FeatureCollectionRef fcr) throws IOException {
    try {
      InvDatasetFeatureCollection result = InvDatasetFeatureCollection.factory(fcr, fcr.getConfig());
      return result;

    } catch (Throwable t) {
      t.printStackTrace();
      throw new IOException(t);
    }
  }
}
