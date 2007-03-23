package ucar.nc2.dt2;

/** A collection of observations at one time and at one station ( = named location)
 * @author caron
 */
public interface StationObsFeature extends PointObsFeature {
  
    /** Station location of the observation */
    public ucar.nc2.dt.Station getStation();
}
