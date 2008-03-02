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
import ucar.nc2.units.DateRange;
import ucar.nc2.VariableSimpleIF;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.List;

/**
 * Superclass for implementations of StationFeature.
 * Concrete subclass must implement getData();
 *
 * @author caron
 */


public class StationFeatureImpl extends StationImpl implements StationFeature {
  protected List<VariableSimpleIF> dataVariables;
  protected DateUnit timeUnit;
  protected int npts;

  protected PointFeatureIterator pfiter;
  protected FeatureIteratorAdapter iter;

  private LatLonRect filter_bb;
  private DateRange filter_date;

  public StationFeatureImpl( List<VariableSimpleIF> dataVariables, String name, String desc, double lat, double lon, double alt, DateUnit timeUnit, int npts) {
    super(name, desc, lat, lon, alt);
    this.dataVariables = dataVariables;
    this.timeUnit = timeUnit;
    this.npts = npts;
  }

  // copy constructor
  protected StationFeatureImpl( StationFeatureImpl from, LatLonRect boundingBox, DateRange dateRange) {
    super(from.name, from.desc, from.lat, from.lon, from.alt);
    this.timeUnit = from.timeUnit;
    this.npts = -1;

    if (from.filter_bb == null)
      this.filter_bb = boundingBox;
    else
      this.filter_bb = (boundingBox == null) ? from.filter_bb : from.filter_bb.intersect( boundingBox);

    if (from.filter_date == null)
      this.filter_date = dateRange;
    else
      this.filter_date = (dateRange == null) ? from.filter_date : from.filter_date.intersect( dateRange);

    iter = new FeatureIteratorAdapter(pfiter, this.filter_bb, this.filter_date);
  }

  protected void setIterator( PointFeatureIterator pfiter) {
    this.pfiter = pfiter;
    this.iter = new FeatureIteratorAdapter(pfiter, this.filter_bb, this.filter_date);
  }

    // an iterator over Features of type getCollectionFeatureType
  public FeatureIterator getFeatureIterator(int bufferSize) throws IOException {
    iter.setBufferSize( bufferSize);
    return iter;
  }

  // an iterator over Features of type PointFeature
  public PointFeatureIterator getPointIterator(int bufferSize) throws IOException {
    iter.setBufferSize( bufferSize);
    return iter;
  }

  public String getId() { return name; }

  // the data variables to be found in the PointFeature
  public List<VariableSimpleIF> getDataVariables() {
    return dataVariables;
  }

  // All features in this collection have this feature type
  public Class getCollectionFeatureType() {
    return PointFeature.class;
  }

  // create a subset
  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new StationFeatureImpl( this, boundingBox, dateRange);
  }

  public int getNumberPoints() {
    return npts;
  }
}