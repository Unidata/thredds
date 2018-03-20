/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import thredds.server.config.TdsContext;
import thredds.server.config.ThreddsConfig;
import thredds.servlet.ServletUtil;
import ucar.nc2.util.DiskCache2;

import java.io.File;

@Component
@DependsOn("TdsContext")
public final class NcssDiskCache {

  @Autowired
  private TdsContext tdsContext;

  private DiskCache2 diskCache;
  private String cachePath;

  public NcssDiskCache() {
  }

  public void init() {

    //maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
    String defaultPath = new File(tdsContext.getThreddsDirectory(), "/cache/ncss/").getPath();
    this.cachePath = ThreddsConfig.get("NetcdfSubsetService.dir", defaultPath);
    File cacheDir = new File(cachePath);
    if (!cacheDir.exists()) {
      if (!cacheDir.mkdirs()) {
        ServletUtil.logServerStartup.error("Cant make cache directory " + cachePath);
        throw new IllegalArgumentException("Cant make cache directory " + cachePath);
      }
    }

    int scourSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.scour", 60 * 10);
    int maxAgeSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.maxAge", -1);
    maxAgeSecs = Math.max(maxAgeSecs, 60 * 5);  // give at least 5 minutes to download before scouring kicks in.
    scourSecs = Math.max(scourSecs, 60 * 5);  // always need to scour, in case user doesnt get the file, we need to clean it up

    // LOOK: what happens if we are still downloading when the disk scour starts?
    diskCache = new DiskCache2(cachePath, false, maxAgeSecs / 60, scourSecs / 60);
    ServletUtil.logServerStartup.info(getClass().getName() + "Ncss.Cache= " + cachePath + " scour = " + scourSecs + " maxAgeSecs = " + maxAgeSecs);
  }

  public DiskCache2 getDiskCache() {
    return this.diskCache;
  }

  public String getServletCachePath() {
    String contextPath = (tdsContext == null) ? "" : tdsContext.getContextPath();  // for unit tests until i can figure out how to get a mock TdsContext
    return contextPath + cachePath + "/";
  }

    // for unit tests until i can figure out how to wire correctly
  public NcssDiskCache(String cachePath) {
    this.cachePath = cachePath;
    this.diskCache = new DiskCache2(cachePath, false, 0, 0);
  }

}
