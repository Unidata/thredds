/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.admin;

import java.io.File;
import java.io.FileNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import thredds.core.TdsRequestedDataset;
import thredds.server.config.TdsContext;
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
  HtmlWriting htmlu;
  
  @RequestMapping("**")
  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
	  
    String path = TdsPathUtils.extractPath(req, "/admin/dir");

    File file = null;
    if (path.startsWith("catalogs/")) {
      file = new File(tdsContext.getThreddsDirectory(), path.substring(9));

    } else if (path.startsWith("content/")) {
      // Check in content/thredds directory (which includes content/thredds/public).
      file = new File(tdsContext.getContentRootDir(), path.substring(8));
      // If not found, check in content/thredds and altContent (but not content/thredds/public).
      //if ( !file.exists() )
      //  file = tdsContext.getCatalogRootDirSource().getFile( path.substring(8));

    } else if (path.startsWith("logs/")) {
      file = new File(tdsContext.getTomcatLogDirectory(), path.substring(5));

    } else if (path.startsWith("dataDir/")) {
      String root = path.substring(8);
      file = TdsRequestedDataset.getFile(root);
    }

    if (file == null || !file.exists()) {
      throw new FileNotFoundException(path);
      // RequestForwardUtils.forwardRequest( path, tdsContext.getDefaultRequestDispatcher(), req, res );  // LOOK wtf ?
      // return null;
    }

    if (file.exists() && file.isDirectory()) {
      int i = htmlu.writeDirectory(res, file, path);
      res.setStatus((i == 0) ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_OK);
      return null;
    }

    return new ModelAndView( "threddsFileView", "file", file);
  }

}