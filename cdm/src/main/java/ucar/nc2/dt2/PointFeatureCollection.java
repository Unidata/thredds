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
 * A Collection of PointFeatures.
 *
 * @author caron
 * @since Mar 1, 2008
 */
public interface PointFeatureCollection extends FeatureCollection {

  // an iterator over Features of type PointFeature
  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws java.io.IOException;

  // create a subset
  // if empty, may return null
  public PointFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox, DateRange dateRange) throws IOException;

}
