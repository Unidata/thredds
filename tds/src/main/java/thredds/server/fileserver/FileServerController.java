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

package thredds.server.fileserver;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.LastModified;
import thredds.core.TdsRequestedDataset;
import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;

/**
 * HTTP File Serving
 *
 * handles /fileServer/*
 */
@Controller
@RequestMapping("/fileServer")
public class FileServerController implements LastModified {
  protected static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileServerController.class);

  public long getLastModified(HttpServletRequest req) {
    String reqPath = TdsPathUtils.extractPath(req, "fileServer/");
    if (reqPath == null) return -1;

    File file = getFile( reqPath);
    if (file == null)
      return -1;

    return file.lastModified();
  }

  @RequestMapping("**")
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    String reqPath = TdsPathUtils.extractPath(req, "fileServer/");
    if (reqPath == null) return;

    if (!TdsRequestedDataset.resourceControlOk(req, res, reqPath)) {  // LOOK or process in TdsRequestedDataset.getFile ??
      return;
    }

    File file = getFile( reqPath);
    ServletUtil.returnFile(null, req, res, file, null);
  }

  private File getFile(String reqPath) {
    if (reqPath == null) return null;

    File file = TdsRequestedDataset.getFile(reqPath);
    if (file == null)
      return null;
    if (!file.exists())
      return null;

    return file;
  }

}
