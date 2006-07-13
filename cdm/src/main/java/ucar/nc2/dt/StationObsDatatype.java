package ucar.nc2.dt;

/** A collection of observations at one time and at one station ( = named location)
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public interface StationObsDatatype extends ucar.nc2.dt.PointObsDatatype {
  
    /** Station location of the observation */
    public ucar.nc2.dt.Station getStation();
}
