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
