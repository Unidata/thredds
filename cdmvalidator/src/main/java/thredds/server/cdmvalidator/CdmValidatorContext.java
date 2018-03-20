/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmvalidator;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import thredds.util.filesource.*;
import thredds.server.config.ThreddsConfig;
import thredds.servlet.ServletUtil;
import thredds.servlet.UsageLog;
import thredds.server.config.HtmlConfigBean;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.DiskCache;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CdmValidatorContext
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( CdmValidatorContext.class );

  private String webappName;
  private String webappVersion;
  private String webappVersionBuildDate;

  private String contextPath;

  private String contentRootPath;

  private String contentPath;

  private File rootDirectory;
  private File contentDirectory;

  private FileSource configSource;

  private String configFileName;

  private HtmlConfigBean htmlConfig;

  private RequestDispatcher defaultRequestDispatcher;
  private RequestDispatcher jspRequestDispatcher;

  private DiskCache2 cdmValidateCache = null;
  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );


  private DiskFileItemFactory fileuploadFileItemFactory;
  private File cacheDir;
  private long maxFileUploadSize;
  private boolean deleteImmediately = true;


  public CdmValidatorContext() {}

  public void setContentRootPath( String contentRootPath) {this.contentRootPath = contentRootPath; }

  public void setContentPath( String contentPath) {this.contentPath = contentPath; }

  public void setConfigFileName( String filename ) { this.configFileName = filename; }
  public String getConfigFileName() { return this.configFileName; }

  public void init( ServletContext servletContext )
  {
    if ( servletContext == null )
      throw new IllegalArgumentException( "ServletContext must not be null.");

    // Set the webapp name.
    this.webappName = servletContext.getServletContextName();

    // Set the context path.
    // Servlet 2.5 allows the following.
    //contextPath = servletContext.getContextPath();
    String tmpContextPath = servletContext.getInitParameter( "ContextPath" );  // cannot be overridden in the ThreddsConfig file
    if ( tmpContextPath == null ) tmpContextPath = "cdmvalidator";
    contextPath = "/" + tmpContextPath;
    
    // ToDo LOOK - Get rid of need for setting contextPath in ServletUtil.
    ServletUtil.setContextPath( contextPath );

    // Set the root directory and source.
    String rootPath = servletContext.getRealPath( "/" );
    if ( rootPath == null )
      throw new IllegalStateException( "Webapp [" + this.webappName + "] must run with exploded deployment directory (not from .war).");
    this.rootDirectory = new File( rootPath );
    DescendantFileSource rootDirSource = new BasicDescendantFileSource( this.rootDirectory );
    this.rootDirectory = rootDirSource.getRootDirectory();
    // ToDo LOOK - Get rid of need for setting rootPath in ServletUtil.
    ServletUtil.setRootPath( rootDirSource.getRootDirectoryPath());

    // Set the content directory and source.
    File contentRootDir = new File( this.contentRootPath );
    if ( ! contentRootDir.isAbsolute() )
      this.contentDirectory = new File( new File( this.rootDirectory, this.contentRootPath ), this.contentPath );
    else
    {
      if ( contentRootDir.isDirectory() )
        this.contentDirectory = new File( contentRootDir, this.contentPath );
      else
        throw new IllegalStateException( "Content root directory [" + this.contentRootPath + "] not a directory.");
    }

    // If the content directory doesn't exist, try to create it.
    if ( ! this.contentDirectory.exists() )
    {
      if ( ! this.contentDirectory.mkdirs())
      {
        String tmpMsg = "Content directory does not exist and could not be created";
        log.error( "init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]" );
        throw new IllegalStateException( tmpMsg );
      }
    }

    // Make sure content directory is a directory.
    DescendantFileSource contentDirSource;
    if ( this.contentDirectory.isDirectory() )
    {
      contentDirSource = new BasicDescendantFileSource( StringUtils.cleanPath( this.contentDirectory.getAbsolutePath() ) );
      this.contentDirectory = contentDirSource.getRootDirectory();
    }
    else
    {
      String tmpMsg = "Content directory not a directory";
      log.error( "init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]" );
      throw new IllegalStateException( tmpMsg );
    }
    ServletUtil.setContentPath( contentDirSource.getRootDirectoryPath());

    // If config file doesn't exist, try to create.
    File configFile = contentDirSource.getFile( this.configFileName );
    if ( configFile == null )
    {
      // Get target File.
      configFile = new File( this.contentDirectory, this.configFileName );

      // Find template configuration file.
      // ToDo LOOK - Move WEB-INF/altContent path to cdmvalidator.properties.TEMPLAT file.
      String templateConfigFilePath = "WEB-INF/altContent/" + this.configFileName;
      File templateConfigFile = rootDirSource.getFile( templateConfigFilePath );
      if ( templateConfigFile == null )
      {
        String tmpMsg = "Non-existent template configuration file";
        log.error( "init(): " + tmpMsg + " [" + templateConfigFilePath + "]" );
        throw new IllegalStateException( tmpMsg );
      }
      try
      {
        ucar.nc2.util.IO.copyFile( templateConfigFile, configFile );
      }
      catch ( IOException e )
      {
        String tmpMsg = "Configuration file doesn't exist and could not be created";
        log.error( "init(): " + tmpMsg + " [" + configFile.getAbsolutePath() + "]", e );
        throw new IllegalStateException( tmpMsg, e );
      }
    }

    // read in persistent user-defined params from threddsConfig.xml
    ThreddsConfig.init( configFile.getPath() );

    this.configSource = contentDirSource;

    jspRequestDispatcher = servletContext.getNamedDispatcher( "jsp" );
    defaultRequestDispatcher = servletContext.getNamedDispatcher( "default" );

    if ( this.htmlConfig != null )
      this.htmlConfig.init( this.getWebappName(),
                               this.getWebappVersion(),
                               this.getWebappVersionBuildDate(),
                               this.getWebappContextPath() );
    this.initCaching();
  }

  private void initCaching()
  {
    // Configure CdmValidator: max upload size; cache dir and scheme.
    maxFileUploadSize = ThreddsConfig.getBytes( "CdmValidatorService.maxFileUploadSize", (long) 1000 * 1000 * 1000 );

    String cacheDirPath = ThreddsConfig.get( "CdmValidatorService.cache.dir", new File( this.getContentDirectory(), "/cache/cdmValidate" ).getPath() );

    int scourSecs = ThreddsConfig.getSeconds( "CdmValidatorService.cache.scour", -1 );
    int maxAgeSecs = ThreddsConfig.getSeconds( "CdmValidatorService.cache.maxAge", -1 );
    final long maxSize = ThreddsConfig.getBytes( "CdmValidatorService.cache.maxSize",
                                                 (long) 1000 * 1000 * 1000 ); // 1 Gbyte
    if ( maxAgeSecs > 0 )
    {
      // Setup cache used by CDM stack (uses DiskCache which is an older static disk cache impl).
      DiskCache.setRootDirectory( cacheDirPath );
      DiskCache.setCachePolicy( true );
      if ( !scheduler.isShutdown() )
      {
        Runnable command = new Runnable()
        {
          public void run()
          {
            StringBuilder sb = new StringBuilder();
            DiskCache.cleanCache( maxSize, sb ); // 1 Gbyte
            sb.append( "----------------------\n" );
            log.debug( "init():Runnable:run(): Scour on ucar.nc2.util.DiskCache:\n" + sb );
          }
        };
        scheduler.scheduleAtFixedRate( command, scourSecs / 2, scourSecs, TimeUnit.SECONDS );
      }

      // Setup cache for file upload (uses DiskCache2 which is a newer disk cache impl).
      deleteImmediately = false;
      cdmValidateCache = new DiskCache2( cacheDirPath, false, maxAgeSecs / 60, scourSecs / 60 );
    }

    // Setup file upload factory.
    cacheDir = new File( cacheDirPath );
    if ( !cacheDir.exists() && !cacheDir.mkdirs() )
    {
      String msg = "File upload cache directory [" + cacheDir + "] doesn't exist and couldn't be created.";
      log.error( "init(): " + msg + " - " + UsageLog.closingMessageNonRequestContext() );
      throw new IllegalStateException( msg );
    }
    fileuploadFileItemFactory = new DiskFileItemFactory( 0, cacheDir ); // LOOK can also do in-memory
  }

  public void destroy()
  {
    if ( this.cdmValidateCache != null )
      this.cdmValidateCache.exit();

    if ( this.scheduler != null )
      this.scheduler.shutdown();
  }

  /**
   * Return the name of the webapp as given by the display-name element in web.xml.
   *
   * @return the name of the webapp as given by the display-name element in web.xml.
   */
  public String getWebappName()
  {
    return this.webappName;
  }

  /**
   * Return the context path under which this web app is running (e.g., "/thredds").
   *
   * @return the context path.
   */
  public String getWebappContextPath()
  {
    return contextPath;
  }

  /**
   * Return the full version string (<major>.<minor>.<bug>.<build>)
   * for this web application.
   *
   * @return the full version string.
   */
  public String getWebappVersion()
  {
    return this.webappVersion;
  }

  public void setWebappVersion( String verFull )
  {
    this.webappVersion = verFull;
  }

  public String getWebappVersionBuildDate()
  {
    return this.webappVersionBuildDate;
  }

   public void setWebappVersionBuildDate( String buildDateString)
   {
     this.webappVersionBuildDate = buildDateString;
   }

  public void setHtmlConfig( HtmlConfigBean htmlConfig )
  {
    this.htmlConfig = htmlConfig;
  }

  /**
   * Return the HtmlConfig object for this context.
   *
   * @return the HtmlConfig
   */
  public HtmlConfigBean getHtmlConfig()
  {
    return this.htmlConfig;
  }

  /**
   * Return the web apps root directory (i.e., getRealPath( "/")).
   *
   * @return the root directory for the web app.
   */
  public File getRootDirectory()
  {
    return rootDirectory;
  }

  /**
   * Return File for content directory (exists() may be false).
   *
   * @return a File to the content directory.
   */
  public File getContentDirectory()
  {
    return contentDirectory;
  }

  public FileSource getConfigFileSource()
  {
    return this.configSource;
  }

  public RequestDispatcher getDefaultRequestDispatcher()
  {
    return this.defaultRequestDispatcher;
  }
  public RequestDispatcher getJspRequestDispatcher()
  {
    return this.jspRequestDispatcher;
  }

  public static Logger getLog()
  {
    return log;
  }

  public File getCacheDir()
  {
    return cacheDir;
  }

  public long getMaxFileUploadSize()
  {
    return maxFileUploadSize;
  }

  public boolean isDeleteImmediately()
  {
    return deleteImmediately;
  }

  public FileItemFactory getFileuploadFileItemFactory()
  {
    return this.fileuploadFileItemFactory;
  }
}