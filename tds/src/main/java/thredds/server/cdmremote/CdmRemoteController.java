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

import org.slf4j.Logger;
import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import thredds.server.config.TdsContext;
import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;
import thredds.servlet.DatasetHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Formatter;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.net.URLDecoder;

import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.ft.point.writer.FeatureDatasetPointXML;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.StringUtil;

/**
 * Controller for CdmRemote service.
 * At the moment, only handles station time series
 *
 * @author caron
 * @since May 28, 2009
 */
public class CdmRemoteController extends AbstractCommandController { // implements LastModified {
  private static final Logger logServerStartup = org.slf4j.LoggerFactory.getLogger( "serverStartup" );
  private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private static boolean debug = false, showTime = false, showReq=false;

  private TdsContext tdsContext;
  private boolean allow = true;
  private CollectionManager collectionManager;
  private DiskCache2 diskCache;

  public CdmRemoteController() {
    setCommandClass(PointQueryBean.class);
    setCommandName("PointQueryBean");
  }

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  public void setAllow(boolean allow) {
    this.allow = allow;
  }

  public void setDiskCache(DiskCache2 diskCache) {
    this.diskCache = diskCache;
  }

  public void setCollections(CollectionManager collectionManager) {
    this.collectionManager = collectionManager;
    Formatter f = new Formatter();
    f.format("CdmRemoteController collections%n");
    collectionManager.show(f);
    logServerStartup.info(f.toString());
  }


  /* public long getLastModified(HttpServletRequest req) {
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(req.getPathInfo()); // LOOK
    if ((file != null) && file.exists())
      return file.lastModified();
    return -1;
  } */

  protected ModelAndView handle(HttpServletRequest req, HttpServletResponse res, Object command, BindException errors) throws Exception {
    log.info(UsageLog.setupRequestContext(req));

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      return null;
    }

    // absolute path of the dataset endpoint
    String absPath = ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath() + req.getPathInfo();

    // ?? needed ??
    FeatureType ft = null;
    String path = req.getPathInfo();
    if (path.endsWith("station")) {
      ft = FeatureType.STATION;
      path = path.substring(0, path.length() - "station".length() - 1);
    }
    if (showReq)
      System.out.printf("CdmRemoteController req=%s%n", req.getRequestURI()+"?"+req.getQueryString());
    if (debug)
      System.out.printf("CdmRemoteController absPath= %s%n path=%s%n query=%s ft=%s%n", absPath, path, req.getQueryString(), ft);

    // query validation - first pass
    PointQueryBean query = (PointQueryBean) command;
    if (!query.validate()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, query.getErrorMessage());
      if (debug) System.out.printf(" query error= %s %n", query.getErrorMessage());
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
      return null;
    }
    if (debug) System.out.printf("%s%n", query);

    FeatureDatasetPoint fd = null;
    try {
      // try it as cdmRemoteFeatureDataset
      fd = collectionManager.getFeatureCollectionDataset(ServletUtil.getRequest(req), path);
      if (fd == null) {
        // try it as cdmRemote
        return handlCdmRequest(req, res, query, path, absPath);
      }

      PointQueryBean.RequestType reqType = query.getRequestType();
      PointQueryBean.ResponseType resType = query.getResponseType();
      switch (reqType) {
        case capabilities:
        case form:
          return processXml(req, res, fd, absPath, query);

        case header:
          return processHeader(absPath, res, fd, query);

        case dataForm:
        case data:
          return processData(req, res, fd, path, query);

        case stations:
          if (resType == PointQueryBean.ResponseType.xml)
            return processXml(req, res, fd, absPath, query);
          else
            return processStations(res, fd, query);
      }

    } catch (FileNotFoundException e) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
      return null;

    } catch (Throwable t) {
      log.error("CdmRemoteController exception:", t);
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
      return null;

    } finally {
      if (showReq)
        System.out.printf(" done%n");
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

  private String getContentType(PointQueryBean query) {
    PointQueryBean.RequestType reqType = query.getRequestType();
    if (reqType == PointQueryBean.RequestType.form)
      return "text/html; charset=iso-8859-1";

    PointQueryBean.ResponseType resType = query.getResponseType();
    switch (resType) {
      case csv:
        return "text/plain";
      case netcdf:
        return "application/x-netcdf";
      case ncstream:
        return "application/octet-stream";
      case xml:
        return "application/xml";
    }
    return "text/plain";
  }

  private String getContentDescription(PointQueryBean query) {
    PointQueryBean.ResponseType resType = query.getResponseType();
    switch (resType) {
      case ncstream:
        return "ncstream";
      default:
        return null;
    }
  }

  private ModelAndView processData(HttpServletRequest req, HttpServletResponse res, FeatureDatasetPoint fdp, String path, PointQueryBean qb) throws IOException {
    long start = 0;
    if (showTime) {
      start = System.currentTimeMillis();
      ucar.unidata.io.RandomAccessFile.setDebugAccess(true);  // LOOK !!
    }

    List<FeatureCollection> coll = fdp.getPointFeatureCollectionList();
    StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) coll.get(0);

    StationWriter stationWriter = new StationWriter(fdp, sfc, qb, diskCache);
    if (!stationWriter.validate(res)) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
      return null; // error was sent
    }

    // set content type, description
    res.setContentType(getContentType(qb));
    if (null != getContentDescription(qb))
      res.setHeader("Content-Description", getContentDescription(qb));

    // special handling for netcdf files
    PointQueryBean.ResponseType resType = qb.getResponseType();
    if (resType == PointQueryBean.ResponseType.netcdf) {
      if (path.startsWith("/")) path = path.substring(1);
      path = StringUtil.replace(path, "/", "-");
      res.setHeader("Content-Disposition", "attachment; filename=" + path + ".nc");

      File file = stationWriter.writeNetcdf();
      ServletUtil.returnFile(req, res, file, getContentType(qb));
      if (!file.delete()) {
        log.warn("file delete failed =" + file.getPath());
      }

      if (showTime) {
        long took = System.currentTimeMillis() - start;
        System.out.println("\ntotal response took = " + took + " msecs");
      }

      return null;
    }

    // otherwise stream it out
    StationWriter.Writer w = stationWriter.write(res);
    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));

    if (showTime) {
      long took = System.currentTimeMillis() - start;
      System.out.printf("%ntotal response took %d msecs nobs = %d%n  seeks= %d nbytes read= %d%n", took, w.count,
              ucar.unidata.io.RandomAccessFile.getDebugNseeks(), ucar.unidata.io.RandomAccessFile.getDebugNbytes());
    }

    return null;
  }

  private ModelAndView processStations(HttpServletResponse res, FeatureDatasetPoint fdp, PointQueryBean query) throws IOException {

    OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);
    res.setContentType(getContentType(query));
    if (null != getContentDescription(query))
      res.setHeader("Content-Description", getContentDescription(query));

    try {
      List<FeatureCollection> coll = fdp.getPointFeatureCollectionList();
      StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) coll.get(0);

      List<Station> stations;
      if (query.getLatLonRect() != null)
        stations = sfc.getStations(query.getLatLonRect());
      else if (query.getStnNames() != null)
        stations = sfc.getStations(Arrays.asList(query.getStnNames()));
      else
        stations = sfc.getStations();

      PointStreamProto.StationList stationsp = PointStream.encodeStations(stations);
      byte[] b = stationsp.toByteArray();
      PointStream.writeMagic(out, PointStream.MessageType.StationList);
      NcStream.writeVInt(out, b.length);
      out.write(b);
      
    } catch (Throwable t) {
      NcStreamProto.Error err = NcStream.encodeErrorMessage( t.getMessage());
      byte[] b = err.toByteArray();
      PointStream.writeMagic(out, PointStream.MessageType.Error);
      NcStream.writeVInt(out, b.length);
      out.write(b);
      
      throw new IOException(t);
    }

    out.flush();
    res.flushBuffer();

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
    return null;
  }

  private ModelAndView processHeader(String absPath, HttpServletResponse res, FeatureDatasetPoint fdp, PointQueryBean query) throws IOException {

    OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);
    res.setContentType(getContentType(query));
    if (null != getContentDescription(query))
      res.setHeader("Content-Description", getContentDescription(query));

    NetcdfFile ncfile = fdp.getNetcdfFile(); // LOOK will fail
    NcStreamWriter ncWriter = new NcStreamWriter(ncfile, absPath);
    WritableByteChannel wbc = Channels.newChannel(out);
    long size = ncWriter.sendHeader(wbc);
    NcStream.writeVInt(out, 0);

    out.flush();
    res.flushBuffer();

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, size+1));
    return null;
  }

  private ModelAndView processXml(HttpServletRequest req, HttpServletResponse res, FeatureDatasetPoint fdp, String absPath, PointQueryBean query) throws IOException {

    //String path = ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath() + datasetPath;
    FeatureDatasetPointXML xmlWriter = new FeatureDatasetPointXML(fdp, absPath);

    PointQueryBean.RequestType reqType = query.getRequestType();
    String infoString;
    if (reqType == PointQueryBean.RequestType.capabilities) {
      Document doc = xmlWriter.getCapabilitiesDocument();
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else if (reqType == PointQueryBean.RequestType.stations) {
      Document doc = xmlWriter.makeStationCollectionDocument(query.getLatLonRect(), query.getStnNames());
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else if (reqType == PointQueryBean.RequestType.form) {
      InputStream xslt = getXSLT("ncssSobs.xsl");
      Document doc = xmlWriter.getCapabilitiesDocument();

      try {
        XSLTransformer transformer = new XSLTransformer(xslt);
        Document html = transformer.transform(doc);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);

      } catch (Exception e) {
        log.error("SobsServlet internal error", e);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SobsServlet internal error");
        return null;
      }

    } else {
      return null;
    }

    res.setContentLength(infoString.length());
    res.setContentType(getContentType(query));

    OutputStream out = res.getOutputStream();
    out.write(infoString.getBytes());
    out.flush();

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, infoString.length()));
    return null;
  }

  private InputStream getXSLT(String xslName) {
    return getClass().getResourceAsStream("/resources/xsl/" + xslName);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected ModelAndView handlCdmRequest(HttpServletRequest req, HttpServletResponse res, PointQueryBean qb, String path,
                                         String absPath) throws Exception {

    NetcdfFile ncfile = null;
    try {
      ncfile = DatasetHandler.getNetcdfFile(req, res, path);
      if (ncfile == null) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
        return null;
      }

      OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);
      long size = -1;

      switch (qb.getRequestType()) {
        case capabilities:
          sendCapabilities(out, FeatureType.GRID, absPath); // LOOK: should default be GRID ?
          res.flushBuffer();
          log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
          return null;

        case form: // LOOK could do a ncss form
        case cdl:
          res.setContentType("text/plain");
          String cdl = ncfile.toString();
          res.setContentLength(cdl.length());
          byte[] b = cdl.getBytes("UTF-8");
          out.write(b);
          size = b.length;
          break;

        case ncml:
          res.setContentType("application/xml");
          ncfile.writeNcML(out, absPath);
          break;

        case header: {
          res.setContentType("application/octet-stream");
          res.setHeader("Content-Description", "ncstream");

          WritableByteChannel wbc = Channels.newChannel(out);
          NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(req));
          size = ncWriter.sendHeader(wbc);
          break;
        }

        default: {
          res.setContentType("application/octet-stream");
          res.setHeader("Content-Description", "ncstream");

          size = 0;
          WritableByteChannel wbc = Channels.newChannel(out);
          NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(req));
          String query = qb.getVar() != null ? qb.getVar() : req.getQueryString();
          if ((query == null) || (query.length() == 0)) {
            log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, 0));
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have query string");
            return null;
          }
          query = URLDecoder.decode(query, "UTF-8");
          StringTokenizer stoke = new StringTokenizer(query, ";"); // need UTF/%decode
          while (stoke.hasMoreTokens()) {
            ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ncfile, stoke.nextToken());
            size += ncWriter.sendData(cer.v, cer.section, wbc);
          }
        }
      } // end switch on req type

      out.flush();
      res.flushBuffer();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, size));

    } catch (FileNotFoundException e) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (IllegalArgumentException e) { // ParsedSectionSpec failed
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, 0));
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

    } catch (Throwable e) {
      e.printStackTrace();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

    } finally {
      if (null != ncfile)
        try {
          ncfile.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + path);
        }
    }

    return null;
  }

  private void sendCapabilities(OutputStream os, FeatureType ft, String absPath) throws IOException {
    Element rootElem = new Element("cdmRemoteCapabilities");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", absPath);
    Element elem = new Element("featureDataset");
    elem.setAttribute("type", ft.toString());
    elem.setAttribute("url", absPath);
    rootElem.addContent(elem);

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(doc, os);
  }

  /*  private ModelAndView sendCapabilities(HttpServletResponse res, NetcdfFile ncfile, String absPath, PointQueryBean query) throws IOException {

     NetcdfDataset ds = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll());
     Formatter errlog = new Formatter();
     try {
       FeatureDataset featureDataset = FeatureDatasetFactoryManager.wrap(null, ds, null, errlog);
       if (featureDataset != null) {
         FeatureType ft = featureDataset.getFeatureType();
         if (ft != null)
           ftype = featureType.toString();
       }
     } catch (Throwable t) {
     }

   this.fdp = fdp;

   List<FeatureCollection> list = fdp.getPointFeatureCollectionList();
   this.sobs = (StationTimeSeriesFeatureCollection) list.get(0);

   String infoString;
   Document doc = xmlWriter.getCapabilitiesDocument();
   XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
   infoString = fmt.outputString(doc);

   res.setContentLength(infoString.length());
   res.setContentType(getContentType(query));

   OutputStream out = res.getOutputStream();
   out.write(infoString.getBytes());
   out.flush();

   log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, infoString.length()));
   return null;
 } */


}


/* create it each time for thread safety, and so that collection is updated
private FeatureDatasetPoint getFeatureCollectionDataset(String uri, String path) throws IOException {

//FeatureDatasetPoint fd = fdmap.get(path);
//if (fd == null) {
  CollectionBean config = collectionDatasets.get(path);
  if (config == null) return null;

  Formatter errlog = new Formatter();
  FeatureDatasetPoint fd = (FeatureDatasetPoint) CompositeDatasetFactory.factory(uri, FeatureType.getType(config.getFeatureType()), config.getSpec(), errlog);
  if (fd == null) {
    log.error("Error opening CompositeDataset path = "+path+"  errlog = ", errlog);
    return null;
  }
  //fdmap.put(path, fd);
//}
return fd;
}

/* private FeatureDatasetPoint getFeatureCollectionDatasetOld(String path) throws IOException {

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
}  */

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
