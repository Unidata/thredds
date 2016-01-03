/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ncml;

import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import thredds.inventory.MFile;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.CancelTask;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.*;

/**
 * JoinExisting Aggregation.
 *
 * @author caron
 */
public class AggregationExisting extends AggregationOuterDimension {

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
    CacheVar coordCacheVar = null;
    if (type != Aggregation.Type.joinExistingOne) {
      Variable tcv = typical.findVariable(dimName);
      if (tcv != null)  {
        coordCacheVar = new CoordValueVar(dimName, tcv.getDataType(), tcv.getUnitsString());
      } else {
        // LOOK what to do; docs claim we can add the coord variable in the outer ncml, so make a fake one for now
        VariableDS fake = new VariableDS(ncDataset, null, null, dimName, DataType.INT, dimName, null, null);
        // fake.setValues(npts, 0, 1);
        ncDataset.addVariable(null, fake);
      }

    } else {
      coordCacheVar = new CoordValueVar(dimName, DataType.STRING, "");      
    }
    if (coordCacheVar != null)
      cacheList.add(coordCacheVar);  // coordinate variable is always cached

    // gotta check persistence info - before buildCoords - if its going to do any good
    persistRead();

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
      String outerName = v.getDimension(0).makeFullName();
      if (!dimName.equals(outerName))
        continue;

      Group newGroup =  DatasetConstructor.findGroup(ncDataset, v.getParentGroup());
      VariableDS vagg = new VariableDS(ncDataset, newGroup, null, v.getShortName(), v.getDataType(),
              v.getDimensionsString(), null, null);
      vagg.setProxyReader( this);
      DatasetConstructor.transferVariableAttributes(v, vagg);

      newGroup.removeVariable( v.getShortName());
      newGroup.addVariable( vagg);
      aggVars.add(vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    // handle the agg coordinate variable
    VariableDS joinAggCoord = (VariableDS) ncDataset.findVariable(dimName); // long name of dimension, coord variable
    if ((joinAggCoord == null) && (type == Type.joinExisting)) {
      typicalDataset.close( typical); // clean up
      throw new IllegalArgumentException("No existing coordinate variable for joinExisting on "+getLocation());
    }

    if (type == Type.joinExistingOne) {
      if (joinAggCoord != null) {
        // replace aggregation coordinate variable
        ncDataset.getRootGroup().removeVariable(joinAggCoord.getShortName());
      }
      
      joinAggCoord = new VariableDS(ncDataset, null, null, dimName, DataType.STRING, dimName, null, null);
      joinAggCoord.setProxyReader( this);
      ncDataset.getRootGroup().addVariable(joinAggCoord);
      aggVars.add(joinAggCoord);
      
      joinAggCoord.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, "Time"));
      joinAggCoord.addAttribute(new Attribute(CDM.LONG_NAME, "time coordinate"));
      joinAggCoord.addAttribute(new ucar.nc2.Attribute(CF.STANDARD_NAME, "time"));
    }

    if (timeUnitsChange) {
      readTimeCoordinates(joinAggCoord, cancelTask);
    }

    // make it a cacheVar
    joinAggCoord.setSPobject( coordCacheVar);

    // check persistence info - may have cached values other than coordinate  LOOK ????
    persistRead();

    setDatasetAcquireProxy(typicalDataset, ncDataset);
    typicalDataset.close( typical);

    if (debugInvocation) System.out.println(ncDataset.getLocation()+" invocation count = "+AggregationOuterDimension.invocation);

    ncDataset.finish();
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
    List<CalendarDate> dateList = new ArrayList<>();
    String timeUnits = null;

    // make concurrent
    for (Dataset dataset : getDatasets()) {
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile(cancelTask);
        Variable v = ncfile.findVariable(timeAxis.getFullNameEscaped());
        if (v == null) {
          logger.warn("readTimeCoordinates: variable = " + timeAxis.getFullName() + " not found in file " + dataset.getLocation());
          return;
        }
        VariableDS vds = (v instanceof VariableDS) ? (VariableDS) v : new VariableDS( null, v, true);
        CoordinateAxis1DTime timeCoordVar = CoordinateAxis1DTime.factory(ncDataset, vds, null);
        dateList.addAll(timeCoordVar.getCalendarDates());

        if (timeUnits == null)
          timeUnits = v.getUnitsString();

      } finally {
        dataset.close( ncfile);
      }
      if (cancelTask != null && cancelTask.isCancel()) return;
    }
    assert timeUnits != null;

    int[] shape = timeAxis.getShape();
    int ntimes = shape[0];
    assert (ntimes == dateList.size());

    DataType coordType = (timeAxis.getDataType() == DataType.STRING) ? DataType.STRING : DataType.DOUBLE;
    Array timeCoordVals = Array.factory(coordType, shape);
    IndexIterator ii = timeCoordVals.getIndexIterator();

    // check if its a String or a udunit
    if (timeAxis.getDataType() == DataType.STRING) {

      for (CalendarDate date : dateList) {
        ii.setObjectNext(date.toString());
      }

    } else {      
      timeAxis.setDataType(DataType.DOUBLE); // otherwise fractional values get lost

      DateUnit du;
      try {
        du = new DateUnit(timeUnits);
      } catch (Exception e) {
        throw new IOException(e.getMessage());
      }
      timeAxis.addAttribute(new Attribute(CDM.UNITS, timeUnits));

      for (CalendarDate date : dateList) {
        double val = du.makeValue(date.toDate());
        ii.setDoubleNext(val);
      }
    }

    timeAxis.setCachedData(timeCoordVals, false);
  }

  /**
   * Persist info (ncoords, coordValues) from joinExisting, since that can be expensive to recreate.
   *
   * @throws IOException
   */
  public void persistWrite() throws IOException {
    if (diskCache2 == null)
      return;

    String cacheName = getCacheName();
    if (cacheName == null) return;
    if (cacheName.startsWith("file:"))      // LOOK
      cacheName = cacheName.substring(5);
    File cacheFile = diskCache2.getCacheFile(cacheName);
    if (cacheFile == null) throw new IllegalStateException();

    // only write out if something changed after the cache file was last written, or if the file has been deleted
    if (!cacheDirty && cacheFile.exists())
      return;

    FileChannel channel = null;
    try {
      File dir = cacheFile.getParentFile();
      if (!dir.exists()) {
        if (!dir.mkdirs())
          logger.error("Cant make cache directory= "+cacheFile);
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

      PrintWriter out = new PrintWriter( new OutputStreamWriter(fos, CDM.utf8Charset));
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
          Array data = pv.getData(dod.getId());
          if (data != null) {
            out.print("    <cache varName='" + pv.varName + "' >");
            NCdumpW.printArray(data, out);
            out.print("</cache>\n");
            if (logger.isDebugEnabled())
              logger.debug(" wrote array = " + pv.varName + " nelems= "+data.getSize()+" for "+dataset.getLocation());
          }
        }
        out.print("  </netcdf>\n");
      }

      out.print("</aggregation>\n");
      out.close(); // this also closes the  channel and releases the lock

      long time = datasetManager.getLastScanned();
      if (time == 0) time = System.currentTimeMillis(); // no scans (eg all static) will have a 0

      if (!cacheFile.setLastModified( time))
        logger.warn("FAIL to set lastModified on {}", cacheFile.getPath());
      cacheDirty = false;

      if (logger.isDebugEnabled())
              logger.debug("Aggregation persisted = " + cacheFile.getPath() + " lastModified= " + new Date(datasetManager.getLastScanned()));

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
    if (cacheName.startsWith("file:"))      // LOOK
      cacheName = cacheName.substring(5);

    File cacheFile = diskCache2.getCacheFile(cacheName);
    if (cacheFile == null) throw new IllegalStateException();
    if (!cacheFile.exists())
      return;
    long lastWritten = cacheFile.lastModified();

    if (logger.isDebugEnabled()) logger.debug(" Try to Read cache {} " + cacheFile.getPath());

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

    List<Element> ncList = aggElem.getChildren("netcdf", Catalog.ncmlNS);
    for (Element netcdfElemNested : ncList) {
      String id = netcdfElemNested.getAttributeValue("id");
      DatasetOuterDimension dod = (DatasetOuterDimension) map.get(id);

      if (null == dod) {
        // this should mean that the dataset has been deleted. so not a problem
        if (logger.isDebugEnabled()) logger.debug(" have cache but no dataset= {}", id);
        continue;
      }
      if (logger.isDebugEnabled()) logger.debug(" use cache for dataset= {}", id);

      MFile mfile = dod.getMFile();
      if (mfile != null && mfile.getLastModified() > lastWritten)  {  // skip datasets that have changed
        if (logger.isDebugEnabled()) logger.debug(" dataset was changed= {}", mfile);
        continue;
      }

      if (dod.ncoord == 0) {
        String ncoordsS = netcdfElemNested.getAttributeValue("ncoords");
        try {
          dod.ncoord = Integer.parseInt(ncoordsS);
          if (logger.isDebugEnabled()) logger.debug(" Read the cache; ncoords = {}", dod.ncoord);
        } catch (NumberFormatException e) {
          logger.error("bad ncoord attribute on dataset=" + id);
        }
      }

      // if (dod.coordValue != null) continue; // allow ncml to override

      List<Element> cacheElemList = netcdfElemNested.getChildren("cache", Catalog.ncmlNS);
      for (Element cacheElemNested : cacheElemList) {
        String varName = cacheElemNested.getAttributeValue("varName");
        CacheVar pv = findCacheVariable(varName);
        if (pv != null) {
          String sdata = cacheElemNested.getText();
          if (sdata.length() == 0) continue;
          if (logger.isDebugEnabled()) logger.debug(" read data for var = " + varName + " size= "+sdata.length());

          //long start = System.nanoTime();
          String[] vals = sdata.split(" ");
          //double took = .001 * .001 * .001 * (System.nanoTime() - start);
          //if (debugPersist) System.out.println("  split took = " + took + " sec; ");

          try {
            //start = System.nanoTime();
            Array data = Array.makeArray(pv.dtype, vals);
            //took = .001 * .001 * .001 * (System.nanoTime() - start);
            //if (debugPersist) System.out.println("  makeArray took = " + took + " sec nelems= "+data.getSize());
            pv.putData(id, data);
            countCacheUse++;

          } catch (Exception e) {
            logger.warn("Error reading cached data ",e);
          }

        } else {
          logger.warn("not a cache var=" + varName);
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

  //////////////////////////////////////////////////
  // back door for testing
  static public int countCacheUse = 0;

}
