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
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;
import java.util.ArrayList;

/**
 * Implementation of PointFeatureDataset.
 * All of the specialization is in List<PointFeatureCollection> collectionList.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public class PointDatasetImpl extends FeatureDatasetImpl implements PointFeatureDataset {
  protected List<FeatureCollection> collectionList;
  protected Class featureInterface;

  // subsetting
  protected PointDatasetImpl(PointDatasetImpl from, LatLonRect filter_bb, DateRange filter_date) {
    super(from);
    this.collectionList = from.collectionList;
    this.featureInterface = from.featureInterface;

    if (filter_bb == null)
      this.boundingBox = from.boundingBox;
    else
      this.boundingBox = (from.boundingBox == null) ? filter_bb : from.boundingBox.intersect( filter_bb);

    if (filter_date == null) {
      this.dateRange = from.dateRange;
    } else {
      this.dateRange =  (from.dateRange == null) ? filter_date : from.dateRange.intersect( filter_date);
    }
  }

  public PointDatasetImpl(NetcdfDataset ds, Class featureInterface) {
    super(ds);
    this.featureInterface = featureInterface;
  }

  protected void setPointFeatureCollection(List<FeatureCollection> collectionList) {
    this.collectionList = collectionList;
  }

  protected void setPointFeatureCollection(FeatureCollection collection) {
    this.collectionList = new ArrayList<FeatureCollection>(1);
    this.collectionList.add(collection);
  }

  public FeatureType getFeatureType() {
    return FeatureType.POINT;
  }

  public List<FeatureCollection> getPointFeatureCollectionList() {
    return collectionList;
  }

  public void getDetailInfo( java.util.Formatter sf) {
    super.getDetailInfo(sf);

    sf.format("\nFeatureCollections\n");
    for (FeatureCollection fc : collectionList) {
      sf.format(" %s type=%s\n", fc.getName(), fc.getCollectionFeatureType());
    }
  }
}
