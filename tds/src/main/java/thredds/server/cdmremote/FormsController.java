/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.cdmremote;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.jdom.Document;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import thredds.server.ncSubset.QueryParams;
import thredds.server.config.TdsContext;
import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

import ucar.nc2.units.DateRange;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.point.writer.FeatureDatasetPointXML;

/**
 * Describe
 *
 * @author caron
 * @since Aug 19, 2009
 */
public class FormsController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private static org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
  private String prefix = "/form";

  //private StationObsCollection soc;
  private boolean debug = true, showTime = false;

  private TdsContext tdsContext;

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  private boolean allow = false;

  public void setAllow(boolean allow) {
    this.allow = allow;
  }

  private CollectionManager collectionManager;

  public void setCollections(CollectionManager collectionManager) {
    this.collectionManager = collectionManager;
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return null;
    }

    log.info(UsageLog.setupRequestContext(req));

    String datasetPath = req.getPathInfo();
    if (datasetPath == null) datasetPath = "";
    String path = datasetPath.substring(0, datasetPath.length() - prefix.length());
    if (debug) System.out.printf("CollectionController path= %s query= %s %n", path, req.getQueryString());

    java.lang.String query = req.getQueryString();
    boolean hasQuery = (query != null) && (query.length() > 0);
    boolean wantDescXML = false;
    boolean wantStationXML = false;
    if (hasQuery) {
      String reqParam = req.getParameter("req");
      wantDescXML = (reqParam != null) && (reqParam.equalsIgnoreCase("getCapabilities"));
      wantStationXML = (reqParam != null) && (reqParam.equalsIgnoreCase("stations"));
    }

    FeatureDatasetPoint fd = null;
    try {
      fd = collectionManager.getFeatureCollectionDataset(ServletUtil.getRequest(req), path);
      if (fd == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
        return null;
      }

      if (!hasQuery || wantDescXML || wantStationXML) {
        showForm(req, res, wantDescXML, wantStationXML, fd, datasetPath);
        return null;
      } else {
        processRequest(req, res, fd);
      }

    } catch (FileNotFoundException e) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
      return null;

    } catch (Throwable e) {
      e.printStackTrace();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return null;

    } finally {
      if (null != fd)
        try {
          fd.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + path);
        }
    }

    return null;
  }

  private void processRequest(HttpServletRequest req, HttpServletResponse res, FeatureDatasetPoint fd) throws IOException {
    long start = System.currentTimeMillis();
    List<FeatureCollection> coll = fd.getPointFeatureCollectionList();
    StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) coll.get(0);

    StationWriter stationWriter = new StationWriter(fd, sfc);

    // parse the input
    QueryParams qp = new QueryParams();
    if (!qp.parseQuery(req, res, new String[]{QueryParams.RAW, QueryParams.CSV, QueryParams.XML, QueryParams.NETCDF, QueryParams.NETCDFS, QueryParams.CdmRemote}))
      return; // has sent the error message

    if (qp.hasBB) {
      qp.stns = stationWriter.getStationNames(qp.getBB());
      if (qp.stns.size() == 0) {
        qp.errs.append("ERROR: Bounding Box contains no stations\n");
        qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
    }

    if (qp.hasStns && stationWriter.isStationListEmpty(qp.stns)) {
      qp.errs.append("ERROR: No valid stations specified\n");
      qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (qp.hasLatlonPoint) {
      qp.stns = new ArrayList<String>();
      qp.stns.add(stationWriter.findClosestStation(qp.lat, qp.lon));
    } else if (qp.fatal) {
      qp.errs.append("ERROR: No valid stations specified\n");
      qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    boolean useAllStations = (!qp.hasBB && !qp.hasStns && !qp.hasLatlonPoint);
    if (useAllStations)
      qp.stns = new ArrayList<String>(); // empty list denotes all

    if (qp.hasTimePoint) { // && (soc.filterDataset(qp.time) == null)) {    LOOK commentedd out
      qp.errs.append("ERROR: This dataset does not contain the time point= " + qp.time + " \n");
      qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (qp.hasDateRange) {
      DateRange dr = qp.getDateRange();
      if (!stationWriter.intersect(dr)) {
        qp.errs.append("ERROR: This dataset does not contain the time range= " + qp.time + " \n");
        qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      if (debug) System.out.println(" date range= " + dr);
    }

    boolean useAllTimes = (!qp.hasTimePoint && !qp.hasDateRange);
    if (useAllStations && useAllTimes) {
      qp.errs.append("ERROR: You must subset by space or time\n");
      qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // set content type
    String contentType = qp.acceptType;
    if (qp.acceptType.equals(QueryParams.CSV))
      contentType = "text/plain"; // LOOK why
    res.setContentType(contentType);

    if (qp.acceptType.equals(QueryParams.NETCDF)) {
      res.setHeader("Content-Disposition", "attachment; filename=metarSubset.nc");
      File file = stationWriter.writeNetcdf(qp);
      ServletUtil.returnFile(req, res, file, QueryParams.NETCDF);
      file.delete();

      if (showTime) {
        long took = System.currentTimeMillis() - start;
        System.out.println("\ntotal response took = " + took + " msecs");
      }
      return;
    }

    stationWriter.write(qp, res);
    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));

    if (showTime) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\ntotal response took = " + took + " msecs");
    }

  }

  private void showForm(HttpServletRequest req, HttpServletResponse res, boolean wantXml, boolean wantStationXml,
                        FeatureDatasetPoint fdp, String datasetPath) throws IOException {

    String path = ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath() + datasetPath;
    FeatureDatasetPointXML xml = new FeatureDatasetPointXML(fdp, path);

    String infoString;
    if (wantXml) {
      Document doc = xml.getCapabilitiesDocument();
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else if (wantStationXml) {
      Document doc = xml.makeStationCollectionDocument();
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else {
      InputStream xslt = getXSLT("ncssSobs.xsl");
      Document doc = xml.getCapabilitiesDocument();

      try {
        XSLTransformer transformer = new XSLTransformer(xslt);
        Document html = transformer.transform(doc);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);

      } catch (Exception e) {
        log.error("SobsServlet internal error", e);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
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

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, infoString.length()));
  }

  private InputStream getXSLT(String xslName) {
    return getClass().getResourceAsStream("/resources/xsl/" + xslName);
  }

}

