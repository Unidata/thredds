/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.grib.grib2.table;

import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

/**
 Center        = (7) US National Weather Service, National Centres for Environmental Prediction (NCEP)
 SubCenter     = (14) NWS Meteorological Development Laboratory
 Master Table  = 1
 Local Table   = 0

 *
 * @author caron
 * @since 1/28/2016.
 */
public class NwsMetDevTables extends NcepLocalTables {
  private static NwsMetDevTables single;

  public static NwsMetDevTables getCust(Grib2Table table) {
    if (single == null) single = new NwsMetDevTables(table);
    return single;
  }

  private NwsMetDevTables(Grib2Table grib2Table) {
    super(grib2Table);
  }

  @Override
  public TimeCoord.TinvDate getForecastTimeInterval(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    if (!pds.isTimeInterval()) return null;
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();

    CalendarDate intvEnd = pdsIntv.getIntervalTimeEnd();

    int ftime = pdsIntv.getForecastTime();

    int timeUnitOrg = pds.getTimeUnit();
    int timeUnitConvert = convertTimeUnit(timeUnitOrg);
    CalendarPeriod unitPeriod = Grib2Utils.getCalendarPeriod(timeUnitConvert);
    if (unitPeriod == null)
      throw new IllegalArgumentException("unknown CalendarPeriod " + timeUnitConvert + " org=" + timeUnitOrg);

    CalendarPeriod.Field fld = unitPeriod.getField();

    CalendarDate referenceDate = gr.getReferenceDate();
    CalendarDate intvStart = referenceDate.add(ftime, fld);

    return new TimeCoord.TinvDate(intvStart, intvEnd);
  }

  @Override
  public double getForecastTimeIntervalSizeInHours(Grib2Pds pds) {
    return 12.0;  // LOOK  WTF ??
  }



}
