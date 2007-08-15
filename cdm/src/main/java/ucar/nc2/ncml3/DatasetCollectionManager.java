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
package ucar.nc2.ncml3;

import ucar.nc2.util.CancelTask;
import ucar.nc2.units.TimeUnit;

import java.util.*;
import java.io.IOException;

/**
 * Manage a list of Scanners that find files (actually CrawlableDataset).
 * Tracks when they need to be rescanned.
 *
 * @author caron
 * @since Aug 10, 2007
 */
public class DatasetCollectionManager {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetCollectionManager.class);

  private List<Scanner> scanList = new ArrayList<Scanner>(); // current set of DirectoryScan for scan elements
  private List<MyCrawlableDataset> files;

  private TimeUnit recheck; // how often to recheck
  private long lastScanned; // last time scanned

  private boolean debugSync = false, debugSyncDetail = false;

  public DatasetCollectionManager(String recheckS) {
    if (recheckS != null) {
      try {
        this.recheck = new TimeUnit(recheckS);
      } catch (Exception e) {
        logger.error("Invalid time unit for recheckEvery = {}", recheckS);
      }
    }
  }

  public void addDirectoryScan(Scanner scan) {
    scanList.add(scan);
  }

  /**
   * Scan the directory(ies) and create MyCrawlableDataset objects.
   * Get the results from getFiles()
   * @param cancelTask allow user to cancel
   * @throws java.io.IOException if io error
   */
  public void scan( CancelTask cancelTask) throws IOException {
    files = new ArrayList<MyCrawlableDataset>();
    scan( files, cancelTask);
    this.lastScanned = System.currentTimeMillis();
  }

  /**
   * Compute if rescan is needed.
   * True if scanList not empty, recheckEvery not null, and recheckEvery time has passed since last scanned.
   *
   * @return true is rescan time has passed
   */
  public boolean timeToRescan() {
    if (scanList.isEmpty()) {
      if (debugSyncDetail) System.out.println(" *Sync not needed, no scanners");
      return false;
    }

    // see if we need to recheck
    if (recheck == null) {
      if (debugSyncDetail) System.out.println(" *Sync not needed, recheck is null");
      return false;
    }

    Date now = new Date();
    Date lastCheckedDate = new Date(lastScanned);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      if (debugSync) System.out.println(" *Sync not needed, last= " + lastCheckedDate + " now = " + now);
      return false;
    }

    return true;
  }

  /**
   * Rescan directories. Files may be deleted or added.
   * If the MyCrawlableDataset already exists in the current list, leave it in the list.
   * If returns true, get the results from getFiles(), otherwise nothing has changed.
   *
   * @return true if anything actually changed.
   * @throws IOException on I/O error
   */
  public boolean rescan() throws IOException {
    if (debugSync) System.out.println(" *Sync at " + new Date());
    lastScanned = System.currentTimeMillis();

    // rescan
    List<MyCrawlableDataset> newFiles = new ArrayList<MyCrawlableDataset>();
    scan(newFiles, null);

    // replace with previous datasets if they exist
    boolean changed = false;
    for (int i = 0; i < newFiles.size(); i++) {
      MyCrawlableDataset newDataset = newFiles.get(i);
      int index = files.indexOf(newDataset); // equal if crawlableDataset.path is equal
      if (index >= 0) {
        newFiles.set(i, files.get(index));
        if (debugSyncDetail) System.out.println("  sync using old Dataset= " + newDataset.file.getPath());
      } else {
        changed = true;
        if (debugSyncDetail) System.out.println("  sync found new Dataset= " + newDataset.file.getPath());
      }
    }

    if (!changed) { // check for deletions
      for (MyCrawlableDataset oldDataset : files) {
        if (newFiles.indexOf(oldDataset) < 0) {
          changed = true;
          if (debugSyncDetail) System.out.println("  sync found deleted Dataset= " + oldDataset.file.getPath());
        }
      }
    }

    if (changed)
      files = newFiles;

    return changed;
  }

  /**
   * Get how often to rescan
   * @return time dureation of rescan period, or null if none.
   */
  public TimeUnit getRecheck() {
    return recheck;
  }

  /**
   * Get the last time scanned
   * @return msecs since 1970
   */
  public long getLastScanned() {
    return lastScanned;
  }

  /**
   * Get the current list of MyCrawlableDataset, since last scan or rescan.
   * @return current list of MyCrawlableDataset
   */
  public List<MyCrawlableDataset> getFiles() {
    return files;
  }

  private void scan( List<MyCrawlableDataset> result, CancelTask cancelTask) throws IOException {

    // run through all scanners and collect MyCrawlableDataset instances
    for (Scanner scanner : scanList) {
      scanner.scanDirectory(result, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

}
