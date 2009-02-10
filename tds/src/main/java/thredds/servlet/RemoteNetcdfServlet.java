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
package thredds.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.StringTokenizer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;

import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.stream.NcStreamWriter;

/**
 * Experimental "remote netcdf streaming" data transfer protocol.
 * <pre>
 * Return NcML to populate the NetcdfFile:
 *   http://localhost:8080/thredds/ncremote/test/testData.nc
 *
 * Return binary data, using direct channel transfer to socket outputStream:
 *   http://localhost:8080/thredds/ncremote/test/testData.nc?Z_sfc(0:0,0:94,0:134)
 * </pre>
 *
 * At the moment its just a faster opendap. possible extensions:
 * <pre>
 * Index with coordinate values:
 *   http://localhost:8080/thredds/ncremote/test/testData.nc?Z_sfc(12.3,1000.0:1200.1,500.0:800.9)
 *   http://localhost:8080/thredds/ncremote/test/testData.nc?Z_sfc&alt=12.3m&lat=34.6:40.7&lon=66.6:69.9
 *
 * FeatureDatasets
 *   http://localhost:8080/thredds/netcdf/stream/test/testData.nc?lat=34.6:40.7&lon=66.6:69.9&featureType=pointData
 *   http://localhost:8080/thredds/netcdf/stream/test/testData.nc?var=sfc,P,T&lat=34.6:40.7&lon=66.6:69.9&featureType=pointData
 * </pre>
 * @author caron
 */
public class RemoteNetcdfServlet extends AbstractServlet {
  private boolean allow = true;

  @Override
  protected String getPath() {
    return "ncstream/";
  }

  @Override
  protected void makeDebugActions() {
  }


  @Override
  public void init() throws ServletException {
    super.init();
  }

  @Override
  protected long getLastModified(HttpServletRequest req) {
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(req.getPathInfo());
    if ((file != null) && file.exists())
      return file.lastModified();
    return -1;
  }

  // we need an efficient HEAD processing so we can disambiguate http URLs on the client
  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    log.info( UsageLog.setupRequestContext(req));

    NetcdfFile ncfile = null;
    try {
      ncfile = DatasetHandler.getNetcdfFile(req, res, req.getPathInfo());
      res.setContentType("application/octet-stream");
      res.setHeader("Content-Description", "ncstream");
      if (ncfile == null) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
      } else {
        res.setStatus(HttpServletResponse.SC_OK);
        ServletUtil.logServerAccess(HttpServletResponse.SC_OK, -1);
      }

    } finally {
      if (null != ncfile)
        try {
          ncfile.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + req.getPathInfo());
        }
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    log.info( UsageLog.setupRequestContext(req));
    ServletUtil.showRequestDetail(this, req);

    String pathInfo = req.getPathInfo();
    System.out.println("req="+pathInfo);

    String query = req.getQueryString();
    if (query != null) System.out.println("req="+pathInfo);

    res.setContentType("application/octet-stream");
    res.setHeader("Content-Description", "ncstream");

    NetcdfFile ncfile = null;
    try {
      ncfile = DatasetHandler.getNetcdfFile(req, res, pathInfo);
      if (ncfile == null) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
      }

      OutputStream out = new BufferedOutputStream( res.getOutputStream(), 10 * 1000);
      NcStreamWriter ncWriter = new NcStreamWriter(ncfile, req.getRequestURI());
      if (query == null) { // just the header
        ncWriter.sendHeader(out);

      } else { // they want some data
        WritableByteChannel wbc = Channels.newChannel(out);

        StringTokenizer stoke = new StringTokenizer(query, ","); // need UTF/%decode
        while (stoke.hasMoreTokens()) {
          ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ncfile, stoke.nextToken());
          ncWriter.sendData(out, cer.v, cer.section, wbc);
        }
      }

      out.flush();
      res.flushBuffer();
      ServletUtil.logServerAccess(HttpServletResponse.SC_OK, -1);

    } catch (Throwable e) {
      e.printStackTrace();
      ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

    } finally {
      if (null != ncfile)
        try {
          ncfile.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + pathInfo);
        }
    }
  }

}
