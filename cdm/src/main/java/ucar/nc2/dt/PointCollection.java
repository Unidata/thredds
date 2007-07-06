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
package ucar.nc2.dt;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * A collection of data at unconnected locations.
 * User can subset by bounding box and by date range.
 * Underlying data can be of any type, but all points have the same type.
 *
 * @author caron
 */
public interface PointCollection {

  /**
   * The getData() methods return objects of this Class
   * @return Class of the data
   */
  public Class getDataClass();

  /**
   * Get the units of Calendar time.
   * To get a Date, from a time value, call DateUnit.makeDate(double value).
   * To get units as a String, call DateUnit.getUnitsString().
   * @return the units of Calendar time.
   */
  public ucar.nc2.units.DateUnit getTimeUnits();

  /**
   * Get all data. Return null if too expensive to implement.
   * Call getDataCount() to get estimate of size.
   * This will return a list of getDataClass(), but the actual data may or may not already be read in
   * to memory. In any case, you call dataType.getData() to get the data.
   *
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData() throws IOException;

  /**
   * Get all data, allow user to cancel. Return null if too expensive to implement.
   * Call getDataCount() to get estimate of size.
   * This will return a list of getDataClass(), but the actual data may or may not already be read in
   * to memory. In any case, you call dataType.getData() to get the data.
   *
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get estimate of number of data records (may not be exact).
   * Return -1 if not able to estimate.
   * @return number of data records or -1
   */
  public int getDataCount();

  /**
   * Get all data within the specified bounding box.
   *
   * @param boundingBox restrict data to this bounding nox
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /**
   * Get all data within the specified bounding box, allow user to cancel.
   *
   * @param boundingBox restrict data to this bounding nox
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get all data within the specified bounding box and date range.
   *
   * @param boundingBox restrict data to this bounding nox
   * @param start restrict data to after this time
   * @param end restrict data to before this time
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end) throws IOException;

  /**
   * Get all data within the specified bounding box and date range, allow user to cancel.
   *
   * @param boundingBox restrict data to this bounding nox
   * @param start restrict data to after this time
   * @param end restrict data to before this time
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get an efficient iterator over all the data in the Collection. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it. DO NOT KEEP ANY REFERENCES to the
   * dataType object or the StructureData object.
   * <p/>
   * This is the efficient way to get all the data, it can be 100 times faster than getData().
   * This will return an iterator over type getDataClass(), and the actual data has already been read
   * into memory, that is, dataType.getData() will not incur any I/O.
   * This is accomplished by buffering bufferSize amount of data at once.
   * <p> We dont need a cancelTask, just stop the iteration if the user want to cancel.
   * <p/>
   * <pre>Example for point observations:
   * <p/>
   * Iterator iter = pointObsDataset.getDataIterator();
   * while (iter.hasNext()) {
   *   PointObsDatatype pobs = (PointObsDatatype) iter.next();
   *   StructureData sdata = pobs.getData();
   *   // process fully
   * }
   * </pre>
   *
   * @param bufferSize if > 0, the internal buffer size, else use the default. Typically 100k - 1M for best results.
   * @return iterator over type getDataClass(), no guarenteed order.
   * @throws java.io.IOException on io error
   */
  public DataIterator getDataIterator(int bufferSize) throws IOException;

  /** Get an efficient iterator over all the data within the specified bounding box.
   * @return iterator over type getDataClass() *
  public DataIterator getDataIterator( ucar.unidata.geoloc.LatLonRect boundingBox, int bufferSize) throws IOException;

  /** Get an efficient iterator over all the data within the specified bounding box and date range.
   * @return iterator over type getDataClass() *
  public DataIterator getDataIterator( ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end, int bufferSize) throws IOException;
   */
}
