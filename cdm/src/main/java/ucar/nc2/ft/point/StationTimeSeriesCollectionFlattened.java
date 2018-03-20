/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import java.io.IOException;
import javax.annotation.Nonnull;

import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;

/**
 * A flattened StationTimeSeriesCollection.
 *
 * @author caron
 * @since Aug 27, 2009
 */
public class StationTimeSeriesCollectionFlattened extends PointCollectionImpl {
  protected StationTimeSeriesCollectionImpl from;

  public StationTimeSeriesCollectionFlattened(StationTimeSeriesCollectionImpl from, CalendarDateRange dateRange) {
    super( from.getName(), from.getTimeUnit(), from.getAltUnits());
    this.from = from;
    if (dateRange != null) {
      getInfo();
      info.setCalendarDateRange(dateRange);
    }
  }

  @Override
  @Nonnull
  public PointFeatureIterator getPointFeatureIterator() throws IOException {
    return new PointIteratorFlatten( from.getPointFeatureCollectionIterator(), null, this.getCalendarDateRange());
  }

}

