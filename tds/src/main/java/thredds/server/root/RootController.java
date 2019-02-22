/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.config.TdsContext;
import thredds.util.RequestForwardUtils;
import thredds.util.TdsPathUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Spring Controller redirects "/" to "/catalog.html"
 *
 * @author edavis
 * @since 4.0
 */
@Controller
public class RootController {
  @Autowired
  private TdsContext tdsContext;

  @RequestMapping(value = {"/", "/catalog.html"}, method = {RequestMethod.GET, RequestMethod.HEAD})
  public String redirectRootCatalog() {
    return "redirect:/catalog/catalog.html";
  }

  @RequestMapping(value = {"/catalog.xml"}, method = {RequestMethod.GET, RequestMethod.HEAD})
  public String redirectRootCatalogXml() {
    return "redirect:/catalog/catalog.xml";
  }

  @RequestMapping(value = {"*.css", "*.gif", "*.jpg", "*.png", "*.jsp", "sitemap.xml"}, method = RequestMethod.GET)
  public ModelAndView serveFromPublicDirectory(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    String path = TdsPathUtils.extractPath(req, null);
    File file = tdsContext.getPublicContentDirSource().getFile(path);
    if (file == null) {
      RequestForwardUtils.forwardRequest(path, tdsContext.getDefaultRequestDispatcher(), req, res);   // tomcat default servlet, not spring
      return null;
    }
    return new ModelAndView("threddsFileView", "file", file);
  }
}
