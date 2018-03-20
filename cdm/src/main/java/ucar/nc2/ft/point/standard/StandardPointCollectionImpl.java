/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;

import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Implementation of PointFeatureCollection using a NestedTable
 * @author caron
 * @since Mar 28, 2008
 */
public class StandardPointCollectionImpl extends PointCollectionImpl {
  private NestedTable ft;

  StandardPointCollectionImpl(NestedTable ft, CalendarDateUnit timeUnit, String altUnits) {
    super(ft.getName(), timeUnit, altUnits);
    this.ft = ft;
    this.extras = ft.getExtras();
  }

  @Override
  public PointFeatureIterator getPointFeatureIterator() throws IOException {
    // only one Cursor object needed - it will be used for each iteration with different structData's
    Cursor tableData = new Cursor(ft.getNumberOfLevels());

    return new StandardPointFeatureIterator(this, ft, timeUnit, ft.getObsDataIterator(tableData), tableData);
  }

}
