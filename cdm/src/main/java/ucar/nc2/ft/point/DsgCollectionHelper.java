/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Helper class for DsgFeatureCollection
 *
 * @author caron
 * @since 9/23/2015.
 */
public class DsgCollectionHelper {

  DsgFeatureCollection dsg;

  public DsgCollectionHelper(DsgFeatureCollection dsg) {
    this.dsg = dsg;
  }

  public Info calcBounds() throws IOException {
    if (dsg instanceof PointFeatureCollection)
      return calcBounds((PointFeatureCollection) dsg);
    else if (dsg instanceof NestedPointFeatureCollection) {
      NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) dsg;
      if (!npfc.isMultipleNested())
        return calcBounds(npfc.getPointFeatureCollectionIterator(-1));
      else
        return calcBounds(npfc.getNestedPointFeatureCollectionIterator(-1));
    }

    throw new IllegalStateException(dsg.getClass().getName());
  }

  private Info calcBounds(PointFeatureCollection pfc) {

    LatLonRect bbox = null;
    double minTime = Double.MAX_VALUE;
    double maxTime = -Double.MAX_VALUE;

    for (PointFeature pf : pfc) {
      CalendarDate cd = pf.getObservationTimeAsCalendarDate();

      if (bbox == null)
        bbox = new LatLonRect(pf.getLocation().getLatLon(), .001, .001);
      else
        bbox.extend(pf.getLocation().getLatLon());

      double obsTime = pf.getObservationTime();
      minTime = Math.min(minTime, obsTime);
      maxTime = Math.max(maxTime, obsTime);
    }

    CalendarDateUnit cdu = dsg.getTimeUnit();
    CalendarDateRange dateRange = CalendarDateRange.of(cdu.makeCalendarDate(minTime), cdu.makeCalendarDate(maxTime));

    return new Info(bbox, dateRange);
  }

  private Info calcBounds(NestedPointFeatureCollectionIterator iter) throws IOException {

    Info result = null;

    while (iter.hasNext()) {
      NestedPointFeatureCollection npfc = iter.next();
      Info b = calcBounds(npfc.getPointFeatureCollectionIterator(-1));
      if (result == null)
        result = b;
      else
        result.extend(b);
    }

    return result;
  }

  private Info calcBounds(PointFeatureCollectionIterator iter) throws IOException {

    Info result = null;

    while (iter.hasNext()) {
      PointFeatureCollection pfc = iter.next();
      Info b = calcBounds(pfc);
      if (result == null)
        result = b;
      else
        result.extend(b);
    }

    return result;
  }

  public static class Info {
    LatLonRect bbox;
    CalendarDateRange dateRange;

    public Info(LatLonRect bbox, CalendarDateRange dateRange) {
      this.bbox = bbox;
      this.dateRange = dateRange;
    }

    void extend(Info info) {
      bbox.extend(info.bbox);
      dateRange = dateRange.extend(info.dateRange);
    }
  }
}
