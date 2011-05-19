/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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
import ucar.nc2.units.TimeUnit;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.IOException;

import thredds.inventory.filter.*;
import thredds.inventory.bdb.MetadataManager;
import ucar.nc2.util.ListenerManager;

/**
 * Manage one or more Directory Scanners that find MFiles
 * Keep track of when they need to be rescanned.
 * Used in:
 * <ul>
 *  <li> replaces older version in ncml.Aggregation
 *  <li> ucar.nc2.ft.point.collection.TimedCollectionImpl
 * </ul>
 *
 * looks like we need to be thread safe, for InvDatasetFeatureCollection
 *
 * @author caron
 * @since Jul 8, 2009
 */
@ThreadSafe
public class DatasetCollectionManager implements CollectionManager {
  static public final String CATALOG = "catalog:";
  static public final String RESCAN = "rescan";
  static public final String PROTO = "proto";

  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetCollectionManager.class);
  static private MController controller;

  // explicit enabling - allows deleteOld to work first time
  static private boolean enableMetadataManager = false;
  static public void enableMetadataManager() {
    enableMetadataManager = true;
  }

  /**
   * Set the MController used by scan. Defaults to thredds.filesystem.ControllerOS() if not set.
   * @param _controller use this MController
   */
  static public void setController(MController _controller) {
    controller = _controller;
  }

  // called from Aggregation - deprecated
  static public DatasetCollectionManager open(String collection, String olderThan, Formatter errlog) throws IOException {
    if (collection.startsWith(CATALOG))
      return new DatasetCollectionFromCatalog(collection);
    else
      return new DatasetCollectionManager(collection, olderThan, errlog);
  }

  ////////////////////////////////////////////////////////////////////

  // these actually dont change
  protected String collectionName;
  protected DateExtractor dateExtractor;

  // these are final
  private final CollectionSpecParser sp;
  private final List<MCollection> scanList = new ArrayList<MCollection>(); // an MCollection is a collection of managed files
  private final TimeUnit recheck; // how often to recheck
  private final double olderThanFilterInSecs;
  private final FeatureCollectionConfig.ProtoChoice protoChoice;

  // this can change
  private Map<String, MFile> map; // current map of MFile in the collection
  private long lastScanned; // last time scanned
  private long lastChanged; // last time the set of files changed

  private ListenerManager lm;

  // simplified version called from DatasetCollectionManager.open()
  private DatasetCollectionManager(String collectionSpec, String olderThan, Formatter errlog) {
    this.sp = new CollectionSpecParser(collectionSpec, errlog);
    this.collectionName = collectionSpec; // StringUtil.escape(collectionSpec, "");
    this.recheck = null;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default

    List<MFileFilter> filters = new ArrayList<MFileFilter>(2);
    if (null != sp.getFilter())
      filters.add(new WildcardMatchOnName(sp.getFilter()));
    olderThanFilterInSecs = getOlderThanFilter(filters, olderThan);

    dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new MCollection(sp.getTopDir(), sp.getTopDir(), sp.wantSubdirs(), filters, null));
    createListenerManager();
  }

  /* public DatasetCollectionManager(CollectionSpecParser sp, Formatter errlog) {
    this.collectionName = sp.getSpec();
    this.sp = sp;
    MFileFilter mfilter = (null == sp.getFilter()) ? null : new WildcardMatchOnName(sp.getFilter());
    this.dateExtractor = (sp.getDateFormatMark() == null) ? null : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new MCollection(sp.getTopDir(), sp.getTopDir(), sp.wantSubdirs(), mfilter, null));
  } */

  public DatasetCollectionManager(FeatureCollectionConfig config, Formatter errlog) {
    this.sp = new CollectionSpecParser(config.spec, errlog);
    this.collectionName = config.name != null ? config.name : config.spec;

    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != sp.getFilter())
      filters.add( new WildcardMatchOnName(sp.getFilter()));
    olderThanFilterInSecs = getOlderThanFilter(filters, config.olderThan);

    dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new MCollection(sp.getTopDir(), sp.getTopDir(), sp.wantSubdirs(), filters, null));

    this.recheck = makeRecheck(config.recheckAfter);
    protoChoice = config.protoConfig.choice;
    createListenerManager();
  }

  private double getOlderThanFilter(List<MFileFilter> filters, String olderThan) {
    if (olderThan != null) {
      try {
        TimeUnit tu = new TimeUnit(olderThan);
        double olderThanV = tu.getValueInSeconds();
        filters.add( new LastModifiedLimit((long) (1000 * olderThanV)));
        return olderThanV;
      } catch (Exception e) {
        logger.error(collectionName + ": Invalid time unit for olderThan = {}", olderThan);
      }
    }
    return -1;
  }

  private TimeUnit makeRecheck(String recheckS) {
    if (recheckS != null) {
      try {
        return new TimeUnit(recheckS);
      } catch (Exception e) {
        logger.error(collectionName+": Invalid time unit for recheckEvery = {}", recheckS);
      }
    }
    return null;
  }

  public void close() {
    if (mm != null) mm.close(); 
  }

  // for subclasses
  protected DatasetCollectionManager() {
    this.recheck = null;
    this.olderThanFilterInSecs = -1;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default
    this.sp = null;
    createListenerManager();
  }

  ////////////////////////////////////////////////////////////////////
  /**
   * For retrofitting to Aggregation
   * Must also call addDirectoryScan one or more times
   * @param recheckS a undunit time unit, specifying how often to rscan
   */
  public DatasetCollectionManager(String recheckS) {
    this.recheck = makeRecheck(recheckS);
    this.olderThanFilterInSecs = -1;
    this.protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate;
    this.sp = null;
    createListenerManager();
  }

  public void setDateExtractor( DateExtractor dateExtractor) {
    this.dateExtractor = dateExtractor;
  }

  /**
   * Add a directory scan to the collection
   * @param dirName scan this directory
   * @param suffix  require this suffix (overriddden by regexp), may be null
   * @param regexpPatternString if present, use this reqular expression to filter files , may be null
   * @param subdirsS if "true", descend into subdirectories, may be null
   * @param olderS udunit time unit - files must be older than this amount of time (now - lastModified > olderTime), may be null
   // * @param dateFormatString dateFormatMark string, may be null
   * @param auxInfo attach this object to any MFile found by this scan
   */
  public void addDirectoryScan(String dirName, String suffix, String regexpPatternString, String subdirsS, String olderS, Object auxInfo) {
    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != regexpPatternString)
      filters.add(new RegExpMatchOnName(regexpPatternString));
    else if (suffix != null)
      filters.add(new WildcardMatchOnPath("*" + suffix+"$"));

    if (olderS != null) {
      try {
        TimeUnit tu = new TimeUnit(olderS);
        filters.add(new LastModifiedLimit((long) (1000 * tu.getValueInSeconds())));
      } catch (Exception e) {
        logger.error(collectionName+": Invalid time unit for olderThan = {}", olderS);
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
  public String getCollectionName() {
    return collectionName;
  }

  @Override
  public String getRoot() {
    return (sp == null) ? null : sp.getTopDir();
  }

  public CollectionSpecParser getCollectionSpecParser() {
    return sp;
  }

  public double getOlderThanFilterInSecs() {
    return olderThanFilterInSecs;
  }

  /**
   * Get how often to rescan
   *
   * @return time dureation of rescan period, or null if none.
   */
  @Override
  public TimeUnit getRecheck() {
    return recheck;
  }

  /**
   * Get the last time scanned
   *
   * @return msecs since 1970
   */
  @Override
  public long getLastScanned() {
    return lastScanned;
  }


  /**
   * Get the last time files changed
   *
   * @return msecs since 1970
   */
  @Override
  public long getLastChanged() {
    return lastChanged;
  }

  /**
   * Scan the directory(ies) and create MFile objects.
   * Get the results from getFiles()
   *
   * @param cancelTask allow user to cancel
   * @throws java.io.IOException if io error
   */
  public void scan(CancelTask cancelTask) throws IOException {
    Map<String, MFile> newMap = new HashMap<String, MFile>();
    if (!hasScans()) {
      map = newMap;
      return;
    }

    scan(newMap, cancelTask);
    deleteOld(newMap);

    // LOOK how often ??
    synchronized(this) {
      map = newMap;
      this.lastScanned = System.currentTimeMillis();
      this.lastChanged = this.lastScanned;
    }

    if (logger.isInfoEnabled()) logger.debug(collectionName+": initial scan found n datasets = "+map.keySet().size());
    lm.sendEvent(new TriggerEvent(RESCAN));
  }

  public boolean rescanIfNeeded() throws IOException {
    if (isRescanNeeded())
      return rescan();
    return false;
  }

  protected boolean hasScans() {
    return !scanList.isEmpty();
  }

  /**
   * Compute if rescan is needed, based on the recheckEvery parameter.
   * True if scanList not empty, recheckEvery not null, and recheckEvery time has passed since last scan
   *
   * @return true if rescan is needed
   */
  @Override
  public boolean isRescanNeeded() {
    if (!hasScans()) {
      if (logger.isDebugEnabled()) logger.debug(collectionName+": rescan not needed, no scanners");
      return false;
    }

    // see if we need to recheck
    if (recheck == null) {
      if (logger.isDebugEnabled()) logger.debug(collectionName+": rescan not needed, recheck null");
      return false;
    }

    Date now = new Date();
    Date lastCheckedDate = new Date(lastScanned);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      if (logger.isDebugEnabled()) logger.debug(collectionName+": rescan not needed, last= " + lastCheckedDate + " now = " + now);
      return false;
    }

    return true;
  }

  /**
   * Rescan the collection. Files may have been deleted or added.
   * If the MFile already exists in the current list, leave it in the list.
   * If returns true, get the results from getFiles(), otherwise nothing has changed.
   *
   * @return true if anything actually changed.
   * @throws IOException on I/O error
   */
  @Override
  public boolean rescan() throws IOException {
    if (map == null) {
      scan(null);
      return hasScans();
    }

    // rescan
    Map<String, MFile> oldMap = map;
    Map<String, MFile> newMap = new HashMap<String, MFile>();
    scan(newMap, null);

    // replace with previous datasets if they exist
    int nnew = 0;
    for (MFile newDataset : newMap.values()) {
      String path = newDataset.getPath();
      MFile oldDataset = oldMap.get(path);
      if (oldDataset != null) {
        newMap.put(path, oldDataset);
        if (logger.isDebugEnabled()) logger.debug(collectionName+": rescan retains old Dataset= " + path);
      } else {
        nnew++;
        if (logger.isDebugEnabled()) logger.debug(collectionName+": rescan found new Dataset= " + path);
      }
    }

    // check for deletions
    int ndelete = 0;
    for (MFile oldDataset : oldMap.values()) {
      String path = oldDataset.getPath();
      MFile newDataset = newMap.get(path);
      if (newDataset == null) {
        ndelete++;
        if (logger.isDebugEnabled()) logger.debug(collectionName+": rescan found deleted Dataset= " + path);
      }
    }

    boolean changed = (nnew > 0) || (ndelete > 0);

    if (changed) {
      //if (logger.isInfoEnabled()) logger.info(collectionName+": rescan found changes new = "+nnew+" delete= "+ndelete);
      synchronized (this) {
        map = newMap;
        this.lastScanned = System.currentTimeMillis();
        this.lastChanged = this.lastScanned;
      }
    } else {
       synchronized (this) {
         this.lastScanned = System.currentTimeMillis();
      }
    }

    if (logger.isInfoEnabled()) logger.info(collectionName+": rescan at " + new Date()+": nnew = "+nnew+" ndelete = "+ndelete);

    if (changed)
      lm.sendEvent(new TriggerEvent(RESCAN));  // LOOK watch out for infinite loop

    return changed;
  }

  /**
   * Get the current collection of MFile, since last scan or rescan.
   * Must call scan() or rescan() at least once
   * @return current list of MFile, sorted by name
   */
  @Override
  public List<MFile> getFiles() {
    if (map == null) return null; // never scanned
    List<MFile> result = new ArrayList<MFile>(map.values());
    Collections.sort(result);
    return result;
  }

  @Override
  public Date extractRunDate(MFile mfile) {
    return (dateExtractor == null) ? null : dateExtractor.getDate(mfile);
  }

  public boolean hasDateExtractor() {
    return (dateExtractor != null) && !(dateExtractor instanceof DateExtractorNone);
  }

  protected void scan(java.util.Map<String, MFile> map, CancelTask cancelTask) throws IOException {
    if (null == controller) controller = new thredds.filesystem.ControllerOS();  // default

    // run through all scanners and collect MFile instances into the Map
    for (MCollection mc : scanList) {

      Iterator<MFile> iter = (mc.wantSubdirs()) ? controller.getInventory(mc) : controller.getInventoryNoSubdirs(mc);
      if (iter == null) {
        logger.error(collectionName+": DatasetCollectionManager Invalid collection= " + mc);
        continue;
      }

      while (iter.hasNext()) {
        MFile mfile = iter.next();
        mfile.setAuxInfo(mc.getAuxInfo());
        map.put(mfile.getPath(), mfile);
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
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


  ////////////////////////////////////////////////////
  // proto dataset handling - LOOK

  public int getProtoIndex() {
    int n = map.values().size();
    int protoIdx = 0;
    switch (protoChoice) {
      case First:
        protoIdx = 0;
        break;
      case Random:
        Random r = new Random(System.currentTimeMillis());
        protoIdx = r.nextInt(n - 1);
        break;
      case Penultimate:
        protoIdx = Math.max(n - 2, 0);
        break;
      case Latest:
        protoIdx = Math.max(n - 1, 0);
        break;
    }
    return protoIdx;
  }

  public void resetProto() {  // LOOK
    lm.sendEvent(new TriggerEvent(PROTO));
  }

  ////////////////////////////////////////////////////
  // events; keep the code from getting too coupled

  private void createListenerManager() {
    lm = new ListenerManager(
            "thredds.inventory.DatasetCollectionManager$TriggerListener",
            "thredds.inventory.DatasetCollectionManager$TriggerEvent",
            "handleCollectionEvent");
  }

  public void addEventListener(TriggerListener l) {
    lm.addListener(l);
  }

  public void removeEventListener(TriggerListener l) {
    lm.removeListener(l);
  }

  public class TriggerEvent extends java.util.EventObject {
    private String message;

    TriggerEvent(String message) {
      super(DatasetCollectionManager.this);
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  public static interface TriggerListener {
    public void handleCollectionEvent(TriggerEvent event);
  }

  /////////////////////////////////////////////////////////////////
  // use bdb to manage metadata associated with the collection. currently, only DatasetInv.xml files

  private MetadataManager mm;

  private void initMM() {
    if (collectionName == null) return; // eg no scan in ncml
    try {
      mm = new MetadataManager(collectionName);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  // clean up deleted files in metadata manager
  private void deleteOld(Map<String, MFile> newMap) {
    if (mm == null && enableMetadataManager) initMM();
    if (mm != null) mm.delete(newMap);
  }

  @Override
  public void putMetadata(MFile file, String key, byte[] value) {
    if (mm == null) initMM();
    if (mm != null) mm.put(file.getPath()+"#"+key, value);
  }

  @Override
  public byte[] getMetadata(MFile file, String key) {
    if (mm == null) initMM();
    return (mm == null) ? null : mm.getBytes(file.getPath()+"#"+key);
  }


}
