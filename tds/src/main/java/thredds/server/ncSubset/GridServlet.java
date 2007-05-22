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

import thredds.servlet.AbstractServlet;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.ServletUtil;
import thredds.servlet.DatasetHandler;

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
  private boolean allow = false, deleteImmediately = true;
  private long maxFileDownloadSize;

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
    maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", (long) 1000 * 1000 * 1000);
    String cache = ThreddsConfig.get("NetcdfSubsetService.dir", contentPath + "/cache");
    File cacheDir = new File(cache);
    cacheDir.mkdirs();

    int scourSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.scour", 60 * 10);
    int maxAgeSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.maxAge", -1);
    maxAgeSecs = Math.max(maxAgeSecs, 60 * 5);  // give at least 5 minutes to download before scouring kicks in.
    scourSecs = Math.max(scourSecs, 60 * 5);  // always need to scour, in case user doesnt get the file, we need to clean it up

    // LOOK: what happens if we are still downloading when the disk scour starts?
    diskCache = new DiskCache2(cache, false, maxAgeSecs / 60, scourSecs / 60);
  }

  public void destroy() {
    if (diskCache != null)
      diskCache.exit();
    super.destroy();
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }
    long start = System.currentTimeMillis();

    ServletUtil.logServerAccessSetup(req);

    String pathInfo = null;
    GridDataset gds = null;
    try {
      pathInfo = req.getPathInfo();

      boolean wantXML = pathInfo.endsWith("/dataset.xml");  // LOOK, maybe accept = html, xml, no queries ??
      boolean showForm = pathInfo.endsWith("/dataset.html");
      if (wantXML || showForm) {
        int len = pathInfo.length();
        if (wantXML)
          pathInfo = pathInfo.substring(0, len - 12);
        else
          pathInfo = pathInfo.substring(0, len - 13);
        if (pathInfo.startsWith("/"))
          pathInfo = pathInfo.substring(1);

        try {
          gds = DatasetHandler.openGridDataset(req, res, pathInfo);
          showForm(res, gds, pathInfo, wantXML);
          return;
        } catch (Exception e) {
          log.error("showForm", e);
          ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
      }

      // parse the input
      QueryParams qp = new QueryParams();
      if (!qp.parseQuery(req, res, new String[]{QueryParams.NETCDF}))
        return; // has sent the error message
      System.out.println(qp);

      try {
        gds = DatasetHandler.openGridDataset(req, res, pathInfo);
      } catch (Exception e) {
        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "Cant find"+pathInfo);
      }

      // make sure all requested variables exist
      if (qp.vars != null) {
        int count = 0;
        StringBuffer buff = new StringBuffer();
        for (String varName : qp.vars) {
          if (null == gds.findGridDatatype(varName)) {
            buff.append(varName);
            if (count > 0) buff.append(";");
            count++;
          }
        }
        if (buff.length() != 0) {
          qp.errs.append("Grid variable(s) not found in dataset=" + buff+"\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
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
          qp.errs.append("Request Bounding Box does not intersect the Data\n"+
            "Data Bounding Box = "+maxBB.toString2()+"\n");
          qp.writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
      }

      // look for strides LOOK not implemented
      boolean hasStride = false;
      int stride_xy = -1;
      String s = ServletUtil.getParameterIgnoreCase(req, "stride_xy");
      if (s != null) {
        try {
          stride_xy = Integer.parseInt(s);
          hasStride = true;
        } catch (NumberFormatException e) {
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "stride_xy must have valid integer argument");
          return;
        }
      }
      int stride_z = -1;
      s = ServletUtil.getParameterIgnoreCase(req, "stride_z");
      if (s != null) {
        try {
          stride_z = Integer.parseInt(s);
          hasStride = true;
        } catch (NumberFormatException e) {
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "stride_z must have valid integer argument");
          return;
        }
      }
      int stride_time = -1;
      s = ServletUtil.getParameterIgnoreCase(req, "stride_time");
      if (s != null) {
        try {
          stride_time = Integer.parseInt(s);
          hasStride = true;
        } catch (NumberFormatException e) {
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "stride_time must have valid integer argument");
          return;
        }
      }

      boolean addLatLon = ServletUtil.getParameterIgnoreCase(req, "addLatLon") != null;

      try {
        sendFile(req, res, gds, qp, hasBB, addLatLon, stride_xy, stride_z, stride_time);
      } catch (InvalidRangeException e) {
        ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Lat/Lon or Time Range");
      }
    } finally {
      if (null != gds)
        try {
          gds.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + pathInfo);
        }
    }

    long took = System.currentTimeMillis() - start;
    System.out.println("\ntotal response took = " + took + " msecs");
  }

  private void sendFile(HttpServletRequest req, HttpServletResponse res, GridDataset gds, QueryParams qp,
          boolean useBB, boolean addLatLon,
          int stride_xy, int stride_z, int stride_time) throws IOException, InvalidRangeException {


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
          addLatLon, stride_xy, stride_z, stride_time); // this line not used

    } catch (IOException ioe) {
      log.error("Writing to " + cacheFilename, ioe);
      ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
      return;
    }

    res.addHeader("Content-Location", url);
    res.setHeader("Content-Disposition", "attachment; filename=" + filename);

    ServletUtil.returnFile(this, "", cacheFilename, req, res, "application/x-netcdf");
  }

  private void showForm(HttpServletResponse res, GridDataset gds, String path, boolean wantXml) throws IOException {
    String infoString;
    GridDatasetInfo writer = new GridDatasetInfo(gds, "path");

    if (wantXml) {
      infoString = writer.writeXML();

    } else {
      InputStream xslt = getXSLT("ncssGrid.xsl");
      Document doc = writer.makeDocument();
      Element root = doc.getRootElement();
      root.setAttribute("location", "/thredds/" + getPath() + path);

      try {
        XSLTransformer transformer = new XSLTransformer(xslt);
        Document html = transformer.transform(doc);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);

      } catch (Exception e) {
        log.error("ForecastModelRunServlet internal error", e);
        ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
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

    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, infoString.length());
  }

  private InputStream getXSLT(String xslName) {
    return getClass().getResourceAsStream("/resources/xsl/" + xslName);
  }

}
