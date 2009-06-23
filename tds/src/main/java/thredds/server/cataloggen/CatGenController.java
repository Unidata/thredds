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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.HashMap;

import thredds.server.config.TdsContext;
import thredds.servlet.UsageLog;
import thredds.servlet.ThreddsConfig;
import thredds.util.RequestForwardUtils;

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
  private org.slf4j.Logger logServerStartup =
          org.slf4j.LoggerFactory.getLogger( "serverStartup" );

  private TdsContext tdsContext;
  private boolean allow;

  private String catGenConfigDirName;
  private File catGenConfigDir;
  private String catGenConfigFileName;
  private File catGenConfigFile;
  private String catGenResultsDirName;
  private File catGenResultsDir;

  private CatGenConfig catGenConfig;
  private CatGenConfigParser catGenConfigParser;

  private CatGenTaskScheduler scheduler;

  public void setTdsContext( TdsContext tdsContext ) { this.tdsContext = tdsContext; }
  public void setCatGenConfigDirName( String catGenConfigDirName ) { this.catGenConfigDirName = catGenConfigDirName; }
  public void setCatGenConfigFileName( String catGenConfigFileName ) { this.catGenConfigFileName = catGenConfigFileName; }
  public void setCatGenResultsDirName( String catGenResultsDirName ) { this.catGenResultsDirName = catGenResultsDirName; }

  public void init()
  {
    logServerStartup.info( "CatalogGen - initialize start - " + UsageLog.setupNonRequestContext() );

    // Check that CatalogGen is enabled.
    this.allow = ThreddsConfig.getBoolean( "CatalogGen.allow", false );
    if ( ! this.allow )
    {
      logServerStartup.info( "CatalogGen not enabled in threddsConfig.xml - " + UsageLog.closingMessageNonRequestContext() );
      return;
    }

    // Check that have TdsContext.
    if ( this.tdsContext == null )
    {
      this.allow = false;
      logServerStartup.error( "CatalogGen - Disabling service - null TdsContext - " + UsageLog.closingMessageNonRequestContext() );
      return;
    }

    // Locate or create CatGen config directory.
    this.catGenConfigDir = this.tdsContext.getConfigFileSource().getFile( this.catGenConfigDirName );
    if ( this.catGenConfigDir == null )
    {
      this.catGenConfigDir = createCatGenConfigDirectory();
      if ( this.catGenConfigDir == null )
      {
        this.allow = false;
        logServerStartup.error( "CatalogGen - Disabling service - could not locate or create CatGenConfig directory - " + UsageLog.closingMessageNonRequestContext() );
        return;
      }
    }

    // Locate or create CatalogGen config document.
    this.catGenConfigFile = new File( this.catGenConfigDir, this.catGenConfigFileName );
    if ( ! this.catGenConfigFile.exists() )
    {
      this.catGenConfigFile = createCatGenConfigFile();
      if ( this.catGenConfigFile == null )
      {
        this.allow = false;
        logServerStartup.error( "CatalogGen - Disabling service - could not locate or create CatGenConfig file - " + UsageLog.closingMessageNonRequestContext() );
        return;
      }
    }

    // Locate or create CatGen results directory.
    this.catGenResultsDir = this.tdsContext.getConfigFileSource().getFile( this.catGenResultsDirName );
    if ( this.catGenResultsDir == null )
    {
      this.catGenResultsDir = createCatGenResultsDirectory();
      if ( this.catGenResultsDir == null )
      {
        logServerStartup.warn( "CatalogGen - Could not locate or create CatGenResults directory - MAY cause problems when writing results."  );
      }
    }

    // Some debug info.
    logServerStartup.debug( "CatalogGen - config directory  = " + this.catGenConfigDir.toString() );
    logServerStartup.debug( "CatalogGen - config file       = " + this.catGenConfigFile.toString() );
    logServerStartup.debug( "CatalogGen - results directory = " + this.catGenResultsDir.toString() );

    this.catGenConfigParser = new CatGenConfigParser();
    try
    {
      this.catGenConfig = catGenConfigParser.parseXML( this.catGenConfigFile );
    }
    catch ( IOException e )
    {
      logServerStartup.error( "CatalogGen - Disabling service - failed to parse CatGenConfig file - "
                 + UsageLog.closingMessageNonRequestContext()
                 + " - " + e.getMessage());
      this.allow = false;
      return;
    }

    // Make sure we can write the results files.
    for ( CatGenTaskConfig curTask : this.catGenConfig.getTaskInfoList() )
    {
      String curTaskResultFileName = curTask.getResultFileName();
      if ( curTaskResultFileName.startsWith( "/" ))
      {
        curTaskResultFileName.substring( 1 );
        File curResultFileParentDir = new File( this.tdsContext.getContentDirectory(),
                                                curTaskResultFileName ).getParentFile();
        // Try to create the parent directory if it doesn't exist.
        if ( ! curResultFileParentDir.exists()
             && ! curResultFileParentDir.mkdirs() )
        {
          // ToDo Should disable this task!!!
          logServerStartup.warn( "CatalogGen - Dropping \"" + curTask.getName() + "\" CatGenConfig Task: Non-existent results directory [" + curResultFileParentDir + "] couldn't be created." );
        }
      }
    }
    if ( ! this.catGenConfig.getTaskInfoList().isEmpty() )
    {
      this.scheduler = new CatGenTaskScheduler( this.catGenConfig,
                                                this.catGenConfigDir,
                                                this.catGenResultsDir,
                                                this.tdsContext.getContentDirectory() );
      this.scheduler.start();
    }
    logServerStartup.info( "CatalogGen - initialize done -  " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy()
  {
    logServerStartup.info( "CatalogGen - destroy start - " + UsageLog.setupNonRequestContext() );
    if ( this.scheduler != null)
      this.scheduler.stop();
    logServerStartup.info( "CatalogGen - destroy done - " + UsageLog.closingMessageNonRequestContext() );
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
          throws Exception
  {
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );

    if ( ! this.allow )
    {
      String msg = "CatalogGen service not supported.";
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, msg.length() ) );
      response.sendError( HttpServletResponse.SC_FORBIDDEN, msg );
      return null;
    }

    String reqPath = request.getPathInfo();
    if ( reqPath.equals( "/" ) )
    {
      Map<String, Object> model = new HashMap<String, Object>();
      model.put( "contextPath", request.getContextPath() );
      model.put( "servletPath", request.getServletPath() );
      model.put( "catGenConfig", this.catGenConfig );
      model.put( "catGenResultsDirName", this.catGenResultsDirName );

      this.tdsContext.getHtmlConfig().addHtmlConfigInfoToModel( model );

      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1 ) );
      return new ModelAndView( "/thredds/server/cataloggen/catGenConfig", model );
    }
    else
    {
      String extendedPath = request.getServletPath() + reqPath;
      if ( extendedPath.startsWith( "/" + this.catGenResultsDirName + "/") )
      {
        String fwdPath = "/catalog" + extendedPath;
        RequestForwardUtils.forwardRequestRelativeToCurrentContext( fwdPath, request, response );
        return null;
      }

    }
      return new ModelAndView( "editTask", "config", "junk" );
    //return new ModelAndView( "thredds/server/cataloggen/index", "config", this.config );
  }

  /**
   * Return newly created CatGenConfig directory. If it already exists, return null.
   * <p/>
   * <p>This method should only be called if CatGenConfig directory was not found
   * in TdsContext ConfigFileSource. I.e., the following
   * <code>this.tdsContext.getConfigFileSource().getFile( this.catGenConfigDirName )</code>
   * returned null.
   *
   * @return the CatGenConfig directory
   */
  private File createCatGenConfigDirectory()
  {
    File configDir = new File( this.tdsContext.getContentDirectory(), this.catGenConfigDirName );
    if ( configDir.exists() )
    {
      log.error( "createCatGenConfigDirectory(): Existing CatGenConfigDir [" + configDir + "] not found in TdsContext ConfigFileSource, check TdsContext config." );
      return null;
    }

    if ( ! configDir.mkdirs() )
    {
      log.error( "createCatGenConfigDirectory(): Failed to create CatGenConfig directory." );
      return null;
    }

    if ( !configDir.equals( this.tdsContext.getConfigFileSource().getFile( this.catGenConfigDirName ) ) )
    {
      log.error( "createCatGenConfigDirectory(): Newly created CatGenConfig directory not found by TdsContext ConfigFileSource." );
      return null;
    }
    return configDir;
  }

  /**
   * Return newly created CatGenResults directory. If it already exists, return null.
   * <p/>
   * <p>This method should only be called if CatGenResults directory was not found
   * in TdsContext ConfigFileSource. I.e., the following
   * <code>this.tdsContext.getConfigFileSource().getFile( this.catGenResultsDirName )</code>
   * returned null.
   *
   * @return the CatGenResults directory
   */
  private File createCatGenResultsDirectory()
  {
    File dir = new File( this.tdsContext.getContentDirectory(), this.catGenResultsDirName );
    if ( dir.exists() )
    {
      log.error( "createCatGenResultsDirectory(): Existing CatGenResultsDir [" + dir + "] not found in TdsContext ConfigFileSource, check TdsContext config." );
      return null;
    }

    if ( ! dir.mkdirs() )
    {
      log.error( "createCatGenResultsDirectory(): Failed to create CatGenResults directory." );
      return null;
    }

    if ( ! dir.equals( this.tdsContext.getConfigFileSource().getFile( this.catGenResultsDirName ) ) )
    {
      log.error( "createCatGenResultsDirectory(): Newly created CatGenResults directory not found by TdsContext ConfigFileSource." );
      return null;
    }
    return dir;
  }

  /**
   * Return newly created CatGenConfig File or null if failed to create new file.
   * If it already exists, return null.
   *
   * @return the CatGenConfig File.
   */
  private File createCatGenConfigFile()
  {
    File configFile = new File( this.catGenConfigDir, this.catGenConfigFileName );
    if ( configFile.exists() )
    {
      log.error( "createCatGenConfigFile(): Existing catGenConfigFile [" + configFile + "] not found in TdsContext ConfigFileSource, check TdsContext config." );
      return null;
    }

    boolean created = false;
    try
    {
      created = configFile.createNewFile();
    }
    catch ( IOException e )
    {
      log.error( "createCatGenConfigFile(): I/O error while creating CatGenConfig file." );
      return null;
    }
    if ( !created )
    {
      log.error( "createCatGenConfigFile(): Failed to create CatGEnConfig file." );
      return null;
    }

    // Write blank config file. Yuck!!!
    if ( !this.writeEmptyConfigDocToFile( configFile ) )
    {
      log.error( "createCatGenConfigFile(): Failed to write empty config file [" + configFile + "]." );
      return null;
    }
    return configFile;
  }

  private boolean writeEmptyConfigDocToFile( File configFile )
  {
    FileOutputStream fos = null;
    OutputStreamWriter writer = null;
    try
    {
      fos = new FileOutputStream( configFile );
      writer = new OutputStreamWriter( fos, "UTF-8" );
      writer.append( this.genEmptyConfigDocAsString() );
      writer.flush();
    }
    catch ( IOException e )
    {
      log.debug( "writeEmptyConfigDocToFile(): IO error writing blank config file: " + e.getMessage() );
      return false;
    }
    finally
    {
      try
      {
        if ( writer != null )
          writer.close();
        if ( fos != null )
          fos.close();
      }
      catch ( IOException e )
      {
        log.debug( "writeEmptyConfigDocToFile(): IO error closing just written blank config file: " + e.getMessage() );
        return true;
      }
    }
    return true;
  }

  private String genEmptyConfigDocAsString()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<preferences EXTERNAL_XML_VERSION='1.0'>\n" )
            .append( "  <root type='user'>\n" )
            .append( "    <map>\n" )
            .append( "      <beanCollection key='config' class='thredds.cataloggen.servlet.CatGenTimerTask'>\n" )
            .append( "      </beanCollection>\n" )
            .append( "    </map>\n" )
            .append( "  </root>\n" )
            .append( "</preferences>" );
    return sb.toString();
  }

}
