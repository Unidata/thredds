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
package ucar.nc2.dt2;

import ucar.nc2.units.DateRange;
import ucar.nc2.VariableSimpleIF;

import java.io.IOException;
import java.util.List;

/**
 * A Feature that is a collection of other Features, ultimately based on PointFeature.
 *
 * @author caron
 * @since Mar 1, 2008
 */
public interface PointFeatureCollection {

  // the data variables to be found in the PointFeature
  public List<VariableSimpleIF> getDataVariables();

  // All features in this collection have this feature type
  public Class getCollectionFeatureType();

  // an iterator over Features of type getCollectionFeatureType
  public FeatureIterator getFeatureIterator(int bufferSize) throws java.io.IOException;

  // an iterator over Features of type PointFeature
  public PointFeatureIterator getPointIterator(int bufferSize) throws java.io.IOException;

  // create a subset
  public PointFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox, DateRange dateRange) throws IOException;

}
