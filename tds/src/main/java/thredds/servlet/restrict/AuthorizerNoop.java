/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.servlet.restrict;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * No-op implementation of Authorizer. Always returns authorize=true.
 *
 * @author caron
 * @since Dec 28, 2009
 */


public class AuthorizerNoop implements Authorizer {

  @Override
  public void setRoleSource(RoleSource roleSource) {
  }

  @Override
  public boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException, ServletException {
    return true;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
  }
}