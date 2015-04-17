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
package thredds.server.root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.LastModified;
import thredds.server.config.TdsContext;
import thredds.util.RequestForwardUtils;
import thredds.util.TdsPathUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Spring Controller redirects "/" to "/catalog.html"
 *
 * @author edavis
 * @since 4.0
 */
@Controller
public class RootController implements LastModified {

  @Autowired
  private TdsContext tdsContext;

  // this is to catch old style catalog requests that dont start with catalog
  @RequestMapping({"**"})
  public String wtf(HttpServletRequest req) throws FileNotFoundException {
    System.out.printf("%s%n", req.getRequestURI());
    throw new FileNotFoundException(req.getRequestURI());
  }

  @RequestMapping(value = {"/", "/catalog.html"}, method = {RequestMethod.GET, RequestMethod.HEAD})
  public String redirectRootCatalog() {
    return "redirect:/catalog/catalog.html";
  }

  @RequestMapping(value = {"/catalog.xml"}, method = {RequestMethod.GET, RequestMethod.HEAD})
  public String redirectRootCatalogXml() {
    return "redirect:/catalog/catalog.xml";
  }

  @RequestMapping(value = {"*.css", "*.gif", "*.jpg", "*.png"}, method = RequestMethod.GET)
  public ModelAndView serveFromPublicDirectory(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    String path = TdsPathUtils.extractPath(req, null);
    File file = tdsContext.getPublicDocFileSource().getFile(path);
    if (file == null) {
      RequestForwardUtils.forwardRequest(path, tdsContext.getDefaultRequestDispatcher(), req, res);
      return null;
    }
    return new ModelAndView("threddsFileView", "file", file);
  }

  public long getLastModified(HttpServletRequest req) {
    String path = TdsPathUtils.extractPath(req, null);
    File file = tdsContext.getPublicDocFileSource().getFile(path);
    if (file == null)
      return -1;
    long lastModTime = file.lastModified();
    if (lastModTime == 0L)
      return -1;
    return lastModTime;
  }
}
