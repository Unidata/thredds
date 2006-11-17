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
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;
import thredds.util.PathMatcher;
import thredds.cataloggen.ProxyDatasetHandler;
import thredds.datatype.DateType;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;

/**
 * The DataRootHandler manages all the "data roots" for a given web application
 * and provides mappings from URLs to catalog and data objects (e.g.,
 * InvCatalog and CrawlableDataset).
 *
 * <p>The "data roots" are read in from one or more trees of config catalogs
 * and are defined by the datasetScan and datasetRoot elements in the config
 * catalogs.
 *
 * <p> Uses the singleton design pattern.
 *
 * @author caron
 */
public class DataRootHandler {
  // dont need to Guard/synchronize singleton, since creation and publication is only done by a servlet init() and therefore only in one thread (per ClassLoader).
  static private DataRootHandler singleton = null;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataRootHandler.class);

  /**
   * Initialize the CatalogHandler singleton instance.
   *
   * This method should only be called once. Additional calls will result in an IllegalStateException being thrown.
   *
   * @param contentPath should be absolute path under which all catalogs lay, typically ${tomcat_home}/content/thredds/
   * @throws IllegalStateException if called more than once.
   */
  static public void init(String contentPath, String servletContextPath) {
    if ( singleton != null)
    {
      log.error( "init(): Can only call init() once: failed call -- init(\"" + contentPath + "\",\"" + servletContextPath + "\"); and successful call -- init(\""+singleton.contentPath+"\",\""+singleton.servletContextPath+"\").");
      throw new IllegalStateException( "Can only call init() once. [Context path: failed="+servletContextPath+"; initialized="+singleton.servletContextPath+".");
    }
    singleton = new DataRootHandler(contentPath, servletContextPath);
  }

  /**
   * Get the singleton.
   *
   * The init() method must be called before this method is called.
   *
   * @return the singleton instance.
   *
   * @throws IllegalStateException if init() has not been called.
   */
  static public DataRootHandler getInstance() {
    if ( singleton == null)
    {
      log.error( "getInstance(): Called without init() having been called.");
      throw new IllegalStateException( "init() must be called first.");
    }
    return singleton;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //private final InvCatalogFactory factory;
  private final String contentPath;
  private final String servletContextPath;

  // @GuardedBy("this")
  //private StringBuffer catalogErrorLog = new StringBuffer(); // used during initialization
  // @GuardedBy("this")
  private ArrayList catalogRootList = new ArrayList(); // List of root catalog filenames (String)
  // @GuardedBy("this") LOOK should be able to access without synchronization
  private HashMap staticCatalogHash = new HashMap(); // Hash of static catalogs, key = path
  // @GuardedBy("this")
  private HashSet idHash = new HashSet(); // Hash of ids, to look for duplicates
  //  PathMatcher is "effectively immutable"; use volatile for visibilty
  private volatile PathMatcher pathMatcher = new PathMatcher(); // Keeps a Collection of DataRoot objects

  /**
   * Constructor.
   * @param contentPath all catalogs are reletive to this file path, eg {tomcat_home}/content/thredds
   * @param servletContextPath all catalogs are reletive to this URL path, eg thredds
   */
  private DataRootHandler(String contentPath, String servletContextPath) {
    this.contentPath = contentPath;
    this.servletContextPath = servletContextPath;
  }

  /**
   * Reread all the catalogs and reinit the data roots.
   * Call this when the catalogs have changed, so you dont have to restart.
   * Also available under the TDS debug menu.
   *
   * @throws IOException
   */
  public synchronized void reinit() throws IOException {
    staticCatalogHash = new HashMap();
    pathMatcher = new PathMatcher();
    catalogRootList = new ArrayList();
    idHash = new HashSet();

    log.info("\n**************************************\n**************************************\nCatalog reinit ");
    /* int size = catalogRootList.size();
    for (int i = 0; i < size; i++) {
      String path;
      path = (String) catalogRootList.get(i);
      // May be too expensive to synchronize over.
      initCatalog(path, true );
    }
    //writeCatalogErrorLog(); */
  }

  /**
   * Read the named catalog and extract the data roots from it.
   * Recurse into nested catalog refs that are reletive to contentPath.
   *
   * @param path file path of catalog, reletive to contentPath, ie catalog fullpath = contentPath + path.
   * @throws IOException
   */
  public synchronized void initCatalog(String path) throws IOException {
    log.info("\n**************************************\nCatalog init "+path);
    if ( ! catalogRootList.contains( path) )
      catalogRootList.add(path);
    initCatalog(path, true );
    //writeCatalogErrorLog();
  }

  /**
   * Does the actual work. Only called by synchronized methods.
   * @param path file path of catalog, reletive to contentPath, ie catalog fullpath = contentPath + path.
   * @param recurse  if true, look for catRefs in this catalog
   * @throws IOException
   */
  private void initCatalog(String path, boolean recurse ) throws IOException {
    String catalogFullPath = contentPath + path;

    // make sure we dont already have it
      if ( staticCatalogHash.containsKey(path)) {
        log.warn("DataRootHandler.initCatalog has already seen catalog=" + catalogFullPath + " possible loop (skip)");
        return;
      }

    InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true); // always validate the config catalogs
    InvCatalogImpl cat = readCatalog( factory, path, catalogFullPath );
    if ( cat == null )
    {
      log.warn( "initCatalog(): failed to read catalog <" + catalogFullPath + ">." );
      return;
    }

    // look for datasetRoots
    Iterator roots = cat.getDatasetRoots().iterator();
    while ( roots.hasNext() ) {
      InvProperty p = (InvProperty) roots.next();
      addRoot( p.getName(), p.getValue() );
    }
    Iterator services = cat.getServices().iterator();
    while ( services.hasNext() ) {
      InvService s = (InvService) services.next();
      roots = s.getDatasetRoots().iterator();
      while ( roots.hasNext() ) {
        InvProperty p = (InvProperty) roots.next();
        addRoot( p.getName(), p.getValue() );
      }
    }

    // get the directory path, reletive to the contentPath
    int pos = path.lastIndexOf( "/" );
    String dirPath = ( pos > 0 ) ? path.substring( 0, pos + 1 ) : "";

    // look for datasetScans and NcML elements
    findSpecialDatasets( cat.getDatasets() );

    // add catalog to hash tables
    staticCatalogHash.put(path, cat);
    if (log.isDebugEnabled()) log.debug("  add static catalog=" + path);

    if (recurse) {
      check4Catrefs(dirPath, cat.getDatasets());
    }
  }

  private InvCatalogImpl readCatalog(InvCatalogFactory factory, String path, String catalogFullPath) {
    URI uri;
    try {
      uri = new URI("file:" + StringUtil.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    }
    catch (URISyntaxException e) {
      log.error("readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    // read the catalog
    log.debug("readCatalog(): full path=" + catalogFullPath + "; path=" + path);
    InvCatalogImpl cat = null;
    FileInputStream ios = null;
    try {
      ios = new FileInputStream(catalogFullPath);
      cat = factory.readXML(ios, uri);

      synchronized (this) {
        log.info("\nCatalog "+catalogFullPath);
        StringBuffer sbuff = new StringBuffer();
        if (!cat.check(sbuff)) {
          log.error("readCatalog(): invalid catalog=" + catalogFullPath + "; " + sbuff.toString());
          return null;
        }
        log.info(sbuff.toString());
      }

    }
    catch (Throwable t) {
      log.error("readCatalog(): Exception on catalog=" + catalogFullPath + " " + t.getMessage(), t);
      return null;
    }
    finally {
      if (ios != null) {
        try {
          ios.close();
        }
        catch (IOException e) {
          log.error("readCatalog(): error closing" + catalogFullPath);
        }
      }
    }

    return cat;
  }

  /**
   * Look for datasetScans and NcML elements recursively in a list of InvDatasetImpl, but dont follow catRefs.
   * Only called by synchronized methods.
   * @param dsList the list of InvDatasetImpl
   */
  private void findSpecialDatasets( List dsList) {
    Iterator iter = dsList.iterator();
    while (iter.hasNext()) {
      InvDatasetImpl invDataset = (InvDatasetImpl) iter.next();

      // look for duplicate ids
      String id = invDataset.getUniqueID();
      if (id != null) {
        if (idHash.contains(id)) {
          log.warn("Duplicate id on  " + invDataset.getFullName() + " id= "+id);
        } else {
          idHash.add(id);
        }
      }

      if (invDataset instanceof InvDatasetScan) {
        InvDatasetScan ds = (InvDatasetScan) invDataset;
        InvService service = ds.getServiceDefault();
        if (service == null) {
          log.error("InvDatasetScan " + ds.getFullName() + " has no default Service - skipping");
          continue;
        }

        addRoot(ds);

      } else if (invDataset instanceof InvDatasetFmrc) {
          InvDatasetFmrc fmrc = (InvDatasetFmrc) invDataset;
          addRoot(fmrc);

        // not a DatasetScan or InvDatasetFmrc
      } else if (invDataset.getNcmlElement() != null) {
          DatasetHandler.putNcmlDataset(invDataset.getUrlPath(), invDataset);
      }

      if (!(invDataset instanceof InvCatalogRef)) {
        // recurse
        findSpecialDatasets( invDataset.getDatasets());
      }
    }

  }

  // Only called by synchronized methods
  private boolean addRoot(InvDatasetScan dscan) {
    // check for duplicates
    String path = dscan.getPath();

    if (path == null) {
      log.error("**Error: "+dscan.getFullName()+" missing a path attribute.");
      return false;
    }

    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      if (!droot.dirLocation.equals( dscan.getScanDir())) {
        String message = "**Error: already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
            " wanted to map to fmrc=<" + dscan.getScanDir() + "> in catalog "+dscan.getParentCatalog().getUriString();
        log.error(message);
      }

      return false;
    }

    // LOOK !!
    // rearrange scanDir if it starts with content
    if (dscan.getScanDir().startsWith("content/"))
      dscan.setScanDir(contentPath + "public/" + dscan.getScanDir().substring(8));

    File file = new File(dscan.getScanDir());
    if (!file.exists()) {
      log.error("**Error: DatasetScan =" + path + " directory= <" + dscan.getScanDir() + "> does not exist");
      return false;
    }

    // add it
    droot = new DataRoot(dscan);
    pathMatcher.put(path, droot);

    log.debug(" added rootPath=<" + path + ">  for directory= <" + dscan.getScanDir() + ">");
    return true;
  }

  // Only called by synchronized methods
  private boolean addRoot(InvDatasetFmrc fmrc) {
    // check for duplicates
    String path = fmrc.getPath();

    if (path == null) {
      log.error(fmrc.getFullName()+" missing a path attribute.");
      return false;
    }

    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
        log.error("**Error: already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
            " wanted to use by FMRC Dataset =<" + fmrc.getFullName() + ">");
      return false;
    }

    // add it
    droot = new DataRoot(fmrc);

    if (droot.dirLocation != null) {
      File file = new File(droot.dirLocation);
      if (!file.exists()) {
        log.error("**Error: DatasetFmrc =" + droot.path + " directory= <" + droot.dirLocation + "> does not exist");
        return false;
      }
    }

    pathMatcher.put(path, droot);

    log.debug(" added rootPath=<" + path + ">  for fmrc= <" + fmrc.getFullName() + ">");
    return true;
  }

  /* private void addRoots(String dirPath, String contextPath, InvService service, InvProperty p) {
    String serviceBase = service.getBase();
    String path = p.getName();
    String dirLocation = p.getValue();

    if (service.getServiceType() != ServiceType.COMPOUND) {

      if (serviceBase.length() != 0) { // not blank
        if (contextPath != null) {
          if (serviceBase.startsWith(contextPath + "/"))
            serviceBase = serviceBase.substring(contextPath.length() + 1);
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
        if (contextPath != null) {
          if (nestedServiceBase.startsWith(contextPath + "/"))
            nestedServiceBase = nestedServiceBase.substring(contextPath.length() + 1);
        }
        addRoot(nestedServiceBase + path, dirLocation);
      }
    }
  } */

  // Only called by synchronized methods
  private boolean addRoot(String path, String dirLocation) {
    // check for duplicates
    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      log.error("**Error: already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">"+
              " wanted to map to <" + dirLocation + ">");

      return false;
    }

    // rearrange dirLocation if it starts with content
    if (dirLocation.startsWith("content/"))
      dirLocation = contentPath + "public/" + dirLocation.substring(8);

    File file = new File(dirLocation);
    if (!file.exists()) {
      log.error("**Error: Data Root =" + path + " directory= <" + dirLocation + "> does not exist");
      return false;
    }

    // add it
    droot = new DataRoot(path, dirLocation);
    pathMatcher.put(path, droot);

    log.debug(" added rootPath=<" + path + ">  for directory= <" + dirLocation + ">");
    return true;
  }

  // Only called by synchronized methods
  private void check4Catrefs(String dirPath, List datasets)  throws IOException
  {
    Iterator iter = datasets.iterator();
    while (iter.hasNext()) {
      InvDatasetImpl invDataset = (InvDatasetImpl) iter.next();

      if ((invDataset instanceof InvCatalogRef) && !(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFmrc)) {
        InvCatalogRef catref = (InvCatalogRef) invDataset;
        String href = catref.getXlinkHref();
        if (!href.startsWith("http:")) // must be a reletive catref
          initCatalog(dirPath + href, true ); // go check it out

      } else if (!(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFmrc)) {
        // recurse through nested datasets
        check4Catrefs(dirPath, invDataset.getDatasets());
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  public class DataRootMatch {
    String rootPath;     // this is the matching part of the URL
    String remaining;   // this is the part of the URL that didnt match
    String dirLocation;   // this is the directory that should be substituted for the rootPath
    DataRoot dataRoot;  // this is the directory that should be substituted for the rootPath
  }

  public class DataRoot {
    String path;         // match this path
    String dirLocation; // to this directory
    InvDatasetScan scan; // the InvDatasetScan that created this (may be null)
    InvDatasetFmrc fmrc; // the InvDatasetFmrc that created this (may be null)

    // Use this to access CrawlableDataset in dirLocation.
    // I.e., used by datasets that reference a <datasetRoot>
    InvDatasetScan datasetRootProxy;

    DataRoot(InvDatasetFmrc fmrc) {
      this.path = fmrc.getPath();
      this.fmrc = fmrc;

      InvDatasetFmrc.InventoryParams params = fmrc.getFmrcInventoryParams();
      if (null != params)
        dirLocation = params.location;
    }

    DataRoot(InvDatasetScan scan) {
      this.path = scan.getPath();
      this.scan = scan;
      this.dirLocation = scan.getScanDir();
      this.datasetRootProxy = null;
    }

    DataRoot(String path, String dirLocation) {
      this.path = path;
      this.dirLocation = dirLocation;
      this.scan = null;

      makeProxy();
    }

    void makeProxy() {
      this.datasetRootProxy = new InvDatasetScan( null, "", this.path, this.dirLocation,
                                                  null, null, null, null, null, false, null, null, null, null );
    }


    // used by PathMatcher
    public String toString() {
      return path;
    }

    /**
     * Instances which have same path are equal.
     */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DataRoot root = (DataRoot) o;
      return path.equals(root.path);
    }

    public int hashCode() {
      return path.hashCode();
    }
  }

  /**
   * Find the longest match for this path.
   * @param fullpath
   * @return best DataRoot or null if no match.
   */
  private DataRoot matchPath2(String fullpath) {
    if ((fullpath.length() > 0) && (fullpath.charAt(0) == '/'))
      fullpath = fullpath.substring(1);

    return (DataRoot) pathMatcher.match(fullpath);
  }

  /**
   * Extract the DataRoot from the request.
   * Use this when you need to manipulate the path based on the part that matches a DataRoot.
   *
   * @param req the request
   * @return the DataRootMatch, or null if not found
   */
  public DataRootMatch findDataRootMatch(HttpServletRequest req) {
    String spath = req.getPathInfo();

    if (spath.length() > 0) {
      if (spath.startsWith("/"))
        spath = spath.substring(1);
    }

    return findDataRootMatch( spath);
  }

  public DataRootMatch findDataRootMatch(String spath) {
    DataRoot dataRoot = matchPath2(spath);
    if (dataRoot == null)
      return null;

    DataRootMatch match = new DataRootMatch();
    match.rootPath = dataRoot.path;
    match.remaining = spath.substring(match.rootPath.length());
    if (match.remaining.startsWith("/"))
      match.remaining = match.remaining.substring(1);
    match.dirLocation = dataRoot.dirLocation;
    match.dataRoot = dataRoot;
    return match;
  }

  /*
   * Extract the path from the request and call translatePath.
   *
   * @param req the request
   * @return the translated path.
   * @deprecated Should instead use InvDatasetScan.translatePathToLocation() or something like ((CrawlableDatasetFile)DataRootHandler.findRequestedDataset( path )).getFile();

   *
  public String translatePath(HttpServletRequest req) {
    String spath = req.getPathInfo(); // was req.getServletPath() + req.getPathInfo();

    if (spath.length() > 0) {
      if (spath.startsWith("/"))
        spath = spath.substring(1);
    }

    return translatePath(spath);
  }

  /*
   * Try to match the given path with all available data roots. If match, then translate into a CrawlableDataset path.
   *
   * @param path the reletive path, ie req.getPathInfo()
   * @return the translated path, as a CrawlableDataset path, or null if no dataroot matches.
   * @deprecated Should instead use InvDatasetScan.translatePathToLocation() or something like ((CrawlableDatasetFile)DataRootHandler.findRequestedDataset( path )).getFile();
   *
  public String translatePath(String path) {
    if (path.startsWith("/"))
      path = path.substring(1);

    DataRoot dataRoot = matchPath2(path);
    if (dataRoot == null) {
      log.debug("translatePath(): no InvDatasetScan for " + path);
      return null;
    }

    // remove the matching part, the rest is the "data directory"
    String dataDir = path.substring(dataRoot.path.length());
    if (dataDir.startsWith("/"))
      dataDir = dataDir.substring(1);

    // the translated path is the dirLocaton plus the data directory
    String fullPath = dataRoot.dirLocation
            + (dataRoot.dirLocation.endsWith("/") ? "" : "/")
            + dataDir;

    if (log.isDebugEnabled()) log.debug(" translatePath= " + path + " to dataset path= " + fullPath);

    return fullPath;
  } */

  /**
   * Return true if the given path matches a dataRoot, otherwise return false.
   * A succesful match means that the request is either for a dynamic catalog
   * or a dataset.
   *
   * @param path the request path, ie req.getServletPath() + req.getPathInfo()
   * @return true if the given path matches a dataRoot, otherwise false.
   */
  public boolean hasDataRootMatch(String path) {
    if ( path.length() > 0 )
      if (path.startsWith("/"))
        path = path.substring(1);

    DataRoot dataRoot = matchPath2(path);
    if (dataRoot == null) {
      log.debug("hasDataRootMatch(): no InvDatasetScan for " + path);
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
   * @throws IOException if an I/O error occurs while locating the requested dataset.
   */
  public CrawlableDataset getCrawlableDataset(String path)
          throws IOException
  {
    if (path.length() > 0) {
      if (path.startsWith("/"))
        path = path.substring(1);
    }

    DataRoot reqDataRoot = matchPath2(path);
    if (reqDataRoot == null)
      return null;
    if ( reqDataRoot.scan != null)
      return reqDataRoot.scan.requestCrawlableDataset( path );

    if ( reqDataRoot.fmrc != null )
      return null;

    if ( reqDataRoot.dirLocation != null ) {
      if (reqDataRoot.datasetRootProxy == null)
        reqDataRoot.makeProxy();
      return reqDataRoot.datasetRootProxy.requestCrawlableDataset( path );
    }

    return null;
  }

  /**
   * Return the java.io.File represented by the CrawlableDataset to which the
   * given path maps. Null is returned if the dataset does not exist, the
   * matching InvDatasetScan or DataRoot filters out the requested
   * CrawlableDataset, the CrawlableDataset does not represent a File
   * (i.e., it is not a CrawlableDatasetFile), or an I/O error occurs whil
   * locating the requested dataset.
   *
   * @param path the request path.
   * @return the requested java.io.File or null.
   * @throws IllegalStateException if the request is not for a descendant of (or the same as) the matching DatasetRoot collection location.
   */
  public File getCrawlableDatasetAsFile( String path ) {
    if (path.length() > 0) {
      if (path.startsWith("/"))
        path = path.substring(1);
    }

    // hack in the fmrc for fileServer
    DataRootMatch match = findDataRootMatch(path);
    if (match == null)
      return null;
    if (match.dataRoot.fmrc != null) {
      return match.dataRoot.fmrc.getFile(match.remaining);
    }

    CrawlableDataset crDs = null;
    try
    {
      crDs = getCrawlableDataset( path );
    }
    catch ( IOException e )
    {
      return null;
    }
    if ( crDs == null ) return null;
    File retFile = null;
    if ( crDs instanceof CrawlableDatasetFile )
      retFile = ((CrawlableDatasetFile) crDs).getFile();

    return retFile;
  }

  public boolean isProxyDataset( String path )
  {
    ProxyDatasetHandler pdh = this.getMatchingProxyDataset( path );
    return pdh != null;
  }

  public boolean isProxyDatasetResolver( String path )
  {
    ProxyDatasetHandler pdh = this.getMatchingProxyDataset( path );
    if ( pdh == null )
      return false;

    return pdh.isProxyDatasetResolver();
  }

  private ProxyDatasetHandler getMatchingProxyDataset( String path )
  {
    InvDatasetScan scan = this.getMatchingScan( path);
    if (null == scan) return null;

    int index = path.lastIndexOf( "/" );
    String proxyName = path.substring( index + 1 );

    Map pdhMap = scan.getProxyDatasetHandlers();
    if ( pdhMap == null ) return null;

    return (ProxyDatasetHandler) pdhMap.get( proxyName );
  }

  private InvDatasetScan getMatchingScan( String path )
  {
    DataRoot reqDataRoot = matchPath2( path );
    if ( reqDataRoot == null )
      return null;

    InvDatasetScan scan = null;
    if ( reqDataRoot.scan != null )
      scan = reqDataRoot.scan;
    else if ( reqDataRoot.fmrc != null )  // TODO refactor UGLY FMRC HACK
      scan = reqDataRoot.fmrc.getRawFileScan();

    return scan;
  }

  public InvCatalog getProxyDatasetResolverCatalog( String path, URI baseURI )
  {
    if ( ! isProxyDatasetResolver( path ) )
      throw new IllegalArgumentException( "Not a proxy dataset resolver path <" + path + ">." );

    InvDatasetScan scan = this.getMatchingScan( path );

    // Call the matching InvDatasetScan to make the proxy dataset resolver catalog.
    //noinspection UnnecessaryLocalVariable
    InvCatalogImpl cat = scan.makeProxyDsResolverCatalog( path, baseURI );

    return cat;
  }

//  public CrawlableDataset getProxyDatasetActualDatset( String path)
//  {
//    ProxyDatasetHandler pdh = this.getMatchingProxyDataset( path );
//    if ( pdh == null )
//      return null;
//
//    pdh.getActualDataset( );
//
//  }

  /** @deprecated */
  public void handleRequestForProxyDatasetResolverCatalog( HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    String path = req.getPathInfo();
    if ( ! isProxyDatasetResolver( path) )
    {
      String resMsg = "Request <" + path + "> not for proxy dataset resolver.";
      ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg.length() );
      log.error( "handleRequestForProxyDatasetResolverCatalog(): " + resMsg );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg );
      return;

    }

    String baseUriString = req.getRequestURL().toString();
    URI baseURI = null;
    try
    {
      baseURI = new URI( baseUriString );
    }
    catch ( URISyntaxException e )
    {
      String resMsg = "Request URL <" + baseUriString + "> not a valid URI: " + e.getMessage();
      ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg.length() );
      log.error( "handleRequestForProxyDatasetResolverCatalog(): " + resMsg );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg );
      return;
    }

    InvCatalogImpl cat = (InvCatalogImpl) this.getProxyDatasetResolverCatalog( path, baseURI );
    if ( cat == null )
    {
      String resMsg = "Could not generate proxy dataset resolver catalog <" + path + ">.";
      ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg.length() );
      log.error( "handleRequestForProxyDatasetResolverCatalog(): " + resMsg );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg );
      return;
    }

    // Return catalog as XML response.
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
    String result = catFactory.writeXML( cat );
    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, result.length() );

    res.setContentLength( result.length() );
    res.setContentType( "text/xml" );
    res.getOutputStream().write( result.getBytes() );

    return;
  }

  /**
   * DO NOT USE, this is Ethan's attempt at designing a generic way to handle data requests.
   *
   * Only use is in ExampleThreddsServlet.
   *
   * @param path
   * @param dsp
   * @param req
   * @param res
   * @throws IOException
   *
   * @deprecated DO NOT USE
   */
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
      if ( dsPath != null )
      {
        // Use the path returned by the DataServiceProvider.
        crDsPath = dsPath;
        dspCanHandle = true;
      }
      else
      {
        // DataServiceProvider recognized request path but returned a null dataset path. Hmm?
        log.warn( "handleRequestForDataset(): DataServiceProvider recognized request path <" + path + "> but returned a null dataset path, using request path." );
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
    CrawlableDataset crDs = this.getCrawlableDataset( crDsPath );
    if ( crDs == null )
    {
      // @todo Check if this is a proxy dataset request (not resolver).
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

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Request processing

  /* public boolean serveCatalog(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    if (processReqForStaticCatalog(servlet, req, res))
      return true;

    if (processReqForDynamicCatalog(servlet, req, res))
      return true;

    return false;
  }


  /**
   * This looks to see if this is a request for a catalog.
   * The request must end in ".html" or ".xml"  or "/"
   * Check to see if a static or dynamic catalog exists for it.
   * If so, it processes the request completely.
   * req.getPathInfo() is used for the path (ie ignores context and servlet path)
   *
   * @param req     the request
   * @param res     the response
   * @return true if request was handled
   * @throws IOException
   * @throws ServletException
   */
  public boolean processReqForCatalog( HttpServletRequest req, HttpServletResponse res)
          throws IOException, ServletException {

    boolean isHtmlReq = false;

    String catPath = req.getPathInfo();
    StringBuffer catBase = req.getRequestURL();
    if (catPath == null) {
      return false;

    } else if (catPath.endsWith("/")) {
      isHtmlReq = true;
      catPath = catPath + "catalog.xml";
      catBase.append( "catalog.xml");

    } else if (catPath.endsWith(".html")) {
      isHtmlReq = true;
      // Change ".html" to ".xml"
      int len = catPath.length();
      catPath = catPath.substring(0, len - 4) + "xml";

      len = catBase.lastIndexOf( ".html" );
      catBase.replace( len+1, len+6, "xml");

    } else if (! catPath.endsWith(".xml")) {
      // Not a catalog request.
      return false;
    }

    URI baseURI;
    try {
      baseURI = new URI( catBase.toString() );
    }
    catch (URISyntaxException e) {
      log.error("processReqForCatalog(): Request base <" + catBase + "> not a URI: " + e.getMessage());
      throw new ServletException("Request base <" + catBase + "> not a URI.", e);
    }
    InvCatalogImpl catalog = (InvCatalogImpl) getCatalog(catPath, baseURI);
    if (catalog == null)
      return false;

    // @todo Check that catalog is valid [ Use catalog.check() ].

    // LOOK: What URL is this ?? Deal with catalogServices
    String query = req.getQueryString();
    if (query != null) {
      CatalogServicesServlet.handleCatalogServiceRequest(catalog, baseURI, isHtmlReq, req, res);
      return true;
    }

    // Return HTML view of catalog.
    if (isHtmlReq) {
      HtmlWriter.getInstance().writeCatalog(res, catalog, true);
      return true;
    }

    // Return catalog as XML response.
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(false);
    String result = catFactory.writeXML(catalog);
    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, result.length());

    res.setContentLength(result.length());
    res.setContentType("text/xml");
    res.getOutputStream().write(result.getBytes());
    return true;
  }

  /**
   * If a catalog exists and is allowed (not filtered out) for the given path, return
   * the catalog as an InvCatalog. Otherwise, return null.
   * <p/>
   * The validity of the returned catalog is not guaranteed. Use InvCatalog.check() to
   * check that the catalog is valid.
   *
   * @param path    the path for the requested catalog.
   * @param baseURI the base URI for the catalog, used to resolve relative URLs.
   * @return the requested InvCatalog, or null if catalog does not exist or is not allowed.
   */
  public InvCatalog getCatalog(String path, URI baseURI) {
    if (path == null)
      return null;

    String workPath = path;
    if (workPath.startsWith("/"))
      workPath = workPath.substring(1);

    // Check for static catalog.
    InvCatalogImpl catalog;
    synchronized (this) {
      catalog = (InvCatalogImpl) staticCatalogHash.get(workPath);
    }

    if (catalog != null) {
      // Check if the cached catalog is stale.
      DateType expiresDateType = catalog.getExpires();
      if (expiresDateType != null) {
        if (expiresDateType.getDate().getTime() < System.currentTimeMillis()) {
          // If stale, re-read catalog from disk.
          String catalogFullPath = contentPath + workPath;
          InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false); // no validation
          InvCatalogImpl reReadCat = readCatalog(factory, workPath, catalogFullPath);

          if (reReadCat != null) {
            synchronized (this) {
              staticCatalogHash.put(workPath, reReadCat);
            }
            catalog = reReadCat;
          }
        }
      }
      // this is the first time we actually know an absolute, external path for the catalog, so we set it here
      // LOOK however, this causes a possible thread safety problem
      catalog.setBaseURI(baseURI);
    }

    // Check for dynamic catalog.
    if (catalog == null)
      catalog = makeDynamicCatalog(workPath, baseURI);

    // Check for proxy dataset resolver catalog.
    if (catalog == null && this.isProxyDatasetResolver(workPath))
      catalog = (InvCatalogImpl) this.getProxyDatasetResolverCatalog(workPath, baseURI);

    return catalog;
  }

  private InvCatalogImpl makeDynamicCatalog(String path, URI baseURI)
  {
    String workPath = path;

    // Make sure this is a dynamic catalog request.
    if ( ! path.endsWith( "/catalog.xml"))
      return null;

    // strip off the filename
    int pos = workPath.lastIndexOf("/");
    if (pos >= 0)
      workPath = workPath.substring(0, pos);

    // now look through the InvDatasetScans and get a maximal match
    DataRootMatch match = findDataRootMatch(workPath);
    if (match == null) {
      log.warn("makeDynamicCatalog(): No DataRoot for =" + workPath + " request path= " + path); // LOOK why should there be a messaage ?
      match = findDataRootMatch(workPath); // debug
      return null;
    }

    // look for the fmrc
    if (match.dataRoot.fmrc != null) {
      return match.dataRoot.fmrc.makeCatalog( match.remaining, path, baseURI);
    }

    // Check that path is allowed, ie not filtered out
    try
    {
      if (getCrawlableDataset(workPath) == null)
        return null;
    }
    catch ( IOException e )
    {
      log.error( "makeDynamicCatalog(): I/O error on request <" + path + ">: " + e.getMessage(), e);
      return null;
    }

    // at this point, its gotta be a DatasetScan, not a DatasetRoot
    if (match.dataRoot.scan == null) {
      log.warn("makeDynamicCatalog(): No InvDatasetScan for =" + workPath + " request path= " + path);
      return null;
    }

    InvDatasetScan dscan = match.dataRoot.scan;
    log.debug("Calling makeCatalogForDirectory( " + baseURI + ", " + path + ").");
    InvCatalogImpl cat = dscan.makeCatalogForDirectory( path, baseURI );

    if (null == cat) {
      log.error("makeCatalogForDirectory failed = " + workPath);
    }

    return cat;
  }

  /* public void handleRequestForDataset(String path, DataServiceProvider dsp, HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    // Can the DataServiceProvider handle the data request given by the path?
    DataServiceProvider.DatasetRequest dsReq = dsp.getRecognizedDatasetRequest(path, req);
    String crDsPath;
    boolean dspCanHandle = false;
    if (dsReq != null) {
      String dsPath = dsReq.getDatasetPath();
      if (dsPath != null) {
        // Use the path returned by the DataServiceProvider.
        crDsPath = dsPath;
        dspCanHandle = true;
      } else {
        // DataServiceProvider recognized request path but returned a null dataset path. Hmm?
        log.warn("handleRequestForDataset(): DataServiceProvider recognized request path <" + path + "> but returned a null dataset path, using request path.");
        // Use the incoming request path.
        crDsPath = path;
      }
    } else {
      // DataServiceProvider  did not recognized request path.
      // Use the incoming request path.
      crDsPath = path;
    }

    // Find the CrawlableDataset represented by the request path.
    CrawlableDataset crDs = this.findRequestedDataset(crDsPath);
    if (crDs == null) {
      // Request is not for a known (or allowed) dataset.
      res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
      return;
    }

    if (dspCanHandle) {
      // Request recognized by DataServiceProvider, handle dataset request.
      dsp.handleRequestForDataset(dsReq, crDs, req, res);
      return;
    } else {
      // Request not recognized by DataServiceProvider.
      if (crDs.isCollection()) {
        // Handle request for a collection dataset.
        dsp.handleUnrecognizedRequestForCollection(crDs, req, res);
        return;
      }

      // Handle request for an atomic dataset.
      dsp.handleUnrecognizedRequest(crDs, req, res);
      return;
    }
  }  */

  /**
   * Process a request for the "latest" dataset. This must be configured
   * through the datasetScan element. Typically you call if the path ends with
   * "latest.xml". Actually this is a "resolver" service, that returns a
   * catalog.xml containing latest dataset.
   *
   * @param servlet requesting servlet
   * @param req     request
   * @param res     response
   * @return true if request was processed successfully.
   * @throws IOException if have I/O trouble writing response.
   *
   * @deprecated  Instead use {@link #processReqForCatalog(HttpServletRequest, HttpServletResponse) processReqForCatalog()} which provides more general proxy dataset handling.
   */
  public boolean processReqForLatestDataset(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res)
          throws IOException
  {
    String orgPath = req.getPathInfo();
    if (orgPath.startsWith("/"))
      orgPath = orgPath.substring(1);

    String path = orgPath;

    // strip off the filename
    int pos = path.lastIndexOf("/");
    if (pos >= 0) {
      path = path.substring(0, pos);
    }

    if (path.equals("/") || path.equals("")) {
      String resMsg = "No data at root level, \"/latest.xml\" request not available.";
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, resMsg.length());
      log.debug(resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Find InvDatasetScan with a maximal match.
    DataRoot dataRoot = matchPath2(path);
    if (dataRoot == null) {
      String resMsg = "No scan root matches requested path <" + path + ">.";
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, resMsg.length());
      log.warn(resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // its gotta be a DatasetScan, not a DatasetRoot
    InvDatasetScan dscan = dataRoot.scan;
    if (dscan == null) {
      String resMsg = "Probable conflict between datasetScan and datasetRoot for path <" + path + ">.";
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, resMsg.length());
      log.warn(resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Check that latest is allowed
    if (dscan.getProxyDatasetHandlers() == null) {
      String resMsg = "No \"addProxies\" or \"addLatest\" on matching scan root <" + path + ">.";
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, resMsg.length());
      log.warn(resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    String reqBase = ServletUtil.getRequestBase(req); // this is the base of the request
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
    InvCatalog cat = dscan.makeLatestCatalogForDirectory(orgPath, reqBaseURI );

    if (null == cat) {
      String resMsg = "Failed to build response catalog <" + path + ">.";
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, resMsg.length());
      log.error(resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Send latest.xml catalog as response.
    InvCatalogFactory catFactory = new InvCatalogFactory("default", true);
    String catAsString = catFactory.writeXML((InvCatalogImpl) cat);
    PrintWriter out = res.getWriter();
    res.setContentType("text/xml");
    out.print(catAsString);
    res.setStatus(HttpServletResponse.SC_OK);
    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, catAsString.length());
    log.debug("Finished \"" + orgPath + "\".");
    return true;
  }

  /**
   * Try to match the given path with all available data roots. If match, then see if there is an NcML document associated
   * with the path.
   *
   * @param path the reletive path, ie req.getServletPath() + req.getPathInfo()
   * @return the NcML (as a JDom element) assocated assocated with this path, or null if no dataroot matches, or no associated NcML.
   */
  public org.jdom.Element getNcML(String path) {
    if (path.startsWith("/"))
      path = path.substring(1);

    DataRoot dataRoot = matchPath2(path);
    if (dataRoot == null) {
      log.debug("_getNcML no InvDatasetScan for =" + path);
      return null;
    }

    InvDatasetScan dscan = dataRoot.scan;
    if ( dscan == null ) dscan = dataRoot.datasetRootProxy;
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

  /*   public boolean isRequestForConfigCatalog(String path) {
    String catPath = path;
    if (catPath.endsWith(".html")) {
      // Change ".html" to ".xml"
      int len = catPath.length();
      catPath = catPath.substring(0, len - 4) + "xml";
    } else if (! catPath.endsWith(".xml")) {
      // Not a catalog request.
      return false;
    }

    if (staticHash.containsKey(catPath)) return true;
    if (catHasScanHash.get(catPath) != null) return true;
    return false;
  }

  public boolean isRequestForGeneratedCatalog(String path) {
    if (path.endsWith("/")
            || path.endsWith("/catalog.html")
            || path.endsWith("/catalog.xml")) {
      String dsPath = path;

      // strip off the filename
      int pos = dsPath.lastIndexOf("/");
      if (pos >= 0)
        dsPath = dsPath.substring(0, pos);

      // Check that path is allowed.
      CrawlableDataset crDs = findRequestedDataset(dsPath);
      if (crDs == null)
        return false;
      if (crDs.isCollection())
        return true;
    }
    return false;
  } */


  /**
   * ***********************************************************************
   */

  public void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("catalogs");
    DebugHandler.Action act;

    /* act = new DebugHandler.Action("showError", "Show catalog error logs") {
     public void doAction(DebugHandler.Event e) {
       synchronized ( DataRootHandler.this )
       {
         try {
          File fileOut = new File(contentPath + "logs/catalogError.log");
          FileOutputStream fos = new FileOutputStream(fileOut);
          //String contents = catalogErrorLog.toString();
          thredds.util.IO.writeContents(contents, fos);
          fos.close();
        } catch (IOException ioe) {
          log.error("DataRootHandler.writeCatalogErrorLog error ", ioe);
        }

         e.pw.println( StringUtil.quoteHtmlContent( "\n"+catalogErrorLog.toString()));
       }
     }
   };
   debugHandler.addAction( act);  */

    act = new DebugHandler.Action("showStatic", "Show static catalogs") {
     public void doAction(DebugHandler.Event e) {
       ArrayList list;
       StringBuffer sbuff = new StringBuffer();
       synchronized ( DataRootHandler.this )
       {
         list = new ArrayList( staticCatalogHash.keySet());
         Collections.sort(list);
         for (int i = 0; i < list.size(); i++) {
           String catPath = (String) list.get(i);
           sbuff.append( " catalog= " ).append( catPath ).append( "\n" );
         }
       }
       e.pw.println( StringUtil.quoteHtmlContent( "\n"+sbuff.toString()));
     }
   };
   debugHandler.addAction( act);

    act = new DebugHandler.Action("showRoots", "Show data roots") {
      public void doAction(DebugHandler.Event e) {
        synchronized ( DataRootHandler.this )
        {
          Iterator iter = pathMatcher.iterator();
          while (iter.hasNext()) {
            DataRoot ds = (DataRoot) iter.next();
            e.pw.print(" <b>" + ds.path+"</b>");
            String url = servletContextPath + "/dataDir/" + ds.path+"/";
            if (ds.fmrc == null) {
              String type = (ds.scan == null) ? "root":"scan";
              e.pw.println(" for "+type+" directory= <a href='" +url+"'>"+ds.dirLocation+"</a> ");
            } else {
              if (ds.dirLocation == null) {
                url = servletContextPath + "/"+ ds.path;
                e.pw.println("  for fmrc= <a href='" +url+"'>"+ds.fmrc.getXlinkHref()+"</a>");
              } else {
                e.pw.println("  for fmrc= <a href='" +url+"'>"+ds.dirLocation+"</a>");
              }
            }
          }
        }
      }
    };
    debugHandler.addAction( act);

    /* moved to ThreddsDefaultServlet
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
    debugHandler.addAction( act); */
  }

}

