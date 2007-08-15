package thredds.server.wcs;

import thredds.wcs.WcsDataset;
import thredds.wcs.GetCoverageRequest;
import thredds.wcs.SectionType;
import thredds.servlet.*;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.util.DiskCache2;

/**
 * Servlet handles serving data via WCS 1.0.
 *
 */
public class WCSServlet extends AbstractServlet {
  private ucar.nc2.util.DiskCache2 diskCache = null;
  private boolean allow = false, deleteImmediately = true;
  private long maxFileDownloadSize;

  private VersionHandler versionHandler;

  // must end with "/"
  protected String getPath() { return "wcs/"; }
  protected void makeDebugActions() {}

  public void init() throws ServletException
  {
    super.init();

    allow = ThreddsConfig.getBoolean("WCS.allow", false);
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
    WcsDataset.setDiskCache(diskCache);
  }

  public void destroy()
  {
    if (diskCache != null)
      diskCache.exit();
    super.destroy();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException
  {
    if (!allow) {
      // ToDo - Server not configured to support WCS. Should response code be 404 (Not Found) instead of 403 (Forbidden)?
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    ServletUtil.logServerAccessSetup( req );

    if ( req.getParameterMap().size() == 0 )
    {
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, "GET request not a WCS KVP request." );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
      return;
    }
    
    String service = ServletUtil.getParameterIgnoreCase( req, "Service");
    if ( service == null || ! service.equals( "WCS"))
    {
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, "GET WCS KVP request missing SERVICE parameter.");
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
      return;
    }

    // Decide on requested version.
    String versions = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions");
    String version = ServletUtil.getParameterIgnoreCase( req, "Version");
    if ( version != null)
      if ( version.equals( "1.0.0"))
        versionHandler = new WCS_1_0();
      else if ( version.equals( "1.1.0"))
        versionHandler = new WCS_1_1_0();
      else
        versionHandler = new WCS_1_1_0();
    else if (versions != null)
      versionHandler = new WCS_1_1_0();
    else
      versionHandler = new WCS_1_1_0();


    versionHandler.handleKVP( this, req, res);
  }
}
