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

package thredds.servlet.restrict;

import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Class Description.
 *
 * @author caron
 */
public class CAMSAuthorizer extends TomcatAuthorizer {
    private boolean debugResourceControl = false;

    public boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException {
      if (hasCAMSrole(req, role))
        return true;

      return super.authorize(req, res, role);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
      ServletUtil.logServerAccessSetup( req );

      HttpSession session = req.getSession();
      if (session != null) {
        String origURI = (String) session.getAttribute("origRequest");
        String role = (String) session.getAttribute("role");

        if (req.isUserInRole(role)) {

          // transfer CAS roles to this session
          ArrayList rolesArray = new ArrayList();
          java.util.Enumeration rolesEnum = req.getHeaders("CAMS-HTTP-ROLE");
          for (Enumeration e = rolesEnum ; rolesEnum.hasMoreElements() ;)
            rolesArray.add( e.nextElement());
          session.setAttribute("camsRoles", rolesArray);

          if (origURI != null) {
            if (debugResourceControl) System.out.println("redirect to origRequest = "+origURI);
            res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
            String frag = (origURI.indexOf("?") > 0) ? "&auth" : "?auth";
            res.addHeader("Location", origURI+frag);
            return;

          } else {
            res.setStatus(HttpServletResponse.SC_OK); // someone came directly to this page
            return;
          }
        }
      }

      res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not authorized to access this dataset.");
    }

  private boolean hasCAMSrole( HttpServletRequest req, String role) {
    HttpSession session = req.getSession();
     if (session != null) {
       List<String> roles = (List<String>) session.getAttribute("camsRoles");
       return (roles != null) && roles.contains( role);
    }
    return false;
  }

}
