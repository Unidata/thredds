/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.sparr.Coordinate;

import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 1/23/14
 */
public abstract class CoordinateTimeAbstract implements Coordinate {

  protected final int code;                  // pdsFirst.getTimeUnit()
  protected final CalendarPeriod timeUnit;
  protected CalendarDate refDate;            // null if dense

  protected String name = "time";
  protected String periodName;

  CoordinateTimeAbstract(int code, CalendarPeriod timeUnit, CalendarDate refDate) {
    this.code = code;
    this.timeUnit = timeUnit;
    this.refDate = refDate;

    CalendarPeriod.Field cf = timeUnit.getField();
    if (cf == CalendarPeriod.Field.Month || cf == CalendarPeriod.Field.Year)
      this.periodName = "calendar "+ cf.toString();
    else
      this.periodName = timeUnit.getField().toString();
  }

  @Override
  public int getCode() {
    return code;
  }

  public CalendarPeriod getPeriod() {
    return timeUnit;
  }

  @Override
  public String getUnit() {
    return periodName;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CalendarDate getRefDate() {
    return refDate;
  }

  public void setRefDate(CalendarDate refDate) {
    this.refDate = refDate;
  }

  public double getTimeUnitScale() {
     return timeUnit.getValue();
   }

  public CalendarPeriod getTimeUnit() {
    return timeUnit;
  }

  ////////////////////////////////////////
  public abstract CoordinateTimeAbstract createBestTimeCoordinate(List<Double> runOffsets);

}
