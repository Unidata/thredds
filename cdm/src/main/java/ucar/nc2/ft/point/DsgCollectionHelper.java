/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import javax.annotation.Nonnull;

import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCC;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonRect;

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

  @Nonnull
  public CollectionInfo calcBounds() throws IOException {
    if (dsg instanceof PointFeatureCollection)
      return calcBounds((PointFeatureCollection) dsg);

    else if (dsg instanceof PointFeatureCC) {
      return calcBounds((PointFeatureCC) dsg);

    } else if (dsg instanceof PointFeatureCCC) {
      return calcBounds((PointFeatureCCC) dsg);
    }

    throw new IllegalStateException(dsg.getClass().getName());
  }

  private CollectionInfo calcBounds(PointFeatureCollection pfc) {

    LatLonRect bbox = null;
    double minTime = Double.MAX_VALUE;
    double maxTime = -Double.MAX_VALUE;
    int count = 0;

    for (PointFeature pf : pfc) {
      if (bbox == null)
        bbox = new LatLonRect(pf.getLocation().getLatLon(), .001, .001);
      else
        bbox.extend(pf.getLocation().getLatLon());

      double obsTime = pf.getObservationTime();
      minTime = Math.min(minTime, obsTime);
      maxTime = Math.max(maxTime, obsTime);
      count++;
    }

    if (count == 0) {
      return new CollectionInfo(null, null, 0, 0);
    }

    CalendarDateUnit cdu = dsg.getTimeUnit();
    CalendarDateRange dateRange = CalendarDateRange.of(cdu.makeCalendarDate(minTime), cdu.makeCalendarDate(maxTime));
    return new CollectionInfo(bbox, dateRange, count, count);
  }

  private CollectionInfo calcBounds(PointFeatureCC pfcc) throws IOException {

    CollectionInfo result = null;
    IOIterator<PointFeatureCollection> iter = pfcc.getCollectionIterator();

    while (iter.hasNext()) {
      PointFeatureCollection pfc = iter.next();
      CollectionInfo b = calcBounds(pfc);
      if (result == null)
        result = b;
      else
        result.extend(b);
    }

    return result;
  }

  private CollectionInfo calcBounds(PointFeatureCCC pfccc) throws IOException {

    CollectionInfo result = null;
    IOIterator<PointFeatureCC> iter = pfccc.getCollectionIterator();

    while (iter.hasNext()) {
      PointFeatureCC pfcc = iter.next();
      CollectionInfo b = calcBounds(pfcc);
      if (result == null)
        result = b;
      else
        result.extend(b);
    }

    return result;
  }

}
