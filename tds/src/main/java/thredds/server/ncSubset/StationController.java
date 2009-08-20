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
package thredds.server.ncSubset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;

import thredds.servlet.*;
import thredds.server.config.TdsContext;
import ucar.nc2.units.DateRange;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

/**
 * @deprecated use cdmremote
 */
public class StationController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  private static org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");

  private StationObsCollection soc;
  private boolean debug = false, showTime = false;

  private TdsContext tdsContext;
  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  private boolean allow = false;
  public void setAllow( boolean allow) {
    this.allow = allow;
  }

  protected void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("StationController");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showMetarFiles", "Show Metar Files") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println("Metar Files\n");
        ArrayList<StationObsCollection.Dataset> list = soc.getDatasets();
        for (StationObsCollection.Dataset ds : list) {
          e.pw.println(" " + ds);
        }
      }
    };
    debugHandler.addAction(act);
  }

  public void init() throws ServletException {
    logServerStartup.info( getClass().getName() + " initialization start - " + UsageLog.setupNonRequestContext());

    /* String metarDir = ThreddsConfig.get("NetcdfSubsetService.metarDataDir", "/opt/tomcat/content/thredds/public/stn/");
    File dir = new File(metarDir);
    if (!dir.exists()) {
      allow = false;
      return;
    }

    String metarRawDir = ThreddsConfig.get("NetcdfSubsetService.metarRawDir", null);
    File rawDir = new File(metarRawDir);
    if (!rawDir.exists()) {
      metarRawDir = null;
    }
    soc = new StationObsCollection(metarDir, metarRawDir); */

    logServerStartup.info( getClass().getName() + " initialization done - " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy() {
    logServerStartup.info( getClass().getName() + " destroy start - " + UsageLog.setupNonRequestContext() );
    if (null != soc)
      soc.close();
    logServerStartup.info( getClass().getName() + " destroy done - " + UsageLog.closingMessageNonRequestContext() );
  }
  

  public long getLastModified(HttpServletRequest req) {
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(req.getPathInfo());
    if ((file != null) && file.exists())
      return file.lastModified();
    return -1;
  }


  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return null;
    }
    if (!soc.isReady()) {
      res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Temporarily Unavailable");
      return null;
    }

    long start = System.currentTimeMillis();

    log.info( UsageLog.setupRequestContext(req));
    if (debug) System.out.println(req.getQueryString());

    String pathInfo = req.getPathInfo();
    if (pathInfo == null) pathInfo = "";

    boolean wantXML = pathInfo.endsWith("dataset.xml");
    boolean showForm = pathInfo.endsWith("dataset.html");
    boolean wantStationXML = pathInfo.endsWith("stations.xml");
    if (wantXML || showForm || wantStationXML) {
      showForm(res, wantXML, wantStationXML);
      return null;
    }

    // parse the input
    QueryParams qp = new QueryParams();
    if (!qp.parseQuery(req, res, new String[]{QueryParams.RAW, QueryParams.CSV, QueryParams.XML, QueryParams.NETCDF, QueryParams.NETCDFS, QueryParams.CdmRemote}))
      return null; // has sent the error message

    if (qp.hasBB) {
      qp.stns = soc.getStationNames(qp.getBB());
      if (qp.stns.size() == 0) {
        qp.errs.append("ERROR: Bounding Box contains no stations\n");
        qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return null;
      }
    }

    if (qp.hasStns && soc.isStationListEmpty(qp.stns)) {
      qp.errs.append("ERROR: No valid stations specified\n");
      qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return null;
    }

    if (qp.hasLatlonPoint) {
      qp.stns = new ArrayList<String>();
      qp.stns.add(soc.findClosestStation(qp.lat, qp.lon));
    } else if (qp.fatal) {
      qp.errs.append("ERROR: No valid stations specified\n");
      qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return null;
    }

    boolean useAllStations = (!qp.hasBB && !qp.hasStns && !qp.hasLatlonPoint);
    if (useAllStations)
      qp.stns = new ArrayList<String>(); // empty list denotes all

    if (qp.hasTimePoint && (soc.filterDataset(qp.time) == null)) {
      qp.errs.append("ERROR: This dataset does not contain the time point= " + qp.time + " \n");
      qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return null;
    }

    if (qp.hasDateRange) {
      DateRange dr = qp.getDateRange();
      if (!soc.intersect(dr)) {
        qp.errs.append("ERROR: This dataset does not contain the time range= " + qp.time + " \n");
        qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return null;
      }
      if (debug) System.out.println(" date range= "+dr);
    }

    boolean useAllTimes = (!qp.hasTimePoint && !qp.hasDateRange);
    if (useAllStations && useAllTimes) {
      qp.errs.append("ERROR: You must subset by space or time\n");
      qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return null;
    }

    // set content type
    String contentType = qp.acceptType;
    if (qp.acceptType.equals(QueryParams.CSV))
      contentType = "text/plain"; // LOOK why
    res.setContentType(contentType);

    if (qp.acceptType.equals(QueryParams.NETCDF)) {
      res.setHeader("Content-Disposition", "attachment; filename=metarSubset.nc");
      File file = soc.writeNetcdf(qp);
      ServletUtil.returnFile( req, res, file, QueryParams.NETCDF);
      file.delete();

      if (showTime) {
        long took = System.currentTimeMillis() - start;
        System.out.println("\ntotal response took = " + took + " msecs");
      }
      return null;
    }

    soc.write(qp, res);
    log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));

    if (showTime) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\ntotal response took = " + took + " msecs");
    }

    return null;
  }

  private void showForm(HttpServletResponse res, boolean wantXml, boolean wantStationXml) throws IOException {
    String infoString;

    if (wantXml) {
      Document doc = soc.getDoc();
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else if (wantStationXml) {
      Document doc = soc.getStationDoc();
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else {
      InputStream xslt = getXSLT("ncssSobs.xsl");
      Document doc = soc.getDoc();

      try {
        XSLTransformer transformer = new XSLTransformer(xslt);
        Document html = transformer.transform(doc);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);

      } catch (Exception e) {
        log.error("SobsServlet internal error", e);
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SobsServlet internal error");
        return;
      }
    }

    res.setContentLength(infoString.length());
    if (wantXml || wantStationXml)
      res.setContentType("text/xml; charset=iso-8859-1");
    else
      res.setContentType("text/html; charset=iso-8859-1");

    OutputStream out = res.getOutputStream();
    out.write(infoString.getBytes());
    out.flush();

    log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, infoString.length()));
  }

  private InputStream getXSLT(String xslName) {
    return getClass().getResourceAsStream("/resources/xsl/" + xslName);
  }

}
