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
package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;

import java.util.Date;

/**
 * Abstract superclass for implementations of PointFeature.
 * Concrete subclass must implement getData();
 *
 * @author caron
 * @since Feb 29, 2008
 */


public abstract class PointFeatureImpl implements PointFeature, Comparable<PointFeature> {
  protected EarthLocation location;
  protected double obsTime, nomTime;
  protected DateUnit timeUnit;

  public PointFeatureImpl( DateUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public PointFeatureImpl( EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit) {
    this.location = location;
    this.obsTime = obsTime;
    this.nomTime = (nomTime == 0) ? obsTime : nomTime; // LOOK temp kludge until protobuf accepts NaN as defaults
    this.timeUnit = timeUnit;
  }

  public EarthLocation getLocation() { return location; }
  public double getNominalTime() { return nomTime; }
  public double getObservationTime() { return obsTime; }

  // LOOK WRONG
  public String getZcoordUnits() {
    return "meters";
  }

  public String getDescription() {
    return location.toString(); // ??
  }

  public Date getObservationTimeAsDate() {
    return timeUnit.makeDate( getObservationTime());
  }

  public Date getNominalTimeAsDate() {
    return timeUnit.makeDate( getNominalTime());
  }

  public DateUnit getTimeUnit() {
    return timeUnit;
  }

  public int compareTo(PointFeature other) {
    if (obsTime < other.getObservationTime()) return -1;
    if (obsTime > other.getObservationTime()) return 1;
    return 0;
  }

  @Override
  public String toString() {
    return "PointFeatureImpl{" +
        "location=" + location +
        ", obsTime=" + obsTime +
        ", nomTime=" + nomTime +
        ", timeUnit=" + timeUnit +
        '}';
  }
}