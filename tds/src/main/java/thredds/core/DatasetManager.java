/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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

package thredds.core;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.*;
import thredds.featurecollection.FeatureCollectionCache;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.server.admin.DebugCommands;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.servlet.DatasetSource;
import thredds.servlet.ServletUtil;
import thredds.servlet.restrict.Authorizer;
import thredds.servlet.restrict.RestrictedAccessController;
import thredds.util.TdsPathUtils;

import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.cache.FileFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Manages the Dataset objects.
 *
 * Need to rethink return type - using null to mean many things
 *
 *  @author caron
  * @since 1/23/2015
 */
@Component
public class DatasetManager implements InitializingBean  {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetManager.class);
  static private final boolean debugResourceControl = false;

  @Autowired
  DebugCommands debugCommands;

  @Autowired
  DataRootManager dataRootManager;

  @Autowired
  FeatureCollectionCache featureCollectionCache;

  @Autowired
  Authorizer restrictedDatasetAuthorizer;

  // InvDataset (not DatasetScan, DatasetFmrc) that have an NcML element in it. key is the request Path
  private Map<String, Dataset> ncmlDatasetHash = new HashMap<>();

  // list of dataset sources. note we have to search this each call to getNetcdfFile - most requests (!)
  // possible change to one global hash table request
  private ArrayList<DatasetSource> sourceList = new ArrayList<>();

  // resource control
  private HashMap<String, String> resourceControlHash = new HashMap<>(); // path, restrictAccess string for datasets
  private volatile PathMatcher<String> resourceControlMatcher = new PathMatcher<>(); // path, restrictAccess string for datasetScan
  private boolean hasResourceControl = false;

  @Override
  public void afterPropertiesSet() throws Exception {
    TdsRequestedDataset.setDatasetManager( this);      // LOOK why not autowire this ?  maybe because staatic ??
    makeDebugActions();
  }

  void reinit() {
    ncmlDatasetHash = new HashMap<>();
    resourceControlHash = new HashMap<>();
    resourceControlMatcher = new PathMatcher<>();
    sourceList = new ArrayList<>();
    hasResourceControl = false;
  }

  void makeDebugActions() {
    DebugCommands.Category debugHandler = debugCommands.findCategory("catalogs");
    DebugCommands.Action act;

    act = new DebugCommands.Action("showNcml", "Show ncml datasets") {
      public void doAction(DebugCommands.Event e) {
        for (Object key : ncmlDatasetHash.keySet()) {
          e.pw.println(" url=" + key);
        }
      }
    };
    debugHandler.addAction(act);
  }

  public void registerDatasetSource(String className) {
    Class vClass;
    try {
      vClass = DatasetManager.class.getClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      log.error("Attempt to load DatasetSource class " + className + " not found");
      return;
    }

    if (!(DatasetSource.class.isAssignableFrom(vClass))) {
      log.error("Attempt to load class " + className + " does not implement " + DatasetSource.class.getName());
      return;
    }

    // create instance of the class
    Object instance;
    try {
      instance = vClass.newInstance();
    } catch (InstantiationException e) {
      log.error("Attempt to load Viewer class " + className + " cannot instantiate, probably need default Constructor.");
      return;
    } catch (IllegalAccessException e) {
      log.error("Attempt to load Viewer class " + className + " is not accessible.");
      return;
    }

    registerDatasetSource((DatasetSource) instance);
  }

  public void registerDatasetSource(DatasetSource v) {
    sourceList.add(v);
    if (debugResourceControl) System.out.println("registerDatasetSource " + v.getClass().getName());
  }

  public String getLocationFromRequestPath(String reqPath) {
    return dataRootManager.getLocationFromRequestPath(reqPath);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile(HttpServletRequest req, HttpServletResponse res) throws IOException {
    return getNetcdfFile(req, res, TdsPathUtils.extractPath(req, null));
  }

  // return null means request has been handled, and calling routine should exit without further processing
  public NetcdfFile getNetcdfFile(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    if (log.isDebugEnabled()) log.debug("DatasetHandler wants " + reqPath);
    if (debugResourceControl) System.out.println("getNetcdfFile = " + ServletUtil.getRequest(req));

    if (reqPath == null)
      return null;

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // see if its under resource control
    if (!resourceControlOk(req, res, reqPath))
      return null;

    // look for a dataset (non scan, non fmrc) that has an ncml element
    Dataset ds = ncmlDatasetHash.get(reqPath);
    if (ds != null) {
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found NcmlDataset= " + ds);
      //String cacheName = ds.getUniqueID(); // LOOK use reqPath !!

      NetcdfFile ncfile = NetcdfDataset.acquireFile(new NcmlFileFactory(ds), null, reqPath, -1, null, null);
      if (ncfile == null) throw new FileNotFoundException(reqPath);
      return ncfile;
    }

    // look for a match
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);

    // look for an feature collection dataset
    if ((match != null) && (match.dataRoot.getFeatureCollection() != null)) {
      FeatureCollectionRef featCollection = match.dataRoot.getFeatureCollection();
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection= " + featCollection);
      InvDatasetFeatureCollection fc = featureCollectionCache.get(featCollection);
      NetcdfFile ncfile = fc.getNetcdfDataset(match.remaining);
      if (ncfile == null) throw new FileNotFoundException(reqPath);
      return ncfile;
    }

    // might be a pluggable DatasetSource: LOOK scalability
    NetcdfFile ncfile = null;
    for (DatasetSource datasetSource : sourceList) {
      if (datasetSource.isMine(req)) {
        ncfile = datasetSource.getNetcdfFile(req, res);
        if (ncfile != null) return ncfile;
      }
    }

    // common case - its a file
    if (match != null) {
      boolean doCache = true; // hack in a "no cache" option
      org.jdom2.Element netcdfElem = null; // find ncml if it exists
      if (match.dataRoot != null) {
        // doCache = match.dataRoot.isCache();  LOOK
        DatasetScan dscan = match.dataRoot.getDatasetScan();
        // if (dscan == null) dscan = match.dataRoot.getDatasetRootProxy();  // no ncml possible in getDatasetRootProxy
        if (dscan != null)
          netcdfElem = dscan.getNcmlElement();
      }

      String location = dataRootManager.getLocationFromRequestPath(reqPath);
      if (location == null)
        throw new FileNotFoundException(reqPath);

      // if theres an ncml element, open it directly through NcMLReader, therefore not being cached.
      // this is safer given all the trouble we have with ncml and caching.
      if (netcdfElem != null) {
        String ncmlLocation = "DatasetScan#" + location; // LOOK some descriptive name
        NetcdfDataset ncd = NcMLReader.readNcML(ncmlLocation, netcdfElem, "file:" + location, null);
        //new NcMLReader().readNetcdf(reqPath, ncd, ncd, netcdfElem, null);
        if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found DataRoot NcML = " + ds);
        return ncd;
      }

      if (doCache)
        ncfile = NetcdfDataset.acquireFile(location, null);
      else
        ncfile = NetcdfDataset.openFile(location, null);

      if (ncfile == null) throw new FileNotFoundException(reqPath);

    }

    return ncfile;
  }

  // LOOK convoluted - simplify
  public FeatureDataset getFeatureDataset(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    FeatureType type;
    FeatureDataset fd = null;
    FeatureCollectionRef ftCollection = getFeatureCollection(req, res, reqPath);

    if (ftCollection != null) {
      type = ftCollection.getFeatureCollectionType().getFeatureType();
      assert type != null;

      if (type == FeatureType.GRID || type == FeatureType.FMRC) {
        return openGridDataset(req, res, reqPath);
      }

      if (type.isPointFeatureType()) {
        return openPointDataset(req, res, reqPath);
      }

    } else {

      //Try as file?
      NetcdfFile ncfile = getNetcdfFile(req, res, reqPath);
      if (ncfile != null) {
        //Wrap it into a FeatureDataset
        Set<NetcdfDataset.Enhance> enhance = Collections.unmodifiableSet(EnumSet.of(NetcdfDataset.Enhance.CoordSystems, NetcdfDataset.Enhance.ConvertEnums));
        fd = FeatureDatasetFactoryManager.wrap(
                FeatureType.ANY,                  // will check FeatureType below if needed...
                NetcdfDataset.wrap(ncfile, enhance),
                null,
                new Formatter(System.err));       // better way to do this?
      }
    }

    return fd;

  }

  // return null means request has been handled, and calling routine should exit without further processing
  public FeatureCollectionRef getFeatureCollection(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    if (reqPath == null)
      return null;

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // see if its under resource control
    if (!resourceControlOk(req, res, reqPath))
      return null;

    // look for a feature collection dataset
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);
    if ((match != null) && (match.dataRoot.getFeatureCollection() != null)) {
      return match.dataRoot.getFeatureCollection();
    }

    return null;
  }

  // used only for the case of Dataset (not DatasetScan) that have an NcML element inside.
  // This makes the NcML dataset the target of the server.
  private class NcmlFileFactory implements FileFactory {
    private Dataset ds;

    NcmlFileFactory(Dataset ds) {
      this.ds = ds;
    }

    public NetcdfFile open(String cacheName, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      org.jdom2.Element netcdfElem = ds.getNcmlElement();
      return NcMLReader.readNcML(cacheName, netcdfElem, cancelTask);
    }
  }

  public InvDatasetFeatureCollection openFeatureCollection(FeatureCollectionRef ftCollection) throws IOException {
      return featureCollectionCache.get(ftCollection);
  }


  /**
   * Open a file as a GridDataset, using getNetcdfFile(), so that it gets wrapped in NcML if needed.
   *
   * @param req     the request
   * @param res     the response
   * @param reqPath the request path
   * @return GridDataset
   * @throws IOException on read error
   */
  public GridDataset openGridDataset(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    // first look for a grid feature collection
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);
    if ((match != null) && (match.dataRoot.getFeatureCollection() != null)) {
      FeatureCollectionRef featCollection = match.dataRoot.getFeatureCollection();
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection= " + featCollection);

      InvDatasetFeatureCollection fc = featureCollectionCache.get(featCollection);
      GridDataset gds = fc.getGridDataset(match.remaining);
      if (gds == null) throw new FileNotFoundException(reqPath);
      return gds;
    }

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = getNetcdfFile(req, res, reqPath);
    if (ncfile == null) return null;

    NetcdfDataset ncd = null;
    try {
      // Convert to NetcdfDataset
      ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getDefaultEnhanceMode());
      return new ucar.nc2.dt.grid.GridDataset(ncd);


    } catch (Throwable t) {
      if (ncd == null)
        ncfile.close();
      else
        ncd.close();

      if (t instanceof IOException)
        throw (IOException) t;

      String msg = ncd == null ? "Problem wrapping NetcdfFile in NetcdfDataset"
              : "Problem creating GridDataset from NetcdfDataset";
      log.error("openGridDataset(): " + msg, t);
      throw new IOException(msg + t.getMessage());
    }
  }

  /**
   * Open a file as a GridDataset, using getNetcdfFile(), so that it gets wrapped in NcML if needed.
   *
   * @param req     the request
   * @param res     the response
   * @param reqPath the request path
   * @return GridDataset
   * @throws IOException on read error
   */
  public FeatureDataset openPointDataset(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    // first look for a feature collection
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);
    if ((match != null) && (match.dataRoot.getFeatureCollection() != null)) {
      FeatureCollectionRef featCollection = match.dataRoot.getFeatureCollection();
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection= " + featCollection);

      InvDatasetFeatureCollection fc = featureCollectionCache.get(featCollection);
      return fc.getFeatureDataset();
    }

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = getNetcdfFile(req, res, reqPath);
    if (ncfile == null) return null;

    Formatter errlog = new Formatter();
    NetcdfDataset ncd = null;
    try {
      ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getDefaultEnhanceMode());
      return FeatureDatasetFactoryManager.wrap(FeatureType.ANY_POINT, ncd, null, errlog);

    } catch (Throwable t) {
      if (ncd == null)
        ncfile.close();
      else
        ncd.close();

      if (t instanceof IOException)
        throw (IOException) t;

      String msg = ncd == null ? "Problem wrapping NetcdfFile in NetcdfDataset; " : "Problem calling FeatureDatasetFactoryManager; ";
      msg += errlog.toString();
      log.error("openGridDataset(): " + msg, t);
      throw new IOException(msg + t.getMessage());
    }
  }

  /**
   * Find the longest match for this path.
   *
   * @param path the complete path name of the dataset
   * @return ResourceControl for this dataset, or null if none
   */
  public String findResourceControl(String path) {
    if (!hasResourceControl) return null;

    if (path.startsWith("/"))
      path = path.substring(1);

    String rc = resourceControlHash.get(path);
    if (null == rc)
      rc = resourceControlMatcher.match(path);

    return rc;
  }

  /**
   * Check if this is making a request for a restricted dataset, and if so, if its allowed.
   *
   * @param req     the request
   * @param res     the response
   * @param reqPath the request path; if null, use req.getPathInfo()
   * @return true if ok to proceed. If false, the appropriate error or redirect message has been sent, the caller only needs to return.
   * @throws IOException on read error
   */
  public boolean resourceControlOk(HttpServletRequest req, HttpServletResponse res, String reqPath) { // throws IOException {
    if (null == reqPath)
      reqPath = TdsPathUtils.extractPath(req, null);

    // see if its under resource control
    String rc = findResourceControl(reqPath);
    if (rc != null) {
      if (debugResourceControl) System.out.println("DatasetHandler request has resource control =" + rc + "\n"
              + ServletUtil.showRequestHeaders(req) + ServletUtil.showSecurity(req, rc));

      try {
        if (!restrictedDatasetAuthorizer.authorize(req, res, rc)) {
          return false;
        }
      } catch (Exception e) {
        throw new RuntimeException(e.getMessage());
      }

      if (debugResourceControl) System.out.println("ResourceControl granted = " + rc);
    }

    return true;
  }

  /**
   * This tracks Dataset elements that have resource control attributes
   *
   * @param ds the dataset
   */
  void putResourceControl(Dataset ds) {
    if (log.isDebugEnabled()) log.debug("putResourceControl " + ds.getRestrictAccess() + " for " + ds.getName());

    // resourceControl is inherited, but no guarentee that children paths are related, unless its a
    //   DatasetScan or InvDatasetFmrc. So we keep track of all datasets that have a ResourceControl, including children
    // DatasetScan and InvDatasetFmrc must use a PathMatcher, others can use exact match (hash)

    if (ds instanceof DatasetScan) {
      DatasetScan scan = (DatasetScan) ds;
      if (debugResourceControl)
        System.out.println("putResourceControl " + ds.getRestrictAccess() + " for datasetScan " + scan.getPath());
      resourceControlMatcher.put(scan.getPath(), ds.getRestrictAccess());

    } else { // dataset
      if (debugResourceControl)
        System.out.println("putResourceControl " + ds.getRestrictAccess() + " for dataset " + ds.getUrlPath());

      // LOOK: seems like you only need to add if InvAccess.InvService.isReletive
      // LOOK: seems like we should use resourceControlMatcher to make sure we match .dods, etc
      for (Access access : ds.getAccess()) {
        if (access.getService().isRelativeBase())
          resourceControlHash.put(access.getUrlPath(), ds.getRestrictAccess());
      }

    }

    hasResourceControl = true;
  }

  /**
   * This tracks Dataset elements that have embedded NcML
   *
   * @param path the req.getPathInfo() of the dataset.
   * @param ds   the dataset
   */
  void putNcmlDataset(String path, Dataset ds) {
    if (log.isDebugEnabled()) log.debug("putNcmlDataset " + path + " for " + ds.getName());
    ncmlDatasetHash.put(path, ds);
  }

}
