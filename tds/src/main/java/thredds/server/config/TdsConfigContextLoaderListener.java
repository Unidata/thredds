package thredds.server.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;

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
    //tdsContext.init( event.getServletContext() );

  }
}
