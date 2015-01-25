/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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

package thredds.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.server.catalogservice.CatalogServiceUtils;
import thredds.server.catalogservice.Command;
import thredds.server.catalogservice.LocalCatalogRequest;
import thredds.server.config.HtmlConfig;
import thredds.server.config.TdsContext;
import thredds.servlet.HtmlWriter;
import thredds.util.RequestForwardUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * LocalCatalogServiceController using client/server catalogs
 *
 * @author caron
 * @since 1/19/2015
 */
public class LocalCatalogServiceController2 {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private HtmlWriter htmlWriter;

  @Autowired
  private HtmlConfig htmlConfig;

  @RequestMapping(value = {"/**/*.xml"}, method = {RequestMethod.GET, RequestMethod.HEAD})
  protected ModelAndView handleXmlRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
    try {
      // Bind HTTP request to a LocalCatalogRequest.
      BindingResult bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest(request, false);

      // If any binding or validation errors, return BAD_REQUEST.
      if (bindingResult.hasErrors()) {
        StringBuilder msg = new StringBuilder("Bad request");
        List<ObjectError> oeList = bindingResult.getAllErrors();
        for (ObjectError e : oeList)
          msg.append(": ").append(e.getDefaultMessage() != null ? e.getDefaultMessage() : e.toString());
        log.info("handleRequestInternal(): " + msg);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg.toString());
        return null;
      }

      // Retrieve the resulting LocalCatalogRequest.
      LocalCatalogRequest catalogServiceRequest = (LocalCatalogRequest) bindingResult.getTarget();

      // Determine path and catalogPath
      String catalogPath = catalogServiceRequest.getPath();

      // Check for matching catalog. LOOK autowired ??
      DataRootHandler drh = DataRootHandler.getInstance();

      Catalog catalog = null;
      String baseUriString = request.getRequestURL().toString();
      try {
        catalog = drh.getCatalog(catalogPath, new URI(baseUriString));
      } catch (URISyntaxException e) {
        String msg = "Bad URI syntax [" + baseUriString + "]: " + e.getMessage();
        log.error("handleRequestInternal(): " + msg);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        return null;
      }

      // If no catalog found, handle as a publicDoc request.
      if (catalog == null)
        return handlePublicDocumentRequest(request, response, catalogPath);

      // Otherwise, handle catalog as indicated by "command".
      if (catalogServiceRequest.getCommand().equals(Command.SHOW)) {
        return new ModelAndView("threddsInvCatXmlView", "catalog", catalog);

      } else if (catalogServiceRequest.getCommand().equals(Command.SUBSET)) {
        String datasetId = catalogServiceRequest.getDataset();
        Dataset dataset = catalog.findDatasetByID(datasetId);
        if (dataset == null) {
          String msg = "Did not find dataset [" + datasetId + "] in catalog [" + baseUriString + "].";
          response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
          return null;
        }

        Catalog subsetCat = catalog.subsetCatalogOnDataset( dataset);
        return new ModelAndView("threddsInvCatXmlView", "catalog", subsetCat);

      } else {
        String msg = "Unsupported request command [" + catalogServiceRequest.getCommand() + "].";
        log.error("handleRequestInternal(): " + msg + " -- NOTE: Should have been caught on input validation.");
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        return null;
      }

    } catch (IOException e) {
      log.error("handleRequestInternal(): Trouble writing to response.", e);
      return null;
    } catch (Throwable e) {
      log.error("handleRequestInternal(): Problem handling request.", e);
      if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;
    }
  }

  @RequestMapping(value = {"/**/*.html"}, method = {RequestMethod.GET, RequestMethod.HEAD})
  protected ModelAndView handleHtmlRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
    try {
      // Bind HTTP request to a LocalCatalogRequest.
      BindingResult bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest(request, true);

      // If any binding or validation errors, return BAD_REQUEST.
      if (bindingResult.hasErrors()) {
        StringBuilder msg = new StringBuilder("Bad request");
        List<ObjectError> oeList = bindingResult.getAllErrors();
        for (ObjectError e : oeList)
          msg.append(": ").append(e.getDefaultMessage() != null ? e.getDefaultMessage() : e.toString());
        log.info("handleRequestInternal(): " + msg);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg.toString());
        return null;
      }

      // Retrieve the resulting LocalCatalogRequest.
      LocalCatalogRequest catalogServiceRequest = (LocalCatalogRequest) bindingResult.getTarget();

      // Determine path and catalogPath
      String path = catalogServiceRequest.getPath();
      String catalogPath = path.replaceAll(".html$", ".xml");

      // Check for matching catalog.
      DataRootHandler drh = DataRootHandler.getInstance();

      Catalog catalog = null;
      String baseUriString = request.getRequestURL().toString();
      try {
        catalog = drh.getCatalog(catalogPath, new URI(baseUriString));
      } catch (URISyntaxException e) {
        String msg = "Bad URI syntax [" + baseUriString + "]: " + e.getMessage();
        log.error("handleRequestInternal(): " + msg);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        return null;
      }

      // If no catalog found, handle as a publicDoc request.
      if (catalog == null)
        return handlePublicDocumentRequest(request, response, path);

      // Otherwise, handle catalog as indicated by "command".
      ConfigCatalogHtmlWriter writer = new ConfigCatalogHtmlWriter(htmlWriter, htmlConfig, tdsContext.getContextPath());
      if (catalogServiceRequest.getCommand().equals(Command.SHOW)) {
          int i = writer.writeCatalog(request, response, catalog, true);
          return null;

      } else if (catalogServiceRequest.getCommand().equals(Command.SUBSET)) {
        String datasetId = catalogServiceRequest.getDataset();
        Dataset dataset = catalog.findDatasetByID(datasetId);
        if (dataset == null) {
          String msg = "Did not find dataset [" + datasetId + "] in catalog [" + baseUriString + "].";
          //log.info( "handleRequestInternal(): " + msg );
          response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
          return null;
        }

        int i = writer.showDataset(baseUriString, dataset, request, response, true);
        return null;

      } else {
        String msg = "Unsupported request command [" + catalogServiceRequest.getCommand() + "].";
        log.error("handleRequestInternal(): " + msg + " -- NOTE: Should have been caught on input validation.");
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        return null;
      }
    } catch (IOException e) {
      log.error("handleRequestInternal(): Trouble writing to response.", e);
      return null;

    } catch (Throwable e) {
      log.error("handleRequestInternal(): Problem handling request.", e);
      if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;
    }
  }

  private ModelAndView handlePublicDocumentRequest(HttpServletRequest request, HttpServletResponse response, String path)
          throws IOException, ServletException {

    // If request doesn't match a known catalog, look for a public document.
    File publicFile = tdsContext.getPublicDocFileSource().getFile(path);
    if (publicFile != null) {
      return new ModelAndView("threddsFileView", "file", publicFile);
    }

    // If request doesn't match a public document, forward to default dispatcher.
    RequestForwardUtils.forwardRequest(path, tdsContext.getDefaultRequestDispatcher(),
            request, response);
    return null;
  }

  	// Exception handlers
	@ExceptionHandler(FileNotFoundException.class)
	public ResponseEntity<String> handle(FileNotFoundException ncsse) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		return new ResponseEntity<>(
				"NetCDF Subset Service exception handled : " + ncsse.getMessage(), responseHeaders,
				HttpStatus.NOT_FOUND);
	}
}
