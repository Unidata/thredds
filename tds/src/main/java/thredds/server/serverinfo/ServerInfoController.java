/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.serverinfo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import thredds.server.config.TdsContext;
import thredds.server.config.TdsServerInfoBean;

@Controller
@RequestMapping(value ="/info", method= RequestMethod.GET)
public class ServerInfoController {

  @Autowired
  private TdsServerInfoBean serverInfo;

  @Autowired
  private TdsContext tdsContext;

  @RequestMapping(value = "/serverInfo.html")
  protected ModelAndView getServerInfoHtml() {
    return new ModelAndView("thredds/server/serverinfo/serverInfo_html", getServerInfoModel());
  }

  @RequestMapping(value = "/serverInfo.xml")
  protected ModelAndView getServerInfoXML() {
    return new ModelAndView("thredds/server/serverinfo/serverInfo_xml", getServerInfoModel());
  }

  @RequestMapping(value = "/serverVersion.txt")
  protected ModelAndView getServerVersion() {
    return new ModelAndView("thredds/server/serverinfo/serverVersion_txt", getServerInfoModel());
  }

  private Map<String, Object> getServerInfoModel() {
    Map<String, Object> model = new HashMap<>();
    model.put("serverInfo", this.serverInfo);
    model.put("webappName", this.tdsContext.getWebappDisplayName());
    model.put("webappVersion", this.tdsContext.getWebappVersion());
    model.put("webappVersionBuildDate", this.tdsContext.getTdsVersionBuildDate());
    return model;
  }
}