/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.ncml;

import ucar.nc2.util.CancelTask;
import ucar.nc2.units.TimeUnit;

import java.util.*;
import java.io.IOException;

import thredds.crawlabledataset.CrawlableDataset;

/**
 * Manage a list of Scanners that find Files (actually finds CrawlableDataset).
 * Tracks when they need to be rescanned.
 *
 * @author caron
 * @since Aug 10, 2007
 */
public class DatasetCollectionManager {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetCollectionManager.class);
  static private boolean debugSync = false, debugSyncDetail = false;

  private List<Scanner> scanList = new ArrayList<Scanner>();
  private Map<String, CrawlableDataset> map; // current map of CrawlableDataset

  private TimeUnit recheck; // how often to recheck
  private long lastScanned; // last time scanned

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
   * Scan the directory(ies) and create CrawlableDataset objects.
   * Get the results from getFiles()
   *
   * @param cancelTask allow user to cancel
   * @throws java.io.IOException if io error
   */
  public void scan(CancelTask cancelTask) throws IOException {
    Map<String, CrawlableDataset> newMap = new HashMap<String, CrawlableDataset>();
    scan(newMap, cancelTask);
    map = newMap;
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
   * If the CrawlableDataset already exists in the current list, leave it in the list.
   * If returns true, get the results from getFiles(), otherwise nothing has changed.
   *
   * @return true if anything actually changed.
   * @throws IOException on I/O error
   */
  public boolean rescan() throws IOException {
    if (debugSync) System.out.println(" *Sync at " + new Date());
    lastScanned = System.currentTimeMillis();

    // rescan
    Map<String, CrawlableDataset> newMap = new HashMap<String, CrawlableDataset>();
    scan(newMap, null);

    // replace with previous datasets if they exist
    boolean changed = false;
    for (CrawlableDataset newDataset : newMap.values()) {
      String path = newDataset.getPath();
      CrawlableDataset oldDataset = map.get( path);
      if (oldDataset != null) {
        newMap.put(path, oldDataset);
        if (debugSyncDetail) System.out.println("  sync using old Dataset= " + path);
      } else {
        changed = true;
        if (debugSyncDetail) System.out.println("  sync found new Dataset= " + path);
      }
    }

    if (!changed) { // check for deletions
      for (CrawlableDataset oldDataset : map.values()) {
        String path = oldDataset.getPath();
        CrawlableDataset newDataset = newMap.get( path);
        if (newDataset == null) {
          changed = true;
          if (debugSyncDetail) System.out.println("  sync found deleted Dataset= " + path);
          break;
        }
      }
    }

    if (changed)
      map = newMap;

    return changed;
  }

  /**
   * Get how often to rescan
   *
   * @return time dureation of rescan period, or null if none.
   */
  public TimeUnit getRecheck() {
    return recheck;
  }

  /**
   * Get the last time scanned
   *
   * @return msecs since 1970
   */
  public long getLastScanned() {
    return lastScanned;
  }

  /**
   * Get the current collection of CrawlableDataset, since last scan or rescan.
   *
   * @return current list of CrawlableDataset
   */
  public Collection<CrawlableDataset> getFiles() {
    return map.values();
  }

  private void scan(java.util.Map<String, CrawlableDataset> map, CancelTask cancelTask) throws IOException {
    // run through all scanners and collect CrawlableDataset instances
    for (Scanner scanner : scanList) {
      scanner.scanDirectory(map, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

}
