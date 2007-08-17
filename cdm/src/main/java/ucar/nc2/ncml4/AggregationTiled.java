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

import ucar.nc2.util.CancelTask;
import ucar.nc2.*;
import ucar.nc2.iosp.RegularSectionLayout;
import ucar.nc2.iosp.Indexer;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.ma2.*;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

/**
 * @author caron
 * @since Aug 16, 2007
 */
public class AggregationTiled extends Aggregation {
  private List<String> dimNames = new ArrayList<String>();
  private List<Dimension> dims = new ArrayList<Dimension>();
  private Section section;

  public AggregationTiled(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Aggregation.Type.TILED, recheckS);

    // parse the tiling dimension names
    StringTokenizer stoke = new StringTokenizer(dimName);
    while (stoke.hasMoreTokens()) {
      dimNames.add( stoke.nextToken());
    }
  }

  protected void buildDataset(CancelTask cancelTask) throws IOException {

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(null);
    DatasetConstructor.transferDataset(typical, ncDataset, null);

    // find the tiling dimensions
    for ( String dimName : dimNames) {
      Dimension dim = ncDataset.getRootGroup().findDimension(dimName); // dimension is from new dataset
      if (null != dim)
        dims.add(dim);
      else
        throw new IllegalArgumentException("Unknown dimension = " + dimName);
    }

    // run through the datasets to get the union of the ranges
    // add names to the dataset sections while were there
    Section result = null;
    for (Dataset d : datasets) {
      DatasetTiled dt = (DatasetTiled) d;
      try {
        dt.section = dt.section.addRangeNames( dimNames);
        result = (result == null) ? dt.section : result.union(dt.section);
      } catch (InvalidRangeException e) {
        throw new IllegalArgumentException(e);
      }
    }

    // sanity checks
    assert result != null;
    assert result.getRank() == dims.size();
    for (Range r : result.getRanges()) {
      assert r.first() == 0;
      assert r.stride() == 1;
    }
    section = result;

    // set dimension lengths to union length
    int count = 0;
    for (Range r : section.getRanges()) {
      Dimension dim = dims.get(count++);
      dim.setLength(r.length());
    }

    // run through all variables
    for (Variable v : typical.getVariables()) {
      if (isTiled(v)) {
        VariableDS vagg = new VariableDS(ncDataset, null, null, v.getShortName(), v.getDataType(),
            v.getDimensionsString(), null, null);
        vagg.setProxyReader2(this); // do the reading here
        DatasetConstructor.transferVariableAttributes(v, vagg);

        ncDataset.removeVariable(null, v.getShortName());
        ncDataset.addVariable(null, vagg);
        // aggVars.add(vagg);
      }
      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    setDatasetAcquireProxy(typicalDataset, ncDataset);
    typical.close(); // close it because we use DatasetProxyReader to acquire    */
  }

  // a variable is tiled if any of its dimensions are tiled
  private boolean isTiled(Variable v) {
    for (Dimension d : v.getDimensions()) {
      for (Range r : section.getRanges()) {
        if (d.getName().equals(r.getName()))
          return true;
      }
    }
    return false;
  }

  protected void rebuildDataset() throws IOException {
    /* buildCoords(null);

    // reset dimension length
    Dimension aggDim = ncDataset.findDimension(dimName);
    aggDim.setLength( getTotalCoords());

    // reset coordiante var
    VariableDS joinAggCoord = (VariableDS) ncDataset.getRootGroup().findVariable(dimName);
    joinAggCoord.setDimensions(dimName); // reset its dimension
    joinAggCoord.invalidateCache(); // get rid of any cached data, since its now wrong

    // reset agg variables
    for (Variable aggVar : aggVars) {
      aggVar.setDimensions(dimName); // reset its dimension
      aggVar.invalidateCache(); // get rid of any cached data, since its now wrong
    }

    // reset the typical dataset, where non-agg variables live
    Dataset typicalDataset = getTypicalDataset();
    DatasetProxyReader proxy = new DatasetProxyReader(typicalDataset);
    for (Variable var : ncDataset.getRootGroup().getVariables()) {
      if (aggVars.contains(var))
        continue;
      VariableEnhanced ve = (VariableEnhanced) var; // need this for getProxyReader2()
      ve.setProxyReader2(proxy);
    }
    typicalDataset.close(); */
  }

  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape());

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      DatasetTiled dt = (DatasetTiled) vnested;

      // construct the "dataSection" by replacing the tiled dimensions
      Section vSection = mainv.getShapeAsSection();
      Section dataSection = new Section();
      for (Range r : vSection.getRanges()) {
        Range rr = dt.section.find( r.getName());
        dataSection.appendRange(rr != null ? rr : r);
      }

      // now use a RegularSectionLayout to figure out how to "distribute" it to the result array
      Indexer index;
      try {
        index = RegularSectionLayout.factory(0, dtype.getSize(), dataSection, vSection);
      } catch (InvalidRangeException e) {
        throw new IOException(e.getMessage());
      }

      // read in the entire data from this nested dataset
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        System.out.println(" chunk: " + chunk+" for var "+ mainv.getName());

        // the dest array (allData) is the "want" Section
        // the src array (varData) acts as the "file", but file pos is in bytes, need to convert to elements
        int srcPos = (int) chunk.getFilePos() / dtype.getSize();
        int resultPos = (int) chunk.getStartElem();
        Array.arraycopy(varData, srcPos, allData, resultPos, chunk.getNelems());
      }
    }

    return allData;
  }

  private Array read1D(Variable mainv, CancelTask cancelTask) throws IOException {
    String vdname = mainv.getDimension(0).getName();

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape());
    int destPos = 0;

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      DatasetTiled dt = (DatasetTiled) vnested;
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      // arraycopy( Array arraySrc, int srcPos, Array arrayDst, int dstPos, int len)
      Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return allData;
  }


  /**
   * Read a section of an aggregation variable.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @param section    read just this section of the data, array of Range
   * @return the data array section
   * @throws IOException
   */
  public Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    // If its full sized, then use full read, so that data gets cached.
    long size = section.computeSize();
    if (size == mainv.getSize())
      return read(mainv, cancelTask);

    // the case of the agg coordinate var
    if (mainv.getShortName().equals(dimName))
      return readAggCoord(mainv, section, cancelTask);

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array sectionData = Array.factory(dtype, section.getShape());
    int destPos = 0;

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<Range>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    if (debug) System.out.println("   agg wants range=" + mainv.getName() + "(" + joinRange + ")");

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset nested : nestedDatasets) {
      DatasetTiled dod = (DatasetTiled) nested;
      if (!dod.isNeeded(null))
        continue;
      //if (debug)
      //  System.out.println("   agg use " + nested.aggStart + ":" + nested.aggEnd + " range= " + nestedJoinRange + " file " + nested.getLocation());

      Array varData;
      if ((type == Type.JOIN_NEW) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        varData = nested.read(mainv, cancelTask, innerSection);
      } else {
        nestedSection.set(0, null);
        varData = nested.read(mainv, cancelTask, nestedSection);
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return sectionData;
  }

  protected Array readAggCoord(Variable aggCoord, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, section.getShape());
    IndexIterator result = allData.getIndexIterator();

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<Range>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      DatasetTiled dod = (DatasetTiled) vnested;
      if (!dod.isNeeded(null))
        continue;
      //if (debug)
      //  System.out.println("   agg use " + vnested.aggStart + ":" + vnested.aggEnd + " range= " + nestedJoinRange + " file " + vnested.getLocation());

      //readAggCoord(aggCoord, cancelTask, dod, dtype, result, nestedJoinRange, nestedSection, innerSection);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    return allData;
  }


  @Override
  protected Dataset makeDataset(String cacheName, String location, String ncoordS, String coordValueS, String sectionSpec, boolean enhance, NetcdfFileFactory reader) {
    return new DatasetTiled(cacheName, location, sectionSpec, enhance, reader);
  }

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   */
  class DatasetTiled extends Dataset {

    protected String sectionSpec;
    protected Section section;


    /**
     * Dataset constructor.
     * With this constructor, the actual opening of the dataset is deferred, and done by the reader.
     * Used with explicit netcdf elements, and scanned files.
     *
     * @param cacheName   a unique name to use for caching
     * @param location    attribute "location" on the netcdf element
     * @param sectionSpec attribute "sectionSpec" on the netcdf element
     * @param enhance     open dataset in enhance mode
     * @param reader      factory for reading this netcdf dataset; if null, use NetcdfDataset.open( location)
     */
    protected DatasetTiled(String cacheName, String location, String sectionSpec, boolean enhance, NetcdfFileFactory reader) {
      super(cacheName, location, enhance, reader);
      this.sectionSpec = sectionSpec;

      try {
        section = new Section(sectionSpec);
      } catch (InvalidRangeException e) {
        throw new IllegalArgumentException(e);
      }
    }

    protected boolean isNeeded(Range totalRange) {
      return false;
    }

    @Override
    protected Array read(Variable mainv, CancelTask cancelTask, List<Range> section) throws IOException, InvalidRangeException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        if (debugRead) {
          System.out.print("agg read " + ncd.getLocation() + " nested= " + getLocation());
          for (Range range : section)
            System.out.print(" " + range + ":");
          System.out.println("");
        }

        Variable v = ncd.findVariable(mainv.getName());

        // its possible that we are asking for more of the time coordinate than actually exists (fmrc ragged time)
        // so we need to read only what is there
        Range fullRange = v.getRanges().get(0);
        Range want = section.get(0);
        if (fullRange.last() < want.last()) {
          Range limitRange = new Range(want.first(), fullRange.last(), want.stride());
          section = new ArrayList<Range>(section); // make a copy
          section.set(0, limitRange);
        }

        return v.read(section);

      } finally {
        if (ncd != null) ncd.close();
      }
    }

  }
}
