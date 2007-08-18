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
      dimNames.add(stoke.nextToken());
    }
  }

  @Override
  protected void buildDataset(CancelTask cancelTask) throws IOException {

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(null);
    DatasetConstructor.transferDataset(typical, ncDataset, null);

    // find the tiling dimensions
    for (String dimName : dimNames) {
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
        dt.section = dt.section.addRangeNames(dimNames);
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

  @Override
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

  @Override
  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape()); // LOOK need fill

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      DatasetTiled dtiled = (DatasetTiled) vnested;

      Section totalSection = mainv.getShapeAsSection();
      // construct the "dataSection" by replacing the tiled dimensions
      Section localSection = dtiled.makeVarSection(mainv);

      // now use a RegularSectionLayout to figure out how to "distribute" it to the result array
      Indexer index;
      try {
        index = RegularSectionLayout.factory(0, dtype.getSize(), localSection, totalSection);
      } catch (InvalidRangeException e) {
        throw new IOException(e.getMessage());
      }

      // read in the entire data from this nested dataset
      Array varData = dtiled.read(mainv, cancelTask);

      // distribute it
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        System.out.println(" chunk: " + chunk + " for var " + mainv.getName());

        // the dest array (allData) is the "want" Section
        // the src array (varData) acts as the "file", but file pos is in bytes, need to convert to elements
        int srcPos = (int) chunk.getFilePos() / dtype.getSize();
        int resultPos = (int) chunk.getStartElem();
        Array.arraycopy(varData, srcPos, allData, resultPos, chunk.getNelems());
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    return allData;
  }

  @Override
  public Array read(Variable mainv, Section wantSection, CancelTask cancelTask) throws IOException {
    // If its full sized, then use full read, so that data might get cached.
    long size = wantSection.computeSize();
    if (size == mainv.getSize())
      return read(mainv, cancelTask);

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, wantSection.getShape()); // LOOK need fill

    // run through all the datasets
    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      DatasetTiled dtiled = (DatasetTiled) vnested;
      Section localSection = dtiled.makeVarSection(mainv);
      Indexer index;
      Array varData;

      try {
        if (!localSection.intersects(wantSection))
          continue;

        index = RegularSectionLayout.factory(0, dtype.getSize(), localSection, wantSection);

        // read in the desired section of data from this nested dataset
        Section needToRead = localSection.intersect(wantSection); // the part we need to read
        Section localNeed = needToRead.shiftOrigin(localSection); // shifted to the local section
        varData = dtiled.read(mainv, cancelTask, localNeed.getRanges());

      } catch (InvalidRangeException e) {
        throw new IllegalArgumentException(e.getMessage());
      }

      int srcPos = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        System.out.println(" chunk: " + chunk + " for var " + mainv.getName());

        // RegularSectionLayout assumes you are reading to a file. We have already done so, and put the
        // data, now contiguous into varData. So we just let RegularSectionLayout tell us where it goes.
        int resultPos = (int) chunk.getStartElem();
        Array.arraycopy(varData, srcPos, allData, resultPos, chunk.getNelems());
        srcPos += chunk.getNelems();
      }

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

    boolean isNeeded(Section wantSection) throws InvalidRangeException {
      return section.intersects(wantSection);
    }

    // construct the Variable section pertaining to this Datatset by replacing the tiled dimensions
    Section makeVarSection(Variable mainv) {
      Section vSection = mainv.getShapeAsSection();
      Section dataSection = new Section();
      for (Range r : vSection.getRanges()) {
        Range rr = section.find(r.getName());
        dataSection.appendRange(rr != null ? rr : r);
      }
      return dataSection;
    }
  }
}
