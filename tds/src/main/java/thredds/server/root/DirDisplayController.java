/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import thredds.servlet.HtmlWriter;
import thredds.servlet.UsageLog;
import thredds.server.config.TdsContext;

import java.io.File;

/**
 * Handle /admin/content/
 * Handle /admin/logs/
 *
 * @author caron
 * @since 4.0
 */
public class DirDisplayController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );


  private TdsContext tdsContext;

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( req ) );

    String path = req.getPathInfo();
    if (path == null) path = "";

    // Don't allow ".." directories in path.
    if (path.indexOf("/../") != -1
        || path.equals("..")
        || path.startsWith("../")
        || path.endsWith("/..")) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Path cannot contain ..");
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      return null;
    }

    File file = null;
    if (path.startsWith("/content/")) {
      file = new File(tdsContext.getContentDirectory(), path.substring(9));
      if ( file == null )
        file = tdsContext.getConfigFileSource().getFile( path.substring(9));
    } else if (path.startsWith("/logs/")) {
      file = new File(tdsContext.getTomcatLogDirectory(), path.substring(6));
    }

    if (file == null) {
      tdsContext.getDefaultRequestDispatcher().forward(req, res);
      return null;
    }

    if (file.isDirectory()) {
      int i = HtmlWriter.getInstance().writeDirectory(res, file, path);
      int status = i == 0 ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_OK;
      log.info( UsageLog.closingMessageForRequestContext( status, i ) );
      return null;
    }

    return new ModelAndView( "threddsFileView", "file", file);
  }

}