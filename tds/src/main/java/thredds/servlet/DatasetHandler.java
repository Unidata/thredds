package thredds.servlet;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileFactory;
import ucar.nc2.NetcdfFileCache;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

import thredds.catalog.InvDatasetImpl;


/**
 * CDM Datasets.
 *   1) if dataset with ncml, open that
 *   2) if datasetScan with ncml, wrap
 */
public class DatasetHandler {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetHandler.class);
  static HashMap ncmlDatasetHash = new HashMap();

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
      return getNcmlDataset( ds);
    }

    // otherwise, must have a datasetRoot in the path
    String filePath = DataRootHandler2.getInstance().translatePath( reqPath);
    // @todo Should instead use ((CrawlableDatasetFile)catHandler2.findRequestedDataset( path )).getFile();
    if (filePath == null) return null;

    // acquire it
    NetcdfFile ncfile = NetcdfDataset.acquireFile(filePath, null);

    // wrap with ncml if needed
    org.jdom.Element netcdfElem = DataRootHandler2.getInstance().getNcML( reqPath);
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
    return new ucar.nc2.dataset.grid.GridDataset( ncd);
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

  /*
  static void putNcmlDataset( String path, InvDatasetImpl ds) {
    List accessList = ds.getAccess();
    for (int i = 0; i < accessList.size(); i++) {
      InvAccess access = (InvAccess) accessList.get(i);
      String fullPath = access.getUrlPath();
      //String fullPath = access.getUnresolvedUrlName();
      //if (fullPath.startsWith( contextPath + "/")) fullPath = fullPath.substring( contextPath.length() + 1 );
      // System.out.println("putAggregationDataset urlPath= "+ds.getUrlPath()+" fullPath= "+fullPath);
      ncmlDatasetHash.put( fullPath, ds);
    }
  }
 */

  static private NetcdfFile getNcmlDataset( InvDatasetImpl ds) throws IOException {
    String cacheName = ds.getUniqueID();
    return NetcdfFileCache.acquire(cacheName, null, new NcmlFileFactory(ds));
  }

  static private class NcmlFileFactory implements NetcdfFileFactory {
    private InvDatasetImpl ds;
    NcmlFileFactory( InvDatasetImpl ds) { this.ds = ds; }

    public NetcdfFile open(String cacheName, CancelTask cancelTask) throws IOException {
      /* File ncmlFile = DiskCache.getCacheFile(cacheName);

      if (ncmlFile.exists()) {
        log.debug("ncmlFile.exists() file= "+ncmlFile.getPath()+" lastModified= "+new Date(ncmlFile.lastModified()));
        return NetcdfDataset.openDataset( ncmlFile.getPath());
      }  */

      // otherwise, open and write it out
      NetcdfDataset ncd = new NetcdfDataset();
      ncd.setCacheName(cacheName);
      org.jdom.Element netcdfElem = ds.getNcmlElement();

      // transfer the ncml into the dataset
      new NcMLReader().readNetcdf( null, ncd, ncd, netcdfElem, null);

      /* cache the full NcML - this has to read the datasets, so may be slow
      OutputStream out = new BufferedOutputStream( new FileOutputStream( ncmlFile));
      ncd.writeNcML(out, null);
      out.close();

      System.out.println("new ncmlFile file= "+ncmlFile.getPath()+" lastModified= "+new Date(ncmlFile.lastModified())); */

      return ncd;
    }
  }

/*  static private void writeNcML( InvDatasetImpl ds, File file) throws IOException {

    Aggregation agg = ds.getAggregation();

    FileOutputStream fout = new FileOutputStream( file);
    PrintStream out = new PrintStream( fout);
    out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
    out.print("<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' >\n");
    out.print("  <aggregation dimName='"+agg.getDimensionName()+"' type='"+agg.getType()+"' >\n");
    out.print("    <fileScan dirLocation='"+agg.getDirLocation()+"' suffix='.nc' />\n");
    out.print("  </aggregation>\n");
    out.print("</netcdf>\n");
    out.close();
  }      */


}
