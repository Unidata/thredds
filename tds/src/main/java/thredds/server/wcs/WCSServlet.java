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

  // must end with "/"
  protected String getPath() { return "wcs/"; }
  protected void makeDebugActions() {}

  public void init() throws ServletException {
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

  public void destroy() {
    if (diskCache != null)
      diskCache.exit();
    super.destroy();
  }

  private WcsDataset openWcsDataset(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String datasetURL = ServletUtil.getParameterIgnoreCase(req, "dataset");
    boolean isRemote = (datasetURL != null);
    String datasetPath = isRemote ? datasetURL : req.getPathInfo();

    // convert to a GridDataset
    GridDataset gd = isRemote ? ucar.nc2.dt.grid.GridDataset.open(datasetPath) : DatasetHandler.openGridDataset( req, res, datasetPath);
    if (gd == null) return null;

    // convert to a WcsDataset
    WcsDataset ds = new WcsDataset(gd, datasetPath, isRemote);
    String setServerBase = ServletUtil.getRequestBase( req);
    ds.setServerBase( setServerBase);

    return ds;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    ServletUtil.logServerAccessSetup( req );

    if (Debug.isSet("showRequest"))
      log.debug("**WCS req=" + ServletUtil.getRequest(req));
    if (Debug.isSet("showRequestDetail"))
      log.debug( ServletUtil.showRequestDetail(this, req));

    // check on static or dynamic catalogs
    //if (DataRootHandler.getInstance().processReqForCatalog( req, res))
    // return;

    // wcs request
    WcsDataset wcsDataset = null;
    try {
      String request = ServletUtil.getParameterIgnoreCase(req, "REQUEST");

      if (request == null) {
        makeServiceException( res, "MissingParameterValue", "REQUEST parameter missing");
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1);
        return;
      }

      wcsDataset = openWcsDataset( req, res);
      if (wcsDataset == null) return;

      if (request.equals("GetCapabilities")) {

        SectionType sectionType = null;
        String section = ServletUtil.getParameterIgnoreCase(req, "SECTION");

        if (section != null) {
          sectionType  = SectionType.getType(section);
          if (sectionType == null) {
            makeServiceException( res, "InvalidParameterValue", "Unknown GetCapabilities section = "+section);
            return;
          }
        }

        OutputStream os= res.getOutputStream();
        res.setContentType("text/xml");
        res.setStatus( HttpServletResponse.SC_OK );
        int len = wcsDataset.getCapabilities(os, sectionType);

        ServletUtil.logServerAccess(HttpServletResponse.SC_OK, len);
        os.flush();

      } else if (request.equals("DescribeCoverage")) {

        String[] coverages = ServletUtil.getParameterValuesIgnoreCase(req, "COVERAGE");
        if (coverages != null) {
          for (int i=0; i<coverages.length; i++) {
            if ( !wcsDataset.hasCoverage( coverages[i])) {
              makeServiceException( res, "CoverageNotDefined", "Unknown Coverage = "+coverages[i]);
              return;
            }
          }
        }

        OutputStream os= res.getOutputStream();
        res.setContentType("text/xml");
        res.setStatus( HttpServletResponse.SC_OK );
        int len = wcsDataset.describeCoverage(os, coverages);

        ServletUtil.logServerAccess(HttpServletResponse.SC_OK, len);
        os.flush();

      } else if (request.equals("GetCoverage")) {

        String coverage = ServletUtil.getParameterIgnoreCase(req, "COVERAGE");
        if (!wcsDataset.hasCoverage(coverage)) {
          makeServiceException(res, "CoverageNotDefined", "Unknown Coverage = " + coverage);
          return;
        }

        String bbox = ServletUtil.getParameterIgnoreCase(req, "BBOX");
        String time = ServletUtil.getParameterIgnoreCase(req, "TIME");
        String vertical = ServletUtil.getParameterIgnoreCase(req, "Vertical");
        String format = ServletUtil.getParameterIgnoreCase(req, "FORMAT");
        GetCoverageRequest r;
        try {
          r = new GetCoverageRequest( coverage, bbox, time, vertical, format);
        } catch (Exception e) {
          makeServiceException(res, "InvalidParameterValue", "query="+req.getQueryString());
          return;
        }

        if ((r.getFormat() == null) || (r.getFormat() == GetCoverageRequest.Format.NONE)) {
          makeServiceException(res, "InvalidFormat", "Invalid Format = " + format);
          return;
        }

        String errMessage;
        if (null != (errMessage = wcsDataset.checkCoverageParameters( r))) {
          makeServiceException(res, "InvalidParameterValue", errMessage);
          return;
        }

        File result = wcsDataset.getCoverage( r);
        ServletUtil.returnFile(this, "", result.getPath(), req, res, null);
        if (deleteImmediately) result.delete();

      } else {
        makeServiceException( res, "InvalidParameterValue", "Unknown request=" +request);
        return;
      }

    } catch (IOException ioe) {
      makeServiceException( res, "Invalid Dataset", ioe);
      return;

    } catch (Throwable t) {
      makeServiceException( res, "Server Error", t);
      t.printStackTrace();
      return;

    } finally {
      if (wcsDataset != null) {
        try {
          wcsDataset.close();
        } catch (IOException ioe) {
          log.error("Failed to close ", ioe);
        }
      }
    }
  }

  private void makeServiceException(HttpServletResponse res, String code, String message) throws IOException {
    res.setContentType("application/vnd.ogc.se_xml");
    PrintStream ps= new PrintStream(res.getOutputStream());

    ps.println("<ServiceExceptionReport version='1.2.0'>");
    ps.println("  <ServiceException code='"+code+"'>");
    ps.println("    "+message);
    ps.println("  </ServiceException>");
    ps.println("</ServiceExceptionReport>");

    ps.flush();
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1); // LOOK, actual return is 200 = OK !
  }

  private void makeServiceException(HttpServletResponse res, String code, Throwable t) throws IOException {
    res.setContentType("application/vnd.ogc.se_xml");
    PrintStream ps= new PrintStream(res.getOutputStream());

    ps.println("<ServiceExceptionReport version='1.2.0'>");
    ps.println("  <ServiceException code='"+code+"'>");

    if (Debug.isSet("trustedMode")) // security issue: only show stack if trusted
      t.printStackTrace(ps);
    else
      ps.println(t.getMessage());

    ps.println("  </ServiceException>");
    ps.println("</ServiceExceptionReport>");

    ps.flush();
    if (t instanceof FileNotFoundException)
      log.info("makeServiceException", t.getMessage()); // dont clutter up log files
    else
      log.info("makeServiceException", t);
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1); // LOOK, actual return is 200 = OK !
  }


}
