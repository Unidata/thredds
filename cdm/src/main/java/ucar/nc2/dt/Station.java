
package ucar.nc2.dt;

/**
 * A named location on the Earth.
 * @author caron
 * @version $Revision$ $Date$
 */
public interface Station extends EarthLocation {
  /** Station name. Must be unique within the collection */
  public String getName();
  /** Station description */
  public String getDescription();
  /** WMO Station ID (optional) */
  public String getWmoId();
}
