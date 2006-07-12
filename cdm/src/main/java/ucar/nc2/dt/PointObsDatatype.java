package ucar.nc2.dt;

import java.util.Date;

/** A collection of observations at one time and location.
 * @author caron
 * @version $Revision$ $Date$
 */
public interface PointObsDatatype  {

  /** Nominal time of the observation. Units are found from getTimeUnits() in the containing dataset. */
  public double getNominalTime();

  /** Actual time of the observation. Units are found from getTimeUnits() in the containing dataset. */
  public double getObservationTime();

  /** Nominal time of the observation, as a Date. */
  public Date getNominalTimeAsDate();

  /** Actual time of the observation, as a Date. */
  public Date getObservationTimeAsDate();

  /** Location of the observation */
  public EarthLocation getLocation();

  /** The actual data of the obvervation. */
  public ucar.ma2.StructureData getData() throws java.io.IOException;
}
