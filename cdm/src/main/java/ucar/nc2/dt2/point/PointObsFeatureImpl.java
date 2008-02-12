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

import ucar.nc2.dt2.*;
import ucar.nc2.units.DateUnit;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.Array;

import java.util.Date;
import java.io.IOException;

/**
 * Abstract superclass for implementations of PointObsFeature.
 * Concrete subclass must implement getId(), getData();
 *
 * @author caron
 */


public abstract class PointObsFeatureImpl extends ObsFeatureImpl implements PointObsFeature, Comparable<PointObsFeature> {
  protected EarthLocation location;
  protected double obsTime, nomTime;

  public PointObsFeatureImpl( FeatureDataset fd, DateUnit timeUnit) {
    super(fd, timeUnit);
  }

  public PointObsFeatureImpl( FeatureDataset fd, EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit) {
    super(fd, timeUnit);
    this.location = location;
    this.obsTime = obsTime;
    this.nomTime = nomTime;
    this.timeUnit = timeUnit;
  }

  public EarthLocation getLocation() { return location; }
  public double getNominalTime() { return nomTime; }
  public double getObservationTime() { return obsTime; }

  public int getNumberPoints() {
    return 1;
  }

  public double getObservationTime(int pt) throws IOException, InvalidRangeException {
    return getObservationTime();
  }

  public Date getObservationTimeAsDate(int pt) throws IOException, InvalidRangeException {
    return getObservationTimeAsDate();
  }

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
    return new DegenerateIterator();
  }

  private class DegenerateIterator implements DataIterator {
    boolean done;
    public boolean hasNext() throws IOException { return !done; }
    public Object nextData() {
      done = true;
      return PointObsFeatureImpl.this;
    }
  }

  public DataCost getDataCost() {
    return new DataCost(1, 1);
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