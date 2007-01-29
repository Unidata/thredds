package thredds.servlet;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileFactory;
import ucar.nc2.NetcdfFileCache;

import java.io.*;
import java.util.HashMap;

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
 *   1) if dataset with ncml, open that
 *   2) if datasetScan with ncml, wrap
 */
public class DatasetHandler {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetHandler.class);

  // NcML datasets
  static private HashMap<String, InvDatasetImpl> ncmlDatasetHash = new HashMap<String, InvDatasetImpl>(); // Map<path, InvDatasetImpl>, for Dataset only (not DatasetScan)

  // resource control
  static private HashMap<String, String> resourceControlHash  = new HashMap<String, String>(); // path, restrictAccess string for datasets
  static private volatile PathMatcher resourceControlMatcher = new PathMatcher(); // path, restrictAccess string for datasetScan
  static private boolean hasResourceControl = false;
  static private boolean debugResourceControl = false;

  static void reinit() {
    ncmlDatasetHash = new HashMap<String, InvDatasetImpl>();
    resourceControlHash = new HashMap<String, String>();
    resourceControlMatcher = new PathMatcher();
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
    debugHandler.addAction( act);
  }

  static public NetcdfFile getNetcdfFile( HttpServletRequest req, HttpServletResponse res, String reqPath) throws IOException {
    if (log.isDebugEnabled()) log.debug("DatasetHandler wants "+reqPath);
    if (debugResourceControl) System.out.println("getNetcdfFile = "+ ServletUtil.getRequest(req));

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // see if its under resource control
    String rc = findResourceControl( reqPath);
    if (rc != null) {
      if (debugResourceControl) System.out.println("DatasetHandler request has resource control ="+ rc+"\n"
              +ServletUtil.showRequestHeaders(req)+ ServletUtil.showSecurity(req, rc));

      try {
        if (!RestrictedDatasetServlet.authorize(req, res, rc)) {
          return null;
        }
      } catch (ServletException e) {
        throw new IOException(e.getMessage());
      }

      if (debugResourceControl) System.out.println("ResourceControl granted = "+rc);
    }

    // look for a dataset that has an ncml element
    InvDatasetImpl ds = ncmlDatasetHash.get(reqPath);
    if (ds != null) {
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found NcmlDataset= "+ds);
      //String cacheName = ds.getUniqueID(); // LOOK use reqPath !!
      NetcdfFile ncfile = NetcdfFileCache.acquire(reqPath, -1, null, null, new NcmlFileFactory(ds));
      if (ncfile == null) throw new FileNotFoundException(reqPath);
      return ncfile;
    }

    // look for an fmrc dataset
    DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch( reqPath);
    if ((match != null) && (match.dataRoot.fmrc != null)) {
      InvDatasetFmrc fmrc = match.dataRoot.fmrc;
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found InvDatasetFmrc= "+fmrc);
      NetcdfFile ncfile =  fmrc.getDataset( match.remaining);
      if (ncfile == null) throw new FileNotFoundException(reqPath);
      return ncfile;
    }

    // otherwise, must have a datasetRoot in the path
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile( reqPath);
    if (file == null) {
      throw new FileNotFoundException(reqPath);
    }

    // acquire it
    NetcdfFile ncfile = NetcdfDataset.acquireFile(file.getPath(), null);
    if (ncfile == null) throw new FileNotFoundException(reqPath);

    // wrap with ncml if needed
    org.jdom.Element netcdfElem = DataRootHandler.getInstance().getNcML( reqPath);
    if (netcdfElem != null) {
      NetcdfDataset ncd = new NetcdfDataset( ncfile, false); // do not enhance !!
      new NcMLReader().readNetcdf( reqPath, ncd, ncd, netcdfElem, null);
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found DataRoot NcML = "+ds);
      return ncd;
    }

    return ncfile;
  }


  // used only for the case of Dataset (not DatasetScan) that have an NcML element inside.
  // This makes the NcML dataset the target of the server.
  static private class NcmlFileFactory implements NetcdfFileFactory {
    private InvDatasetImpl ds;
    NcmlFileFactory( InvDatasetImpl ds) { this.ds = ds; }

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
      ncd.enhance();
    } else {
      ncd = new NetcdfDataset( ncfile, true);
    }

    // convert to a GridDataset
    return new ucar.nc2.dt.grid.GridDataset( ncd);
  }


  /**
   * Find the longest match for this path.
   * @param path the complete path name of the dataset
   * @return ResourceControl for this dataset, or null if none
   */
  static public String findResourceControl( String path) {
    if (!hasResourceControl) return null;

    if (path.startsWith("/"))
      path = path.substring(1);

    String rc = resourceControlHash.get(path);
    if (null == rc)
      rc = (String) resourceControlMatcher.match(path);

     return rc;
  }

  /**
   * This tracks Dataset elements that have resource control attributes
   * @param ds   the dataset
   */
  static void putResourceControl(InvDatasetImpl ds) {
    if (log.isDebugEnabled()) log.debug("putResourceControl " + ds.getRestrictAccess() + " for " + ds.getName());

    // resourceControl is inherited, but no guarentee that children paths are related, unless its a
    //   InvDatasetScan or InvDatasetFmrc. So we keep track of all datasets that have a ResourceControl, including children
    // InvDatasetScan and InvDatasetFmrc must use a PathMatcher, others can use exact match (hash)

    if (ds instanceof InvDatasetScan) {
      InvDatasetScan scan = (InvDatasetScan) ds;
      if (debugResourceControl) System.out.println("putResourceControl " + ds.getRestrictAccess() + " for datasetScan " + scan.getPath());
      resourceControlMatcher.put(scan.getPath(), ds.getRestrictAccess());
    } else if (ds instanceof InvDatasetFmrc) {
      InvDatasetFmrc fmrc = (InvDatasetFmrc) ds;
      if (debugResourceControl) System.out.println("putResourceControl " + ds.getRestrictAccess() + " for datasetFmrc " + fmrc.getPath());
      resourceControlMatcher.put(fmrc.getPath(), ds.getRestrictAccess());
    } else { // dataset
      if (debugResourceControl) System.out.println("putResourceControl " + ds.getRestrictAccess() + " for dataset " + ds.getUrlPath());
      resourceControlHash.put(ds.getUrlPath(), ds.getRestrictAccess());
    }

    hasResourceControl = true;
  }

  /**
   * This tracks Dataset elements that have embedded NcML
   * @param path the req.getPathInfo() of the dataset.
   * @param ds the dataset
   */
  static void putNcmlDataset( String path, InvDatasetImpl ds) {
    if (log.isDebugEnabled()) log.debug("putNcmlDataset "+path+" for "+ds.getName());
    ncmlDatasetHash.put( path, ds);
  }

}
