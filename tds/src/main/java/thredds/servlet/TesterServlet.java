/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.nc2.util.IO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author caron
 * @since Dec 9, 2007
 */
public class TesterServlet extends AbstractServlet {
  private String stuff = "four score and 7 years ago";

  protected String getPath() { return "tester/"; }
  protected void makeDebugActions() {  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
                             throws ServletException, IOException {

    log.info( UsageLog.setupRequestContext(req));

    res.setHeader("Content-Encoding","gzip");
    OutputStream out = res.getOutputStream();
    GZIPOutputStream zout = new GZIPOutputStream(out);
    IO.writeContents(stuff, zout);
    zout.finish();
    out.flush();
  }
}
