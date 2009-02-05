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
package thredds.server.cataloggen;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import thredds.server.config.TdsContext;
import thredds.servlet.UsageLog;
import thredds.servlet.ThreddsConfig;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenController extends AbstractController
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenController.class );

  private boolean allow;
  private String servletName = "cataloggen";
  private String catGenConfigDirName = "config";
  private String catGenConfigFileName = "config.xml";
  private String catGenResultCatalogsDirName = "catalogs";

  private CatGenConfig config;
  private CatGenConfigParser configParser;

  private CatGenTaskScheduler scheduler;

  private TdsContext tdsContext;
  private CatGenContext catGenContext;

  public void setTdsContext( TdsContext tdsContext ) { this.tdsContext = tdsContext; }
  public void setCatGenContext( CatGenContext catGenContext ) { this.catGenContext = catGenContext; }

  public void init()
  {
    log.info( "init(): " + UsageLog.setupNonRequestContext() );
    this.allow = ThreddsConfig.getBoolean( "CatalogGen.allow", false );
    if ( ! this.allow )
    {
      String msg = "CatalogGen not enabled in threddsConfig.xml.";
      log.info( "init(): " + msg );
      log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
      return;
    }

    WebApplicationContext webAppContext = this.getWebApplicationContext();
    ServletContext sc = webAppContext.getServletContext();
    catGenContext.init( tdsContext, servletName, catGenConfigDirName, catGenConfigFileName, catGenResultCatalogsDirName);

    // Some debug info.
    log.debug( "init(): CatGen content path = " + this.catGenContext.getContentDirectory() );
    log.debug( "init(): CatGen config path = " + this.catGenContext.getConfigDirectory() );
    log.debug( "init(): CatGen config file = " + this.catGenContext.getConfigFile() );
    log.debug( "init(): CatGen result path = " + this.catGenContext.getResultDirectory() );

    this.configParser = new CatGenConfigParser();
    if ( catGenContext.getConfigFile().exists())
    {
      try
      {
        this.config = configParser.parseXML( catGenContext.getConfigFile() );
      }
      catch ( IOException e )
      {
        log.error( "init(): Failed to parse config file (disabling CatalogGen): " + e.getMessage() );
        this.allow = false;
        return;
      }
    }
    else
    {
      log.error( "init(): Config file does not exist (disabling CatalogGen)." );
      this.allow = false;
      return;
    }

    if ( catGenContext.getConfigDirectory().exists()
         && catGenContext.getResultDirectory().exists()
         && ! this.config.getTaskInfoList().isEmpty() )
    {
      this.scheduler = new CatGenTaskScheduler( this.config,
                                                catGenContext.getConfigDirectory(),
                                                catGenContext.getResultDirectory() );
      this.scheduler.start();
    }
    log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy()
  {
    log.info( "destroy(): " + UsageLog.setupNonRequestContext() );
    if ( this.scheduler != null)
      this.scheduler.stop();
    log.info( "destroy()" + UsageLog.closingMessageNonRequestContext() );
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest req, HttpServletResponse res ) throws Exception
  {
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( req ) );

    if ( ! this.allow )
    {
      String msg = "CatalogGen service not supported.";
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, msg.length() ) );
      res.sendError( HttpServletResponse.SC_FORBIDDEN, msg );
      return null;
    }
    return new ModelAndView( "editTask", "config", "junk" );
    //return new ModelAndView( "thredds/server/cataloggen/index", "config", this.config );
  }
}
