/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ncml4;

import ucar.nc2.ncml.AggregationIF;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;

import java.util.List;
import java.util.Date;
import java.io.File;

import thredds.util.DateFromString;

/**
 * Encapsolate a "scan" or "scan2" element: a directory that we want to scan.
 *
 * @author caron
 * @since Aug 10, 2007
 */
public class DirectoryScan implements Scanner {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DirectoryScan.class);

  private String dirName, dateFormatMark;
  private String runMatcher, forecastMatcher, offsetMatcher; // scan2
  private boolean enhance = false, isDate = false;
  private boolean wantSubdirs = true;

  // filters
  private String suffix;
  private java.util.regex.Pattern regexpPattern = null;
  private long olderThan_msecs; // files must not have been modified for this amount of time (msecs)

  private DateFormatter formatter = new DateFormatter();
  private boolean debugScan = false;

  DirectoryScan(AggregationIF.Type type, String dirName, String suffix, String regexpPatternString, String dateFormatMark, String enhanceS, String subdirsS, String olderS) {
    this.dirName = dirName;
    this.suffix = suffix;
    if (null != regexpPatternString)
      this.regexpPattern = java.util.regex.Pattern.compile(regexpPatternString);

    this.dateFormatMark = dateFormatMark;
    if ((enhanceS != null) && enhanceS.equalsIgnoreCase("true"))
      enhance = true;
    if ((subdirsS != null) && subdirsS.equalsIgnoreCase("false"))
      wantSubdirs = false;
    if (type == AggregationIF.Type.FORECAST_MODEL_COLLECTION)
      enhance = true;

    if (olderS != null) {
      try {
        TimeUnit tu = new TimeUnit(olderS);
        this.olderThan_msecs = (long) (1000 * tu.getValueInSeconds());
      } catch (Exception e) {
        logger.error("Invalid time unit for olderThan = {}", olderS);
      }
    }

    if (dateFormatMark != null)
      isDate = true;
  }

  DirectoryScan(AggregationIF.Type type, String dirName, String suffix, String regexpPatternString, String subdirsS, String olderS,
                String runMatcher, String forecastMatcher, String offsetMatcher) {
    this(type, dirName, suffix, regexpPatternString, null, "true", subdirsS, olderS);

    this.runMatcher = runMatcher;
    this.forecastMatcher = forecastMatcher;
    this.offsetMatcher = offsetMatcher;
  }

  public boolean isEnhance() { return enhance; }
  public String getDateFormatMark() { return dateFormatMark; }


  /**
   * Recursively crawl directories, add matching MyFile files to result List
   *
   * @param result     add MyFile objects to this list
   * @param cancelTask user can cancel
   */
  public void scanDirectory(List<MyCrawlableDataset> result, CancelTask cancelTask) {
    scanDirectory(dirName, new Date().getTime(), result, cancelTask);
  }

  private void scanDirectory(String dirName, long now, List<MyCrawlableDataset> result, CancelTask cancelTask) {
    File allDir = new File(dirName);
    if (!allDir.exists()) {
      String tmpMsg = "Non-existent scan location <" + dirName;
      logger.error("scanDirectory(): " + tmpMsg);
      throw new IllegalArgumentException(tmpMsg);
    }

    for (File f : allDir.listFiles()) {
      String location = f.getAbsolutePath();

      if (f.isDirectory()) {
        if (wantSubdirs) scanDirectory(location, now, result, cancelTask);

      } else if (accept(location)) {
        // dont allow recently modified
        if (olderThan_msecs > 0) {
          long lastModified = f.lastModified();
          if (now - lastModified < olderThan_msecs)
            continue;
        }

        /* add to result
        MyCrawlableDataset myf = new MyCrawlableDataset(this, f);
        result.add(myf);

        /* if (null != myf.dir.getDateFormatMark()) {
          String filename = myf.file.getName();
          myf.dateCoord = DateFromString.getDateUsingDemarkatedCount(filename, myf.dir.getDateFormatMark(), '#');
          myf.dateCoordS = formatter.toDateTimeStringISO(myf.dateCoord);
          if (debugScan) System.out.println("  adding " + myf.file.getPath() + " date= " + myf.dateCoordS);
        } else {
          if (debugScan) System.out.println("  adding " + myf.file.getPath());
        } */
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

  protected boolean accept(String location) {
    if (null != regexpPattern) {
      java.util.regex.Matcher matcher = regexpPattern.matcher(location);
      return matcher.matches();
    }

    return (suffix == null) || location.endsWith(suffix);
  }

}