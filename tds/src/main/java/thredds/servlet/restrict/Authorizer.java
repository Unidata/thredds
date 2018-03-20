/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet.restrict;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * An implementation must have a no-arg constructor, so it can be created through reflection.
 * This design allows for third party plug-ins.
 *
 * @author caron
 */
public interface Authorizer {

  /**
   * Set the role source, if there is one. If not, assume no role authentication is needed.
   * use RoleSource.hasRole() to test for role.
   *
   * @param roleSource tells whether a user has the named role.
   */
  void setRoleSource( RoleSource roleSource);

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
  boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException, ServletException;

  /**
   * Process this request. May be a no-op. 
   * @param req the request
   * @param res the response
   * @throws IOException I/O error, eg network
   * @throws ServletException other errors
   */
  void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException;
}
