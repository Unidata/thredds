// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.servlet;

import thredds.catalog.*;
import thredds.util.PathMatcher;
import thredds.crawlabledataset.CrawlableDataset;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;

/**
 * This manages the catalogs served by a TDS, as well as the "data roots" in TDS config catalogs.
 * Data roots are set with datasetRoot or datasetScan elements, and allow mapping between URLs and file paths.
 *
 * Uses the singleton design pattern.
 *
 * @author caron
 *
 */
public class DataRootHandler {
  static private DataRootHandler singleton = null;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataRootHandler.class);

  /**
   * Initialize the CatalogHandler singleton instance.
   * @param contentPath should be absolute path under which all catalogs lay, typically ${tomcat_home}/content/thredds/
   */
  static public void init( String contentPath, String servletContextPath)  {
    singleton = new DataRootHandler( contentPath, servletContextPath);
  }

  /**
   * Get the singleton.
   */
  static public DataRootHandler getInstance() {
    return singleton;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private InvCatalogFactory factory;
  private StringBuffer catalogErrorLog = new StringBuffer(); // used during initialization

  private String contentPath;
  private String servletContextPath;
  private ArrayList catalogRootList = new ArrayList(); // List of root catalog filenames (String)
  private ArrayList catalogStaticList = new ArrayList(); // List of static catalog filenames (String)

  private HashMap staticHash = new HashMap(); // Hash of static catalog filenames (String)
  private HashMap catHasScanHash = new HashMap(); // Hash of InvCatalogImpl that have scans and must be processed, key is path name

  private PathMatcher pathMatcher = new PathMatcher(); // collection of DataRoot objects

  // private HashMap dataRootsHash = new HashMap(); // Hash of DataRoot objects, key is path name
  // private ArrayList dataRoots = new ArrayList(); // List of DataRoot objects

  private DataRootHandler( String contentPath, String servletContextPath)
  {
    this.contentPath = contentPath;
    this.servletContextPath = servletContextPath;
    this.factory = InvCatalogFactory.getDefaultFactory( true); // always validate the config catalogs
  }

  /**
   * Reread all the catalogs and reinit the data roots.
   * Call this when the catalogs have changed, so you dont have to restart.
   * Also available under the TDS debug menu.
   * @throws IOException
   */
  public synchronized void reinit() throws IOException {
    staticHash = new HashMap();
    catHasScanHash = new HashMap();
    pathMatcher = new PathMatcher();
    catalogStaticList = new ArrayList();
    catalogErrorLog = new StringBuffer();

    for (int i = 0; i < catalogRootList.size(); i++) {
      String path = (String) catalogRootList.get(i);
      initCatalog(path, true);
    }
    writeCatalogErrorLog();
  }

  /**
   * Read the named catalog and extract the data roots from it.
   * Recurse into nested catalog refs that are reletive to contentPath.
   *
   * @param path file path of catalog, reletive to contentPath. catalog fullpath = contentPath + path.
   * @throws IOException
   */
  public void initCatalog(String path) throws IOException {
    catalogRootList.add( path);
    initCatalog(path, true);
    writeCatalogErrorLog();
  }

  private void initCatalog(String path, boolean recurse) throws IOException {
    String catalogFullPath = contentPath + path;

    // make sure we dont already have it
    if (staticHash.containsKey(path) || catHasScanHash.containsKey( path)) {
      log.error("CatalogHandler.initCatalog has already seen catalog=" + catalogFullPath+" possible loop");
      return;
    }

    // get the directory path, reletive to the contentPath
    int pos = path.lastIndexOf( "/" );
    String dirPath = ( pos > 0 ) ? path.substring( 0, pos+1 ) : "";

    URI uri;
    try {
      uri = new URI("file:" + StringUtil.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    } catch (URISyntaxException e) {
      log.error("CatalogHandler.initCatalog has URISyntaxException=" + e.getMessage());
      throw new IOException( e.getMessage());
    }

    // read the catalog
    log.debug("CatalogHandler initCatalog=" + catalogFullPath+" path="+path);
    InvCatalogImpl cat = null;
    FileInputStream ios = null;
    try {
      ios = new FileInputStream(catalogFullPath);
      cat = factory.readXML(ios, uri);
      catalogErrorLog.append( "\nCatalog " ).append( catalogFullPath ).append( "\n" );
      if (!cat.check(catalogErrorLog)) {
        log.error("CatalogHandler invalid catalog=" + catalogFullPath+"; "+catalogErrorLog.toString());
        // return;
      }

    } catch (IOException ioe) {
      log.error("IOException on catalog=" + catalogFullPath + " " + ioe.getMessage());
      return;

    } finally {
      if (ios != null)
        try {
          ios.close();
        } catch (IOException e) {
          log.error("CatalogHandler error closing" + catalogFullPath);
        }
    }

    // look for datasetRoots
    Iterator services = cat.getServices().iterator();
    while (services.hasNext()) {
      InvService s = (InvService) services.next();
      Iterator roots = s.getDatasetRoots().iterator();
      while (roots.hasNext()) {
        InvProperty root = (InvProperty) roots.next();
        addRoots(dirPath, this.servletContextPath, s, root);
      }
    }

    // look for datasetScans
    boolean hasScans = findSpecialDatasets(dirPath, cat.getDatasets());

    // add catalog to static or hasScan hash tables
    if (!hasScans) {
      staticHash.put( path, null); // dont cache it, since we will serve as raw XML
      log.debug("  add static catalog=" + path);
    } else {
      catHasScanHash.put( path, cat); // it has datasetScan elements and must be converted.
      log.debug("  add hasScan catalog=" + path);
      // LOOK could just convert here
    }
    catalogStaticList.add(path);

    if (recurse)
      check4Catrefs( dirPath, cat.getDatasets());
  }

  // look for any data roots anywhere in this catlaog, but dont follow catRefs
  // return true if has InvDatasetScan elements
  private boolean findSpecialDatasets(String dirPath, List dsList) {
    boolean hasScans = false;
    Iterator iter = dsList.iterator();
    while (iter.hasNext()) {
      InvDatasetImpl invDataset = (InvDatasetImpl) iter.next();

      if (invDataset instanceof InvDatasetScan) {
        InvDatasetScan ds = (InvDatasetScan) invDataset;
        InvService service = ds.getServiceDefault();
        if (service == null) {
          log.error("InvDatasetScan "+ ds.getFullName()+" has no default Service - skipping");
          continue;
        }
        String serviceBase = service.getBase();

        // always have to add the "reletive path" because the catrefs use it
        boolean ok = addRoot( dirPath + ds.getPath(), ds);
        hasScans = hasScans || ok;

        // the non-blank and Compound services need extra attention
        if (service.getServiceType() != ServiceType.COMPOUND) {

          if (serviceBase.length() != 0) { // not blank
            if ( servletContextPath != null )
            {
              if ( serviceBase.startsWith( servletContextPath + "/" ) )
                serviceBase = serviceBase.substring( servletContextPath.length() + 1 );
            }
            if (!serviceBase.equals(dirPath)) { // dont add again if it matches the reletive path
              ok = addRoot( serviceBase + ds.getPath(), ds);
              hasScans = hasScans || ok;
            }
          }

        } else { // compound type

          java.util.List serviceList = service.getServices();
          for (int i = 0; i < serviceList.size(); i++) {
            InvService nestedService = (InvService) serviceList.get(i);
            String nestedServiceBase = nestedService.getBase();
            if ( servletContextPath != null )
            {
              if (nestedServiceBase.startsWith( servletContextPath + "/" ))
                nestedServiceBase = nestedServiceBase.substring( servletContextPath.length() + 1 );
            }
            if (!nestedServiceBase.equals(dirPath)) { // dont add again if it matches the reletive path
              ok = addRoot( nestedServiceBase + ds.getPath(), ds);
              hasScans = hasScans || ok;
            }
          }
        } // compound
      } // dataset scan

      if (invDataset.getNcmlElement() != null) {
//        DatasetHandler.putNcmlDataset( invDataset, this.servletContextPath);
      }

      if (!(invDataset instanceof InvCatalogRef)) {
        // recurse
        boolean nestedScan = findSpecialDatasets(dirPath, invDataset.getDatasets());
        hasScans = hasScans || nestedScan;
      }
    }

    return hasScans;
  }

  private boolean addRoot(String path, InvDatasetScan dscan) {
    // check for duplicates
    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      log.error(" already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">");
      return false;
    }

    // rearrange scanDir if it starts with content
    if (dscan.getScanDir().startsWith("content/"))
      dscan.setScanDir( contentPath +  dscan.getScanDir().substring(8));

    // add it
    droot = new DataRoot( path, dscan);
    pathMatcher.put(  path, droot);

    log.debug(" added rootPath=<" + path + ">  for directory= <" + dscan.getScanDir() + ">");
    return true;
  }

  private void addRoots(String dirPath, String contextPath, InvService service, InvProperty p) {
    String serviceBase = service.getBase();
    String path = p.getName();
    String dirLocation = p.getValue();

    if (service.getServiceType() != ServiceType.COMPOUND) {

      if (serviceBase.length() != 0) { // not blank
        if ( contextPath != null )
        {
          if ( serviceBase.startsWith(contextPath + "/"))
            serviceBase = serviceBase.substring( contextPath.length() + 1);
        }
        addRoot(serviceBase + path, dirLocation);

      } else {
        addRoot(dirPath + path, dirLocation);
      }

    } else { // compound type

      java.util.List serviceList = service.getServices();
      for (int i = 0; i < serviceList.size(); i++) {
        InvService nestedService = (InvService) serviceList.get(i);
        String nestedServiceBase = nestedService.getBase();
        if ( contextPath != null )
        {
          if ( nestedServiceBase.startsWith( contextPath + "/" ) )
            nestedServiceBase = nestedServiceBase.substring( contextPath.length() + 1 );
        }
        addRoot(nestedServiceBase + path, dirLocation);
      }
    }
  }

  private boolean addRoot(String path, String dirLocation) {
    // check for duplicates
    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      log.error(" already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">");
      return false;
    }

    // rearrange dirLocation if it starts with content
    if (dirLocation.startsWith("content/"))
      dirLocation = contentPath +  dirLocation.substring(8);

    // add it
    droot = new DataRoot( path, dirLocation);
    pathMatcher.put(  path, droot);

    log.debug(" added rootPath=<" + path + ">  for directory= <" + dirLocation + ">");
    return true;
  }

  private void check4Catrefs( String dirPath, List datasets) throws IOException {
    Iterator iter = datasets.iterator();
    while (iter.hasNext()) {
      InvDatasetImpl invDataset = (InvDatasetImpl) iter.next();

      if ((invDataset instanceof InvCatalogRef) && !(invDataset instanceof InvDatasetScan)) {
        InvCatalogRef catref = (InvCatalogRef) invDataset;
        String href = catref.getXlinkHref();
        if ( !href.startsWith("http:")) // must be a reletive catref
          initCatalog( dirPath + href, true); // go check it out

      } else if (!(invDataset instanceof InvDatasetScan)) {
        // recurse through nested datasets
        check4Catrefs( dirPath, invDataset.getDatasets());
      }
    }
  }

  public class DataRootMatch {
    String rootPath;     // this is the matching part of the URL
    String dirLocation;  // this is the directory that should be substituted for the rootPath
    String remaining;   // this is the part of the URL that didnt match
  }

  private class DataRoot {
    String path;         // match this path
    String dirLocation; // to this directory
    InvDatasetScan scan; // the InvDatasetScan that created this (may be null)

    DataRoot(String path, InvDatasetScan scan) {
      this.path = path;
      this.scan = scan;
      this.dirLocation = scan.getScanDir();
    }

    DataRoot(String path, String dirLocation) {
      this.path = path;
      this.dirLocation = dirLocation;
    }

    void setPath( String path) {
      if (path.startsWith("/"))
        this.path = path.substring(1);
      else
        this.path = path;
    }

    void setDirLocation( String dirLocation) {
      if (dirLocation.startsWith("/"))
        this.dirLocation = dirLocation.substring(1);
      else
        this.dirLocation = dirLocation;
    }

    // used by PathMatcher
    public String toString() { return path; }

    /** Instances which have same path are equal. */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DataRoot root = (DataRoot) o;
      return path.equals(root.path);
    }
    public int hashCode() { return path.hashCode(); }
  }

  private void writeCatalogErrorLog() {
    try {
      File fileOut = new File(contentPath+ "logs/catalogErrorLog.txt");
      FileOutputStream fos = new FileOutputStream( fileOut);
      thredds.util.IO.writeContents(catalogErrorLog.toString(), fos);
      fos.close();
    } catch (IOException ioe) {
      log.error("CatalogHandler.writeCatalogErrorLog error ", ioe);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Request processing

  /* public boolean serveCatalog(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    if (processReqForStaticCatalog(servlet, req, res))
      return true;

    if (processReqForDynamicCatalog(servlet, req, res))
      return true;

    return false;
  } */

  public boolean isRequestForConfigCatalog( String path )
  {
    String catPath = path;
    if ( catPath.endsWith( ".html" ) )
    {
      // Change ".html" to ".xml"
      int len = catPath.length();
      catPath = catPath.substring( 0, len - 4 ) + "xml";
    }
    else if ( ! catPath.endsWith( ".xml" ) )
    {
      // Not a catalog request.
      return false;
    }

    if ( staticHash.containsKey( catPath ) ) return true;
    if ( catHasScanHash.get( catPath ) != null ) return true;
    return false;
  }

  public boolean isRequestForGeneratedCatalog( String path )
  {
    if ( path.endsWith( "/")
         || path.endsWith( "/catalog.html")
         || path.endsWith( "/catalog.xml") )
    {
      String dsPath = path;

      // strip off the filename
      int pos = dsPath.lastIndexOf( "/" );
      if ( pos >= 0 )
        dsPath = dsPath.substring( 0, pos );

      // Check that path is allowed.
      CrawlableDataset crDs = findRequestedDataset( dsPath );
      if ( crDs == null )
        return false;
      if ( crDs.isCollection() )
        return true;
    }
    return false;
  }

  /**
   * Return true if the given path matches a dataRoot, otherwise return false.
   * A successful match means that the request is either for a dynamic catalog
   * or a dataset.
   *
   * @param path the request path, ie req.getServletPath() + req.getPathInfo()
   * @return true if the given path matches a dataRoot, otherwise false.
   */
  public boolean hasDataRootMatch( String path )
  {
    if ( path.startsWith( "/" ) )
      path = path.substring( 1 );

    DataRoot dataRoot = matchPath2( path );
    if ( dataRoot == null )
    {
      log.debug( "hasDataRootMatch(): no InvDatasetScan for " + path );
      return false;
    }
    return true;
  }

  /**
   * Return the CrawlableDataset to which the given path maps, null if the
   * dataset does not exist or the matching InvDatasetScan filters out the
   * requested CrawlableDataset.
   * <p/>
   * Use this method to check that a data request is requesting an allowed dataset.
   *
   * @param path the request path.
   * @return the requested CrawlableDataset or null if the requested dataset is not allowed by the matching InvDatasetScan.
   * @throws IllegalStateException if the request is not for a descendent of (or the same as) the matching DatasetRoot collection location.
   */
  public CrawlableDataset findRequestedDataset( String path )
  {
    if ( path.length() > 0 )
    {
      if ( path.startsWith( "/" ) )
        path = path.substring( 1 );
    }

    DataRoot reqDataRoot = matchPath2( path );
    if ( reqDataRoot == null )
      return null;
    if ( reqDataRoot.scan == null )
      return null;

    //noinspection UnnecessaryLocalVariable
    CrawlableDataset crDs = reqDataRoot.scan.requestCrawlableDataset( path );
//    if ( crDs == null )
//    {
//      reqDataRoot.scan.isProxyDataset();
//    }
    
    return crDs;
  }

  public void handleRequestForDataset( String path, DataServiceProvider dsp, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    // Can the DataServiceProvider handle the data request given by the path?
    DataServiceProvider.DatasetRequest dsReq = dsp.getRecognizedDatasetRequest( path, req );
    String crDsPath;
    boolean dspCanHandle = false;
    if ( dsReq != null )
    {
      String dsPath = dsReq.getDatasetPath();
      if ( dsPath != null)
      {
        // Use the path returned by the DataServiceProvider.
        crDsPath = dsPath;
        dspCanHandle = true;
      }
      else
      {
        // DataServiceProvider recognized request path but returned a null dataset path. Hmm?
        log.warn( "handleRequestForDataset(): DataServiceProvider recognized request path <" + path + "> but returned a null dataset path, using request path.");
        // Use the incoming request path.
        crDsPath = path;
      }
    }
    else
    {
      // DataServiceProvider  did not recognized request path.
      // Use the incoming request path.
      crDsPath = path;
    }

    // Find the CrawlableDataset represented by the request path.
    CrawlableDataset crDs = this.findRequestedDataset( crDsPath );
    if ( crDs == null )
    {
      // Request is not for a known (or allowed) dataset.
      res.sendError( HttpServletResponse.SC_NOT_FOUND ); // 404
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, -1 );
      return;
    }

    if ( dspCanHandle )
    {
      // Request recognized by DataServiceProvider, handle dataset request.
      dsp.handleRequestForDataset( dsReq, crDs, req, res );
      return;
    }
    else
    {
      // Request not recognized by DataServiceProvider.
      if ( crDs.isCollection() )
      {
        // Handle request for a collection dataset.
        dsp.handleUnrecognizedRequestForCollection( crDs, req, res );
        return;
      }

      // Handle request for an atomic dataset.
      dsp.handleUnrecognizedRequest( crDs, req, res );
      return;
    }
  }

  /**
   * If a catalog exists and is allowed (not filtered out) for the given path, return
   * the catalog as an InvCatalog. Otherwise, return null.
   *
   * The validity of the returned catalog is not guaranteed. Use InvCatalog.check() to
   * check that the catalog is valid.
   *
   * @param path the path for the requested catalog.
   * @param baseURI the base URI for the catalog, used to resolve relative URLs.
   * @return the requested InvCatalog, or null if catalog does not exist or is not allowed.
   */
  public InvCatalog getCatalog( String path, URI baseURI )
  {
    // Check for static catalog.
    InvCatalog catalog = getStaticCatalog( path, baseURI );

    // If not static, check for dynamic catalog.
    if ( catalog == null )
      catalog = getDynamicCatalog( path, baseURI );

    return catalog;
  }

  /**
   * If a static catalog exists for the path, return the catalog. Otherwise, return null.
   *
   * @param path the path for the requested catalog.
   * @param baseURI the base URI for the catalog, used to resolve relative URLs.
   * @return the requested InvCatalog, or null if catalog does not exist or is not allowed.
   */
  private InvCatalog getStaticCatalog( String path, URI baseURI )
  {
    if ( path == null) return null;
    String workPath = path;
    InvCatalogImpl catalog;
    if ( workPath.startsWith( "/" ) )
      workPath = workPath.substring( 1 );

    // Check for static catalog (WITHOUT datasetScans).
    if ( staticHash.containsKey( workPath ) )
    {
      String filename = ServletUtil.formFilename( contentPath, workPath );
      if ( filename == null )
        return null;

      File file = new File( filename );
      if ( ! file.exists() )
        return null;
      if ( file.isDirectory() )
        return null;

      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      FileInputStream is;
      try
      {
        is = new FileInputStream( file );
      }
      catch ( FileNotFoundException e )
      {
        log.warn( "getStaticCatalog(): File not found: " + e.getMessage());
        return null;
      }

      catalog = fac.readXML( is, baseURI );
      return catalog;
    }

    // Check for static catalog (WITH datasetScans).
    catalog = (InvCatalogImpl) catHasScanHash.get( workPath );
    if ( catalog != null )
      catalog.setBaseURI( baseURI );

    return catalog;
  }

  /**
   * If a dynamic catalog exists (and is allowed) for the path, return the catalog.
   * Otherwise, return null.
   *
   * @param path the path for the requested catalog.
   * @param baseURI the base URI for the catalog, used to resolve relative URLs.
   * @return the requested InvCatalog, or null if catalog does not exist or is not allowed.
   */
  private InvCatalog getDynamicCatalog( String path, URI baseURI )
  {
    InvCatalogImpl catalog = makeDynamicCatalog( path, baseURI );
    if ( catalog != null )
      catalog.setBaseURI( baseURI );

    return catalog;
  }

  /**
   * This looks to see if this is a request for a catalog, either as xml or as html.
   * If so, it processes the request completely.
   *
   * @param servlet calling servlet
   * @param req the request
   * @param res the response
   * @param path use this path to match for catalogs. this allows callers to munge it, eg create defaults etc.
   * @return true if request was handled
   * @throws IOException
   * @throws ServletException
   */
  public boolean processReqForCatalog(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res, String path)
          throws IOException, ServletException
  {
    String catPath = path;
    boolean isHtmlReq = false;
    if ( catPath.endsWith( "/"))
    {
      isHtmlReq = true;
      catPath = catPath + "catalog.xml";
    }
    else if ( catPath.endsWith( ".html"))
    {
      isHtmlReq = true;
      // Change ".html" to ".xml"
      int len = catPath.length();
      catPath = catPath.substring( 0, len - 4 ) + "xml";
    }
    else if ( ! catPath.endsWith( ".xml"))
    {
      // Not a catalog request.
      return false;
    }

    URI baseURI;
    String reqBase = req.getRequestURL().toString();
    try
    {
      baseURI = new URI( reqBase );
    }
    catch ( URISyntaxException e )
    {
      log.error( "processReqForCatalog(): Request base <" + reqBase + "> not a URI: " + e.getMessage() );
      throw new ServletException( "Request base <" + reqBase + "> not a URI.", e);
    }
    InvCatalogImpl catalog = (InvCatalogImpl) getCatalog( catPath, baseURI );
    if ( catalog == null )
      return false;

    // @todo Check that catalog is valid [ Use catalog.check() ].

    // Deal with catalogServices
    String query = req.getQueryString();
    if ( query != null )
    {
      CatalogServicesServlet.handleCatalogServiceRequest( catalog, baseURI, isHtmlReq, req, res );
      return true;
    }

    // Return HTML view of catalog.
    if ( isHtmlReq )
    {
      HtmlWriter2.getInstance().writeCatalog( res, catalog, true );
      return true;
    }

    // Return catalog as XML response.
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
    String result = catFactory.writeXML( catalog );
    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, result.length() );

    res.setContentLength( result.length() );
    res.setContentType( "text/xml" );
    res.getOutputStream().write( result.getBytes() );
    return true;
  }

  /**
   * See if this request is for a static catalog. If so, send it.
   * Converts any embedded datasetScan elemets.
   *
   * @param servlet requesting servlet
   * @param req request
   * @param res response
   * @return true if static catalog was found and served
   * @throws IOException
   */
  private boolean processReqForStaticCatalog(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    String path = ServletUtil.getRequestPath( req);
    boolean isHtml = path.endsWith(".html");
    if (isHtml) {
      int pos = path.lastIndexOf(".");
      path = path.substring(0,pos) +".xml";
    }

    if (path.startsWith("/"))
      path = path.substring(1);

    // static
    if (staticHash.containsKey(path)) {
      if (isHtml) {
        ServletUtil.forwardToCatalogServices(req, res);
        return true;
      }

      // yes, its non-dynamic
      ServletUtil.returnFile(servlet, contentPath, path, req, res, null);
      return true;
    }

    // must be converted
    InvCatalogImpl cat = (InvCatalogImpl) catHasScanHash.get(path);
    if (cat != null) {
      if (isHtml) {
        ServletUtil.forwardToCatalogServices(req, res);
        return true;
      }

      // LOOK : generate catalog each time; use object pool or cache ?
      InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false);
      String result = catFactory.writeXML(cat);
      res.setContentLength(result.length());
      res.setContentType("text/xml");

      res.getOutputStream().write( result.getBytes());
      ServletUtil.logServerAccess( HttpServletResponse.SC_OK, result.length() );
      return true;
    }

    return false;
  }

  /**
   * Check if request for dynamic catalog, generated from a DatasetScan.
   *
   * @param servlet requesting servlet
   * @param req request
   * @param res response
   * @return true if request was processed.
   * @throws IOException
   */
  private boolean processReqForDynamicCatalog(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res)
          throws IOException
  {
    URI baseURI;
    String reqBase = ServletUtil.getRequestBase( req );
    try
    {
      baseURI = new URI( reqBase );
    }
    catch ( URISyntaxException e )
    {
      log.error( "processReqForDynamicCatalog(): Request base <" + reqBase + "> not a URI: " + e.getMessage());
      throw new IllegalStateException( "Request base <" + reqBase + "> not a URI: " + e.getMessage());
    }
    InvCatalogImpl cat = makeDynamicCatalog( ServletUtil.getRequestPath( req ),
                                             baseURI );
                                            // @todo Why not use req.getRequestURL()??
    if (cat == null) {
      return false;
    }

    // write XML
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false);
    String result = catFactory.writeXML(cat);

    res.setContentLength(result.length());
    res.setContentType("text/xml");
    res.getOutputStream().write( result.getBytes());
    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, result.length() );
    return true;
  }

  private InvCatalogImpl makeDynamicCatalog( String path, URI baseURI ) {
    String workPath = path;
    if (workPath.startsWith("/"))
      workPath = workPath.substring(1);

    // strip off the filename
    int pos = workPath.lastIndexOf("/");
    if (pos  >= 0)
      workPath = workPath.substring(0, pos);

    // Check that path is allowed.
    if ( findRequestedDataset( workPath ) == null )
      return null;

    // now look through the InvDatasetScans and get a maximal match
    DataRoot dataRoot = matchPath2( workPath );
    if (dataRoot == null) {
      log.warn("makeDynamicCatalog(): No InvDatasetScan for ="+ workPath +" request path= "+path); // LOOK why should there be a messaage ?
      return null;
    }
    // its gotta be a DatasetScan, not a DatasetRoot
    if (dataRoot.scan == null) {
      log.warn("makeDynamicCatalog(): No InvDatasetScan for ="+ workPath +" request path= "+path);
      return null;
    }

    InvDatasetScan dscan = dataRoot.scan;
    log.debug( "Calling makeCatalogForDirectory( "+ baseURI +", "+path+").");
    InvCatalogImpl cat = dscan.makeCatalogForDirectory(baseURI, path);

    if (null == cat) {
      log.error("makeCatalogForDirectory failed = "+ workPath );
    }

    return cat;
  }

  /**
   * Process a request for the "latest" dataset. This must be configured through the datasetScan element.
   * Typically you call if the path ends with "latest.xml".
   * Actually this is a "resolver" service, that returns a catalog.xml containing latest dataset.
   *
   * @param servlet requesting servlet
   * @param req request
   * @param res response
   * @return true if request was processed successfully.
   * @throws IOException
   */
  public boolean processReqForLatestDataset( HttpServlet servlet, HttpServletRequest req, HttpServletResponse res) throws IOException
  {
    String orgPath = ServletUtil.getRequestPath(req);
    if (orgPath.startsWith("/"))
      orgPath = orgPath.substring(1);

    String path = orgPath;

    // strip off the filename
    int pos = path.lastIndexOf( "/" );
    if ( pos >= 0 )
    {
      path = path.substring( 0, pos );
    }

    if ( path.equals( "/" ) || path.equals( "" ) )
    {
      String resMsg = "No data at root level, \"/latest.xml\" request not available.";
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, resMsg.length() );
      log.debug( resMsg );
      res.sendError( HttpServletResponse.SC_NOT_FOUND, resMsg );
      return false;
    }

    // Find InvDatasetScan with a maximal match.
    DataRoot dataRoot = matchPath2( path);
    if ( dataRoot == null )
    {
      String resMsg = "No scan root matches requested path <" + path + ">.";
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, resMsg.length());
      log.warn( resMsg);
      res.sendError( HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // its gotta be a DatasetScan, not a DatasetRoot
    InvDatasetScan dscan = dataRoot.scan;
    if (dscan == null) {
      String resMsg = "Probable conflict between datasetScan and datasetRoot for path <" + path + ">.";
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, resMsg.length());
      log.warn( resMsg);
      res.sendError( HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Check that latest is allowed
    if ( dscan.getProxyDatasetHandlers().isEmpty() )
    {
      String resMsg = "No \"addProxies\" or \"addLatest\" on matching scan root <" + path + ">.";
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, resMsg.length() );
      log.warn( resMsg );
      res.sendError( HttpServletResponse.SC_NOT_FOUND, resMsg );
      return false;
    }

    String reqBase = ServletUtil.getRequestBase( req ); // this is the base of the request
    URI reqBaseURI = null;
    try
    {
      reqBaseURI = new URI( reqBase );
    }
    catch ( URISyntaxException e )
    {
      String resMsg = "Request base URL <" + reqBase + "> not valid URI (???): " + e.getMessage();
      ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg.length() );
      log.error( resMsg );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg );
      return false;
    }
    InvCatalog cat = dscan.makeLatestCatalogForDirectory( reqBaseURI, orgPath );

    if ( null == cat )
    {
      String resMsg = "Failed to build response catalog <" + path + ">.";
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, resMsg.length() );
      log.error( resMsg );
      res.sendError( HttpServletResponse.SC_NOT_FOUND, resMsg );
      return false;
    }

    // Send latest.xml catalog as response.
    InvCatalogFactory catFactory = new InvCatalogFactory( "default", true );
    String catAsString = catFactory.writeXML( (InvCatalogImpl) cat );
    PrintWriter out = res.getWriter();
    res.setContentType( "text/xml" );
    out.print( catAsString );
    res.setStatus( HttpServletResponse.SC_OK );
    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, catAsString.length() );
    log.debug( "Finished \"" + orgPath + "\"." );
    return true;
  }

  /**
   * Extract the DataRoot from the request.
   *
   * @param spath the url path
   * @return the DataRootMatch, or null if not found
   */
  public DataRootMatch findDataRootMatch( String spath ) {

    if (spath.length() > 0) {
      if (spath.startsWith("/"))
        spath = spath.substring(1);
    }

    DataRoot dataRoot = matchPath2( spath);
    if (dataRoot == null)
      return null;

    DataRootMatch match = new DataRootMatch();
    match.rootPath = dataRoot.path;
    match.remaining = spath.substring( match.rootPath.length());
    if (match.remaining.startsWith("/"))
      match.remaining = match.remaining.substring(1);
    match.dirLocation = dataRoot.dirLocation;
    return match;
  }


  /**
   * Extract the path from the request and call translatePath.
   * @param req the request
   * @return the translated path.
   */
  public String translatePath( HttpServletRequest req ) {
    String spath = req.getServletPath() + req.getPathInfo();

    if (spath.length() > 0) {
      if (spath.startsWith("/"))
        spath = spath.substring(1);
    }

    return translatePath( spath);
  }

   /**
   * Try to match the given path with all available data roots. If match, then translate into a CrawlableDataset path.
   * @param path the reletive path, ie req.getServletPath() + req.getPathInfo()
   * @return the translated path, as a CrawlableDataset path, or null if no dataroot matches.
   */
  public String translatePath( String path) {
    if (path.startsWith("/"))
      path= path.substring(1);

    DataRoot dataRoot = matchPath2( path);
    if (dataRoot == null) {
      log.debug("translatePath(): no InvDatasetScan for "+ path);
      return null;
    }

    // remove the matching part, the rest is the "data directory"
    String dataDir = path.substring( dataRoot.path.length());
    if (dataDir.startsWith("/"))
      dataDir = dataDir.substring(1);

    // the translated path is the dirLocaton plus the data directory
    String fullPath = dataRoot.dirLocation
                      + (dataRoot.dirLocation.endsWith( "/") ? "" : "/")
                      + dataDir;
    log.debug(" translatePath= "+ path+" to dataset path= "+fullPath);

    return fullPath;
  }

   /**
   * Try to match the given path with all available data roots. If match, then see if there is an NcML document associated
   * with the path.
   * @param path the reletive path, ie req.getServletPath() + req.getPathInfo()
   * @return the NcML (as a JDom element) assocated assocated with this path, or null if no dataroot matches, or no associated NcML.
   */
   public org.jdom.Element getNcML( String path) {
    if (path.startsWith("/"))
      path= path.substring(1);

    DataRoot dataRoot = matchPath2( path);
    if (dataRoot == null) {
      log.debug("_getNcML no InvDatasetScan for ="+ path);
      return null;
    }

     InvDatasetScan dscan = dataRoot.scan;
     if (dscan == null) return null;
     return dscan.getNcmlElement();
  }

   /* LOOK
   public String getResourceControl( String path) {
    if (path.startsWith("/"))
      path= path.substring(1);

    DataRoot dataRoot = matchPath2( path);
    if (dataRoot == null) {
      log.debug("_getNcML no InvDatasetScan for ="+ path);
      return null;
    }

     InvDatasetScan dscan = dataRoot.scan;
     if (dscan == null) return null;
     return dscan.getResourceControl();
  } */

  private DataRoot matchPath2(String fullpath) {
    if ((fullpath.length() > 0) && (fullpath.charAt(0) == '/'))
      fullpath = fullpath.substring(1);

    return (DataRoot) pathMatcher.match( fullpath);
  }

  /***************************************************************************/

  public void makeDebugActions() {
    DebugHandler debugHandler = new DebugHandler("catalogs");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showError", "Show catalog error logs") {
     public void doAction(DebugHandler.Event e) {
       e.pw.println( StringUtil.quoteHtmlContent( "\n"+catalogErrorLog.toString()));
     }
   };
   debugHandler.addAction( act);

    act = new DebugHandler.Action("showStatic", "Show static catalogs") {
     public void doAction(DebugHandler.Event e) {
       StringBuffer sbuff = new StringBuffer();
       for (int i = 0; i < catalogStaticList.size(); i++) {
         String catPath = (String) catalogStaticList.get(i);
         sbuff.append( " catalog=" ).append( catPath ).append( "\n" );
       }
       e.pw.println( StringUtil.quoteHtmlContent( "\n"+sbuff.toString()));
     }
   };
   debugHandler.addAction( act);

    act = new DebugHandler.Action("showRoots", "Show data roots") {
      public void doAction(DebugHandler.Event e) {
        Iterator iter = pathMatcher.iterator();
        while (iter.hasNext()) {
          DataRoot ds = (DataRoot) iter.next();
          String url = servletContextPath + "/dataDir/" + ds.path+"/";
          e.pw.print(" rootPath=" + ds.path + "  for directory= ");
          e.pw.println(" <a href='" +url+"'>"+ds.dirLocation+"</a>");
        }
      }
    };
    debugHandler.addAction( act);

    act = new DebugHandler.Action("reinit", "Reinitialize") {
      public void doAction(DebugHandler.Event e) {
        try {
          DatasetHandler.reinit();
          singleton.reinit();
          e.pw.println( "reinit ok");
        } catch (IOException e1) {
          e.pw.println( "Error on reinit "+e1.getMessage());
          log.error( "Error on reinit "+e1.getMessage());
        }
      }
    };
    debugHandler.addAction( act);

  }


}

