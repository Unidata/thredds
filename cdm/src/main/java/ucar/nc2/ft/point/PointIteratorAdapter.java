/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point;

import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Adapt a FeatureIterator to a PointFeatureIterator
 *
 * @author caron
 * @since Mar 19, 2008
 */
public class PointIteratorAdapter implements PointFeatureIterator {
  private PointFeatureCollectionIterator fiter;
  private LatLonRect filter_bb;
  private DateRange filter_date;

  private PointFeatureIterator pfiter;
  private PointFeature pointFeature;
  private boolean done = false;

  PointIteratorAdapter(PointFeatureCollectionIterator fiter, LatLonRect filter_bb, DateRange filter_date) {
    this.fiter = fiter;
    this.filter_bb = filter_bb;
    this.filter_date = filter_date;
  }

  public void setBufferSize(int bytes) {
    fiter.setBufferSize(bytes);
  }

  public boolean hasNext() throws IOException {
    if (done) return false;

    pointFeature = nextFilteredDataPoint();
    if (pointFeature != null) return true;

    PointFeatureCollection feature = nextFilteredCollection();
    if (feature == null) {
      done = true;
      return false;
    }

    pfiter = feature.getPointFeatureIterator(-1);
    return hasNext();
  }

  public PointFeature next() throws IOException {
    return done ? null : pointFeature;
  }

  private PointFeatureCollection nextFilteredCollection() throws IOException {
    if (!fiter.hasNext()) return null;
    PointFeatureCollection f = (PointFeatureCollection) fiter.next();

    if (filter_bb == null)
      return f;

    /* while (!filter_bb.contains(f.getLatLon())) {
      if (!fiter.hasNext()) return null;
      f = (PointFeatureCollection) fiter.nextFeature();
    } LOOK */
    return f;
  }

  private PointFeature nextFilteredDataPoint() throws IOException {
    if (pfiter == null) return null;
    if (!pfiter.hasNext()) return null;
    PointFeature pdata = pfiter.next();

    if (filter_date == null)
      return pdata;

    while (!filter_date.included(pdata.getObservationTimeAsDate())) {
      if (!pfiter.hasNext()) return null;
      pdata = pfiter.next();
    }
    return pdata;
  }

}
