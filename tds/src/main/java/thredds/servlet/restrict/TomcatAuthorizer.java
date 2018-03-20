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

import org.springframework.beans.factory.annotation.Autowired;
import thredds.server.config.TdsContext;
import thredds.servlet.ServletUtil;

/**
 * Use Tomcat security.
 *
 * @author caron
 */
public class TomcatAuthorizer implements Authorizer {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TomcatAuthorizer.class);

  @Autowired
  private TdsContext tdsContext;

  private boolean useSSL = false;
  private String sslPort = "8443";

  public void setUseSSL(boolean useSSL) {
    this.useSSL = useSSL;
  }

  public void setSslPort(String sslPort) {
    this.sslPort = sslPort;
  }

  public void setRoleSource(RoleSource db) {
    // not used
  }

  public boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException {
    if (log.isDebugEnabled()) log.debug("TomcatAuthorizer.authorize req=" + ServletUtil.getRequest(req));

    if (req.isUserInRole(role)) {
      if (log.isDebugEnabled()) log.debug("TomcatAuthorizer.authorize ok for role {}", role);
      return true;
    }
    
    // redirect for authentication / authorization
    HttpSession session = req.getSession();
    session.setAttribute("origRequest", ServletUtil.getRequest(req));
    session.setAttribute("role", role);

    String urlr = useSSL ? "https://" + req.getServerName() + ":"+ sslPort + tdsContext.getContextPath()+"/restrictedAccess/" + role :
                           "http://" + req.getServerName() + ":"+ req.getServerPort() + tdsContext.getContextPath()+"/restrictedAccess/" + role;

    if (log.isDebugEnabled()) log.debug("redirect to = {}", urlr);
    res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    res.addHeader("Location", urlr);
    res.setHeader("Last-Modified", "");  // LOOK
    return false;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    HttpSession session = req.getSession();
    if (session != null) {
      String origURI = (String) session.getAttribute("origRequest");
      String role = (String) session.getAttribute("role");

      if (req.isUserInRole(role)) {

        if (origURI != null) {
          res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
          String frag = (origURI.indexOf("?") > 0) ? "&auth" : "?auth";  // WTF ?? breaks simple authentication, eg on opendap
          //res.addHeader("Location", origURI+frag); // comment out for now 12/22/2010 - needed for CAS or CAMS or ESG ?
          res.addHeader("Location", origURI);
          if (log.isDebugEnabled()) log.debug("redirect to origRequest = " + origURI); // +frag);
          return;

        } else {
          res.setStatus(HttpServletResponse.SC_OK); // someone came directly to this page
          return;
        }
      }
    }

    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized to access this dataset.");
  }

}
