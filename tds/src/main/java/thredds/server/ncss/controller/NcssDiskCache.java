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
