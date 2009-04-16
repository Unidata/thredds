/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dt.fmrc;

import ucar.nc2.dataset.NetcdfDataset;

import java.util.Date;
import java.util.List;
import java.io.IOException;

/**
 * Forecast Model Run Collection
 *
 * @author caron
 */
public interface ForecastModelRunCollection {

  /**
   * Get the list of possible run dates, to be used in getRunTimeDataset().
   * @return List of Date
   */
  public List<Date> getRunDates();

   /**
   * Get a NetcdfDataset that has all the data for a model run.
   * The time coordinate will be the forecast time.
   * The runtime will be a global attribute called "_runTime" containing an ISO date string.
   * @param runTime names the run
   * @return the NetcdfDataset for that data.
    * @throws java.io.IOException on read error
   */
  public NetcdfDataset getRunTimeDataset( Date runTime) throws IOException;

  /**
   * Get the list of possible forecast dates, to be used in getForecastTimeDataset().
   * @return List of Date
   */
  public List<Date> getForecastDates();

  /**
   * Get a NetcdfDataset that has all the data for a fixed forecast time, across model runs.
   * The time coordinate will be the run time.
   * The forecast time will be a global attribute called "_forecastTime" containing an ISO date string.
   * @param forecastTime the forecast time to fix.
   * @return the NetcdfDataset for that data.
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset getForecastTimeDataset( Date forecastTime)  throws IOException;

  /**
   * Get the list of possible forecast offsets, to be used in getForecastOffsetDataset().
   * @return List of Double
   */
  public List<Double> getForecastOffsets();

  /**
   * Get a NetcdfDataset that has all the data for a fixed forecast offset, across model runs.
   * The time coordinate will be the forecast time.
   * There will be a String-valued variable called "RunTime(time)" containing an array of ISO date strings.
   * @param hours the forecast offset time to fix, in hours.
   * @return the NetcdfDataset containing that data.
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset getForecastOffsetDataset( double hours)  throws IOException;

  /**
   * Get a NetcdfDataset that has the "best" time series, across model runs.
   * The time coordinate will be the forecast time.
   * There will be a String-valued variable called "RunTime(time)" containing an array of ISO date strings.
   * @return the NetcdfDataset containing that data.
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset getBestTimeSeries( )  throws IOException;


  /**
   * Get a NetcdfDataset that is the underlying datasets with the "2d time"
   * @return the NetcdfDataset containing that data.
   */
  public NetcdfDataset getFmrcDataset( );

  /**
   * Get the underlying GridDataset.
   * @return the GridDataset containing that data.
   */
  public ucar.nc2.dt.GridDataset getGridDataset();

  /** Check if file has changed, and reread metadata if needed.
   * @return true if file was changed.
   * @throws java.io.IOException on io error
   */
  public boolean sync() throws IOException;

  // close and release all resources
  public void close() throws IOException;
}
