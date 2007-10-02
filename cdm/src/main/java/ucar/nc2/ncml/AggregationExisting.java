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

package ucar.nc2.ncml;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.jdom.Element;

/**
 * JoinExisting Aggregation.
 *
 * @author caron
 */
public class AggregationExisting extends Aggregation {

  public AggregationExisting(NetcdfDataset ncd, String dimName, String recheckS) {
    super( ncd, dimName, Aggregation.Type.JOIN_EXISTING, recheckS);
  }

  // for AggregationExistingOne
  protected AggregationExisting(NetcdfDataset ncd, String dimName, Aggregation.Type type, String recheckS) {
    super( ncd, dimName, type, recheckS);
  }

  protected void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical =  typicalDataset.acquireFile(null);
    DatasetConstructor.transferDataset(typical, ncDataset, isNew ? null : new MyReplaceVariableCheck());

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension(dimName, getTotalCoords());
    ncDataset.removeDimension(null, dimName); // remove previous declaration, if any
    ncDataset.addDimension(null, aggDim);

    // now we can create the real aggExisting variables
    // all variables with the named aggregation dimension
    for (Variable v : typical.getVariables()) {
      if (v.getRank() < 1)
        continue;
      Dimension d = v.getDimension(0);
      if (!dimName.equals(d.getName()))
        continue;

      VariableDS vagg = new VariableDS(ncDataset, null, null, v.getShortName(), v.getDataType(),
          v.getDimensionsString(), null, null);
      vagg.setProxyReader(this);
      DatasetConstructor.transferVariableAttributes(v, vagg);

      ncDataset.removeVariable(null, v.getShortName());
      ncDataset.addVariable(null, vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    ncDataset.finish();
    makeProxies(typicalDataset, ncDataset);
    typical.close();
  }
  
  /**
   * Persist info (nccords, coorValues) from joinExisting, since that can be expensive to recreate.
   * @throws IOException
   */
  public void persist() throws IOException {
    if (diskCache2 == null)
      return;

    FileChannel channel = null;
    try {
      String cacheName = getCacheName();
      if (cacheName == null) return;

      File cacheFile = diskCache2.getCacheFile(cacheName);
      boolean exists = cacheFile.exists();
      if (!exists) {
        File dir = cacheFile.getParentFile();
        dir.mkdirs();
      }

      // only write out if something changed after the cache file was last written
      if (!wasChanged)
        return;

      // Get a file channel for the file
      FileOutputStream fos = new FileOutputStream(cacheFile);
      channel = fos.getChannel();

      // Try acquiring the lock without blocking. This method returns
      // null or throws an exception if the file is already locked.
      FileLock lock;
      try {
        lock = channel.tryLock();
      } catch (OverlappingFileLockException e) {
        // File is already locked in this thread or virtual machine
        return; // give up
      }
      if (lock == null) return;

      PrintStream out = new PrintStream(fos);
      out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
      out.print("<aggregation xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' ");
      out.print("type='" + type + "' ");
      if (dimName != null)
        out.print("dimName='" + dimName + "' ");
      if (recheck != null)
        out.print("recheckEvery='" + recheck + "' ");
      out.print(">\n");

      for (Dataset dataset : nestedDatasets) {
        out.print("  <netcdf location='" + dataset.getLocation() + "' ");
        out.print("ncoords='" + dataset.getNcoords(null) + "' ");

        if (dataset.coordValue != null)
          out.print("coordValue='" + dataset.coordValue + "' ");

        out.print("/>\n");
      }

      out.print("</aggregation>\n");
      out.close(); // this also closes the  channel and releases the lock

      cacheFile.setLastModified(lastChecked);
      wasChanged = false;

      if (debug)
        System.out.println("Aggregation persisted = " + cacheFile.getPath() + " lastModified= " + new Date(lastChecked));

    } finally {
      if (channel != null)
        channel.close();
    }
  }

  // read info from the persistent XML file, if it exists
  protected void persistRead() {
    String cacheName = getCacheName();
    if (cacheName == null) return;

    File cacheFile = diskCache2.getCacheFile(cacheName);
    if (!cacheFile.exists())
      return;

    if (debug) System.out.println(" *Read cache " + cacheFile.getPath());

    Element aggElem;
    try {
      aggElem = ucar.nc2.util.xml.Parse.readRootElement(cacheFile.getPath());
    } catch (IOException e) {
      return;
    }

    List ncList = aggElem.getChildren("netcdf", NcMLReader.ncNS);
    for (int j = 0; j < ncList.size(); j++) {
      Element netcdfElemNested = (Element) ncList.get(j);
      String location = netcdfElemNested.getAttributeValue("location");

      Dataset ds = findDataset(location);
      if ((null != ds) && (ds.ncoord == 0)) {
        if (debugCacheDetail) System.out.println("  use cache for " + location);
        String ncoordsS = netcdfElemNested.getAttributeValue("ncoords");
        try {
          ds.ncoord = Integer.parseInt(ncoordsS);
        } catch (NumberFormatException e) {
          logger.error("bad ncoord attribute on dataset=" + location);
        }

        String coordValue = netcdfElemNested.getAttributeValue("coordValue");
        if (coordValue != null) {
          ds.coordValue = coordValue;
        }

      }
    }

  }

    // find a dataset in the nestedDatasets by location
  private Dataset findDataset(String location) {
      for (Dataset ds : nestedDatasets) {
        if (location.equals(ds.getLocation()))
          return ds;
      }
      return null;
  }

  // name to use in the DiskCache2 for the persistent XML info.
  // Document root is aggregation
  // has the name getCacheName()
  private String getCacheName() {
    String cacheName = ncDataset.getLocation();
    if (cacheName == null) cacheName = ncDataset.getCacheName();
    return cacheName;
  }

}
