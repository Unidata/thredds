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
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.units.DateFormatter;

import java.util.*;
import java.io.IOException;

/**
 * ForecastModelRunCollection implementation.
 * Uses a GridDataset that has two time dimensions.
 * Assume all grids have the same runTime dimension.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class FmrcImpl implements ForecastModelRunCollection {
  private GridDataset gds;
  private CoordinateAxis1DTime runtimeCoord;
  private HashMap timeMap = new HashMap();
  private HashMap offsetMap = new HashMap();
  private ArrayList forecasts = new ArrayList();
  private ArrayList offsetHours = new ArrayList();

  public FmrcImpl(String filename) throws IOException {
    this.gds = ucar.nc2.dataset.grid.GridDataset.open( filename);

    List gridsets = gds.getGridSets();
    if (gridsets.size() == 0)
      throw new IllegalArgumentException("no grids");
    GridDataset.Gridset gset = (GridDataset.Gridset) gridsets.get(0);
    GridCoordSystem gcs = gset.getGeoCoordSystem();

    if (!gcs.hasTimeAxis() || gcs.hasTimeAxis1D())
       throw new IllegalArgumentException("must have 2D time axis");

    runtimeCoord = gcs.getRunTimeAxis();
    if (null == runtimeCoord)
      throw new IllegalArgumentException("no runtime coordinate");
    Date[] runDates = runtimeCoord.getTimeDates();

    CoordinateAxis2D forecastCoord = (CoordinateAxis2D) gcs.getTimeAxis();
    int[] shape = forecastCoord.getShape();
    int nruns = shape[0];
    int ntimes = shape[1];
    System.out.println(" nruns="+nruns+" ntimes="+ntimes);

    for (int run = 0; run < nruns; run++) {
      Date runDate = runDates[run];

      CoordinateAxis1DTime timeCoordRun = gcs.getTimeAxisForRun( run);
      Date[] forecastDates = timeCoordRun.getTimeDates();

      for (int time = 0; time < ntimes; time++) {
        Date forecastDate = forecastDates[time];
        double hourOffset = getOffsetHour(runDate, forecastDate);

        Inventory inv = new Inventory(runDate, forecastDate, hourOffset, run, time);

        Double dd = new Double(hourOffset);
        ArrayList offsetList = (ArrayList) offsetMap.get(dd);
        if (offsetList == null) {
          offsetList = new ArrayList();
          offsetMap.put(dd, offsetList);
          offsetHours.add( dd);
        }
        offsetList.add(inv);

        ArrayList timeList = (ArrayList) timeMap.get(forecastDate);
        if (timeList == null) {
          timeList = new ArrayList();
          timeMap.put(forecastDate, timeList);
          forecasts.add( forecastDate);
        }
        timeList.add(inv);
      }
    }

    Collections.sort( forecasts);
    Collections.sort( offsetHours);
  }

    private double getOffsetHour( Date run, Date forecast) {
    double diff = forecast.getTime() - run.getTime();
    return diff / 1000.0 / 60.0 / 60.0;
  }

  private class Inventory {
    Date forecastTime;
    Date runTime;
    double hourOffset;
    int run, time;

    Inventory(Date runTime, Date forecastTime, double hourOffset, int run, int time) {
      this.runTime = runTime;
      this.hourOffset = hourOffset;
      this.forecastTime = forecastTime;
      this.run = run;
      this.time = time;
    }
  }

  public List getRunDates() {
    return Arrays.asList( runtimeCoord.getTimeDates() );
  }

  public NetcdfDataset getRunTimeDataset(Date runTime) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getForecastDates() {
    return forecasts;
  }

  public NetcdfDataset getForecastTimeDataset(Date forecastTime) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getForecastOffsets() {
    return offsetHours;
  }

  public NetcdfDataset getForecastOffsetDataset(double hours) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public NetcdfDataset getBestTimeSeries() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public static void main(String args[]) throws IOException {
    FmrcImpl fmrc = new FmrcImpl("C:/dev/thredds/cdm/src/test/data/ncml/aggFmrcNetcdf.xml");
    DateFormatter df = new DateFormatter();

    List dates = fmrc.getRunDates();
    System.out.println("\nRun Dates= "+dates.size());
    for (int i = 0; i < dates.size(); i++) {
      Date date = (Date) dates.get(i);
      System.out.println(" "+df.toDateTimeString(date));
    }

    dates = fmrc.getForecastDates();
    System.out.println("\nForecast Dates= "+dates.size());
    for (int i = 0; i < dates.size(); i++) {
      Date date = (Date) dates.get(i);
      ArrayList list = (ArrayList) fmrc.timeMap.get(date);
      System.out.print(" "+df.toDateTimeString(date)+" (");
      for (int j = 0; j < list.size(); j++) {
        Inventory inv = (Inventory) list.get(j);
        System.out.print(" "+inv.hourOffset);
      }
      System.out.println(")");
    }

    List hours = fmrc.getForecastOffsets();
    System.out.println("\nForecast Hours= "+hours.size());
    for (int i = 0; i < hours.size(); i++) {
      Double hour = (Double) hours.get(i);
      ArrayList offsetList = (ArrayList) fmrc.offsetMap.get(hour);
      System.out.println(" "+hour+" ("+offsetList.size()+")");
    }

  }

}
