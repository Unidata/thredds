package ucar.nc2.util.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Describe
 *
 * @author caron
 * @since 11/12/2014
 */
public class FileCacheGuava implements FileCacheIF {

  private final String name;
  private final LoadingCache<String, FileCacheable> cache;
  private final FileFactory factory;

  public FileCacheGuava(String name, FileFactory _factory, int maxSize) {
    this.name = name;
    this.factory = _factory;
    this.cache = CacheBuilder.newBuilder()
           .maximumSize(maxSize)
           // .removalListener(MY_LISTENER)
           .build(
                   new CacheLoader<String, FileCacheable>() {
                     public FileCacheable load(String key) throws IOException {
                       return factory.open(key, -1, null, null);
                     }
                   });

   }


  @Override
  public void enable() {

  }

  @Override
  public void disable() {

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
  public void eject(Object hashKey) throws IOException {

  }

  @Override
  public void clearCache(boolean force) {

  }

  @Override
  public void resetTracking() {

  }

  @Override
  public void showTracking(Formatter format) {

  }

  @Override
  public void showCache(Formatter format) {

  }

  @Override
  public void showStats(Formatter format) {

  }

  @Override
  public List<String> showCache() {
    return null;
  }
}
