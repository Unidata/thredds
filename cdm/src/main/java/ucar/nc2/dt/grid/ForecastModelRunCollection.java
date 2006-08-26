// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dt.grid;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;

import java.util.Date;
import java.util.List;
import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public interface ForecastModelRunCollection {

  /**
   * Get the list of possible run dates, to be used in getRunTimeDataset().
   * @return List of Date
   */
  public List getRunDates();

   /**
   * Get a NetcdfDataset that has all the data for a model run.
   * The time coordinate will be the forecast time.
   * The runtime will be a global attribute called "_runTime" containing an ISO date string.
   * @param runTime names the run
   * @return the NetcdfDataset for that data.
   */
  public NetcdfDataset getRunTimeDataset( Date runTime);

  /**
   * Get the list of possible forecast dates, to be used in getForecastTimeDataset().
   * @return List of Date
   */
  public List getForecastDates();

  /**
   * Get a NetcdfDataset that has all the data for a fixed forecast time, across model runs.
   * The time coordinate will be the run time.
   * The forecast time will be a global attribute called "_forecastTime" containing an ISO date string.
   * @param forecastTime the forecast time to fix.
   * @return the NetcdfDataset for that data.
   */
  public NetcdfDataset getForecastTimeDataset( Date forecastTime);

  /**
   * Get the list of possible forecast offsets, to be used in getForecastOffsetDataset().
   * @return List of Double
   */
  public List getForecastOffsets();

  /**
   * Get a NetcdfDataset that has all the data for a fixed forecast offset, across model runs.
   * The time coordinate will be the forecast time.
   * There will be a String-valued variable called "RunTime(time)" containing an array of ISO date strings.
   * @param hours the forecast offset time to fix, in hours.
   * @return the NetcdfDataset containing that data.
   */
  public NetcdfDataset getForecastOffsetDataset( double hours);

  /**
   * Get a NetcdfDataset that has the "best" time series, across model runs.
   * The time coordinate will be the forecast time.
   * There will be a String-valued variable called "RunTime(time)" containing an array of ISO date strings.
   * @return the NetcdfDataset containing that data.
   */
  public NetcdfDataset getBestTimeSeries( );


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
   * @throws java.io.IOException
   */
  public boolean sync() throws IOException;
}
