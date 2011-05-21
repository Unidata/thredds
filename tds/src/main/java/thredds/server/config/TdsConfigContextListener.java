/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.Log4jWebConfigurer;
import thredds.inventory.CollectionUpdater;
import thredds.servlet.*;

/**
 * This is the first TDS code called in the servlet init sequence.
 * Configured in web.xml.
 *
 * @author edavis
 * @since 4.0
 */
public class TdsConfigContextListener implements ServletContextListener {
  private org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger( "serverStartup" );

  public void contextInitialized( ServletContextEvent event ) {
    // ToDo Instead of stdout, use servletContext.log( "...")  [NOTE: it writes to localhost.*.log rather than catalina.out].
    System.out.println( "TdsConfigContextListener.contextInitialized(): start." );

    // Get webapp context.
    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );

    // Initialize the TDS context.
    TdsContext tdsContext = (TdsContext) wac.getBean( "tdsContext", TdsContext.class );
    tdsContext.init( servletContext );

    // tdsContext.init() call above initializes tds.log.dir system property
    // which is used in log4j.xml file loaded here.
    Log4jWebConfigurer.initLogging( servletContext );
    logServerStartup.info( "TdsConfigContextListener.contextInitialized() start[2]: " + UsageLog.setupNonRequestContext() );

    // Initialize the CDM, now that tdsContext is ready
    CdmInit cdmInit = (CdmInit) wac.getBean( "cdmInit", CdmInit.class );
    cdmInit.init( tdsContext);
    logServerStartup.info( "TdsConfigContextListener: CdmInit done" );

    // Initialize the DataRootHandler.
    DataRootHandler catHandler = (DataRootHandler) wac.getBean( "tdsDRH", DataRootHandler.class );
    catHandler.registerConfigListener( new RestrictedAccessConfigListener() );

    catHandler.init();
    DataRootHandler.setInstance( catHandler );
    logServerStartup.info( "TdsConfigContextListener: DataRootHandler done" );

    // YUCK! This is done so that not-yet-Spring-ified servlets can access the singleton HtmlWriter.
    // LOOK! ToDo This should be removed once the catalog service controllers uses JSP.
    HtmlWriter htmlWriter = (HtmlWriter) wac.getBean( "htmlWriter", HtmlWriter.class );
    htmlWriter.setSingleton( htmlWriter );

    logServerStartup.info( "TdsConfigContextListener.contextInitialized(): done - " + UsageLog.closingMessageNonRequestContext() );
  }

  public void contextDestroyed( ServletContextEvent event ) {
    logServerStartup.info( "TdsConfigContextListener.contextDestroyed(): start." + UsageLog.setupNonRequestContext() );
    CollectionUpdater.INSTANCE.shutdown();

    ServletContext servletContext = event.getServletContext();
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );
    TdsContext tdsContext = (TdsContext) wac.getBean( "tdsContext", TdsContext.class );
    tdsContext.destroy();

    logServerStartup.info( "TdsConfigContextListener.contextDestroyed(): Done except for shutdownLogging() - " + UsageLog.closingMessageNonRequestContext());
    Log4jWebConfigurer.shutdownLogging( servletContext );
  }
}
