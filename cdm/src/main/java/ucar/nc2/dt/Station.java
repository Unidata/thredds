
package ucar.nc2.dt;

/**
 * A named location on the Earth.
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public interface Station extends EarthLocation, Comparable {
  /** Station name. Must be unique within the collection */
  public String getName();
  /** Station description */
  public String getDescription();
  /** WMO Station ID (optional) */
  public String getWmoId();
}
