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

package thredds.servlet.restrict;

import thredds.servlet.UsageLog;

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
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

    private boolean debugResourceControl = false;

    public boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException {
      if (hasCAMSrole(req, role))
        return true;

      return super.authorize(req, res, role);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
      log.info( UsageLog.setupRequestContext(req));

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
            log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_TEMPORARY_REDIRECT, -1 ) );
            return;

          } else {
            res.setStatus(HttpServletResponse.SC_OK); // someone came directly to this page
            log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1));
            return;
          }
        }
      }

      res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not authorized to access this dataset.");
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_UNAUTHORIZED, -1 ));
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
