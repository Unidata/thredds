// $Id: GeoGrid.java 68 2006-07-13 00:08:20Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.dataset.grid;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.util.NamedObject;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.unidata.util.Format;
import ucar.unidata.geoloc.*;

import java.util.*;
import java.io.IOException;

/**
 * A georeferencing "gridded" VariableEnhanced, that has a GridCoordSys.
 * In VisAD data model, it is a sampled Field.
 * The dimension are put into canonical order: (t, z, y, x).
 *
 * <p> Implementation note:
 * If the Horizontal axes are 2D, the x and y dimensions are arbitrarily chosen to be
 *   gcs.getXHorizAxis().getDimension(1), gcs.getXHorizAxis().getDimension(0) respectively.
 *
 * <p> Note: these classes should be considered experimental and will likely be refactored in the next release.
 *
 * @author caron
 * @version $Revision: 68 $ $Date: 2006-07-13 00:08:20Z $
 */

public class GeoGrid implements NamedObject, ucar.nc2.dt.GridDatatype {

  private GridDataset dataset;
  private GridCoordSys gcs;
  private VariableEnhanced vs;
  private int xDimOrgIndex = -1, yDimOrgIndex = -1, zDimOrgIndex = -1, tDimOrgIndex = -1, eDimOrgIndex = -1, rtDimOrgIndex = -1;
  private int xDimNewIndex = -1, yDimNewIndex = -1, zDimNewIndex = -1, tDimNewIndex = -1, eDimNewIndex = -1, rtDimNewIndex = -1;
  private ArrayList mydims;

  private boolean debugArrayShape = false;

  /**
   * Constructor.
   * @param dataset belongs to this dataset
   * @param dsvar wraps this Variable
   * @param gcs has this grid coordinate system
   */
  public GeoGrid(GridDataset dataset, VariableEnhanced dsvar, GridCoordSys gcs) {
    this.dataset = dataset;
    this.vs = dsvar;
    this.gcs = gcs;

    CoordinateAxis xaxis = gcs.getXHorizAxis();
    if (xaxis instanceof CoordinateAxis1D) {
      xDimOrgIndex = findDimension( gcs.getXHorizAxis().getDimension(0));
      yDimOrgIndex = findDimension( gcs.getYHorizAxis().getDimension(0));

    } else { // 2D case
      yDimOrgIndex = findDimension( gcs.getXHorizAxis().getDimension(0));
      xDimOrgIndex = findDimension( gcs.getXHorizAxis().getDimension(1));
    }

    if (gcs.getVerticalAxis() != null) zDimOrgIndex = findDimension( gcs.getVerticalAxis().getDimension(0));
    if (gcs.getTimeAxis() != null) {
      if (gcs.getTimeAxis1D() != null)
        tDimOrgIndex = findDimension( gcs.getTimeAxis1D().getDimension(0));
      else
        tDimOrgIndex = findDimension( gcs.getTimeAxis().getDimension(1));
    }
    if (gcs.getEnsembleAxis() != null) eDimOrgIndex = findDimension( gcs.getEnsembleAxis().getDimension(0));
    if (gcs.getRunTimeAxis() != null) rtDimOrgIndex = findDimension( gcs.getRunTimeAxis().getDimension(0));

    // construct canonical dimension list
    int count = 0;
    this.mydims = new ArrayList();
    if (rtDimOrgIndex >= 0) {
      mydims.add( dsvar.getDimension( rtDimOrgIndex));
      rtDimNewIndex = count++;
    }
    if (eDimOrgIndex >= 0) {
      mydims.add( dsvar.getDimension( eDimOrgIndex));
      eDimNewIndex = count++;
    }
    if (tDimOrgIndex >= 0) {
      mydims.add( dsvar.getDimension( tDimOrgIndex));
      tDimNewIndex = count++;
    }
    if (zDimOrgIndex >= 0) {
      mydims.add( dsvar.getDimension( zDimOrgIndex));
      zDimNewIndex = count++;
    }
    if (yDimOrgIndex >= 0) {
      mydims.add( dsvar.getDimension( yDimOrgIndex));
      yDimNewIndex = count++;
    }
    if (xDimOrgIndex >= 0) {
      mydims.add( dsvar.getDimension( xDimOrgIndex));
      xDimNewIndex = count;
    }
  }

  private int findDimension( Dimension want) {
    java.util.List dims = vs.getDimensions();
    for (int i=0; i < dims.size();i ++) {
      Dimension d = (Dimension) dims.get(i);
      if (d.equals( want))
        return i;
    }
    return -1;
  }

  /**
   * Returns an ArrayList containing the dimensions used by this geoGrid.
   * The dimension are put into canonical order: (t, z, y, x). Note that the z and t dimensions are optional.
   * If the Horizontal axes are 2D, the x and y dimensions are arbitrarily chosen to be
   *   gcs.getXHorizAxis().getDimension(1), gcs.getXHorizAxis().getDimension(0), respectively.

   * @return List with objects of type Dimension, in canonical order.
   */
  public java.util.List getDimensions() { return new ArrayList(mydims); }

  /** get the ith dimension
   * @param i : which dimension
   * @return ith Dimension
   */
  public Dimension getDimension(int i) {
    if ((i < 0) || (i >= mydims.size())) return null;
    return (Dimension) mydims.get(i);
  }

  /** get the time Dimension, if it exists */
  public Dimension getTimeDimension() { return tDimNewIndex < 0 ? null : getDimension( tDimNewIndex); }
  /** get the z Dimension, if it exists */
  public Dimension getZDimension() { return zDimNewIndex < 0 ? null : getDimension( zDimNewIndex); }
  /** get the y Dimension, if it exists */
  public Dimension getYDimension() { return yDimNewIndex < 0 ? null : getDimension( yDimNewIndex); }
  /** get the x Dimension, if it exists */
  public Dimension getXDimension() { return xDimNewIndex < 0 ? null : getDimension( xDimNewIndex); }
  /** get the ensemble Dimension, if it exists */
  public Dimension getEnsembleDimension() { return eDimNewIndex < 0 ? null : getDimension( eDimNewIndex); }
  /** get the run time Dimension, if it exists */
  public Dimension getRunTimeDimension() { return rtDimNewIndex < 0 ? null : getDimension( rtDimNewIndex); }

  /** get the time Dimension index in the geogrid (canonical order), or -1 if none */
  public int getTimeDimensionIndex() { return tDimNewIndex; }
  /** get the z Dimension index in the geogrid (canonical order), or -1 if none */
  public int getZDimensionIndex() { return zDimNewIndex; }
  /** get the y Dimension index in the geogrid (canonical order) */
  public int getYDimensionIndex() { return yDimNewIndex; }
  /** get the x Dimension index in the geogrid (canonical order) */
  public int getXDimensionIndex() { return xDimNewIndex; }
  /** get the ensemble Dimension index in the geogrid (canonical order) */
  public int getEDimensionIndex() { return eDimNewIndex; }
  /** get the runtime Dimension index in the geogrid (canonical order) */
  public int getRunTimeDimensionIndex() { return rtDimNewIndex; }

  /**
   * Convenience function; lookup Attribute by name.
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttributeIgnoreCase(String name) {
    return vs.findAttributeIgnoreCase(name);
  }

  /**
   * Convenience function; lookup Attribute value by name. Must be String valued
   * @param attName name of the attribute
   * @param defaultValue if not found, use this as the default
   * @return Attribute string value, or default if not found.
   */
  public String findAttValueIgnoreCase( String attName, String defaultValue) {
    return dataset.getNetcdfDataset().findAttValueIgnoreCase((Variable) vs, attName, defaultValue);
  }

  // implementation of GridDatatype interface

  /** get the rank */
  public int getRank() { return vs.getRank(); }

  /** get the shape */
  public int[] getShape() {
    int[] shape = new int[mydims.size()];
    for (int i = 0; i < mydims.size(); i++) {
      Dimension d = (Dimension) mydims.get(i);
      shape[i] = d.getLength();
    }
    return shape;
  }

  /** get the data type */
  public DataType getDataType() { return vs.getDataType(); }

  public List getAttributes() { return vs.getAttributes(); }

  public VariableEnhanced getVariable() { return vs; }

  /** get the name of the geoGrid.*/
  public String getName() { return vs.getName(); }
  /** get the GridCoordSys for this GeoGrid. */
  public GridCoordSys getCoordinateSystem() { return gcs; }
  public GridCoordSystem getGridCoordSystem() { return gcs; }

  /** get the Projection. */
  public ProjectionImpl  getProjection() { return gcs.getProjection(); }

    /** ArrayList of thredds.util.NamedObject, from the  GridCoordSys. */
  public List getLevels() { return gcs.getLevels(); }

  /** ArrayList of thredds.util.NamedObject, from the  GridCoordSys. */
  public List getTimes() { return gcs.getTimes(); }

  /** get the standardized description, see VariableStandardized.getDescription() */
  public String getDescription() { return vs.getDescription(); }
  /** get the unit as a string, see VariableStandardized.getUnitString() */
  public String getUnitsString() {
    String units = vs.getUnitsString();
    return (units == null) ? "" : units;
  }

  /** @deprecated use getUnitsString() */
  public java.lang.String getUnitString() {
    return getUnitsString();
  }

  //public ucar.unidata.geoloc.ProjectionImpl getProjection() { return gcs.getProjection(); }

    /** true if there may be missing data, see VariableStandardized.hasMissing() */
  public boolean hasMissingData() { return vs.hasMissing(); }

  /** if val is missing data, see VariableStandardized.isMissingData() */
  public boolean isMissingData(double val) { return vs.isMissing(val); }

  /**
   * Convert (in place) all values in the given array that are considered
   * as "missing" to Float.NaN, according to vs.isMissing(val).
   *
   * @param values input array
   * @return input array, with missing values converted to NaNs.
   */
  public float[] setMissingToNaN (float[] values) {
    if (!vs.hasMissing()) return values;
    final int length = values.length;
    for (int i = 0; i < length; i++) {
      double value = values[i];
      if (vs.isMissing( value ))
        values[i] = Float.NaN;
    }
    return values;
  }

  /**
   * Get the minimum and the maximum data value of the previously read Array,
   *  skipping missing values as defined by isMissingData(double val).
   * @param a Array to get min/max values
   * @return both min and max value.
   */
  public MAMath.MinMax getMinMaxSkipMissingData( Array a) {
    if (!hasMissingData())
      return MAMath.getMinMax( a);

    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (isMissingData(val))
        continue;
      if (val > max)
        max = val;
      if (val < min)
        min = val;
    }
    return new MAMath.MinMax(min, max);
  }

  /**
   * Reads in the data "volume" at the given time index.
   * If its a product set, put into canonical order (z-y-x).
   * If not a product set, reorder to (z,i,j), where i, j are from the
   * original
   *
   * @param t time index; ignored if no time axis.
   * @return data[z,y,x] or data[y,x] if no z axis.
   */
  public Array readVolumeData(int t) throws java.io.IOException {
    //if (gcs.isProductSet())
      return readDataSlice(t, -1, -1, -1);
    /* else { // 2D XY
      int rank = vs.getRank();
      int[] shape = vs.getShape();
      int [] start = new int[rank];

      CoordinateAxis taxis = gcs.getTimeAxis();
      if (taxis != null) {
        if ((t >= 0) && (t < taxis.getSize()))
          shape[ tDim] = 1;  // fix t
          start[ tDim] = t;
      }

      if (debugArrayShape) {
        System.out.println("getDataVolume shape = ");
        for (int i=0; i<rank; i++)
          System.out.println("   start = "+start[i]+" shape = "+ shape[i]);
      }

      Array dataVolume;
      try {
        dataVolume = vs.read( start, shape);
      } catch (Exception e) {
        System.out.println("Exception: GeoGridImpl.getdataSlice() on dataset "+getName());
        e.printStackTrace();
        throw new java.io.IOException(e.getMessage());
      }

      // no reordering FIX
      return dataVolume.reduce();
    } */
  }

  /**
   * Reads a Y-X "horizontal slice" at the given time and vertical index.
   * If its a product set, put into canonical order (y-x).
   *
   * @param t time index; ignored if no time axis.
   * @param z vertical index; ignored if no z axis.
   * @return data[y,x]
   */
  public Array readYXData(int t, int z) throws java.io.IOException {
    return readDataSlice(t, z, -1, -1);
  }


  /**
   * Reads a Z-Y "vertical slice" at the given time and x index.
   * If its a product set, put into canonical order (z-y).
   *
   * @param t time index; ignored if no time axis.
   * @param x x index; ignored if no x axis.
   * @return data[z,y]
   */
  public Array readZYData(int t, int x) throws java.io.IOException {
    return readDataSlice(t, -1, -1, x);
  }

  /**
   * @deprecated use readDataSlice
   */
  public Array getDataSlice(int t, int z, int y, int x) throws java.io.IOException {
    return readDataSlice( t, z, y, x);
  }

  /**
   * This reads an arbitrary data slice, returning the data in
   * canonical order (t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param t if < 0, get all of time dim; if valid index, fix slice to that value.
   * @param z if < 0, get all of z dim; if valid index, fix slice to that value.
   * @param y if < 0, get all of y dim; if valid index, fix slice to that value.
   * @param x if < 0, get all of x dim; if valid index, fix slice to that value.
   *
   * @return data[t,z,y,x], eliminating missing or fixed dimension.
   */
  public Array readDataSlice(int t, int z, int y, int x) throws java.io.IOException {
    return readDataSlice(0, 0, t, z, y, x);
  }

  /**
   * This reads an arbitrary data slice, returning the data in
   * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param rt if < 0, get all of runtime dim; if valid index, fix slice to that value.
   * @param e if < 0, get all of ensemble dim; if valid index, fix slice to that value.
   * @param t if < 0, get all of time dim; if valid index, fix slice to that value.
   * @param z if < 0, get all of z dim; if valid index, fix slice to that value.
   * @param y if < 0, get all of y dim; if valid index, fix slice to that value.
   * @param x if < 0, get all of x dim; if valid index, fix slice to that value.
   *
   * @return data[rt,e,t,z,y,x], eliminating missing or fixed dimension.
   */
  public Array readDataSlice(int rt, int e, int t, int z, int y, int x) throws java.io.IOException {

    int rank = vs.getRank();
    int [] start = new int[rank];
    int [] shape = new int[rank];
    for (int i=0; i<rank; i++) {
      start[i] = 0;
      shape[i] = 1;
    }
    Dimension xdim = getXDimension( );
    Dimension ydim = getYDimension( );
    Dimension zdim = getZDimension( );
    Dimension tdim = getTimeDimension( );
    Dimension edim = getEnsembleDimension( );
    Dimension rtdim = getRunTimeDimension( );

      // construct the shape of the data volume to be read
    if (rtdim != null) {
      if ((rt >= 0) && (rt < rtdim.getLength()))
        start[ rtDimOrgIndex] = rt;  // fix rt
      else {
        shape[ rtDimOrgIndex] = rtdim.getLength(); // all of rt
      }
    }

    if (edim != null) {
      if ((e >= 0) && (e < edim.getLength()))
        start[ eDimOrgIndex] = e;  // fix e
      else {
        shape[ eDimOrgIndex] = edim.getLength(); // all of e
      }
    }

    if (tdim != null) {
      if ((t >= 0) && (t < tdim.getLength()))
        start[ tDimOrgIndex] = t;  // fix t
      else {
        shape[ tDimOrgIndex] = tdim.getLength(); // all of t
      }
    }

    if (zdim != null) {
      if ((z >= 0) && (z < zdim.getLength()))
        start[ zDimOrgIndex] = z;  // fix z
      else {
        shape[ zDimOrgIndex] = zdim.getLength(); // all of z
      }
    }

    if (ydim != null) {
      if ((y >= 0) && (y < ydim.getLength()))
        start[ yDimOrgIndex] = y;  // fix y
      else {
        shape[ yDimOrgIndex] = ydim.getLength(); // all of y
      }
    }

    if (xdim != null) {
      if ((x >= 0) && (x < xdim.getLength())) // all of x
        start[ xDimOrgIndex] = x;  // fix x
      else {
        shape[ xDimOrgIndex] = xdim.getLength(); // all of x
      }
    }

    if (debugArrayShape) {
      System.out.println("read shape from org variable = ");
      for (int i=0; i<rank; i++)
        System.out.println("   start = "+start[i]+" shape = "+ shape[i] +" name = "+vs.getDimension(i).getName());
    }

    // read it
    Array dataVolume;
    try {
      dataVolume = vs.read( start, shape);
    } catch (Exception ex) {
      System.out.println("Exception: GeoGrid.getdataSlice() on dataset "+getName());
      ex.printStackTrace();
      throw new java.io.IOException(ex.getMessage());
    }

    // LOOK: the real problem is the lack of named dimensions in the Array object
    // figure out correct permutation for canonical ordering for permute
    ArrayList oldDims = new ArrayList( vs.getDimensions());
    int [] permuteIndex = new int[dataVolume.getRank()];
    int count = 0;
    if (oldDims.contains(rtdim)) permuteIndex[count++] = oldDims.indexOf(rtdim);
    if (oldDims.contains(edim)) permuteIndex[count++] = oldDims.indexOf(edim);
    if (oldDims.contains(tdim)) permuteIndex[count++] = oldDims.indexOf(tdim);
    if (oldDims.contains(zdim)) permuteIndex[count++] = oldDims.indexOf(zdim);
    if (oldDims.contains(ydim)) permuteIndex[count++] = oldDims.indexOf(ydim);
    if (oldDims.contains(xdim)) permuteIndex[count] = oldDims.indexOf(xdim);

    if (debugArrayShape) {
      System.out.println("oldDims = ");
      for (int i=0; i<oldDims.size(); i++)
        System.out.println("   oldDim = "+((Dimension)oldDims.get(i)).getName());
      System.out.println("permute dims = ");
      for (int i=0; i<permuteIndex.length; i++)
        System.out.println("   oldDim index = "+permuteIndex[i]);
    }

    // permute to the order rt,e,t,z,y,x
    dataVolume = dataVolume.permute(permuteIndex);

    // eliminate fixed dimensions, but not all dimensions of length 1.
    count = 0;
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


  //////////////////////////////////

  /**
   * Create a new GeoGrid that is a logical subset of this GeoGrid.
   *
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param bbox a lat/lon bounding box, or null if you want all x,y
   * @param z_stride use only if z_range is null, then take all z with this stride (1 means all)
   * @param y_stride use this stride on the y coordinate (1 means all)
   * @param x_stride use this stride on the x coordinate (1 means all)
   *
   * @return subsetted GeoGrid
   * @throws InvalidRangeException
   */
  public GeoGrid subset(Range t_range, Range z_range, LatLonRect bbox, int z_stride, int y_stride, int x_stride) throws InvalidRangeException  {

    if ((z_range == null) && (z_stride > 1)) {
      Dimension zdim = getZDimension();
      if (zdim != null)
        z_range = new Range( 0, zdim.getLength()-1, z_stride);
    }

    Range y_range = null, x_range = null;
    if (bbox != null) {
      List yx_ranges = gcs.getRangesFromLatLonRect(bbox);
      y_range = (Range) yx_ranges.get(0);
      x_range = (Range) yx_ranges.get(1);
    }

    if (y_stride > 1) {
      if (y_range == null) {
        Dimension ydim = getYDimension();
        y_range = new Range( 0, ydim.getLength()-1, y_stride);
      } else {
        y_range= new Range( y_range.first(), y_range.last(), y_stride);
      }
    }

    if (x_stride > 1) {
      if (x_range == null) {
        Dimension xdim = getXDimension();
        x_range = new Range( 0, xdim.getLength()-1, x_stride);
      } else {
        x_range= new Range( x_range.first(), x_range.last(), x_stride);
      }
    }

    return subset( t_range, z_range, y_range, x_range);
  }

  public GridDatatype makeSubset(Range t_range, Range z_range, LatLonRect bbox, int z_stride, int y_stride, int x_stride) throws InvalidRangeException  {
    return subset(t_range, z_range, bbox, z_stride, y_stride, x_stride);
  }

  /**
   * Create a new GeoGrid that is a logical subset of this GeoGrid.
   *
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param y_range subset the y dimension, or null if you want all of it
   * @param x_range subset the x dimension, or null if you want all of it
   *
   * @return subsetted GeoGrid
   * @throws InvalidRangeException
   */
  public GeoGrid subset(Range t_range, Range z_range, Range y_range, Range x_range) throws InvalidRangeException  {
    // get the ranges list
    int rank = getRank();
    Range[] ranges = new Range[rank];
    if (null != getXDimension())
      ranges[xDimOrgIndex] = x_range;
    if (null != getYDimension())
      ranges[yDimOrgIndex] = y_range;
    if (null != getZDimension())
      ranges[zDimOrgIndex] = z_range;
    if (null != getTimeDimension())
      ranges[tDimOrgIndex] = t_range;
    List rangesList = Arrays.asList( ranges);

    // subset the variable
    VariableDS v_section = (VariableDS) vs.section( rangesList);
    List dims = v_section.getDimensions();
    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = (Dimension) dims.get(j);
      dim.setShared( true); // make them shared (section will make them unshared)
    }

    // subset the axes in the GridCoordSys
    GridCoordSys gcs_section = new GridCoordSys( gcs, t_range, z_range, y_range, x_range);

    // now we can make the geogrid
    return new GeoGrid( dataset, v_section, gcs_section);
  }

  public GridDatatype makeSubset(Range rt_range, Range e_range, Range t_range, Range z_range, Range y_range, Range x_range) throws InvalidRangeException  {
    return subset(t_range, z_range, y_range, x_range);
  }

  /** experimental - do not use */
  public void writeFile( String filename) throws IOException {
    FileWriter writer = new FileWriter( filename, true);

    ArrayList varList = new ArrayList();
    varList.add( vs);

    List axes = gcs.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      Variable v = (Variable) axes.get(i);
      varList.add(v);
    }

    writer.writeVariables( varList);

    ProjectionCT projCT = gcs.getProjectionCT();
    if (projCT != null) {
      VariableDS v = CoordTransBuilder.makeDummyTransformVariable(dataset.getNetcdfDataset(), projCT);
      v.addAttribute( new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
      writer.writeVariable( v);
    }

    String location = dataset.getNetcdfDataset().getLocation();
    writer.writeGlobalAttribute( new Attribute("History", "GeoGrid extracted from dataset "+location));
    writer.writeGlobalAttribute( new Attribute("Convention", _Coordinate.Convention));
    writer.finish();
  }

  /////////////////////////////////////////////////////////////////////////////////
  /**
   * Instances which have same name and coordinate system are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof GeoGrid))
      return false;

    GeoGrid d = (GeoGrid) oo;
    // if (!dataset.getName().equals(d.getDataset().getName())) return false;
    if (!getName().equals(d.getName())) return false;

    return getCoordinateSystem().equals(d.getCoordinateSystem());
  }

  /**
   * Override Object.hashCode() to be consistent with equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      // result = 37*result + dataset.getName().hashCode();
      result = 37*result + getName().hashCode();
      result = 37*result + getCoordinateSystem().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

  /** string representation */
  public String toString() { return getName(); }

  /** nicely formatted information */
  public String getInfo() {
    StringBuffer buf = new StringBuffer(200);
    buf.setLength(0);
    buf.append(getName());
    Format.tab(buf, 15, true);
    buf.append(getUnitsString());
    Format.tab(buf, 30, true);
    buf.append(hasMissingData());
    Format.tab(buf, 40, true);
    buf.append(getDescription());
    return buf.toString();
  }

}