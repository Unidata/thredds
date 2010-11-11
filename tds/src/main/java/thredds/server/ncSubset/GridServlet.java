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

import thredds.servlet.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Random;

import ucar.nc2.util.DiskCache2;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridDatasetInfo;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.ma2.InvalidRangeException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;

/**
 * Netcdf Subset Service for Grids.
 *
 * @author caron
 */
public class GridServlet extends AbstractServlet {
  private ucar.nc2.util.DiskCache2 diskCache = null;
  private boolean allow = false, debug = false;

  // must end with "/"
  protected String getPath() {
    return "ncss/grid/";
  }

  protected void makeDebugActions() {
  }


  public void init() throws ServletException {
    super.init();

    /*   <NetcdfSubsetService>
    <allow>true</allow>
    <dir>/temp/ncache/</dir>
    <maxFileDownloadSize>1 Gb</maxFileDownloadSize>
  </NetcdfSubsetService> */

    allow = ThreddsConfig.getBoolean("NetcdfSubsetService.allow", false);
    logServerStartup.error("NetcdfSubsetService:allow= "+allow);
    if (!allow) return;

    //maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", (long) 1000 * 1000 * 1000);
    String cache = ThreddsConfig.get("NetcdfSubsetService.dir", contentPath + "/cache/ncss/");
    File cacheDir = new File(cache);
    if (!cacheDir.exists())  {
      if (!cacheDir.mkdirs()) {
        logServerStartup.error("Cant make cache directory "+cache);
        throw new IllegalArgumentException("Cant make cache directory "+cache);
      }
    }

    int scourSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.scour", 60 * 10);
    int maxAgeSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.maxAge", -1);
    maxAgeSecs = Math.max(maxAgeSecs, 60 * 5);  // give at least 5 minutes to download before scouring kicks in.
    scourSecs = Math.max(scourSecs, 60 * 5);  // always need to scour, in case user doesnt get the file, we need to clean it up

    // LOOK: what happens if we are still downloading when the disk scour starts?
    diskCache = new DiskCache2(cache, false, maxAgeSecs / 60, scourSecs / 60);
    logServerStartup.info(getClass().getName() + "Ncss.Cache= "+cache+" scour = "+scourSecs+" maxAgeSecs = "+maxAgeSecs);

    logServerStartup.info( getClass().getName() + " initialization done -  " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy() {
    logServerStartup.info( getClass().getName() + " destroy start -  " + UsageLog.setupNonRequestContext() );
    if (diskCache != null)
      diskCache.exit();
    super.destroy();
    logServerStartup.info( getClass().getName() + " destroy done -  " + UsageLog.closingMessageNonRequestContext() );
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }
    log.info( "doGet(): " + UsageLog.setupRequestContext(req));

    String pathInfo = req.getPathInfo();
    if ( pathInfo == null ) {
      log.info( "doGet(): Path info was null - " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError( HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // the forms and dataset description
    boolean wantXML = pathInfo.endsWith("/dataset.xml");
    boolean showForm = pathInfo.endsWith("/dataset.html");
    boolean showPointForm = pathInfo.endsWith("/pointDataset.html");
    if (wantXML || showForm || showPointForm) {
      int len = pathInfo.length();
      if (wantXML)
        pathInfo = pathInfo.substring(0, len - 12);
      else if (showForm)
        pathInfo = pathInfo.substring(0, len - 13);
      else if (showPointForm)
        pathInfo = pathInfo.substring(0, len - 18);

      if (pathInfo.startsWith("/"))
        pathInfo = pathInfo.substring(1);

      GridDataset gds = null;
      try {
        gds = DatasetHandler.openGridDataset(req, res, pathInfo);
        if (null == gds) {
          log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
          res.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
        }
        showForm(res, gds, pathInfo, wantXML, showPointForm);
        //log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, 0));

      } catch (java.io.FileNotFoundException ioe) {
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
        if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_NOT_FOUND);

      } catch (Exception e) {
        log.error("GridServlet.showForm", e);
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
        if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        
      } finally {
        if (null != gds)
          try {
            gds.close();
          } catch (IOException ioe) {
            log.error("Failed to close = " + pathInfo);
          }
      }

      return;
    }

    // otherwise assume its a data request

    String point = ServletUtil.getParameterIgnoreCase(req, "point");
    if (point != null && (point.equalsIgnoreCase("true"))) {
      processGridAsPoint(req, res, pathInfo);
      return;
    }

    processGrid(req, res, pathInfo);
  }

  private void processGridAsPoint(HttpServletRequest req, HttpServletResponse res, String pathInfo) throws IOException {
    long start = System.currentTimeMillis();

    GridDataset gds = null;
    try {

      // parse the input
      QueryParams qp = new QueryParams();
      if (!qp.parseQuery(req, res, new String[]{QueryParams.CSV, QueryParams.XML, QueryParams.NETCDF}))
        return; // has sent the error message

      try {
        gds = DatasetHandler.openGridDataset(req, res, pathInfo);
        if (null == gds) {
          log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
          res.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
        }

      } catch (FileNotFoundException e) {
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "Cant find " + pathInfo);
        return;
      }

      // make sure some variables requested
      if ((qp.vars == null) || (qp.vars.size() == 0)){
          qp.errs.append("No Grid variables selected\n");
          qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
      }

      // make sure all requested variables exist
      if (qp.vars != null) {
        int count = 0;
        StringBuilder buff = new StringBuilder();
        for (String varName : qp.vars) {
          if (null == gds.findGridDatatype(varName)) {
            buff.append(varName);
            if (count > 0) buff.append(";");
            count++;
          }
        }
        if (buff.length() != 0) {
          qp.errs.append("Grid variable(s) not found in dataset=" + buff + "\n");
          qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
      } else {
        qp.errs.append("You must specify at least one variable\n");
        qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      // must specify a lat/lon point
      if (qp.hasLatlonPoint) {
        LatLonRect bb = gds.getBoundingBox();
        LatLonPoint pt = qp.getPoint();
        if (!bb.contains(pt)) {
          qp.errs.append("Requested Lat/Lon Point (+" + pt + ") is not contained in the Data\n" +
                  "Data Bounding Box = " + bb.toString2() + "\n");
          qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
      } else {
        qp.errs.append("Must specify a Lat/Lon Point\n");
        qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      GridPointWriter writer = new GridPointWriter(gds, diskCache);

      // set content type
      String contentType = qp.acceptType;
      if (qp.acceptType.equals(QueryParams.CSV))
        contentType = "text/plain"; // LOOK why
      res.setContentType(contentType);

      try {

        if (!qp.acceptType.equals(QueryParams.NETCDF)) {
          writer.write(qp, res.getWriter());
          if (debug) {
            long took = System.currentTimeMillis() - start;
            System.out.println("\ntotal response took = " + took + " msecs");
          }
          
          log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
          return;
        }

        sendPointFile(req, res, gds, qp);

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, 0));
        if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Lat/Lon or Time Range");
      }

    } catch (Throwable e) {
      log.error("GridServlet.processGridAsPoint", e);
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    } finally {
      if (null != gds)
        try {
          gds.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + pathInfo);
        }
    }

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\ntotal response took = " + took + " msecs");
    }
  }

  private void sendPointFile(HttpServletRequest req, HttpServletResponse res, GridDataset gds, QueryParams qp) throws IOException, InvalidRangeException {
    String filename = req.getRequestURI();
    int pos = filename.lastIndexOf("/");
    filename = filename.substring(pos + 1);
    if (!filename.endsWith(".nc"))
      filename = filename + ".nc";

    Random random = new Random(System.currentTimeMillis());
    int randomInt = random.nextInt();

    String pathname = Integer.toString(randomInt) + "/" + filename;
    File ncFile = diskCache.getCacheFile(pathname);
    String cacheFilename = ncFile.getPath();
    File result;

    String url = "/thredds/ncServer/cache/" + pathname;

    try {
      GridPointWriter writer = new GridPointWriter(gds, diskCache);
      PrintWriter pw = !qp.acceptType.equals(QueryParams.NETCDF) ? res.getWriter() : null;
      result = writer.write(qp, pw);

    } catch (IOException ioe) {
      log.error("Writing to " + cacheFilename, ioe);
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
      return;
    }

    res.addHeader("Content-Location", url);
    res.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    ServletUtil.returnFile(this, req, res, result, "application/x-netcdf");
  }

  /////////////////////////////////////////////////////////////////////////////////////////////

  private void processGrid(HttpServletRequest req, HttpServletResponse res, String pathInfo) throws IOException {
    long start = System.currentTimeMillis();
    GridDataset gds = null;

    try {

      // parse the input
      QueryParams qp = new QueryParams();
      if (!qp.parseQuery(req, res, new String[]{QueryParams.NETCDF}))
        return; // has sent the error message

      try {
        gds = DatasetHandler.openGridDataset(req, res, pathInfo);
        if (null == gds) {
          log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
          res.sendError(HttpServletResponse.SC_NOT_FOUND, "Cant find " + pathInfo);
          return;
        }

      } catch (FileNotFoundException e) {
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "Cant find " + pathInfo);
        return;
      }

      // make sure some variables requested
      if ((qp.vars == null) || (qp.vars.size() == 0)){
          qp.errs.append("No Grid variables selected\n");
          qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
      }

      // make sure all requested variables exist
      if (qp.vars != null) {
        int count = 0;
        StringBuilder buff = new StringBuilder();
        for (String varName : qp.vars) {
          if (null == gds.findGridDatatype(varName)) {
            buff.append(varName);
            if (count > 0) buff.append(";");
            count++;
          }
        }
        if (buff.length() != 0) {
          qp.errs.append("Grid variable(s) not found in dataset=" + buff + "\n");
          qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
      }

      boolean hasBB = false;
      if (qp.hasBB) {
        LatLonRect maxBB = gds.getBoundingBox();
        hasBB = !ucar.nc2.util.Misc.closeEnough(qp.north, maxBB.getUpperRightPoint().getLatitude()) ||
                !ucar.nc2.util.Misc.closeEnough(qp.south, maxBB.getLowerLeftPoint().getLatitude()) ||
                !ucar.nc2.util.Misc.closeEnough(qp.east, maxBB.getUpperRightPoint().getLongitude()) ||
                !ucar.nc2.util.Misc.closeEnough(qp.west, maxBB.getLowerLeftPoint().getLongitude());

        if (maxBB.intersect(qp.getBB()) == null) {
          qp.errs.append("Request Bounding Box does not intersect the Data\n" +
                  "Data Bounding Box = " + maxBB.toString2() + "\n");
          qp.writeErr(req, res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
      }

      boolean addLatLon = ServletUtil.getParameterIgnoreCase(req, "addLatLon") != null;

      try {
        sendFile(req, res, gds, qp, hasBB, addLatLon);
      } catch (InvalidRangeException e) {
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, 0));
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Lat/Lon or Time Range: "+e.getMessage());
      }

    } catch (Throwable e) {
      log.error("GridServlet.processGrid", e);
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    } finally {
      if (null != gds)
        try {
          gds.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + pathInfo);
        }
    }

    if (log.isDebugEnabled()) {
      long took = System.currentTimeMillis() - start;
      log.debug(" total response took = " + took + " msecs");
    }
  }

  private void sendFile(HttpServletRequest req, HttpServletResponse res, GridDataset gds, QueryParams qp,
          boolean useBB, boolean addLatLon) throws IOException, InvalidRangeException {


    String filename = req.getRequestURI();
    int pos = filename.lastIndexOf("/");
    filename = filename.substring(pos + 1);
    if (!filename.endsWith(".nc"))
      filename = filename + ".nc";

    Random random = new Random(System.currentTimeMillis());
    int randomInt = random.nextInt();

    String pathname = Integer.toString(randomInt) + "/" + filename;
    File ncFile = diskCache.getCacheFile(pathname);
    String cacheFilename = ncFile.getPath();

    String url = "/thredds/ncServer/cache/" + pathname;

    try {
      NetcdfCFWriter writer = new NetcdfCFWriter();
      writer.makeFile(cacheFilename, gds, qp.vars,
              useBB ? qp.getBB() : null,
              qp.hasDateRange ? qp.getDateRange() : null,
              addLatLon, qp.horizStride, qp.vertStride, qp.timeStride);

    } catch (IllegalArgumentException e) { // file too big
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, 0));
      res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
      return;

    } catch (Exception ioe) {
      log.error("Writing to " + cacheFilename, ioe);
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
      return;
    }


    res.addHeader("Content-Location", url);
    res.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    ServletUtil.returnFile(this, req, res, new File(cacheFilename), "application/x-netcdf");
  }

  private void showForm(HttpServletResponse res, GridDataset gds, String path, boolean wantXml, boolean isPoint) throws IOException {
    String infoString;
    GridDatasetInfo writer = new GridDatasetInfo(gds, "path");

    if (wantXml) {
      infoString = writer.writeXML(writer.makeDatasetDescription());

    } else {
      InputStream xslt = getXSLT(isPoint ? "ncssGridAsPoint.xsl" : "ncssGrid.xsl");
      Document doc = writer.makeGridForm();
      Element root = doc.getRootElement();
      root.setAttribute("location", "/thredds/" + getPath() + path);

      try {
        XSLTransformer transformer = new XSLTransformer(xslt);
        Document html = transformer.transform(doc);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);

      } catch (Exception e) {
        log.error("ForecastModelRunServlet internal error", e);
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ForecastModelRunServlet internal error");
        return;
      }
    }

    res.setContentLength(infoString.length());
    if (wantXml)
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
