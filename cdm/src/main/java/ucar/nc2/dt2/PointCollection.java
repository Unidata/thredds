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
package ucar.nc2.dt2;

import java.io.IOException;
import java.util.Date;

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
   * @return the class of the underlying data type
   */
  public Class getFeatureClass();

  /**
   * Subset the collection using the specified bounding box and date range.
   *
   * @param boundingBox spatial subset
   * @param start       starting date
   * @param end         ending date
   * @return subsetted collection
   * @throws java.io.IOException on i/o error
   */
  public PointCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end) throws IOException;

  /**
   * Get an efficient iterator over all the data in the Collection. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it. DO NOT KEEP ANY REFERENCES to the
   * dataType object or the StructureData object.
   * <p/>
   * This is the efficient way to get all the data, it can be 100 times faster than getData().
   * This will return an iterator over type getDataClass(), and the actual data has already been read
   * into memory, that is, dataType.getData() will not incur any I/O.
   * This is accomplished by buffering bufferSize amount of data at once.
   * <p/>
   * <p> We dont need a cancelTask, just stop the iteration if the user wants to cancel.
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
   * @throws IOException on i/o error
   */
  public DataIterator getDataIterator(int bufferSize) throws IOException;

  /**
   * Get estimate of the cost of accessing all the data in the Collection.
   * @return DataCost or null if not able to estimate.
   */
  public DataCost getDataCost();

}