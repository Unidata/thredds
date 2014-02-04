/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import thredds.inventory.MCollection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Collections partitioned by time range, resulting in a collection of CollectionManager objects.
 * Kludge because these are not MFiles, as the CollectionManager contract claims.
 * However, we get to use the TriggerEvent mechanism.
 *
 * @author caron
 * @since 4/16/11
 */
public class TimePartitionCollectionManager extends MFileCollectionManager implements PartitionManager {
  static protected enum Type {setfromExistingIndices, directory, timePeriod}

  static public PartitionManager factory(FeatureCollectionConfig config, String topDir, IndexReader indexReader, Formatter errlog, org.slf4j.Logger logger) {
    if (config.timePartition == null)
      throw new IllegalArgumentException("Must specify time partition in config = " + config.name);

    if (config.timePartition.equalsIgnoreCase("directory")) {
      Path topPath = Paths.get(topDir);
      return new DirectoryPartition(config, topPath, indexReader, logger);

    } else {
      return new thredds.inventory.partition.TimePartitionCollectionManager(config, errlog, logger);
    }
  }

  //////////////////////////////

  protected Type type;

  protected TimePartitionCollectionManager(FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) {
    super(config, errlog, logger);
    if (dateExtractor == null)
      throw new IllegalArgumentException("Time partition must specify a date extractor");
    this.type = Type.timePeriod;
  }


  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {
    if (forceCollection == null) forceCollection = CollectionUpdateType.test;

    List<DatedMFile> files = new ArrayList<DatedMFile>();
    for (MFile mfile : getFilesSorted()) {
      CalendarDate cdate = dateExtractor.getCalendarDate(mfile);
      if (cdate == null)
        logger.error("Date extraction failed on file= {} dateExtractor = {}", mfile.getPath(), dateExtractor);
      files.add( new DatedMFile(mfile, cdate));
    }
    CalendarDateFormatter cdf = new CalendarDateFormatter("yyyyMMdd");

    List<MCollection> result = new ArrayList<>();
    TimePartitionCollection curr = null;
    for (DatedMFile dmf : files) {
      if ((curr == null) || (!curr.endPartition.isAfter(dmf.cdate))) {
        CalendarPeriod period = CalendarPeriod.of(config.timePartition);
        CalendarDate start = dmf.cdate.truncate(period.getField()); // start on a boundary
        CalendarDate end = start.add( period);
        String name = collectionName + "-"+ cdf.toString(dmf.cdate);   // LOOK
        if (startCollection == null) startCollection = start; // grab the first one

        curr = new TimePartitionCollection(name, start, end, getRoot());
        result.add(curr);
      }
      curr.add(dmf);
    }

    return result;
  }

  @Override
  public String toString() {
    return "TimePartitionCollection{" +
            "name='" + collectionName + '\'' +
            ", dateExtractor=" + dateExtractor +
            ", type=" + type +
            '}';
  }

  // wrapper around an MFile
  protected class DatedMFile implements Comparable<DatedMFile> {
    MFile mfile;
    CalendarDate cdate;

    private DatedMFile(MFile mfile, CalendarDate cdate) {
      this.mfile = mfile;
      this.cdate = cdate;
    }

    @Override
    public int compareTo(DatedMFile o) {
      return cdate.compareTo(o.cdate);
    }
  }

  // a partition of a collection, based on time intervals
  private class TimePartitionCollection extends CollectionAbstract {
    //final String root;
    final CalendarDate startPartition, endPartition;

    List<MFile> files;

    TimePartitionCollection(String name, CalendarDate startPartition, CalendarDate endPartition, String root) {
      super(name, TimePartitionCollectionManager.this.logger);
      this.startPartition = startPartition;
      this.endPartition = endPartition;
      this.root = root;
      this.files = new ArrayList<>();
      this.setDateExtractor(TimePartitionCollectionManager.this.dateExtractor);
    }

    void add(DatedMFile dmfile) {
      files.add(dmfile.mfile);
    }

    @Override
    public CalendarDate getStartCollection() {
      return startPartition;
    }

    @Override
    public MFile getLatestFile() {
      int last = files.size()-1;
      return last < 0 ? null : files.get(last);
    }

    @Override
    public void close() {
    // NOOP
    }

    @Override
    public boolean isPartition() {
      return true;
    }

    @Override
    public Iterable<MFile> getFilesSorted() {
      return files;
    }

    @Override
    public CloseableIterator<MFile> getFileIterator() throws IOException {
      return new MFileIterator(files.iterator(), filter);
    }

    @Override
    public String toString() {
      return "TimePartitionCollectionManager{" +
              "name='" + collectionName + '\'' +
              ", startPartition=" + startPartition +
              ", endPartition=" + endPartition +
              '}';
    }
  }
}