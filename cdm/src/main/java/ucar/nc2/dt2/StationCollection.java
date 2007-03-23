
package ucar.nc2.dt2;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/** A time series collection of features at station locations.
 * @author caron
 */
public interface StationCollection extends PointCollection {

  /**
   * Get all the Stations in the collection.
   * @return List of Station */
  public List<Station> getStations() throws IOException;

  /**
   * Get all the Stations within a bounding box.
   * @param boundingBox spatial subset
   * @return List of Station */
  public List<Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /**
   * Find a Station by name.
   * @return Station or null if not found */
  public Station getStation( String name);

  /**
   * Get the collection of data for this Station.
   * @param s at this station
   * @return collection of data for this Station.
   */
  public TimeSeriesCollection subset( Station s) throws IOException;

  /**
   * Get the collection of data for this Station and date range.
   * @param start starting date
   * @param end ending date
   * @return collection of data for this Station and date range.
   */
  public TimeSeriesCollection subset( Station s, Date start, Date end) throws IOException;

  /**
   * Get collection of data for a list of Stations.
   * @return Iterator over type getDataClass() */
  public StationCollection subset(List<Station> stations) throws IOException;

}
