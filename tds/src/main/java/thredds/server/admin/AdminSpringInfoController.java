/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Show spring request maps
 *
 * @author caron
 * @since 10/23/13
 * @see "http://www.java-allandsundry.com/2012/03/endpoint-documentation-controller-for.html"
 */
@Controller
@RequestMapping(value ="/admin/spring", method= RequestMethod.GET)
public class AdminSpringInfoController {

  @Autowired
  private RequestMappingHandlerMapping handlerMapping;

  @RequestMapping(value = "/**", method = RequestMethod.GET)
  public ModelAndView show() {
    return new ModelAndView("springRequestMap", "handlerMethods", this.handlerMapping.getHandlerMethods());
  }

}
