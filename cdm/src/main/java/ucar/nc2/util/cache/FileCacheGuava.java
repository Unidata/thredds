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

package ucar.nc2.util.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
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
  public FileCacheable acquire(FileFactory factory, String location) throws IOException {
    return acquire(factory, location, location, -1, null, null);
  }

  @Override
  public FileCacheable acquire(final FileFactory factory, final Object hashKey, final String location, final int buffer_size, final CancelTask cancelTask, final Object spiObject) throws IOException {
    try {
      // If the key wasn't in the "easy to compute" group, we need to
      // do things the hard way.
      return cache.get(location, () -> factory.open(location, buffer_size, cancelTask, spiObject));
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
