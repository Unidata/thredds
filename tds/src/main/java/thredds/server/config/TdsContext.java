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

import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import thredds.util.filesource.*;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.ServletUtil;
import thredds.catalog.InvDatasetScan;
import ucar.nc2.util.IO;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsContext
{
//  ToDo Once Log4j config is called by Spring listener instead of ours, use this logger instead of System.out.println.
//  private org.slf4j.Logger logServerStartup =
//          org.slf4j.LoggerFactory.getLogger( "serverStartup" );

  private String webappName;
  private String webappVersion;
  private String webappVersionBrief;
  private String webappVersionBuildDate;

  private String contextPath;

  private String contentRootPath;

  private String contentPath;
  private String startupContentPath;
  private String iddContentPath;
  private String motherlodeContentPath;
  private String webinfPath;

  private File rootDirectory;
  private File contentDirectory;
  private File publicContentDirectory;
  private File tomcatLogDir;

  private File startupContentDirectory;
  private File iddContentDirectory;
  private File motherlodeContentDirectory;

  private DescendantFileSource rootDirSource;
  private DescendantFileSource contentDirSource;
  private DescendantFileSource publicContentDirSource;
  private DescendantFileSource startupContentDirSource;
  private DescendantFileSource iddContentPublicDirSource;
  private DescendantFileSource motherlodeContentPublicDirSource;

  private FileSource configSource;
  private FileSource publicDocSource;

  private RequestDispatcher defaultRequestDispatcher;
  private RequestDispatcher jspRequestDispatcher;

  private String tdsConfigFileName;
  private HtmlConfig htmlConfig;
  private TdsServerInfo serverInfo;
  private WmsConfig wmsConfig;

  public TdsContext() {}

  public void setWebappVersion( String verFull ) { this.webappVersion = verFull; }
  public void setWebappVersionBrief( String ver) { this.webappVersionBrief = ver; }
  public void setWebappVersionBuildDate( String buildDateString) { this.webappVersionBuildDate = buildDateString; }

  public void setContentRootPath( String contentRootPath) {this.contentRootPath = contentRootPath; }
  
  public void setContentPath( String contentPath) {this.contentPath = contentPath; }
  public void setStartupContentPath( String startupContentPath ) { this.startupContentPath = startupContentPath; }
  public void setIddContentPath( String iddContentPath ) { this.iddContentPath = iddContentPath; }
  public void setMotherlodeContentPath( String motherlodeContentPath ) { this.motherlodeContentPath = motherlodeContentPath; }

  public void setTdsConfigFileName( String filename ) {
    this.tdsConfigFileName = filename;
  }

  public String getTdsConfigFileName() {
    return this.tdsConfigFileName;
  }

  public void setServerInfo( TdsServerInfo serverInfo ) {
    this.serverInfo = serverInfo;
  }

  public TdsServerInfo getServerInfo() {
    return serverInfo;
  }

  public void setHtmlConfig( HtmlConfig htmlConfig ) {
    this.htmlConfig = htmlConfig;
  }

  public HtmlConfig getHtmlConfig() {
    return this.htmlConfig;
  }

  public WmsConfig getWmsConfig() {
    return wmsConfig;
  }

  public void setWmsConfig( WmsConfig wmsConfig ) {
    this.wmsConfig = wmsConfig;
  }

  public void destroy() {}

  public void init( ServletContext servletContext )
  {
    if ( servletContext == null )
      throw new IllegalArgumentException( "ServletContext must not be null.");

    // ToDo LOOK - Are we still using this.
    ServletUtil.initDebugging( servletContext );

    // Set the webapp name.
    this.webappName = servletContext.getServletContextName();

    // Set the context path.
    // Servlet 2.5 allows the following.
    //contextPath = servletContext.getContextPath();
    String tmpContextPath = servletContext.getInitParameter( "ContextPath" );  // cannot be overridden in the ThreddsConfig file
    if ( tmpContextPath == null ) tmpContextPath = "thredds";
    contextPath = "/" + tmpContextPath;
    // ToDo LOOK - Get rid of need for setting contextPath in ServletUtil.
    ServletUtil.setContextPath( contextPath );

    // Check the version.
    if ( this.webappVersion != null
         && ! webappVersion.startsWith( this.webappVersionBrief + "." ))
    {
      String msg = "Full version [" + this.webappVersion + "] must start with version [" + this.webappVersionBrief + "].";
      System.out.println( "ERROR - TdsContext.init(): " + msg );
      //logServerStartup.error( "TdsContext.init(): " + msg );
      throw new IllegalStateException( msg );
    }

    // Set the root directory and source.
    String rootPath = servletContext.getRealPath( "/" );
    if ( rootPath == null )
    {
      String msg = "Webapp [" + this.webappName + "] must run with exploded deployment directory (not from .war).";
      System.out.println( "ERROR - TdsContext.init(): " + msg );
      //logServerStartup.error( "TdsContext.init(): " + msg );
      throw new IllegalStateException( msg );
    }
    this.rootDirectory = new File( rootPath );
    this.rootDirSource = new BasicDescendantFileSource( this.rootDirectory );
    this.rootDirectory = this.rootDirSource.getRootDirectory();
    // ToDo LOOK - Get rid of need for setting rootPath in ServletUtil.
    ServletUtil.setRootPath( this.rootDirSource.getRootDirectoryPath());

    // Set the startup (initial install) content directory and source.
    this.startupContentDirectory = new File( this.rootDirectory, this.startupContentPath );
    this.startupContentDirSource = new BasicDescendantFileSource( this.startupContentDirectory );
    this.startupContentDirectory = this.startupContentDirSource.getRootDirectory();

    this.webinfPath = this.rootDirectory +"/WEB-INF";


    // set the tomcat logging directory
    try
    {
      String base = System.getProperty("catalina.base");
      if (base != null)
      {
        this.tomcatLogDir = new File( base, "logs").getCanonicalFile();
        if ( !this.tomcatLogDir.exists() )
        {
          String msg = "'catalina.base' directory not found";
          System.out.println( "WARN - TdsContext.init(): " + msg );
          //logServerStartup.error( "TdsContext.init(): " + msg );
        }
      }
      else
      {
        String msg = "'catalina.base' property not found - probably not a tomcat server";
        System.out.println( "WARN - TdsContext.init(): " + msg );
        //logServerStartup.warn( "TdsContext.init(): " + msg );
      }

    } catch (IOException e)
    {
      String msg = "tomcatLogDir could not be created";
      System.out.println( "WARN - TdsContext.init(): " + msg );
      //logServerStartup.error( "TdsContext.init(): " + msg );
    }

    // Set the content directory and source.
    File contentRootDir = new File( this.contentRootPath );
    if ( ! contentRootDir.isAbsolute() )
      this.contentDirectory = new File( new File( this.rootDirectory, this.contentRootPath ), this.contentPath );
    else
    {
      if ( contentRootDir.isDirectory() )
        this.contentDirectory = new File( contentRootDir, this.contentPath );
      else
      {
        String msg = "Content root directory [" + this.contentRootPath + "] not a directory.";
        System.out.println( "ERROR - TdsContext.init(): " + msg );
        //logServerStartup.error( "TdsContext.init(): " + msg );
        throw new IllegalStateException( msg );
      }
    }
    // If the content directory doesn't exist, try to copy startup content directory.
    if ( ! this.contentDirectory.exists() )
    {
      try
      {
        IO.copyDirTree( this.startupContentDirectory.getPath(), this.contentDirectory.getPath() );
      }
      catch ( IOException e )
      {
        String tmpMsg = "Content directory does not exist and could not be created";
        System.out.println( "ERROR - TdsContext.init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]." );
        //logServerStartup.error( "TdsContext.init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]" );
        throw new IllegalStateException( tmpMsg );
      }
    }

    // If content directory exists, make sure it is a directory.
    if ( this.contentDirectory.isDirectory() )
    {
      this.contentDirSource = new BasicDescendantFileSource( StringUtils.cleanPath( this.contentDirectory.getAbsolutePath() ) );
      this.contentDirectory = this.contentDirSource.getRootDirectory();
    }
    else
    {
      String tmpMsg = "Content directory not a directory";
      System.out.println( "ERROR - TdsContext.init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]." );
      //logServerStartup.error( "TdsContext.init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]" );
      throw new IllegalStateException( tmpMsg );
    }
    ServletUtil.setContentPath( this.contentDirSource.getRootDirectoryPath());

    // read in persistent user-defined params from threddsConfig.xml
    File tdsConfigFile = this.contentDirSource.getFile( this.getTdsConfigFileName() );
    String tdsConfigFilename = tdsConfigFile != null ? tdsConfigFile.getPath() : "";
    ThreddsConfig.init( tdsConfigFilename );

    File logDir = new File( this.contentDirectory, "logs");
    if ( ! logDir.exists())
    {
      if ( ! logDir.mkdirs())
      {
        String msg = "Couldn't create TDS log directory [" + logDir.getPath() + "].";
        System.out.println( "ERROR - TdsContext.init(): " + msg);
        //logServerStartup.error( "TdsContext.init(): " + msg  );
        // ToDo thow an IllegalStateException() ????
      }
    }
    System.setProperty( "tds.log.dir", logDir.getPath() ); // variable substitution

    this.publicContentDirectory = new File( this.contentDirectory, "public");
    this.publicContentDirSource = new BasicDescendantFileSource( this.publicContentDirectory);

    this.iddContentDirectory = new File( this.rootDirectory, this.iddContentPath);
    this.iddContentPublicDirSource = new BasicDescendantFileSource( this.iddContentDirectory );

    this.motherlodeContentDirectory = new File( this.rootDirectory, this.motherlodeContentPath);
    this.motherlodeContentPublicDirSource = new BasicDescendantFileSource( this.motherlodeContentDirectory );

    List<DescendantFileSource> chain = new ArrayList<DescendantFileSource>();
    DescendantFileSource contentMinusPublicSource =
            new BasicWithExclusionsDescendantFileSource( this.contentDirectory,
                                                         Collections.singletonList( "public" ) );
    chain.add( contentMinusPublicSource );
    for ( String curContentRoot : ThreddsConfig.getContentRootList() )
    {
      if ( curContentRoot.equalsIgnoreCase( "idd" ))
        chain.add( this.iddContentPublicDirSource );
      else if ( curContentRoot.equalsIgnoreCase( "motherlode" ))
        chain.add( this.motherlodeContentPublicDirSource );
      else
      {
        try
        {
          chain.add( new BasicDescendantFileSource( StringUtils.cleanPath( curContentRoot ) ) );
        }
        catch ( IllegalArgumentException e )
        {
          String msg = "Couldn't add content root [" + curContentRoot + "]: " + e.getMessage();
          System.out.println( "WARN - TdsContext.init(): " + msg );
          //logServerStartup.warn( "TdsContext.init(): " + msg, e );
        }
      }
    }
    this.configSource = new ChainedFileSource( chain );
    this.publicDocSource = this.publicContentDirSource;

    // ToDo LOOK Find a better way once thredds.catalog2 is used.
    InvDatasetScan.setContext( contextPath );
    InvDatasetScan.setCatalogServletName( "/catalog" );

    jspRequestDispatcher = servletContext.getNamedDispatcher( "jsp" );
    defaultRequestDispatcher = servletContext.getNamedDispatcher( "default" );

    TdsConfigMapper tdsConfigMapper = new TdsConfigMapper();
    tdsConfigMapper.setTdsServerInfo( this.serverInfo );
    tdsConfigMapper.setHtmlConfig( this.htmlConfig );
    tdsConfigMapper.setWmsConfig( this.wmsConfig );
    tdsConfigMapper.init( this);
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
  public String getContextPath()
  {
    return contextPath;
  }

  /**
   * Return the context path under which this web app is running (e.g., "/thredds").
   *
   * @return the context path.
   */
  public String getWebinfPath()
  {
    return webinfPath;
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

  /**
   * Return the version string (<major>.<minor>) for this web application.
   *
   * @return the version string.
   */
  public String getWebappVersionBrief()
  {
    return this.webappVersionBrief;
  }

  public String getWebappVersionBuildDate()
  {
    return this.webappVersionBuildDate;
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
   * Return the tomcat logging directory
   *
   * @return the tomcat logging directory.
   */
  public File getTomcatLogDirectory()
  {
    return tomcatLogDir;
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

  /**
   * Return File for the initial content directory. I.e., the directory
   * that contains default content for the content directory, copied
   * there when TDS is first installed.
   *
   * @return a File to the initial content directory.
   */
  public File getStartupContentDirectory()
  {
    return startupContentDirectory;
  }

  public File getIddContentDirectory()
  {
    return iddContentDirectory;
  }

  public File getMotherlodeContentDirectory()
  {
    return motherlodeContentDirectory;
  }

  public FileSource getConfigFileSource()
  {
    return this.configSource;
  }

  public FileSource getPublicDocFileSource()
  {
    return this.publicDocSource;
  }

  public RequestDispatcher getDefaultRequestDispatcher()
  {
    return this.defaultRequestDispatcher;
  }
  public RequestDispatcher getJspRequestDispatcher()
  {
    return this.jspRequestDispatcher;
  }
}
