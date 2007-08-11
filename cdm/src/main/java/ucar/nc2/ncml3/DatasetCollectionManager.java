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
import ucar.nc2.ncml.AggregationIF;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.TimeUnit;

import java.util.*;
import java.io.IOException;

import thredds.util.DateFromString;

/**
 * @author caron
 * @since Aug 10, 2007
 */
public class DatasetCollectionManager {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetCollectionManager.class);

  private AggregationIF.Type type;

  protected List<Scanner> scanList = new ArrayList<Scanner>(); // current set of DirectoryScan for scan elements
  protected List<Aggregation.Dataset> explicitDatasets = new ArrayList<Aggregation.Dataset>(); // explicitly created Dataset objects from netcdf elements
  protected List<Aggregation.Dataset> datasets = new ArrayList<Aggregation.Dataset>(); // explicitly and scanned

  protected TimeUnit recheck; // how often to recheck
  protected long lastChecked; // last time checked
  protected boolean isDate = false;  // has a dateFormatMark, so agg coordinate variable is a Date

  private boolean debugSync = false, debugSyncDetail = false;

  public DatasetCollectionManager(AggregationIF.Type type, String recheckS) {
    this.type = type;

    if (recheckS != null) {
      try {
        this.recheck = new TimeUnit(recheckS);
      } catch (Exception e) {
        logger.error("Invalid time unit for recheckEvery = {}", recheckS);
      }
    }
  }

  public void addDirectoryScan(Scanner scan) { scanList.add( scan); }

  public void addExplicitDataset( Aggregation.Dataset d) { explicitDatasets.add( d); }

  public void scan(Aggregation agg, CancelTask cancelTask) throws IOException {
    datasets = new ArrayList< Aggregation.Dataset>();

    for (Aggregation.Dataset dataset : explicitDatasets) {
      if (dataset.checkOK(cancelTask))
        datasets.add(dataset);
    }

    scan(agg, datasets, cancelTask);

    this.lastChecked = System.currentTimeMillis();
  }

  /**
   * Rescan if recheckEvery time has passed
   * @return if theres new datasets, put new datasets into nestedDatasets
   */
  public boolean timeToRescan() {
    if (type == Aggregation.Type.UNION) {
      if (debugSyncDetail) System.out.println(" *Sync not needed for Union");
      return false;
    }

    // see if we need to recheck
    if (recheck == null) {
      if (debugSyncDetail) System.out.println(" *Sync not needed, recheck is null");
      return false;
    }

    Date now = new Date();
    Date lastCheckedDate = new Date(lastChecked);
    Date need = recheck.add( lastCheckedDate);
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
    List<Aggregation.Dataset> newDatasets = new ArrayList<Aggregation.Dataset>();
    scan(agg, newDatasets, null);

    // replace with previous datasets if they exist
    boolean changed = false;
    for (int i = 0; i < newDatasets.size(); i++) {
      Aggregation.Dataset newDataset = newDatasets.get(i);
      int index = datasets.indexOf(newDataset); // equal if location is equal
      if (index >= 0) {
        newDatasets.set(i, datasets.get(index));
        if (debugSyncDetail) System.out.println("  sync using old Dataset= " + newDataset.getLocation());
      } else {
        changed = true;
        if (debugSyncDetail) System.out.println("  sync found new Dataset= " + newDataset.getLocation());
      }
    }

    if (!changed) { // check for deletions
      for (Aggregation.Dataset oldDataset : datasets) {
        if ((newDatasets.indexOf(oldDataset) < 0) && (explicitDatasets.indexOf(oldDataset) < 0)) {
          changed = true;
          if (debugSyncDetail) System.out.println("  sync found deleted Dataset= " + oldDataset.getLocation());
        }
      }
    }

    if (!changed) return false;

    // recreate the list of datasets
    datasets = new ArrayList<Aggregation.Dataset>();
    datasets.addAll(explicitDatasets);
    datasets.addAll(newDatasets);

    return true;
  }

  public TimeUnit getRecheck() { return recheck; }
  public long getLastChecked()  { return lastChecked; }

  public List<Aggregation.Dataset> getDatasets() { return datasets; }


   /**
   * Scan the directory(ies) and create nested Aggregation.Dataset objects.
   * Directories are scanned recursively, by calling File.listFiles().
   * Sort by date if it exists, else filename.
   *
   * @param result     add to this List objects of type Aggregation.Dataset
   * @param cancelTask allow user to cancel
   * @throws java.io.IOException if io error
   */
  private void scan( Aggregation agg, List<Aggregation.Dataset> result, CancelTask cancelTask) throws IOException {

    // run through all scanners and collect MyFile instances
    List<MyFile> fileList = new ArrayList<MyFile>();
    for (Scanner scanner : scanList) {
      scanner.scanDirectory(fileList, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }

    /* extract date if possible, before sorting
    for (MyFile myf : fileList) {
      // optionally parse for date
      if (null != myf.dir.dateFormatMark) {
        String filename = myf.file.getName();
        myf.dateCoord = DateFromString.getDateUsingDemarkatedCount(filename, myf.dir.dateFormatMark, '#');
        myf.dateCoordS = formatter.toDateTimeStringISO(myf.dateCoord);
        if (debugScan) System.out.println("  adding " + myf.file.getAbsolutePath() + " date= " + myf.dateCoordS);
      } else {
        if (debugScan) System.out.println("  adding " + myf.file.getAbsolutePath());
      }
    } */

    // Sort by date if it exists, else filename.
    Collections.sort(fileList, new Comparator<MyFile>() {
      public int compare(MyFile mf1, MyFile mf2) {
        if (mf1.dateCoord != null) // LOOK
          return mf1.dateCoord.compareTo(mf2.dateCoord);
        else
          return mf1.file.getName().compareTo(mf2.file.getName());
      }
    });

    // now add the ordered list of Datasets to the result List
    for (MyFile myf : fileList) {
      String location = myf.file.getPath();
      String coordValue = (type == AggregationIF.Type.JOIN_NEW) || (type == AggregationIF.Type.JOIN_EXISTING_ONE) || (type == AggregationIF.Type.FORECAST_MODEL_COLLECTION) ? myf.dateCoordS : null;
      Aggregation.Dataset ds = agg.makeDataset(location, location, null, coordValue, myf.dir.isEnhance(), null);
      ds.coordValueDate = myf.dateCoord;
      result.add(ds);

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

  public void close() throws IOException {
    for (Aggregation.Dataset ds : datasets) {
      ds.close();
    }
  }
}
