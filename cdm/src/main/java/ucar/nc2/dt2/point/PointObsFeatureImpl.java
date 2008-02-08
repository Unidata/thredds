/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.EarthLocation;
import ucar.nc2.dt2.PointObsFeature;
import ucar.nc2.dt2.DataIterator;
import ucar.nc2.dt2.DataCost;
import ucar.nc2.units.DateUnit;
import ucar.nc2.VariableSimpleIF;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.Array;

import java.util.Date;
import java.util.List;
import java.io.IOException;

/**
 * Abstract superclass for implementations of PointObsFeature.
 * Concrete subclass must implement getData();
 *
 * @author caron
 */


public class PointObsFeatureImpl implements PointObsFeature, Comparable<PointObsFeature> {
  protected EarthLocation location;
  protected double obsTime, nomTime;
  protected DateUnit timeUnit;

  public PointObsFeatureImpl() {
  }

  public PointObsFeatureImpl( EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit) {
    this.location = location;
    this.obsTime = obsTime;
    this.nomTime = nomTime;
    this.timeUnit = timeUnit;
  }

  public EarthLocation getLocation() { return location; }
  public double getNominalTime() { return nomTime; }
  public double getObservationTime() { return obsTime; }

  public String getId() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getDescription() {
    return null;
  }

  public int getNumberPoints() {
    return 1;
  }

  public double getObservationTime(int pt) throws IOException, InvalidRangeException {
    return getObservationTime();
  }

  public Date getObservationTimeAsDate(int pt) throws IOException, InvalidRangeException {
    return getObservationTimeAsDate();
  }

  public ucar.nc2.units.DateUnit getTimeUnits() { return timeUnit; }

  public double getLatitude(int pt) throws IOException, InvalidRangeException {
    return location.getLatitude();
  }

  public double getLongitude(int pt) throws IOException, InvalidRangeException {
    return location.getLongitude();
  }

  public double getZcoordinate(int pt) throws IOException, InvalidRangeException {
    return location.getAltitude();
  }

  public String getZcoordUnits() {
    return "meters";
  }

  public List<VariableSimpleIF> getDataVariables() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public VariableSimpleIF getDataVariable(String name) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public StructureData getData(int pt) throws IOException, InvalidRangeException {
    return getData();
  }

  public Array getData(int pt, String memberName) throws IOException, InvalidRangeException {
    return getData( memberName);
  }

  public Array getData(String memberName) throws IOException, InvalidRangeException {
    StructureData sdata = getData();
    return sdata.getArray(memberName);
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public DataCost getDataCost() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Date getObservationTimeAsDate() {
    return timeUnit.makeDate( getObservationTime());
  }

  public Date getNominalTimeAsDate() {
    return timeUnit.makeDate( getNominalTime());
  }

  public int compareTo(PointObsFeature other) {
    if (obsTime < other.getObservationTime()) return -1;
    if (obsTime > other.getObservationTime()) return 1;
    return 0;
  }
}