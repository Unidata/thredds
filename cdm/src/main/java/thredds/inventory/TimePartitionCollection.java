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

package thredds.inventory;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.filter.WildcardMatchOnName;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Collections partitioned by time range, resulting in a collection of CollectionManager objects.
 * Kludge because these are not MFiles, as the CollectionManager contract claims.
 * However, we get to use the TriggerEvent mechanism.
 *
 * @author caron
 * @since 4/16/11
 */
public class TimePartitionCollection extends MFileCollectionManager {
  static private enum Type {setfromExistingIndices, directory, timePeriod}

  static public TimePartitionCollection factory(FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) {
    if (config.timePartition == null)
      throw new IllegalArgumentException("Must specify time partition spec = "+ config.spec);

    return new TimePartitionCollection(config, errlog, logger);
  }

  static public TimePartitionCollection fromExistingIndices(FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) {
    if (config.timePartition == null)
      throw new IllegalArgumentException("Must specify time partition spec = "+ config.spec);

    return new TimePartitionCollection(config, errlog, logger);
  }

  //////////////////////////////

  //private int npartitions;
  //private boolean setfromExistingIndices;
  private Type type;

  private TimePartitionCollection(FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) {
    super(config, errlog, logger);
    //this.setfromExistingIndices = setfromExistingIndices;
    if (dateExtractor == null)
      throw new IllegalArgumentException("Time partition must specify a date extractor");
  }

  // a scan has already been done, and something changed.
  public List<CollectionManager> makePartitions() throws IOException {
    List<CollectionManager> result;
    /* if (setfromExistingIndices)
      result = makePartitionsFromIndices();
    else */
    if (config.timePartition.equalsIgnoreCase("directory"))
      result = makePartitionsFromSubdirs();
    else
      result = makePartitionsByPeriod();
    //npartitions = result.size();
    return result;
  }

  /* private List<CollectionManager> makePartitionsFromIndices() {
    this.type = Type.setfromExistingIndices;
    MController controller = MFileCollectionManager.getController();
    MCollection mc = new MCollection(sp.getRootDir(), sp.getRootDir(), false, (MFileFilter) null, null);

    File root = new File(sp.getRootDir());
    if (!root.exists()) {
      logger.error("TimePartitionCollections root = {} does not exist ", sp.getRootDir());
      return null;
    }

    Iterator<MFile> iter = controller.getSubdirs(mc, true);
    if (iter == null) {
      logger.error("TimePartitionCollections Invalid collection, no subdirectories found; root = {}, collection= {} ", sp.getRootDir(), mc);
      return null;
    }

    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != sp.getFilter())
      filters.add(new WildcardMatchOnName(sp.getFilter()));


    List<CollectionManager> result = new ArrayList<CollectionManager>();
    while (iter.hasNext()) {
      MFile mfile = iter.next();
      MCollection mcs = new MCollection(mfile.getName(), mfile.getPath(), sp.wantSubdirs(), filters, null);

      CalendarDate cdate = dateExtractor.getCalendarDate(mfile);
      if (cdate == null)  {
        logger.error("TimePartitionCollections dateExtractor = {} not working on mfile = {} ", dateExtractor, mfile.getPath());
        return null;
      }

      // make sure theres an index file, otherwise skip it
      boolean haveIndex = false;
      File dir = new File(mfile.getPath());
      for (File f : dir.listFiles()) {
        if (f.getPath().endsWith(".ncx")) {
          haveIndex = true;
          break;
        }
      }
      if (!haveIndex) {
        System.out.printf("makePartitionsFromIndices skip %s%n",mfile.getPath());
        continue;
      }
      System.out.printf("makePartitionsFromIndices add %s%n",mfile.getPath());

      MFileCollectionManager dcm = new MFileCollectionManager(mfile.getName(), mcs, cdate);
      dcm.setDateExtractor(dateExtractor);
      result.add(dcm);
      //System.out.printf("%s%n", dcm);
    }

    // sort by partition starting time
    Collections.sort( result, new Comparator<CollectionManager>() {
      public int compare(CollectionManager o1, CollectionManager o2) {
        return o1.getStartCollection().compareTo(o2.getStartCollection());
      }
    });

    return result;
  } */

  // LOOK : not working - something about date Extractor - must work on directory name ??
  private List<CollectionManager> makePartitionsFromSubdirs() throws IOException {
    this.type = Type.directory;
    MController controller = MFileCollectionManager.getController(); // make sure loaded

    Formatter errlog = new Formatter();
    CollectionSpecParser sp = new CollectionSpecParser(config.spec, errlog);
    MCollection mc = new MCollection(sp.getRootDir(), sp.getRootDir(), false, (MFileFilter) null, null);

    File root = new File(sp.getRootDir());
    if (!root.exists()) {
      logger.error("TimePartitionCollections root = {} does not exist ", sp.getRootDir());
      return null;
    }

    Iterator<MFile> iter = controller.getSubdirs(mc, true);
    if (iter == null) {
      logger.error("TimePartitionCollections Invalid collection, no subdirectories found; root = {}, collection= {} ", sp.getRootDir(), mc);
      return null;
    }

    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != sp.getFilter())
      filters.add(new WildcardMatchOnName(sp.getFilter()));

    List<CollectionManager> result = new ArrayList<CollectionManager>();
    while (iter.hasNext()) {
      MFile mfile = iter.next();
      MCollection mcs = new MCollection(mfile.getName(), mfile.getPath(), sp.wantSubdirs(), filters, null);

      CalendarDate cdate = dateExtractor.getCalendarDate(mfile);
      if (cdate == null)  {
        logger.error("TimePartitionCollections dateExtractor = {} not working on mfile = {} ", dateExtractor, mfile.getPath());
        return null;
      }

      // String name = collectionName+"-"+mfile.getName();
      String name = mfile.getName();
      MFileCollectionManager dcm = new MFileCollectionManager(name, mcs, cdate, this.logger);
      dcm.setDateExtractor(dateExtractor);
      if (config != null && config.gribConfig != null)
        dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);
      dcm.scan(false);
      result.add(dcm);
      //System.out.printf("%s%n", dcm);
    }

    // sort by partition starting time
    Collections.sort( result, new Comparator<CollectionManager>() {
      public int compare(CollectionManager o1, CollectionManager o2) {
        return o1.getStartCollection().compareTo(o2.getStartCollection());
      }
    });

    return result;
  }

  /* private List<CollectionManager> makePartitionsFromSubdirs() throws IOException {
    this.type = Type.directory;

    List<CollectionManager> result = new ArrayList<CollectionManager>();
    Map<String, TimePartitionCollectionManager> map = new HashMap<String, TimePartitionCollectionManager>(100);

    TimePartitionCollectionManager curr = null;
    for (MFile mfile : getFiles()) {
      CalendarDate cdate = dateExtractor.getCalendarDate(mfile);
      if (cdate == null)
        logger.error("Date extraction failed on file= {} dateExtractor = {}", mfile.getPath(), dateExtractor);
      DatedMFile dmf = new DatedMFile(mfile, cdate);

      // directory name
      String[] paths = mfile.getPath().split("/");
      String directory =
      MFile parent2 = map.get(parent);
      if (parent2 == null) {
        curr = new TimePartitionCollectionManager(parent.getName(), dmf, getRoot(), this.auxInfo);
        result.add(curr);
      }
      curr.add(dmf);
    }

    // sort by partition starting time
    Collections.sort( result, new Comparator<CollectionManager>() {
      public int compare(CollectionManager o1, CollectionManager o2) {
        return o1.getStartCollection().compareTo(o2.getStartCollection());
      }
    });

    return result;
  } */

  private List<CollectionManager> makePartitionsByPeriod() throws IOException {
    this.type = Type.timePeriod;

    List<DatedMFile> files = new ArrayList<DatedMFile>();
    for (MFile mfile : getFiles()) {
      CalendarDate cdate = dateExtractor.getCalendarDate(mfile);
      if (cdate == null)
        logger.error("Date extraction failed on file= {} dateExtractor = {}", mfile.getPath(), dateExtractor);
      files.add( new DatedMFile(mfile, cdate));
    }
    Collections.sort(files);

    CalendarDateFormatter cdf = new CalendarDateFormatter("yyyyMMdd");

    List<CollectionManager> result = new ArrayList<CollectionManager>();
    TimePartitionCollectionManager curr = null;
    for (DatedMFile dmf : files) {
      if ((curr == null) || (!curr.endPartition.isAfter(dmf.cdate))) {
        CalendarPeriod period = CalendarPeriod.of(config.timePartition);
        CalendarDate start = dmf.cdate.truncate(period.getField()); // start on a boundary
        CalendarDate end = start.add( period);
        String name = collectionName + "-"+ cdf.toString(dmf.cdate);

        curr = new TimePartitionCollectionManager(name, start, end, getRoot(), this.auxInfo, this.logger);

        result.add(curr);
      }
      curr.add(dmf);
    }

    return result;
  }

  @Override
  public CalendarDate getStartCollection() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
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
  private class DatedMFile implements Comparable<DatedMFile> {
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
  private class TimePartitionCollectionManager extends CollectionManagerAbstract {
    final String root;
    final CalendarDate startPartition, endPartition;
    List<MFile> files;

    TimePartitionCollectionManager(String name, CalendarDate start, CalendarDate end, String root, Map<String, Object> auxInfo, org.slf4j.Logger logger) {
      super(name, logger);

      this.startPartition = start;
      this.endPartition = end;
      this.files = new ArrayList<MFile>();
      this.root = root;
      this.auxInfo = auxInfo;
    }

    void add(DatedMFile dmfile) {
      files.add(dmfile.mfile);
    }

    @Override
    public boolean isScanNeeded() {
      return false;
    }

    @Override
    public boolean scanIfNeeded() {
      return false;
    }

    @Override
    public boolean hasDateExtractor() {
      return true;
    }

    @Override
    public CalendarDate getStartCollection() {
      return startPartition;
    }

    @Override
    public boolean scan(boolean sendEvent) throws IOException {
      return false;
    }

    @Override
    public long getLastScanned() {
      return -1;
    }

    @Override
    public long getLastChanged() {
      return -1;
    }

    @Override
    public String getRoot() {
      return root;
    }

    @Override
    public Iterable<MFile> getFiles() {
      return files;
    }

    @Override
    public void setFiles(Iterable<MFile> files) {
      List<MFile> result = new ArrayList<MFile>();
      for (MFile f : files) result.add(f);
      this.files = result;
    }

    @Override
    public CalendarDate extractRunDate(MFile mfile) {
      return dateExtractor.getCalendarDate(mfile);
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

  ///////////////////////////////////////////////

  /* testing only
  private TimePartitionCollection(String spec, String timePartition, Formatter errlog) {
    this.sp = new CollectionSpecParser(spec, timePartition, errlog);
    if (sp.getDateFormatMark() == null)
      throw new IllegalArgumentException("Time partition must specify a date extractor spec = "+ timePartition);

    this.dateExtractor = new DateExtractorFromName(sp.getDateFormatMark(), sp.useName());
  } */

   private static void doit(FeatureCollectionConfig config) throws IOException {
    TimePartitionCollection tpc = TimePartitionCollection.factory(config, new Formatter(System.out), null);
    System.out.printf("tpc = %s%n", tpc);
     if (tpc.makePartitions() == null) {
       System.out.printf("*** No partitions%n");
       return;
     }
    List<CollectionManager> m = tpc.makePartitions();
    for (CollectionManager dcm : m) {
      System.out.printf(" dcm = %s timePartition=%s %n", dcm, dcm.getStartCollection());
      /* dcm.scan(null);
      for (MFile mfile : dcm.getFiles()) {
        System.out.printf("  %s == %s%n", mfile.getPath(), dcm.extractRunDate(mfile));
      } */

    }
    System.out.printf("-----------------------------------%n");
  }

  public static void main(String[] args) throws IOException {

    FeatureCollectionConfig config = new FeatureCollectionConfig();
    config.spec = "G:/nomads/cfsr/timeseries/**/.*grb2$";
    config.dateFormatMark = "#timeseries/#yyyyMM";
    config.timePartition = "true";
    //doit(config);

    config = new FeatureCollectionConfig();
    config.spec = "G:/mlode/gefs/.*grib2$";
    config.dateFormatMark = "#GEFS_Global_1p0deg_Ensemble_#yyyyMMdd_HHmm";
    config.timePartition = "day";
    // doit(config);

    config = new FeatureCollectionConfig();
    config.spec = "g:/mlode/SREF_CONUS_40km/.*grib2$";
    config.dateFormatMark = "yyyyMMdd_HHmm#.grib2#";
    config.timePartition = "day";
    //doit(config);

    config = new FeatureCollectionConfig();
    config.spec = "g:/mlode/radar/**/.*grib2$";
    config.dateFormatMark = "#radar/#yyyyMMdd";
    config.timePartition = "directory";
    doit(config);
  }
}
