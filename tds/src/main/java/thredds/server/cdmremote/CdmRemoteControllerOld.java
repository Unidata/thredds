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
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.LastModified;
import org.springframework.web.servlet.ModelAndView;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import thredds.server.config.TdsContext;
import thredds.servlet.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.util.StringTokenizer;

import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.units.DateRange;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.stream.NcStreamWriter;
import ucar.unidata.geoloc.LatLonRect;

/**
 * This is a prototype for "cdm remote service".
 * This handles the cdmRemote service for index access to CDM datasets
 *
 * @author caron
 * @since Feb 16, 2009
 * @see "http://www.unidata.ucar.edu/software/netcdf-java/stream/NcStream.html"
 */
public class CdmRemoteControllerOld extends AbstractController implements LastModified {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

  private TdsContext tdsContext;
  private boolean allow = true;
  private boolean debug = false;

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  public void setAllow(boolean allow) {
    this.allow = allow;
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

    log.info(UsageLog.setupRequestContext(req));

    String pathInfo = req.getPathInfo();
    if (debug) System.out.println("request= " + ServletUtil.getRequest(req));
    //System.out.println(ServletUtil.showRequestDetail(null, req));

    /* String ft = ServletUtil.getParameterIgnoreCase(req, "ft");
    if (ft != null) {
      String newPath = req.getContextPath() + req.getServletPath() + "/" + ft + pathInfo;
      System.out.printf("newPath=%s%n", newPath);
      res.sendRedirect(newPath);
      return null;
    } */

    String query = EscapeStrings.unescapeOGC(req.getQueryString());
    //if (query != null) System.out.println(" query=" + query);

    String view = ServletUtil.getParameterIgnoreCase(req, "view");
    if (view == null) view = query;
    if (view == null) view = "";

    // LOOK! bogus !!
    if (view.equalsIgnoreCase("getCapabilities")) {
      sendCapabilities(req, res.getOutputStream(), FeatureType.STATION, true);
      res.flushBuffer();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
      return null;
    }

    NetcdfFile ncfile = null;
    try {
      ncfile = DatasetHandler.getNetcdfFile(req, res, pathInfo);
      if (ncfile == null) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
        return null;
      }
    } catch (FileNotFoundException e) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (Throwable e) {
      e.printStackTrace();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    try {
      OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);

      if ((query == null) || view.equalsIgnoreCase("cdl")) {
        res.setContentType("text/plain");
        String cdl = ncfile.toString();
        res.setContentLength(cdl.length());
        out.write(cdl.getBytes());

      } else if (view.equalsIgnoreCase("ncml")) {
        res.setContentType("application/xml");
        ncfile.writeNcML(out, pathInfo);

      } else {

        res.setContentType("application/octet-stream");
        res.setHeader("Content-Description", "ncstream");

        WritableByteChannel wbc = Channels.newChannel(out);
        NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(req));
        if (query.equals("header")) { // just the header
          ncWriter.sendHeader( wbc);

        } else { // they want some data
          query = EscapeStrings.urlDecode(query);  // old: URLDecoder.decode(query, "UTF-8");
          StringTokenizer stoke = new StringTokenizer(query, ";"); // need UTF/%decode
          while (stoke.hasMoreTokens()) {
            ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ncfile, stoke.nextToken());
            ncWriter.sendData( cer.v, cer.section, wbc);
          }
        }
      }

      out.flush();
      res.flushBuffer();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));

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
          log.error("Failed to close = " + pathInfo);
        }
    }

    return null;
  }

  static void sendCapabilities(HttpServletRequest req, OutputStream os, FeatureType ft, boolean addSuffix) throws IOException {
    Element rootElem = new Element("cdmRemoteCapabilities");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", ServletUtil.getRequestBase(req));
    Element elem = new Element("featureDataset");
    elem.setAttribute("type", ft.toString());
    elem.setAttribute("url", makeFeatureUri(req, ft, addSuffix));
    rootElem.addContent(elem);

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(doc, os);
  }

  static void sendCapabilities(HttpServletRequest req, OutputStream os, FeatureDatasetPoint fd, boolean addSuffix) throws IOException {
    Element rootElem = new Element("cdmRemoteCapabilities");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", ServletUtil.getRequestBase(req));
    Element elem = new Element("featureDataset");
    elem.setAttribute("type", fd.getFeatureType().toString());
    elem.setAttribute("url", makeFeatureUri(req, fd.getFeatureType(), addSuffix));
    rootElem.addContent(elem);

    LatLonRect bb = fd.getBoundingBox(); // LOOK should be a utility somewhere
    if (bb != null) {
      elem = new Element("geospatialCoverage");
      rootElem.addContent(elem);
      Element northsouth =  new Element("northsouth");
      elem.addContent(northsouth);
      northsouth.addContent( new Element("start").setText( Double.toString(bb.getLatMin())));
      northsouth.addContent( new Element("size").setText( Double.toString(bb.getHeight())));

      Element eastwest =  new Element("eastwest");
      elem.addContent(eastwest);
      eastwest.addContent( new Element("start").setText( Double.toString(bb.getLonMin())));
      eastwest.addContent( new Element("size").setText( Double.toString(bb.getWidth())));
    }

    DateRange t = fd.getDateRange(); // LOOK should be a utility somewhere
    if (t != null) {
      elem = new Element("timeCoverage");
      rootElem.addContent(elem);

      DateType start = t.getStart();
      DateType end = t.getEnd();
      TimeDuration duration = t.getDuration();

      if (t.useStart() && (start != null) && !start.isBlank()) {
        Element startElem = new Element("start");
        startElem.setText(start.toString());
        elem.addContent(startElem);
      }

      if (t.useEnd() && (end != null) && !end.isBlank()) {
        Element telem = new Element("end");
        telem.setText(end.toString());
        elem.addContent(telem);
      }

      if (t.useDuration() && (duration != null) && !duration.isBlank()) {
        Element telem = new Element("duration");
        telem.setText(duration.toString());
        elem.addContent(telem);
      }
    }

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(doc, os);
  }


  static String makeFeatureUri(HttpServletRequest req, FeatureType ft, boolean addSuffix) {
    /* String path = req.getPathInfo().substring(1); // remove leading '/'
    int pos = path.indexOf("/");
    path = path.substring(pos+1); // remove next segment '/'
    return ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath() + "/" + ft.toString().toLowerCase() + "/" + path;
    */
    String url = ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath() + req.getPathInfo();
    if (addSuffix)
      url = url + "/"+ ft.toString().toLowerCase();
    return url;
  }

}
