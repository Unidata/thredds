/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt2;

import java.util.List;
import java.util.Date;
import java.io.IOException;

/**
 * Superclass for "point observation" features.
 * The actual data is returned as StructureData, containing one or more member data variables,
 *  all colocated in space and time.
 *
 * The Feature itself contains one or more observations; the subclasses describe their topology.
 *
 * @author caron
 * @since Feb 8, 2008
 */
public interface ObsFeature {

  /**
   * The ID of the feature, unique within the containing collection.
   *
   * @return ID of the feature, may not be null.
   */
  public String getId();

  /**
   * A description of the feature.
   *
   * @return description of the feature, may be null.
   */
  public String getDescription();

  /**
   * The number of observations in the Feature.
   *
   * @return number of observations in the Feature.
   */
  public int getNumberPoints();

  /**
   * Actual time of this Feature at the ith observation.
   *
   * @param pt index of the observation.
   * @return actual time of the observation at the ith point.
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if pt < 0 or >= getNumberPoints()
   */
  public double getObservationTime(int pt) throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Actual time of this Feature at the ith observation, as a Date.
   *
   * @param pt index of the observation.
   * @return actual time of the observation at the ith point, as a Date.
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if pt < 0 or >= getNumberPoints()
   */
  public Date getObservationTimeAsDate(int pt) throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Get the units of time, as a DateUnit.
   * To get a Date, from a time value, call DateUnit.getStandardDate(double value).
   * To get units as a String, call DateUnit.getUnitsString().
   *
   * @return the units of time
   */
  public ucar.nc2.units.DateUnit getTimeUnits();

  /**
   * Get the latitude of the ith observation in units of "degrees_north".
   *
   * @param pt index of the observation.
   * @return the latitude of the ith point in units of "degrees_north".
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if pt < 0 or >= getNumberPoints()
   */
  public double getLatitude(int pt) throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Get the longitude of the ith observation in units of "degrees_east".
   *
   * @param pt index of the observation.
   * @return the longitude of the ith point in units of "degrees_east".
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if pt < 0 or >= getNumberPoints()
   */
  public double getLongitude(int pt) throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Get the z coordinate of the ith observation
   *
   * @param pt index of the observation.
   * @return the z coordinate of the ith point
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if pt < 0 or >= getNumberPoints()
   */
  public double getZcoordinate(int pt) throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * The units of the z coordinate.
   *
   * @return units of the z coordinate as a String.
   */
  public String getZcoordUnits();

  /**
   * Return a list of the data Variables available in this dataset.
   *
   * @return List of type VariableSimpleIF
   */
  public List<ucar.nc2.VariableSimpleIF> getDataVariables();

  /**
   * Get the named data Variable.
   *
   * @param name of data Variable.
   * @return VariableSimpleIF or null if not found.
   */
  public ucar.nc2.VariableSimpleIF getDataVariable(String name);

  /**
   * Get all of the Feature data at one observation.
   *
   * @param pt index of the point.
   * @return the actual data of this observation.
   * @throws java.io.IOException            on read error
   * @throws ucar.ma2.InvalidRangeException if pt out of range
   */
  public ucar.ma2.StructureData getData(int pt) throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Get the data for one member,  at one observation.
   *
   * @param pt         index of the point.
   * @param memberName name of a member of the Structure.
   * @return an Array of the member's data
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if pt < 0 or >= getNumberPoints()
   */
  public ucar.ma2.Array getData(int pt, String memberName) throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Get the data for one member, along the entire Feature.
   *
   * @param memberName name of a member of the Structure.
   * @return an Array of the member's data
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if pt < 0 or >= getNumberPoints()
   */
  public ucar.ma2.Array getData(String memberName) throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Get an efficient iterator over all the data in the Feature in z order. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it. DO NOT KEEP ANY REFERENCES to the
   * dataType object or the StructureData object.
   * <pre>Example for point observations:
   * Iterator iter = profileDataset.getDataIterator(-1);
   * while (iter.hasNext()) {
   *   ProfileFeature pobs = (ProfileFeature) iter.next();
   *   StructureData sdata = pobs.getData();
   *   // process fully
   * }
   * </pre>
   *
   * @param bufferSize if > 0, the internal buffer size, else use the default.
   * @return iterator over type Trajectory in time order.
   * @throws java.io.IOException on i/o error
   */
  public DataIterator getDataIterator(int bufferSize) throws java.io.IOException;

  /**
   * Get estimate of the cost of accessing all the data in the Feature using getDataIterator().
   *
   * @return DataCost or null if not able to estimate.
   */
  public DataCost getDataCost();

}
