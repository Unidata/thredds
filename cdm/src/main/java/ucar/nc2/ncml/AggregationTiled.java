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

import ucar.nc2.util.CancelTask;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.ma2.*;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

/**
 * Tiled Aggregation.
 * @author caron
 * @since Aug 16, 2007
 */
public class AggregationTiled extends Aggregation {
  private List<String> dimNames = new ArrayList<String>();
  private List<Dimension> dims = new ArrayList<Dimension>();
  private Section section;

  private boolean debug = false;

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
        Group newGroup =  DatasetConstructor.findGroup(ncDataset, v.getParentGroup());
        VariableDS vagg = new VariableDS(ncDataset, newGroup, null, v.getShortName(), v.getDataType(),
                v.getDimensionsString(), null, null);   // LOOK what about anon dimensions?
        vagg.setProxyReader(this); // do the reading here
        DatasetConstructor.transferVariableAttributes(v, vagg);

        newGroup.removeVariable( v.getShortName());
        newGroup.addVariable( vagg);
        // aggVars.add(vagg);
      }
      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    setDatasetAcquireProxy(typicalDataset, ncDataset);
    typicalDataset.close(typical); // close it because we use DatasetProxyReader to acquire    */
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
    ncDataset.empty();
    dims = new ArrayList<Dimension>();
    buildDataset(null);
  }

  @Override
  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape()); // LOOK need fill
    Section wantSection = mainv.getShapeAsSection();
    if (debug) System.out.println("wantSection: " + wantSection+" for var "+mainv.getName());

    // make concurrent
    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      DatasetTiled dtiled = (DatasetTiled) vnested;

      // construct the "dataSection" by replacing the tiled dimensions
      Section tiledSection = dtiled.makeVarSection(mainv);
      //System.out.println(" tiledSection: " + tiledSection);

      // now use a TileLayout to figure out how to "distribute" it to the result array
      Array varData;
      TileLayout index;
      try {
        // read in the entire data from this nested dataset
        varData = dtiled.read(mainv, cancelTask);
        index = new TileLayout(tiledSection, wantSection);

        if (debug) System.out.println(" varData read: " + new Section(varData.getShape()));
      } catch (InvalidRangeException e) {
        throw new IllegalArgumentException(e.getMessage());
      }

      while (index.hasNext()) {
        try {
          Array.arraycopy(varData, index.srcPos, allData, index.resultPos, index.nelems);
        } catch (RuntimeException e) {
          System.out.println(index.toString());
          throw e;
        }
      }

      // covers the case of coordinate variables for a 1 row or 1 col tiling.
      // doesnt eliminate duplicate reading in general
      if (varData.getSize() == mainv.getSize()) break;

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
    if (debug) {
      try {
        System.out.println(dtype + " allData allocated: " + new Section(allData.getShape()));
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    }

    // make concurrent

    // run through all the datasets
    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      DatasetTiled dtiled = (DatasetTiled) vnested;
      Section tiledSection = dtiled.makeVarSection(mainv);
      TileLayout index;
      Array varData;

      try {
        if (!tiledSection.intersects(wantSection))
          continue;

        // read in the desired section of data from this nested dataset
        Section needToRead = tiledSection.intersect(wantSection); // the part we need to read

        if (debug) System.out.println(" tiledSection: " + tiledSection+" from file "+dtiled.getLocation());
        if (debug) System.out.println(" intersection: " + needToRead);

        Section localNeed = needToRead.shiftOrigin(tiledSection); // shifted to the tiled section
        varData = dtiled.read(mainv, cancelTask, localNeed.getRanges());

        index = new TileLayout(needToRead, wantSection);

      } catch (InvalidRangeException e) {
        throw new IllegalArgumentException(e.getMessage());
      }

      while (index.hasNext()) {
        try {
          Array.arraycopy(varData, index.srcPos, allData, index.resultPos, index.nelems);
        } catch (RuntimeException e) {
          System.out.println(" tiledSection: " + tiledSection);
          System.out.println(index.toString());
          throw e;
        }
      }

      // covers the case of coordinate variables for a 1 row or 1 col tiling.
      // doesnt eliminate duplicate reading in general
      if (varData.getSize() == mainv.getSize()) break;

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    return allData;
  }

  private class TileLayout {
    //Section dataSection, resultSection;
    private int srcPos = 0, resultPos, nelems;
    private int total, startElem;
    Index index;

    TileLayout(Section localSection, Section wantSection) throws InvalidRangeException {
      Section dataSection = localSection.compact();
      Section resultSection = wantSection.compact();
      System.out.println(" resultSection: " + resultSection);
      System.out.println(" dataSection: " + dataSection);

      int rank = dataSection.getRank();

      // total elements to transfer
      total = (int) dataSection.computeSize();

      // figure out the offset
      long product = 1;
      startElem = 0; // offset in want
      for (int ii = rank - 1; ii >= 0; ii--) {
        int d = dataSection.getOrigin(ii) - resultSection.getOrigin(ii);
        if (d > 0) startElem += product * d;
        product *= resultSection.getShape(ii);
      }
      resultPos = startElem;

      // we will use an Index object to keep track of the chunks
      // last range length is nelems; reduce index rank
      nelems = localSection.getShape(rank - 1);
      int[] stride = new int[rank - 1];
      int[] shape = new int[rank - 1];

      product = resultSection.getShape(rank - 1);
      for (int ii = rank - 2; ii >= 0; ii--) {
        stride[ii] = (int) product;
        shape[ii] = dataSection.getShape(ii);
        product *= resultSection.getShape(ii);
      }
      index = new Index(shape, stride);
    }

    boolean first = true;

    boolean hasNext() {
      if (first) {
        first = false;
        return true;
      }

      srcPos += nelems;
      if (srcPos >= total)
        return false;

      index.incr();
      resultPos = startElem + index.currentElement();
      return true;
    }

    public String toString() {
      return "  nElems: " + nelems + " srcPos: " + srcPos + " resultPos: " + resultPos;
    }
  }

  @Override
  protected Dataset makeDataset(String cacheName, String location, String ncoordS, String coordValueS, String sectionSpec,
          NetcdfDataset.EnhanceMode enhance, ucar.nc2.util.cache.FileFactory reader) {
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
    protected DatasetTiled(String cacheName, String location, String sectionSpec, NetcdfDataset.EnhanceMode enhance,
            ucar.nc2.util.cache.FileFactory reader) {
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
