/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ncml4.NcMLReader;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileFactory;
import ucar.nc2.NetcdfFileCache;

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;

import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDatasetFmrc;
import thredds.catalog.InvDatasetScan;
import thredds.util.PathMatcher;
import thredds.servlet.restrict.RestrictedDatasetServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;


/**
 * CDM Datasets.
 * 1) if dataset with ncml, open that
 * 2) if datasetScan with ncml, wrap
 */
public class DatasetHandler {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetHandler.class);

  // InvDataset (not DatasetScan, DatasetFmrc) that have an NcML element in it. key is the request Path
  static private HashMap<String, InvDatasetImpl> ncmlDatasetHash = new HashMap<String, InvDatasetImpl>();

  static private ArrayList<DatasetSource> sourceList = new ArrayList<DatasetSource>();

  // resource control
  static private HashMap<String, String> resourceControlHash = new HashMap<String, String>(); // path, restrictAccess string for datasets
  static private volatile PathMatcher resourceControlMatcher = new PathMatcher(); // path, restrictAccess string for datasetScan
  static private boolean hasResourceControl = false;
  static private boolean debugResourceControl = false;

  static void reinit() {
    ncmlDatasetHash = new HashMap<String, InvDatasetImpl>();
    resourceControlHash = new HashMap<String, String>();
    resourceControlMatcher = new PathMatcher();
    sourceList = new ArrayList<DatasetSource>();

    hasResourceControl = false;
  }

  public static void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("catalogs");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showNcml", "Show ncml datasets") {
      public void doAction(DebugHandler.Event e) {
        for (Object key : ncmlDatasetHash.keySet()) {
          e.pw.println(" url=" + key);
        }
      }
    };
    debugHandler.addAction(act);
  }

  static public void registerDatasetSource(String className) {
    Class vClass;
    try {
      vClass = DatasetHandler.class.getClassLoader().loadClass(className);
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

  static public void registerDatasetSource(DatasetSource v) {
    sourceList.add(v);
    System.out.println("registerDatasetSource "+ v.getClass().getName());
  }

  static public NetcdfFile getNetcdfFile(HttpServletRequest req, HttpServletResponse res) throws IOException {
    return getNetcdfFile( req, res, req.getPathInfo());
  }

  static public NetcdfFile getNetcdfFile(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    if (log.isDebugEnabled()) log.debug("DatasetHandler wants " + reqPath);
    if (debugResourceControl) System.out.println("getNetcdfFile = " + ServletUtil.getRequest(req));

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // see if its under resource control
    if (!resourceControlOk( req, res, reqPath))
      return null;

    // look for a dataset (non scan, non fmrc) that has an ncml element
    InvDatasetImpl ds = ncmlDatasetHash.get(reqPath);
    if (ds != null) {
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found NcmlDataset= " + ds);
      //String cacheName = ds.getUniqueID(); // LOOK use reqPath !!
      NetcdfFile ncfile = NetcdfFileCache.acquire(reqPath, -1, null, null, new NcmlFileFactory(ds));
      if (ncfile == null) throw new FileNotFoundException(reqPath);
      return ncfile;
    }

    // look for an fmrc dataset
    DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);
    if ((match != null) && (match.dataRoot.fmrc != null)) {
      InvDatasetFmrc fmrc = match.dataRoot.fmrc;
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found InvDatasetFmrc= " + fmrc);
      NetcdfFile ncfile = fmrc.getDataset(match.remaining);
      if (ncfile == null) throw new FileNotFoundException(reqPath);
      return ncfile;
    }

    // might be a pluggable DatasetSource
    NetcdfFile ncfile = null;
    for (DatasetSource datasetSource : sourceList) {
      if (datasetSource.isMine(req)) {
        ncfile = datasetSource.getNetcdfFile(req, res);
        if (ncfile == null) return null;
      }
    }

    // common case - its a file
    if (ncfile == null) {
      // otherwise, must have a datasetRoot in the path
      File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(reqPath);
      if (file == null) {
        throw new FileNotFoundException(reqPath);
      }

      ncfile = NetcdfDataset.acquireFile(file.getPath(), null);
      if (ncfile == null) throw new FileNotFoundException(reqPath);
    }

    // wrap with ncml if needed : for DatasetScan only
    org.jdom.Element netcdfElem = DataRootHandler.getInstance().getNcML(reqPath);
    if (netcdfElem != null) {
      NetcdfDataset ncd = new NetcdfDataset(ncfile, false); // do not enhance !!
      new NcMLReader().readNetcdf(reqPath, ncd, ncd, netcdfElem, null);
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found DataRoot NcML = " + ds);
      return ncd;
    }

    return ncfile;
  }


  // used only for the case of Dataset (not DatasetScan) that have an NcML element inside.
  // This makes the NcML dataset the target of the server.
  static private class NcmlFileFactory implements NetcdfFileFactory {
    private InvDatasetImpl ds;

    NcmlFileFactory(InvDatasetImpl ds) {
      this.ds = ds;
    }

    public NetcdfFile open(String cacheName, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      org.jdom.Element netcdfElem = ds.getNcmlElement();
      return NcMLReader.readNcML(cacheName, netcdfElem, cancelTask);
    }
  }

  static public GridDataset openGridDataset(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = getNetcdfFile(req, res, reqPath);
    if (ncfile == null) return null;

    // convert to NetcdfDataset with enhance
    NetcdfDataset ncd;
    if (ncfile instanceof NetcdfDataset) {
      ncd = (NetcdfDataset) ncfile;
      if (ncd.getEnhanceMode() == NetcdfDataset.EnhanceMode.None)
        ncd.enhance();
    } else {
      ncd = new NetcdfDataset(ncfile, true);
    }

    // convert to a GridDataset
    return new ucar.nc2.dt.grid.GridDataset(ncd);
  }


  /**
   * Find the longest match for this path.
   *
   * @param path the complete path name of the dataset
   * @return ResourceControl for this dataset, or null if none
   */
  static private String findResourceControl(String path) {
    if (!hasResourceControl) return null;

    if (path.startsWith("/"))
      path = path.substring(1);

    String rc = resourceControlHash.get(path);
    if (null == rc)
      rc = (String) resourceControlMatcher.match(path);

    return rc;
  }

  /**
   * Check if this is making a request for a restricted dataset, and if so, if its allowed.
   *
   * @param req the request
   * @param res the response
   * @param reqPath  the request path; if null, use req.getPathInfo()
   *
   * @return true if ok to proceed. If false, the apppropriate error or redirect message has been sent, the caller only needs to return.
   * @throws IOException on read error
   */
  static public boolean resourceControlOk(HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    if (null == reqPath)
      reqPath = req.getPathInfo();

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // see if its under resource control
    String rc = findResourceControl(reqPath);
    if (rc != null) {
      if (debugResourceControl) System.out.println("DatasetHandler request has resource control =" + rc + "\n"
              + ServletUtil.showRequestHeaders(req) + ServletUtil.showSecurity(req, rc));

      try {
        if (!RestrictedDatasetServlet.authorize(req, res, rc)) {
          return false;
        }
      } catch (ServletException e) {
        throw new IOException(e.getMessage());
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
  static void putResourceControl(InvDatasetImpl ds) {
    if (log.isDebugEnabled()) log.debug("putResourceControl " + ds.getRestrictAccess() + " for " + ds.getName());

    // resourceControl is inherited, but no guarentee that children paths are related, unless its a
    //   InvDatasetScan or InvDatasetFmrc. So we keep track of all datasets that have a ResourceControl, including children
    // InvDatasetScan and InvDatasetFmrc must use a PathMatcher, others can use exact match (hash)

    if (ds instanceof InvDatasetScan) {
      InvDatasetScan scan = (InvDatasetScan) ds;
      if (debugResourceControl)
        System.out.println("putResourceControl " + ds.getRestrictAccess() + " for datasetScan " + scan.getPath());
      resourceControlMatcher.put(scan.getPath(), ds.getRestrictAccess());
    } else if (ds instanceof InvDatasetFmrc) {
      InvDatasetFmrc fmrc = (InvDatasetFmrc) ds;
      if (debugResourceControl)
        System.out.println("putResourceControl " + ds.getRestrictAccess() + " for datasetFmrc " + fmrc.getPath());
      resourceControlMatcher.put(fmrc.getPath(), ds.getRestrictAccess());
    } else { // dataset
      if (debugResourceControl)
        System.out.println("putResourceControl " + ds.getRestrictAccess() + " for dataset " + ds.getUrlPath());
      resourceControlHash.put(ds.getUrlPath(), ds.getRestrictAccess());
    }

    hasResourceControl = true;
  }

  /**
   * This tracks Dataset elements that have embedded NcML
   *
   * @param path the req.getPathInfo() of the dataset.
   * @param ds   the dataset
   */
  static void putNcmlDataset(String path, InvDatasetImpl ds) {
    if (log.isDebugEnabled()) log.debug("putNcmlDataset " + path + " for " + ds.getName());
    ncmlDatasetHash.put(path, ds);
  }

}
