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

import java.io.IOException;

/**
 * Subsets a PointFeatureCollection, turns it into a CollectionOfPointFeatures
 * @author caron
 * @since Feb 18, 2008
 */
public class PointFeatureCollectionSubset extends CollectionOfPointFeatures {
  private PointFeatureCollection from;
  private LatLonRect filter_bb;
  private DateRange filter_date;

  PointFeatureCollectionSubset(PointFeatureCollection from, PointFeatureIterator piter, LatLonRect filter_bb, DateRange filter_date) {
    super( from.getDataVariables(), piter);
    this.from = from;
    this.filter_bb = filter_bb;
    this.filter_date = filter_date;
  }

  // copy constructor
  PointFeatureCollectionSubset(PointFeatureCollectionSubset from, LatLonRect filter_bb, DateRange filter_date) {
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

  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new PointFeatureCollectionSubset( this, boundingBox, dateRange);
  }

  public FeatureIterator getFeatureIterator(int bufferSize) throws IOException {
    return new FeatureIteratorAdapter();
  }

  // iterate through Features, then PointFeature
  private class FeatureIteratorAdapter implements FeatureIterator {
    FeatureIterator fiter;
    PointFeatureIterator diter;
    PointFeature pdata;
    boolean done = false;

    FeatureIteratorAdapter() throws IOException {
      fiter = from.getFeatureIterator(-1);
      FeatureWithPointFeature feature = nextFilteredFeature();
      if (feature == null)
        done = true;
      else
       diter = feature.getDataIterator( -1);
     }

    public boolean hasNext() throws IOException {
      if (done) return false;

      pdata = nextFilteredDataPoint();
      if (pdata != null) return true;

      FeatureWithPointFeature feature = nextFilteredFeature();
      if (feature == null) {
        done = true;
        return false;
      }

      diter = feature.getDataIterator( -1);
      return hasNext();
    }

    public Feature nextFeature() throws IOException {
      if (done) return null;
      return new PointFeatureAdapter(pdata.hashCode(), null, pdata);
    }

    private FeatureWithPointFeature nextFilteredFeature() throws IOException {
      if (!fiter.hasNext()) return null;
      FeatureWithPointFeature f = (FeatureWithPointFeature) fiter.nextFeature();

      if (filter_bb == null)
        return f;

      while (!filter_bb.contains(f.getLatLon())) {
        if (!fiter.hasNext()) return null;
        f = (FeatureWithPointFeature) fiter.nextFeature();
      }
      return f;
    }

    private PointFeature nextFilteredDataPoint() throws IOException {
      if (!diter.hasNext()) return null;
      PointFeature pdata = diter.nextData();

      if (filter_date == null)
        return pdata;

      while (!filter_date.included(pdata.getObservationTimeAsDate())) {
        if (!diter.hasNext()) return null;
        pdata = diter.nextData();
      }
      return pdata;
    }

  }

  // iterate through Features, then PointFeature
  private class FeatureIteratorAdapter2 implements FeatureIterator {
    FeatureDataset featureDataset;
    boolean hasPointFeature;

    FeatureIterator fiter;
    PointFeatureIterator diter;
    PointFeature pdata;
    boolean done = false;

    FeatureIteratorAdapter2(FeatureDataset featureDataset) throws IOException {
      this.featureDataset = featureDataset;
      Class featureClass = featureDataset.getClass();
      hasPointFeature = featureClass.isInstance(FeatureWithPointFeature.class);
    }

    void init() throws IOException {
      fiter = featureDataset.getFeatureIterator(-1);
      FeatureWithPointFeature feature = nextFilteredFeature();
      if (feature == null)
        done = true;
      else
       diter = feature.getDataIterator( -1);
     }

    public boolean hasNext() throws IOException {
      if (done) return false;

      pdata = nextFilteredDataPoint();
      if (pdata != null) return true;

      FeatureWithPointFeature feature = nextFilteredFeature();
      if (feature == null) {
        done = true;
        return false;
      }

      diter = feature.getDataIterator( -1);
      return hasNext();
    }

    // must return a PointFeature
    public Feature nextFeature() throws IOException {
      if (done) return null;
      return new PointFeatureAdapter(pdata.hashCode(), null, pdata);
    }

    private FeatureWithPointFeature nextFilteredFeature() throws IOException {
      if (!fiter.hasNext()) return null;
      FeatureWithPointFeature f = (FeatureWithPointFeature) fiter.nextFeature();

      if (filter_bb == null)
        return f;

      while (!filter_bb.contains(f.getLatLon())) {
        if (!fiter.hasNext()) return null;
        f = (FeatureWithPointFeature) fiter.nextFeature();
      }
      return f;
    }

    private PointFeature nextFilteredDataPoint() throws IOException {
      if (!diter.hasNext()) return null;
      PointFeature pdata = diter.nextData();

      if (filter_date == null)
        return pdata;

      while (!filter_date.included(pdata.getObservationTimeAsDate())) {
        if (!diter.hasNext()) return null;
        pdata = diter.nextData();
      }
      return pdata;
    }

  }

  public DataCost getDataCost() {
    return from.getDataCost();
  }
}
