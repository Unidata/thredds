/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.collection;

import ucar.nc2.time.CalendarDateRange;

import java.io.IOException;

/**
 * Mixin for update() method.
 *
 * @author caron
 * @since Nov 22, 2010
 */
public interface UpdateableCollection {
  CalendarDateRange update() throws IOException;
}
