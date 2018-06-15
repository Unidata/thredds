/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.catalogservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
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
import thredds.core.AllowedServices;
import thredds.core.StandardService;
import thredds.server.config.HtmlConfigBean;
import thredds.server.exception.ServiceNotAllowed;

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

  @Autowired
  private AllowedServices allowedServices;

  @Autowired
  private HtmlConfigBean htmlConfig;

  @Autowired
  private CatalogViewContextParser parser;

  @InitBinder // ("RemoteCatalogRequest")  LOOK
  protected void initBinder(WebDataBinder binder) {
    binder.setValidator(new RemoteCatalogRequestValidator());
  }

  @RequestMapping(method = {RequestMethod.GET})
  protected ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response,
                                       @Valid RemoteCatalogRequest params, BindingResult validationResult) throws Exception {

    if (!allowedServices.isAllowed(StandardService.catalogRemote))
      throw new ServiceNotAllowed(StandardService.catalogRemote.toString());

    if (validationResult.hasErrors())
      throw new BindException(validationResult);

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
        return new ModelAndView("templates/catalog", parser.getCatalogViewContext(catalog, request,false));

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
          return new ModelAndView("templates/dataset", parser.getDatasetViewContext(dataset, request, false));
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

  public static ModelAndView constructValidationMessageModelAndView(URI uri, String validationMessage, HtmlConfigBean htmlConfig) {
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
