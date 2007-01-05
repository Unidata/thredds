
package ucar.nc2.dt;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Iterator;

/** A collection of data at unconnected radar station.
 * User can subset by stations, bounding box and by date range.
 * Underlying data can be of any type, but all points have the same type.
 * @author
 * @version $Revision:  $
 */
public interface StationaryRadarCollection extends StationCollection {

  /** check if the product available for all stations.
   * @return true of false */
  public boolean checkStationProduct(String product);


  /** check if the product available for one station
   * @return true of false */
  public boolean checkStationProduct(Station s, String product);

  /**
   * How many Data Products are available for this Station?
   * @param s station, and product requested
   * @return count or -1 if unknown.
   */
  public List getStationDataProducts( Station s);

 /** Get all specific data within the specified bounding box.
   * @return List of type RadialDatasetSweep data
   */
  public List getData( String product, ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /** Get all specific data within the specified bounding box, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of type RadialDatasetSweep data
   */
  public List getData( String product, ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get all specific data within the specified bounding box and date range.
   * @return List of type RadialDatasetSweep data
   */
  public List getData( String product, ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end) throws IOException;

  /** Get all specific data within the specified bounding box and date range, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of type RadialDatasetSweep data
   */
  public List getData( String product, ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;


  /** Get data for this Station within the specified date range.
   * @return List of getDataClass() */
  public List getData( Station s, String product, Date start, Date end) throws IOException;

  /** Get data for this Station within the specified date range, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of RadialDatasetSweep data
   */
  public List getData( Station s, String product, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get all data for a list of Stations.
   * @return List of RadialDatasetSweep data
   */
  public List getData(List stations, String product) throws IOException;

  /** Get all data for a list of Stations, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of RadialDatasetSweep data
   */
  public List getData(List stations, String product, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Get data for a list of Stations within the specified date range.
   * @return List of RadialDatasetSweep data
   */
  public List getData(List stations, String product, Date start, Date end) throws IOException;

  /** Get data for a list of Stations within the specified date range, allow user to cancel.
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of RadialDatasetSweep data
   */
  public List getData(List stations, String product, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;



}
