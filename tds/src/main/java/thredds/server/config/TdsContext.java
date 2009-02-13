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
import ucar.nc2.util.IO;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsContext
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TdsContext.class );

  private String webappName;
  private String webappVersion;
  private String webappVersionFull;
  private String webappVersionBuildDate;

  private String contextPath;

  private String contentPath;
  private String startupContentPath;
  private String iddContentPath;
  private String motherlodeContentPath;

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

  private String tdsConfigFilename;
  private TdsConfigHtml tdsConfigHtml;

  public TdsContext() {}
  public void setWebappVersion( String ver) { this.webappVersion = ver; }
  public void setWebappVersionFull( String verFull) { this.webappVersionFull = verFull; }
  public void setWebappVersionBuildDate( String buildDateString) { this.webappVersionBuildDate = buildDateString; }

  public void setContentPath( String contentPath) {this.contentPath = contentPath; }
  public void setStartupContentPath( String startupContentPath ) { this.startupContentPath = startupContentPath; }
  public void setIddContentPath( String iddContentPath ) { this.iddContentPath = iddContentPath; }
  public void setMotherlodeContentPath( String motherlodeContentPath ) { this.motherlodeContentPath = motherlodeContentPath; }

  public void setTdsConfigFileName( String filename ) { this.tdsConfigFilename = filename; }
  public String getTdsConfigFileName() { return this.tdsConfigFilename; }

  public void setTdsConfigHtml( TdsConfigHtml tdsConfigHtml ) { this.tdsConfigHtml = tdsConfigHtml; }
  public TdsConfigHtml getTdsConfigHtml() { return this.tdsConfigHtml; }

//  /**
//   * Constructor.
//   *
//   * @param startupContentPath initial content path (relative to the web app root directory).
//   * @param iddContentPath
//   * @param motherlodeContentPath
//   */
//  public TdsContext( String startupContentPath,
//                     String iddContentPath, String motherlodeContentPath )
//  {
//    if ( startupContentPath == null
//         || iddContentPath == null
//         || motherlodeContentPath == null )
//      throw new IllegalArgumentException( "Null values not allowed.");
//
//    this.startupContentPath = startupContentPath;
//    this.iddContentPath = iddContentPath;
//    this.motherlodeContentPath = motherlodeContentPath;
//  }

  public void destroy() {}

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
    if ( tmpContextPath == null ) tmpContextPath = "thredds";
    contextPath = "/" + tmpContextPath;

    // Check the version.
    if ( this.webappVersionFull != null
         && ! webappVersionFull.startsWith( this.webappVersion + "." ))
      throw new IllegalStateException( "Full version [" + this.webappVersionFull + "] must start with version [" + this.webappVersion + "].");

    // Set the root directory and source.
    this.rootDirectory = new File( servletContext.getRealPath( "/" ) );
    this.rootDirSource = new BasicDescendantFileSource( this.rootDirectory );
    this.rootDirectory = this.rootDirSource.getRootDirectory();

    // Set the startup (initial install) content directory and source.
    this.startupContentDirectory = new File( this.rootDirectory, this.startupContentPath );
    this.startupContentDirSource = new BasicDescendantFileSource( this.startupContentDirectory );
    this.startupContentDirectory = this.startupContentDirSource.getRootDirectory();

    // set the tomcat logging directory
    try {
      String base = System.getProperty("catalina.base");
      if (base != null) {
        this.tomcatLogDir = new File( base, "logs").getCanonicalFile();
        if ( !this.tomcatLogDir.exists() )
          log.error( "init(): 'catalina.base' directory not found");
      } else
        log.warn( "init(): 'catalina.base' property not found - probably not a tomcat server");

    } catch (IOException e) {
      log.error( "init(): tomcatLogDir could not be created");
    }

    // Set the content directory and source.
    this.contentDirectory = new File( new File( this.rootDirectory, "../../content"), this.contentPath);
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
        log.error( "init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]" );
        System.out.println( "ERROR - TdsContext.init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]." );
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
      log.error( "init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]" );
      System.out.println( "ERROR - TdsContext.init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]." );
      throw new IllegalStateException( tmpMsg );
    }

    // read in persistent user-defined params from threddsConfig.xml
    File tdsConfigFile = this.contentDirSource.getFile( this.getTdsConfigFileName() );
    String tdsConfigFilename = tdsConfigFile != null ? tdsConfigFile.getPath() : "";
    ThreddsConfig.init( servletContext, tdsConfigFilename, log );

    File logDir = new File( this.contentDirectory, "logs");
    if ( ! logDir.exists())
    {
      if ( ! logDir.mkdirs())
      {
        System.out.println( "ERROR - TdsContext.init(): Couldn't create TDS log directory [" + logDir.getPath() + "]." );
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
          System.out.println( "WARN - TdsContext.init(): Couldn't add content root [" + curContentRoot + "]: " + e.getMessage() );
        }
      }
    }
    this.configSource = new ChainedFileSource( chain );
    this.publicDocSource = this.publicContentDirSource;

    jspRequestDispatcher = servletContext.getNamedDispatcher( "jsp" );
    defaultRequestDispatcher = servletContext.getNamedDispatcher( "default" );

    if ( this.tdsConfigHtml != null )
      this.tdsConfigHtml.init( this);
  }

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
   * Return the version string (<major>.<minor>) for this web application.
   *
   * @return the version string.
   */
  public String getWebappVersion()
  {
    return this.webappVersion;
  }

  /**
   * Return the full version string (<major>.<minor>.<bug>.<build>)
   * for this web application.
   *
   * @return the full version string.
   */
  public String getWebappVersionFull()
  {
    return this.webappVersionFull;
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
