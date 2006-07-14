// $Id:ForecastModelRunCollectionSave.java 51 2006-07-12 17:13:13Z caron $
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

import java.util.List;
import java.util.Date;

/**
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public interface FmrcIF {

  /**
   * Get the list of Variable names available in this collection.
   * Only Variables with a forecast time coordinate are included
   * @return List of String
   */
  List getVariableNames();

  /**
   * Get the TimeMatrix which shows what is available for the given variable
   * @param varName name of Variable
   * @return a TimeMatrix
   */
  TimeMatrix getVariableMatrix(String varName);

  /**
   * Get the TimeMatrix which shows what is available for the entire dataset.
   * This is the union of the Variable TimeMatrices.
   * Use getRunTimes(), getTimeInventory() for the set of valid dates as input to getRunTimeDataset(),
   *  getForecastTimeDataset().
   * @return a TimeMatrix
   */
  TimeMatrixDataset getDatasetMatrix();

  /**
   * Get a NetcdfDataset that has all the data for a model run.
   * The time coordinate will be the forecast time.
   * The runtime will be a global attribute called "_runTime" containing an ISO date string.
   * @param runTime names the run
   * @return the NetcdfDataset for that run.
   */
  NetcdfDataset getRunTimeDataset( Date runTime);

  /**
   * Get a NetcdfDataset that has all the data for a fixed forecast time, across model runs.
   * The time coordinate will be the run time.
   * The forecast time will be a global attribute called "_forecastTime" containing an ISO date string.
   * @param forecastTime the forecast time to fix.
   * @return the NetcdfDataset for that run.
   */
  NetcdfDataset getForecastTimeDataset( Date forecastTime);

  /**
   * Get a NetcdfDataset that has all the data for a fixed forecast offset, across model runs.
   * The time coordinate will be the forecast time.
   * The runtime will be a global attribute called "_runTime" containing an array of ISO date strings.
   * @param offset the forecast time to fix.
   * @return the NetcdfDataset for that run.
   */
  NetcdfDataset getForecastOffsetDataset( Date offset);


  public interface TimeMatrix {
    List getRunTimes(); // list of Date
    List getForecastTimes(); // list of Date
    boolean isPresent(Date runTime, Date forecastTime);
  }

  public interface TimeMatrixDataset {
    List getRunTimes(); // list of Date
    List getForecastTimes(); // list of Date
    int countPresent(Date runTime, Date forecastTime);
    int countTotal(Date runTime, Date forecastTime);
  }

}
