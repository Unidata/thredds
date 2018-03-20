/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs;

import ucar.nc2.util.DiskCache2;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Each implementation handles a different version of WCS requests.
 *
 * @author edavis
 * @since 4.0
 */
public interface VersionHandler {

  public Version getVersion();

  public VersionHandler setDiskCache(DiskCache2 diskCache);

  /**
   * Set whether generated files are deleted immediately after sent (true) or handled by cache (false).
   * <p/>
   * Note: currently (2008-03-05), each request generates a unique file. So caching doesn't make much sense.
   *
   * @param deleteImmediately if true, delete immediately, otherwise allow cache to handle.
   * @return this VersionHandler
   */
  public VersionHandler setDeleteImmediately(boolean deleteImmediately);

  public void handleKVP(HttpServlet servlet,
                        HttpServletRequest req,
                        HttpServletResponse res)
          throws ServletException, IOException;

  public void handleExceptionReport(HttpServletResponse res,
                                    String code, String locator,
                                    String message)
          throws IOException;

}
