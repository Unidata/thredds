
package ucar.nc2.dt;

/** A collection of data in a time series.
 * Underlying data can be of any type.
 * @author caron
 * @version $Revision$ $Date$
 */
public interface TimeSeriesCollection {

  /** The getData() methods returns List of objects of this Class */
  public Class getDataClass();

  /** Get number of points in the series */
  public int getNumTimes();

  /** get the time of the nth point. */
  public double getTime(int timePt);

  /** Get the units of Calendar time.
   *  To get a Date, from a time value, call DateUnit.getStandardDate(double value).
   *  To get units as a String, call DateUnit.getUnitsString().
   */
  public ucar.nc2.units.DateUnit getTimeUnits();

  /** Get the data for the nth point. @return Object of type getDataClass() */
  public Object getData(int timePt);
}
