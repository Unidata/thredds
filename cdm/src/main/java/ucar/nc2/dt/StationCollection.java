
package ucar.nc2.dt;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Iterator;

/** A collection of data at unconnected station locations, typically time series.
 * User can subset by stations, bounding box and by date range.
 * Underlying data can be of any type, but all points have the same type.
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public interface StationCollection extends PointCollection {

  /** Get all the Stations in the collection.
   * @return List of Station */
  public List getStations() throws IOException;

  /** Get all the Stations in the collection, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of Station */
  public List getStations(ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get all the Stations within a bounding box.
   * @return List of Station */
  public List getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /** Get all the Stations within a bounding box, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of Station */
  public List getStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Find a Station by name */
  public Station getStation( String name);

  /**
   * How many Data objects are available for this Station?
   * @param s station
   * @return count or -1 if unknown.
   */
  public int getStationDataCount( Station s);

  /** Get all data for this Station.
   * @return List of getDataClass() */
  public List getData( Station s) throws IOException;

  /** Get all data for this Station, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass() */
  public List getData( Station s, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get data for this Station within the specified date range.
   * @return List of getDataClass() */
  public List getData( Station s, Date start, Date end) throws IOException;

  /** Get data for this Station within the specified date range, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass() */
  public List getData( Station s, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get all data for a list of Stations.
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData(List stations) throws IOException;

  /** Get all data for a list of Stations, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData(List stations, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get data for a list of Stations within the specified date range.
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData(List stations, Date start, Date end) throws IOException;

  /** Get data for a list of Stations within the specified date range, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   */
  public List getData(List stations, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;


  /** Get all data for this Station.
   * @return iterator over type getDataClass() *
  public DataIterator getDataIterator( Station s) throws IOException;

  /** Get data for this Station within the specified date range.
   * @return Iterator over type getDataClass() *
  public DataIterator getDataIterator( Station s, Date start, Date end) throws IOException;

  /** Get all data for a list of Stations.
   * @return Iterator over type getDataClass() *
  public DataIterator getDataIterator(List stations) throws IOException;

  /** Get data for a list of Stations within the specified date range.
   * @return Iterator over type getDataClass() *
  public DataIterator getDataIterator(List stations, Date start, Date end) throws IOException;
  */

}
