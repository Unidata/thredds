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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * An implementation must have a no-arg constructor, so it can be created through reflection.
 *
 * @author caron
 */
public interface Authorizer {

  /**
   * Initialize with servlet parameters.
   * @param servlet get init parameters from here.
   * @throws ServletException if error
   */
  public void init(HttpServlet servlet) throws ServletException;

  /**
   * Set the role source, if there is one. If not, assume no role authentication is needed.
   * use RoleSource.hasRole() to test for role.
   *
   * @param roleSource tells whether a user has the named role.
   */
  public void setRoleSource( RoleSource roleSource);

  /**
   * Decide is this request is authorized in the named role.
   * This method must be thread-safe.
   *
   * @param req the request
   * @param res the response
   * @param role need this role
   * @return true if user is authenticated. if false, must set res.setStatus().
   * @throws IOException I/O error, eg network
   * @throws ServletException other errors
   */
  public boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException, ServletException;

  /**
   * Process this request. May be a no-op. 
   * @param req the request
   * @param res the response
   * @throws IOException I/O error, eg network
   * @throws ServletException other errors
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException;
}
