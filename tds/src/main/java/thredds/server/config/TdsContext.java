/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.util.Log4jWebConfigurer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.servlet.ServletUtil;
import thredds.util.filesource.*;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil2;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * TDScontext implements ServletContextAware so it gets a ServletContext and performs most initial THREDDS set up:
 * - checks version
 * - check for latest stable and development release versions
 * - sets the content directory
 * - reads persistent user defined params and runs ThreddsConfig.init
 * - creates, if don't exist, log and public dirs in content directory
 * - Get default and jsp dispatchers from servletContext
 * - Creates and initializes the TdsConfigMapper
 *
 * LOOK would be nice to make Immutable
 * @author edavis
 * @since 4.0
 */
@Component("TdsContext")
public final class TdsContext implements ServletContextAware, InitializingBean, DisposableBean {
  private final Logger logServerStartup = LoggerFactory.getLogger("serverStartup");
  private final Logger logCatalogInit = LoggerFactory.getLogger(TdsContext.class.getName() + ".catalogInit");

  /////////////////
  // Properties from tds.properties

  // The values for these properties all come from tds/src/main/template/thredds/server/tds.properties except for
  // "tds.content.root.path", which must be defined on the command line.

  @Value("${tds.version}")
  private String tdsVersion;

  @Value("${tds.version.builddate}")
  private String tdsVersionBuildDate;

  @Value("${tds.content.root.path}")
  private String contentRootPathProperty; // wants a trailing slash

  @Value("${tds.content.path}")
  private String contentPathProperty;

  @Value("${tds.config.file}")
  private String configFileProperty;

  @Value("${tds.content.startup.path}")
  private String contentStartupPathProperty;

  ////////////////////////////////////
  // set by spring

  @Autowired
  private HtmlConfig htmlConfig;

  @Autowired
  private TdsServerInfo serverInfo;

  @Autowired
  private WmsConfig wmsConfig;

  @Autowired
  private CorsConfig corsConfig;

  @Autowired
  private TdsUpdateConfig tdsUpdateConfig;

  private ServletContext servletContext;

  ///////////////////////////////////////////

  private String contextPath;
  private String webappDisplayName;

  private File rootDirectory;
  private File contentDirectory;
  private File publicContentDirectory;
  private File startupContentDirectory;
  private File tomcatLogDir;

  private FileSource publicContentDirSource;
  private FileSource catalogRootDirSource;  // look for catalog files at this(ese) root(s)

  private RequestDispatcher defaultRequestDispatcher;

  //////////// called by Spring lifecycle management
  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Override
  public void destroy() {
    logServerStartup.info("TdsContext: shutdownLogging()");
    Log4jWebConfigurer.shutdownLogging(servletContext); // probably not needed anymore with log4j-web in classpath
  }

  @Override
  public void afterPropertiesSet() {
    if (servletContext == null)
      throw new IllegalArgumentException("ServletContext must not be null.");

    // Set the webapp name. display-name from web.xml
    this.webappDisplayName = servletContext.getServletContextName();

    // Set the context path, eg "thredds"
    contextPath = servletContext.getContextPath();
    ServletUtil.setContextPath(contextPath);
    InvDatasetFeatureCollection.setContextName(contextPath);

    // Set the root directory and source.
    String rootPath = servletContext.getRealPath("/");
    if (rootPath == null) {
      String msg = "Webapp [" + this.webappDisplayName + "] must run with exploded deployment directory (not from .war).";
      logServerStartup.error("TdsContext.init(): " + msg);
      throw new IllegalStateException(msg);
    }
    this.rootDirectory = new File(rootPath);
    BasicDescendantFileSource rootDirSource = new BasicDescendantFileSource(this.rootDirectory);
    this.rootDirectory = rootDirSource.getRootDirectory();
    ServletUtil.setRootPath(rootDirSource.getRootDirectoryPath());

    // Set the startup (initial install) content directory and source.
    this.startupContentDirectory = new File(this.rootDirectory, this.contentStartupPathProperty);
    DescendantFileSource startupContentDirSource = new BasicDescendantFileSource(this.startupContentDirectory);
    this.startupContentDirectory = startupContentDirSource.getRootDirectory();

    // set the tomcat logging directory
    try {
      String base = System.getProperty("catalina.base");
      if (base != null) {
        this.tomcatLogDir = new File(base, "logs").getCanonicalFile();
        if (!this.tomcatLogDir.exists()) {
          String msg = "'catalina.base' directory not found: " + this.tomcatLogDir;
          logServerStartup.error("TdsContext.init(): " + msg);
        }
      } else {
        String msg = "'catalina.base' property not found - probably not a tomcat server";
        logServerStartup.warn("TdsContext.init(): " + msg);
      }

    } catch (IOException e) {
      String msg = "tomcatLogDir could not be created";
      logServerStartup.error("TdsContext.init(): " + msg);
    }

    ////////////////////////////////
    String contentRootPathKey = "tds.content.root.path";

    // In applicationContext-tdsConfig.xml, we have ignoreUnresolvablePlaceholders set to "true".
    // As a result, when properties aren't defined, they will keep their placeholder String.
    // In this case, that's "${tds.content.root.path}".
    if (this.contentRootPathProperty.equals("${tds.content.root.path}")) {
      String message = String.format("\"%s\" property isn't defined.", contentRootPathKey);
      logServerStartup.error(message);
      throw new IllegalStateException(message);
    }

    contentRootPathProperty = StringUtil2.replace(contentRootPathProperty, "\\", "/");
    if (!contentRootPathProperty.endsWith("/"))
      contentRootPathProperty += "/";

    // Set the content directory and source.
    File contentRootDir = new File(this.contentRootPathProperty);
    if (!contentRootDir.isAbsolute())
      this.contentDirectory = new File(new File(this.rootDirectory, this.contentRootPathProperty), this.contentPathProperty);
    else {
      if (contentRootDir.isDirectory())
        this.contentDirectory = new File(contentRootDir, this.contentPathProperty);
      else {
        String msg = "Content root directory [" + this.contentRootPathProperty + "] not a directory.";
        logServerStartup.error("TdsContext.init(): " + msg);
        throw new IllegalStateException(msg);
      }
    }

    // If content directory exists, make sure it is a directory.
    DescendantFileSource contentDirSource;
    if (this.contentDirectory.isDirectory()) {
      contentDirSource = new BasicDescendantFileSource(StringUtils.cleanPath(this.contentDirectory.getAbsolutePath()));
      this.contentDirectory = contentDirSource.getRootDirectory();
    } else {
      String message = String.format(
              "TdsContext.init(): Content directory is not a directory: %s", this.contentDirectory.getAbsolutePath());
      logServerStartup.error(message);
      throw new IllegalStateException(message);
    }
    ServletUtil.setContentPath(contentDirSource.getRootDirectoryPath());

    // public content
    this.publicContentDirectory = new File(this.contentDirectory, "public");
    if (!publicContentDirectory.exists()) {
      if (!publicContentDirectory.mkdirs()) {
        String msg = "Couldn't create TDS public directory [" + publicContentDirectory.getPath() + "].";
        logServerStartup.error("TdsContext.init(): " + msg);
        throw new IllegalStateException(msg);
      }
    }
    this.publicContentDirSource = new BasicDescendantFileSource(this.publicContentDirectory);

    // places to look for catalogs ??
    List<DescendantFileSource> chain = new ArrayList<>();
    DescendantFileSource contentMinusPublicSource =
            new BasicWithExclusionsDescendantFileSource(this.contentDirectory, Collections.singletonList("public"));
    chain.add(contentMinusPublicSource);
    this.catalogRootDirSource = new ChainedFileSource(chain);

    //jspRequestDispatcher = servletContext.getNamedDispatcher("jsp");
    defaultRequestDispatcher = servletContext.getNamedDispatcher("default");

    //////////////////////////////////// Copy default startup files, if necessary ////////////////////////////////////

    try {
      File catalogFile = new File(contentDirectory, "catalog.xml");
      if (!catalogFile.exists()) {
        File defaultCatalogFile = new File(startupContentDirectory, "catalog.xml");
        logServerStartup.info("TdsContext.init(): Copying default catalog file from {}.", defaultCatalogFile);
        IO.copyFile(defaultCatalogFile, catalogFile);

        File enhancedCatalogFile = new File(contentDirectory, "enhancedCatalog.xml");
        File defaultEnhancedCatalogFile = new File(startupContentDirectory, "enhancedCatalog.xml");
        logServerStartup.info("TdsContext.init(): Copying default enhanced catalog file from {}.", defaultEnhancedCatalogFile);
        IO.copyFile(defaultEnhancedCatalogFile, enhancedCatalogFile);

        File dataDir = new File(new File(contentDirectory, "public"), "testdata");
        File defaultDataDir = new File(new File(startupContentDirectory, "public"), "testdata");
        logServerStartup.info("TdsContext.init(): Copying default testdata directory from {}.", defaultDataDir);
        IO.copyDirTree(defaultDataDir.getCanonicalPath(), dataDir.getCanonicalPath());
      }

      File threddsConfigFile = new File(contentDirectory, "threddsConfig.xml");
      if (!threddsConfigFile.exists()) {
        File defaultThreddsConfigFile = new File(startupContentDirectory, "threddsConfig.xml");
        logServerStartup.info("TdsContext.init(): Copying default THREDDS config file from {}.", defaultThreddsConfigFile);
        IO.copyFile(defaultThreddsConfigFile, threddsConfigFile);
      }

      File wmsConfigXmlFile = new File(contentDirectory, "wmsConfig.xml");
      if (!wmsConfigXmlFile.exists()) {
        File defaultWmsConfigXmlFile = new File(startupContentDirectory, "wmsConfig.xml");
        logServerStartup.info("TdsContext.init(): Copying default WMS config file from {}.", defaultWmsConfigXmlFile);
        IO.copyFile(defaultWmsConfigXmlFile, wmsConfigXmlFile);

        File wmsConfigDtdFile = new File(contentDirectory, "wmsConfig.dtd");
        File defaultWmsConfigDtdFile = new File(startupContentDirectory, "wmsConfig.dtd");
        logServerStartup.info("TdsContext.init(): Copying default WMS config DTD from {}.", defaultWmsConfigDtdFile);
        IO.copyFile(defaultWmsConfigDtdFile, wmsConfigDtdFile);
      }
    } catch (IOException e) {
      String message = String.format("Could not copy default startup files to %s.", contentDirectory);
      logServerStartup.error("TdsContext.init(): " + message);
      throw new IllegalStateException(message, e);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // logging

    File logDir = new File(this.contentDirectory, "logs");
    if (!logDir.exists()) {
      if (!logDir.mkdirs()) {
        String msg = "Couldn't create TDS log directory [" + logDir.getPath() + "].";
        logServerStartup.error("TdsContext.init(): " + msg);
        throw new IllegalStateException(msg);
      }
    }
    String loggingDirectory = StringUtil2.substitute(logDir.getPath(), "\\", "/");
    System.setProperty("tds.log.dir", loggingDirectory); // variable substitution

    // LOOK Remove log4j init JC 6/13/2012
    // which is used in log4j.xml file loaded here.
    // LOOK Remove Log4jWebConfigurer,initLogging - depends on log4g v1, we are using v2 JC 9/2/2013
    // Log4jWebConfigurer.initLogging( servletContext );
    logServerStartup.info("TdsContext version= " + getVersionInfo());
    logServerStartup.info("TdsContext intialized logging in " + logDir.getPath());


    //////////////////////////////////////////////////////////////////////////////////////
    // read in persistent user-defined params from threddsConfig.xml

    File tdsConfigFile = contentDirSource.getFile(this.getConfigFileProperty());
    if (tdsConfigFile == null) {
      tdsConfigFile = new File(contentDirSource.getRootDirectory(), this.getConfigFileProperty());
      String msg = "TDS configuration file doesn't exist: " + tdsConfigFile;
      logServerStartup.error("TdsContext.init(): " + msg);
      throw new IllegalStateException(msg);
    }

    ThreddsConfig.init(tdsConfigFile.getPath());

    // initialize/populate all of the various config objects
    TdsConfigMapper tdsConfigMapper = new TdsConfigMapper();
    tdsConfigMapper.setTdsServerInfo(this.serverInfo);
    tdsConfigMapper.setHtmlConfig(this.htmlConfig);
    tdsConfigMapper.setWmsConfig(this.wmsConfig);
    tdsConfigMapper.setCorsConfig(this.corsConfig);
    tdsConfigMapper.setTdsUpdateConfig(this.tdsUpdateConfig);
    tdsConfigMapper.init(this);

    // log current server version in catalogInit, where it is
    //  most likely to be seen by the user
    String message = "You are currently running TDS version " + this.getVersionInfo();
    logCatalogInit.info(message);
    // check and log the latest stable and development version information
    //  only if it is OK according to the threddsConfig file.
    if (this.tdsUpdateConfig.isLogVersionInfo()) {
      Map<String, String> latestVersionInfo = getLatestVersionInfo();
      if (!latestVersionInfo.isEmpty()) {
        logCatalogInit.info("Latest Available TDS Version Info:");
        for (Map.Entry entry : latestVersionInfo.entrySet()) {
          message = "latest " + entry.getKey() + " version = " + entry.getValue();
          logServerStartup.info("TdsContext: " + message);
          logCatalogInit.info("    " + message);
        }
        logCatalogInit.info("");
      }
    }
  }


  /**
   * Retrieve the latest stable and development versions
   * available from Unidata. Needs to connect to
   * http://www.unidata.ucar.edu in order to get the
   * latest version numbers. The propose is to easily let users
   * know if the version of TDS they are running is out of
   * date, as this information is recorded in the
   * serverStartup.log file.
   *
   * @return A hashmap containing versionTypes as key (i.e.
   * "stable", "development") and their corresponding
   * version numbers (i.e. 4.5.2)
   */
  public Map<String, String> getLatestVersionInfo() {
    int socTimeout = 1; // http socket timeout in seconds
    int connectionTimeout = 3; // http connection timeout in seconds
    Map<String, String> latestVersionInfo = new HashMap<>();

    String versionUrl = "http://www.unidata.ucar.edu/software/thredds/latest.xml";
    try {
      try (HTTPMethod method = HTTPFactory.Get(versionUrl)) {
        HTTPSession httpClient = method.getSession();
        httpClient.setSoTimeout(socTimeout * 1000);
        httpClient.setConnectionTimeout(connectionTimeout * 1000);
        httpClient.setUserAgent("TDS_" + getVersionInfo().replace(" ", ""));
        method.execute();
        InputStream responseIs = method.getResponseBodyAsStream();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(responseIs);
        Element docEle = dom.getDocumentElement();
        NodeList versionElements = docEle.getElementsByTagName("version");
        if(versionElements != null && versionElements.getLength() > 0) {
          for(int i = 0;i < versionElements.getLength();i++) {
            //get the version element
            Element versionElement = (Element) versionElements.item(i);
            String verType = versionElement.getAttribute("name");
            String verStr = versionElement.getAttribute("value");
            latestVersionInfo.put(verType, verStr);
          }
        }
      }
    } catch (IOException e) {
      logServerStartup.warn("TdsContext - Could not get latest version information from Unidata.");
    } catch (ParserConfigurationException e) {
      logServerStartup.error("TdsContext - Error configuring latest version xml parser" + e.getMessage() + ".");
    } catch (SAXException e) {
      logServerStartup.error("TdsContext - Could not parse latest version information.");
    }
    return latestVersionInfo;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TdsContext{");
    sb.append("\n  contextPath='").append(contextPath).append('\'');
    sb.append("\n  webappName='").append(webappDisplayName).append('\'');
    sb.append("\n  webappVersion='").append(tdsVersion).append('\'');
    sb.append("\n  webappVersionBuildDate='").append(tdsVersionBuildDate).append('\'');
    sb.append("\n");
    sb.append("\n  contentPath= '").append(contentPathProperty).append('\'');
    sb.append("\n  contentRootPath= '").append(contentRootPathProperty).append('\'');
    sb.append("\n  contentStartupPath= '").append(contentStartupPathProperty).append('\'');
    sb.append("\n  configFile= '").append(configFileProperty).append('\'');
    sb.append("\n");
    sb.append("\n  rootDirectory=   ").append(rootDirectory);
    sb.append("\n  contentDirectory=").append(contentDirectory);
    sb.append("\n  publicContentDir=").append(publicContentDirectory);
    sb.append("\n  startupContentDirectory=").append(startupContentDirectory);
    sb.append("\n  tomcatLogDir=").append(tomcatLogDir);
    sb.append("\n");
    sb.append("\n  publicContentDirSource= ").append(publicContentDirSource);
    sb.append("\n  catalogRootDirSource=").append(catalogRootDirSource);
    sb.append('}');
    return sb.toString();
  }

  ////////////////////////////////////////////////////
  // public getters

  /**
   * Return the context path under which this web app is running (e.g., "/thredds").
   *
   * @return the context path.
   */
  public String getContextPath() {
    return contextPath;
  }

  /**
   * Return the full version string (<major>.<minor>.<bug>.<build>)*
   * @return the full version string.
   */
  public String getWebappVersion() {
    return this.tdsVersion;
  }

  public String getTdsVersionBuildDate() {
    return this.tdsVersionBuildDate;
  }

  public String getVersionInfo() {
    Formatter f = new Formatter();
    f.format("%s", getWebappVersion());
    if (getTdsVersionBuildDate() != null) {
      f.format(" - %s", getTdsVersionBuildDate());
    }
    return f.toString();
  }

  /**
   * @return the name of the webapp as given by the display-name element in web.xml.
   */
  public String getWebappDisplayName() {
    return this.webappDisplayName;
  }

  /**
   * Return the web apps root directory (i.e., getRealPath( "/")).
   * Typically {tomcat}/webapps/thredds
   * @return the root directory for the web app.
   */
  public File getRootDirectory() {
    return rootDirectory;
  }

  public File getTomcatLogDirectory() {
    return tomcatLogDir;
  }

  /**
   * Return File for content directory (exists() may be false).
   *
   * @return a File to the content directory.
   */
  public File getContentDirectory() {
    return contentDirectory;
  }

  public FileSource getCatalogRootDirSource() {
    return this.catalogRootDirSource;
  }

  // {tomcat}/content/thredds/public
  public FileSource getPublicContentDirSource() {
    return this.publicContentDirSource;
  }

  public RequestDispatcher getDefaultRequestDispatcher() {
    return this.defaultRequestDispatcher;
  }

  public String getContentRootPathProperty() {
    return this.contentRootPathProperty;
  }


  public String getConfigFileProperty() {
    return this.configFileProperty;
  }

  public TdsServerInfo getServerInfo() {
    return serverInfo;
  }

  public HtmlConfig getHtmlConfig() {
    return this.htmlConfig;
  }

  public WmsConfig getWmsConfig() {
    return wmsConfig;
  }


  /////////////////////////////////////////////////////

  // used by MockTdsContextLoader
  public void setContentRootPathProperty(String contentRootPathProperty) {
    this.contentRootPathProperty = contentRootPathProperty;
  }

}
