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
package thredds.server.admin;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import thredds.core.TdsRequestedDataset;
import thredds.server.config.TdsContext;
import thredds.util.RequestForwardUtils;
import thredds.util.TdsPathUtils;

/**
 * Handle /admin/content/
 * Handle /admin/logs/
 * Handle /admin/dataDir/
 *
 * Make sure this is only done under https.
 *
 * @author caron
 * @since 4.0
 */
@Controller
@RequestMapping("/admin/dir")
public class AdminDirDisplayController {

  @Autowired
  private TdsContext tdsContext;
  
  @Autowired
  thredds.servlet.HtmlWriting htmlu;
  
  @RequestMapping("**")
  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
	  
    String path = TdsPathUtils.extractPath(req, "/admin/dir");

    /* Don't allow ".." directories in path.   now done in TdsPathUtils.extractPath
    if (path.contains("/../")
        || path.equals("..")
        || path.startsWith("../")
        || path.endsWith("/..")) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Path cannot contain ..");
      return null;
    } */

    File file = null;
    if (path.startsWith("content/")) {
      // Check in content/thredds directory (which includes content/thredds/public).
      file = new File(tdsContext.getContentDirectory(), path.substring(8));
      // If not found, check in content/thredds and altContent (but not content/thredds/public).
      if ( ! file.exists() )
        file = tdsContext.getConfigFileSource().getFile( path.substring(8));

    } else if (path.startsWith("logs/")) {
      file = new File(tdsContext.getTomcatLogDirectory(), path.substring(5));

    } else if (path.startsWith("dataDir/")) {
      String root = path.substring(8);
      file = TdsRequestedDataset.getFile(root);
    }

    if (file == null) {
      // RequestForwardUtils.forwardRequest( path, tdsContext.getDefaultRequestDispatcher(), req, res );  // LOOK wtf ?
      return null;
    }

    if (file.isDirectory()) {
      int i = htmlu.writeDirectory(res, file, path);
      int status = (i == 0) ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_OK;
      return null;
    }

    return new ModelAndView( "threddsFileView", "file", file);
  }

}