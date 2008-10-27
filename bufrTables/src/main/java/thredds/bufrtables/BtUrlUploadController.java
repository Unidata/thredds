/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.bufrtables;

import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.validation.BindException;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

/**
 * Get a submitted URL for BUFR validation, copy to disk, redirect client
 *
 * @author caron
 * @since Oct 4, 2008
 */
public class BtUrlUploadController extends AbstractCommandController {
  private DiskCache2 diskCache = null;

  public void setCache(DiskCache2 cache) {
    diskCache = cache;
  }

  protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
      throws Exception {

    UrlUploadBean bean = (UrlUploadBean) command;
    String urlString = bean.getUrl();
    if ((urlString == null) || (urlString.length() == 0)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No url was specified");
      return null;
    }

    String filename = StringUtil.replace(urlString, "/", "-");
    filename = StringUtil.filter(filename, ".-_");

    String username = bean.getUsername();
    if (username == null) username = "anon";
    username = username.trim();
    if (username.length() == 0) username = "anon";

    String cacheName = username + "/" + filename;

    String redirectURL = "/validate/file";

    Map map = new HashMap();
    map.put("filename", cacheName);
    map.put("xml", bean.isXml());

    // copy into the cache,
    File dest = diskCache.getCacheFile(cacheName);
    try {
      IO.readURLtoFileWithExceptions(urlString, dest);
    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return null;
    }

    return new ModelAndView(new RedirectView(redirectURL, true), map);
  }

}
