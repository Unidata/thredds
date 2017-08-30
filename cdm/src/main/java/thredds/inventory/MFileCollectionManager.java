/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package thredds.inventory;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.filesystem.MFileOS;
import thredds.inventory.filter.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.TimeDuration;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manage Collections of MFiles.
 * Used in:
 * <ul>
 * <li> thredds.inventory
 * <li> ucar.nc2.ft.fmrc.Fmrc
 * <li> ucar.nc2.ncml.Aggregation
 * </ul>
 * <p/>
 * we need to be thread safe, for InvDatasetFeatureCollection
 * @author caron
 * @since Jul 8, 2009
 */
@ThreadSafe
public class MFileCollectionManager extends CollectionManagerAbstract {
  static private MController controller;

  /**
   * Set the MController used by scan. Defaults to thredds.filesystem.ControllerOS() if not set.
   *
   * @param _controller use this MController
   */
  static public void setController(MController _controller) {
    controller = _controller;
  }

  static public MController getController() {
    if (null == controller) controller = new thredds.filesystem.ControllerOS();  // default
    return controller;
  }

  // called from Aggregation, Fmrc, FeatureDatasetFactoryManager
  static public MFileCollectionManager open(String collectionName, String collectionSpec, String olderThan, Formatter errlog) throws IOException {
    return new MFileCollectionManager(collectionName, collectionSpec, olderThan, errlog);
  }

  // retrofit to Aggregation
  static public MFileCollectionManager openWithRecheck(String collectionName, String recheckS) {
    return new MFileCollectionManager(collectionName, recheckS);
  }

  ////////////////////////////////////////////////////////////////////

  // these are final
  private final List<CollectionConfig> scanList = new ArrayList<>(); // an MCollection is a collection of managed files
  private final long olderThanInMsecs;  // LOOK why not use LastModifiedLimit filter ?
  //protected String rootDir;
  protected FeatureCollectionConfig config;

  @GuardedBy("this")
  private Map<String, MFile> map; // current map of MFile in the collection. this can change = keep under lock

  @GuardedBy("this")
  private long lastScanned;       // last time scanned
  @GuardedBy("this")
  private AtomicLong lastChanged = new AtomicLong(); // last time the set of files changed

  @GuardedBy("this")
  private Map<String, String> filesRunDateMap = new HashMap<>();

  private MFileCollectionManager(String collectionName, String collectionSpec, String olderThan, Formatter errlog) {
    super(collectionName, null);
    CollectionSpecParser sp = new CollectionSpecParser(collectionSpec, errlog);
    this.recheck = null;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default
    this.root = sp.getRootDir();

    CompositeMFileFilter filters = new CompositeMFileFilter();
    if (null != sp.getFilter())
      filters.addIncludeFilter(new WildcardMatchOnName(sp.getFilter()));
    olderThanInMsecs = parseOlderThanFilter(olderThan);

    dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new CollectionConfig(sp.getRootDir(), sp.getRootDir(), sp.wantSubdirs(), filters, null));
  }

  // this is the full featured constructor, using FeatureCollectionConfig for config.
  public MFileCollectionManager(FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) {
    super(config.collectionName != null ? config.collectionName : config.spec, logger);
    this.config = config;

    CollectionSpecParser sp = config.getCollectionSpecParser(errlog);
    this.root = sp.getRootDir();

    CompositeMFileFilter filters = new CompositeMFileFilter();
    if (null != sp.getFilter())
      filters.addIncludeFilter(new WildcardMatchOnName(sp.getFilter()));
    olderThanInMsecs = parseOlderThanFilter(config.olderThan);

    if (config.dateFormatMark != null)
      dateExtractor = new DateExtractorFromName(config.dateFormatMark, false);
    else if (sp.getDateFormatMark() != null)
      dateExtractor = new DateExtractorFromName(sp.getDateFormatMark(), true);
    else
      dateExtractor = new DateExtractorNone();

    scanList.add(new CollectionConfig(sp.getRootDir(), sp.getRootDir(), sp.wantSubdirs(), filters, null));

    if (config.protoConfig != null)
      protoChoice = config.protoConfig.choice;

    if (config.updateConfig != null) {
      this.recheck = makeRecheck(config.updateConfig.recheckAfter);

      // static means never rescan on checkState; let it be externally triggered.
      if ((config.updateConfig.recheckAfter == null) && (config.updateConfig.rescan == null) && (config.updateConfig.deleteAfter == null))
        setStatic(true);
    }

    if (this.auxInfo == null) this.auxInfo = new HashMap<>(10);
    this.auxInfo.put(FeatureCollectionConfig.AUX_CONFIG, config);
  }


  private long parseOlderThanFilter(String olderThan) {
    if (olderThan != null) {
      try {
        TimeDuration tu = new TimeDuration(olderThan);
        return (long) (1000 * tu.getValueInSeconds());
      } catch (Exception e) {
        logger.error(collectionName + ": Invalid time unit for olderThan = {}", olderThan);
      }
    }
    return -1;
  }

  private TimeDuration makeRecheck(String recheckS) {
    if (recheckS != null) {
      try {
        return new TimeDuration(recheckS);
      } catch (Exception e) {
        logger.error(collectionName + ": Invalid time unit for recheckEvery = {}", recheckS);
      }
    }
    return null;
  }

  // for subclasses
  protected MFileCollectionManager(String name, org.slf4j.Logger logger) {
    super(name, logger);
    this.recheck = null;
    this.olderThanInMsecs = -1;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default
  }

  ////////////////////////////////////////////////////////////////////////////////

  public MFileCollectionManager(String name, String spec, Formatter errlog, org.slf4j.Logger logger) {
    super(name, logger);
    CollectionSpecParser sp = new CollectionSpecParser(spec, errlog);
    this.root = sp.getRootDir();

    CompositeMFileFilter filters = new CompositeMFileFilter();
    if (null != sp.getFilter())
      filters.addIncludeFilter(new WildcardMatchOnName(sp.getFilter()));

    dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new CollectionConfig(sp.getRootDir(), sp.getRootDir(), sp.wantSubdirs(), filters, null));

    this.recheck = null;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default
    this.olderThanInMsecs = -1;
  }

  public MFileCollectionManager(String name, CollectionConfig mc, CalendarDate startPartition, org.slf4j.Logger logger) {
    super(name, logger);
    this.startCollection = startPartition;
    this.scanList.add(mc);

    this.root = mc.getDirectoryName();
    this.recheck = null;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default
    this.olderThanInMsecs = -1;
  }

  ////////////////////////////////////////////////////////////////////
  // Aggregation retrofit

  /**
   * For retrofitting to Aggregation
   * Must also call addDirectoryScan one or more times
   *
   * @param recheckS a undunit time unit, specifying how often to rscan
   */
  private MFileCollectionManager(String collectionName, String recheckS) {
    super(collectionName, null);
    this.recheck = makeRecheck(recheckS);
    this.olderThanInMsecs = -1;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate;
  }

  /**
   * Add a directory scan to the collection
   *
   * @param dirName             scan this directory
   * @param suffix              require this suffix (overriddden by regexp), may be null
   * @param regexpPatternString if present, use this reqular expression to filter files , may be null
   * @param subdirsS            if "true", descend into subdirectories, may be null
   * @param olderS              udunit time unit - files must be older than this amount of time (now - lastModified > olderTime), may be null
   *                            // * @param dateFormatString dateFormatMark string, may be null
   * @param auxInfo             attach this object to any MFile found by this scan
   */
  public void addDirectoryScan(String dirName, String suffix, String regexpPatternString, String subdirsS, String olderS, Object auxInfo) {
    CompositeMFileFilter filters = new CompositeMFileFilter();
    if (null != regexpPatternString)
      filters.addIncludeFilter(new RegExpMatchOnName(regexpPatternString));
    else if (suffix != null)
      filters.addIncludeFilter(new WildcardMatchOnPath("*" + suffix + "$"));

    if (olderS != null) {
      try {
        TimeDuration tu = new TimeDuration(olderS);
        filters.addAndFilter(new LastModifiedLimit((long) (1000 * tu.getValueInSeconds())));
      } catch (Exception e) {
        logger.error(collectionName + ": Invalid time unit for olderThan = {}", olderS);
      }
    }

    boolean wantSubdirs = true;
    if ((subdirsS != null) && subdirsS.equalsIgnoreCase("false"))
      wantSubdirs = false;

    CollectionConfig mc = new CollectionConfig(dirName, dirName, wantSubdirs, filters, auxInfo);

    // create name
    StringBuilder sb = new StringBuilder(dirName);
    if (wantSubdirs)
      sb.append("**/");
    if (null != regexpPatternString)
      sb.append(regexpPatternString);
    else if (suffix != null)
      sb.append(suffix);
    else
      sb.append("noFilter");

    collectionName = sb.toString();
    scanList.add(mc);
  }

  ////////////////////////////////////////////////////////////////////

  public long getOlderThanFilterInMSecs() {
    return olderThanInMsecs;
  }

  @Override
  public synchronized long getLastScanned() {
    return lastScanned;
  }

  @Override
  public synchronized long getLastChanged() {
    return lastChanged.get();
  }

  protected boolean hasScans() {
    return !scanList.isEmpty();
  }

  /**
   * Compute if synchronous scan is needed.
   * True if recheck is true and enough time has elapsed.
   * @return true if rescan is needed
   */
  @Override
  public boolean isScanNeeded() {
    // see if we need to recheck
    if (recheck == null) {
      logger.debug("{}: scan not needed, recheck null", collectionName);
      return false;
    }

    if (!hasScans()) {
      logger.debug("{}: scan not needed, no scanners", collectionName);
      return false;
    }

    synchronized (this) {
      if (map == null && !isStatic()) {
        logger.debug("{}: scan needed, never scanned", collectionName);
        return true;
      }
    }

    Date now = new Date();
    Date lastCheckedDate = new Date(getLastScanned());
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      logger.debug("{}: scan not needed, last scanned={}, now={}", collectionName, lastCheckedDate, now);
      return false;
    }

    return true;
  }

    ////////////////////////
  // experimental
  protected ChangeChecker changeChecker = null;

  public synchronized void setChangeChecker(ChangeChecker strat) {
    this.changeChecker = strat;
  }

  @Override
  public synchronized boolean scan(boolean sendEvent) throws IOException {
    if (map == null) {
      boolean changed = scanFirstTime();
      if (changed && sendEvent)
        sendEvent(new TriggerEvent(this, CollectionUpdateType.always));  // watch out for infinite loop
      return changed;
    }

    long olderThan = (olderThanInMsecs <= 0) ? -1 : System.currentTimeMillis() - olderThanInMsecs; // new files must be older than this.

    // rescan
    Map<String, MFile> oldMap = map;
    Map<String, MFile> newMap = new HashMap<>();

    if ((!hasScans())) {
      // if no directory scans, the map of files should not change
      // but we should still make a new map to see if the files
      // have been updated since the last recheck
      for (String file : oldMap.keySet()) {
        newMap.put(file, MFileOS.getExistingFile(file));
      }
    } else {
      // we have a directory scan, so scan it
      reallyScan(newMap);
    }

    // replace with previous datasets if they exist
    int nnew = 0;
    int nchange = 0;
    Iterator<MFile> iter = newMap.values().iterator(); // need iterator so we can remove()
    while (iter.hasNext()) {
      MFile newFile = iter.next();
      String path = newFile.getPath();
      MFile oldFile = oldMap.get(path);
      if (oldFile != null) {
        if (newFile.getLastModified() > oldFile.getLastModified()) { // the file has changed since last time
          nchange++;
          logger.debug("{}: scan found Dataset changed= {}", collectionName, path);

        } else if (changeChecker != null && changeChecker.hasntChangedSince(newFile, oldFile.getLastModified())) { // the ancilliary file hasnt changed
          nchange++;
          logger.debug("{}: scan changeChecker found Dataset changed= {}", collectionName, path);
        }
      } else { // oldFile doesnt exist
        if (olderThan > 0 && newFile.getLastModified() > olderThan) { // the file is too new
          iter.remove();
          logger.debug("{}: scan found new Dataset but its too recently modified = {}", collectionName, path);
        } else {
          nnew++;
          logger.debug("{}: scan found new Dataset= {} ", collectionName, path);
        }
      }
    }

    // check for deletions
    int ndelete = 0;
    for (MFile oldDataset : oldMap.values()) {
      String path = oldDataset.getPath();
      MFile newDataset = newMap.get(path);
      if (newDataset == null) {
        ndelete++;
        logger.debug("{}: scan found deleted Dataset={}", collectionName, path);
      }
    }

    boolean changed = (nnew > 0) || (ndelete > 0) || (nchange > 0);
    if (changed) {
      if (logger.isInfoEnabled())
        logger.info("{}: scan found changes {}: nnew={}, nchange={}, ndelete={}", collectionName, new Date(), nnew, nchange, ndelete);

        map = newMap;
        this.lastScanned = System.currentTimeMillis();
        this.lastChanged.set(this.lastScanned);
    } else {
        this.lastScanned = System.currentTimeMillis();
    }

    if (changed && sendEvent) {  // event is processed on this thread
      sendEvent(new TriggerEvent(this, CollectionUpdateType.always));  // watch out for infinite loop
    }

    return changed;
  }

  public void setFiles(Iterable<MFile> files) {
    Map<String, MFile> newMap = new HashMap<>();
    for (MFile file : files)
      newMap.put(file.getPath(), file);

    synchronized (this) {
      map = newMap;
      this.lastScanned = System.currentTimeMillis();
      this.lastChanged.set(this.lastScanned);
    }
  }

  public void setFilesAndRunDate(Map<String, String> filesRunDateMap) {
    // a simple mapping between file names and coordValue dates for
    // aggregations in which files are explicitly defined.
    this.filesRunDateMap = filesRunDateMap;
    // update MFileCollection map of files
    List<MFile> files = new ArrayList<>(filesRunDateMap.size());
    for (String file : filesRunDateMap.keySet()) {
      files.add(MFileOS.getExistingFile(file));
    }
    setFiles(files);
  }

  @Override
  public synchronized Iterable<MFile> getFilesSorted() {
    if (map == null)
      try {
        scanFirstTime(); // never scanned
      } catch (IOException e) {
        e.printStackTrace();
        return Collections.emptyList();
      }

    List<MFile> result = new ArrayList<>(map.values());
    if (hasDateExtractor()) {
      Collections.sort(result, new DateSorter());
    } else {
      Collections.sort(result);
    }

    return result;
  }

  @Override
  public boolean hasDateExtractor() {
    return (dateExtractor != null) && !(dateExtractor instanceof DateExtractorNone);
  }

  public synchronized Map<String, String> getFilesRunDateMap() {
    return filesRunDateMap;
  }

  // only called from synch methods
  private boolean scanFirstTime() throws IOException {
    Map<String, MFile> newMap = new HashMap<>();
    if (!hasScans()) {
        map = newMap;
        return false;
    }

    reallyScan(newMap);
    // deleteOld(newMap); // ?? hmmmmm LOOK this seems wrong; maintainence in background ?? generally collection doesnt exist

    // implement olderThan
    if (olderThanInMsecs > 0) {
      long olderThan = System.currentTimeMillis() - olderThanInMsecs; // new files must be older than this.
      Iterator<MFile> iter = newMap.values().iterator(); // need iterator so we can remove()
      while (iter.hasNext()) {
        MFile newFile = iter.next();
        String path = newFile.getPath();
        if (newFile.getLastModified() > olderThan) { // the file is too new
          iter.remove();
          logger.debug("{}: scan found new Dataset but its too recently modified = {}", collectionName, path);
        }
      }
    }

    map = newMap;
    this.lastScanned = System.currentTimeMillis();
    this.lastChanged.set(this.lastScanned);
    logger.debug("{} : initial scan found n datasets = {} ", collectionName, map.keySet().size());
    return map.keySet().size() > 0;
  }

  @Override
  CalendarDate extractRunDateWithError(MFile mfile) {
    CalendarDate result = super.extractRunDateWithError(mfile);
    // if there isn't a DateExtractor, see if a mapping exists between
    // filenames and runtimes as defied by the coordValue attribute
    // in explicitly defined file aggregations (i.e. not a directory scan)
    if (result == null)
      if (!this.filesRunDateMap.isEmpty()) {
        String dateString = filesRunDateMap.get(mfile.getPath());
        result = CalendarDate.parseISOformat(null, dateString);
      }
    if (result == null)
      logger.error("Failed to find a run date associated with file {}", mfile.getPath());
    return result;
  }

  protected void reallyScan(java.util.Map<String, MFile> map) throws IOException {
    getController(); // make sure a controller is instantiated

    // run through all scanners and collect MFile instances into the Map
    int count = 0;
    for (CollectionConfig mc : scanList) {
      long start = System.currentTimeMillis();
      // System.out.printf("MFileCollectionManager reallyScan %s %s%n", mc.getDirectoryName(), CalendarDate.present());

      // lOOK: are there any circumstances where we dont need to recheck against OS, ie always use cached values?
      Iterator<MFile> iter = (mc.wantSubdirs()) ? controller.getInventoryAll(mc, true) : controller.getInventoryTop(mc, true);  /// NCDC wants subdir /global/nomads/nexus/gfsanl/**/gfsanl_3_.*\.grb$
      if (iter == null) {
        logger.error(collectionName + ": Invalid collection= " + mc);
        continue;
      }

      while (iter.hasNext()) {
        MFile mfile = iter.next();
        mfile.setAuxInfo(mc.getAuxInfo());
        map.put(mfile.getPath(), mfile);
        count++;
      }


      long took = (System.currentTimeMillis() - start) / 1000;
      // System.out.printf("MFileCollectionManager reallyScan %s took %d secs%n", collectionName, took);
      if (logger.isDebugEnabled()) {
        long took2 = (System.currentTimeMillis() - start) / 1000;
        logger.debug("{} : was scanned nfiles= {} took={} secs", collectionName, count, took2);
     }
    }

    if (map.size() == 0) {
      if (hasScans()) {
        // only warn if a directory scan comes up with no files found
        logger.warn("MFileCollectionManager: No files found for {}", collectionName);
      }
    }
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("DatasetCollectionManager{ collectionName='%s' recheck=%s ", collectionName, recheck);
    for (CollectionConfig mc : scanList) {
      f.format("%n dir=%s filter=%s", mc.getDirectoryName(), mc.getFileFilter());
    }
    return f.toString();
  }

}
