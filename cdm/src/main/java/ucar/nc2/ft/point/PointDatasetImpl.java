/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point;

import ucar.nc2.Variable;
import ucar.nc2.ft.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.time.CalendarDateRange;
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
public class PointDatasetImpl extends FeatureDatasetImpl implements FeatureDatasetPoint {
  protected List<FeatureCollection> collectionList;
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

  protected void setPointFeatureCollection(List<FeatureCollection> collectionList) {
    this.collectionList = collectionList;
  }

  protected void setPointFeatureCollection(FeatureCollection collection) {
    this.collectionList = new ArrayList<>(1);
    this.collectionList.add(collection);
  }

  @Override
  public FeatureType getFeatureType() {
    return featureType;
  }

  protected void setFeatureType(FeatureType ftype) {
    this.featureType = ftype;
  }

  @Override
  public List<FeatureCollection> getPointFeatureCollectionList() {
    return collectionList;
  }

  @Override
  public void calcBounds() throws java.io.IOException {  // LOOK this sucks
    if ((boundingBox != null) && (dateRange != null)) return;

    LatLonRect bb = null;
    CalendarDateRange dates = null;
    for (FeatureCollection fc : collectionList) {

      if (fc instanceof PointFeatureCollection) {
        PointFeatureCollection pfc = (PointFeatureCollection) fc;
        pfc.calcBounds();
        if (bb == null)
          bb = pfc.getBoundingBox();
        else
          bb.extend(pfc.getBoundingBox());
        if (dates == null)
          dates = pfc.getCalendarDateRange();
        else
          dates.extend(pfc.getCalendarDateRange());

      }  else if (fc instanceof StationTimeSeriesFeatureCollection) {

        StationTimeSeriesFeatureCollection sc = (StationTimeSeriesFeatureCollection) fc;
        if (this.boundingBox == null) {
          if (bb == null) {
            bb = sc.getBoundingBox();
          } else if (sc.getBoundingBox() != null) {
            bb.extend(sc.getBoundingBox());
          }
        }

        if (dateRange == null) {
          PointFeatureCollection pfc = sc.flatten(null, (CalendarDateRange) null);
          pfc.calcBounds();
          if (dates == null)
            dates = pfc.getCalendarDateRange();
          else
            dates.extend(pfc.getCalendarDateRange());
        }
      }

    }

    if (boundingBox == null) boundingBox = bb;
    if (dateRange == null) dateRange = dates;
  }

  @Override
  public void getDetailInfo(java.util.Formatter sf) {
    super.getDetailInfo(sf);

    int count = 0;
    for (FeatureCollection fc : collectionList) {
      if (fc instanceof PointFeatureCollection) {
        sf.format("%nPointFeatureCollection %d %n", count);
        PointFeatureCollection pfc = (PointFeatureCollection) fc;
        sf.format(" %s %s %n", pfc.getCollectionFeatureType(), pfc.getName());
        sf.format("   npts = %d %n", pfc.size());
        sf.format("     bb = %s %n", pfc.getBoundingBox() == null ? "" : pfc.getBoundingBox().toString2());
        sf.format("  dates = %s %n", pfc.getCalendarDateRange());
        List<Variable> extra = pfc.getExtraVariables();
        if (extra.size() > 0) {
          sf.format("  extra variables = ");
          for (Variable v : extra) sf.format("%s,", v.getNameAndDimensions());
          sf.format("%n");
        }
        sf.format("   timeUnit = %s %n", pfc.getTimeUnit());
        sf.format("    altUnit = %s %n", pfc.getAltUnits());

    } else if (fc instanceof NestedPointFeatureCollection) {
        sf.format("NestedPointFeatureCollection %d %n", count);
        NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) fc;
        sf.format(" %s %s %n", npfc.getCollectionFeatureType(), npfc.getName());
        sf.format("      npts = %d %n", npfc.size());
        sf.format("    multipleNested = %s %n", npfc.isMultipleNested());
        List<Variable> extra = npfc.getExtraVariables();
        if (extra.size() > 0) {
          sf.format("  extra variables = ");
          for (Variable v : extra) sf.format("%s,", v.getNameAndDimensions());
          sf.format("%n");
        }
        sf.format("   timeUnit = %s %n", npfc.getTimeUnit());
        sf.format("    altUnit = %s %n", npfc.getAltUnits());
      }
      count++;
    }
  }

}
