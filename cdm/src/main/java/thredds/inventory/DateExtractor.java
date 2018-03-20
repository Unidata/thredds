/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import ucar.nc2.time.CalendarDate;

import java.util.Date;

/**
 * Knows how to extract a date from a MFile.
 *
 * @author caron
 * @since Jun 26, 2009
 */
public interface DateExtractor {
  public Date getDate( MFile mfile); // deprecate
  public CalendarDate getCalendarDate( MFile mfile);
  public CalendarDate getCalendarDateFromPath(String path);

}
