/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package thredds.core;

import com.coverity.security.Escape;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import thredds.featurecollection.FeatureCollectionCache;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.server.admin.DebugCommands;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.catalog.tracker.DatasetTracker;
import thredds.servlet.DatasetSource;
import thredds.servlet.ServletUtil;
import thredds.servlet.restrict.Authorizer;
import thredds.util.TdsPathUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.simpgeometry.SimpleGeometryFeatureDataset;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.Optional;
import ucar.nc2.util.cache.FileFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Formatter;

/**
 * Provides an API to obtain the various Dataset objects, given the request Path.
 * <p>
 * Need to rethink return type - using null to mean many things
 *
 * @author caron
 * @since 1/23/2015
 *
 * This needs revision to separate out the url path stuff from the more general
 * file stuff.
 */
@Component
public class DatasetManager implements InitializingBean {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetManager.class);
  static final boolean debugResourceControl = false;

  @Autowired
  private DataRootManager dataRootManager;

  @Autowired
  private FeatureCollectionCache featureCollectionCache;

  @Autowired
  @Qualifier("restrictedDatasetAuthorizer")
  private Authorizer restrictedDatasetAuthorizer;

  @Autowired
  private DebugCommands debugCommands;

  // injected by catalogInitializer, when catalogs are reread
  private DatasetTracker datasetTracker;

  // list of dataset sources. note we have to search this each call to getNetcdfFile - most requests (!)
  // possible change to one global hash table request
  private ArrayList<DatasetSource> datasetSources = new ArrayList<>();

  @Override
  public void afterPropertiesSet() throws Exception {
    TdsRequestedDataset.setDatasetManager(this);      // LOOK why not autowire this ?  maybe because static ??
    makeDebugActions();
  }

  public void setDatasetTracker(DatasetTracker datasetTracker) {
    if (this.datasetTracker != null)
      try {
        this.datasetTracker.close();
      } catch (IOException e) {
        log.error("Cant close datasetTracker ", e);
      }

    this.datasetTracker = datasetTracker;
  }

  public DatasetManager() {
  }

  public String getLocationFromRequestPath(String reqPath) {
    return dataRootManager.getLocationFromRequestPath(reqPath);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////


  // used only for the case of Dataset (not DatasetScan) that have an NcML element inside.
  // This makes the NcML dataset the target of the server.
  private static class NcmlFileFactory implements FileFactory {
    private String ncml;

    NcmlFileFactory(String ncml) {
      this.ncml = ncml;
    }

    public NetcdfFile open(DatasetUrl durl, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      return NcMLReader.readNcML(new StringReader(ncml), durl.trueurl, cancelTask);
    }
  }

  // return null means request has been handled, and calling routine should exit without further processing
  public NetcdfFile openNetcdfFile(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    if (log.isDebugEnabled()) log.debug("DatasetHandler wants " + reqPath);

    if (reqPath == null)
      return null;

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // see if its under resource control
    if (!resourceControlOk(req, res, reqPath))
      return null;

    // HEY LOOK datascan below has its own Ncml
    // look for a dataset (non scan, non fmrc) that has an ncml element
    String ncml = datasetTracker.findNcml(reqPath);
    if (ncml != null) {
      NetcdfFile ncfile = NetcdfDataset.acquireFile(new NcmlFileFactory(ncml), null, DatasetUrl.findDatasetUrl(reqPath), -1, null, null);
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

    // might be a pluggable DatasetSource:
    NetcdfFile ncfile = null;
    for (DatasetSource datasetSource : datasetSources) {   // LOOK linear
      if (datasetSource.isMine(req)) {
        ncfile = datasetSource.getNetcdfFile(req, res);
        if (ncfile != null) return ncfile;
      }
    }

    // common case - its a file
    if (match != null) {
      org.jdom2.Element netcdfElem = null; // find ncml if it exists
      if (match.dataRoot != null) {
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
        //if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found DataRoot NcML = " + ds);
        return ncd;
      }

      DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
      ncfile = NetcdfDataset.acquireFile(durl, null);
    }

    if (ncfile == null) throw new FileNotFoundException(reqPath);
    return ncfile;
  }

  /**
   * Open a file as a GridDataset, using getNetcdfFile(), so that it gets wrapped in NcML if needed.
   */
  // return null means request has been handled, and calling routine should exit without further processing
  public GridDataset openGridDataset(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    // first look for a feature collection
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);
    if ((match != null) && (match.dataRoot.getFeatureCollection() != null)) {
      // see if its under resource control
      if (!resourceAuthorized(req, res, match.dataRoot.getRestrict()))
        return null;

      FeatureCollectionRef featCollection = match.dataRoot.getFeatureCollection();
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection= " + featCollection);

      InvDatasetFeatureCollection fc = featureCollectionCache.get(featCollection);
      GridDataset gds = fc.getGridDataset(match.remaining);
      if (gds == null) throw new FileNotFoundException(reqPath);
      return gds;
    }

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = openNetcdfFile(req, res, reqPath);
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

      String msg = ncd == null ? "Problem wrapping NetcdfFile in NetcdfDataset" : "Problem creating GridDataset from NetcdfDataset";
      log.error("openGridDataset(): " + msg, t);
      throw new IOException(msg + t.getMessage());
    }
  }

  // return null means request has been handled, and calling routine should exit without further processing
  public FeatureDatasetPoint openPointDataset(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    // first look for a feature collection
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);
    if ((match != null) && (match.dataRoot.getFeatureCollection() != null)) {
      // see if its under resource control
      if (!resourceAuthorized(req, res, match.dataRoot.getRestrict()))
        return null;

      FeatureCollectionRef featCollection = match.dataRoot.getFeatureCollection();
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection= " + featCollection);

      InvDatasetFeatureCollection fc = featureCollectionCache.get(featCollection);
      FeatureDatasetPoint fd = fc.getPointDataset(match.remaining);
      if (fd == null) throw new IllegalArgumentException("Not a Point Dataset " + fc.getName());
      return fd;
    }

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = openNetcdfFile(req, res, reqPath);
    if (ncfile == null) return null;

    Formatter errlog = new Formatter();
    NetcdfDataset ncd = null;
    try {
      ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getDefaultEnhanceMode());
      return (FeatureDatasetPoint) FeatureDatasetFactoryManager.wrap(FeatureType.ANY_POINT, ncd, null, errlog);

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

  // return null means request has been handled, and calling routine should exit without further processing
  public CoverageCollection openCoverageDataset(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    if (reqPath == null)
      return null;

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // see if its under resource control
    if (!resourceControlOk(req, res, reqPath))
      return null;

    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);

    // first look for a feature collection
    if ((match != null) && (match.dataRoot.getFeatureCollection() != null)) {
      FeatureCollectionRef featCollection = match.dataRoot.getFeatureCollection();
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection= " + featCollection);

      InvDatasetFeatureCollection fc = featureCollectionCache.get(featCollection);
      CoverageCollection gds = fc.getGridCoverage(match.remaining);
      if (gds == null) throw new FileNotFoundException(reqPath);
      return gds;
    }

    // otherwise assume its a local file

    // try to open as a FeatureDatasetCoverage. This allows GRIB to be handle specially
    String location = getLocationFromRequestPath(reqPath);
    if (location != null) {
      Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(location);
      if (!opt.isPresent())
        throw new FileNotFoundException("Not a Grid Dataset " + reqPath + " err=" + opt.getErrorMessage());

      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection from file= " + location);
      return opt.get().getSingleCoverageCollection(); // LOOK doesnt have to be single, then what is the URL?

    }

    // if ncml, must handle special, otherwise we're out of options for opening
    // a coverage collection.

    String ncml = datasetTracker.findNcml(reqPath);
    if (ncml != null) {
      Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openNcmlString(ncml);
      if (!opt.isPresent())
        throw new FileNotFoundException("NcML is not a Grid Dataset " + reqPath + " err=" + opt.getErrorMessage());

      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection from NcML");
      return opt.get().getSingleCoverageCollection();
    }

    return null;
  }
  public SimpleGeometryFeatureDataset openSimpleGeometryDataset(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    // first look for a feature collection
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);
    if ((match != null) && (match.dataRoot.getFeatureCollection() != null)) {
      // see if its under resource control
      if (!resourceAuthorized(req, res, match.dataRoot.getRestrict()))
        return null;

      FeatureCollectionRef featCollection = match.dataRoot.getFeatureCollection();
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found FeatureCollection= " + featCollection);

      InvDatasetFeatureCollection fc = featureCollectionCache.get(featCollection);
      SimpleGeometryFeatureDataset fd = fc.getSimpleGeometryDataset(match.remaining);
      if (fd == null) throw new IllegalArgumentException("Not a Simple Geometry Dataset " + fc.getName());
      return fd;
    }

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = openNetcdfFile(req, res, reqPath);
    if (ncfile == null) return null;

    Formatter errlog = new Formatter();
    NetcdfDataset ncd = null;
    try {
      ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getDefaultEnhanceMode());
      return (SimpleGeometryFeatureDataset) FeatureDatasetFactoryManager.wrap(FeatureType.SIMPLE_GEOMETRY, ncd, null, errlog);

    } catch (Throwable t) {
      if (ncd == null)
        ncfile.close();
      else
        ncd.close();

      if (t instanceof IOException)
        throw (IOException) t;

      String msg = ncd == null ? "Problem wrapping NetcdfFile in NetcdfDataset; " : "Problem calling FeatureDatasetFactoryManager; ";
      msg += errlog.toString();
      log.error("openSimpleGeometryDataset(): " + msg, t);
      throw new IOException(msg + t.getMessage());
    }
  }
  /////////////////////////////////////////////////////////////////
  // Resource control

  /**
   * Check if this is making a request for a restricted dataset, and if so, if its allowed.
   *
   * @param req     the request
   * @param res     the response
   * @param reqPath the request path; if null, use req.getPathInfo()
   * @return true if ok to proceed. If false, the appropriate error or redirect message has been sent, the caller only needs to return.
   */
  public boolean resourceControlOk(HttpServletRequest req, HttpServletResponse res, String reqPath) {
    if (null == reqPath)
      reqPath = TdsPathUtils.extractPath(req, null);

    // see if its under resource control
    String rc = null;
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);
    if (match != null) {
      rc = match.dataRoot.getRestrict(); // datasetScan, featCollection are restricted at the dataRoot
    }

    if (rc == null) {
      rc = datasetTracker.findResourceControl(reqPath); // regular datasets tracked here
    }

    return resourceAuthorized(req, res, rc);
  }

  private boolean resourceAuthorized(HttpServletRequest req, HttpServletResponse res, String rc) {
    if (rc == null) return true;
    if (debugResourceControl) System.out.println("DatasetHandler request has resource control =" + rc + "\n"
            + ServletUtil.showRequestHeaders(req) + ServletUtil.showSecurity(req, rc));

    // Principal p = req.getUserPrincipal(); // debug

    try {
      if (!restrictedDatasetAuthorizer.authorize(req, res, rc)) {
        return false;
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

    if (debugResourceControl) System.out.println("ResourceControl granted = " + rc);

    return true;
  }


  /////////////////////////////////////////////////////////
  // DatasetSource

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
    datasetSources.add(v);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////
  // debugging
  public InvDatasetFeatureCollection openFeatureCollection(FeatureCollectionRef ftCollection) throws IOException {
    return featureCollectionCache.get(ftCollection);
  }

  void makeDebugActions() {
    DebugCommands.Category debugHandler = debugCommands.findCategory("Catalogs");
    DebugCommands.Action act;

    act = new DebugCommands.Action("showDatasetTrackerDB", "Show DatasetTracker database") {
      public void doAction(DebugCommands.Event e) {
        Formatter f = new Formatter();
        datasetTracker.showDB(f);
        e.pw.println(Escape.html(f.toString()));
      }
    };
    debugHandler.addAction(act);

  }

}
