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

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.validation.BindException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

import ucar.nc2.util.DiskCache2;

/**
 * Get a submitted file for BUFR validation, copy to disk, redirect client
 *
 * @author caron
 * @since Oct 3, 2008
 */
public class BtFileUploadController extends SimpleFormController {
  private DiskCache2 diskCache = null;

  public void setCache(DiskCache2 cache) {
    diskCache = cache;
  }

  protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response,
                                  Object command, BindException errors) throws Exception {

    // cast the bean
    FileUploadBean bean = (FileUploadBean) command;

    // let's see if there's content there
    MultipartFile file = bean.getFile();
    if (file == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file was uploaded");
      return null;
    }

    String username = bean.getUsername();
    if (username == null) username = "anon";
    username = username.trim();
    if (username.length() == 0) username = "anon";

    String fname = username+"/"+file.getOriginalFilename();

    String redirectURL = "/validate/file";

    Map map = new HashMap();
    map.put("filename", fname);
    map.put("xml", bean.isXml());

    // copy into the cache,
    File dest = diskCache.getCacheFile(fname);
    file.transferTo(dest);

    return new ModelAndView(new RedirectView(redirectURL, true), map);
  }

}
