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
