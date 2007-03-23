package ucar.nc2.dt2;

import java.util.Date;

/**
 * A collection of observations at one time and location.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public interface PointObsFeature  {
  
   /** Get the units of Calendar time.
   *  To get a Date, from a time value, call DateUnit.getStandardDate(double value).
   *  To get units as a String, call DateUnit.getUnitsString().
   */
  public ucar.nc2.units.DateUnit getTimeUnits();

  /** Actual time of the observation. Units are found from getTimeUnits() in the containing dataset. */
  public double getObservationTime();

  /** Actual time of the observation, as a Date. */
  public Date getObservationTimeAsDate();

  /** Location of the observation */
  public EarthLocation getLocation();

  /** The actual data of the obsvervation. */
  public ucar.ma2.StructureData getData() throws java.io.IOException;
}
