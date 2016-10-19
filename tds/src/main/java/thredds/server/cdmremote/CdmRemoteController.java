/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.cdmremote;

import org.jdom2.Document;
import org.jdom2.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import thredds.core.AllowedServices;
import thredds.core.StandardService;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.TdsContext;
import thredds.server.exception.ServiceNotAllowed;
import thredds.servlet.ServletUtil;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.stream.NcStreamWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

/**
 * Spring controller for CdmRemote service.
 *
 * @author caron
 * @since May 28, 2009
 */
@Controller
@RequestMapping("/cdmremote")
public class CdmRemoteController {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CdmRemoteController.class);
  private static final boolean debug = false, showReq = false;

  @Autowired
  TdsContext tdsContext;

  @Autowired
  private AllowedServices allowedServices;

  @InitBinder("CdmRemoteQueryBean")
  protected void initBinder(WebDataBinder binder) {
    binder.setValidator(new CdmRemoteQueryBeanValidator());
  }

  // everything but header, data, which is binary data, and capabilities which is XML
  @RequestMapping(value = "/**", method = RequestMethod.GET)
  public ResponseEntity<String> handleCapabilitiesRequest(HttpServletRequest request, HttpServletResponse response, @RequestParam String req) throws IOException {

    if (!allowedServices.isAllowed(StandardService.cdmRemote))
      throw new ServiceNotAllowed(StandardService.cdmRemote.toString());

    String datasetPath = TdsPathUtils.extractPath(request, "/cdmremote");
    String absPath = getAbsolutePath(request);
    HttpHeaders responseHeaders;

    if (showReq)
      System.out.printf("CdmRemoteController req=%s%n", absPath + "?" + request.getQueryString());
    if (debug)
      System.out.printf(" path=%s%n query=%s%n", datasetPath, request.getQueryString());

    // LOOK heres where we want the Dataset, not the netcdfFile (!)
    try (NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(request, response, datasetPath)) {
      if (ncfile == null) return null;  // failed resource control

      responseHeaders = new HttpHeaders();
      responseHeaders.setDate("Last-Modified", TdsRequestedDataset.getLastModified(datasetPath));

      // a request without a parameter is a test to see if this is a valid cdremote endpoint.
      // just setHeader("Content-Description", "ncstream"), no body
      // on client, see DatasetUrl.disambiguateHttp
      if (req == null) {
          response.setContentType(ContentType.binary.getContentHeader());
          response.setHeader("Content-Description", "ncstream");
          return new ResponseEntity<>(null, responseHeaders, HttpStatus.OK);
        }

      switch (req.toLowerCase()) {
        case "form":    // ol
        case "cdl":
          ncfile.setLocation(datasetPath); // hide where the file is stored  LOOK
          String cdl = ncfile.toString();
          responseHeaders.set(ContentType.HEADER, ContentType.text.getContentHeader());
          return new ResponseEntity<>(cdl, responseHeaders, HttpStatus.OK);

        case "ncml":
          String ncml = ncfile.toNcML(absPath);
          responseHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
          return new ResponseEntity<>(ncml, responseHeaders, HttpStatus.OK);

        default:
          return new ResponseEntity<>("Unrecognized request", null, HttpStatus.BAD_REQUEST);
      }
    }
  }

  @RequestMapping(value = "/**", method = RequestMethod.GET, params = "req=capabilities")
  public ModelAndView handleCapabilitiesRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

    if (!allowedServices.isAllowed(StandardService.cdmRemote))
      throw new ServiceNotAllowed(StandardService.cdmRemote.toString());

    String datasetPath = TdsPathUtils.extractPath(request, "/cdmremote");
    String absPath = getAbsolutePath(request);

    try (NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(request, response, datasetPath)) {
      if (ncfile == null) return null;

      Element rootElem = new Element("cdmRemoteCapabilities");
      Document doc = new Document(rootElem);
      rootElem.setAttribute("location", absPath);

      Element elem = new Element("featureDataset");
      FeatureType ftFromMetadata = FeatureDatasetFactoryManager.findFeatureType(ncfile); // LOOK BAD - must figure out what is the featureType and save it
      if (ftFromMetadata != null)
        elem.setAttribute("type", ftFromMetadata.toString());
      elem.setAttribute("url", absPath);
      rootElem.addContent(elem);

      return new ModelAndView("threddsXmlView", "Document", doc);
    }
  }

  @RequestMapping(value = "/**", method = RequestMethod.GET, params = "req=header")
  public void handleHeaderRequest(HttpServletRequest request, HttpServletResponse response, OutputStream out) throws IOException {

    if (!allowedServices.isAllowed(StandardService.cdmRemote))
      throw new ServiceNotAllowed(StandardService.cdmRemote.toString());

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
      response.addDateHeader("Last-Modified", TdsRequestedDataset.getLastModified(datasetPath));

      NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequestBase(request));
      long size = ncWriter.sendHeader(out);
      out.flush();

      if (debug)
        System.out.printf("CdmRemoteController header ok, size=%s%n", size);
    }
  }

  @RequestMapping(value = "/**", method = RequestMethod.GET, params = "req=data")
  public void handleDataRequest(HttpServletRequest request, HttpServletResponse response,
                            @Valid CdmRemoteQueryBean qb, BindingResult validationResult, OutputStream out)
          throws IOException, BindException, InvalidRangeException {

    if (!allowedServices.isAllowed(StandardService.cdmRemote))
      throw new ServiceNotAllowed(StandardService.cdmRemote.toString());

    if (validationResult.hasErrors())
      throw new BindException(validationResult);

    String datasetPath = TdsPathUtils.extractPath(request, "/cdmremote");
    String absPath = getAbsolutePath(request);

    if (showReq)
      System.out.printf("CdmRemoteController req=%s%n", absPath + "?" + request.getQueryString());
    if (debug)
      System.out.printf(" path=%s%n query=%s%n", datasetPath, request.getQueryString());
    long start = System.currentTimeMillis();

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

      // query = EscapeStrings.unescapeURLQuery(query);
      StringTokenizer stoke = new StringTokenizer(query, ";"); // need UTF/%decode
      while (stoke.hasMoreTokens()) {
        ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ncfile, stoke.nextToken());
        // size += ncWriter.sendData(cer.v, cer.section, out, qb.getCompression());
        size += ncWriter.sendData2(cer.v, cer.section, out, qb.getCompression());
        // size += ncWriter.sendData3(cer.v, cer.section, out, qb.getCompression());
      }
      out.flush();

      if (debug)
        System.out.printf("CdmRemoteController data ok, size=%s took=%d%n", size, System.currentTimeMillis() - start);

    } //catch (IllegalArgumentException | InvalidRangeException e) { // ParsedSectionSpec failed
     // response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    // }

  }

  private String getAbsolutePath(HttpServletRequest req) {
    return ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath();
  }

}
