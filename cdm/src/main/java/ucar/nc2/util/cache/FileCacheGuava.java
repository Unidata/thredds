package ucar.nc2.util.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Callable;
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
  public FileCacheable acquire(FileFactory factory, String location) throws IOException {
    return acquire(factory, location, location, -1, null, null);
  }

  @Override
  public FileCacheable acquire(final FileFactory factory, final Object hashKey, final String location, final int buffer_size, final CancelTask cancelTask, final Object spiObject) throws IOException {
    try {
      // If the key wasn't in the "easy to compute" group, we need to
      // do things the hard way.
      return cache.get(location, new Callable<FileCacheable>() {
        @Override
        public FileCacheable call() throws IOException {
          return factory.open(location, buffer_size, cancelTask, spiObject);
        }
      });
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
