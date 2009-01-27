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
