/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage.adapter;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VariableDS;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.util.Format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * fork ucar.nc2.dt.grid.GeoGrid for adaption of GridCoverage.
 * Minimalist, does not do make a "logical GeoGrid subset"
 * LOOK maybe can only be used for GRID ?
 *
 * @author caron
 * @since 5/26/2015
 */
public class DtCoverage implements IsMissingEvaluator {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DtCoverage.class);
  static private final boolean debugArrayShape = false;

  private final DtCoverageDataset dataset;
  private final DtCoverageCS gcs;
  private final VariableDS vs;
  private int xDimOrgIndex = -1, yDimOrgIndex = -1, zDimOrgIndex = -1, tDimOrgIndex = -1, eDimOrgIndex = -1, rtDimOrgIndex = -1;
  private int xDimNewIndex = -1, yDimNewIndex = -1, zDimNewIndex = -1, tDimNewIndex = -1, eDimNewIndex = -1, rtDimNewIndex = -1;
  private final List<Dimension> mydims;

  /**
   * Constructor.
   *
   * @param dataset belongs to this dataset
   * @param dsvar   wraps this Variable
   * @param gcs     has this grid coordinate system
   */
  public DtCoverage(DtCoverageDataset dataset, DtCoverageCS gcs, VariableDS dsvar) {
    this.dataset = dataset;
    this.vs = dsvar;
    this.gcs = gcs;

    CoordinateAxis xaxis = gcs.getXHorizAxis();
    if (xaxis instanceof CoordinateAxis1D) {
      xDimOrgIndex = findDimension(xaxis.getDimension(0));
      yDimOrgIndex = findDimension(gcs.getYHorizAxis().getDimension(0));

    } else { // 2D case
      //coverity[COPY_PASTE_ERROR]
      yDimOrgIndex = findDimension(gcs.getXHorizAxis().getDimension(0));
      xDimOrgIndex = findDimension(gcs.getXHorizAxis().getDimension(1));
    }

    if (gcs.getVerticalAxis() != null) zDimOrgIndex = findDimension(gcs.getVerticalAxis().getDimension(0));
    if (gcs.getTimeAxis() != null) {
      if (gcs.getTimeAxis().getRank() == 1)
        tDimOrgIndex = findDimension(gcs.getTimeAxis().getDimension(0));
      else
        tDimOrgIndex = findDimension(gcs.getTimeAxis().getDimension(1));
      // deal with swath where time not independent dimension LOOK rewrite ??
      if ((tDimOrgIndex == yDimOrgIndex) || (tDimOrgIndex == xDimOrgIndex)) {
        tDimOrgIndex = -1;
      }
    }
    if (gcs.getEnsembleAxis() != null) eDimOrgIndex = findDimension(gcs.getEnsembleAxis().getDimension(0));
    if (gcs.getRunTimeAxis() != null) rtDimOrgIndex = findDimension(gcs.getRunTimeAxis().getDimension(0));

    // construct canonical dimension list
    int count = 0;
    this.mydims = new ArrayList<>();
    if ((rtDimOrgIndex >= 0) && (rtDimOrgIndex != tDimOrgIndex)) {
      mydims.add(dsvar.getDimension(rtDimOrgIndex));
      rtDimNewIndex = count++;
    }
    if (eDimOrgIndex >= 0) {
      mydims.add(dsvar.getDimension(eDimOrgIndex));
      eDimNewIndex = count++;
    }
    if (tDimOrgIndex >= 0) {
      Dimension tdim = dsvar.getDimension(tDimOrgIndex);
      mydims.add(tdim);
      tDimNewIndex = count++;
    }
    if (zDimOrgIndex >= 0) {
      mydims.add(dsvar.getDimension(zDimOrgIndex));
      zDimNewIndex = count++;
    }
    if (yDimOrgIndex >= 0) {
      mydims.add(dsvar.getDimension(yDimOrgIndex));
      yDimNewIndex = count++;
    }
    if (xDimOrgIndex >= 0) {
      mydims.add(dsvar.getDimension(xDimOrgIndex));
      xDimNewIndex = count;
    }
  }

  private int findDimension(Dimension want) {
    java.util.List dims = vs.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension d = (Dimension) dims.get(i);
      if (d.equals(want))
        return i;
    }
    return -1;
  }

  /**
   * Returns an ArrayList containing the dimensions used by this geoGrid.
   * The dimension are put into canonical order: (rt, e, t, z, y, x). Note that the z and t dimensions are optional.
   * If the Horizontal axes are 2D, the x and y dimensions are arbitrarily chosen to be
   * gcs.getXHorizAxis().getDimension(1), gcs.getXHorizAxis().getDimension(0), respectively.
   *
   * @return List with objects of type Dimension, in canonical order.
   */
  public java.util.List<Dimension> getDimensions() {
    return new ArrayList<>(mydims);
  }

  /**
   * get the ith dimension
   *
   * @param i : which dimension
   * @return ith Dimension
   */
  public Dimension getDimension(int i) {
    if ((i < 0) || (i >= mydims.size())) return null;
    return mydims.get(i);
  }

  /**
   * get the time Dimension, if it exists
   */
  public Dimension getTimeDimension() {
    return tDimNewIndex < 0 ? null : getDimension(tDimNewIndex);
  }

  /**
   * get the z Dimension, if it exists
   */
  public Dimension getZDimension() {
    return zDimNewIndex < 0 ? null : getDimension(zDimNewIndex);
  }

  /**
   * get the y Dimension, if it exists
   */
  public Dimension getYDimension() {
    return yDimNewIndex < 0 ? null : getDimension(yDimNewIndex);
  }

  /**
   * get the x Dimension, if it exists
   */
  public Dimension getXDimension() {
    return xDimNewIndex < 0 ? null : getDimension(xDimNewIndex);
  }

  /**
   * get the ensemble Dimension, if it exists
   */
  public Dimension getEnsembleDimension() {
    return eDimNewIndex < 0 ? null : getDimension(eDimNewIndex);
  }

  /**
   * get the run time Dimension, if it exists
   */
  public Dimension getRunTimeDimension() {
    return rtDimNewIndex < 0 ? null : getDimension(rtDimNewIndex);
  }

  /**
   * get the time Dimension index in the geogrid (canonical order), or -1 if none
   */
  public int getTimeDimensionIndex() {
    return tDimNewIndex;
  }

  /**
   * get the z Dimension index in the geogrid (canonical order), or -1 if none
   */
  public int getZDimensionIndex() {
    return zDimNewIndex;
  }

  /**
   * get the y Dimension index in the geogrid (canonical order)
   */
  public int getYDimensionIndex() {
    return yDimNewIndex;
  }

  /**
   * get the x Dimension index in the geogrid (canonical order)
   */
  public int getXDimensionIndex() {
    return xDimNewIndex;
  }

  /**
   * get the ensemble Dimension index in the geogrid (canonical order)
   */
  public int getEnsembleDimensionIndex() {
    return eDimNewIndex;
  }

  /**
   * get the runtime Dimension index in the geogrid (canonical order)
   */
  public int getRunTimeDimensionIndex() {
    return rtDimNewIndex;
  }

  /**
   * Convenience function; lookup Attribute by name.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttributeIgnoreCase(String name) {
    return vs.findAttributeIgnoreCase(name);
  }

  /**
   * Convenience function; lookup Attribute value by name. Must be String valued
   *
   * @param attName      name of the attribute
   * @param defaultValue if not found, use this as the default
   * @return Attribute string value, or default if not found.
   */
  public String findAttValueIgnoreCase(String attName, String defaultValue) {
    return dataset.getNetcdfDataset().findAttValueIgnoreCase(vs, attName, defaultValue);
  }

  // implementation of GeoGrid interface

  /**
   * get the rank
   */
  public int getRank() {
    return mydims.size();
  }

  /**
   * get the shape
   */
  public int[] getShape() {
    int[] shape = new int[mydims.size()];
    for (int i = 0; i < mydims.size(); i++) {
      Dimension d = mydims.get(i);
      shape[i] = d.getLength();
    }
    return shape;
  }

  /**
   * get the data type
   */
  public DataType getDataType() {
    return vs.getDataType();
  }

  public List<Attribute> getAttributes() {
    return vs.getAttributes();
  }

  public VariableDS getVariable() {
    return vs;
  }

  public String getFullName() {
    return vs.getFullName();
  }

  public String getName() {
    return vs.getFullName();
  }

  public String getShortName() {
    return vs.getShortName();
  }

  /**
   * get the GeoGridCoordSys for this GeoGrid.
   */
  public DtCoverageCS getCoordinateSystem() {
    return gcs;
  }

  /**
   * get the Projection.
   */
  public ProjectionImpl getProjection() {
    return gcs.getProjection();
  }

  /**
   * get the standardized description, or null if none.
   */
  public String getDescription() {
    return vs.getDescription();
  }

  /**
   * get the unit as a string
   */
  public String getUnitsString() {
    String units = vs.getUnitsString();
    return (units == null) ? "" : units;
  }


  //public ucar.unidata.geoloc.ProjectionImpl getProjection() { return gcs.getProjection(); }

  /**
   * true if there may be missing data, see VariableDS.hasMissing()
   */
  public boolean hasMissingData() {
    return vs.hasMissing();
  }

  /**
   * if val is missing data, see VariableDS.isMissingData()
   */
  public boolean isMissingData(double val) {
    return vs.isMissing(val);
  }

  public boolean hasMissing() {
    return vs.hasMissing();
  }

  public boolean isMissing(double val) {
    return vs.isMissing(val);
  }

  /**
   * Convert (in place) all values in the given array that are considered
   * as "missing" to Float.NaN, according to isMissingData(val).
   *
   * @param values input array
   * @return input array, with missing values converted to NaNs.
   */
  public float[] setMissingToNaN(float[] values) {
    if (!vs.hasMissing()) return values;
    final int length = values.length;
    for (int i = 0; i < length; i++) {
      double value = values[i];
      if (vs.isMissing(value))
        values[i] = Float.NaN;
    }
    return values;
  }

  /**
   * Get the minimum and the maximum data value of the previously read Array,
   * skipping missing values as defined by isMissingData(double val).
   *
   * @param a Array to get min/max values
   * @return both min and max value.
   */
  public MAMath.MinMax getMinMaxSkipMissingData(Array a) {
    return MAMath.getMinMaxSkipMissingData(a, this);
  }

  /**
   * This reads an arbitrary data slice, returning the data in
   * canonical order (t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param t if < 0, get all of time dim; if valid index, fix slice to that value.
   * @param z if < 0, get all of z dim; if valid index, fix slice to that value.
   * @param y if < 0, get all of y dim; if valid index, fix slice to that value.
   * @param x if < 0, get all of x dim; if valid index, fix slice to that value.
   * @return data[t, z, y, x], eliminating missing or fixed dimension.
   */
  public Array readDataSlice(int t, int z, int y, int x) throws java.io.IOException {
    return readDataSlice(0, 0, t, z, y, x);
  }

  /**
   * This reads an arbitrary data slice, returning the data in
   * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param rt if < 0, get all of runtime dim; if valid index, fix slice to that value.
   * @param e  if < 0, get all of ensemble dim; if valid index, fix slice to that value.
   * @param t  if < 0, get all of time dim; if valid index, fix slice to that value.
   * @param z  if < 0, get all of z dim; if valid index, fix slice to that value.
   * @param y  if < 0, get all of y dim; if valid index, fix slice to that value.
   * @param x  if < 0, get all of x dim; if valid index, fix slice to that value.
   *
   * @return data[rt, e, t, z, y, x], eliminating missing or fixed dimension.
   */
  public Array readDataSlice(int rt, int e, int t, int z, int y, int x) throws java.io.IOException {

    int rank = vs.getRank();
    int[] start = new int[rank];
    int[] shape = new int[rank];
    for (int i = 0; i < rank; i++) {
      start[i] = 0;
      shape[i] = 1;
    }
    Dimension xdim = getXDimension();
    Dimension ydim = getYDimension();
    Dimension zdim = getZDimension();
    Dimension tdim = getTimeDimension();
    Dimension edim = getEnsembleDimension();
    Dimension rtdim = getRunTimeDimension();

    // construct the shape of the data volume to be read
    if (rtdim != null) {
      if ((rt >= 0) && (rt < rtdim.getLength()))
        start[rtDimOrgIndex] = rt;  // fix rt
      else {
        shape[rtDimOrgIndex] = rtdim.getLength(); // all of rt
      }
    }

    if (edim != null) {
      if ((e >= 0) && (e < edim.getLength()))
        start[eDimOrgIndex] = e;  // fix e
      else {
        shape[eDimOrgIndex] = edim.getLength(); // all of e
      }
    }

    if (tdim != null) {
      if ((t >= 0) && (t < tdim.getLength()))
        start[tDimOrgIndex] = t;  // fix t
      else {
        shape[tDimOrgIndex] = tdim.getLength(); // all of t
      }
    }

    if (zdim != null) {
      if ((z >= 0) && (z < zdim.getLength()))
        start[zDimOrgIndex] = z;  // fix z
      else {
        shape[zDimOrgIndex] = zdim.getLength(); // all of z
      }
    }

    if (ydim != null) {
      if ((y >= 0) && (y < ydim.getLength()))
        start[yDimOrgIndex] = y;  // fix y
      else {
        shape[yDimOrgIndex] = ydim.getLength(); // all of y
      }
    }

    if (xdim != null) {
      if ((x >= 0) && (x < xdim.getLength())) // all of x
        start[xDimOrgIndex] = x;  // fix x
      else {
        shape[xDimOrgIndex] = xdim.getLength(); // all of x
      }
    }

    if (debugArrayShape) {
      System.out.println("read shape from org variable = ");
      for (int i = 0; i < rank; i++)
        System.out.println("   start = " + start[i] + " shape = " + shape[i] + " name = " + vs.getDimension(i).getShortName());
    }

    // read it
    Array dataVolume;
    try {
      dataVolume = vs.read(start, shape);
    } catch (Exception ex) {
      log.error("GeoGrid.getdataSlice() on variable " + vs.getFullName() + " dataset " + dataset.getLocation(), ex);
      throw new java.io.IOException(ex);
    }

    // permute to canonical order if needed; order rt,e,t,z,y,x
    int[] permuteIndex = calcPermuteIndex();
    if (permuteIndex != null)
      dataVolume = dataVolume.permute(permuteIndex);

    // eliminate fixed dimensions, but not all dimensions of length 1.
    int count = 0;
    if (rtdim != null) {
      if (rt >= 0) dataVolume = dataVolume.reduce(count);
      else count++;
    }
    if (edim != null) {
      if (e >= 0) dataVolume = dataVolume.reduce(count);
      else count++;
    }
    if (tdim != null) {
      if (t >= 0) dataVolume = dataVolume.reduce(count);
      else count++;
    }
    if (zdim != null) {
      if (z >= 0) dataVolume = dataVolume.reduce(count);
      else count++;
    }
    if (ydim != null) {
      if (y >= 0) dataVolume = dataVolume.reduce(count);
      else count++;
    }
    if (xdim != null) {
      if (x >= 0) dataVolume = dataVolume.reduce(count);
    }

    return dataVolume;
  }

  /**
   * This reads an arbitrary data section, returning the data in
   * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param subset - each Range must be named by the axisType that its used for. order not important
   *
   * @return data[rt, e, t, z, y, x], eliminating missing dimensions. length=1 not eliminated
   */
  public Array readDataSection(Section subset, boolean canonicalOrder) throws InvalidRangeException, IOException {

    // get the ranges list in the order of the variable; a null range means "all" to vs.read()
    Range[] varRange = new Range[getRank()];
    for (Range r : subset.getRanges()) {
      AxisType type = AxisType.valueOf(r.getName());
      switch (type) {
        case Lon:
        case GeoX:
          varRange[xDimOrgIndex] = r;
          break;
        case Lat:
        case GeoY:
          varRange[yDimOrgIndex] = r;
          break;
        case Height:
        case Pressure:
        case GeoZ:
          varRange[zDimOrgIndex] = r;
          break;
        case Time:
          varRange[tDimOrgIndex] = r;
          break;
        case RunTime:
          varRange[rtDimOrgIndex] = r;
          break;
        case Ensemble:
          varRange[eDimOrgIndex] = r;
          break;
      }
    }

    // read it
    Array dataVolume = vs.read(Arrays.asList(varRange));

    // permute to canonical order if needed; order rt,e,t,z,y,x
    if (canonicalOrder) {
      int[] permuteIndex = calcPermuteIndex();
      if (permuteIndex != null)
        dataVolume = dataVolume.permute(permuteIndex);
    }

    return dataVolume;
  }

  private int[] calcPermuteIndex() throws java.io.IOException {
    Dimension xdim = getXDimension();
    Dimension ydim = getYDimension();
    Dimension zdim = getZDimension();
    Dimension tdim = getTimeDimension();
    Dimension edim = getEnsembleDimension();
    Dimension rtdim = getRunTimeDimension();

    // LOOK: the real problem is the lack of named dimensions in the Array object
    // figure out correct permutation for canonical ordering for permute
    List<Dimension> oldDims = vs.getDimensions();
    int[] permuteIndex = new int[vs.getRank()];
    int count = 0;
    if (oldDims.contains(rtdim)) permuteIndex[count++] = oldDims.indexOf(rtdim);
    if (oldDims.contains(edim)) permuteIndex[count++] = oldDims.indexOf(edim);
    if (oldDims.contains(tdim)) permuteIndex[count++] = oldDims.indexOf(tdim);
    if (oldDims.contains(zdim)) permuteIndex[count++] = oldDims.indexOf(zdim);
    if (oldDims.contains(ydim)) permuteIndex[count++] = oldDims.indexOf(ydim);
    if (oldDims.contains(xdim)) permuteIndex[count] = oldDims.indexOf(xdim);

    if (debugArrayShape) {
      System.out.println("oldDims = ");
      for (Dimension oldDim : oldDims) System.out.println("   oldDim = " + oldDim.getShortName());
      System.out.println("permute dims = ");
      for (int aPermuteIndex : permuteIndex) System.out.println("   oldDim index = " + aPermuteIndex);
    }

    // check to see if we need to permute
    boolean needPermute = false;
    for (int i = 0; i < permuteIndex.length; i++) {
      if (i != permuteIndex[i]) needPermute = true;
    }

    return needPermute ? permuteIndex : null;
  }

  /////////////////////////////////////////////////////////////////////////////////

  /**
   * Instances which have same name and coordinate system are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof DtCoverage))
      return false;

    DtCoverage d = (DtCoverage) oo;
    if (!getFullName().equals(d.getFullName())) return false;
    return getCoordinateSystem().equals(d.getCoordinateSystem());
  }

  /**
   * Override Object.hashCode() to be consistent with equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      // result = 37*result + dataset.getName().hashCode();
      result = 37 * result + getFullName().hashCode();
      result = 37 * result + getCoordinateSystem().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private int hashCode = 0; // Bloch, item 8

  /**
   * string representation
   */
  public String toString() {
    return getFullName();
  }

  /**
   * nicely formatted information
   */
  public String getInfo() {
    StringBuilder buf = new StringBuilder(200);
    buf.setLength(0);
    buf.append(getFullName());
    Format.tab(buf, 30, true);
    buf.append(getUnitsString());
    Format.tab(buf, 60, true);
    buf.append(hasMissingData());
    Format.tab(buf, 66, true);
    buf.append(getDescription());
    return buf.toString();
  }

  public int compareTo(DtCoverage g) {
    return getFullName().compareTo(g.getFullName());
  }
}


