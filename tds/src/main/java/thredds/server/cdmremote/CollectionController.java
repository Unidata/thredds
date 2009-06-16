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

import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.mvc.LastModified;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.servlet.UsageLog;
import thredds.servlet.DatasetHandler;
import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Formatter;
import java.util.List;
import java.util.HashMap;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.ft.point.collection.CompositeDatasetFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.stream.NcStream;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.Station;

/**
 * Describe
 *
 * @author caron
 * @since May 28, 2009
 */
public class CollectionController extends AbstractCommandController implements LastModified {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private boolean debug = true;

  private String prefix = "/collection"; // LOOK how do we obtain this?
  private TdsContext tdsContext;

  public CollectionController() {
    setCommandClass(PointQueryBean.class);
    setCommandName("PointQueryBean");
  }

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  private boolean allow = true;

  public void setAllow(boolean allow) {
    this.allow = allow;
  }


  private String configDirectory;

  public void setConfigDirectory(String configDirectory) {
    this.configDirectory = configDirectory;
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
    if (debug) System.out.printf("CollectionController path= %s query= %s %n", path, req.getQueryString());

    PointQueryBean query = (PointQueryBean) command;
    if (debug) System.out.printf(" query= %s %n", query);
    if (!query.parse()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, query.getErrorMessage());
      if (debug) System.out.printf(" query error= %s %n", query.getErrorMessage());
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
      return null;
    }

    String queryS = req.getQueryString();

    FeatureDatasetPoint fd = null;

    try {
      fd = getFeatureCollectionDataset(path);
      if (fd == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
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
        CdmRemoteController.sendCapabilities(req, out, fd, false);
        res.flushBuffer();
        out.flush();

      } else {
        res.setContentType("application/octet-stream");
        res.setHeader("Content-Description", "ncstream");

        List<FeatureCollection> coll = fd.getPointFeatureCollectionList();
        StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) coll.get(0);

        if (query.wantHeader()) { // just the header
          NetcdfFile ncfile = fd.getNetcdfFile(); // LOOK will fail
          NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(req));
          WritableByteChannel wbc = Channels.newChannel(out);
          ncWriter.sendHeader(wbc);

        } else if (query.wantStations()) { // just the station list
          PointStreamProto.StationList stationsp;
          if (query.getLatLonRect() == null)
            stationsp = PointStream.encodeStations(sfc.getStations());
          else
            stationsp = PointStream.encodeStations(sfc.getStations(query.getLatLonRect()));

          byte[] b = stationsp.toByteArray();
          NcStream.writeVInt(out, b.length);
          out.write(b);

        } else if (query.getStns() != null) { // a list of stations
          String[] names = query.getStns().split(",");
          List<Station> stns = sfc.getStations(names);
          for (Station s : stns) {
            StationTimeSeriesFeature series = sfc.getStationFeature(s);
            sendData(s.getName(), series, out);
          }

        } else { // they want some data
          PointFeatureCollection pfc = sfc.flatten(query.getLatLonRect(), query.getDateRange());
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
      if (null != fd)
        try {
          fd.close();
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
        if (count == 0) {  // first time
          PointStreamProto.PointFeatureCollection proto = PointStream.encodePointFeatureCollection(location, pf);
          byte[] b = proto.toByteArray();
          NcStream.writeVInt(out, b.length);
          out.write(b);
        }

        PointStreamProto.PointFeature pfp = PointStream.encodePointFeature(pf);
        byte[] b = pfp.toByteArray();
        NcStream.writeVInt(out, b.length);
        out.write(b);
        //System.out.println(" CollectionController len= " + b.length+ " count = "+count);

        count++;
      }
    } finally {
      pfIter.finish();
    }
    if (debug) System.out.printf(" sent %d features to %s %n ", count, location);
  }

  private HashMap<String, FeatureDatasetPoint> fdmap = new HashMap<String, FeatureDatasetPoint>();

  private FeatureDatasetPoint getFeatureCollectionDataset(String path) throws IOException {
    FeatureDatasetPoint fd = fdmap.get(path);
    if (fd == null) {
      File content = tdsContext.getContentDirectory();
      File config = new File(content, path);
      if (!config.exists()) {
        log.error("Config file %s doesnt exists %n", config);
        return null;
      }

      //fd = (FeatureDatasetPoint) CompositeDatasetFactory.factory(FeatureType.STATION, "D:/formats/gempak/surface/*.gem?#yyyyMMdd");
      Formatter errlog = new Formatter();
      fd = (FeatureDatasetPoint) CompositeDatasetFactory.factory(path, config, errlog);
      if (fd == null) {
        log.error("Error opening dataset error =", errlog);
        return null;
      }
      fdmap.put(path, fd);
    }
    return fd;
  }


  // one could use this for non-collection datasets
  private FeatureDatasetPoint getFeatureDataset(HttpServletRequest req, HttpServletResponse res, String path) throws IOException {
    NetcdfDataset ncd = null;
    try {
      NetcdfFile ncfile = DatasetHandler.getNetcdfFile(req, res, path);
      if (ncfile == null) {
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll());
      Formatter errlog = new Formatter();
      FeatureDatasetPoint fd = (FeatureDatasetPoint) FeatureDatasetFactoryManager.wrap(FeatureType.STATION, ncd, null, errlog);
      if (fd == null) {
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, errlog.toString());
        if (ncd != null) ncd.close();
        return null;
      }

      return fd;

    } catch (FileNotFoundException e) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (Throwable e) {
      e.printStackTrace();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    if (ncd != null) ncd.close();
    return null;
  }
}
