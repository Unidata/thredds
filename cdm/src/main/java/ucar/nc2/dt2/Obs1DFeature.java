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

import ucar.nc2.Structure;
import ucar.ma2.StructureData;

import java.util.List;
import java.util.Date;
import java.io.IOException;

/**
 * Superclass for one-dimensional "point observation" features.
 * The actual data is returned as StructureData, containing one or more member data variables,
 *  all colocated in space and time.
 *
 * The Feature itself contains one or more observations; the subclasses describe their topology.
 *
 * @author caron
 * @since Feb 8, 2008
 */
public interface Obs1DFeature extends Feature {

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
   * Get the latitude of the given observation in units of "degrees_north".
   *
   * @param sdata StructureData obtained from getDataIterator()
   * @return the latitude of the ith point in units of "degrees_north".
   */
  public double getLatitude(StructureData sdata);

  /**
   * Get the longitude of the given observation in units of "degrees_east".
   *
   * @param sdata StructureData obtained from getDataIterator()
   * @return the longitude of the ith point in units of "degrees_east".
   */
  public double getLongitude(StructureData sdata);

  /**
   * Get the z coordinate of the given observation
   *
   * @param sdata StructureData obtained from getDataIterator()
   * @return the z coordinate of the ith point
   */
  public double getZcoordinate(StructureData sdata);

  /**
   * The units of the z coordinate.
   *
   * @return units of the z coordinate as a String.
   */
  public String getZcoordUnits();

  /**
    * Actual time of this Feature at the given observation.
    *
    * @param sdata StructureData obtained from getDataIterator()
    * @return actual time of the observation.
    */
   public double getObservationTime(StructureData sdata);

   /**
    * Actual time of this Feature at the given observation, as a Date.
    *
    * @param sdata StructureData obtained from getDataIterator()
    * @return actual time of the observation at the ith point, as a Date.
    */
   public Date getObservationTimeAsDate(StructureData sdata);

   /**
    * Get the units of time, as a DateUnit.
    * To get a Date, from a time value, call DateUnit.getStandardDate(double value).
    * To get units as a String, call DateUnit.getUnitsString().
    *
    * @return the units of time
    */
   public ucar.nc2.units.DateUnit getTimeUnits();


  /**
   * Get the data for one member, at the given observation.
   *
   * @param sdata StructureData obtained from getDataIterator()
   * @param memberName name of a member of the Structure.
   * @return an Array of the member's data
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if pt < 0 or >= getNumberPoints()
   *
  public ucar.ma2.Array getData(StructureData sdata, String memberName) throws IOException, ucar.ma2.InvalidRangeException;
  */

  /**
   * Get the data for one member, along the entire Feature.
   *
   * @param memberName name of a member of the Structure.
   * @return an Array of the member's data
   * @throws java.io.IOException on read error
   *
  public ucar.ma2.Array getData(String memberName) throws IOException; */

  /**
   * Get an efficient iterator over all the data in the Feature.
   * NOTE there is no guarenteed order, no mater what the subclass. LOOK ???
   *
   * You must fully process the data, or copy it out of the StructureData, as you iterate over it.
   * DO NOT KEEP ANY REFERENCES to the dataType object or the StructureData object.
   * <pre>Example for point observations:
   * Iterator iter = profileDataset.getDataIterator(-1);
   * while (iter.hasNext()) {
   *   StructureData sdata = iter.next();
   *   // process fully
   * }
   * </pre>
   *
   * @param bufferSize if > 0, the internal buffer size, else use the default.
   * @return iterator over type StructureData.
   * @throws java.io.IOException on i/o error
   */
  public DataIterator getDataIterator(int bufferSize) throws java.io.IOException;

  /**
   * Make the observation into a PointObsFeature
   * @param sdata StructureData obtained from getDataIterator()
   * @return a PointObsFeature
   */
  public PointObsFeature makePointObsFeature( StructureData sdata);
  /**
   * Get estimate of the cost of accessing all the data in the Feature using getDataIterator().
   *
   * @return DataCost or null if not able to estimate.
   */
  public DataCost getDataCost();

}
