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
 * Manages a list of Scanners that find files (actually CrawlableDataset).
 * Wraps these in Aggregation.Dataset objects.
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
  private long lastChecked; // last time checked

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

  public void scan(Aggregation agg, CancelTask cancelTask) throws IOException {
    files = new ArrayList<MyCrawlableDataset>();
    scan( files, cancelTask);
    this.lastChecked = System.currentTimeMillis();
  }

  /**
   * Rescan if recheckEvery time has passed
   *
   * @return if theres new datasets, put new datasets into nestedDatasets
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
    Date lastCheckedDate = new Date(lastChecked);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      if (debugSync) System.out.println(" *Sync not needed, last= " + lastCheckedDate + " now = " + now);
      return false;
    }

    return true;
  }

  // protected by synch
  public boolean rescan(Aggregation agg) throws IOException {
    // ok were gonna recheck
    lastChecked = System.currentTimeMillis();
    if (debugSync) System.out.println(" *Sync at " + new Date());

    // rescan
    List<MyCrawlableDataset> newDatasets = new ArrayList<MyCrawlableDataset>();
    scan(newDatasets, null);

    // replace with previous datasets if they exist
    boolean changed = false;
    for (int i = 0; i < newDatasets.size(); i++) {
      MyCrawlableDataset newDataset = newDatasets.get(i);
      int index = files.indexOf(newDataset); // equal if location is equal
      if (index >= 0) {
        newDatasets.set(i, files.get(index));
        if (debugSyncDetail) System.out.println("  sync using old Dataset= " + newDataset.file.getPath());
      } else {
        changed = true;
        if (debugSyncDetail) System.out.println("  sync found new Dataset= " + newDataset.file.getPath());
      }
    }

    if (!changed) { // check for deletions
      for (MyCrawlableDataset oldDataset : files) {
        if (newDatasets.indexOf(oldDataset) < 0) {
          changed = true;
          if (debugSyncDetail) System.out.println("  sync found deleted Dataset= " + oldDataset.file.getPath());
        }
      }
    }

    return changed;
  }

  public TimeUnit getRecheck() {
    return recheck;
  }

  public long getLastChecked() {
    return lastChecked;
  }

  /* public List<Aggregation.Dataset> getDatasets() {
    return datasets;
  } */

  public List<MyCrawlableDataset> getFiles() {
    return files;
  }


  /**
   * Scan the directory(ies) and create MyCrawlableDataset objects.
   * Directories are scanned recursively, by calling File.listFiles().
   * Sort by date if it exists, else filename.
   *
   * @param result place results here
   * @param cancelTask allow user to cancel
   * @throws java.io.IOException if io error
   */
  private void scan( List<MyCrawlableDataset> result, CancelTask cancelTask) throws IOException {

    // run through all scanners and collect MyCrawlableDataset instances
    for (Scanner scanner : scanList) {
      scanner.scanDirectory(result, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

}
