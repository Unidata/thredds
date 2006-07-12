package ucar.nc2.dt;

/** A collection of observations at one time and at one station ( = named location)
 * @author caron
 * @version $Revision$ $Date$
 */
public interface StationObsDatatype extends ucar.nc2.dt.PointObsDatatype {
  
    /** Station location of the observation */
    public ucar.nc2.dt.Station getStation();
}
