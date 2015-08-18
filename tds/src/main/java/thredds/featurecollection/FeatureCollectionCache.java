/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.featurecollection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Component
public class FeatureCollectionCache implements InitializingBean {
  static private final Logger logger = LoggerFactory.getLogger(FeatureCollectionCache.class);

  @Autowired
  @Qualifier("fcTriggerEventBus")
  private EventBus eventBus;

  @Autowired
  CollectionUpdater collectionUpdater;

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
      eventBus.register(result);
      collectionUpdater.scheduleTasks(fcr.getConfig(), null);  // use default logger
      return result;

    } catch (Throwable t) {
      t.printStackTrace();
      throw new IOException(t);
    }
  }
}
