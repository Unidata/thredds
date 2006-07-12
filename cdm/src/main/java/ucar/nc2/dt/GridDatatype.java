// $Id: $
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

package ucar.nc2.dt;

import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.ma2.MAMath;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;

/**
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public interface GridDatatype {
  public String getName();
  public String getDescription();
  public String getUnitsString();

  public DataType getDataType();
  public int getRank();
  public int[] getShape();

  public List getAttributes();
  public String findAttValueIgnoreCase(String p0, String p1);
  public Attribute findAttributeIgnoreCase(String p0);

  public List getDimensions();
  public Dimension getDimension(int p0);
  public Dimension getXDimension();
  public int getXDimensionIndex();
  public Dimension getYDimension();
  public int getYDimensionIndex();
  public Dimension getZDimension();
  public int getZDimensionIndex();
  public Dimension getTimeDimension();
  public int getTimeDimensionIndex();

  public GridCoordSystem getGridCoordSystem();
  //public List getLevels();
  public ProjectionImpl getProjection();
  //public List getTimes();

  public boolean hasMissingData();
  public boolean isMissingData(double vl);
  public MAMath.MinMax getMinMaxSkipMissingData(Array data);
  public float[] setMissingToNaN(float[] data);

  /**
   * This reads an arbitrary data slice, returning the data in
   * canonical order (t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param rt_index if < 0, get all of runtime dim; if valid index, fix slice to that value.
   * @param e_index if < 0, get all of ensemble dim; if valid index, fix slice to that value.
   * @param t_index if < 0, get all of time dim; if valid index, fix slice to that value.
   * @param z_index if < 0, get all of z dim; if valid index, fix slice to that value.
   * @param y_index if < 0, get all of y dim; if valid index, fix slice to that value.
   * @param x_index if < 0, get all of x dim; if valid index, fix slice to that value.
   *
   * @return data[t,z,y,x], eliminating missing or fixed dimension.
   */
  public Array readDataSlice(int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index) throws java.io.IOException;

  public Array readDataSlice(int t_index, int z_index, int y_index, int x_index) throws java.io.IOException;
  public Array readVolumeData(int t_index) throws java.io.IOException;
  /* public Array readYXData(int p0, int p1) throws java.io.IOException;
  public Array readZYData(int p0, int p1) throws java.io.IOException; */

  /**
   * Create a new GeoGrid that is a logical subset of this GeoGrid.
   *
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param y_range subset the y dimension, or null if you want all of it
   * @param x_range subset the x dimension, or null if you want all of it
   *
   * @return subsetted GeoGrid
   */
  public GridDatatype makeSubset(Range t_range, Range z_range, Range y_range, Range x_range) throws ucar.ma2.InvalidRangeException;
  public GridDatatype makeSubset(Range p0, Range p1, LatLonRect p2, int p3, int p4, int p5) throws ucar.ma2.InvalidRangeException;

  public String getInfo();
  public VariableEnhanced getVariable();
}
