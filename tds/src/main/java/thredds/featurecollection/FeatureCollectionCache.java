/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.featurecollection;

import com.google.common.cache.*;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import thredds.server.catalog.FeatureCollectionRef;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Keep cache of InvDatasetFeatureCollection
 * Using guava cache.
 * On cache miss, call InvDatasetFeatureCollection.factory().
 * On cache remove, call InvDatasetFeatureCollection.close().
 * The cache is filled lazily, when that fc is requested.
 *
 * @author caron
 * @since 4/2/2015
 */
@Component
public class FeatureCollectionCache implements InitializingBean {
  static private final Logger logger = LoggerFactory.getLogger(FeatureCollectionCache.class);

  @Autowired
  @Qualifier("fcTriggerEventBus")
  private EventBus eventBus;

  @Autowired
  CollectionUpdater collectionUpdater;

  private Cache<String, InvDatasetFeatureCollection> cache; // key is the collectionName

  public FeatureCollectionCache() {
  }

  public FeatureCollectionCache(int maxSize) {

    RemovalListener<String, InvDatasetFeatureCollection> removalListener = new RemovalListener<String, InvDatasetFeatureCollection>() {
      public void onRemoval(RemovalNotification<String, InvDatasetFeatureCollection> removal) {
        InvDatasetFeatureCollection fc = removal.getValue();
        if (fc != null) fc.close();
      }
    };

    this.cache = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .recordStats()
            .removalListener(removalListener)
            .build();
  }

  @Override
  public void afterPropertiesSet() {
    this.cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .recordStats()
            .build();
  }

 /* public void put(String collectionName, InvDatasetFeatureCollection fc) throws IOException {
    cache.put(collectionName, fc);
  }

  public void invalidate(String collectionName) throws IOException {
    cache.invalidate(collectionName);
  }

  public InvDatasetFeatureCollection getIfPresent(String collectionName) throws IOException {
    return cache.getIfPresent(collectionName);
  } */

  public void invalidateAll() { // LOOK may need to call close on anything in the cache
    cache.invalidateAll();
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
      eventBus.register(result);                               // LOOK on reread, do we want updating?
      collectionUpdater.scheduleTasks(fcr.getConfig(), null);  // schedule any updating specified in the <update> element
                                                               // null means use default logger
      return result;

    } catch (Throwable t) {
      t.printStackTrace();
      throw new IOException(t);
    }
  }
}
