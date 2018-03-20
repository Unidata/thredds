/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import ucar.nc2.time.CalendarDate;

import java.util.Date;

/**
 * Always returns null
 *
 * @author caron
 * @since Jan 15, 2010
 */
public class DateExtractorNone implements DateExtractor {
  public Date getDate(MFile mfile) {
    return null;
  }
  public CalendarDate getCalendarDate(MFile mfile) {
    return null;
  }
  public CalendarDate getCalendarDateFromPath(String path) {
    return null;
  }
}
