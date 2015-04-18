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
package thredds.server.catalogservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.core.ConfigCatalogHtmlWriter;
import thredds.core.DataRootManager;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.TdsContext;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * LocalCatalogServiceController using client/server catalogs
 *
 * @author caron
 * @since 1/19/2015
 */

@Controller
@RequestMapping(value = "/catalog")
public class CatalogServiceController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private DataRootManager dataRootManager;

  @Autowired
  ConfigCatalogHtmlWriter writer;

  @RequestMapping(method = {RequestMethod.GET})
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
      catalog = dataRootManager.getCatalog(catalogPath, baseUri);

    } catch (URISyntaxException e) {
      String msg = "Bad URI syntax [" + baseUriString + "]: " + e.getMessage();
      throw new URISyntaxException(msg, e.getReason());
    }

      /*  If no catalog found, handle as a publicDoc request.
      if (catalog == null)
        return handlePublicDocumentRequest(request, response, catalogPath); */

    // Otherwise, handle catalog as indicated by "command".
    if (params.dataset != null) {
      Dataset dataset = catalog.findDatasetByID(params.dataset);
      if (dataset == null)
        throw new DatasetNotFound("Did not find dataset [" + params.dataset + "] in catalog [" + baseUriString + "].");

      if (isHtml) {
        int i = writer.showDataset(baseUriString, dataset, request, response, true);
        return null;

      } else {
        Catalog subsetCat = catalog.subsetCatalogOnDataset(dataset);
        return new ModelAndView("threddsInvCatXmlView", "catalog", subsetCat);
      }

    } else {
      if (isHtml) {
        int i = writer.writeCatalog(request, response, catalog, true);
        return null;

      } else {
        return new ModelAndView("threddsInvCatXmlView", "catalog", catalog);
      }
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

  // Exception handlers
  @ExceptionHandler(URISyntaxException.class)
  public ResponseEntity<String> handle(URISyntaxException ex) {
    log.error("LocalCatalogService: ", ex);

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("Catalog Service exception handled : " + ex.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
  }

  @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such Dataset")
  private class DatasetNotFound extends RuntimeException {
    DatasetNotFound(String msg) {
      super(msg);
    }
  }

}