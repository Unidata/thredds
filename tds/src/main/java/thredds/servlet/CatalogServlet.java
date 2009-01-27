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
