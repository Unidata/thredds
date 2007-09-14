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

package ucar.nc2.ncml4;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.units.DateUnit;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.jdom.Element;

/**
 * JoinExisting Aggregation.
 *
 * @author caron
 */
public class AggregationExisting extends AggregationOuterDimension {
  private boolean debugPersist = false, debugPersistDetail = false;

  public AggregationExisting(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Aggregation.Type.JOIN_EXISTING, recheckS);
  }

  protected void buildDataset(CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(null);
    DatasetConstructor.transferDataset(typical, ncDataset, null);

    // LOOK not dealing with groups

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension(dimName, getTotalCoords(), true);
    ncDataset.removeDimension(null, dimName); // remove previous declaration, if any
    ncDataset.addDimension(null, aggDim);

    promoteGlobalAttributes( (DatasetOuterDimension) typicalDataset);

    // now create the agg variables
    // all variables with the named aggregation dimension
    for (Variable v : typical.getVariables()) {
      if (v.getRank() < 1)
        continue;
      Dimension d = v.getDimension(0);
      if (!dimName.equals(d.getName()))
        continue;

      VariableDS vagg = new VariableDS(ncDataset, null, null, v.getShortName(), v.getDataType(),
              v.getDimensionsString(), null, null);
      vagg.setProxyReader(this); // do the reading here
      DatasetConstructor.transferVariableAttributes(v, vagg);

      ncDataset.removeVariable(null, v.getShortName());
      ncDataset.addVariable(null, vagg);
      aggVars.add(vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    VariableDS joinAggCoord = (VariableDS) ncDataset.getRootGroup().findVariable(dimName);
    if (joinAggCoord == null) {
      throw new IllegalArgumentException("No existing coordinate variable for joinExisting on "+getLocation());
    }

    if (type == Type.JOIN_EXISTING_ONE) {
      // replace aggregation coordinate variable
      joinAggCoord.setDataType(DataType.STRING);
      joinAggCoord.getAttributes().clear();
      joinAggCoord.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, "Time"));
      joinAggCoord.addAttribute(new Attribute("long_name", "time coordinate"));
      joinAggCoord.addAttribute(new ucar.nc2.Attribute("standard_name", "time"));
    }

    if (timeUnitsChange) {
      readTimeCoordinates(joinAggCoord, cancelTask);
    }

    // make it a cacheVar
    CacheVar cv = new CoordValueVar(joinAggCoord);
    joinAggCoord.setSPobject( cv);
    cacheList.add(cv);

    setDatasetAcquireProxy(typicalDataset, ncDataset);
    typicalDataset.close( typical);
  }

  protected void rebuildDataset() throws IOException {
    super.rebuildDataset();

    if (timeUnitsChange) {
      VariableDS joinAggCoord = (VariableDS) ncDataset.getRootGroup().findVariable(dimName);
      readTimeCoordinates(joinAggCoord, null);
    }
  }

  // time units change - must read in time coords and convert, cache the results
  // must be able to be made into a CoordinateAxis1DTime
  protected void readTimeCoordinates(VariableDS timeAxis, CancelTask cancelTask) throws IOException {
    List<Date> dateList = new ArrayList<Date>();
    String units = null;

    for (Dataset dataset : getDatasets()) {
      NetcdfDataset ncfile = null;
      try {
        ncfile = (NetcdfDataset) dataset.acquireFile(cancelTask);
        VariableDS v = (VariableDS) ncfile.findVariable(timeAxis.getName());
        if (v == null) {
          logger.warn("readTimeCoordinates: variable = " + timeAxis.getName() + " not found in file " + dataset.getLocation());
          return;
        }
        CoordinateAxis1DTime timeCoordVar = CoordinateAxis1DTime.factory(ncDataset, v, null);
        java.util.Date[] dates = timeCoordVar.getTimeDates();
        for (Date d : dates)
          dateList.add(d);

        if (units == null)
          units = v.getUnitsString();

      } finally {
        dataset.close( ncfile);
      }
      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    int[] shape = timeAxis.getShape();
    int ntimes = shape[0];
    assert (ntimes == dateList.size());

    Array timeCoordVals = Array.factory(timeAxis.getDataType(), shape);
    IndexIterator ii = timeCoordVals.getIndexIterator();
    timeAxis.setCachedData(timeCoordVals, false);

    // check if its a String or a udunit
    if (timeAxis.getDataType() == DataType.STRING) {

      for (Date date : dateList) {
        ii.setObjectNext(formatter.toDateTimeStringISO(date));
      }

    } else {

      DateUnit du;
      try {
        du = new DateUnit(units);
      } catch (Exception e) {
        throw new IOException(e.getMessage());
      }
      timeAxis.addAttribute(new Attribute("units", units));

      for (Date date : dateList) {
        double val = du.makeValue(date);
        ii.setDoubleNext(val);
      }
    }
  }

  /**
   * Persist info (nccords, coorValues) from joinExisting, since that can be expensive to recreate.
   *
   * @throws IOException
   */
  public void persist() throws IOException {
    if (diskCache2 == null)
      return;

    // only write out if something changed after the cache file was last written
    if (!cacheDirty)
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
      if (datasetManager.getRecheck() != null)
        out.print("recheckEvery='" + datasetManager.getRecheck() + "' ");
      out.print(">\n");

      List<Dataset> nestedDatasets = getDatasets();
      for (Dataset dataset : nestedDatasets) {
        DatasetOuterDimension dod = (DatasetOuterDimension) dataset;

        out.print("  <netcdf location='" + dataset.getLocation() + "' ");
        out.print("ncoords='" + dod.getNcoords(null) + "' ");

        if (dod.coordValue != null)
          out.print("coordValue='" + dod.coordValue + "' ");

        out.print("/>\n");
      }

      out.print("</aggregation>\n");
      out.close(); // this also closes the  channel and releases the lock

      cacheFile.setLastModified(datasetManager.getLastScanned());
      cacheDirty = false;

      if (debug)
        System.out.println("Aggregation persisted = " + cacheFile.getPath() + " lastModified= " + new Date(datasetManager.getLastScanned()));

    } finally {
      if (channel != null)
        channel.close();
    }
  }

  // read info from the persistent XML file, if it exists
  protected void persistRead() {
    if (diskCache2 == null) return;

    String cacheName = getCacheName();
    if (cacheName == null) return;

    File cacheFile = diskCache2.getCacheFile(cacheName);
    if (!cacheFile.exists())
      return;

    if (debugCache) System.out.println(" Try to Read cache " + cacheFile.getPath());

    Element aggElem;
    try {
      aggElem = ucar.nc2.util.xml.Parse.readRootElement("file:"+cacheFile.getPath());
    } catch (IOException e) {
      if (debugCache) System.out.println(" No cache for " + cacheName+" - "+e.getMessage());
      return;
    }

    // use a map to find datasets to avoid O(n**2) searching
    Map<String,Dataset> map = new HashMap<String,Dataset>();
    for (Dataset ds : getDatasets()) {
      map.put(ds.getLocation(), ds);
    }

    List<Element> ncList = aggElem.getChildren("netcdf", NcMLReader.ncNS);
    for (Element netcdfElemNested : ncList) {
      String location = netcdfElemNested.getAttributeValue("location");
      DatasetOuterDimension dod = (DatasetOuterDimension) map.get(location);

      if ((null != dod) && (dod.ncoord == 0)) {
        if (debugPersistDetail) System.out.println("  use cache for " + location);
        String ncoordsS = netcdfElemNested.getAttributeValue("ncoords");
        try {
          dod.ncoord = Integer.parseInt(ncoordsS);
        } catch (NumberFormatException e) {
          logger.error("bad ncoord attribute on dataset=" + location);
        }

        String coordValue = netcdfElemNested.getAttributeValue("coordValue");
        if (coordValue != null) {
          dod.coordValue = coordValue;
        }

      }
    }

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
