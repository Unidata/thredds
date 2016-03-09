/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.catalog;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import thredds.server.catalog.builder.ConfigCatalogBuilder;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

/**
 * Keep a cache of ConfigCatalog objects.
 * Uses guava com.google.common.cache.
 * If cache miss, call ConfigCatalogBuilder
 *
 * @author caron
 * @since 3/21/2015
 */
@Component
@DependsOn("TdsContext")
public class ConfigCatalogCache implements CatalogReader {
  static private final Logger logger = LoggerFactory.getLogger(ConfigCatalogCache.class);
  static private final String ERROR = "*** ERROR ";

  private String rootPath;
  private Cache<String, ConfigCatalog> cache;

  public ConfigCatalogCache() {
  }

  public ConfigCatalogCache(String rootPath, int maxSize) {
    this.rootPath = rootPath;
    this.cache = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .recordStats()
            .build();
  }

  public void init(String rootPath, int maxSize) {
    this.rootPath = rootPath;
    this.cache = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .recordStats()
                    // .removalListener(MY_LISTENER)
            .build(new CacheLoader<String, ConfigCatalog>() {
              public ConfigCatalog load(String key) throws IOException {
                return readCatalog(key);
              }
            });
  }

  public void put(String catKey, ConfigCatalog cat) throws IOException {
    cache.put(catKey, cat);
  }

  /*
  public void invalidate(String catKey) throws IOException {
    cache.invalidate(catKey);
  }

  public ConfigCatalog getIfPresent(String catKey) throws IOException {
    return cache.getIfPresent(catKey);
  } */

  public void invalidateAll() {
    cache.invalidateAll();
  }

  public ConfigCatalog getFromAbsolutePath(String catalogFullPath) throws IOException {
    catalogFullPath = StringUtil2.substitute(catalogFullPath, "\\", "/"); // nasty microsnot
    if (catalogFullPath.startsWith(rootPath)) {
      String catKey = catalogFullPath.substring(rootPath.length());
      // if (catKey.startsWith("/")) catKey = catKey.substring(1);
      return get(catKey);
    }

    return readCatalog(catalogFullPath);
  }


  public ConfigCatalog get(final String catKey) throws IOException {
    try {

      /* LOOK could check expires here
          if (catalog != null) {  // see if its stale
      CalendarDate expiresDateType = catalog.getExpires();
      if ((expiresDateType != null) && expiresDateType.getMillis() < System.currentTimeMillis())
        reread = true;     // LOOK reread ??
    }
       */

      return cache.get(catKey, () -> readCatalog(rootPath + catKey));

    } catch (ExecutionException e) {
      Throwable c = e.getCause();
      if (c instanceof IOException) throw (IOException) c;
      throw new RuntimeException(e.getCause());
    }
  }

  static public ConfigCatalog readCatalog(String catalogFullPath) throws IOException {

    // see if it exists
    File catFile = new File(catalogFullPath);
    if (!catFile.exists())
      throw new FileNotFoundException();

    URI uri;
    try {
      uri = new URI("file:" + StringUtil2.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    } catch (URISyntaxException e) {
      logger.error(ERROR + "readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    ConfigCatalogBuilder builder = new ConfigCatalogBuilder();
    //try {
      // read the catalog
      //logger.info("\n-------readCatalog(): full path=" + catalogFullPath + "; catKey=" + catKey);
      ConfigCatalog cat = (ConfigCatalog) builder.buildFromURI(uri);          // LOOK use file and keep lastModified
      if (builder.hasFatalError()) {
        // logger.error(ERROR + "   invalid catalog -- " + builder.getErrorMessage());
        throw new IOException("invalid catalog "+catalogFullPath);
      }

      //if (builder.getErrorMessage().length() > 0)
      //  logger.debug(builder.getErrorMessage());

      return cat;

    //} catch (Throwable t) {
    //  logger.error(ERROR + "  Exception on catalog=" + catalogFullPath + " " + t.getMessage() + "\n log=" + builder.getErrorMessage(), t);
    //  return null;
    //}

  }

}
