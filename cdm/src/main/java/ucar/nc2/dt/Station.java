
package ucar.nc2.dt;

/**
 * A named location on the Earth.
 * @author caron
 * @version $Revision: 1.3 $ $Date: 2005/03/04 20:18:23 $
 */
public interface Station extends EarthLocation {
  /** Station name. Must be unique within the collection */
  public String getName();
  /** Station description */
  public String getDescription();
  /** WMO Station ID (optional) */
  public String getWmoId();
}
