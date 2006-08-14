// $Id: FileServerServlet.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;

/**
 * HTTP File Serving
 *
 * handles /fileServer/*
 */
public class FileServerServlet extends HttpServlet {
  protected static org.slf4j.Logger log;

  public void init() throws ServletException {
    super.init();
    log = org.slf4j.LoggerFactory.getLogger(getClass());
  }

  protected long getLastModified(HttpServletRequest req) {
    File file = getFile( req);
    if (file == null)
      return -1;

    return file.lastModified();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    ServletUtil.logServerAccessSetup( req );

    File file = getFile( req);
    ServletUtil.returnFile(this, req, res, file, null);
  }

  private File getFile(HttpServletRequest req) {
    String reqPath = req.getPathInfo();
    if (reqPath.length() > 0) {
      if (reqPath.startsWith("/"))
        reqPath = reqPath.substring(1);
    }

    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile( reqPath);
    if (file == null)
      return null;
    if (!file.exists())
      return null;

    return file;
  }

}
