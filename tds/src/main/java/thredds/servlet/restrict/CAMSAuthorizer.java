/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet.restrict;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * CAMS authorizarion.
 *
 * @author caron
 */
public class CAMSAuthorizer extends TomcatAuthorizer {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

  public boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException {
    if (hasCAMSrole(req, role))
      return true;

    return super.authorize(req, res, role);
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    HttpSession session = req.getSession();
    if (session != null) {
      String origURI = (String) session.getAttribute("origRequest");
      String role = (String) session.getAttribute("role");

      if (req.isUserInRole(role)) {

        // transfer CAS roles to this session
        List<String> rolesArray = new ArrayList<>();
        java.util.Enumeration<String> rolesEnum = req.getHeaders("CAMS-HTTP-ROLE");
        while (rolesEnum.hasMoreElements())
          rolesArray.add(rolesEnum.nextElement());
        session.setAttribute("camsRoles", rolesArray);

        if (origURI != null) {
          if (log.isDebugEnabled()) log.debug("redirect to origRequest = " + origURI);
          res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
          String frag = (origURI.indexOf("?") > 0) ? "&auth" : "?auth";
          res.addHeader("Location", origURI + frag);
          return;

        } else {
          res.setStatus(HttpServletResponse.SC_OK); // someone came directly to this page
          return;
        }
      }
    }

    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized to access this dataset.");
  }

  private boolean hasCAMSrole(HttpServletRequest req, String role) {
    HttpSession session = req.getSession();
    if (session != null) {
      List<String> roles = (List<String>) session.getAttribute("camsRoles");
      return (roles != null) && roles.contains(role);
    }
    return false;
  }

}
