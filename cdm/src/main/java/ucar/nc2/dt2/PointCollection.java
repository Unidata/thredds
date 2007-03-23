
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

  /** The getData() methods return objects of this Class */
  public Class getFeatureClass();

  /**
   * Subset the collection using the specified bounding box and date range.
   *
   * @param boundingBox spatial subset
   * @param start starting date
   * @param end ending date
   *
   * @return subsetted collection
   */
  public PointCollection subset( ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end) throws IOException;

  /**
   * Get an efficient iterator over all the data in the Collection. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it. DO NOT KEEP ANY REFERENCES to the
   * dataType object or the StructureData object.
   *
   * This is the efficient way to get all the data, it can be 100 times faster than getData().
   * This will return an iterator over type getDataClass(), and the actual data has already been read
   * into memory, that is, dataType.getData() will not incur any I/O.
   * This is accomplished by buffering bufferSize amount of data at once.
   *
   * <p> We dont need a cancelTask, just stop the iteration if the user wants to cancel.
   *
   * <pre>Example for point observations:
   *
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
   * @throws IOException
   */
  public DataIterator getDataIterator( int bufferSize) throws IOException;

  /** Get estimate of the cost of accessing all the data in the Collection.
   * Return DataCost or null if not able to estimate.
   */
  public DataCost getDataCost();

}