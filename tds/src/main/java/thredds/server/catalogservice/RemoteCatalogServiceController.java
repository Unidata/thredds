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

package thredds.server.catalogservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.core.ConfigCatalogHtmlWriter;
import thredds.server.config.HtmlConfig;
import thredds.server.config.ThreddsConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URI;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Remote CatalogService Controller using client/server catalogs
 *
 * @author caron
 * @since 1/19/2015
 */
@Controller
@RequestMapping("/remoteCatalogService")
public class RemoteCatalogServiceController {

  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private boolean allowRemote = ThreddsConfig.getBoolean("CatalogServices.allowRemote", false);

  @Autowired
  private HtmlConfig htmlConfig;

  @Autowired
  private ConfigCatalogHtmlWriter writer;

  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.setValidator(new RemoteCatalogRequestValidator());
  }

  @RequestMapping(method = {RequestMethod.GET})
  protected ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response,
                                       @Valid RemoteCatalogRequest params, BindingResult validationResult) throws Exception {

    if (!allowRemote)
      throw new UnsupportedOperationException("Catalog services not supported for remote catalogs.");

    // Determine path and catalogPath
    URI uri = params.getCatalogUri();

    CatalogBuilder builder = new CatalogBuilder();
    Catalog catalog = builder.buildFromURI(uri);
    if (builder.hasFatalError() || catalog == null) {
      Formatter f = new Formatter();
      f.format("Error reading catalog '%s' err=%s%n", uri, builder.getErrorMessage());
      log.debug(f.toString());
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, f.toString());
      return null;

    } else {
      String mess = builder.getErrorMessage();
      if (mess.length() > 0)
        System.out.printf(" parse Messages = %s%n", builder.getErrorMessage());
    }

    ///////////////////////////////////////////
    // Otherwise, handle catalog as indicated by "command".
     switch (params.getCommand()) {
      case SHOW:
        writer.writeCatalog(request, response, catalog, false);
        return null;

      case SUBSET:
        String datasetId = params.getDataset();
        Dataset dataset = catalog.findDatasetByID(datasetId);
        if (dataset == null) {
          String msg = "Did not find dataset [" + HtmlUtils.htmlEscape(datasetId) + "] in catalog [" + uri + "].";
          log.info("handleRequestInternal(): " + msg);
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
          return null;
        }

        if (params.isHtmlView()) {
          writer.showDataset(uri.toString(), dataset, request, response, false);
          return null;

        } else {
          Catalog subsetCat = catalog.subsetCatalogOnDataset(dataset);
          return new ModelAndView("threddsInvCatXmlView", "catalog", subsetCat);
        }

      case VALIDATE:
        return constructValidationMessageModelAndView(uri, builder.getValidationMessage(), htmlConfig);

      default:
        String msg = "Unsupported request command [" + params.getCommand() + "].";
        log.error("handleRequestInternal(): " + msg + " -- NOTE: Should have been caught on input validation.");
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        return null;
    }

  }

  public static ModelAndView constructValidationMessageModelAndView(URI uri, String validationMessage, HtmlConfig htmlConfig) {
    Map<String, Object> model = new HashMap<>();
    model.put("catalogUrl", uri);
    model.put("message", validationMessage);

    htmlConfig.addHtmlConfigInfoToModel(model);  // LOOK cant be right
    return new ModelAndView("/thredds/server/catalogservice/validationMessage", model);
  }

  /*
  @RequestMapping(value = "/remoteCatalogValidation.html", method = {RequestMethod.GET})
  protected ModelAndView handleFormRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

    if (!allowRemote)
      throw new UnsupportedOperationException("Catalog services not supported for remote catalogs.");

    Map<String, Object> model = new HashMap<>();

    htmlConfig.addHtmlConfigInfoToModel(model); // LOOK cant be right

    return new ModelAndView("/thredds/server/catalogservice/validationForm", model);
  } */

}
