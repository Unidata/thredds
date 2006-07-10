package thredds.wcs.servlet;

import thredds.wcs.WcsDataset;
import thredds.wcs.GetCoverageRequest;
import thredds.wcs.SectionType;
import thredds.catalog.InvCatalogImpl;
import thredds.servlet.*;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.dt.GridDataset;

/**
 * Servlet handles serving data via WCS 1.0.
 *
 */
public class WCSServlet extends AbstractServlet {
  // private HashMap datasetHash = new HashMap(20);
  private InvCatalogImpl rootCatalog;
  private String tempDir;

  // must end with "/"
  protected String getPath() { return "wcs/"; }
  protected void makeDebugActions() {}

  public void init() throws ServletException {
    super.init();

    try {
      tempDir = contentPath+"temp/";
      File tempDirFile = new File(tempDir);
      tempDirFile.mkdirs();

    } catch (Throwable t) {
      log.error("CatalogServlet init", t);
      t.printStackTrace();
    }
  }

  private WcsDataset openWcsDataset(HttpServletRequest req) throws IOException {
    String datasetURL = ServletUtil.getParameterIgnoreCase(req, "dataset");
    boolean isRemote = (datasetURL != null);
    String datasetPath = isRemote ? datasetURL : req.getPathInfo();

    // convert to a GridDataset
    GridDataset gd = isRemote ? ucar.nc2.dataset.grid.GridDataset.open(datasetPath) : DatasetHandler.openGridDataset( datasetPath);

    // convert to a WcsDataset
    WcsDataset ds = new WcsDataset(gd, datasetPath, isRemote);
    String setServerBase = ServletUtil.getRequestBase( req);
    ds.setServerBase( setServerBase);

    return ds;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    ServletUtil.logServerAccessSetup( req );

    if (Debug.isSet("showRequest"))
      log.debug("**WCS req=" + ServletUtil.getRequest(req));
    if (Debug.isSet("showRequestDetail"))
      log.debug( ServletUtil.showRequestDetail(this, req));

    // check on static or dynamic catalogs
    //if (DataRootHandler2.getInstance().processReqForCatalog( req, res))
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

      wcsDataset = openWcsDataset( req);

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
        wcsDataset.getCapabilities(os, sectionType);

        res.setStatus(HttpServletResponse.SC_OK);
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
        wcsDataset.describeCoverage(os, coverages);

        res.setStatus(HttpServletResponse.SC_OK);
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
        GetCoverageRequest r = new GetCoverageRequest( coverage, bbox, time, vertical, format);

        if ((r.getFormat() == null) || (r.getFormat() == GetCoverageRequest.Format.NONE)) {
          makeServiceException(res, "InvalidFormat", "Invalid Format = " + format);
          return;
        }

        String errMessage = null;
        if (null != (errMessage = wcsDataset.checkCoverageParameters( r))) {
          makeServiceException(res, "InvalidParameterValue", errMessage);
          return;
        }

        String filename = wcsDataset.getCoverage( r);
        ServletUtil.returnFile(this, "", filename, req, res, null);

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
        } catch (IOException ioe) { }
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
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1); // LOOK, actual return is 200 = OK !
  }


}
