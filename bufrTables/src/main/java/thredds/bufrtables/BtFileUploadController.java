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
