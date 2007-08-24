package thredds.server.wcs;

import thredds.servlet.*;
import thredds.wcs.WcsDataset;
import thredds.wcs.SectionType;
import thredds.wcs.GetCoverageRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.*;

import ucar.nc2.util.DiskCache2;
import ucar.nc2.dt.GridDataset;

/**
 * Servlet handles serving data via WCS 1.0.
 *
 */
public class WCS_1_0 implements VersionHandler
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( WCS_1_0.class );

  private Version version;

  /**
   * Declare the default constructor to be package private.
   */
  WCS_1_0()
  {
    this.version = new Version( "1.0.0");
  }

  private WcsDataset openWcsDataset( HttpServletRequest req, HttpServletResponse res) throws IOException
  {
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

  public Version getVersion()
  {
    return this.version;  
  }

  public void handleKVP( HttpServlet servlet, HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException
  {

    if ( Debug.isSet("showRequest"))
      log.debug("**WCS req=" + ServletUtil.getRequest(req));
    if (Debug.isSet("showRequestDetail"))
      log.debug( ServletUtil.showRequestDetail(servlet, req));

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
        ServletUtil.returnFile(servlet, "", result.getPath(), req, res, null);
       // if (deleteImmediately) result.delete();

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
    res.setStatus( HttpServletResponse.SC_BAD_REQUEST );

    PrintStream ps= new PrintStream(res.getOutputStream());

    ps.println("<ServiceExceptionReport version='1.2.0'>");
    ps.println("  <ServiceException code='"+code+"'>");
    ps.println("    "+message);
    ps.println("  </ServiceException>");
    ps.println("</ServiceExceptionReport>");

    ps.flush();
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 ); // LOOK, actual return is 200 = OK !
  }

  private void makeServiceException(HttpServletResponse res, String code, Throwable t) throws IOException {
    res.setContentType("application/vnd.ogc.se_xml");
    res.setStatus( HttpServletResponse.SC_BAD_REQUEST );

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
    if (t instanceof FileNotFoundException )
      log.info("makeServiceException", t.getMessage()); // dont clutter up log files
    else
      log.info("makeServiceException", t);
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1); // LOOK, actual return is 200 = OK !
  }


}
