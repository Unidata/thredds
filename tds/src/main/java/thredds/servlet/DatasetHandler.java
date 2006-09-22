package thredds.servlet;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileFactory;
import ucar.nc2.NetcdfFileCache;
import ucar.nc2.util.CancelTask;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDatasetFmrc;
import org.jdom.Element;


/**
 * CDM Datasets.
 *   1) if dataset with ncml, open that
 *   2) if datasetScan with ncml, wrap
 */
public class DatasetHandler {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetHandler.class);
  static HashMap ncmlDatasetHash = new HashMap(); // Map<path, InvDatasetImpl>, for Dataset only (not DatasetScan)

  static public void reinit() {
    ncmlDatasetHash = new HashMap();
  }

  public static void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("catalogs");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showNcml", "Show ncml datasets") {
      public void doAction(DebugHandler.Event e) {
        Iterator iter = ncmlDatasetHash.keySet().iterator();
        while (iter.hasNext()) {
          String key = (String) iter.next();
          e.pw.println(" url=" + key);
        }
      }
    };
    debugHandler.addAction( act);
  }

  static public NetcdfFile getNetcdfFile( String  reqPath) throws IOException {
    if (log.isDebugEnabled()) log.debug("DatasetHandler wants "+reqPath);

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // look for a dataset that has an ncml element
    InvDatasetImpl ds = (InvDatasetImpl) ncmlDatasetHash.get(reqPath);
    if (ds != null) {
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found NcmlDataset= "+ds);
      String cacheName = ds.getUniqueID();
      return NetcdfFileCache.acquire(cacheName, -1, null, null, new NcmlFileFactory(ds));
    }

    // look for an fmrc dataset
    DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch( reqPath);
    if ((match != null) && (match.dataRoot.fmrc != null)) {
      InvDatasetFmrc fmrc = match.dataRoot.fmrc;
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found InvDatasetFmrc= "+fmrc);
      return fmrc.getDataset( match.remaining);
    }

    // otherwise, must have a datasetRoot in the path
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile( reqPath);
    if (file == null)
      return null;

    // acquire it
    NetcdfFile ncfile = NetcdfDataset.acquireFile(file.getPath(), null);

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

  static public GridDataset openGridDataset(String reqPath) throws IOException {

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = getNetcdfFile(reqPath);
    if (ncfile == null) throw new FileNotFoundException(reqPath);

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
   * This tracks Dataset elements that have embedded NcML
   * @param path the req.getPathInfo() of the dataset.
   * @param ds the dataset
   */
  static void putNcmlDataset( String path, InvDatasetImpl ds) {
    if (log.isDebugEnabled()) log.debug("putNcmlDataset "+path+" for "+ds.getName());
    ncmlDatasetHash.put( path, ds);
  }

  // used only for the case of Dataset (not DatasetScan) that have an NcML element inside.
  // This makes the NcML dataset the target of the server.
  static private class NcmlFileFactory implements NetcdfFileFactory {
    private InvDatasetImpl ds;
    NcmlFileFactory( InvDatasetImpl ds) { this.ds = ds; }

    public NetcdfFile open(String cacheName, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      org.jdom.Element netcdfElem = ds.getNcmlElement();

      /*
      NetcdfDataset ncd = new NetcdfDataset();
      ncd.setCacheName(cacheName);

      // transfer the ncml into the dataset
      new NcMLReader().readNetcdf( null, ncd, ncd, netcdfElem, null);  */

      return NcMLReader.readNcML(netcdfElem, cancelTask);
    }
  }


}
