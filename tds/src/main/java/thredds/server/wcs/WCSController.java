/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.server.config.TdsContext;
import thredds.server.config.ThreddsConfig;
import thredds.servlet.*;

import java.io.*;
import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.util.DiskCache2;

/**
 * Handles serving data via WCS 1.0.
 */
@Controller
@RequestMapping("/wcs")
public class WCSController {
  private static org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");

  @Autowired
  private TdsContext tdsContext;

  private ucar.nc2.util.DiskCache2 diskCache = null;
  private boolean allow = true, deleteImmediately = true;
  private boolean allowRemote = false;
  private long maxFileDownloadSize;

  private WcsHandler wcsHandler;

  public enum Operation {
    GetCapabilities, DescribeCoverage, GetCoverage
  }

  @PostConstruct
  public void init() throws ServletException {

    allow = ThreddsConfig.getBoolean("WCS.allow", true);
    logServerStartup.info("WCS:allow= " + allow);
    if (!allow) {
      logServerStartup.info("WCS service not enabled in threddsConfig.xml: ");
      return;
    }
    allowRemote = ThreddsConfig.getBoolean("WCS.allowRemote", false);
    deleteImmediately = ThreddsConfig.getBoolean("WCS.deleteImmediately", deleteImmediately);
    maxFileDownloadSize = ThreddsConfig.getBytes("WCS.maxFileDownloadSize", (long) 1000 * 1000 * 1000);
    String cache = ThreddsConfig.get("WCS.dir", new File(tdsContext.getThreddsDirectory(), "/cache/wcs/").getPath());
    File cacheDir = new File(cache);
    cacheDir.mkdirs();

    int scourSecs = ThreddsConfig.getSeconds("WCS.scour", 60 * 10);
    int maxAgeSecs = ThreddsConfig.getSeconds("WCS.maxAge", -1);
    maxAgeSecs = Math.max(maxAgeSecs, 60 * 5);  // give at least 5 minutes to download before scouring kicks in.
    scourSecs = Math.max(scourSecs, 60 * 5);  // always need to scour, in case user doesnt get the file, we need to clean it up

    // LOOK: what happens if we are still downloading when the disk scour starts?
    diskCache = new DiskCache2(cache, false, maxAgeSecs / 60, scourSecs / 60);

    // Version Handlers
    // - Latest non-experimental version supported is "1.0.0"
    this.wcsHandler = new WcsHandler("1.0.0")
            .setDeleteImmediately(deleteImmediately)
            .setDiskCache(diskCache);

    logServerStartup.info("WCS service - init done - ");
  }

  @RequestMapping("**")
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    // Check whether TDS is configured to support WCS.
    if (!allow) {
      // ToDo - Server not configured to support WCS. Should response code be 404 (Not Found) instead of 403 (Forbidden)?
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "WCS service not supported");
      return;
    }

    // Check if TDS is configured to support WCS on remote datasets.
    String datasetURL = ServletUtil.getParameterIgnoreCase(req, "dataset");
    // ToDo LOOK - move this into TdsConfig?
    if (datasetURL != null && !allowRemote) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "WCS service not supported for remote datasets.");
      return;
    }

    // Get parameters needed to determine version.
    String serviceParam = ServletUtil.getParameterIgnoreCase(req, "Service");
    String requestParam = ServletUtil.getParameterIgnoreCase(req, "Request");
    String acceptVersionsParam = ServletUtil.getParameterIgnoreCase(req, "AcceptVersions");
    String versionParam = ServletUtil.getParameterIgnoreCase(req, "Version");

    // Make sure this is a WCS KVP request.
    if (serviceParam == null || !serviceParam.equalsIgnoreCase("WCS")) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "GET request not a WCS KVP requestParam (missing or bad SERVICE parameter).");
      return;
    }

    wcsHandler.handleKVP(null, req, res);
  }

}
