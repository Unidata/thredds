/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.viewer;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import thredds.server.config.TdsContext;
import thredds.servlet.ServletUtil;
import thredds.util.ContentType;
import thredds.util.StringValidateEncodeUtils;
import ucar.unidata.util.StringUtil2;

@Controller
@RequestMapping("/view")
@Deprecated
public class ViewerController {
  private static Logger log = LoggerFactory.getLogger(ViewerController.class);

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  ViewerService viewerService;

  @RequestMapping(value = "{viewer}.jnlp", method = RequestMethod.GET)
  public void launchViewer(@Valid ViewerRequestParamsBean params, BindingResult result, HttpServletResponse res, HttpServletRequest req) throws IOException {

    if (result.hasErrors()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (!StringValidateEncodeUtils.validAlphanumericString(params.getViewer())) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    String viewerName = StringUtil2.filter7bits(params.getViewer()) + ".jnlp";

    //Check paths LOOK lame
    File viewPath = new File(tdsContext.getServletRootDirectory(), "/WEB-INF/views/" + viewerName);
    String template = viewerService.getViewerTemplate(viewPath.getPath());

    if (template == null) {
      viewPath = new File(tdsContext.getContentRootDir(), "views/" + viewerName);
      template = viewerService.getViewerTemplate(viewPath.getPath());
    }
    if (template == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String strResp = fillTemplate(req, template);

    try {
      res.setContentType(ContentType.jnlp.getContentHeader());
      ServletUtil.returnString(strResp, res);

    } catch (Throwable t) {
      log.error(" jnlp=" + strResp, t);
      if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }


  @SuppressWarnings("unchecked")
  private String fillTemplate(HttpServletRequest req, String template) {

    StringBuilder sbuff = new StringBuilder(template);

    Enumeration<String> params = req.getParameterNames();
    while (params.hasMoreElements()) {
      String name = params.nextElement();
      String values[] = req.getParameterValues(name);
      if (values != null) {
        String sname = "{" + name + "}";
        for (String value : values) {
          String filteredValue = StringUtil2.filter7bits(value);
          StringUtil2.substitute(sbuff, sname, filteredValue); // multiple occurences in the template will all get replaced
        }
      }
    }

    return sbuff.toString();
  }
}
