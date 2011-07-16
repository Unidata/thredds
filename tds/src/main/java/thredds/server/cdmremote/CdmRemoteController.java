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

import ucar.unidata.util.EscapeStrings;
import org.slf4j.Logger;
import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.springframework.web.servlet.mvc.LastModified;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;
import thredds.servlet.DatasetHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.StringTokenizer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;

/**
 * Spring controller for CdmRemote service.
 *
 * @author caron
 * @since May 28, 2009
 */
public class CdmRemoteController extends AbstractCommandController implements LastModified {
  private static final Logger logServerStartup = org.slf4j.LoggerFactory.getLogger( "serverStartup" );
  private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private static boolean debug = false, showTime = false, showReq=false;

  private TdsContext tdsContext;
  private boolean allow = true;

  public CdmRemoteController() {
    setCommandClass(CdmRemoteQueryBean.class);
    setCommandName("PointQueryBean");
  }

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  public void setAllow(boolean allow) {
    this.allow = allow;
  }

  @Override
  public long getLastModified(HttpServletRequest req) {
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(req.getPathInfo());
    if ((file != null) && file.exists())
      return file.lastModified();
    return -1;
  }

  @Override
  protected ModelAndView handle(HttpServletRequest req, HttpServletResponse res, Object command, BindException errors) throws Exception {
    log.info(UsageLog.setupRequestContext(req));

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      return null;
    }

    String absPath = ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath() + req.getPathInfo();
    String path = req.getPathInfo();

    if (showReq)
      System.out.printf("CdmRemoteController req=%s%n", absPath+"?"+req.getQueryString());
    if (debug) {
      System.out.printf(" path=%s%n query=%s%n", path, req.getQueryString());
    }

    // query validation - first pass
    CdmRemoteQueryBean qb = (CdmRemoteQueryBean) command;
    if (!qb.validate()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, qb.getErrorMessage());
      if (debug) System.out.printf(" query error= %s %n", qb.getErrorMessage());
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
      return null;
    }
    if (debug) System.out.printf(" %s%n", qb);

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
          sendCapabilities(out, FeatureType.GRID, absPath); // LOOK BAD - must figure out what is the featureType
          res.flushBuffer();
          log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
          return null;

        case form: // LOOK could do a ncss style form
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
          String query;
          if(qb.getVar() != null)
              query = qb.getVar();
          else
              query = req.getQueryString();

          if ((query == null) || (query.length() == 0)) {
            log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, 0));
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have query string");
            return null;
          }
          query = EscapeStrings.unescapeURLQuery(query);
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
      if (showReq)
        System.out.printf("CdmRemoteController ok, size=%s%n", size);

    } catch (FileNotFoundException e) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (IllegalArgumentException e) { // ParsedSectionSpec failed
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, 0));
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

    } catch (InvalidRangeException e) { // ParsedSectionSpec failed
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, 0));
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

    } catch (Throwable e) {
      log.error(e.getMessage(), e);
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
