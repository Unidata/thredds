package thredds.server.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.Log4jWebConfigurer;
import thredds.servlet.DataRootHandler;
import thredds.servlet.RestrictedAccessConfigListener;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsConfigContextListener
        implements ServletContextListener
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( TdsConfigContextListener.class );

  public void contextInitialized( ServletContextEvent event )
  {
    System.out.println( "TdsConfigContextListener.contextInitialized(): start." );

    // Get webapp context.
    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );

    // Initialize the TDS context.
    TdsContext tdsContext = (TdsContext) wac.getBean( "tdsContext", TdsContext.class );
    tdsContext.init( servletContext );
    Log4jWebConfigurer.initLogging( servletContext );

    // Initialize the DataRootHandler.
    DataRootHandler catHandler = (DataRootHandler) wac.getBean( "tdsDRH", DataRootHandler.class );
    catHandler.registerConfigListener( new RestrictedAccessConfigListener() );
    catHandler.init();
    DataRootHandler.setInstance( catHandler );

    logger.debug( "contextInitialized(): done.");
  }

  public void contextDestroyed( ServletContextEvent event )
  {
    logger.debug( "contextDestroyed(): start." );
    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );
    TdsContext tdsContext = (TdsContext) wac.getBean( "tdsContext", TdsContext.class );
    tdsContext.destroy();
    Log4jWebConfigurer.shutdownLogging( servletContext );
  }
}
