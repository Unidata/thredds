
package ucar.nc2.dt;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Iterator;

/** A collection of data at unconnected locations.
 * User can subset by bounding box and by date range.
 * Underlying data can be of any type, but all points have the same type.
 * @author caron
 * @version $Revision$ $Date$
 */
public interface PointCollection {

  /** The getData() methods return objects of this Class */
  public Class getDataClass();

  /** Get the units of Calendar time.
   *  To get a Date, from a time value, call DateUnit.makeDate(double value).
   *  To get units as a String, call DateUnit.getUnitsString().
   */
  public ucar.nc2.units.DateUnit getTimeUnits();

  /** Get all data. Return null if too expensive to implement.
   *  Call getDataCount() to get estimate of size.
   *  This will return a list of getDataClass(), but the actual data may or may not already be read in
   *  to memory. In any case, you call dataType.getData() to get the data.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData() throws IOException;

  /** Get all data, allow user to cancel. Return null if too expensive to implement.
   *  Call getDataCount() to get estimate of size.
   *  This will return a list of getDataClass(), but the actual data may or may not already be read in
   *  to memory. In any case, you call dataType.getData() to get the data.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData(ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get estimate of number of data records (may not be exact).
   * Return -1 if not able to estimate.
   */
  public int getDataCount();

  /** Get all data within the specified bounding box.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData( ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /** Get all data within the specified bounding box, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData( ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get all data within the specified bounding box and date range.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData( ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end) throws IOException;

  /** Get all data within the specified bounding box and date range, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of type getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData( ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get an efficient iterator over all the data in the Collection. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it. DO NOT KEEP ANY REFERENCES to the
   * dataType object or the StructureData object.
   *
   * This is the efficient way to get all the data, it can be 100 times faster than getData().
   * This will return an iterator over type getDataClass(), and the actual data has already been read
   * into memory, that is, dataType.getData() will not incur any I/O.
   * This is accomplished by buffering bufferSize amount of data at once.
   * <p> We dont need a cancelTask, just stop the iteration if the user want to cancel.
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
  public DataIterator getDataIterator(int bufferSize) throws IOException;

  /** Get an efficient iterator over all the data within the specified bounding box.
   * @return iterator over type getDataClass() *
  public DataIterator getDataIterator( ucar.unidata.geoloc.LatLonRect boundingBox, int bufferSize) throws IOException;

  /** Get an efficient iterator over all the data within the specified bounding box and date range.
   * @return iterator over type getDataClass() *
  public DataIterator getDataIterator( ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end, int bufferSize) throws IOException;
  */
}
