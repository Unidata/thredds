package thredds.dqc.server;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.config.TdsContext;
import thredds.servlet.UsageLog;
import thredds.servlet.ThreddsConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DqcServiceController extends AbstractController
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;

  private boolean allow = false;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  public void init()
  {
    log.info( "init(): " + UsageLog.setupNonRequestContext() );
    this.allow = ThreddsConfig.getBoolean( "CatalogGen.allow", false );
    if ( !this.allow )
    {
      String msg = "CatalogGen not enabled in threddsConfig.xml.";
      log.info( "init(): " + msg );
      log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
      return;
    }

    // read config
    // setup scheduled events

    log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy()
  {
    log.info( "destroy(): " + UsageLog.setupNonRequestContext() );

    // Shutdown all scheduled events

    // clean up anything else needed

//    if ( this.scheduler != null )
//      this.scheduler.stop();
    log.info( "destroy()" + UsageLog.closingMessageNonRequestContext() );
  }


  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    // Gather diagnostics for logging request.
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );

    if ( ! this.allow )
    {
      String msg = "CatalogGen service not supported.";
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, msg.length() ) );
      response.sendError( HttpServletResponse.SC_FORBIDDEN, msg );
      return null;
    }

    // Handle requests.

    return null;
  }
}
