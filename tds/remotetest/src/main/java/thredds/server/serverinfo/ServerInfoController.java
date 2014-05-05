package thredds.server.serverinfo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import thredds.server.config.TdsContext;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
@Controller
public class ServerInfoController
{
  //private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  @Autowired
  private TdsContext tdsContext;


  @RequestMapping(value="/serverInfo.html")
  protected ModelAndView getServerInfoHtml(){
    return new ModelAndView( "thredds/server/serverinfo/serverInfo_html", getServerInfoModel());    
  }
  
  
  @RequestMapping(value="/serverInfo.xml")
  protected ModelAndView getServerInfoXML(){
	 return new ModelAndView( "thredds/server/serverinfo/serverInfo_xml", getServerInfoModel());
  }  
  
  @RequestMapping(value="/serverVersion.txt")
  protected ModelAndView getServerVersion(){
	  return new ModelAndView( "thredds/server/serverinfo/serverVersion_txt", getServerInfoModel() );
  }  
  
  private Map<String, Object> getServerInfoModel(){
	  
	    Map<String,Object> model = new HashMap<String,Object>();
	    model.put( "serverInfo", this.tdsContext.getServerInfo() );
	    model.put( "webappName", this.tdsContext.getWebappName() );
	    model.put( "webappVersion", this.tdsContext.getWebappVersion() );
	    model.put( "webappVersionBuildDate", this.tdsContext.getWebappVersionBuildDate() );	  
	    
	    return model;
  }
}