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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import thredds.core.TdsRequestedDataset;
import thredds.util.Constants;
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
import thredds.servlet.ServletUtil;

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
public class CdmRemoteController implements LastModified {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CdmRemoteController.class);
  private static final boolean debug = false, showReq = false;

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
  public long getLastModified(HttpServletRequest req) {
    String path = TdsPathUtils.extractPath(req, "cdmremote/");
    return TdsRequestedDataset.getLastModified(path);
  }

  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.setValidator(new CdmRemoteQueryBeanValidator());
  }

  // everything but header, data
  @RequestMapping(value = "/**", method = RequestMethod.GET)
  public ResponseEntity<String> handleCapabilitiesRequest(HttpServletRequest request, HttpServletResponse response, @RequestParam String req) throws IOException {

    if (!allow) {
      return new ResponseEntity<>("Service not supported", null, HttpStatus.FORBIDDEN);
    }

    String datasetPath = TdsPathUtils.extractPath(request, "/cdmremote");
    String absPath = getAbsolutePath(request);
    HttpHeaders responseHeaders;

    if (showReq)
      System.out.printf("CdmRemoteController req=%s%n", absPath + "?" + request.getQueryString());
    if (debug) {
      System.out.printf(" path=%s%n query=%s%n", datasetPath, request.getQueryString());
    }

    // LOOK heres where we want the Dataset, not the netcdfFile (!)
    try (NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(request, response, datasetPath)) {
      if (ncfile == null) return null;  // ??

      switch (req.toLowerCase()) {
        case "capabilities":
          Element rootElem = new Element("cdmRemoteCapabilities");
          Document doc = new Document(rootElem);
          rootElem.setAttribute("location", absPath);

          Element elem = new Element("featureDataset");
          FeatureType ftFromMetadata = FeatureDatasetFactoryManager.findFeatureType(ncfile); // LOOK BAD - must figure out what is the featureType and save it
          if (ftFromMetadata != null)
            elem.setAttribute("type", ftFromMetadata.toString());
          elem.setAttribute("url", absPath);
          rootElem.addContent(elem);

          XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
          String result = fmt.outputString(doc);

          responseHeaders = new HttpHeaders();
          //responseHeaders.setContentType(ContentType.HEADER, ContentType.xml.getMediaType);
          // responseHeaders.setContentDispositionFormData("MyResponseHeader", "MyValue");
          responseHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
          //responseHeaders.set(Constants.Content_Disposition, Constants.setContentDispositionValue(datasetPath, ".xml"));
          return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);

        case "form":
        case "cdl":
          ncfile.setLocation(datasetPath); // hide where the file is stored  LOOK
          String cdl = ncfile.toString();
          responseHeaders = new HttpHeaders();
          responseHeaders.set(ContentType.HEADER, ContentType.text.getContentHeader());
          return new ResponseEntity<>(cdl, responseHeaders, HttpStatus.OK);

        case "ncml":
          String ncml = ncfile.toNcML(absPath);
          responseHeaders = new HttpHeaders();
          responseHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
          return new ResponseEntity<>(ncml, responseHeaders, HttpStatus.OK);

        default:
          return new ResponseEntity<>("Unrecognized request", null, HttpStatus.BAD_REQUEST);
      }

    }
  }

  @RequestMapping(value = "/**", method = RequestMethod.GET, params = "req=header")
  public void handleHeaderRequest(HttpServletRequest request, HttpServletResponse response, OutputStream out) throws IOException {

    if (!allow) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return; //  "Service not supported";
    }

    String datasetPath = TdsPathUtils.extractPath(request, "/cdmremote");
    String absPath = getAbsolutePath(request);

    if (showReq)
      System.out.printf("CdmRemoteController req=%s%n", absPath + "?" + request.getQueryString());
    if (debug) {
      System.out.printf(" path=%s%n query=%s%n", datasetPath, request.getQueryString());
    }

    try (NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(request, response, datasetPath)) {
      if (ncfile == null) return;

      response.setContentType(ContentType.binary.getContentHeader());
      response.setHeader("Content-Description", "ncstream");
      NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(request));
      long size = ncWriter.sendHeader(out);
      out.flush();

      if (showReq)
        System.out.printf("CdmRemoteController header ok, size=%s%n", size);

    }

  }

  @RequestMapping(value = "/**", method = RequestMethod.GET, params = "req=data")
  public void handleRequest(HttpServletRequest request, HttpServletResponse response,
                            @Valid CdmRemoteQueryBean qb, BindingResult validationResult, OutputStream out) throws IOException {

    if (!allow) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return; //  "Service not supported";
    }

    if (qb.hasErrors()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, qb.toString());
      return;
    }

    String datasetPath = TdsPathUtils.extractPath(request, "/cdmremote");
    String absPath = getAbsolutePath(request);

    if (showReq)
      System.out.printf("CdmRemoteController req=%s%n", absPath + "?" + request.getQueryString());
    if (debug) {
      System.out.printf(" path=%s%n query=%s%n", datasetPath, request.getQueryString());
    }

    try (NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(request, response, datasetPath)) {
      if (ncfile == null) return;

      response.setContentType(ContentType.binary.getContentHeader());
      response.setHeader("Content-Description", "ncstream");

      long size = 0;
      //WritableByteChannel wbc = Channels.newChannel(out);
      NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(request));
      String query;
      if (qb.getVar() != null)
        query = qb.getVar();
      else
        query = request.getQueryString(); // LOOK ??

      if ((query == null) || (query.length() == 0)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have query string");
        return;
      }

      query = EscapeStrings.unescapeURLQuery(query);
      StringTokenizer stoke = new StringTokenizer(query, ";"); // need UTF/%decode
      while (stoke.hasMoreTokens()) {
        ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ncfile, stoke.nextToken());
        size += ncWriter.sendData(cer.v, cer.section, out, false);
      }
      out.flush();

      if (showReq)
        System.out.printf("CdmRemoteController data ok, size=%s%n", size);

    } catch (IllegalArgumentException | InvalidRangeException e) { // ParsedSectionSpec failed
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }

  }

  public String getAbsolutePath(HttpServletRequest req) {
    return ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath();
  }

}
