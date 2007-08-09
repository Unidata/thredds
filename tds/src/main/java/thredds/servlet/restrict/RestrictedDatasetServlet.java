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

package thredds.servlet.restrict;

import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Implements restricted datasets.
 * Can plug in your own Authorizer.
 * Its a servlet in case you want to use /thredds/restrictedDataset/* as a "guard page".
 *
 * @author caron
 */
public class RestrictedDatasetServlet extends HttpServlet {
  private static org.slf4j.Logger log;
  private static Authorizer handler = new TomcatAuthorizer();
  private static boolean initOK = false;
  private static boolean debugResourceControl = true;

  public void init() throws ServletException {
    super.init();

    log = org.slf4j.LoggerFactory.getLogger(getClass());

    String authName = getInitParameter("Authorizer");
    if (authName != null) {
      Class authClass;
      try {
        authClass = Class.forName(authName);
      } catch (ClassNotFoundException e) {
        throw new ServletException("Cant find class " + authName, e);
      }

      Authorizer authObject;
      try {
        authObject = (Authorizer) authClass.newInstance();

        String roleSourceName = getInitParameter("RoleSource");
        if (roleSourceName != null) {
          try {
            Class clazz = Class.forName(roleSourceName);
            RoleSource rs = (RoleSource) clazz.newInstance();
            authObject.setRoleSource(rs);
          } catch (ClassNotFoundException e) {
            log.error("Failed to instantiate " + roleSourceName, e);
            throw new ServletException("Failed to instantiate " + roleSourceName, e);
          }
        } else {

          String roleDBfile = getInitParameter("RoleDatabase");
          if (roleDBfile != null) {
            RoleDatabase db;
            try {
              db = new RoleDatabase(roleDBfile);
              authObject.setRoleSource(db);
            } catch (IOException e) {
              log.error("Failed to read in RoleDatabase " + roleDBfile, e);
              throw new ServletException("Failed to read in RoleDatabase " + roleDBfile, e);
            }
          }
        }

      } catch (InstantiationException e) {
        log.error("Cant instantiate class " + authName, e);
        throw new ServletException("Cant instantiate class " + authName, e);
      } catch (IllegalAccessException e) {
        log.error("Cant access class " + authName, e);
        throw new ServletException("Cant access class " + authName, e);
      }

      authObject.init(this);
      handler = authObject;
    }

    initOK = true;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    // ServletUtil.logServerAccessSetup( req );
    if (debugResourceControl) System.out.println("RestrictedDatasetServlet = " + ServletUtil.getRequest(req));
    handler.doGet(req, res);
  }

  static public boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException, ServletException {
    return initOK && handler.authorize(req, res, role);
  }

}
