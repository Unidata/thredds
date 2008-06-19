package thredds.server.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.Log4jWebConfigurer;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsConfigContextLoaderListener
        //extends org.springframework.web.context.ContextLoaderListener
        implements ServletContextListener
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( TdsConfigContextLoaderListener.class );

  public void contextInitialized( ServletContextEvent event )
  {
    //super.contextInitialized( event );

    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );
    TdsContext tdsContext = (TdsContext) wac.getBean( "tdsContext", TdsContext.class );
    tdsContext.init( servletContext );
    Log4jWebConfigurer.initLogging( servletContext );
    logger.error( "contextInitialized(): NOT ERROR - done.");
  }

  public void contextDestroyed( ServletContextEvent event )
  {
    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );
    TdsContext tdsContext = (TdsContext) wac.getBean( "tdsContext", TdsContext.class );
    tdsContext.destroy();
    Log4jWebConfigurer.shutdownLogging( servletContext );
  }
}
