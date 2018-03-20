/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Memory cache using guava cache
 *
 * @author caron
 * @since 11/12/2014
 */
public class FileCacheGuava implements FileCacheIF {

  private final String name;
  private LoadingCache<String, FileCacheable> cache;

  public FileCacheGuava(String name, int maxSize) {
    this.name = name;
    this.cache = CacheBuilder.newBuilder()
           .maximumSize(maxSize)
                   .recordStats()
           // .removalListener(MY_LISTENER)
           .build(
                   new CacheLoader<String, FileCacheable>() {
                     public FileCacheable load(String key) throws IOException {
                       throw new IllegalStateException();
                     }
                   });
   }


  @Override
  public void enable() {

  }

  @Override
  public void disable() {
    clearCache(true);
    cache = null;
  }

  @Override
  public FileCacheable acquire(FileFactory factory, DatasetUrl durl) throws IOException {
    return acquire(factory, durl.trueurl, durl, -1, null, null);
  }

  @Override
  public FileCacheable acquire(final FileFactory factory, Object hashKey, final DatasetUrl durl, final int buffer_size, final CancelTask cancelTask, final Object spiObject) throws IOException {
    if (null == hashKey) hashKey = durl.trueurl;
    if (null == hashKey) throw new IllegalArgumentException();

    try {
      // If the key wasn't in the "easy to compute" group, we need to use the factory.
      return cache.get((String)hashKey, () -> factory.open(durl, buffer_size, cancelTask, spiObject));
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  @Override
  public boolean release(FileCacheable ncfile) throws IOException {
    return false;
  }

  @Override
  public void eject(Object hashKey) {

  }

  @Override
  public void clearCache(boolean force) {
    cache.invalidateAll();
  }

  @Override
  public void resetTracking() {

  }

  @Override
  public void showTracking(Formatter format) {

  }

  @Override
  public void showCache(Formatter f) {
    CacheStats stats = cache.stats();
    f.format("%n%s%n%s%n", name, stats);
  }

  @Override
  public void showStats(Formatter f) {
    CacheStats stats = cache.stats();
    f.format("%s", stats);
  }

  @Override
  public List<String> showCache() {
    return null;
  }
}
