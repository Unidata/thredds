/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import ucar.nc2.time.CalendarDate;

import java.util.HashMap;
import java.util.Map;

/**
 * Reuse immutable calendar date objects.
 * LOOK: This optimization could go away.
 *
 * @author caron
 * @since 4/3/2015
 */
public class CalendarDateFactory {
  private Map<Long, CalendarDate> map;

  public CalendarDateFactory(CoordinateRuntime master) {
    map = new HashMap<>(master.getSize() * 2);
    for (Object valo : master.getValues()) {
      CalendarDate cd = CalendarDate.of((Long) valo);
      map.put(cd.getMillis(), cd);
    }
  }

  public CalendarDate get( CalendarDate cd) {
    CalendarDate cdc = map.get(cd.getMillis());
    if (cdc != null) return cdc;
    map.put(cd.getMillis(), cd);
    return cd;
  }
}
