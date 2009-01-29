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
package ucar.nc2.dt;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.VariableSimpleIF;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * A sequence (one-dimensional, variable-length list) of PointObsDatatype, connected in space and time.
 * The observations are ordered in time (in other words, the time dimension must
 * increase monotonically along the trajectory).
 *
 * @author caron
 */
public interface TrajectoryObsDatatype {

    /** The ID of the trajectory, may not be null. */
    public String getId();
    /** A description of the trajectory. */
    public String getDescription();

    /** The number of points along the trajectory. */
    public int getNumberPoints();

  /**
   * Return a list of the data Variables available in this dataset. The list
   * contains only "data variables of interest", not metadata or coordinate
   * system variables, etc.
   *
   * @return List of type TypedDataVariable
   */
  public List getDataVariables();

  /**
   * Get the named data Variable.
   *
   * @param name of data Variable.
   * @return TypedDataVariable or null.
   */
  public VariableSimpleIF getDataVariable( String name );
//  public VariableSimpleIF getTimeDataVariable( String name );
//  public VariableSimpleIF getLatitudeDataVariable( String name );
//  public VariableSimpleIF getLongitudeDataVariable( String name );
//  public VariableSimpleIF getElevationDataVariable( String name );

    /**
     * Get a PointObsDatatype for the requested trajectory point.
     *
     * @param point the point along the trajectory
     * @return corresponding PointObsDatatype
     * @throws IOException
     */
    public PointObsDatatype getPointObsData(int point) throws IOException;

   /////////////////////////////////////////////////////////////
   // all below are convenience routines

     /** Start date for the trajectory. */
    public Date getStartDate();

    /** End date for the trajectory. */
    public Date getEndDate();

    /** BoundingBox for the trajectory. May not be available. */
    public ucar.unidata.geoloc.LatLonRect getBoundingBox();

    /** Return the time for the given point as a java.util.Date. */
    public java.util.Date getTime(int point) throws IOException;
    /** Return the location for the given point. */
    public ucar.unidata.geoloc.EarthLocation getLocation(int point) throws IOException;

    /** Return the time for the given point as a double in the units given by getTimeUnitsIdentifier(). */
    public double getTimeValue(int point) throws IOException;
    /** Return the String representation of the units for time. */
    public String getTimeUnitsIdentifier();
    /** Return the latitude at the requested trajectory point in units of "degrees_north". */
    public double getLatitude(int point) throws IOException;
    /** Return the longitude at the requested trajectory point in units of "degrees_east". */
    public double getLongitude(int point) throws IOException;
    /** Return the elevation at the requested trajectory point in units of meters, missing values = NaN. */
    public double getElevation(int point) throws IOException;

  /**
   * Get values for all parameters (except time, lat, lon, and elev) at the requested
   * trajectory point. Units are as given by getXXUnitsIdentifier() methods.
   *
   * @param point the index point into the  trajectory
   * @return ucar.nc2.StructureData containing an Array for each parameter.
   * @throws IOException if problems reading data.
   */
    public ucar.ma2.StructureData getData(int point) throws IOException, InvalidRangeException;
    public ucar.ma2.Array getData( int point, String parameterName) throws IOException;

  /**
   * Get an efficient iterator over all the data in the Trajectory in time order.
   * This is an optional method; it will return null if no iterator is available.
   *
   * This is the efficient way to get all the data, it can be 100 times faster than getData().
   * This will return an iterator over type getDataClass(), and the actual data has already been read
   * into memory, that is, dataType.getData() will not incur any I/O.
   * <p> This is accomplished by buffering bufferSize amount of data at once. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it, in order for the garbage collector
   * to work.
   * <p> We dont need a cancelTask, just stop the iteration if the user want to cancel.
   *
   * @param bufferSize if > 0, the internal buffer size, else use the default. Typically 100k - 1M for best results.
   * @return Iterator over type PointObsDatatype, time order; or null if not available.
   * @throws IOException
   */
    public DataIterator getDataIterator( int bufferSize ) throws IOException;

    // read a number of rows all at once; may be more efficient that one row at a time
    // public ucar.ma2.ArrayStructure getData(ucar.ma2.Range range) throws IOException;
    // public ucar.ma2.ArrayStructure getData(ucar.ma2.Range range, List paramNames) throws IOException;
   // read all rows all at once; may be more efficient than one row at a time
    // public ucar.ma2.ArrayStructure getData() throws IOException;

    /** Get the range for the entire trajectory. */
    public ucar.ma2.Range getFullRange();
    /** Get a range for a single point in the trajectory. */
    public ucar.ma2.Range getPointRange( int point) throws InvalidRangeException;
    /** Get a range with the given start, end, and stride in the trajectory. */
    public ucar.ma2.Range getRange( int start, int end, int stride) throws InvalidRangeException;

    /** Get the time values on the given Range of the trajectory. */
    public ucar.ma2.Array getTime( ucar.ma2.Range range) throws IOException, InvalidRangeException;
    /** Get the latitude values on the given Range of the trajectory. */
    public ucar.ma2.Array getLatitude( ucar.ma2.Range range) throws IOException, InvalidRangeException;
    /** Get the longitude values on the given Range of the trajectory. */
    public ucar.ma2.Array getLongitude( ucar.ma2.Range range) throws IOException, InvalidRangeException;
    /** Get the elevation values on the given Range of the trajectory. */
    public ucar.ma2.Array getElevation( ucar.ma2.Range range) throws IOException, InvalidRangeException;

    /** Get the values of the requested parameter on the given Range of the trajectory. */
    public ucar.ma2.Array getData( ucar.ma2.Range range, String parameterName) throws IOException, InvalidRangeException;

    // @todo Return a StructureData with time, lat, lon, elev, and some set of params
    // public StructureData getData( ucar.ma2.Range range, List parameterNames) throws IOException

    // @todo allow selection on time range, i.e., map from time range to index space
    // public StructureData getData( Date start, Date end, List parameterNames) throws IOException
    // public StructureData getData( double start, double end, List parameterNames) throws IOException

}
