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
import ucar.coord.CoordinateRuntime;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.collection.*;
import ucar.nc2.time.CalendarDate;
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

  static private final String GC_DATASET = GribCollectionImmutable.Type.GC.toString();
  static private final String BEST_DATASET = GribCollectionImmutable.Type.Best.toString();
  static private final String TWOD_DATASET = GribCollectionImmutable.Type.TwoD.toString();

  /////////////////////////////////////////////////////////////////////////////
  protected class StateGrib extends State {
    GribCollectionImmutable gribCollection;   // top level
    GribCollectionImmutable latest;
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
      GribCollectionImmutable previous = localState.gribCollection;
      GribCollectionImmutable previousLatest = localState.latest;

      localState.latest = null; // will get updated next time its asked for
      localState.gribCollection = GribCdmIndex.openGribCollection(this.config, force, logger);
      if (localState.gribCollection == null)
        logger.error("InvDatasetFcGrib.updateCollection failed "+this.config);
      logger.debug("{}: GribCollection object was recreated", getName());
      if (previous != null) previous.close();                 // LOOK may be another thread using - other thread will fail
      if (previousLatest != null) previousLatest.close();

    } catch (IOException ioe) {
      logger.error("GribFc updateCollection", ioe);
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private InvCatalogImpl makeCatalogFromCollection(URI catURI, String parentName, GribCollectionImmutable fromGc) throws IOException { // }, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl result = new InvCatalogImpl(fromGc.getName(), parentCatalog.getVersion(), catURI);  // LOOK is catURL right ??
    // result.addService(orgService);
    result.addService(virtualService);

    InvDatasetImpl ds = makeDatasetFromCollection(parentName, fromGc);
    result.addDataset(ds);
    // String serviceName = ds.getServiceName(); LAME - cant do this way, needs serice already added -fix in cat2
    result.finish();

    return result;
  }

  private InvDatasetImpl makeDatasetFromCollection( String parentName, GribCollectionImmutable fromGc) {
    // StateGrib localState = (StateGrib) state;

    InvDatasetImpl result = new InvDatasetImpl(this);
    result.setName(fromGc.getName());
    result.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      result.transferMetadata(parent, true); // make all inherited metadata local

    String tpath = getPath()+"/"+COLLECTION;
    ThreddsMetadata tmi = result.getLocalMetadataInheritable();
    tmi.addVariableMapLink(makeMetadataLink(tpath, VARIABLES));
    tmi.setServiceName(Virtual_Services);

    String pathStart = parentName == null ? getPath() :  getPath() + "/"+parentName;

    for (GribCollectionImmutable.Dataset ds : fromGc.getDatasets()) {
      boolean isSingleGroup = ds.getGroupsSize() == 1;

      if (ds.getType() == GribCollectionImmutable.Type.TwoD) {
        Iterable<GribCollectionImmutable.GroupGC> groups = ds.getGroups();
        tmi.setGeospatialCoverage(extractGeospatial(groups)); // set extent from twoD dataset for all

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.TwoD)) {
          InvDatasetImpl twoD = new InvDatasetImpl(this, getDatasetNameTwoD());
          String path = pathStart + "/" + TWOD_DATASET;
          twoD.setUrlPath(path);
          //twoD.setID(path);
          twoD.tm.addDocumentation("summary", "Two time dimensions: reference and forecast; full access to all GRIB records");
          twoD.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
          twoD.tmi.setTimeCoverage(extractCalendarDateRange(groups));
          result.addDataset(twoD);

          // Collections.sort(groups);
          makeDatasetsFromGroups(twoD, groups, isSingleGroup);
        }

      } else if (ds.getType() == GribCollectionImmutable.Type.Best) {

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Best)) {
          Iterable<GribCollectionImmutable.GroupGC> groups = ds.getGroups();
          InvDatasetImpl best = new InvDatasetImpl(this, getDatasetNameBest(fromGc.getName()));
          String path = pathStart + "/" + BEST_DATASET;
          best.setUrlPath(path);
          best.tm.addDocumentation("summary", "Single time dimension: for each forecast time, use GRIB record with smallest offset from reference time");
          best.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
          best.tmi.setTimeCoverage(extractCalendarDateRange(groups));
          result.addDataset(best);

          // Collections.sort(groups);
          makeDatasetsFromGroups(best, groups, isSingleGroup);
        }

      } else {
        tmi.setServiceName(Virtual_Services);

        CoordinateRuntime runCoord = fromGc.getMasterRuntime();
        assert runCoord.getSize() == 1;
        CalendarDate runtime = runCoord.getFirstDate();
        String path = pathStart + "/" + GC_DATASET;
        result.setUrlPath(path);

        if (ds.getType() == GribCollectionImmutable.Type.SRC) {
          result.tm.addDocumentation("summary", "Single reference time Grib Collection");
          result.tmi.addDocumentation("Reference Time", runtime.toString());
        } else {
          result.tm.addDocumentation("summary", "Multiple reference time Grib Collection");
        }

        Iterable<GribCollectionImmutable.GroupGC> groups = ds.getGroups();
        result.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
        result.tmi.setTimeCoverage(extractCalendarDateRange(groups));

        makeDatasetsFromGroups(result, groups, isSingleGroup);
      }

    }

    if (fromGc instanceof PartitionCollectionImmutable) {
      if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Latest) && parentName == null) {  // latest for top only
        InvDatasetImpl ds = new InvDatasetImpl(this, getDatasetNameLatest(fromGc.getName()));
        ds.setUrlPath(LATEST_DATASET_CATALOG);
        // ds.setID(getPath() + "/" + FILES + "/" + LATEST_DATASET_CATALOG);
        ds.setServiceName(LATEST_SERVICE);
        ds.finish();
        result.addDataset(ds);
        //this.addService(InvService.latest);
      }

      PartitionCollectionImmutable pc =  (PartitionCollectionImmutable) fromGc;
      for (PartitionCollectionImmutable.Partition partition : pc.getPartitionsSorted()) {
        InvDatasetImpl partDs = makeDatasetFromPartition(this, parentName, partition, true);
        result.addDataset(partDs);
      }
    }

    result.finish();
    return result;
  }

  private void makeDatasetsFromGroups(InvDatasetImpl parent, Iterable<GribCollectionImmutable.GroupGC> groups,  boolean isSingleGroup) {
    // Collections.sort(groups);
    // boolean isSingleGroup = (groups.size() == 1);

    for (GribCollectionImmutable.GroupGC group : groups) {
      InvDatasetImpl ds = isSingleGroup ? parent : new InvDatasetImpl(this, group.getDescription());

      String groupId = isSingleGroup ? null : group.getId();
      String dpath =  isSingleGroup ? parent.getUrlPath() : parent.getUrlPath() + "/" + groupId;
      ds.setID(dpath);
      ds.setUrlPath(dpath);

      if (!isSingleGroup) {
        parent.addDataset(ds);
      }

      ds.tmi.setGeospatialCoverage(extractGeospatial(group));
      ds.tmi.setTimeCoverage(group.makeCalendarDateRange());

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

  private InvDatasetImpl makeDatasetFromPartition(InvDatasetImpl parent, String parentName, PartitionCollectionImmutable.Partition partition, boolean isSingleGroup) {
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
    result.setServiceName(Virtual_Services);

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
      if (localState.gribCollection instanceof PartitionCollectionImmutable) {
        String[] paths = match.split("/");
        PartitionCollectionImmutable pc = (PartitionCollectionImmutable) localState.gribCollection;
        return drillPartitionCatalog(pc, paths, 0, catURI);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }
  private InvCatalogImpl drillPartitionCatalog(PartitionCollectionImmutable pc, String[] paths, int idx, URI catURI) throws IOException {
    if (paths.length < idx+1) return null;
    PartitionCollectionImmutable.Partition pcp = pc.getPartitionByName(paths[idx]);
    if (pcp == null) return null;

    try (GribCollectionImmutable gc =  pcp.getGribCollection()) {
      if (paths.length > idx+1 && gc instanceof PartitionCollectionImmutable) {
        PartitionCollectionImmutable pcNested = (PartitionCollectionImmutable) gc;
        PartitionCollectionImmutable.Partition pcpNested = pcNested.getPartitionByName(paths[idx+1]);
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

  // path/latest.xml
  @Override
  public InvCatalogImpl makeLatest(String matchPath, String reqPath, URI catURI) throws IOException {
    StateGrib localState = (StateGrib) checkState();
    if (!(localState.gribCollection instanceof PartitionCollectionImmutable)) return null;

    PartitionCollectionImmutable pc = (PartitionCollectionImmutable) localState.gribCollection;
    if (localState.latest == null) {
      List<String> paths = new ArrayList<>();
      GribCollectionImmutable latest = pc.getLatestGribCollection(paths);
      if (latest == null) return null;
      latest.close(); // doesnt need to be open

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
      return makeCatalogFromCollection(catURI, localState.latestPath, localState.latest); // LOOK does gc need to be open ??  I think not.
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  ///////////////////////////////////////////////////////////////////////////

  private ThreddsMetadata.GeospatialCoverage extractGeospatial(Iterable<GribCollectionImmutable.GroupGC> groups) {
    ThreddsMetadata.GeospatialCoverage gcAll = null;
    for (GribCollectionImmutable.GroupGC group : groups) {
      ThreddsMetadata.GeospatialCoverage gc = extractGeospatial(group);
      if (gcAll == null) gcAll = gc;
      else gcAll.extend(gc);
    }
    return gcAll;
  }


  private ThreddsMetadata.GeospatialCoverage extractGeospatial(GribCollectionImmutable.GroupGC group) {
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

  private CalendarDateRange extractCalendarDateRange(Iterable<GribCollectionImmutable.GroupGC> groups) {
    CalendarDateRange gcAll = null;
    for (GribCollectionImmutable.GroupGC group : groups) {
      CalendarDateRange gc = group.makeCalendarDateRange();
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

  @Override
  public File getFile(String remaining) {
    if (null == topDirectory) return null;

    StateGrib localState = (StateGrib) checkState();

    DatasetParse dp = null;
    try {
      dp = parse(remaining, localState);
      if (dp == null) return super.getFile(remaining);

      GribCollectionImmutable gc = null;
      boolean isPartitionGc = false;
      try {
        if (dp.partition != null) {   // specific time partition
          gc =  dp.partition.getGribCollection();
          isPartitionGc = true;
        } else {
          gc = localState.gribCollection;
        }
        String first = gc.getFirstFilename();
        if (first == null) return null;
        return new File(first);

      } finally {
        if (isPartitionGc) gc.close(); // leave main gc open
      }

    } catch (IOException e) {
      logger.error("Failed to get file="+ remaining, e);
      return null;
    }
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
      try (GribCollectionImmutable gc =  dp.partition.getGribCollection()) {
        return gc.getGridDataset(dp.ds, dp.group, dp.filename, config, null, logger);
      }
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
      try (GribCollectionImmutable gc =  dp.partition.getGribCollection()) {
        return gc.getNetcdfDataset(dp.ds, dp.group, dp.filename, config, null, logger);
      }
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

    GribCollectionImmutable.Dataset ds = localState.gribCollection.getDataset(paths[0]);
    if (ds != null) {
      boolean isSingleGroup = ds.getGroupsSize() == 1;
      if (paths.length == 1) {                               // case 1
        if (!isSingleGroup) return null;
        GribCollectionImmutable.GroupGC g = ds.getGroup(0);
        return new DatasetParse(null, localState.gribCollection, ds, g);
      }

      if (paths.length == 2) {                              // case 2
        String groupName = paths[1];
        GribCollectionImmutable.GroupGC g = ds.findGroupById(groupName);
        if (g != null)
          return new DatasetParse(null, localState.gribCollection, ds, g);
        else
          return null;
      }
    }

    if (paths.length < 2) return null;
    if (!(localState.gribCollection instanceof PartitionCollectionImmutable)) return null;

    PartitionCollectionImmutable pc = (PartitionCollectionImmutable) localState.gribCollection;
    return drill(pc, paths, 0);                              // case 3 and 4
  }

  private DatasetParse drill(PartitionCollectionImmutable pc, String[] paths, int idx) throws IOException {
    if (paths.length <= idx+1) return null;
    PartitionCollectionImmutable.Partition pcp = pc.getPartitionByName(paths[idx]);
    if (pcp == null) return null;

    try (GribCollectionImmutable gc =  pcp.getGribCollection()) {
      if (gc instanceof PartitionCollectionImmutable) {
        PartitionCollectionImmutable pcNested = (PartitionCollectionImmutable) gc;
        PartitionCollectionImmutable.Partition pcpNested = pcNested.getPartitionByName(paths[idx+1]);
        if (pcpNested != null)  // recurse
          return drill(pcNested, paths, idx+1);
      }

      String datasetName = paths[idx+1];
      GribCollectionImmutable.Dataset ds = gc.getDataset(datasetName);
      if (ds == null) return null;                         // case 3        // case 4
      GribCollectionImmutable.GroupGC g = (paths.length <= idx+2) ? ds.getGroup(0) : ds.findGroupById(paths[idx+2]);
      if (g == null) return null;
      return new DatasetParse(pcp, gc, ds, g);
    }
  }

  private static class DatasetParse {
    final PartitionCollectionImmutable.Partition partition; // missing for collection level
    final GribCollectionImmutable gc;
    final GribCollectionImmutable.Dataset ds;
    final GribCollectionImmutable.GroupGC group;
    final String filename; // only for isFile

    private DatasetParse(PartitionCollectionImmutable.Partition tpp, GribCollectionImmutable gc, GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) {
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
