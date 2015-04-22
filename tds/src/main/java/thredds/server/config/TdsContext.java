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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
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
import thredds.featurecollection.CollectionUpdater;
import thredds.servlet.ServletUtil;
import thredds.util.filesource.*;
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
 * TDS context implements ServletContextAware so it gets a ServletContext and performs most initial THREDDS set up:
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

  /////////////////
  // Properties from tds.properties

  @Value("${tds.version}")
  private String webappVersion;

  //@Value("${tds.version.brief}")
  //private String webappVersionBrief;


  @Value("${tds.version.builddate}")
  private String webappVersionBuildDate;

  @Value("${tds.content.root.path}")
  private String contentRootPath;

  @Value("${tds.content.path}")
  private String contentPath;

  @Value("${tds.content.idd.path}")
  private String iddContentPath;

  @Value("${tds.content.motherlode.path}")
  private String motherlodeContentPath;

  @Value("${tds.config.file}")
  private String tdsConfigFileName;

  @Value("${tds.content.startup.path}")
  private String startupContentPath;

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
  private String webappName;
  private String webinfPath;

  private File rootDirectory;
  private File contentDirectory;
  private File publicContentDirectory;
  private File tomcatLogDir;

  private File startupContentDirectory;

  private DescendantFileSource rootDirSource;
  private DescendantFileSource contentDirSource;
  private DescendantFileSource publicContentDirSource;
  private DescendantFileSource startupContentDirSource;

  private FileSource configSource;
  private FileSource publicDocSource;

  private RequestDispatcher defaultRequestDispatcher;
  private RequestDispatcher jspRequestDispatcher;


  private TdsContext() {
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  /*
   * Release tdsContext resources
   * (non-Javadoc)
   * @see org.springframework.beans.factory.DisposableBean#destroy()
   */
  public void destroy() {
    logServerStartup.info("TdsContext: releasing resources");
    logServerStartup.info("TdsContext: Shutting down collection manager");
    CollectionUpdater.INSTANCE.shutdown();
    logServerStartup.info("TdsContext: shutdownLogging()");
    Log4jWebConfigurer.shutdownLogging(servletContext); // probably not needed anymore with log4j-web in classpath
  }

  public void afterPropertiesSet() {
    // ToDo Instead of stdout, use servletContext.log( "...") [NOTE: it writes to localhost.*.log rather than catalina.out].
    if (servletContext == null)
      throw new IllegalArgumentException("ServletContext must not be null.");

    // Set the webapp name.
    this.webappName = servletContext.getServletContextName();

    // Set the context path.
    // Servlet 2.5 allows the following.
    //contextPath = servletContext.getContextPath();
    String tmpContextPath = servletContext.getInitParameter("ContextPath");  // cannot be overridden in the ThreddsConfig file
    if (tmpContextPath == null) tmpContextPath = "thredds";
    contextPath = "/" + tmpContextPath;
    // ToDo LOOK - Get rid of need for setting contextPath in ServletUtil.
    ServletUtil.setContextPath(contextPath);

    // Set the root directory and source.
    String rootPath = servletContext.getRealPath("/");
    if (rootPath == null) {
      String msg = "Webapp [" + this.webappName + "] must run with exploded deployment directory (not from .war).";
      logServerStartup.error("TdsContext.init(): " + msg);
      throw new IllegalStateException(msg);
    }
    this.rootDirectory = new File(rootPath);
    this.rootDirSource = new BasicDescendantFileSource(this.rootDirectory);
    this.rootDirectory = this.rootDirSource.getRootDirectory();
    // ToDo LOOK - Get rid of need for setting rootPath in ServletUtil.
    ServletUtil.setRootPath(this.rootDirSource.getRootDirectoryPath());

    // Set the startup (initial install) content directory and source.
    this.startupContentDirectory = new File(this.rootDirectory, this.startupContentPath);
    this.startupContentDirSource = new BasicDescendantFileSource(this.startupContentDirectory);
    this.startupContentDirectory = this.startupContentDirSource.getRootDirectory();

    this.webinfPath = this.rootDirectory + "/WEB-INF";

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

    // Set the content directory and source.
    File contentRootDir = new File(this.contentRootPath);
    if (!contentRootDir.isAbsolute())
      this.contentDirectory = new File(new File(this.rootDirectory, this.contentRootPath), this.contentPath);
    else {
      if (contentRootDir.isDirectory())
        this.contentDirectory = new File(contentRootDir, this.contentPath);
      else {
        String msg = "Content root directory [" + this.contentRootPath + "] not a directory.";
        logServerStartup.error("TdsContext.init(): " + msg);
        throw new IllegalStateException(msg);
      }
    }

    // If content directory exists, make sure it is a directory.
    if (this.contentDirectory.isDirectory()) {
      this.contentDirSource = new BasicDescendantFileSource(StringUtils.cleanPath(this.contentDirectory.getAbsolutePath()));
      this.contentDirectory = this.contentDirSource.getRootDirectory();
    } else {
      String message = String.format(
              "TdsContext.init(): Content directory is not a directory: %s", this.contentDirectory.getAbsolutePath());
      logServerStartup.error(message);
      throw new IllegalStateException(message);
    }
    ServletUtil.setContentPath(this.contentDirSource.getRootDirectoryPath());

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

    // read in persistent user-defined params from threddsConfig.xml
    File tdsConfigFile = this.contentDirSource.getFile(this.getTdsConfigFileName());
    if (tdsConfigFile == null) {
      tdsConfigFile = new File(this.contentDirSource.getRootDirectory(), this.getTdsConfigFileName());
      String msg = "TDS configuration file doesn't exist: " + tdsConfigFile;
      logServerStartup.error("TdsContext.init(): " + msg);
      throw new IllegalStateException(msg);
    }

    ThreddsConfig.init(tdsConfigFile.getPath());

    this.publicContentDirectory = new File(this.contentDirectory, "public");
    if (!publicContentDirectory.exists()) {
      if (!publicContentDirectory.mkdirs()) {
        String msg = "Couldn't create TDS public directory [" + publicContentDirectory.getPath() + "].";
        logServerStartup.error("TdsContext.init(): " + msg);
        throw new IllegalStateException(msg);
      }
    }
    this.publicContentDirSource = new BasicDescendantFileSource(this.publicContentDirectory);

    /* this.iddContentDirectory = new File(this.rootDirectory, this.iddContentPath);
    this.iddContentPublicDirSource = new BasicDescendantFileSource(this.iddContentDirectory);

    this.motherlodeContentDirectory = new File(this.rootDirectory, this.motherlodeContentPath);
    this.motherlodeContentPublicDirSource = new BasicDescendantFileSource(this.motherlodeContentDirectory);


    for (String curContentRoot : ThreddsConfig.getContentRootList()) {
      if (curContentRoot.equalsIgnoreCase("idd"))
        chain.add(this.iddContentPublicDirSource);
      else if (curContentRoot.equalsIgnoreCase("motherlode"))
        chain.add(this.motherlodeContentPublicDirSource);
      else {
        try {
          chain.add(new BasicDescendantFileSource(StringUtils.cleanPath(curContentRoot)));
        } catch (IllegalArgumentException e) {
          String msg = "Couldn't add content root [" + curContentRoot + "]: " + e.getMessage();
          logServerStartup.warn("TdsContext.init(): " + msg, e);
        }
      }
    }  */

    List<DescendantFileSource> chain = new ArrayList<>();
    DescendantFileSource contentMinusPublicSource =
            new BasicWithExclusionsDescendantFileSource(this.contentDirectory, Collections.singletonList("public"));
    chain.add(contentMinusPublicSource);
    this.configSource = new ChainedFileSource(chain);
    this.publicDocSource = this.publicContentDirSource;

    // ToDo LOOK Find a better way once thredds.catalog2 is used.
    //InvDatasetScan.setContext(contextPath);
    //InvDatasetScan.setCatalogServletName("/catalog");
    InvDatasetFeatureCollection.setContext(contextPath);
    // GridServlet.setContextPath( contextPath ); // Won't need when switch GridServlet to use Swing MVC and TdsContext

    jspRequestDispatcher = servletContext.getNamedDispatcher("jsp");
    defaultRequestDispatcher = servletContext.getNamedDispatcher("default");

    TdsConfigMapper tdsConfigMapper = new TdsConfigMapper();
    tdsConfigMapper.setTdsServerInfo(this.serverInfo);
    tdsConfigMapper.setHtmlConfig(this.htmlConfig);
    tdsConfigMapper.setWmsConfig(this.wmsConfig);
    tdsConfigMapper.setCorsConfig(this.corsConfig);
    tdsConfigMapper.setTdsUpdateConfig(this.tdsUpdateConfig);
    tdsConfigMapper.init(this);
    
    // check and log the latest stable and development version information
    // only if it is OK according to the threddsConfig file.
    if (this.tdsUpdateConfig.isLogVersionInfo()) {
      Map<String, String> latestVersionInfo = getLatestVersionInfo(); 
      for (Map.Entry entry : latestVersionInfo.entrySet()) {
        logServerStartup.info("TdsContext latest " + entry.getKey() + " version = " + entry.getValue());
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
  private Map<String, String> getLatestVersionInfo() {
    int conTimeOut = 1; // http connection timeout in seconds
    int socTimeOut = 1; // http socket timeout in seconds
    Map<String, String> latestVersionInfo = new HashMap<>();

    String versionUrl = "http://www.unidata.ucar.edu/software/thredds/latest.xml";
    HttpClient httpclient = new DefaultHttpClient();
    final HttpParams httpParameters = httpclient.getParams();
    
    HttpConnectionParams.setConnectionTimeout(httpParameters, conTimeOut * 1000);
    HttpConnectionParams.setSoTimeout(httpParameters, socTimeOut * 1000);
    HttpGet request = new HttpGet(versionUrl);
    request.setHeader("User-Agent", "TDS_" + getVersionInfo().replace(" ", ""));

    HttpResponse response;
    try {
      response = httpclient.execute(request);
      HttpEntity entity = response.getEntity();

      try (InputStream content = entity.getContent()) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(content);
        Element docEle = dom.getDocumentElement();
        NodeList versionElements = docEle.getElementsByTagName("version");
        if (versionElements != null && versionElements.getLength() > 0) {
          for (int i = 0; i < versionElements.getLength(); i++) {
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
    sb.append("webappName='").append(webappName).append('\'');
    sb.append("\n  contextPath='").append(contextPath).append('\'');
    sb.append("\n  webappVersion='").append(webappVersion).append('\'');
    sb.append("\n  webappVersionBuildDate='").append(webappVersionBuildDate).append('\'');
    sb.append("\n  contentRootPath='").append(contentRootPath).append('\'');
    sb.append("\n  contentPath='").append(contentPath).append('\'');
    sb.append("\n  iddContentPath='").append(iddContentPath).append('\'');
    sb.append("\n  motherlodeContentPath='").append(motherlodeContentPath).append('\'');
    sb.append("\n  tdsConfigFileName='").append(tdsConfigFileName).append('\'');
    sb.append("\n  startupContentPath='").append(startupContentPath).append('\'');
    sb.append("\n  webinfPath='").append(webinfPath).append('\'');
    sb.append("\n  rootDirectory=").append(rootDirectory);
    sb.append("\n  contentDirectory=").append(contentDirectory);
    sb.append("\n  publicContentDirectory=").append(publicContentDirectory);
    sb.append("\n  tomcatLogDir=").append(tomcatLogDir);
    sb.append("\n  startupContentDirectory=").append(startupContentDirectory);
    //sb.append("\n  iddContentDirectory=").append(iddContentDirectory);
    //sb.append("\n  motherlodeContentDirectory=").append(motherlodeContentDirectory);
    sb.append("\n  rootDirSource=").append(rootDirSource);
    sb.append("\n  contentDirSource=").append(contentDirSource);
    sb.append("\n  publicContentDirSource=").append(publicContentDirSource);
    sb.append("\n  startupContentDirSource=").append(startupContentDirSource);
    //sb.append("\n  iddContentPublicDirSource=").append(iddContentPublicDirSource);
    //sb.append("\n  motherlodeContentPublicDirSource=").append(motherlodeContentPublicDirSource);
    sb.append("\n  configSource=").append(configSource);
    sb.append("\n  publicDocSource=").append(publicDocSource);
    sb.append('}');
    return sb.toString();
  }

  ////////////////////////////////////////////////////
  // public getters

  /**
   * Return the name of the webapp as given by the display-name element in web.xml.
   *
   * @return the name of the webapp as given by the display-name element in web.xml.
   */
  public String getWebappName() {
    return this.webappName;
  }

  /**
   * Return the context path under which this web app is running (e.g., "/thredds").
   *
   * @return the context path.
   */
  public String getContextPath() {
    return contextPath;
  }

  /**
   * Return the context path under which this web app is running (e.g., "/thredds").
   *
   * @return the context path.
   */
  public String getWebinfPath() {
    return webinfPath;
  }

  /**
   * Return the full version string (<major>.<minor>.<bug>.<build>)
   * for this web application.
   *
   * @return the full version string.
   */
  public String getWebappVersion() {
    return this.webappVersion;
  }

  public String getWebappVersionBuildDate() {
    return this.webappVersionBuildDate;
  }

  public String getVersionInfo() {
    Formatter f = new Formatter();
    f.format("%s", getWebappVersion());
    if (getWebappVersionBuildDate() != null) {
      f.format(" - %s", getWebappVersionBuildDate());
    }
    return f.toString();
  }

  /**
   * Return the web apps root directory (i.e., getRealPath( "/")).
   *
   * @return the root directory for the web app.
   */
  public File getRootDirectory() {
    return rootDirectory;
  }

  /**
   * Return the tomcat logging directory
   *
   * @return the tomcat logging directory.
   */
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

  /**
   * Return File for the initial content directory. I.e., the directory
   * that contains default content for the content directory, copied
   * there when TDS is first installed.
   *
   * @return a File to the initial content directory.
   */
  public File getStartupContentDirectory() {
    return startupContentDirectory;
  }

  public FileSource getConfigFileSource() {
    return this.configSource;
  }

  public FileSource getPublicDocFileSource() {
    return this.publicDocSource;
  }

  public RequestDispatcher getDefaultRequestDispatcher() {
    return this.defaultRequestDispatcher;
  }

  public RequestDispatcher getJspRequestDispatcher() {
    return this.jspRequestDispatcher;
  }

  public ServletContext getServletContext() {
    return this.servletContext;
  }

  public String getContentRootPath() {
    return this.contentRootPath;
  }

  public String getContentRootPathAbsolute() {
    File abs = new File(this.contentRootPath);
    return abs.getAbsolutePath();
  }


  public String getTdsConfigFileName() {
    return this.tdsConfigFileName;
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

  public void setWmsConfig(WmsConfig wmsConfig) {
    this.wmsConfig = wmsConfig;
  }

  public CorsConfig getCorsConfig() { return corsConfig; }


  public TdsUpdateConfig getTdsUpdateConfig() { return tdsUpdateConfig; }

  /////////////////////////////////////////////////////

  // used by MockTdsContextLoader
  public void setContentRootPath(String contentRootPath) {
    this.contentRootPath = contentRootPath;
  }

}
