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
package thredds.server.cdmremote;

import org.springframework.web.servlet.mvc.LastModified;
import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import thredds.server.config.TdsContext;
import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;
import thredds.servlet.DataRootHandler;
import thredds.servlet.DatasetHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.stream.NcStream;

import java.io.*;
import java.util.Formatter;
import java.util.List;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;

/**
 * @author caron
 * @since Feb 16, 2009
 */
public class PointStreamControllerOld extends AbstractCommandController implements LastModified {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private boolean debug = false;

  private String prefix = "/point"; // LOOK how do we obtain this?
  private TdsContext tdsContext;

  public PointStreamControllerOld() {
    setCommandClass(CdmRemoteQueryBean.class);
    setCommandName("PointQueryBean");
  }

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  private boolean allow = true;

  public void setAllow(boolean allow) {
    this.allow = allow;
  }

  public long getLastModified(HttpServletRequest req) {
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(req.getPathInfo()); // LOOK
    if ((file != null) && file.exists())
      return file.lastModified();
    return -1;
  }

  protected ModelAndView handle(HttpServletRequest req, HttpServletResponse res, Object command, BindException errors) throws Exception {
    log.info(UsageLog.setupRequestContext(req));

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      return null;
    }

    String pathInfo = req.getPathInfo();
    String path = pathInfo.substring(0, pathInfo.length() - prefix.length());
    if (debug) System.out.printf("PointStreamController path= %s query= %s %n", path, req.getQueryString());

    CdmRemoteQueryBean query = (CdmRemoteQueryBean) command;
    if (debug) System.out.printf(" query= %s %n", query);
    if (!query.validate()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, query.getErrorMessage());
      if (debug) System.out.printf(" query error= %s %n", query.getErrorMessage());
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
      return null;
    }

    String queryS = req.getQueryString();

    NetcdfDataset ncd = null;
    FeatureDatasetPoint fd;
    PointFeatureCollection pfc;

    try {
      NetcdfFile ncfile = DatasetHandler.getNetcdfFile(req, res, path);
      if (ncfile == null) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
        return null;
      }

      ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll());
      Formatter errlog = new Formatter();
      fd = (FeatureDatasetPoint) FeatureDatasetFactoryManager.wrap(FeatureType.POINT, ncd, null, errlog);
      if (fd == null) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, errlog.toString());
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
        return null;
      }

      OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);
      if (queryS == null) {
        res.setContentType("text/plain");
        Formatter f = new Formatter(out);
        fd.getDetailInfo(f);
        f.flush();
        out.flush();

      } else if (queryS.equalsIgnoreCase("getCapabilities")) {
        // CdmRemoteControllerOld.sendCapabilities(req, out, fd.getFeatureType(), true);
        res.flushBuffer();
        out.flush();

      } else {
        res.setContentType("application/octet-stream");
        res.setHeader("Content-Description", "ncstream");

        if (true) { // query.wantHeader()) { // just the header
          NcStreamWriter ncWriter = new NcStreamWriter(ncd, ServletUtil.getRequestBase(req));
          WritableByteChannel wbc = Channels.newChannel(out);
          ncWriter.sendHeader(wbc);

        } else { // they want some data
          List<FeatureCollection> coll = fd.getPointFeatureCollectionList();
          pfc = (PointFeatureCollection) coll.get(0);

          if (query.getLatLonRect() != null) {
            pfc = pfc.subset(query.getLatLonRect(), null);
          }

          sendData(fd.getLocation(), pfc, out);
        }
        NcStream.writeVInt(out, 0);  // LOOK: terminator ?

        out.flush();
        res.flushBuffer();
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
      if (null != ncd)
        try {
          ncd.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + path);
        }
    }

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
    return null;
  }

  private void sendData(String location, PointFeatureCollection pfc, OutputStream out) throws IOException {
    int count = 0;
    PointFeatureIterator pfIter = pfc.getPointFeatureIterator(-1);
    try {
      while (pfIter.hasNext()) {
        PointFeature pf = pfIter.next();
        if (count == 0) {
          PointStreamProto.PointFeatureCollection proto = PointStream.encodePointFeatureCollection(location, pf);
          byte[] b = proto.toByteArray();
          NcStream.writeVInt(out, b.length);
          out.write(b);
        }

        PointStreamProto.PointFeature pfp = PointStream.encodePointFeature(pf);
        byte[] b = pfp.toByteArray();
        NcStream.writeVInt(out, b.length);
        out.write(b);
        count++;
      }
    } finally {
      pfIter.finish();
    }
    if (debug) System.out.printf(" sent %d features to %s %n ", count, location);
  }

}
