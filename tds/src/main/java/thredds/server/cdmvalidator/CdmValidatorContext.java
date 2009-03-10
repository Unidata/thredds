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
package thredds.server.cdmvalidator;

import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import thredds.util.filesource.*;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.ServletUtil;
import thredds.server.config.TdsContext;
import thredds.server.config.TdsConfigHtml;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CdmValidatorContext
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TdsContext.class );

  private String webappName;
  private String webappVersion;
  private String webappVersionBrief;
  private String webappVersionBuildDate;

  private String contextPath;

  private String contentRootPath;

  private String contentPath;

  private File rootDirectory;
  private File contentDirectory;

  private FileSource configSource;
  private FileSource publicDocSource;

  private RequestDispatcher defaultRequestDispatcher;
  private RequestDispatcher jspRequestDispatcher;

  private String configFileName;

  private TdsConfigHtml htmlConfig;

  public CdmValidatorContext() {}

  public void setWebappVersion( String verFull ) { this.webappVersion = verFull; }
  public void setWebappVersionBrief( String ver) { this.webappVersionBrief = ver; }
  public void setWebappVersionBuildDate( String buildDateString) { this.webappVersionBuildDate = buildDateString; }

  public void setContentRootPath( String contentRootPath) {this.contentRootPath = contentRootPath; }

  public void setContentPath( String contentPath) {this.contentPath = contentPath; }

  public void setConfigFileName( String filename ) { this.configFileName = filename; }
  public String getConfigFileName() { return this.configFileName; }

  public void setHtmlConfig( TdsConfigHtml htmlConfig ) { this.htmlConfig = htmlConfig; }
  public TdsConfigHtml getHtmlConfig() { return this.htmlConfig; }

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
    if ( tmpContextPath == null ) tmpContextPath = "cdmvalidator";
    contextPath = "/" + tmpContextPath;
    
    // ToDo LOOK - Get rid of need for setting contextPath in ServletUtil.
    ServletUtil.setContextPath( contextPath );

    // Check the version.
    if ( this.webappVersion != null
         && ! webappVersion.startsWith( this.webappVersionBrief + "." ))
      throw new IllegalStateException( "Full version [" + this.webappVersion + "] must start with version [" + this.webappVersionBrief + "].");

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
    // If the content directory doesn't exist, try to create it and config file.
    File configFile = new File( this.contentDirectory, this.configFileName );
    if ( ! this.contentDirectory.exists() )
    {
      if ( ! this.contentDirectory.mkdirs())
      {
        String tmpMsg = "Content directory does not exist and could not be created";
        log.error( "init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]" );
        throw new IllegalStateException( tmpMsg );
      }
      else
      {
        boolean success = false;
        try
        {
          success = configFile.createNewFile();
        }
        catch ( IOException e )
        {
          String tmpMsg = "Configuration file doesn't exist and could not be created";
          log.error( "init(): " + tmpMsg + " [" + configFile.getAbsolutePath() + "]", e );
          throw new IllegalStateException( tmpMsg, e );
        }
        if ( ! success )
        {
          String tmpMsg = "Configuration file doesn't exist and could not be created";
          log.error( "init(): " + tmpMsg + " [" + configFile.getAbsolutePath() + "]" );
          throw new IllegalStateException( tmpMsg );
        }
        else
        {
          log.error( "init(): Empty configuration file [" + configFile + "].");
          throw new IllegalStateException( "Empty configuration file." );
        }
      }
    }

    // If content directory exists, make sure it is a directory.
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

    // read in persistent user-defined params from threddsConfig.xml
    String configFilename = configFile != null ? configFile.getPath() : "";
    ThreddsConfig.init( servletContext, configFilename, log );

    File publicContentDirectory = new File( this.contentDirectory, "public" );
    DescendantFileSource publicContentDirSource = new BasicDescendantFileSource( publicContentDirectory );

    this.configSource =
            new BasicWithExclusionsDescendantFileSource( this.contentDirectory,
                                                         Collections.singletonList( "public" ) );
    this.publicDocSource = publicContentDirSource;

    jspRequestDispatcher = servletContext.getNamedDispatcher( "jsp" );
    defaultRequestDispatcher = servletContext.getNamedDispatcher( "default" );

//    if ( this.htmlConfig != null )
//      this.htmlConfig.init( this);
    if ( this.htmlConfig != null )
      this.htmlConfig.init( this.getWebappName(),
                               this.getWebappVersion(),
                               this.getWebappVersionBrief(),
                               this.getWebappVersionBuildDate(),
                               this.getWebappContextPath() );
  }

  public void destroy()
  {
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