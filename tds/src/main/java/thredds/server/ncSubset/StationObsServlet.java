/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.server.ncSubset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;

import thredds.servlet.*;
import ucar.nc2.units.DateRange;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;

/**
 * Netcdf StationObs subsetting.
 *
 * @author caron
 */
public class StationObsServlet extends AbstractServlet {

  private boolean allow = false;
  private StationObsCollection soc;
  private boolean debug = false, showTime = false;

  // must end with "/"
  protected String getPath() {
    return "ncss/metars/";
  }

  protected void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("NetcdfSubsetServer");
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
    super.init();

    allow = ThreddsConfig.getBoolean("NetcdfSubsetService.allow", false);
    if (!allow) return;

    String metarDir = ThreddsConfig.get("NetcdfSubsetService.metarDataDir", "/opt/tomcat/content/thredds/public/stn/");
    File dir = new File(metarDir);
    if (!dir.exists()) {
      allow = false;
      return;
    }

    String metarRawDir = ThreddsConfig.get("NetcdfSubsetService.metarRawDir", "/data/ldm/pub/decoded/netcdf/surface/metar/");
    File rawDir = new File(metarRawDir);
    if (!rawDir.exists()) {
      metarRawDir = null;
    }
    soc = new StationObsCollection(metarDir, metarRawDir);
  }

  public void destroy() {
    super.destroy();
    if (null != soc)
      soc.close();
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }
    if (!soc.isReady()) {
      res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Temporarily Unavailable");
      return;
    }

    long start = System.currentTimeMillis();

    log.info( UsageLog.setupInfo(req));
    if (debug) System.out.println(req.getQueryString());

    String pathInfo = req.getPathInfo();
    if (pathInfo == null) pathInfo = "";

    boolean wantXML = pathInfo.endsWith("dataset.xml");
    boolean showForm = pathInfo.endsWith("dataset.html");
    boolean wantStationXML = pathInfo.endsWith("stations.xml");
    if (wantXML || showForm || wantStationXML) {
      showForm(res, wantXML, wantStationXML);
      return;
    }

    // parse the input
    QueryParams qp = new QueryParams();
    if (!qp.parseQuery(req, res, new String[]{QueryParams.RAW, QueryParams.CSV, QueryParams.XML, QueryParams.NETCDF, QueryParams.NETCDFS}))
      return; // has sent the error message

    if (qp.hasBB) {
      qp.stns = soc.getStationNames(qp.getBB());
      if (qp.stns.size() == 0) {
        qp.errs.append("ERROR: Bounding Box contains no stations\n");
        qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
    }

    if (qp.hasStns && soc.isStationListEmpty(qp.stns)) {
      qp.errs.append("ERROR: No valid stations specified\n");
      qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (qp.hasLatlonPoint) {
      qp.stns = new ArrayList<String>();
      qp.stns.add(soc.findClosestStation(qp.lat, qp.lon));
    } else if (qp.fatal) {
      qp.errs.append("ERROR: No valid stations specified\n");
      qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    boolean useAllStations = (!qp.hasBB && !qp.hasStns && !qp.hasLatlonPoint);
    if (useAllStations)
      qp.stns = new ArrayList<String>(); // empty list denotes all

    if (qp.hasTimePoint && (soc.filterDataset(qp.time) == null)) {
      qp.errs.append("ERROR: This dataset does not contain the time point= " + qp.time + " \n");
      qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (qp.hasDateRange) {
      DateRange dr = qp.getDateRange();
      if (!soc.intersect(dr)) {
        qp.errs.append("ERROR: This dataset does not contain the time range= " + qp.time + " \n");
        qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      if (debug) System.out.println(" date range= "+dr);
    }

    boolean useAllTimes = (!qp.hasTimePoint && !qp.hasDateRange);
    if (useAllStations && useAllTimes) {
      qp.errs.append("ERROR: You must subset by space or time\n");
      qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // set content type
    String contentType = qp.acceptType;
    if (qp.acceptType.equals(QueryParams.CSV))
      contentType = "text/plain"; // LOOK why
    res.setContentType(contentType);

    if (qp.acceptType.equals(QueryParams.NETCDF)) {
      res.setHeader("Content-Disposition", "attachment; filename=metarSubset.nc");
      File file = soc.writeNetcdf(qp);
      ServletUtil.returnFile(this, req, res, file, QueryParams.NETCDF);
      file.delete();

      if (showTime) {
        long took = System.currentTimeMillis() - start;
        System.out.println("\ntotal response took = " + took + " msecs");
      }
      return;
    }

    soc.write(qp, res);
    log.info( UsageLog.accessInfo(HttpServletResponse.SC_OK, -1));

    if (showTime) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\ntotal response took = " + took + " msecs");
    }
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
        log.info( UsageLog.accessInfo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
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

    log.info( UsageLog.accessInfo(HttpServletResponse.SC_OK, infoString.length()));
  }

  private InputStream getXSLT(String xslName) {
    return getClass().getResourceAsStream("/resources/xsl/" + xslName);
  }

}
