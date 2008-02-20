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

import ucar.unidata.geoloc.LatLonRect;
import ucar.nc2.units.DateRange;
import ucar.nc2.dt2.*;
import ucar.ma2.StructureData;

import java.io.IOException;

/**
 * Adapts Obs1DFeature to a PointObsDataset
 * @author caron
 * @since Feb 18, 2008
 */
public class PointObsDatasetAdapter extends PointObsDatasetImpl {
  private Obs1DDataset from;
  private LatLonRect filter_bb;
  private DateRange filter_date;

  // turn a StationObsDataset into a PointObsDataset
  PointObsDatasetAdapter(Obs1DDataset from) {
    super( (FeatureDatasetImpl) from, null, null);
    this.from = from;
  }

  // copy constructor
  PointObsDatasetAdapter(PointObsDatasetAdapter from, LatLonRect filter_bb, DateRange filter_date) {
    super(from, filter_bb, filter_date);

    if (from.filter_bb == null)
      this.filter_bb = filter_bb;
    else
      this.filter_bb = (filter_bb == null) ? from.filter_bb : from.filter_bb.intersect( filter_bb);

    if (from.filter_date == null)
      this.filter_date = filter_date;
    else
      this.filter_date = (filter_date == null) ? from.filter_date : from.filter_date.intersect( filter_date);
  }


  public PointObsDataset subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new PointObsDatasetAdapter( this, boundingBox, dateRange);
  }

  public FeatureIterator getFeatureIterator(int bufferSize) throws IOException {
    return new FeatureIteratorAdapter(from);
  }

  private class FeatureIteratorAdapter implements FeatureIterator {
    FeatureIterator fiter;
    Obs1DFeature feature;
    DataIterator diter;

    FeatureIteratorAdapter(Obs1DDataset from) throws IOException {
       fiter = from.getFeatureIterator(-1);
       feature = (Obs1DFeature) fiter.nextFeature();
       diter = feature.getDataIterator( -1);
     }

    public boolean hasNext() throws IOException {
      if (diter.hasNext()) return true;
      if (!fiter.hasNext()) return false;
      ObsFeature1DImpl f = (ObsFeature1DImpl) fiter.nextFeature();
      diter = f.getDataIterator( -1);
      if (!diter.hasNext()) return hasNext();
      return true;
    }

    public Feature nextFeature() throws IOException {
      StructureData sdata = diter.nextData();
      return feature.makePointObsFeature(sdata);
    }
  }

  public DataCost getDataCost() {
    return from.getDataCost();
  }
}
