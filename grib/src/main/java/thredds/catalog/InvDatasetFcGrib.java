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

package thredds.catalog;

import net.jcip.annotations.ThreadSafe;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.Grib1Iosp;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Iosp;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implement FeatureCollection GRIB - a collection of Grib1 or Grib2 files that are served as Grids.
 * <p/>
 * Dataset naming
 * <ol>
 * <li>Single group in collection
 * <pre>
 *     $collectionName-collection
 *   </pre>
 * </li>
 * </ol>
 *
 * @author caron
 * @since 4/15/11
 */
@ThreadSafe
public class InvDatasetFcGrib extends InvDatasetFeatureCollection {
  //static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFcGrib.class);
  static private final String COLLECTION = "collection";
  static private final String BEST_DATASET = "best";

  static protected final String LATEST_DATASET = "latest";
  static protected final String LATEST_DATASET_NAME = "Latest Run";

  /////////////////////////////////////////////////////////////////////////////
  protected class StateGrib extends State {
    TimePartition timePartition;
    GribCollection gribCollection;
    InvDatasetImpl top;

    protected StateGrib(StateGrib from) {
      super(from);
      if (from != null) {
        this.timePartition = from.timePartition;
        this.gribCollection = from.gribCollection;
        this.top = from.top;
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  private final FeatureCollectionConfig.GribConfig gribConfig;
  private final AtomicBoolean needsUpdate = new AtomicBoolean();
  private final AtomicBoolean needsProto = new AtomicBoolean();
  private boolean first = true;
  private DataFormatType format;
  private String bestDatasetName = "Best Timeseries";

  public InvDatasetFcGrib(InvDatasetImpl parent, String name, String path, FeatureCollectionType fcType, FeatureCollectionConfig config) {
    super(parent, name, path, fcType, config);
    this.gribConfig = config.gribConfig;

    Formatter errlog = new Formatter();
    //if (config.useIndexOnly)
    //  this.dcm = TimePartitionCollection.fromExistingIndices(config, errlog); // not used
    // else
    if (config.timePartition != null) {
      this.dcm = TimePartitionCollection.factory(config, errlog, logger);
      this.dcm.setChangeChecker(GribIndex.getChangeChecker());
    } else {
      this.dcm = new MFileCollectionManager(config, errlog, logger);
      this.dcm.setChangeChecker(GribIndex.getChangeChecker());
    }

    // sneak in extra config info
    if (config.gribConfig != null)
      dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);

    String errs = errlog.toString();
    if (errs.length() > 0) logger.warn("{}: CollectionManager parse error = {} ", name, errs);

    tmi.setDataType(FeatureType.GRID); // override GRIB

    if (config.gribConfig.bestNamer != null) {
      this.bestDatasetName = config.gribConfig.bestNamer;
    }

    finish();
  }

  @Override
  public void update(CollectionManager.Force force) { // this may be called from a background thread
    if (first) {
      synchronized (lock) {
        this.format = getDataFormatType(); // why wait until now ??
        firstInit(); // why ??
        first = false;
      }
    }

    /* if (force == CollectionManager.Force.nocheck) {
      // we need to update the dcm without triggering an index rewrite
      try {
        dcm.scan(false);
      } catch (IOException e) {
        logger.error("Error on scan " + dcm, e);
      }
    } */

    // do the update in a local object
    StateGrib localState = new StateGrib((StateGrib) state);
    try {
      updateCollection(localState, force);
    } catch (Throwable e) {
      logger.error("Fail to create/update collection", e);
      return;
    }
    makeDatasetTop(localState);
    localState.lastInvChange = System.currentTimeMillis();

    // switch to live
    synchronized (lock) {
      needsUpdate.set(false);
      needsProto.set(false);
      state = localState;
    }

  }

  @Override
  public void updateProto() {
    needsProto.set(true);

    // no actual work, wait until next call to updateCollection (??)
    // not sure proto is used in GribFc
  }

  @Override
  protected StateGrib checkState() throws IOException { // this is called from the request thread
    synchronized (lock) {
      if (first) {
        this.format = getDataFormatType(); // for some reason have to wait until first request ??
        firstInit();
        dcm.scanIfNeeded();
        first = false;

      } else {
        if (!dcm.scanIfNeeded())
          return (StateGrib) state;
      }

      // if this is the TDS, and its using the TDM, then you are not allowed to update
      // if there is no update config, assume static, and try to skip checking for changes. got that?
      boolean tdsUsingTdm = !CollectionUpdater.INSTANCE.isTdm() && config.tdmConfig != null;
      CollectionManager.Force ff = (tdsUsingTdm || dcm.isStatic()) ? CollectionManager.Force.nocheck : CollectionManager.Force.test;

      // update local copy of state, then switch all at once
      StateGrib localState = new StateGrib((StateGrib) state);
      updateCollection(localState, ff);

      makeDatasetTop(localState);
      localState.lastInvChange = System.currentTimeMillis();
      needsUpdate.set(false);
      needsProto.set(false);

      state = localState;
      return localState;
    }
  }

  private void updateCollection(StateGrib localState, CollectionManager.Force force) throws IOException {
    if (config.timePartition != null) {
      TimePartition previous = localState.timePartition;
      localState.timePartition = TimePartition.factory(format == DataFormatType.GRIB1, (TimePartitionCollection) this.dcm, force, logger);
      localState.gribCollection = null;

      if (previous != null) previous.delete(); // LOOK may be another thread using - other thread will fail
      logger.debug("{}: TimePartition object was recreated", getName());

    } else {
      GribCollection previous = localState.gribCollection;
      localState.gribCollection = GribCollection.factory(format == DataFormatType.GRIB1, dcm, force, logger);
      localState.timePartition = null;
      if (previous != null) previous.close(); // LOOK may be another thread using - other thread will fail
      logger.debug("{}: GribCollection object was recreated", getName());
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private void makeDatasetTop(StateGrib localState) {
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      top.transferMetadata(parent, true); // make all inherited metadata local
    String tpath = getPath()+"/"+COLLECTION;
    top.setID(tpath);

    GribCollection gc = localState.timePartition == null ? localState.gribCollection : localState.timePartition;
    List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gc.getGroups());
    Collections.sort(groups);
    boolean isSingleGroup = (groups.size() == 1);

    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    tmi.addVariableMapLink(makeMetadataLink(tpath, VARIABLES));
    tmi.setGeospatialCoverage(extractGeospatial(groups));
    tmi.setTimeCoverage(extractCalendarDateRange(groups));
    tmi.setServiceName(virtualService.getName());

    if (localState.timePartition == null) { // no time partitions

      for (GribCollection.GroupHcs group : groups) {
        InvDatasetImpl ds = isSingleGroup ? top : new InvDatasetImpl(this, group.getDescription());

        String groupId = isSingleGroup ? null : group.getId();
        String dpath =  isSingleGroup ? getPath() : getPath() + "/" + groupId;

        if (!isSingleGroup) {
          ds.setID(dpath);
          top.addDataset(ds);
        }

        ds.tmi.setGeospatialCoverage(extractGeospatial(group));
        ds.tmi.setTimeCoverage(extractCalendarDateRange(group));

        if (gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Best)) {
          InvDatasetImpl best = new InvDatasetImpl(this, getBestDatasetName());
          String path = dpath + "/" + BEST_DATASET;
          best.setUrlPath(path);
          best.setID(path);
          best.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
          ds.addDataset(best);
        }

        if (gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Files)) {
          String name = isSingleGroup ? FILES : groupId + "/" + FILES;
          InvCatalogRef filesCat = new InvCatalogRef(this, FILES, getCatalogHref(name));
          filesCat.finish();
          ds.addDataset(filesCat);
        }

      }

    } else { // is a time partition

      if (isSingleGroup) {
        if (gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Best)) {
          InvDatasetImpl best = new InvDatasetImpl(this, getBestDatasetName());
          String path = getPath() + "/" + BEST_DATASET;
          best.setUrlPath(path);
          best.setID(path);
          best.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
          top.addDataset(best);
        }

        if (gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.LatestFile)) {
          InvDatasetImpl ds2 = new InvDatasetImpl(this, getLatestFileName());
          ds2.setUrlPath(FILES+"/"+LATEST_DATASET_CATALOG);
          ds2.setID(FILES+"/"+LATEST_DATASET_CATALOG);
          ds2.setServiceName(LATEST_SERVICE);
          ds2.finish();
          top.addDataset(ds2);
        }

      } else {

        if (gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Best)) {
          String dname =  getPath() + "/" + BEST_DATASET;
          InvCatalogRef ds = new InvCatalogRef(this, getBestDatasetName(), getCatalogHref(dname));
          top.addDataset(ds);
        }
      }

      for (TimePartition.Partition dc : localState.timePartition.getPartitionsSorted()) {
        String dname = dc.getName();
        InvCatalogRef ds = new InvCatalogRef(this, dname, getCatalogHref(dname));
        top.addDataset(ds);
      }

    }

    top.finish();
    localState.top = top;
  }

  /* file datasets of the partition catalog are InvCatalogRef pointing to "FileCatalogs"
  private void addFileDatasets(InvDatasetImpl parent, String prefix) {
    String name = (prefix == null) ? FILES : prefix + "/" + FILES;
    InvCatalogRef ds = new InvCatalogRef(this, FILES, getCatalogHref(name));
    ds.finish();
    parent.addDataset(ds);
  }  */

  /////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when a catref is requested
  /* Possible catref paths: (OLD)
      1. path/files/catalog.xml
      2. path/partitionName/files/catalog.xml
      3. path/groupName/files/catalog.xml

      4. path/collection/catalog.xml
      5. path/partitionName/catalog.xml

     Possible catref paths: (NEW)
      1. path/files/catalog.xml
      2. path/partitionName/files/catalog.xml
      3. path/groupName/files/catalog.xml

      4. path/collection/catalog.xml
      5. path/partitionName/catalog.xml
   */
  @Override
  public InvCatalogImpl makeCatalog(String match, String reqPath, URI catURI) {
    //logger.debug("{}: make catalog for {} {}", name, match, baseURI);
    StateGrib localState;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    if (localState == null) return null; // not ready yet I think

    try {
      // top catalog : uses state.top previously made in checkState()
      if ((match == null) || (match.length() == 0)) {
        return makeCatalogTop(catURI, localState);
        //main.addService(virtualService);
        //main.getDataset().getLocalMetadataInheritable().setServiceName(virtualService.getName());
        //main.finish();
        //return main;
      }

      if (localState.timePartition == null) {
        String[] path = match.split("/");
        GribCollection.GroupHcs group;
        if ((path.length == 1) && (path[0].equals(FILES))) { // single group case
          group = localState.gribCollection.getGroup(0);
        } else {
          if (path.length < 2) return null;
          group = localState.gribCollection.findGroupById(path[0]);
        }
        if (group != null) {
          return makeCatalogFiles(localState.gribCollection, group, catURI, localState, false);
        }

      } else { // time partitions

        if (match.endsWith(BEST_DATASET)) {
          return makeCatalogPartition(localState.timePartition, catURI, localState);
        }

        TimePartition.Partition tpp = localState.timePartition.getPartitionByName(match);
        if (tpp != null) {
          GribCollection gc = tpp.getGribCollection();
          InvCatalogImpl result = makeCatalogPartition(gc, catURI, localState);
          gc.close();
          return result;
        }

        // files catalogs
        String[] path = match.split("/");
        if (path.length < 2) return null;

        /* collection level has form <partitionName>/<hcs>/files eg 200808/LatLon-181X360/files
        if (path[0].equals(timePartition.getName())) {
          GribCollection.GroupHcs group = timePartition.findGroup(path[1]);
          if (group == null) return null;
          return makeFilesCatalog(timePartition, group, baseURI, localState);
        } */

        // otherwise of form <partition>/<hcs>/files eg 200808/LatLon-181X360/files
        tpp = localState.timePartition.getPartitionByName(path[0]);
        if (tpp != null) {
          InvCatalogImpl result;
          GribCollection gc = tpp.getGribCollection();
          if (path[1].equals(FILES)) {
            GribCollection.GroupHcs group = gc.getGroup(0);
            result = makeCatalogFiles(gc, group, catURI, localState, true);
          } else {
            GribCollection.GroupHcs group = gc.findGroupById(path[1]);
            if (group == null) return null;
            result = makeCatalogFiles(gc, group, catURI, localState, true);
          }
          gc.close();
          return result;
        }
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }


  @Override
  protected InvCatalogImpl makeCatalogTop(URI catURI, State localState) throws IOException, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), catURI);

    mainCatalog.addDataset(((StateGrib)localState).top);
    mainCatalog.addService(InvService.latest);  // in case its needed
    mainCatalog.addService(virtualService);
    // top.getLocalMetadataInheritable().setServiceName(virtualService.getName());  //??
    mainCatalog.finish();

    mainCatalog.finish();

    return mainCatalog;
  }

  // each partition gets its own catalog, showing the different groups (horiz coord sys)
  private InvCatalogImpl makeCatalogPartition(GribCollection gribCollection, URI catURI, State localState) throws IOException {

    String partitionName = gribCollection.getName();
    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();

    InvCatalogImpl partCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), catURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(partitionName);
    top.setID(partitionName);
    partCatalog.addDataset(top);

    // services need to be local
    partCatalog.addService(virtualService);
    top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gribCollection.getGroups());
    Collections.sort(groups);
    boolean isSingleGroup = (groups.size() == 1);
    boolean isBest = gribCollection instanceof TimePartition;

    String tpath = getPath()+"/"+partitionName;
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    tmi.addVariableMapLink(makeMetadataLink(tpath, VARIABLES));
    tmi.setGeospatialCoverage(extractGeospatial(groups));
    tmi.setTimeCoverage(extractCalendarDateRange(groups));

    for (GribCollection.GroupHcs group : groups) {
      InvDatasetImpl ds;
      String groupId = group.getId();
      String dpath;
      InvDatasetImpl container = top;

      if (!isSingleGroup && !isBest) {
        container = new InvDatasetImpl(this, group.getDescription());
        container.setID(group.getId());
        top.addDataset(container);
      }

      if (gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Best)) {

        if (isSingleGroup) {
          ds = new InvDatasetImpl(this, getBestDatasetName() +" for "+ partitionName);
          dpath = this.path + "/" + partitionName + "/" + BEST_DATASET;

        } else if (isBest) { // over all partitions
          ds = new InvDatasetImpl(this, getBestDatasetName() +" for "+ groupId);
          dpath = this.path + "/" + groupId + "/" + BEST_DATASET;

        } else { // specific to a partition and group
          ds = new InvDatasetImpl(this, getBestDatasetName() +" for "+ partitionName+" and "+groupId);
          dpath = this.path + "/" + partitionName + "/" + groupId + "/" + BEST_DATASET;
        }

        ds.setUrlPath(dpath);
        ds.setID(dpath);
        ds.tmi.addVariableMapLink(makeMetadataLink(dpath, VARIABLES));

        // metadata is specific to each group
        ds.tmi.setGeospatialCoverage(extractGeospatial(group));
        CalendarDateRange cdr = extractCalendarDateRange(group);
        if (cdr != null) ds.tmi.setTimeCoverage(cdr);

        ds.finish();
        container.addDataset(ds);
      }

      if (!isBest && gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Files)) {
        String name = isSingleGroup ? partitionName + "/" + FILES : partitionName + "/" + groupId + "/" + FILES;
        InvCatalogRef filesCat = new InvCatalogRef(this, FILES, getCatalogHref(name));
        filesCat.finish();
        container.addDataset(filesCat);
      }

    }

    partCatalog.finish();
    return partCatalog;
  }

  // this catalog lists the individual files comprising a grib collection.
  // cant use InvDatasetScan because we might have multiple hcs
  private InvCatalogImpl makeCatalogFiles(GribCollection gc, GribCollection.GroupHcs group, URI catURI, State localState, boolean isTimePartition) throws IOException {
    boolean isSingleGroup = gc.getGroups().size() == 1;
    List<String> filenames = isSingleGroup ? gc.getFilenames() : group.getFilenames();

    boolean addLatest = (!isTimePartition && gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.LatestFile));
    return makeCatalogFiles(catURI, localState, filenames, addLatest);
  }

  @Override
  public InvCatalogImpl makeLatest(String matchPath, String reqPath, URI catURI) {
    //logger.debug("{}: make catalog for {} {}", name, match, baseURI);
    StateGrib localState;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    GribCollection gc = localState.timePartition == null ? localState.gribCollection : localState.timePartition;
    List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gc.getGroups());

    /*
     1. files
     2. domain1/files
    */
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;

    try {
      if (localState.timePartition == null) {

        if ((paths.length == 1) && paths[0].equals(FILES)) {
          return makeLatestCatalog(gc, groups.get(0), catURI, localState);  // case 1
        } if ((paths.length == 2) && paths[1].equals(FILES)) {
          return makeLatestCatalog(gc, gc.findGroupById(paths[0]), catURI, localState); // case 2
        }

      } else {

        if ((paths.length == 1) && paths[0].equals(FILES)) {
          TimePartition.Partition p = localState.timePartition.getPartitionLast();
          GribCollection pgc = p.getGribCollection();
          InvCatalogImpl cat = makeLatestCatalog(pgc, groups.get(0), catURI, localState);  // case 1
          pgc.close();
          return cat;

         } if ((paths.length == 2) && paths[1].equals(FILES)) {
           TimePartition.Partition p = localState.timePartition.getPartitionByName(paths[0]);
           GribCollection pgc = p.getGribCollection();
           InvCatalogImpl cat =  makeLatestCatalog(pgc, groups.get(0), catURI, localState);  // case 3
           pgc.close();
           return cat;
         }
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }

    // this catalog lists the individual files comprising a grib collection.
  // cant use InvDatasetScan because we might have multiple hcs
  private InvCatalogImpl makeLatestCatalog(GribCollection gc, GribCollection.GroupHcs group, URI catURI, State localState) throws IOException {

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl result = new InvCatalogImpl(getFullName(), parent.getVersion(), catURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(getLatestFileName());

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.coverage != null) tmi.setGeospatialCoverage(localState.coverage);
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    result.addDataset(top);

    // services need to be local
    // result.addService(InvService.latest);
    result.addService(orgService);
    top.getLocalMetadataInheritable().setServiceName(orgService.getName());

    boolean isSingleGroup = gc.getGroups().size() == 1;
    List<String> filenames = isSingleGroup ? gc.getFilenames() : group.getFilenames();
    String f = filenames.get(filenames.size()-1);
    if (!f.startsWith(topDirectory))
      logger.warn("File {} doesnt start with topDir {}", f, topDirectory);

    String fname = f.substring(topDirectory.length() + 1);
    String path = FILES + "/" + fname;
    //InvDatasetImpl ds = new InvDatasetImpl(this, fname);
    top.setUrlPath(this.path + "/" + path);
    top.setID(this.path + "/" + path);
    top.tmi.addVariableMapLink(makeMetadataLink(this.path + "/" + path, VARIABLES));
    File file = new File(f);
    top.tm.setDataSize(file.length());

    result.finish();
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////

  private ThreddsMetadata.GeospatialCoverage extractGeospatial(List<GribCollection.GroupHcs> groups) {
    ThreddsMetadata.GeospatialCoverage gcAll = null;
    for (GribCollection.GroupHcs group : groups) {
      ThreddsMetadata.GeospatialCoverage gc = extractGeospatial(group);
      if (gcAll == null) gcAll = gc;
      else gcAll.extend(gc);
    }
    return gcAll;
  }


  private ThreddsMetadata.GeospatialCoverage extractGeospatial(GribCollection.GroupHcs group) {
    GdsHorizCoordSys gdsCoordSys = group.hcs;
    LatLonRect llbb = GridCoordSys.getLatLonBoundingBox(gdsCoordSys.proj, gdsCoordSys.getStartX(), gdsCoordSys.getStartY(),
            gdsCoordSys.getEndX(), gdsCoordSys.getEndY());

    ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
    if (llbb != null)
      gc.setBoundingBox(llbb);

    if (group.hcs.isLatLon()) {
      gc.setLonResolution(gdsCoordSys.dx);
      gc.setLatResolution(gdsCoordSys.dy);
    }

    return gc;
  }

  private CalendarDateRange extractCalendarDateRange(List<GribCollection.GroupHcs> groups) {
    CalendarDateRange gcAll = null;
    for (GribCollection.GroupHcs group : groups) {
      CalendarDateRange gc = extractCalendarDateRange(group);
      if (gcAll == null) gcAll = gc;
      else gcAll.extend(gc);
    }
    return gcAll;
  }


  private CalendarDateRange extractCalendarDateRange(GribCollection.GroupHcs group) {
    TimeCoord max = null;

    for (TimeCoord tc : group.timeCoords) {
      if (!tc.isInterval()) {
        if ((max == null) || (max.getSize() < tc.getSize()))
          max = tc;
      }
    }

    if (max == null) {
      for (TimeCoord tc : group.timeCoords) {
        if (tc.isInterval()) {
          if ((max == null) || (max.getSize() < tc.getSize()))
            max = tc;
        }
      }
    }

    return (max == null) ? null : max.getCalendarRange();
  }

  //////////////////////////////////////////////////////////////////////////////

  protected String getBestDatasetName() {
      return bestDatasetName;
  }

  @Override
  public ucar.nc2.dt.GridDataset getGridDataset(String matchPath) throws IOException {
    /* handle FILES
    GridDataset result = super.getGridDataset(matchPath);
    if (result != null) return result; */

    StateGrib localState;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    DatasetParse dp = parse(matchPath, localState);
    if (dp == null) return null;

    if (dp.filename != null) {  // case 7
      File want = new File(topDirectory, dp.filename);
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(null, want.getPath(), null, -1, null, gribConfig.getIospMessage());
      return new ucar.nc2.dt.grid.GridDataset(ncd);
    }

    if (localState.timePartition == null) { // not a time partition
      return localState.gribCollection.getGridDataset(dp.group, dp.filename, gribConfig, logger);

    } else {
      if (dp.partition != null) {   // specific time partition
        GribCollection gc =  dp.partition.getGribCollection();
        GridDataset gd = gc.getGridDataset(dp.group, dp.filename, gribConfig, logger);
        gc.close(); // LOOK WTF ??
        return gd;

      } else {  // entire collection
        return localState.timePartition.getGridDataset(dp.group, dp.filename, gribConfig, logger);
      }
    }

  }

  @Override
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    /* handle FILES
    NetcdfDataset result = super.getNetcdfDataset(matchPath); // case 7
    if (result != null) return result;  */

    StateGrib localState;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    DatasetParse dp = parse(matchPath, localState);
    if (dp == null) return null;

    if (dp.filename != null) {  // case 7
      File want = new File(topDirectory, dp.filename);
      return NetcdfDataset.acquireDataset(null, want.getPath(), null, -1, null, gribConfig.getIospMessage());
    }

    if (localState.timePartition == null) { // not a time partition
      return localState.gribCollection.getNetcdfDataset(dp.group, dp.filename, gribConfig, logger); // case 1 and 2

    } else {
      if (dp.partition != null)  { // specific time partition
        GribCollection gc =  dp.partition.getGribCollection();
        NetcdfDataset gd = gc.getNetcdfDataset(dp.group, dp.filename, gribConfig, logger);
        gc.close();
        return gd;

      } else { // entire collection
        return localState.timePartition.getNetcdfDataset(dp.group, dp.filename, gribConfig, logger); // case 3
      }
    }
  }

  /* possible forms of path:
    regular, single group:
      1. BEST

     regular, multiple group:
      2. groupName/BEST

   time partition, single group:
      3. BEST                   overall collection for all partitions
      4. partitionName/BEST     one partition

   time partition, multiple groups:
      5. groupName/BEST                    overall collection for group
      6. partitionName/groupName/BEST      collection for group and partition

   all:
      7. FILES/filename
      8. allow COLLECTION as alias for BEST
  */
  private DatasetParse parse(String matchPath, StateGrib localState) {
    if ((matchPath == null) || (matchPath.length() == 0)) return null;
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;

    if (paths.length >= 2  && paths[0].equals(FILES))
        return new DatasetParse(matchPath.substring(paths[0].length()));  // case 7

    if (localState.timePartition == null) {
      boolean isBest = paths[0].equals(BEST_DATASET) || paths[0].equals(COLLECTION);
      String groupName = isBest ? localState.gribCollection.getGroup(0).getId() : paths[0];
      return new DatasetParse(null, groupName); // case 1 and 2

    } else { // is a time partition
      List<GribCollection.GroupHcs> groups = localState.timePartition.getGroups();
      boolean isSingleGroup = groups.size() == 1;

      if (paths.length == 1) {
        boolean isBest = paths[0].equals(BEST_DATASET) || paths[0].equals(COLLECTION);
        if (isBest) {
          String groupName = localState.timePartition.getGroup(0).getId();
          return new DatasetParse(null, groupName); // case 3
        }
      }

      if (paths.length == 2) {
        boolean isBest = paths[1].equals(BEST_DATASET) || paths[1].equals(COLLECTION);
        if (isSingleGroup) {
          TimePartition.Partition tpp = localState.timePartition.getPartitionByName(paths[0]);
          if (tpp != null)
            return new DatasetParse(tpp, localState.timePartition.getGroup(0).getId()); // case 4 :  overall collection for partition, one group
        } else {
          return new DatasetParse(null, paths[0]);  // case 5 : overall collection for group, multiple partitions
        }
      }

      if (paths.length == 3) {
         boolean isBest = paths[2].equals(BEST_DATASET) || paths[2].equals(COLLECTION);
         TimePartition.Partition tpp = localState.timePartition.getPartitionByName(paths[0]);
         String groupName = paths[1];
         return new DatasetParse(tpp, groupName); // case 6
      }
    }

    return null;
  }

  private class DatasetParse {
    TimePartition.Partition partition; // missing for collection level
    String group;
    String filename; // only for isFile
    //FeatureCollectionConfig.GribDatasetType dtype;
    //boolean isFile;

    private DatasetParse(TimePartition.Partition tpp, String group) {
      this.partition = tpp;
      this.group = group;
    }

    private DatasetParse(String filename) {
      this.filename = filename;
      //this.isFile = true;
    }

  }
  ///////////////////////////

  private ThreddsMetadata.Variables extractThreddsVariables(GribCollection gribCollection, GribCollection.GroupHcs group) {
    ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables(format.toString());
    for (GribCollection.VariableIndex vindex : group.varIndex)
      vars.addVariable(extractThreddsVariables(gribCollection, vindex));
    vars.sort();
    return vars;
  }

  // we need to cache this by variable to reduce duplication
  private Map<Integer, ThreddsMetadata.Variable> map = null;
  private Grib1Customizer cust1 = null;
  private Grib2Customizer cust2 = null;

  public ThreddsMetadata.Variable extractThreddsVariables(GribCollection gc, GribCollection.VariableIndex vindex) {
    if (map == null) map = new HashMap<Integer, ThreddsMetadata.Variable>(100);

    ThreddsMetadata.Variable tv = map.get(vindex.cdmHash);
    if (tv != null) return tv;
    tv = new ThreddsMetadata.Variable();

    if (gc.isGrib1()) {
      if (cust1 == null) cust1 = Grib1Customizer.factory(gc.getCenter(), gc.getSubcenter(), gc.getLocal(), null);
      tv.setName(cust1.makeVariableName(gc, vindex));
      tv.setDescription(Grib1Iosp.makeVariableLongName(cust1, gc, vindex));
      tv.setUnits(Grib1Iosp.makeVariableUnits(cust1, gc, vindex));
      tv.setVocabularyId("1-" + vindex.discipline + "-" + vindex.category + "-" + vindex.parameter);

      map.put(vindex.cdmHash, tv);
      return tv;

    } else {
      if (cust2 == null)
        cust2 = Grib2Customizer.factory(gc.getCenter(), gc.getSubcenter(), gc.getMaster(), gc.getLocal());

      tv.setName(Grib2Iosp.makeVariableName(cust2, gc, vindex));
      tv.setDescription(Grib2Iosp.makeVariableLongName(cust2, vindex));
      tv.setUnits(Grib2Iosp.makeVariableUnits(cust2, vindex));
      tv.setVocabularyId("2-" + vindex.discipline + "-" + vindex.category + "-" + vindex.parameter);

      String paramDisc = cust2.getTableValue("0.0", vindex.discipline);
      if (paramDisc == null) paramDisc = "Unknown";
      String paramCategory = cust2.getCategory(vindex.discipline, vindex.category);
      if (paramCategory == null) paramCategory = "Unknown";
      String paramName = cust2.getVariableName(vindex.discipline, vindex.category, vindex.parameter);
      tv.setVocabularyName(paramDisc + " / " + paramCategory + " / " + paramName);
      map.put(vindex.cdmHash, tv);
      return tv;
    }
  }

}
