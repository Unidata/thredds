/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateFromString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import thredds.inventory.DateExtractor;
import thredds.inventory.MFile;

/**
 * Extract Date from filename, using DateFromString.getDateUsingSimpleDateFormat on the name or path
 *
 * @author caron
 * @since Jun 26, 2009
 */
public class DateExtractorFromName implements DateExtractor {

  private String dateFormatMark;
  private boolean useName;

  /**
   * Ctor
   * @param dateFormatMark DemarkatedCount or DemarkatedMatch
   * @param useName use name if true, else use path
   */
  public DateExtractorFromName(String dateFormatMark, boolean useName) {
    this.dateFormatMark = dateFormatMark;
    this.useName = useName;
  }

  @Override
  public Date getDate(MFile mfile) {
    if (useName)
      return DateFromString.getDateUsingDemarkatedCount(mfile.getName(), dateFormatMark, '#');
    else
      return DateFromString.getDateUsingDemarkatedMatch(mfile.getPath(), dateFormatMark, '#');
  }

  @Override
  public CalendarDate getCalendarDate(MFile mfile) {
    Date d = getDate(mfile);
    return (d == null) ? null : CalendarDate.of(d);
  }

  @Override
  public CalendarDate getCalendarDateFromPath(String path) {
    Date d;
    if (useName) {
      Path p = Paths.get(path);
      d = DateFromString.getDateUsingDemarkatedCount(p.getFileName().toString(), dateFormatMark, '#');
    } else
      d = DateFromString.getDateUsingDemarkatedMatch(path, dateFormatMark, '#');

    return (d == null) ? null : CalendarDate.of(d);
  }

  @Override
  public String toString() {
    return "DateExtractorFromName{" +
            "dateFormatMark='" + dateFormatMark + '\'' +
            ", useName=" + useName +
            '}';
  }

}
