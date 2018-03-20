/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.List;

import com.beust.jcommander.internal.Lists;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetImpl;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Implementation of PointFeatureDataset.
 * All of the specialization is in List<DsgFeatureCollection> collectionList.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public class PointDatasetImpl extends FeatureDatasetImpl implements FeatureDatasetPoint {
  protected List<DsgFeatureCollection> collectionList;
  protected FeatureType featureType;

  protected PointDatasetImpl(FeatureType featureType) {
    this.featureType = featureType;
  }

  // subsetting
  protected PointDatasetImpl(PointDatasetImpl from, LatLonRect filter_bb, CalendarDateRange filter_date) {
    super(from);
    this.collectionList = from.collectionList;
    this.featureType = from.featureType;

    if (filter_bb == null)
      this.boundingBox = from.boundingBox;
    else
      this.boundingBox = (from.boundingBox == null) ? filter_bb : from.boundingBox.intersect(filter_bb);

    if (filter_date == null) {
      this.dateRange = from.dateRange;
    } else {
      this.dateRange = (from.dateRange == null) ? filter_date : from.dateRange.intersect(filter_date);
    }
  }

  public PointDatasetImpl(NetcdfDataset ds, FeatureType featureType) {
    super(ds);
    this.featureType = featureType;
  }

  protected void setPointFeatureCollection(List<DsgFeatureCollection> collectionList) {
    this.collectionList = collectionList;
  }

  protected void setPointFeatureCollection(DsgFeatureCollection collection) {
    this.collectionList = Lists.newArrayList(collection);
  }

  @Override
  public FeatureType getFeatureType() {
    return featureType;
  }

  protected void setFeatureType(FeatureType ftype) {
    this.featureType = ftype;
  }

  @Override
  public List<DsgFeatureCollection> getPointFeatureCollectionList() {
    return collectionList;
  }

  @Override
  public void getDetailInfo(java.util.Formatter sf) {
    super.getDetailInfo(sf);

    int count = 0;
    for (DsgFeatureCollection pfc : collectionList) {
      sf.format("%nPointFeatureCollection %d %n", count);
      sf.format(" %s %s %n", pfc.getCollectionFeatureType(), pfc.getName());
      sf.format("   npts = %d %n", pfc.size());
      /* List<Variable> extra = pfc.getExtraVariables();
      if (extra.size() > 0) {
        sf.format("  extra variables = ");
        for (Variable v : extra) sf.format("%s,", v.getNameAndDimensions());
        sf.format("%n");
      } */
      sf.format("   timeUnit = %s %n", pfc.getTimeUnit());
      sf.format("    altUnit = %s %n", pfc.getAltUnits());
      count++;
    }
  }

  @Override
  public void calcBounds(java.util.Formatter sf) {
    for (DsgFeatureCollection pfc : collectionList) {
      try {
        CollectionInfo info  = new DsgCollectionHelper(pfc).calcBounds();
        sf.format("     bb = %s %n", info.bbox == null ? "" :info.bbox.toString2());
        sf.format("  dates = %s %n", info.getCalendarDateRange(pfc.getTimeUnit()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
