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

import thredds.util.Version;
import thredds.servlet.*;
import thredds.server.wcs.v1_0_0_1.WcsHandler;

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
  private List<VersionHandler> experimentalHandlers;
  private String supportedVersionsString;
  private VersionHandler latestSupportedVersion;

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
    logServerStartup.error("WCS:allow= "+allow);
    if ( ! allow )
    {
      logServerStartup.info( "WCS service not enabled in threddsConfig.xml: " + UsageLog.closingMessageNonRequestContext() );
      return;
    }
    allowRemote = ThreddsConfig.getBoolean( "WCS.allowRemote", false );
    deleteImmediately = ThreddsConfig.getBoolean( "WCS.deleteImmediately", deleteImmediately);
    maxFileDownloadSize = ThreddsConfig.getBytes("WCS.maxFileDownloadSize", (long) 1000 * 1000 * 1000);
    String cache = ThreddsConfig.get("WCS.dir", contentPath + "/cache/wcs/");
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
    this.latestSupportedVersion = new WcsHandler( "1.0.0" )
            .setDeleteImmediately( deleteImmediately )
            .setDiskCache( diskCache );
    // - Experimental WCS_Plus handler.
    VersionHandler wcsPlusVersion = new thredds.server.wcs.v1_0_0_Plus.WcsHandler( "1.0.0.11" )
            .setDeleteImmediately( deleteImmediately )
            .setDiskCache( diskCache );

    // Supported Versions [Note: Make sure to add these in increasing order!]
    this.versionHandlers = new ArrayList<VersionHandler>();
    this.versionHandlers.add( this.latestSupportedVersion );  // "1.0.0"
    for ( VersionHandler vh: this.versionHandlers)
    {
      supportedVersionsString = (supportedVersionsString == null ? "" : supportedVersionsString + ",") + vh.getVersion().getVersionString();
    }

    // Experimental Handlers [Requests must match exactly.]
    this.experimentalHandlers = new ArrayList<VersionHandler>();
    this.experimentalHandlers.add( wcsPlusVersion );          // "1.0.0.11"

    logServerStartup.info( "WCS service - init done - " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy()
  {
    logServerStartup.info( "WCSServlet.destroy() start: " + UsageLog.setupNonRequestContext() );
    if (diskCache != null)
      diskCache.exit();
    super.destroy();
    logServerStartup.info( "WCSServlet.destroy() done:" + UsageLog.closingMessageNonRequestContext() );
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException
  {
    log.info( UsageLog.setupRequestContext(req));

    // Check whether TDS is configured to support WCS.
    if ( ! allow )
    {
      // ToDo - Server not configured to support WCS. Should response code be 404 (Not Found) instead of 403 (Forbidden)?
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "WCS service not supported");
      log.debug( "WCS service not supported in threddsConfig.xml");
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ));
      return;
    }

    // Check if TDS is configured to support WCS on remote datasets.
    String datasetURL = ServletUtil.getParameterIgnoreCase( req, "dataset" );
    // ToDo LOOK - move this into TdsConfig?
    if ( datasetURL != null && ! allowRemote )
    {
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ));
      res.sendError( HttpServletResponse.SC_FORBIDDEN, "WCS service not supported for remote datasets." );
      return;
    }

    // Get parameters needed to determine version.
    String serviceParam = ServletUtil.getParameterIgnoreCase( req, "Service");
    String requestParam = ServletUtil.getParameterIgnoreCase( req, "Request" );
    String acceptVersionsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions" );
    String versionParam = ServletUtil.getParameterIgnoreCase( req, "Version" );

    // Make sure this is a WCS KVP request.
    if ( serviceParam == null || ! serviceParam.equalsIgnoreCase( "WCS"))
    {
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, "GET request not a WCS KVP requestParam (missing or bad SERVICE parameter).");
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, -1 ));
      return;
    }

    // Decide on requested version.
    VersionHandler targetHandler = null;
    if ( requestParam == null )
    {
      this.latestSupportedVersion.handleExceptionReport( res, "MissingParameterValue", "Request", "" );
      return;
    }
    else if ( requestParam.equalsIgnoreCase( Operation.GetCapabilities.toString() ))
    {
      if ( acceptVersionsParam == null && versionParam == null )
        targetHandler = this.latestSupportedVersion;
      else if ( acceptVersionsParam != null && versionParam != null )
      {
        this.latestSupportedVersion.handleExceptionReport( res, "NoApplicableCode", "", "Request requires one and only one version parameter: either \"Version\" or \"AcceptVersions\"." );
        return;
      }
      else if ( acceptVersionsParam != null )
      {
        // Version negotiation per 1.1.0 spec.
        targetHandler = getHandlerUsingNegotiation_1_1_0( acceptVersionsParam );
        if ( targetHandler == null )
        {
          this.latestSupportedVersion.handleExceptionReport( res, "VersionNegotiationFailed", "AcceptVersions", "The \"AcceptVersions\" parameter value [" + acceptVersionsParam + "[ did not match any supported versions [" + supportedVersionsString + "]." );
          return;
        }
      }
      else if ( versionParam != null )
      {
        // Version negotiation per 1.0.0 spec.
        targetHandler = getHandlerUsingNegotiation_1_0_0( versionParam );
        if ( targetHandler == null )
        {
          this.latestSupportedVersion.handleExceptionReport( res, "InvalidParameterValue", "Version", "Invale \"Version\" parameter value [" + acceptVersionsParam + "] did not match any supported versions [" + supportedVersionsString + "]." );
          return;
        }
      }
      else
      {
        // [[Can't really get here given all the if clauses above.]]
        // No version specified, use latest supported version.
        targetHandler = this.latestSupportedVersion;
      }
    }
    else
    {
      // Find requested version (no negotiation for "DescribeCoverage" and "GetCoverage" requests).
      if ( ! requestParam.equalsIgnoreCase( Operation.DescribeCoverage.toString()) &&
           ! requestParam.equalsIgnoreCase( Operation.GetCoverage.toString()) )
      {
        this.latestSupportedVersion.handleExceptionReport( res, "InvalidParameterValue", "Request", "Invalid \"Operation\" parameter value [" + requestParam + "]." );
        return;
      }
      if ( versionParam == null )
      {
        this.latestSupportedVersion.handleExceptionReport( res, "InvalidParameterValue", "Version", "Request requires a \"Version\" parameter." );
        return;
      }
      else
      {
        // Find matching supported version.
        targetHandler = getMatchingVersionHandler( versionParam );
        if ( targetHandler == null )
        {
          // Find matching "experimental" version.
          targetHandler = getMatchingExpermimentalVersionHandler( versionParam );
          if ( targetHandler == null)
          {
            this.latestSupportedVersion.handleExceptionReport( res, "InvalidParameterValue", "Version", "Invaled \"Version\" parameter value [" + versionParam + "]." );
            return;
          }
        }
      }
    }

    if ( targetHandler == null )
    {
      this.latestSupportedVersion.handleExceptionReport( res, "VersionNegotiationFailed", "", "Version negotiation failed." );
      return;
    }

    targetHandler.handleKVP( this, req, res);
  }

  protected void doPost( HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException
  {
    log.info( UsageLog.setupRequestContext(req));
    if ( ! allow )
    {
      res.sendError( HttpServletResponse.SC_FORBIDDEN, "WCS service not supported" );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ) );
      return;
    }

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
   */
  private VersionHandler getHandlerUsingNegotiation_1_0_0( String versionNumber )
  {
    if ( versionNumber == null )
      return null;

    // Parse request version
    Version reqVersion = null;
    try { reqVersion = new Version( versionNumber ); }
    catch ( IllegalArgumentException e )
    { return null; }

    // Do version negotiation using list of supported non-experimental version handlers.
    VersionHandler handler = null;
    VersionHandler prevVh = null;
    for ( VersionHandler curVh: this.versionHandlers)
    {
      int reqCompareCur = reqVersion.compareTo( curVh.getVersion() );
      if ( reqCompareCur == 0 )
        // Use matching version handler.
        return curVh;
      else if ( reqCompareCur < 0 )
      {
        if ( prevVh == null)
          // Requested version lower than lowest supported version,
          // so use lowest supported version.
          return curVh;
        else
          // Requested version lower than current version,
          // so use previous version.
          return prevVh;
      }
      else if ( reqCompareCur > 0)
      {
        // Requested version greater than current version,
        // so keep current version around in case it is needed.
        prevVh = curVh;
      }
    }
    if ( handler == null)
    {
      // Look for exact matches in experimental version handlers.
      for ( VersionHandler curVh : this.experimentalHandlers )
      {
        if ( reqVersion.equals( curVh.getVersion() ) )
          return curVh;
      }

      // If no exact match on experimental version handlers, revert to
      // latest supported non-experimental version. [Note: Requested version is
      // greater than the latest supported non-experimental version, so use
      // latest supported version (should be same as prevVh).]
      return this.latestSupportedVersion;
    }

    return handler;
  }

  private VersionHandler getHandlerUsingNegotiation_1_1_0( String acceptVersionsParam )
          throws IOException
  {
    VersionHandler handler = null;
    String acceptableVersions[] = acceptVersionsParam.split( "," );
    for ( String curVerString : acceptableVersions )
    {
      handler = getMatchingVersionHandler( curVerString );
      if ( handler != null )
        break;
    }
    return handler;
  }

  /**
   * Return the VersionHandler that supports the given version number or null
   * if the given version number is not supported.
   *
   * @param versionNumber the requested version string
   * @return the VersionHandler that supports the requested version string or null.
   */
  private VersionHandler getMatchingVersionHandler( String versionNumber )
  {
    if ( versionNumber == null )
      return null;

    Version reqVersion = null;
    try { reqVersion = new Version( versionNumber ); }
    catch ( IllegalArgumentException e )
    { return null; }

    for ( VersionHandler curVh: this.versionHandlers)
    {
      if ( reqVersion.equals( curVh.getVersion()) )
        // Return the matching version.
        return curVh;
    }

    return null;
  }

  /**
   * Return the "experimental" VersionHandler that supports the given version number or null
   * if no "experimental" VersionHandler supports the given version number.
   *
   * @param versionNumber the requested version string
   * @return the "experimental" VersionHandler that supports the requested version string or null.
   */
  private VersionHandler getMatchingExpermimentalVersionHandler( String versionNumber )
  {
    if ( versionNumber == null )
      return null;

    Version reqVersion = null;
    try { reqVersion = new Version( versionNumber ); }
    catch ( IllegalArgumentException e )
    { return null; }

    for ( VersionHandler curVh: this.experimentalHandlers)
    {
      if ( reqVersion.equals( curVh.getVersion()) )
        // Matching version found.
        return curVh;
    }

    return null;
  }
}
