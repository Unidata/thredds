/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmvalidator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;
import thredds.servlet.UsageLog;

/**
 * CDM Validator initializer - called on startup
 *
 * @author edavis
 * @since 4.0
 */
public class CdmValidatorConfigContextListener implements ServletContextListener
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( CdmValidatorConfigContextListener.class );

  public void contextInitialized( ServletContextEvent event )
  {
    logger.info( "contextInitialized(): start - " + UsageLog.setupNonRequestContext() );

    // Get webapp context.
    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );

    // Initialize the TDS context.
    CdmValidatorContext cdmValidatorContext = wac.getBean( "cdmValidatorContext", CdmValidatorContext.class );
    cdmValidatorContext.init( servletContext );

    logger.info( "contextInitialized(): done - " + UsageLog.closingMessageNonRequestContext() );
  }

  public void contextDestroyed( ServletContextEvent event )
  {
    logger.info( "contextDestroyed(): start." + UsageLog.setupNonRequestContext() );
    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );
    CdmValidatorContext cdmValidatorContext = wac.getBean( "cdmValidatorContext", CdmValidatorContext.class );
    cdmValidatorContext.destroy();

    logger.info( "contextDestroyed(): Done - " + UsageLog.closingMessageNonRequestContext());
  }
}