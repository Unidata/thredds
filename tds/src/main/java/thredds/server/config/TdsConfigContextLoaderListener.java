package thredds.server.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;

import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsConfigContextLoaderListener
        extends org.springframework.web.context.ContextLoaderListener
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( TdsConfigContextLoaderListener.class );

  public void contextInitialized( ServletContextEvent event )
  {
    super.contextInitialized( event );

    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );
    TdsContext tdsContext = (TdsContext) wac.getBean( "tdsContext", TdsContext.class );
    tdsContext.init( servletContext );
    TdsCatConfig tdsCatConfig = (TdsCatConfig) wac.getBean( "tdsCatConfig", TdsCatConfig.class );
    tdsCatConfig.init();

  }
}
