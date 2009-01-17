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
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.*;
import ucar.nc2.units.DateUnit;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

import java.io.*;
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
  static private boolean debugPersist = true, debugPersistDetail = true;

  public AggregationExisting(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Aggregation.Type.joinExisting, recheckS);
  }

  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(null);
    DatasetConstructor.transferDataset(typical, ncDataset, null);

    // LOOK not dealing with groups

    // a little tricky to get the coord var cached if we have to read through the datasets on the buildCoords()
    String dimName = getDimensionName();
    Variable tcv = typical.findVariable(dimName);
    CacheVar coordCacheVar = new CoordValueVar(dimName, tcv.getDataType(), tcv.getUnitsString());
    cacheList.add(coordCacheVar);  // coordinate variable is always cached

    // now find out how many coordinates we have, caching values if needed
    buildCoords(cancelTask);

    // create aggregation dimension, now that we know the size
    Dimension aggDim = new Dimension(dimName, getTotalCoords());
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

      Group newGroup =  DatasetConstructor.findGroup(ncDataset, v.getParentGroup());
      VariableDS vagg = new VariableDS(ncDataset, newGroup, null, v.getShortName(), v.getDataType(),
              v.getDimensionsString(), null, null);
      vagg.setProxyReader(this); // do the reading here
      DatasetConstructor.transferVariableAttributes(v, vagg);

      newGroup.removeVariable( v.getShortName());
      newGroup.addVariable( vagg);
      aggVars.add(vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    // handle the agg coordinate variable
    VariableDS joinAggCoord = (VariableDS) ncDataset.getRootGroup().findVariable(dimName);
    if ((joinAggCoord == null) && (type == Type.joinExisting)) {
      typicalDataset.close( typical); // clean up
      throw new IllegalArgumentException("No existing coordinate variable for joinExisting on "+getLocation());
    }

    if (type == Type.joinExistingOne) {
      // ok if cordinate doesnt exist for a "join existing one", since we have to create it anyway
      if (joinAggCoord == null) {
        joinAggCoord = new VariableDS(ncDataset, null, null, dimName, DataType.STRING, dimName, null, null);
        joinAggCoord.setProxyReader(this); // do the reading here
        ncDataset.getRootGroup().addVariable(joinAggCoord);
        aggVars.add(joinAggCoord);

      } else {
        // replace aggregation coordinate variable
        //joinAggCoord.setDataType(DataType.STRING);
        //joinAggCoord.getAttributes().clear(); // why ?
      }
      
      joinAggCoord.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, "Time"));
      joinAggCoord.addAttribute(new Attribute("long_name", "time coordinate"));
      joinAggCoord.addAttribute(new ucar.nc2.Attribute("standard_name", "time"));
    }

    if (timeUnitsChange) {
      readTimeCoordinates(joinAggCoord, cancelTask);
    }

    // make it a cacheVar
    joinAggCoord.setSPobject( coordCacheVar);

    // check persistence info - may have cached values
    persistRead();

    setDatasetAcquireProxy(typicalDataset, ncDataset);
    typicalDataset.close( typical);

    if (debugInvocation) System.out.println(ncDataset.getLocation()+" invocation count = "+AggregationOuterDimension.invocation);
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

    // make concurrent
    for (Dataset dataset : getDatasets()) {
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile(cancelTask);
        Variable v = ncfile.findVariable(timeAxis.getName());
        if (v == null) {
          logger.warn("readTimeCoordinates: variable = " + timeAxis.getName() + " not found in file " + dataset.getLocation());
          return;
        }
        VariableDS vds = (v instanceof VariableDS) ? (VariableDS) v : new VariableDS(null, v, true);
        CoordinateAxis1DTime timeCoordVar = CoordinateAxis1DTime.factory(ncDataset, vds, null);
        java.util.Date[] dates = timeCoordVar.getTimeDates();
        dateList.addAll(Arrays.asList(dates));

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
  public void persistWrite() throws IOException {
    if (diskCache2 == null)
      return;

    String cacheName = getCacheName();
    if (cacheName == null) return;
    File cacheFile = diskCache2.getCacheFile(cacheName);

    // only write out if something changed after the cache file was last written, or if the file has been deleted
    if (!cacheDirty && cacheFile.exists())
      return;

    FileChannel channel = null;
    try {
      if (!cacheFile.exists()) {
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

      PrintWriter out = new PrintWriter(fos);
      out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
      out.print("<aggregation xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' version='3' ");
      out.print("type='" + type + "' ");
      if (dimName != null)
        out.print("dimName='" + dimName + "' ");
      if (datasetManager.getRecheck() != null)
        out.print("recheckEvery='" + datasetManager.getRecheck() + "' ");
      out.print(">\n");

      List<Dataset> nestedDatasets = getDatasets();
      for (Dataset dataset : nestedDatasets) {
        DatasetOuterDimension dod = (DatasetOuterDimension) dataset;
        if (dod.getId() == null) logger.warn("id is null");

        out.print("  <netcdf id='" + dod.getId() + "' ");
        out.print("ncoords='" + dod.getNcoords(null) + "' >\n");

        for (CacheVar pv : cacheList) {
          Array data = pv.dataMap.get(dod.getId());
          if (data != null) {
            out.print("    <cache varName='" + pv.varName + "' >");
            NCdumpW.printArray(data, out);
            out.print("</cache>\n");
            if (debugPersist)
              System.out.println(" wrote array = " + pv.varName + " nelems= "+data.getSize()+" for "+dataset.getLocation());
          }
        }
        out.print("  </netcdf>\n");
      }

      out.print("</aggregation>\n");
      out.close(); // this also closes the  channel and releases the lock

      cacheFile.setLastModified( datasetManager.getLastScanned());
      cacheDirty = false;

      if (debugPersist)
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

    if (debugPersist) System.out.println(" Try to Read cache " + cacheFile.getPath());

    Element aggElem;
    try {
      aggElem = ucar.nc2.util.xml.Parse.readRootElement("file:"+cacheFile.getPath());
    } catch (IOException e) {
      if (debugCache) System.out.println(" No cache for " + cacheName+" - "+e.getMessage());
      return;
    }

    String version = aggElem.getAttributeValue("version");
    if ((version == null) || !version.equals("3")) return; // dont read old cache files, recreate

    // use a map to find datasets to avoid O(n**2) searching
    Map<String,Dataset> map = new HashMap<String,Dataset>();
    for (Dataset ds : getDatasets()) {
      map.put(ds.getId(), ds);
    }

    List<Element> ncList = aggElem.getChildren("netcdf", NcMLReader.ncNS);
    for (Element netcdfElemNested : ncList) {
      String id = netcdfElemNested.getAttributeValue("id");
      DatasetOuterDimension dod = (DatasetOuterDimension) map.get(id);

      if (null == dod) {
        if (debugPersist) System.out.println(" have cache but no dataset= " + id);
        logger.warn(" have cache but no dataset= " + id);

      } else {

        if (debugPersist) System.out.println(" use cache for dataset= " + id);
        if (dod.ncoord == 0) {
          String ncoordsS = netcdfElemNested.getAttributeValue("ncoords");
          try {
            dod.ncoord = Integer.parseInt(ncoordsS);
            if (debugPersist) System.out.println(" Read the cache; ncoords = " + dod.ncoord);
          } catch (NumberFormatException e) {
            logger.error("bad ncoord attribute on dataset=" + id);
          }
        }

        // if (dod.coordValue != null) continue; // allow ncml to override

        List<Element> cacheElemList = netcdfElemNested.getChildren("cache", NcMLReader.ncNS);
        for (Element cacheElemNested : cacheElemList) {
          String varName = cacheElemNested.getAttributeValue("varName");
          CacheVar pv = findCacheVariable(varName);
          if (pv != null) {
            String sdata = cacheElemNested.getText();
            if (sdata.length() == 0) continue;
            if (debugPersist) System.out.println(" read data for var = " + varName + " size= "+sdata.length());
            
            long start = System.nanoTime();
            String[] vals = sdata.split(" ");
            double took = .001 * .001 * .001 * (System.nanoTime() - start);
            if (debugPersist) System.out.println("  split took = " + took + " sec; ");

            try {
              start = System.nanoTime();
              Array data = Array.makeArray(pv.dtype, vals);
              took = .001 * .001 * .001 * (System.nanoTime() - start);
              if (debugPersist) System.out.println("  makeArray took = " + took + " sec nelems= "+data.getSize());
              pv.dataMap.put(id, data);
            } catch (Exception e) {
              logger.warn("Error reading cached data ",e);
            }

          } else
            logger.error("not a cache var=" + varName);
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
