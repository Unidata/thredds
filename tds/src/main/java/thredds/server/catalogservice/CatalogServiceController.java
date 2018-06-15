/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalogservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.core.CatalogManager;
import thredds.core.TdsRequestedDataset;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * LocalCatalogServiceController using client/server catalogs
 *
 * @author caron
 * @since 1/19/2015
 */

@Controller
@RequestMapping(value = "/catalog")
public class CatalogServiceController {
  // private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

  @Autowired
  private CatalogManager catalogManager;

  @Autowired
  CatalogViewContextParser parser;

  @RequestMapping(value = "**", method = {RequestMethod.GET})
  protected ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response, CatalogRequest params) throws Exception {

    TdsRequestedDataset reqD = new TdsRequestedDataset(request, "/catalog");
    String path = reqD.getPath();
    boolean isHtml = path.endsWith(".html");
    String catalogPath = path.replaceAll(".html$", ".xml");

    Catalog catalog;
    URI baseUri;
    String baseUriString = request.getRequestURL().toString();
    try {
      baseUri = new URI(baseUriString);
      catalog = catalogManager.getCatalog(catalogPath, baseUri);

    } catch (URISyntaxException e) {
      String msg = "Bad URI syntax [" + baseUriString + "]: " + e.getMessage();
      throw new URISyntaxException(msg, e.getReason());
    }

    // no catalog found
    if (catalog == null)
      throw new FileNotFoundException(request.getRequestURI());

    if (isHtml) {
      return handleHTMLRequest(request, response, catalog, params);
    } else {
      return handleXMLRequest(request, response, catalog, params);
    }
  }

  protected ModelAndView handleXMLRequest(HttpServletRequest request, HttpServletResponse response, Catalog catalog, CatalogRequest params) throws Exception {
    if (params.dataset != null) {
      Dataset dataset = catalog.findDatasetByID(params.dataset);
      if (dataset == null)
        throw new FileNotFoundException("Did not find dataset [" + params.dataset + "] in catalog [" + request.getRequestURL().toString() + "].");

      Catalog subsetCat = catalog.subsetCatalogOnDataset(dataset);
      return new ModelAndView("threddsInvCatXmlView", "catalog", subsetCat);

    } else {
      return new ModelAndView("threddsInvCatXmlView", "catalog", catalog);
    }  
  }

  protected ModelAndView handleHTMLRequest(HttpServletRequest request, HttpServletResponse response, Catalog catalog, CatalogRequest params) throws Exception {
    if (params.dataset != null) {
      Dataset dataset = catalog.findDatasetByID(params.dataset);
      if (dataset == null)
        throw new FileNotFoundException("Did not find dataset [" + params.dataset + "] in catalog [" + request.getRequestURL().toString() + "].");
      return new ModelAndView("templates/dataset", parser.getDatasetViewContext(dataset, request,true));
    } else {
      return new ModelAndView("templates/catalog", parser.getCatalogViewContext(catalog, request,true));
    }
  }

  /* private ModelAndView handlePublicDocumentRequest(HttpServletRequest request, HttpServletResponse response, String path)
          throws IOException, ServletException {

    // If request doesn't match a known catalog, look for a public document.
    File publicFile = tdsContext.getPublicDocFileSource().getFile(path);
    if (publicFile != null) {
      return new ModelAndView("threddsFileView", "file", publicFile);
    }

    // If request doesn't match a public document, forward to default dispatcher.
    RequestForwardUtils.forwardRequest(path, tdsContext.getDefaultRequestDispatcher(), request, response);
    return null;
  }  */

  /* Exception handlers
  @ExceptionHandler(URISyntaxException.class)
  public ResponseEntity<String> handle(URISyntaxException ex) {
    log.error("LocalCatalogService: ", ex);

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("Catalog Service exception handled : " + ex.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
  }  */
}