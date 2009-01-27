/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.wcs;

import thredds.servlet.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.util.DiskCache2;

/**
 * Servlet handles serving data via WCS 1.0.
 *
 */
public class WCSServlet extends AbstractServlet {
  private ucar.nc2.util.DiskCache2 diskCache = null;
  private boolean allow = false, deleteImmediately = true;
  private boolean allowRemote = false;
  private long maxFileDownloadSize;

  // ToDo Consider using a SortedMap to contain handlers.
  private List<VersionHandler> versionHandlers;
  private String supportedVersionsString;

  public enum Operation
  {
    GetCapabilities, DescribeCoverage, GetCoverage
  }

  // must end with "/"
  protected String getPath() { return "wcs/"; }
  protected void makeDebugActions() {}

  public void init() throws ServletException
  {
    super.init();

    allow = ThreddsConfig.getBoolean("WCS.allow", false);
    allowRemote = ThreddsConfig.getBoolean( "WCS.allowRemote", false );
    deleteImmediately = ThreddsConfig.getBoolean( "WCS.deleteImmediately", deleteImmediately);
    maxFileDownloadSize = ThreddsConfig.getBytes("WCS.maxFileDownloadSize", (long) 1000 * 1000 * 1000);
    String cache = ThreddsConfig.get("WCS.dir", contentPath + "/wcache");
    File cacheDir = new File(cache);
    cacheDir.mkdirs();

    int scourSecs = ThreddsConfig.getSeconds("WCS.scour", 60 * 10);
    int maxAgeSecs = ThreddsConfig.getSeconds("WCS.maxAge", -1);
    maxAgeSecs = Math.max(maxAgeSecs, 60 * 5);  // give at least 5 minutes to download before scouring kicks in.
    scourSecs = Math.max(scourSecs, 60 * 5);  // always need to scour, in case user doesnt get the file, we need to clean it up

    // LOOK: what happens if we are still downloading when the disk scour starts?
    diskCache = new DiskCache2(cache, false, maxAgeSecs / 60, scourSecs / 60);

    // Make sure to add these in increasing order!
    versionHandlers = new ArrayList<VersionHandler>();
    versionHandlers.add( new thredds.server.wcs.v1_0_0_1.WcsHandler( "1.0.0" )
            .setDeleteImmediately( deleteImmediately )
            .setDiskCache( diskCache ) );
    versionHandlers.add( new thredds.server.wcs.v1_0_0_Plus.WcsHandler( "1.0.0.11" )
            .setDeleteImmediately( deleteImmediately )
            .setDiskCache( diskCache ) );
    for ( VersionHandler vh: versionHandlers)
    {
      supportedVersionsString = (supportedVersionsString == null ? "" : supportedVersionsString + ",") + vh.getVersion().getVersionString();
    }

    log.info( "init()" + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy()
  {
    log.info( "destroy(): " + UsageLog.setupNonRequestContext() );
    if (diskCache != null)
      diskCache.exit();
    super.destroy();
    log.info( "destroy()" + UsageLog.closingMessageNonRequestContext() );
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException
  {
    log.info( UsageLog.setupRequestContext(req));

    // Check whether TDS is configured to support WCS.
    if (!allow) {
      // ToDo - Server not configured to support WCS. Should response code be 404 (Not Found) instead of 403 (Forbidden)?
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ));
      return;
    }

    // Check if TDS is configured to support WCS on remote datasets.
    String datasetURL = ServletUtil.getParameterIgnoreCase( req, "dataset" );
    // ToDo LOOK - move this into TdsConfig?
    if ( datasetURL != null && ! allowRemote )
    {
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ));
      res.sendError( HttpServletResponse.SC_FORBIDDEN, "Catalog services not supported for remote catalogs." );
      return;
    }

    // Get parameters needed to determine version.
    String serviceParam = ServletUtil.getParameterIgnoreCase( req, "Service");
    String requestParam = ServletUtil.getParameterIgnoreCase( req, "Request" );
    String acceptVersionsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions" );
    String versionParam = ServletUtil.getParameterIgnoreCase( req, "Version" );

    // Make sure this is a WCS KVP request.
    if ( serviceParam == null || ! serviceParam.equals( "WCS"))
    {
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, "GET request not a WCS KVP requestParam (missing or bad SERVICE parameter).");
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, -1 ));
      return;
    }

    // Decide on requested version.
    VersionHandler targetHandler = null;
    if ( requestParam == null )
    {
      versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "MissingParameterValue", "Request", "" );
      return;
    }
    else if ( requestParam.equals( Operation.GetCapabilities.toString()))
    {
      // Version negotiation using "acceptVersions" parameter.
      if ( acceptVersionsParam != null )
      {
        if ( versionParam != null )
        {
          versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "NoApplicableCode", "", "Request included both \"Version\" and \"AcceptVersions\" parameters." );
          return;
        }
        String acceptableVersions[] = acceptVersionsParam.split( "," );
        for ( String curVerString : acceptableVersions )
        {
          try { targetHandler = getVersionHandler( curVerString ); }
          catch ( IllegalArgumentException e )
          {
            versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "InvalidParameterValue", "AcceptVersions", "Invalid \"AcceptVersions\" parameter value <" + acceptVersionsParam + ">." );
          }
          if ( targetHandler != null )
            break;
        }
        if ( targetHandler == null )
        {
          versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "VersionNegotiationFailed", "", "The \"AcceptVersions\" parameter value <" + acceptVersionsParam + "> did not match any supported versions <" + supportedVersionsString + ">." );
        }
      }
      else if ( versionParam != null )
      {
        // Version negotiation using WCS 1.0.0 spec (uses "Version" parameter).
        try { targetHandler = getVersionHandler_1_0_0( versionParam); }
        catch ( IllegalArgumentException e )
        {
          versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "InvalidParameterValue", "Version", "Invalid \"Version\" parameter value <" + versionParam + ">." );
          return;
        }
      }
      else
      {
        // No version specified, use latest version.
        targetHandler = versionHandlers.get( versionHandlers.size() - 1);
      }
    }
    else
    {
      // Find requested version (no negotiation for "DescribeCoverage" and "GetCoverage" requests).
      if ( ! requestParam.equals( Operation.DescribeCoverage.toString()) &&
           ! requestParam.equals( Operation.GetCoverage.toString()) )
      {
        versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "InvalidParameterValue", "Request", "Invalid \"Operation\" parameter value <" + requestParam + ">." );
        return;
      }
      if ( versionParam == null )
      {
        versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "MissingParameterValue", "Version", "" );
        return;
      }
      else
      {
        try { targetHandler = getVersionHandler( versionParam); }
        catch ( IllegalArgumentException e )
        {
          versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "InvalidParameterValue", "Version", "Invalid \"Version\" parameter value <" + versionParam + ">." );
          return;
        }
        if ( targetHandler == null )
        {
          versionHandlers.get( versionHandlers.size() - 1 ).handleExceptionReport( res, "InvalidParameterValue", "Version", "Invalid \"Version\" parameter value <" + versionParam + ">." );
          return;
        }
      }
    }

    targetHandler.handleKVP( this, req, res);
  }

  protected void doPost( HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException
  {
    log.info( UsageLog.setupRequestContext(req));

    res.setStatus( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
    res.setHeader( "Allow", "GET");
    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_METHOD_NOT_ALLOWED, -1 ));
    return;
  }

  /**
   * Return the VersionHandler appropriate for the given version number
   * using the WCS 1.0.0 version number negotiation rules.

   * @param versionNumber the requested version string
   * @return the appropriate VersionHandler for the requested version string (as per WCS 1.0 version negotiation).  
   * @throws IllegalArgumentException if versionNumber is null or an invalid version string.
   */
  private VersionHandler getVersionHandler_1_0_0( String versionNumber )
  {
    Version reqVersion = new Version( versionNumber );

    VersionHandler targetHandler = null;

    VersionHandler prevVh = null;
    for ( VersionHandler curVh: versionHandlers)
    {
      if ( reqVersion.equals( curVh.getVersion()) )
      {
        // Use matching version.
        targetHandler = curVh;
        break;
      }
      else if ( reqVersion.lessThan( curVh.getVersion()))
      {
        if ( prevVh == null)
          // Request less than lowest version, use lowest version.
          targetHandler = curVh;
        else
          // Request less than current version, use previous version.
          targetHandler = prevVh;
        break;
      }
      else if ( reqVersion.greaterThan( curVh.getVersion()))
      {
        prevVh = curVh;
      }
    }
    if ( targetHandler == null && prevVh.equals( versionHandlers.get( versionHandlers.size() - 1)))
    {
      // Request greater than largest version, use largest version.
      targetHandler = prevVh;
    }

    return targetHandler;
  }

  /**
   * Return the VersionHandler that supports the given version number or null
   * if the given version number is not supported.
   *
   * @param versionNumber the requested version string
   * @return the VersionHandler that supports the requested version string or null.
   * @throws IllegalArgumentException if versionNumber is null or an invalid version string.
   */
  private VersionHandler getVersionHandler( String versionNumber )
  {
    Version reqVersion = new Version( versionNumber );

    VersionHandler targetHandler = null;

    for ( VersionHandler curVh: versionHandlers)
    {
      if ( reqVersion.equals( curVh.getVersion()) )
      {
        // Matching version found.
        targetHandler = curVh;
        break;
      }
    }

    return targetHandler;
  }
}
