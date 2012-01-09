/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.inventory;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateFromString;

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
  public String toString() {
    return "DateExtractorFromName{" +
            "dateFormatMark='" + dateFormatMark + '\'' +
            ", useName=" + useName +
            '}';
  }

  public CalendarDate getDate(String  name) {
    Date d = null;
    if (useName)
      d = DateFromString.getDateUsingDemarkatedCount(name, dateFormatMark, '#');
    else
      d = DateFromString.getDateUsingDemarkatedMatch(name, dateFormatMark, '#');
    return (d == null) ? null : CalendarDate.of(d);
  }


  static public void doit(String name, String dateFormatMark) {
    DateExtractorFromName de = new DateExtractorFromName(dateFormatMark, false);
    CalendarDate d = de.getDate(name);
    System.out.printf("%s == %s%n", name , d);
  }

  static public void main(String args[]) {
    doit("/san4/work/jcaron/cfsrr/198507", "#cfsrr/#yyyyMM");
    doit("/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/20111226/Run_1200.grib1", "#Alaska_191km/#yyyyMMdd'/Run_'HHmm");
  }
}
