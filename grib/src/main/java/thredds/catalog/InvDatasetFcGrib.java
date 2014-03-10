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
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.collection.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Implement FeatureCollection GRIB - a collection of Grib1 or Grib2 files that are served as Grids.
 *
 * catalogs
 *  path/catalog.xml                       // top catalog
 *  path/partitionName/catalog.xml
 *  path/partitionName/../partitionName/catalog.xml
 *  path/latest.xml                       // latest (resolver)
 *
 * datasets
 *  path/dataset (BEST, TWOD, GC)                // top collection, single group
 *  path/dataset/groupName                       // top collection, multiple group
 *  path/partitionName/dataset                   // partition, single group
 *  path/partitionName/../partitionName/dataset
 *  path/partitionName/dataset/groupName         // partition, multiple group
 *  path/partitionName/../partitionName/dataset/groupName
 *
 * @author caron
 * @since 4/15/11
 */
@ThreadSafe
public class InvDatasetFcGrib extends InvDatasetFeatureCollection {
  static private final String COLLECTION = "collection";

  static private final String GC_DATASET = GribCollection.Type.GC.toString();
  static private final String BEST_DATASET = GribCollection.Type.Best.toString();
  static private final String TWOD_DATASET = GribCollection.Type.TwoD.toString();

  /////////////////////////////////////////////////////////////////////////////
  protected class StateGrib extends State {
    GribCollection gribCollection;
    GribCollection latest;
    String latestPath;

    protected StateGrib(StateGrib from) {
      super(from);
      if (from != null) {
        this.gribCollection = from.gribCollection;
        this.latest = from.latest;
        this.latestPath = from.latestPath;
      }
    }

    @Override
    public State copy() {
      return new StateGrib(this);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  private final FeatureCollectionConfig config;

  public InvDatasetFcGrib(InvDatasetImpl parent, String name, String path, FeatureCollectionType fcType, FeatureCollectionConfig config) {
    super(parent, name, path, fcType, config);
    this.config = config;

    Formatter errlog = new Formatter();
    CollectionSpecParser sp = new CollectionSpecParser(config.spec, errlog);
    topDirectory = sp.getRootDir();

    String errs = errlog.toString();
    if (errs.length() > 0) logger.warn("{}: CollectionManager parse error = {} ", name, errs);

    tmi.setDataType(FeatureType.GRID); // override GRIB

    state = new StateGrib(null);
    finish();
  }

  @Override
  public FeatureDataset getFeatureDataset() {
    return null;
  }

  @Override
  protected void firstInit() {
    super.firstInit();
  }

  @Override
  public void updateProto() {
    // needsProto.set(true);
    // no actual work, wait until next call to updateCollection (??)
    // not sure proto is used in GribFc
  }

  @Override
  protected void updateCollection(State state, CollectionUpdateType force) {
    try {
      StateGrib localState = (StateGrib) state;
      GribCollection previous = localState.gribCollection;
      GribCollection previousLatest = localState.latest;

      localState.gribCollection = GribCdmIndex.openGribCollection(this.config, force, logger);
      logger.debug("{}: GribCollection object was recreated", getName());
      if (previous != null) previous.close();                 // LOOK may be another thread using - other thread will fail
      if (previousLatest != previousLatest) previous.close();

    } catch (IOException ioe) {
      logger.error("GribFc updateCollection", ioe);
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private InvCatalogImpl makeCatalogFromCollection(URI catURI, String parentName, GribCollection gc) throws IOException { // }, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl result = new InvCatalogImpl(gc.getName(), parentCatalog.getVersion(), catURI);  // LOOK is catURL right ??

    if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Latest))
      result.addService(InvService.latest);
    result.addDataset(makeDatasetFromCollection(parentName, gc));
    result.addService(virtualService);
    result.finish();

    return result;
  }

  private InvDatasetImpl makeDatasetFromCollection( String parentName, GribCollection gc) {
    // StateGrib localState = (StateGrib) state;

    InvDatasetImpl result = new InvDatasetImpl(this);
    result.setName(gc.getName());
    result.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      result.transferMetadata(parent, true); // make all inherited metadata local

    String tpath = getPath()+"/"+COLLECTION;
    ThreddsMetadata tmi = result.getLocalMetadataInheritable();
    tmi.addVariableMapLink(makeMetadataLink(tpath, VARIABLES));
    tmi.setServiceName(virtualService.getName());

    String pathStart = parentName == null ? getPath() :  getPath() + "/"+parentName;

    for (GribCollection.Dataset ds : gc.getDatasets()) {
      boolean isSingleGroup = ds.getGroupsSize() == 1;

      if (ds.getType() == GribCollection.Type.TwoD) {
        Iterable<GribCollection.GroupGC> groups = ds.getGroups();
        tmi.setGeospatialCoverage(extractGeospatial(groups)); // set extent from twoD dataset for all

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.TwoD)) {
          InvDatasetImpl twoD = new InvDatasetImpl(this, getDatasetNameTwoD());
          String path = pathStart + "/" + TWOD_DATASET;
          twoD.setUrlPath(path);
          //twoD.setID(path);
          twoD.tmi.addDocumentation("summary", "Two time dimensions: reference and forecast; full access to all GRIB records");
          twoD.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
          twoD.tmi.setTimeCoverage(extractCalendarDateRange(groups));
          result.addDataset(twoD);

          // Collections.sort(groups);
          makeDatasetsFromGroups(twoD, groups, isSingleGroup);
        }
      }

      if (ds.getType() == GribCollection.Type.Best) {

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Best)) {
          Iterable<GribCollection.GroupGC> groups = ds.getGroups();
          InvDatasetImpl best = new InvDatasetImpl(this, getDatasetNameBest(gc.getName()));
          String path = pathStart + "/" + BEST_DATASET;
          best.setUrlPath(path);
          best.tmi.addDocumentation("summary", "Single time dimension: for each forecast time, use GRIB record with smallest offset from reference time");
          best.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
          best.tmi.setTimeCoverage(extractCalendarDateRange(groups));
          result.addDataset(best);

          // Collections.sort(groups);
          makeDatasetsFromGroups(best, groups, isSingleGroup);
        }
      }

      if (ds.getType() == GribCollection.Type.GC) {

        //InvDatasetImpl gcds = new InvDatasetImpl(this, getDatasetNameGC());
        String path = pathStart + "/" + GC_DATASET;
        result.setUrlPath(path);

        Iterable<GribCollection.GroupGC> groups = ds.getGroups();
        result.tmi.addDocumentation("summary", "Single reference time Grib Collection");
        result.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
        result.tmi.setTimeCoverage(extractCalendarDateRange(groups));

        makeDatasetsFromGroups(result, groups, isSingleGroup);
      }

    }

    if (gc instanceof PartitionCollection) {
      if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Latest) && parentName == null) {  // latest for top only
        InvDatasetImpl ds = new InvDatasetImpl(this, getDatasetNameLatest(gc.getName()));
        ds.setUrlPath(LATEST_DATASET_CATALOG);
        // ds.setID(getPath() + "/" + FILES + "/" + LATEST_DATASET_CATALOG);
        ds.setServiceName(LATEST_SERVICE);
        ds.finish();
        result.addDataset(ds);
        //this.addService(InvService.latest);
      }

      PartitionCollection pc =  (PartitionCollection) gc;
      for (PartitionCollection.Partition partition : pc.getPartitionsSorted()) {
        InvDatasetImpl partDs = makeDatasetFromPartition(this, parentName, partition, true);
        result.addDataset(partDs);
      }
    }

    result.finish();
    return result;
  }

  private void makeDatasetsFromGroups(InvDatasetImpl parent, Iterable<GribCollection.GroupGC> groups,  boolean isSingleGroup) {
    // Collections.sort(groups);
    // boolean isSingleGroup = (groups.size() == 1);

    for (GribCollection.GroupGC group : groups) {
      InvDatasetImpl ds = isSingleGroup ? parent : new InvDatasetImpl(this, group.getDescription());

      String groupId = isSingleGroup ? null : group.getId();
      String dpath =  isSingleGroup ? parent.getUrlPath() : parent.getUrlPath() + "/" + groupId;
      ds.setID(dpath);
      ds.setUrlPath(dpath);

      if (!isSingleGroup) {
        parent.addDataset(ds);
      }

      ds.tmi.setGeospatialCoverage(extractGeospatial(group));
      ds.tmi.setTimeCoverage(group.getCalendarDateRange());

     /* if (gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.TwoD)) {
        InvDatasetImpl twoD = new InvDatasetImpl(this, getTwodDatasetName());
        String path = dpath + "/" + TWOD_DATASET;
        twoD.setUrlPath(path);
        twoD.setID(path);
        twoD.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
        ds.addDataset(twoD);
      }

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
      }  */

    }

  }

  private InvDatasetImpl makeDatasetFromPartition(InvDatasetImpl parent, String parentName, PartitionCollection.Partition partition, boolean isSingleGroup) {
    InvDatasetImpl result;
    String dname = partition.getName();

    /* if (isSingleGroup) {
      result = new InvDatasetImpl(parent, dname);

    } else {
      result = new InvCatalogRef(parent, dname, getCatalogHref(dname));
    } */

    String startPath = parentName == null ? getPath() : getPath() + "/"+ parentName;
    String dpath = startPath + "/"+ dname;
    result = new InvCatalogRef(parent, dname, buildCatalogServiceHref(dpath));
    result.setID(dpath);
    result.setUrlPath(dpath);
    result.setServiceName(orgService.getName());

    //result.tmi.setGeospatialCoverage(extractGeospatial(group));
    //result.tmi.setTimeCoverage(group.getCalendarDateRange());
    return result;
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
  /*
     Possible catref paths:
      0. path/catalog.xml                       // top catalog
      1. path/partitionName/catalog.xml
      2. path/partitionName/../partitionName/catalog.xml

      ////
      1. path/files/catalog.xml
      2. path/partitionName/files/catalog.xml
      3. path/groupName/files/catalog.xml

      4. path/TWOD/catalog.xml
      4. path/BEST/catalog.xml
      5. path/partitionName/catalog.xml
   */

    /* possible forms of dataset path:

    regular, single group:
      1. dataset (BEST, TWOD, GC)

     regular, multiple group:
      2. dataset/groupName

     partition, single group:
      3. partitionName/dataset
      3. partitionName/../partitionName/dataset

     partition, multiple group:
      4. partitionName/dataset/groupName
      4. partitionName/../partitionName/dataset/groupName
  */
  @Override
  public InvCatalogImpl makeCatalog(String match, String reqPath, URI catURI) {
    StateGrib localState = (StateGrib) checkState();
    if (localState == null) return null; // not ready yet I think

    try {

      // case 0
      if ((match == null) || (match.length() == 0)) {
        return makeCatalogTop(catURI, localState);  // top catalog : uses state.top previously made in checkState()
      }

      // case 1
      if (localState.gribCollection instanceof PartitionCollection) {
        String[] paths = match.split("/");
        PartitionCollection pc = (PartitionCollection) localState.gribCollection;
        return drillPartitionCatalog(pc, paths, 0, catURI);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }
  private InvCatalogImpl drillPartitionCatalog(PartitionCollection pc, String[] paths, int idx, URI catURI) throws IOException {
    if (paths.length < idx+1) return null;
    PartitionCollection.Partition pcp = pc.getPartitionByName(paths[idx]);
    if (pcp == null) return null;

    try (GribCollection gc =  pcp.getGribCollection()) {
      if (paths.length > idx+1 && gc instanceof PartitionCollection) {
        PartitionCollection pcNested = (PartitionCollection) gc;
        PartitionCollection.Partition pcpNested = pcNested.getPartitionByName(paths[idx+1]);
        if (pcpNested != null)  // recurse
          return drillPartitionCatalog(pcNested, paths, idx+1, catURI);
      }

      // build the parent name
      int i = 0;
      StringBuilder parentName = new StringBuilder();
      for (String s : paths) {
        if (i++ > 0) parentName.append("/");
        parentName.append(s);
      }
      return makeCatalogFromCollection(catURI, parentName.toString(), gc);

    }
  }

      /*  if (isGribCollection || isFilePartition) {
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
          return makeCatalogPartition(localState.gribCollection, catURI, localState);
        }

        PartitionCollection pc =  (PartitionCollection) localState.gribCollection;
        PartitionCollection.Partition tpp = pc.getPartitionByName(match);
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
 /*       tpp = pc.getPartitionByName(path[0]);
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
      }   */

  @Override
  protected InvCatalogImpl makeCatalogTop(URI catURI, State localState) throws IOException, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), catURI);

    if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Latest))
      mainCatalog.addService(InvService.latest);
    mainCatalog.addDataset(((StateGrib) localState).top);
    mainCatalog.addService(virtualService);
    mainCatalog.finish();

    return mainCatalog;
  }


  @Override
  protected void makeDatasetTop(State state) {
    StateGrib localState = (StateGrib) state;
    localState.top = makeDatasetFromCollection(null, localState.gribCollection);
  }

  // each partition gets its own catalog, showing the different groups (horiz coord sys)
  /* private InvCatalogImpl makeCatalogPartition(GribCollection gribCollection, URI catURI, State localState) throws IOException {

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
    boolean isBest = gribCollection instanceof PartitionCollection;

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
        CalendarDateRange cdr = group.getCalendarDateRange();
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
  }  */

  // this catalog lists the individual files comprising a grib collection.
  // cant use InvDatasetScan because we might have multiple hcs
 /* private InvCatalogImpl makeCatalogFiles(GribCollection gc, GribCollection.GroupHcs group, URI catURI, State localState, boolean isTimePartition) throws IOException {
    boolean isSingleGroup = gc.getGroups().size() == 1;
    List<String> filenames = isSingleGroup ? gc.getFilenames() : group.getFilenames();

    boolean addLatest = (!isTimePartition && gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.LatestFile));
    return makeCatalogFiles(catURI, localState, filenames, addLatest);
  } */

  // path/latest.xml
  @Override
  public InvCatalogImpl makeLatest(String matchPath, String reqPath, URI catURI) throws IOException {
    StateGrib localState = (StateGrib) checkState();
    if (!(localState.gribCollection instanceof PartitionCollection)) return null;

    PartitionCollection pc = (PartitionCollection) localState.gribCollection;
    if (localState.latest == null) {
      List<String> paths = new ArrayList<>();
      GribCollection latest = pc.getLatestGribCollection(paths);
      if (latest == null) return null;

      // make pathname
      Formatter f = new Formatter();
      int count = 0;
      for (String p : paths) {
        if (count++ > 0) f.format("/");
        f.format("%s",p);
      }

      synchronized (lock) {
        localState.latest = latest;
        localState.latestPath = f.toString();
      }
    }

    try {
      return makeCatalogFromCollection(catURI, localState.latestPath, localState.latest);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  /*
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
  }     */

  ///////////////////////////////////////////////////////////////////////////

  private ThreddsMetadata.GeospatialCoverage extractGeospatial(Iterable<GribCollection.GroupGC> groups) {
    ThreddsMetadata.GeospatialCoverage gcAll = null;
    for (GribCollection.GroupGC group : groups) {
      ThreddsMetadata.GeospatialCoverage gc = extractGeospatial(group);
      if (gcAll == null) gcAll = gc;
      else gcAll.extend(gc);
    }
    return gcAll;
  }


  private ThreddsMetadata.GeospatialCoverage extractGeospatial(GribCollection.GroupGC group) {
    GdsHorizCoordSys gdsCoordSys = group.getGdsHorizCoordSys();
    LatLonRect llbb = GridCoordSys.getLatLonBoundingBox(gdsCoordSys.proj, gdsCoordSys.getStartX(), gdsCoordSys.getStartY(),
            gdsCoordSys.getEndX(), gdsCoordSys.getEndY());

    ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
    if (llbb != null)
      gc.setBoundingBox(llbb);

    if (gdsCoordSys.isLatLon()) {
      gc.setLonResolution(gdsCoordSys.dx);
      gc.setLatResolution(gdsCoordSys.dy);
    }

    return gc;
  }

  private CalendarDateRange extractCalendarDateRange(Iterable<GribCollection.GroupGC> groups) {
    CalendarDateRange gcAll = null;
    for (GribCollection.GroupGC group : groups) {
      CalendarDateRange gc = group.getCalendarDateRange();
      if (gcAll == null) gcAll = gc;
      else gcAll.extend(gc);
    }
    return gcAll;
  }


  //////////////////////////////////////////////////////////////////////////////

  protected String getDatasetNameLatest(String gcName) {
    if (config.gribConfig != null && config.gribConfig.latestNamer != null) {
      return config.gribConfig.latestNamer;
    } else {
      return "Latest Reference Time Collection for "+gcName;
    }
  }

  protected String getDatasetNameBest(String gcName) {
    if (config.gribConfig != null && config.gribConfig.bestNamer != null) {
      return config.gribConfig.bestNamer;
    } else {
      return "Best "+gcName +" Time Series";
    }
  }

  protected String getDatasetNameTwoD() {
    return "Full Collection (Reference / Forecast Time) Dataset";
  }

  protected String getDatasetNameGC() {
    return "Grib Collection";
  }

  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(String matchPath) throws IOException {
    StateGrib localState = (StateGrib) checkState();

    DatasetParse dp = parse(matchPath, localState);
    if (dp == null) return null;

    if (dp.filename != null) {  // case 7
      File want = new File(topDirectory, dp.filename);
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(null, want.getPath(), null, -1, null, config.gribConfig.getIospMessage());
      return new ucar.nc2.dt.grid.GridDataset(ncd);
    }

    if (dp.partition != null) {   // specific time partition
      GribCollection gc =  dp.partition.getGribCollection();
      GridDataset gd = gc.getGridDataset(dp.ds, dp.group, dp.filename, config, null, logger);
      gc.close(); // LOOK WTF ??
      return gd;
    }

    return localState.gribCollection.getGridDataset(dp.ds, dp.group, dp.filename, config, null, logger);
  }

  @Override
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    StateGrib localState = (StateGrib) checkState();

    DatasetParse dp = parse(matchPath, localState);
    if (dp == null) return null;

    if (dp.filename != null) {  // case 7
      File want = new File(topDirectory, dp.filename);
      return NetcdfDataset.acquireDataset(null, want.getPath(), null, -1, null, config.gribConfig.getIospMessage());
    }

    if (dp.partition != null)  { // specific time partition
      GribCollection gc =  dp.partition.getGribCollection();
      NetcdfDataset gd = gc.getNetcdfDataset(dp.ds, dp.group, dp.filename, config, null, logger);
      gc.close();
      return gd;
    }

    return localState.gribCollection.getNetcdfDataset(dp.ds, dp.group, dp.filename, config, null, logger);
  }

  /* possible forms of dataset path:

    regular, single group:
      1. dataset (BEST, TWOD, GC)

     regular, multiple group:
      2. dataset/groupName

     partition, single group:
      3. partitionName/dataset
      3. partitionName/../partitionName/dataset

     partition, multiple group:
      4. partitionName/dataset/groupName
      4. partitionName/../partitionName/dataset/groupName
  */
  private DatasetParse parse(String matchPath, StateGrib localState) throws IOException {
    if ((matchPath == null) || (matchPath.length() == 0)) return null;
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;

    GribCollection.Dataset ds = localState.gribCollection.findDataset(paths[0]);
    if (ds != null) {
      boolean isSingleGroup = ds.getGroupsSize() == 1;
      if (paths.length == 1) {                               // case 1
        if (!isSingleGroup) return null;
        GribCollection.GroupGC g = ds.getGroup(0);
        return new DatasetParse(null, localState.gribCollection, ds, g);
      }

      if (paths.length == 2) {                              // case 2
        String groupName = paths[1];
        GribCollection.GroupGC g = ds.findGroupById(groupName);
        if (g != null)
          return new DatasetParse(null, localState.gribCollection, ds, g);
        else
          return null;
      }
    }

    if (paths.length < 2) return null;
    if (!(localState.gribCollection instanceof PartitionCollection)) return null;

    PartitionCollection pc = (PartitionCollection) localState.gribCollection;
    return drill(pc, paths, 0);                              // case 3 and 4
  }

  private DatasetParse drill(PartitionCollection pc, String[] paths, int idx) throws IOException {
    if (paths.length <= idx+1) return null;
    PartitionCollection.Partition pcp = pc.getPartitionByName(paths[idx]);
    if (pcp == null) return null;

    try (GribCollection gc =  pcp.getGribCollection()) {
      if (gc instanceof PartitionCollection) {
        PartitionCollection pcNested = (PartitionCollection) gc;
        PartitionCollection.Partition pcpNested = pcNested.getPartitionByName(paths[idx+1]);
        if (pcpNested != null)  // recurse
          return drill(pcNested, paths, idx+1);
      }

      String datasetName = paths[idx+1];
      GribCollection.Dataset ds = gc.findDataset(datasetName);
      if (ds == null) return null;                         // case 3        // case 4
      GribCollection.GroupGC g = (paths.length <= idx+2) ? ds.getGroup(0) : ds.findGroupById(paths[idx+2]);
      if (g == null) return null;
      return new DatasetParse(pcp, gc, ds, g);
    }
  }

  private class DatasetParse {
    final PartitionCollection.Partition partition; // missing for collection level
    final GribCollection gc;
    final GribCollection.Dataset ds;
    final GribCollection.GroupGC group;
    final String filename; // only for isFile

    private DatasetParse(PartitionCollection.Partition tpp, GribCollection gc, GribCollection.Dataset ds, GribCollection.GroupGC group) {
      this.partition = tpp;
      this.gc = gc;
      this.ds = ds;
      this.group = group;
      this.filename = null;
    }

   /*  private DatasetParse(String filename) {
      this.partition = null;
      this.gc = null;
      this.ds = null;
      this.group = null;
      this.filename = filename;
    }  */

  }

}
