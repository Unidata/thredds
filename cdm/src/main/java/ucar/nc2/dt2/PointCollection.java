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

import ucar.nc2.units.DateRange;

import java.io.IOException;

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
   * @param boundingBox spatial subset, may be null
   * @param dateRange   dateRange, may be null
   * @return subsetted collection
   * @throws java.io.IOException on i/o error
   */
  public PointCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox, DateRange dateRange) throws IOException;

  /**
   * Get an efficient iterator over all the data in the Collection. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it. DO NOT KEEP ANY REFERENCES to the
   * dataType object or the StructureData object.
   * <pre>Example for point observations:
   * Iterator iter = pointObsDataset.getDataIterator();
   * while (iter.hasNext()) {
   *   PointObsFeature pobs = (PointObsFeature) iter.next();
   *   StructureData sdata = pobs.getData();
   *   // process fully
   * }
   * </pre>
   *
   * @param bufferSize if > 0, the internal buffer size, else use the default. 100k - 1M gives good results in all cases.
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