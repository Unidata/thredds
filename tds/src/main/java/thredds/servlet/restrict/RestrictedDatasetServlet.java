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
  private static boolean debugResourceControl = false;

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
