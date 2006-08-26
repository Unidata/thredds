// $Id: Aggregation.java 69 2006-07-13 00:12:58Z caron $
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
package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.*;

/**
 * Implement NcML Forecast Model Aggregation
 *
 * @deprecated use AggregationFmrc
 * @author caron
 * @version $Revision: 69 $ $Date: 2006-07-13 00:12:58Z $
 */
public class AggregationFmr extends Aggregation {

  private boolean isConstantForecast, isForecastOffset, isForecastTimeOffset;
  private Date forecastDate;
  private TimeUnit forecastTimeOffset;
  private String forecastDateVariable, referenceDateVariable;
  private int aggForecastOffset;

  public AggregationFmr(NetcdfDataset ncd, String dimName, String typeName, String recheckS) {
    super( ncd, dimName, typeName, recheckS);
  }

  public void setForecastDate(String forecastDateS, String forecastDateVariable) {
    this.forecastDate = DateUnit.getStandardOrISO( forecastDateS);
    if (forecastDate == null) {
      logger.error("ForecastDate { } invalid: must be ISO 8601 or udunit date. ", forecastDateS);
      return;
    }
    this.forecastDateVariable = forecastDateVariable;
    this.isConstantForecast = true;
  }

  public void setForecastOffset(String forecastOffsetS) {
    this.aggForecastOffset = Integer.parseInt( forecastOffsetS);
    this.isForecastOffset = true;
  }

  public void setForecastTimeOffset(String forecastTimeOffsetS, String forecastDateVariable, String referenceDateVariable) {
    try {
      this.forecastTimeOffset = new TimeUnit( forecastTimeOffsetS);
    } catch (Exception e) {
      logger.error("ForecastTimeOffset { } invalid: must be udunit time. ", forecastTimeOffsetS);
      return;
    }

    this.forecastDateVariable = forecastDateVariable;
    this.referenceDateVariable = referenceDateVariable;
    this.isForecastTimeOffset = true;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   * public for NcMLWriter
   */
  public class Dataset extends Aggregation.Dataset {

    private Date modelRunDate;
    private ArrayList forecastDates;
    private int forecastOffset;

    // joinNew or joinExisting. dataset opening is deferred
    Dataset(String cacheName, String location, String ncoordS, String coordValueS, boolean enhance, NetcdfFileFactory reader) {
      super(cacheName, location, ncoordS, coordValueS, enhance, reader);
    }

    protected Variable modifyVariable(NetcdfFile ncfile, String name) throws IOException {
      Variable want = ncfile.findVariable( name);
      if (want.getRank() < 1) return want;
      Dimension d = want.getDimension(0);
      if (!dimName.equals(d.getName()))
        return want;

      int n = getNcoords(null); // not needed since its always 1 now
      if (d.getLength() == n)
        return want;

      // looks like we need to modify it
      Range aggRange;
      try {
        aggRange = new Range( forecastOffset, forecastOffset);
      } catch (InvalidRangeException e) {
        logger.error(" Aggregation.modify make Range", e);
        return want;
      }

      // replace the first Range
      List ranges = want.getRanges();
      ranges.set(0, aggRange);

      // subset it
      Variable vsubset;
      try {
        vsubset = want.section(ranges);
      } catch (InvalidRangeException e) {
        logger.error(" Aggregation.modify make Variable "+want.getName(), e);
        return want;
      }

      return vsubset;
    }

    protected boolean checkOK(CancelTask cancelTask) throws IOException {
      if (isForecastOffset) {
        forecastOffset = aggForecastOffset;
        return true;
      }

      // otherwise, check if this file has the requested forecast time
      NetcdfFile ncfile = null;
      try {
        ncfile = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return false;

        makeForecastDates(ncfile);
        if (isForecastTimeOffset) readModelRunDate(ncfile);
        ncfile.close();

        Date want = isConstantForecast ? forecastDate : forecastTimeOffset.add( modelRunDate);
        for (int i = 0; i < forecastDates.size(); i++) {
          Date date = (Date) forecastDates.get(i);
          if (date.equals(want)) {
            if (debug) System.out.println(" found date " + formatter.toDateTimeString(date) + " at index " + i);
            forecastOffset = i;
            return true;
          }
        }

        // cant find this date
        if (debug)
          System.out.println(" didnt find date " + formatter.toDateTimeString(want) + " in file " + ncfile.getLocation());
        return false;
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }

    /* private int findForecastOffset(NetcdfFile ncfile) throws IOException {
      if (forecastDates == null)
        makeForecastDates( ncfile);

      Date want = forecastDate;
      for (int i = 0; i < forecastDates.size(); i++) {
        Date date = (Date) forecastDates.get(i);
        if (date.equals(want)) {
          System.out.println(" found date "+DateUnit.getStandardDateString(date)+" at index "+i);
          return i;
        }
      }

      // cant find this date
      System.out.println(" didnt find date "+DateUnit.getStandardDateString(want)+" in file "+ncfile.getLocation());

      return 0;
    } */

    private void makeForecastDates(NetcdfFile ncfile) throws IOException {
      forecastDates = new ArrayList();
      Variable coordVar = ncfile.findVariable(forecastDateVariable);
      Array coordValues = coordVar.read();
      Index index = coordValues.getIndex();
      int n = (int) coordValues.getSize();

      // see if it has a valid udunits unit
      String units = coordVar.getUnitsString();
      if (units != null) {
        SimpleUnit su = SimpleUnit.factory(units);
        if ((su != null) && (su instanceof DateUnit)) {
          DateUnit du = (DateUnit) su;
          for (int i = 0; i < n; i++) {
            Date d = du.makeDate(coordValues.getDouble(index.set(i)));
            forecastDates.add(d);
            if (debug) System.out.println(" added forecast date "+formatter.toDateTimeString(d)+" for file "+ncfile.getLocation());
          }
          return;
        }
      }

      // otherwise, see if its a String, and if we can parse the values as an ISO date
      if (coordVar.getDataType() == DataType.STRING) {
        for (int i = 0; i < n; i++) {
          String coordValue = (String) coordValues.getObject(index.set(i));
          Date d = formatter.getISODate(coordValue);
          if (d == null) {
            logger.error("Error on forecast date variable, not udunit or ISO. "+ coordValue);
            forecastDates.add(null);
          } else {
            forecastDates.add(d);
          }
        }
        return;
      }

      if (coordVar.getDataType() == DataType.CHAR) {
        ArrayChar coordValuesChar = (ArrayChar) coordValues;
        for (int i = 0; i < n; i++) {
          String coordValue = coordValuesChar.getString(i);
          Date d = formatter.getISODate(coordValue);
          if (d == null) {
            logger.error("Error on forecast date variable, not udunit or ISO. "+ coordValue);
            forecastDates.add(null);
          } else {
            forecastDates.add(d);
          }
        }
        return;
      }

      logger.error("Error on forecast date variable, not udunit or ISO formatted.");
    }

    private void readModelRunDate(NetcdfFile ncfile) throws IOException {
      Variable coordVar = ncfile.findVariable(referenceDateVariable);
      Array coordValues = coordVar.read();
      Index index = coordValues.getIndex();
      int n = (int) coordValues.getSize();

      // see if it has a valid udunits unit
      String units = coordVar.getUnitsString();
      if (units != null) {
        SimpleUnit su = SimpleUnit.factory(units);
        if ((su != null) && (su instanceof DateUnit)) {
          DateUnit du = (DateUnit) su;
          modelRunDate = du.makeDate( coordValues.getDouble(index));
          return;
        }
      }

      // otherwise, see if its a String, and if we can parse the values as an ISO date
      if (coordVar.getDataType() == DataType.STRING) {
          String coordValue = (String) coordValues.getObject(index);
          Date d = formatter.getISODate(coordValue);
          if (d == null) {
            logger.error("Error on referenceDateVariable, not udunit or ISO. "+ coordValue);
          } else {
            modelRunDate = d;
          }
        return;
        }

      if (coordVar.getDataType() == DataType.CHAR) {
        ArrayChar coordValuesChar = (ArrayChar) coordValues;
          String coordValue = coordValuesChar.getString(0);
          Date d = formatter.getISODate(coordValue);
          if (d == null) {
            logger.error("Error on referenceDateVariable, not udunit or ISO. "+ coordValue);
            forecastDates.add(null);
          } else {
            forecastDates.add(d);
          }
        return;
      }

      logger.error("Error on referenceDateVariable, not udunit or ISO formatted.");
    }
  }
}