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
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.StructureData;

import java.io.IOException;

/**
 * Decorate a PointFeatureIterator with filtering on dateRange and/or bounding box.
 * @author caron
 * @since Mar 20, 2008
 */
public class PointIteratorFiltered extends PointIteratorAbstract {
  private PointFeatureIterator orgIter;
  private LatLonRect filter_bb;
  private DateRange filter_date;

  private PointFeature pointFeature;
  private boolean finished = false;

  PointIteratorFiltered(PointFeatureIterator orgIter, LatLonRect filter_bb, DateRange filter_date) {
    this.orgIter = orgIter;
    this.filter_bb = filter_bb;
    this.filter_date = filter_date;
  }

  public void setBufferSize(int bytes) {
    orgIter.setBufferSize(bytes);
  }

  public void finish() {
    if (finished) return;
    orgIter.finish();
    finishCalcBounds();
    finished = true;
  }

  public boolean hasNext() throws IOException {
    pointFeature = nextFilteredDataPoint();
    boolean done = (pointFeature == null);
    if (done) finish();
    return !done;
  }

  public PointFeature next() throws IOException {
    if (pointFeature == null) return null;
    calcBounds(pointFeature);
    return pointFeature;
  }

  private boolean filter(PointFeature pdata) {
    if ((filter_date != null) && !filter_date.included(pdata.getObservationTimeAsDate()))
      return false;

    if ((filter_bb != null) && !filter_bb.contains(pdata.getLocation().getLatitude(), pdata.getLocation().getLongitude()))
      return false;

    return true;
  }

  private PointFeature nextFilteredDataPoint() throws IOException {
    if ( orgIter == null) return null;
    if (!orgIter.hasNext()) return null;

    PointFeature pdata = orgIter.next();
    while (!filter(pdata)) {
      if (!orgIter.hasNext()) return null;
      pdata = orgIter.next();
    }

    return pdata;
  }

}

