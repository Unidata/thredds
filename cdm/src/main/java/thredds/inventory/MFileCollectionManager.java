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

import net.jcip.annotations.ThreadSafe;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.TimeDuration;

import java.util.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import thredds.inventory.filter.*;

/**
 * Manage Collections of MFiles.
 * Used in:
 * <ul>
 * <li> replaces older version in ncml.Aggregation
 * <li> ucar.nc2.ft.point.collection.TimedCollectionImpl (ucar.nc2.ft.point.collection.CompositeDatasetFactory)
 * <li> ucar.nc2.ft.fmrc.Fmrc
 * <li> ucar.nc2.grib.GribCollection
 * </ul>
 * <p/>
 * we need to be thread safe, for InvDatasetFeatureCollection
 *
 * @author caron
 * @since Jul 8, 2009
 */
@ThreadSafe
public class MFileCollectionManager extends CollectionManagerAbstract {
  static public final String CATALOG = "catalog:";

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
  static public MFileCollectionManager open(String collectionName, String olderThan, Formatter errlog) throws IOException {
    if (collectionName.startsWith(CATALOG))
      return new CatalogCollectionManager(collectionName);
    else
      return new MFileCollectionManager(collectionName, olderThan, errlog);
  }

  // retrofit to Aggregation
  static public MFileCollectionManager openWithRecheck(String collectionName, String recheckS) {
    return new MFileCollectionManager(collectionName, recheckS);
  }

  ////////////////////////////////////////////////////////////////////

  // these actually dont change, but are not set in the constructor
  protected DateExtractor dateExtractor;
  protected CalendarDate startPartition;

  // these are final
  private final List<MCollection> scanList = new ArrayList<MCollection>(); // an MCollection is a collection of managed files
  private final long olderThanInMsecs;
  private final String rootDir;
  protected FeatureCollectionConfig config;

  // this can change = keep under lock
  private Map<String, MFile> map; // current map of MFile in the collection
  private long lastScanned; // last time scanned
  private AtomicLong lastChanged = new AtomicLong(); // last time the set of files changed

  // simplified version called from DatasetCollectionManager.open()
  private MFileCollectionManager(String collectionSpec, String olderThan, Formatter errlog) {
    super(collectionSpec, null);
    CollectionSpecParser sp = new CollectionSpecParser(collectionSpec, errlog);
    this.recheck = null;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default
    this.rootDir = sp.getRootDir();

    List<MFileFilter> filters = new ArrayList<MFileFilter>(2);
    if (null != sp.getFilter())
      filters.add(new WildcardMatchOnName(sp.getFilter()));
    olderThanInMsecs = parseOlderThanFilter(olderThan);

    dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new MCollection(sp.getRootDir(), sp.getRootDir(), sp.wantSubdirs(), filters, null));
  }

  // this is the full featured constructor, using FeatureCollectionConfig for config.
  public MFileCollectionManager(FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) {
    super(config.name != null ? config.name : config.spec, logger);
    this.config = config;

    CollectionSpecParser sp = new CollectionSpecParser(config.spec, errlog);
    this.rootDir = sp.getRootDir();

    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != sp.getFilter())
      filters.add(new WildcardMatchOnName(sp.getFilter()));
    olderThanInMsecs = parseOlderThanFilter(config.olderThan);

    if (config.dateFormatMark != null)
      dateExtractor = new DateExtractorFromName(config.dateFormatMark, false);
    else if (sp.getDateFormatMark() != null)
      dateExtractor = new DateExtractorFromName(sp.getDateFormatMark(), true);
    else
      dateExtractor = new DateExtractorNone();

    scanList.add(new MCollection(sp.getRootDir(), sp.getRootDir(), sp.wantSubdirs(), filters, null));

    this.recheck = makeRecheck(config.updateConfig.recheckAfter);
    protoChoice = config.protoConfig.choice;

    // static means never rescan on checkState; let it be externally triggered.
    if ((config.updateConfig.recheckAfter == null) && (config.updateConfig.rescan == null) &&  (config.updateConfig.deleteAfter == null))
      setStatic(true);
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
    this.rootDir = null;
  }

  ////////////////////////////////////////////////////////////////////////////////

  public MFileCollectionManager(String name, String spec, Formatter errlog, org.slf4j.Logger logger) {
    super(name, logger);
    CollectionSpecParser sp = new CollectionSpecParser(spec, errlog);
    this.rootDir = sp.getRootDir();

    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != sp.getFilter())
      filters.add(new WildcardMatchOnName(sp.getFilter()));

    dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new MCollection(sp.getRootDir(), sp.getRootDir(), sp.wantSubdirs(), filters, null));

    this.recheck = null;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default
    this.olderThanInMsecs = -1;
  }

  public MFileCollectionManager(String name, MCollection mc, CalendarDate startPartition, org.slf4j.Logger logger) {
    super(name, logger);
    this.startPartition = startPartition;
    this.scanList.add(mc);

    this.rootDir = mc.getDirectoryName();
    this.recheck = null;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default
    this.olderThanInMsecs = -1;
  }

  @Override
  public CalendarDate getStartCollection() {
    return startPartition;
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
    this.rootDir = null;
  }

  public void setDateExtractor(DateExtractor dateExtractor) {
    this.dateExtractor = dateExtractor;
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
    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != regexpPatternString)
      filters.add(new RegExpMatchOnName(regexpPatternString));
    else if (suffix != null)
      filters.add(new WildcardMatchOnPath("*" + suffix + "$"));

    if (olderS != null) {
      try {
        TimeDuration tu = new TimeDuration(olderS);
        filters.add(new LastModifiedLimit((long) (1000 * tu.getValueInSeconds())));
      } catch (Exception e) {
        logger.error(collectionName + ": Invalid time unit for olderThan = {}", olderS);
      }
    }

    boolean wantSubdirs = true;
    if ((subdirsS != null) && subdirsS.equalsIgnoreCase("false"))
      wantSubdirs = false;

    MFileFilter filter = (filters.size() == 0) ? null : ((filters.size() == 1) ? filters.get(0) : new Composite(filters));
    MCollection mc = new thredds.inventory.MCollection(dirName, dirName, wantSubdirs, filter, auxInfo);

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

  @Override
  public String getRoot() {
    return rootDir;
  }

  @Override
  public long getOlderThanFilterInMSecs() {
    return olderThanInMsecs;
  }

  @Override
  public long getLastScanned() {
    return lastScanned;
  }

  @Override
  public long getLastChanged() {
    return lastChanged.get();
  }

  @Override
  public boolean scanIfNeeded() throws IOException {
    if (map == null && !isStatic()) return true;
    return isScanNeeded() && scan(true);
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

    if (map == null && !isStatic()) {
      logger.debug("{}: scan needed, never scanned", collectionName);
      return true;
    }

    Date now = new Date();
    Date lastCheckedDate = new Date(lastScanned);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      logger.debug("{}: scan not needed, last scanned={}, now={}", collectionName, lastCheckedDate, now);
      return false;
    }

    return true;
  }

  /**
   * Do not use
   * @throws IOException
   */
  public void scanDebug(Formatter f) throws IOException {
    getController(); // make sure a controller is instantiated

    // run through all scanners and collect MFile instances into the Map
    for (MCollection mc : scanList) {

      // lOOK: are there any circumstances where we dont need to recheck against OS, ie always use cached values?
      Iterator<MFile> iter = (mc.wantSubdirs()) ? controller.getInventoryAll(mc, true) : controller.getInventoryTop(mc, true);  /// NCDC wants subdir /global/nomads/nexus/gfsanl/**/gfsanl_3_.*\.grb$
      if (iter == null) {
        logger.error(collectionName + ": Invalid collection= " + mc);
        continue;
      }

      int count = 0;
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        mfile.setAuxInfo(mc.getAuxInfo());
        map.put(mfile.getPath(), mfile);
        count++;
      }
      logger.debug("{} : was scanned nfiles= {} ", collectionName, count);
    }

  }

  @Override
  public boolean scan(boolean sendEvent) throws IOException {
    if (map == null) {
      boolean changed = scanFirstTime();
      if (changed && sendEvent)
        sendEvent(new TriggerEvent(this, TriggerType.update));  // watch out for infinite loop
      return changed;
    }

    long olderThan = (olderThanInMsecs <= 0) ? -1 : System.currentTimeMillis() - olderThanInMsecs; // new files must be older than this.

    // rescan
    Map<String, MFile> oldMap = map;
    Map<String, MFile> newMap = new HashMap<String, MFile>();
    reallyScan(newMap);

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

      synchronized (this) {
        map = newMap;
        this.lastScanned = System.currentTimeMillis();
        this.lastChanged.set(this.lastScanned);
      }
    } else {
      synchronized (this) {
        this.lastScanned = System.currentTimeMillis();
      }
    }

    if (changed && sendEvent) {  // event is processed on this thread
      sendEvent(new TriggerEvent(this, TriggerType.update));  // watch out for infinite loop
    }

    return changed;
  }

  @Override
  public void setFiles(Iterable<MFile> files) {
    Map<String, MFile> newMap = new HashMap<String, MFile>();
    for (MFile file : files)
      newMap.put(file.getPath(), file);

    synchronized (this) {
      map = newMap;
      this.lastScanned = System.currentTimeMillis();
      this.lastChanged.set(this.lastScanned);
    }
  }

  @Override
  public Iterable<MFile> getFiles() {
    if (map == null)
      try {
        scanFirstTime(); // never scanned
      } catch (IOException e) {
        e.printStackTrace();
        return Collections.emptyList();
      }

    List<MFile> result = new ArrayList<MFile>(map.values());
    if (hasDateExtractor()) {
      Collections.sort(result, new DateSorter());
    } else {
      Collections.sort(result);
    }

    return result;
  }

  private class DateSorter implements Comparator<MFile> {
    public int compare(MFile m1, MFile m2) {
      return extractRunDateWithError(m1).compareTo(extractRunDateWithError(m2));
    }
  }

  @Override
  public CalendarDate extractRunDate(MFile mfile) {
    return (dateExtractor == null) ? null : dateExtractor.getCalendarDate(mfile);
  }

  private CalendarDate extractRunDateWithError(MFile mfile) {
    CalendarDate result = extractRunDate(mfile);
    if (result == null)
      logger.error("Failed to extract date from file {} with Extractor {}", mfile.getPath(), dateExtractor);
    return result;
  }

  @Override
  public boolean hasDateExtractor() {
    return (dateExtractor != null) && !(dateExtractor instanceof DateExtractorNone);
  }

  private boolean scanFirstTime() throws IOException {
    Map<String, MFile> newMap = new HashMap<String, MFile>();
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

    synchronized (this) {
      map = newMap;
      this.lastScanned = System.currentTimeMillis();
      this.lastChanged.set(this.lastScanned);
    }
    logger.debug("{} : initial scan found n datasets = {} ", collectionName, map.keySet().size());
    return map.keySet().size() > 0;
  }

  protected void reallyScan(java.util.Map<String, MFile> map) throws IOException {
    getController(); // make sure a controller is instantiated

    // run through all scanners and collect MFile instances into the Map
    for (MCollection mc : scanList) {

      // lOOK: are there any circumstances where we dont need to recheck against OS, ie always use cached values?
      Iterator<MFile> iter = (mc.wantSubdirs()) ? controller.getInventoryAll(mc, true) : controller.getInventoryTop(mc, true);  /// NCDC wants subdir /global/nomads/nexus/gfsanl/**/gfsanl_3_.*\.grb$
      if (iter == null) {
        logger.error(collectionName + ": Invalid collection= " + mc);
        continue;
      }

      int count = 0;
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        mfile.setAuxInfo(mc.getAuxInfo());
        map.put(mfile.getPath(), mfile);
        count++;
      }
      logger.debug("{} : was scanned nfiles= {} ", collectionName, count);
    }

    if (map.size() == 0) {
      logger.warn("MFileCollectionManager: No files found for {}", collectionName);
    }
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("DatasetCollectionManager{ collectionName='%s' recheck=%s ", collectionName, recheck);
    for (MCollection mc : scanList) {
      f.format("%n dir=%s filter=%s", mc.getDirectoryName(), mc.getFileFilter());
    }
    return f.toString();
  }

}