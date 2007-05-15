// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.server.ncSubset;

import ucar.nc2.units.TimeUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;

/**
 * Manage collection of files.
 * temp kludge until we consolidate NcML agg and DatasetScan (CrawlableDataset)
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class CollectionManager {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CollectionManager.class);


  private ArrayList scanList = new ArrayList(); // current set of DirectoryScan for scan elements
  private TimeUnit recheck; // how often to recheck
  private long lastChecked; // last time checked
  private boolean wasChanged = true; // something changed since last aggCache file was written
  private boolean isDate = false;  // has a dateFormatMark, so agg coordinate variable is a Date

  private DateFormatter formatter = new DateFormatter();

  private String dirName, dateFormatMark;
  private String runMatcher, forecastMatcher, offsetMatcher; // scan2
  private boolean wantSubdirs = true;

  // filters
  private String suffix;
  private java.util.regex.Pattern regexpPattern = null;
  private long olderThan_msecs; // files must not have been modified for this amount of time (msecs)

  public CollectionManager(String dirName, String suffix, String regexpPatternString, boolean wantSubdirs, String olderS) {
    this.dirName = dirName;
    this.suffix = suffix;
    if (null != regexpPatternString)
      this.regexpPattern = java.util.regex.Pattern.compile(regexpPatternString);

    this.wantSubdirs = wantSubdirs;

    if (olderS != null) {
      try {
        TimeUnit tu = new TimeUnit(olderS);
        this.olderThan_msecs = (long) (1000 * tu.getValueInSeconds());
      } catch (Exception e) {
        logger.error("Invalid time unit for olderThan = {}", olderS);
      }
    }
  }

  /**
   * Recursively crawl directories, add matching MyFile files to result List
   *
   * @param result     add MyFile objects to this list
   * @param cancelTask user can cancel
   */
  public void scanDirectory(List<MyFile> result, CancelTask cancelTask) {
    scanDirectory(dirName, new Date().getTime(), result, cancelTask);
  }

  private void scanDirectory(String dirName, long now, List<MyFile> result, CancelTask cancelTask) {
    File allDir = new File(dirName);
    if (!allDir.exists()) {
      String tmpMsg = "Non-existent scan location <" + dirName + ">.";
      logger.error("scanDirectory(): " + tmpMsg);
      throw new IllegalArgumentException(tmpMsg);
    }
    File[] allFiles = allDir.listFiles();
    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
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

        // add to result
        result.add(new MyFile(this, f));
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

  private boolean debug, debugSyncDetail;
  // check if recheckEvery time has passed

  /**
   * Rescan if recheckEvery time has passed
   *
   * @return if theres new datasets, put new datasets into nestedDatasets
   * @throws IOException
   */
  protected boolean timeToRescan() {
    // see if we need to recheck
    if (recheck == null) {
      if (debugSyncDetail) System.out.println(" *Sync not needed, recheck is null");
      return false;
    }

    Date now = new Date();
    Date lastCheckedDate = new Date(lastChecked);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      if (debug) System.out.println(" *Sync not needed, last= " + lastCheckedDate + " now = " + now);
      return false;
    }

    return true;
  }

  /* private boolean rescan() throws IOException {

    // ok were gonna recheck
    lastChecked = System.currentTimeMillis();
    if (debug) System.out.println(" *Sync at " + new Date());

    // rescan
    ArrayList newDatasets = new ArrayList();
    scan(newDatasets, null);

    // replace with previous datasets if they exist
    boolean changed = false;
    for (int i = 0; i < newDatasets.size(); i++) {
      Dataset newDataset = (Dataset) newDatasets.get(i);
      int index = nestedDatasets.indexOf(newDataset); // equal if location is equal
      if (index >= 0) {
        newDatasets.set(i, nestedDatasets.get(index));
        if (debugSyncDetail) System.out.println("  sync using old Dataset= " + newDataset.location);
      } else {
        changed = true;
        if (debugSyncDetail) System.out.println("  sync found new Dataset= " + newDataset.location);
      }
    }

    if (!changed) { // check for deletions
      for (int i = 0; i < nestedDatasets.size(); i++) {
        Dataset oldDataset = (Dataset) nestedDatasets.get(i);
        if ((newDatasets.indexOf(oldDataset) < 0) && (explicitDatasets.indexOf(oldDataset) < 0)) {
          changed = true;
          if (debugSyncDetail) System.out.println("  sync found deleted Dataset= " + oldDataset.location);
        }
      }
    }

    if (!changed) return false;

    // recreate the list of datasets
    nestedDatasets = new ArrayList();
    nestedDatasets.addAll(explicitDatasets);
    nestedDatasets.addAll(newDatasets);

    return true;
  }  */

  /**
   * Encapsolate a file that was scanned.
   * Created in scanDirectory()
   */
  public class MyFile {
    CollectionManager dir;
    File file;

    Date dateCoord; // will have both or neither
    String dateCoordS;

    Date runDate; // fmrcHourly only
    Double offset;

    MyFile(CollectionManager dir, File file) {
      this.dir = dir;
      this.file = file;
    }
  }


}
