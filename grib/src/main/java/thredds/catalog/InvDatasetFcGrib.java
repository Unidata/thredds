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

  public InvDatasetFcGrib(InvDatasetImpl parent, FeatureCollectionConfig config) {
    super(parent, config);

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

  private String makeCollectionShortName(String collectionName) {
    String topCollectionName = config.collectionName;
    if (collectionName.equals(topCollectionName)) return topCollectionName;
    if (collectionName.startsWith(topCollectionName)) {
      return getName()+collectionName.substring(topCollectionName.length());
    }
    return collectionName;
  }

  private InvDatasetImpl makeDatasetFromCollection( boolean isTop, String parentCollectionName, GribCollectionImmutable fromGc) throws IOException {
    // StateGrib localState = (StateGrib) state;

    String dsName = isTop ? getName() : makeCollectionShortName(fromGc.getName());
    InvDatasetImpl result = new InvDatasetImpl(this);
    result.setName(dsName);
    result.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      result.transferMetadata(parent, true); // make all inherited metadata local

    String tpath = getPath()+"/"+COLLECTION;
    ThreddsMetadata tmi = result.getLocalMetadataInheritable();
    tmi.addVariableMapLink(makeMetadataLink(tpath, VARIABLES));
    tmi.setServiceName(Virtual_Services);
    tmi.setDataFormatType(fromGc.isGrib1 ? DataFormatType.GRIB1 : DataFormatType.GRIB2);
    tmi.addProperties(fromGc.getGlobalAttributes());

    String pathStart = parentCollectionName == null ? getPath() :  getPath()+"/"+parentCollectionName;

    for (GribCollectionImmutable.Dataset ds : fromGc.getDatasets()) {
      boolean isSingleGroup = ds.getGroupsSize() == 1;
      Iterable<GribCollectionImmutable.GroupGC> groups = ds.getGroups();

      if (ds.getType() == GribCollectionImmutable.Type.TwoD) {

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.TwoD)) {
          InvDatasetImpl twoD = new InvDatasetImpl(this, getDatasetNameTwoD(result.getName()));
          String path = pathStart + "/" + TWOD_DATASET;
          twoD.setUrlPath(path);
          twoD.tm.addDocumentation("summary", "Two time dimensions: reference and forecast; full access to all GRIB records");
          result.addDataset(twoD);

          // Collections.sort(groups);
          makeDatasetsFromGroups(twoD, groups, isSingleGroup);
        }

      } else if (ds.getType() == GribCollectionImmutable.Type.Best) {

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Best)) {
          InvDatasetImpl best = new InvDatasetImpl(this, getDatasetNameBest(result.getName()));
          String path = pathStart + "/" + BEST_DATASET;
          best.setUrlPath(path);
          best.tm.addDocumentation("summary", "Single time dimension: for each forecast time, use GRIB record with smallest offset from reference time");
          result.addDataset(best);

          makeDatasetsFromGroups(best, groups, isSingleGroup);
        }

      } else {
        tmi.setServiceName(Virtual_Services);
        result.setUrlPath(pathStart);

        if (ds.getType() == GribCollectionImmutable.Type.SRC) {
          CoordinateRuntime runCoord = fromGc.getMasterRuntime();
          assert runCoord.getSize() == 1;
          CalendarDate runtime = runCoord.getFirstDate();
          result.tm.addDocumentation("summary", "Single reference time Grib Collection");
          result.tmi.addDocumentation("Reference Time", runtime.toString());

        } else if (ds.getType() == GribCollectionImmutable.Type.TP) {
          result.tm.addDocumentation("summary", "Multiple reference, single time Grib Partition");

        } else if (ds.getType() == GribCollectionImmutable.Type.MRSTC) {
          result.tm.addDocumentation("summary", "Multiple reference time Grib Collection");
        }

        makeDatasetsFromGroups(result, groups, isSingleGroup);
      }

    }

    if (fromGc instanceof PartitionCollectionImmutable) {
      if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Latest) && parentCollectionName == null) {  // latest for top only
        InvDatasetImpl ds = new InvDatasetImpl(this, getDatasetNameLatest(result.getName()));
        ds.setUrlPath(LATEST_DATASET_CATALOG);
        // ds.setID(getPath() + "/" + FILES + "/" + LATEST_DATASET_CATALOG);
        ds.setServiceName(LATEST_SERVICE);
        ds.finish();
        result.addDataset(ds);
        //this.addService(InvService.latest);
      }

      PartitionCollectionImmutable pc =  (PartitionCollectionImmutable) fromGc;
      for (PartitionCollectionImmutable.Partition partition : pc.getPartitionsSorted()) {
        InvDatasetImpl partDs = makeDatasetFromPartition(this, parentCollectionName, partition, pc.isPartitionOfPartitions());
        result.addDataset(partDs);
      }
    }

    result.finish();
    return result;
  }

  private void makeDatasetsFromGroups(InvDatasetImpl parent, Iterable<GribCollectionImmutable.GroupGC> groups,  boolean isSingleGroup) {

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
      ds.tmi.addVariableMapLink(makeMetadataLink(dpath, VARIABLES));

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

  private InvDatasetImpl makeDatasetFromPartition(InvDatasetImpl parent, String parentCollectionName, PartitionCollectionImmutable.Partition partition, boolean isPofP) throws IOException {
    InvDatasetImpl result;
    String dsName = makeCollectionShortName(partition.getName());

    // if (isPofP) { // make a catRef
      String startPath = parentCollectionName == null ? getPath() : getPath() + "/" + parentCollectionName;
      String dpath = startPath + "/" + partition.getName();
      result = new InvCatalogRef(parent, dsName, buildCatalogServiceHref(dpath)); // LOOK could be plain if not PoP ??
      result.setID(dpath);
      result.setUrlPath(dpath);
      result.setServiceName(Virtual_Services);

    /* } else {   // make a InvDatasetImpl
      try (GribCollectionImmutable gc = partition.getGribCollection()) {
        result = makeDatasetFromCollection(false, parentCollectionName, gc);
      }
    }   */

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
  public InvCatalogImpl makeCatalog(String match, String reqPath, URI catURI) throws IOException {
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
        return makeCatalogFromPartition(pc, paths, 0, catURI);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + configPath, e);
    }

    return null;
  }

  private InvCatalogImpl makeCatalogFromPartition(PartitionCollectionImmutable pc, String[] paths, int idx, URI catURI) throws IOException {
    if (paths.length < idx+1) return null;
    PartitionCollectionImmutable.Partition pcp = pc.getPartitionByName(paths[idx]);
    if (pcp == null) return null;

    try (GribCollectionImmutable gc =  pcp.getGribCollection()) {
      if (paths.length > idx+1 && gc instanceof PartitionCollectionImmutable) {
        PartitionCollectionImmutable pcNested = (PartitionCollectionImmutable) gc;
        PartitionCollectionImmutable.Partition pcpNested = pcNested.getPartitionByName(paths[idx+1]);
        if (pcpNested != null)  // recurse
          return makeCatalogFromPartition(pcNested, paths, idx + 1, catURI);
      }

      // build the parent name
      int i = 0;
      StringBuilder parentName = new StringBuilder();
      for (String s : paths) {
        if (i++ > 0) parentName.append("/");
        parentName.append(s);
      }
      return makeCatalogFromCollection(gc, parentName.toString(), catURI);
    }
  }

  private InvCatalogImpl makeCatalogFromCollection(GribCollectionImmutable fromGc, String parentCollectionName, URI catURI) throws IOException { // }, URISyntaxException {
    InvCatalogImpl result = new InvCatalogImpl(makeCollectionShortName(fromGc.getName()), getParentCatalog().getVersion(), catURI);  // LOOK is catURL right ??
    // result.addService(orgService);
    result.addService(virtualService);

    InvDatasetImpl ds = makeDatasetFromCollection(false, parentCollectionName, fromGc);
    result.addDataset(ds);
    // String serviceName = ds.getServiceName(); LAME - cant do this way, needs service already added -fix in cat2; YEAH right, cat2
    result.finish();

    return result;
  }

  /////////////////////////////////////


  @Override
  protected void makeDatasetTop(State state) throws IOException {
    StateGrib localState = (StateGrib) state;
    localState.top = makeDatasetFromCollection(true, null, localState.gribCollection);
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

    return makeCatalogFromCollection(localState.latest, localState.latestPath, catURI);
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

  protected String getDatasetNameLatest(String dsName) {
    if (config.gribConfig != null && config.gribConfig.latestNamer != null) {
      return config.gribConfig.latestNamer;
    } else {
      return "Latest Collection for "+dsName;
    }
  }

  protected String getDatasetNameBest(String dsName) {
    if (config.gribConfig != null && config.gribConfig.bestNamer != null) {
      return config.gribConfig.bestNamer;
    } else {
      return "Best "+dsName +" Time Series";
    }
  }

  protected String getDatasetNameTwoD(String dsName) {
    return "Full Collection (Reference / Forecast Time) Dataset";
  }

  @Override
  public File getFile(String remaining) {
    return null;
  }

  // LOOK
  /*  if (null == topDirectory) return null;

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
  }  */


  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(String matchPath) throws IOException {
    StateGrib localState = (StateGrib) checkState();

    return (ucar.nc2.dt.grid.GridDataset) findDataset(matchPath, localState.gribCollection, new DatasetCreator() {
      @Override
      public Object obtain(GribCollectionImmutable gc, GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) throws IOException {
        return gc.getGridDataset(ds, group, null, config, null, logger);
      }
    });
  }

  @Override
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    StateGrib localState = (StateGrib) checkState();

    return (NetcdfDataset) findDataset(matchPath, localState.gribCollection, new DatasetCreator() {
      @Override
      public Object obtain(GribCollectionImmutable gc, GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) throws IOException {
        return gc.getNetcdfDataset(ds, group, null, config, null, logger);
      }
    });
  }

  /*
    possible forms of dataset path:
    [partition/][partition/]dataset[/group]
    dataset = BEST | TWOD | filename
    if group is missing, use first one

    regular, single group:
      1. dataset (BEST, TWOD, filename)

     regular, multiple group:
      2. dataset/group

     partition, single group:
      3. partitionName/dataset
      3. partitionName/../partitionName/dataset

     partition, multiple group:
      4. partitionName/dataset/groupName
      4. partitionName/../partitionName/dataset/group
  */

  private Object findDataset(String matchPath, GribCollectionImmutable topCollection, DatasetCreator visit) throws IOException {
    if ((matchPath == null) || (matchPath.length() == 0)) return null;
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;
    List<String> pathList = Arrays.asList(paths);  // DO ew need to strip off a GC in case of bookmarked URLs?

    DatasetAndGroup dg = findDatasetAndGroup(pathList, topCollection);
    if (dg != null)
      return visit.obtain(topCollection, dg.ds, dg.group);  //  case 1 and 2

    if (!(topCollection instanceof PartitionCollectionImmutable)) return null;
    PartitionCollectionImmutable pc = (PartitionCollectionImmutable) topCollection;
    return findDatasetPartition(visit, pc, pathList);    // case 3 and 4
  }

  private class DatasetAndGroup {
    GribCollectionImmutable.Dataset ds;
    GribCollectionImmutable.GroupGC group;

    private DatasetAndGroup(GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) {
      this.ds = ds;
      this.group = group;
    }
  }

  private DatasetAndGroup findDatasetAndGroup(List<String> paths, GribCollectionImmutable gc)  {
    if (paths.size() < 1) return null;

    GribCollectionImmutable.Dataset ds = gc.getDatasetByTypeName(paths.get(0));
    if (ds != null) {
      boolean isSingleGroup = ds.getGroupsSize() == 1;
      if (paths.size() == 1) {                               // case 1
        if (!isSingleGroup) return null;
        GribCollectionImmutable.GroupGC g = ds.getGroup(0);
        return new DatasetAndGroup(ds, g);
      }

      if (paths.size() == 2) {                              // case 2
        String groupName = paths.get(1);
        GribCollectionImmutable.GroupGC g = ds.findGroupById(groupName);
        if (g != null)
          return new DatasetAndGroup(ds, g);
        else
          return null;
      }
    }
    return null;
  }

  private Object findDatasetPartition(DatasetCreator visit, PartitionCollectionImmutable outerPartition, List<String> pathList) throws IOException {
    int n = pathList.size();
    if (pathList.size() < 1) return null;
    PartitionCollectionImmutable.Partition pcp = outerPartition.getPartitionByName(pathList.get(0));
    if (pcp == null) {
      DatasetAndGroup dg = findDatasetAndGroup(pathList, outerPartition);
      if (dg != null)
        return visit.obtain(outerPartition, dg.ds, dg.group);
      else
        return null;
    }

    try (GribCollectionImmutable gc =  pcp.getGribCollection()) {
      if (gc instanceof PartitionCollectionImmutable) {
        PartitionCollectionImmutable pcNested = (PartitionCollectionImmutable) gc;
        return findDatasetPartition(visit, pcNested, pathList.subList(1, n));
      }

      DatasetAndGroup dg = findDatasetAndGroup(pathList.subList(1, n), gc);
      if (dg != null)
        return visit.obtain(gc, dg.ds, dg.group);
      else {
        GribCollectionImmutable.Dataset ds = gc.getDataset(0);
        GribCollectionImmutable.GroupGC group = ds.getGroup(0);
        return visit.obtain(gc, ds, group);
      }
    }
  }

  private interface DatasetCreator {  // Visitor pattern
    Object obtain(GribCollectionImmutable gc, GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) throws IOException;
  }

}
