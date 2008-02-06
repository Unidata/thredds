/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.util.Date;
import java.io.IOException;

/**
 * Abstract superclass for implementations of PointObsDataset
 * @author caron
 * @since Sep 7, 2007
 */
public abstract class PointObsDatasetImpl extends FeatureDatasetImpl implements PointObsDataset {

  protected PointObsDatasetImpl(PointObsDatasetImpl from, LatLonRect filter_bb, DateRange filter_date) {
    super(from, filter_bb, filter_date);
  }

  public PointObsDatasetImpl() {}
  public PointObsDatasetImpl(NetcdfDataset ds) {
    super(ds);
  }

  public Class getFeatureClass() {
    return PointObsFeature.class;
  }

}
