
package ucar.nc2.dt;

import thredds.catalog.query.Station;
import java.io.IOException;
import java.util.List;

import ucar.unidata.util.Product;


/** A collection of data at unconnected radar station.
 * User can subset by stations, bounding box and by date range.
 * Underlying data can be of any type, but all points have the same type.
 * @author
 * @version $Revision:  $
 */
public interface StationRadarCollection {

    /** Get all the Stations in the collection.
     * @return List of Station */
    public List getStations() throws IOException;

    /** Get all the Stations within a bounding box, allow user to cancel.
     * @param cancel allow user to cancel. Implementors should return ASAP.
     * @return List of Station */
    public List getStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

    /** Find a Station by name */
    //public Station getRadarStation( String name);



  /** check if the product available for all stations.
   * @return true of false */
    public boolean checkStationProduct(Product product);


  /** check if the product available for one station
   * @return true of false */
    public boolean checkStationProduct(String sName, Product product);

  /**
   * How many Data Products are available for this Station?
   * @param sName station name
   * @return count or -1 if unknown.
   */
    public int getStationProductCount( String sName);

}
