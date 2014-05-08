package thredds.server.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Show spring request maps
 *
 * @author caron
 * @since 10/23/13
 * @see " http://www.java-allandsundry.com/2012/03/endpoint-documentation-controller-for.html"
 */
@Controller
public class SpringDocController {

  @Autowired
  private RequestMappingHandlerMapping handlerMapping;

  @RequestMapping(value = "/requestMaps", method = RequestMethod.GET)
  public void show(Model model) {
    model.addAttribute("handlerMethods", this.handlerMapping.getHandlerMethods());
  }

}
