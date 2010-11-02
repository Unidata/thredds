package thredds.server.serverinfo;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.server.config.TdsContext;
import thredds.server.config.TdsServerInfo;
import thredds.servlet.UsageLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class ServerInfoController extends AbstractController
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;

  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );
    // Get the request path.
    String reqPath = request.getServletPath();
    if ( reqPath == null )
    {
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ) );
      response.sendError( HttpServletResponse.SC_NOT_FOUND );
      return null;
    }

    Map<String,Object> model = new HashMap<String,Object>();
    model.put( "serverInfo", this.tdsContext.getServerInfo() );
    model.put( "webappName", this.tdsContext.getWebappName() );
    model.put( "webappVersion", this.tdsContext.getWebappVersion() );
    model.put( "webappVersionBuildDate", this.tdsContext.getWebappVersionBuildDate() );
    
    if ( reqPath.equals( "/serverInfo.html" )) {
      return new ModelAndView( "thredds/server/serverinfo/serverInfo_html", model);
    } else if ( reqPath.equals( "/serverInfo.xml" )) {
      return new ModelAndView( "thredds/server/serverinfo/serverInfo_xml", model );
    } else if ( reqPath.equals( "/serverVersion.txt" )) {
      return new ModelAndView( "thredds/server/serverinfo/serverVersion_txt", model );
    } else{
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ) );
      response.sendError( HttpServletResponse.SC_NOT_FOUND );
      return null;
    }
  }
}