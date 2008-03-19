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

import ucar.nc2.dt2.PointFeatureCollection;
import ucar.nc2.dt2.FeatureIterator;
import ucar.nc2.dt2.PointFeatureIterator;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;
import java.io.IOException;

/**
 * Abstract superclass for PointFeatureCollection
 * Subclass must call setIterators(), and implement subset()
 * @author caron
 * @since Mar 1, 2008
 */
public abstract class PointFeatureCollectionImpl implements PointFeatureCollection {
  protected Class featureClass;
  protected List<? extends VariableSimpleIF> dataVariables;
  protected FeatureIterator fiter;
  protected PointFeatureIterator pfiter;

  protected PointFeatureCollectionImpl(Class featureClass, List<? extends VariableSimpleIF> dataVariables) {
    this.featureClass = featureClass;
    this.dataVariables = dataVariables;
  }

  protected void setIterators( FeatureIterator fiter, PointFeatureIterator pfiter) {
    this.fiter = fiter;
    this.pfiter = pfiter;
  }

  // copy constructor
  protected PointFeatureCollectionImpl(PointFeatureCollectionImpl from) {
    this.featureClass = from.featureClass;
    this.dataVariables = from.dataVariables;
    this.fiter = from.fiter;
    this.pfiter = from.pfiter;
  }

  // the data variables to be found in the PointFeature
  public List<? extends VariableSimpleIF> getDataVariables() {
    return dataVariables;
  }

  // All features in this collection have this feature type
  public Class getCollectionFeatureType() {
    return featureClass;
  }

  // an iterator over Features of type getCollectionFeatureType
  public FeatureIterator getFeatureIterator(int bufferSize) throws IOException {
    fiter.setBufferSize( bufferSize);
    return fiter;
  }

  // an iterator over Features of type PointFeature
  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
    pfiter.setBufferSize( bufferSize);
    return pfiter;
  }

}
