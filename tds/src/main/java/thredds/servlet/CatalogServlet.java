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

package thredds.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.IOException;

/**
 * Dynamic Catalog Serving
 *
 * handles /catalog/*
 */
public class CatalogServlet extends HttpServlet {

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    UsageLog.log.info( UsageLog.setupRequestContext(req));

    String path = req.getPathInfo();
    //StringBuffer reqURL = req.getRequestURL();

    if ( path == null )
    {
      // Redirect "/thredds/catalog" request to "/thredds/catalog.html".
      ServletUtil.sendPermanentRedirect( ServletUtil.getContextPath()+"/catalog.html", req, res );
      return;
    }

    if ( path.startsWith( "/redirectTest/"))
    {
      if ( path.startsWith( "/redirectTest/good/"))
      {
        this.getServletContext().getRequestDispatcher( "/catalog.xml").forward( req, res);
        return;
      }
      else if ( path.startsWith( "/redirectTest/301/"))
      {
        // 301 "Moved Permanently"
        ServletUtil.sendPermanentRedirect( ServletUtil.getContextPath()+"/catalog.xml", req, res );
        return;
      }
      else if ( path.startsWith( "/redirectTest/302/" ) )
      {
        // 302 "Found"
        res.sendRedirect( "/catalog.xml" );
        return;
      }
      else
      {
        res.sendRedirect( "/catalog.xml" );
        return;
      }
    }

    DataRootHandler drh = DataRootHandler.getInstance();

    // see if its a catalog
    boolean ok = drh.processReqForCatalog( req, res);

    if ( ! ok )
    {
      UsageLog.log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ));
      res.sendError( HttpServletResponse.SC_NOT_FOUND );
    }
  }

}
