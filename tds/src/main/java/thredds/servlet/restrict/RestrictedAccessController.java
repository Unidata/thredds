/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet.restrict;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Implements restricted datasets.
 * Can plug in your own Authorizer.
 *
 * @author caron
 */
@Controller("RestrictedAccess")
@RequestMapping("/restrictedAccess")
public class RestrictedAccessController {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestrictedAccessController.class);

  @Autowired
  Authorizer handler;

  @RequestMapping(value="**", method= RequestMethod.GET)
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (log.isDebugEnabled()) log.debug("RestrictedAccessController.get req=" + ServletUtil.getRequest(req));
    handler.doGet(req, res);
  }

}
