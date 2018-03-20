/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
    if (!catFile.exists()) {
      int pos = catalogFullPath.indexOf("content/thredds/");
      String filename = (pos > 0) ? catalogFullPath.substring(pos+16) : catalogFullPath;
      throw new FileNotFoundException(filename);
    }

    URI uri;
    try {
      uri = new URI("file:" + StringUtil2.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    } catch (URISyntaxException e) {
      logger.error(ERROR + "readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    ConfigCatalogBuilder builder = new ConfigCatalogBuilder();
    ConfigCatalog cat = (ConfigCatalog) builder.buildFromURI(uri);          // LOOK use file and keep lastModified
    if (builder.hasFatalError()) {
      throw new IOException("invalid catalog " + catalogFullPath);
    }
    return cat;
  }

}
