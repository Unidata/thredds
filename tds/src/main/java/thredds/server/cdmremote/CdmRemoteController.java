/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.cdmremote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.server.AbstractController;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.util.EscapeStrings;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;
import org.springframework.web.servlet.mvc.LastModified;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.servlet.ServletUtil;
import thredds.servlet.DatasetHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
import java.util.StringTokenizer;

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
@Controller
@RequestMapping("/cdmremote")
public class CdmRemoteController extends AbstractController implements LastModified {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CdmRemoteController.class);
  private static final boolean debug = false, showReq=false;

  @Autowired
  TdsContext tdsContext;

  private boolean allow = true;

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  public void setAllow(boolean allow) {
    this.allow = allow;
  }

  @Override
  protected String getControllerPath() { return "/cdmremote/"; }

  @Override
  protected String[] getEndings() { return new String[0]; }

  @Override
  public long getLastModified(HttpServletRequest req) {
    String path = TdsPathUtils.extractPath(req, "wcs/");
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(path);
    if ((file != null) && file.exists())
      return file.lastModified();
    return -1;
  }

  @RequestMapping("**")
  public void handleRequest(HttpServletRequest req, HttpServletResponse res,
                       @Valid CdmRemoteQueryBean qb,
                       BindingResult validationResult) throws IOException { //}, NcssException, ParseException, InvalidRangeException {

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    String datasetPath = getDatasetPath(req);
    String absPath = getAbsolutePath(req);

    if (showReq)
      System.out.printf("CdmRemoteController req=%s%n", absPath+"?"+req.getQueryString());
    if (debug) {
      System.out.printf(" path=%s%n query=%s%n", datasetPath, req.getQueryString());
    }

    // query validation - first pass
    if (!qb.validate()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, qb.getErrorMessage());
      if (debug) System.out.printf(" query error= %s %n", qb.getErrorMessage());
      return;
    }
    if (debug) System.out.printf(" %s%n", qb);

    NetcdfFile ncfile = null;
    try {
      ncfile = DatasetHandler.getNetcdfFile(req, res, datasetPath);
      if (ncfile == null) return;
      /*
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        log.debug("DatasetHandler.FAIL path={}", datasetPath);
        return;
      } */

      long size = -1;

      switch (qb.getRequestType()) {
        case capabilities: {
          res.setContentType(ContentType.xml.getContentHeader());
          PrintWriter pw = res.getWriter();

          FeatureType ftFromMetadata = FeatureDatasetFactoryManager.findFeatureType(ncfile);
          sendCapabilities(pw, ftFromMetadata, absPath); // LOOK BAD - must figure out what is the featureType and save it
          res.flushBuffer();
          return;
        }

        case form: // LOOK could do a ncss style form
        case cdl:  {
          res.setContentType(ContentType.text.getContentHeader());
          PrintWriter pw = res.getWriter();

          ncfile.setLocation(datasetPath); // hide where the file is stored
          String cdl = ncfile.toString();
          size = (long) thredds.servlet.ServletUtil.setResponseContentLength(res, cdl);
          pw.write(cdl);
          break;
        }

        case ncml: {
          res.setContentType(ContentType.xml.getContentHeader());
          PrintWriter pw = res.getWriter();
          ncfile.writeNcML(pw, absPath);
          break;
        }

        case header: {
          res.setContentType(ContentType.binary.getContentHeader());
          res.setHeader("Content-Description", "ncstream");

          OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);
          //WritableByteChannel wbc = Channels.newChannel(out);
          NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(req));
          size = ncWriter.sendHeader(out);
          out.flush();
          break;
        }

        default: {
          res.setContentType(ContentType.binary.getContentHeader());
          res.setHeader("Content-Description", "ncstream");

          size = 0;
          //WritableByteChannel wbc = Channels.newChannel(out);
          NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(req));
          String query;
          if(qb.getVar() != null)
              query = qb.getVar();
          else
              query = req.getQueryString();

          if ((query == null) || (query.length() == 0)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have query string");
            return;
          }

          OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);
          query = EscapeStrings.unescapeURLQuery(query);
          StringTokenizer stoke = new StringTokenizer(query, ";"); // need UTF/%decode
          while (stoke.hasMoreTokens()) {
            ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ncfile, stoke.nextToken());
            size += ncWriter.sendData(cer.v, cer.section, out, qb.getCompression());
          }
          out.flush();
        }
      } // end switch on req type

      res.flushBuffer();
      if (showReq)
        System.out.printf("CdmRemoteController ok, size=%s%n", size);

    } catch (FileNotFoundException e) {
      log.debug("FAIL", e);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (IllegalArgumentException | InvalidRangeException e) { // ParsedSectionSpec failed
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

    } finally {
      if (null != ncfile)
        try {
          ncfile.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + datasetPath);
        }
    }

  }

  private void sendCapabilities(PrintWriter pw, FeatureType ft, String absPath) throws IOException {
    Element rootElem = new Element("cdmRemoteCapabilities");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", absPath);
    Element elem = new Element("featureDataset");
    if (ft != null)                            // LOOK lame
      elem.setAttribute("type", ft.toString());
    elem.setAttribute("url", absPath);
    rootElem.addContent(elem);

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(doc, pw);
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

   return null;
 } */


}
