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

package thredds.featurecollection;

import net.jcip.annotations.ThreadSafe;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.core.StandardServices;
import thredds.inventory.*;
import ucar.coord.CoordinateRuntime;
import ucar.nc2.constants.DataFormatType;
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
 * <p/>
 * catalogs                                see makeCatalog()
 * path/catalog.xml                       // top catalog
 * path/partitionName/catalog.xml
 * path/partitionName/../partitionName/catalog.xml
 * path/latest.xml                       // latest (resolver)
 * <p/>
 * datasets                                see findDataset()
 * path/dataset (dataset = BEST, TWOD, TP, "")  // top collection, single group
 * path/dataset/groupName                       // top collection, multiple group
 * path/partitionName/dataset                   // partition, single group
 * path/partitionName/../partitionName/dataset
 * path/partitionName/dataset/groupName         // partition, multiple group
 * path/partitionName/../partitionName/dataset/groupName
 * <p/>
 * files
 * path/partitionName/../partitionName/FILES/filename
 *
 * @author caron
 * @since 4/15/11
 */
@ThreadSafe
public class InvDatasetFcGrib extends InvDatasetFeatureCollection {
  static private final String COLLECTION = "collection";

  static private final String BEST_DATASET = GribCollectionImmutable.Type.Best.toString();
  static private final String TWOD_DATASET = GribCollectionImmutable.Type.TwoD.toString();
  static private final String TP_DATASET = GribCollectionImmutable.Type.TP.toString();

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

  public InvDatasetFcGrib(Dataset parent, FeatureCollectionConfig config) {
    super(parent, config);

    Formatter errlog = new Formatter();
    CollectionSpecParser sp = config.getCollectionSpecParser(errlog);
    topDirectory = sp.getRootDir();

    String errs = errlog.toString();
    if (errs.length() > 0) logger.warn("{}: CollectionManager parse error = {} ", name, errs);

    state = new StateGrib(null);
  }

  @Override
  public FeatureDataset getFeatureDataset() {
    return null;
  }

  @Override
  protected void _showStatus(Formatter f, boolean summaryOnly, String type) throws IOException {
    StateGrib localState;
    synchronized (lock) {
      localState = (StateGrib) state;
    }
    if (localState.gribCollection != null) {
      if (summaryOnly)
        localState.gribCollection.showStatusSummary(f, type);
      else
        localState.gribCollection.showStatus(f);

    }
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
        logger.error("InvDatasetFcGrib.updateCollection failed " + this.config);

      logger.debug("{}: GribCollection object was recreated", name);
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
      return name + collectionName.substring(topCollectionName.length());
    }
    return collectionName;
  }

  private DatasetBuilder makeDatasetFromCollection(URI catURI, boolean isTop, CatalogBuilder parentCatalog, String parentCollectionName, GribCollectionImmutable fromGc) throws IOException {

    String dsName = isTop ? name : makeCollectionShortName(fromGc.getName());
    DatasetBuilder result = new DatasetBuilder(null);
    result.setName(dsName);
    if (parent != null)
      result.transferMetadata(parent, true); // make all inherited metadata local

    String tpath = getPath() + "/" + COLLECTION;
    ThreddsMetadata tmi = result.getInheritableMetadata();
    tmi.set(Dataset.VariableMapLinkURI, new ThreddsMetadata.UriResolved(makeMetadataLink(tpath, VARIABLES), catURI));
    tmi.set(Dataset.ServiceName, Virtual_Services_Name);
    tmi.set(Dataset.DataFormatType, fromGc.isGrib1 ? DataFormatType.GRIB1.toString() : DataFormatType.GRIB2.toString());
    tmi.set(Dataset.Properties, Property.convertToProperties(fromGc.getGlobalAttributes()));
    tmi.set(Dataset.FeatureType, FeatureType.GRID.toString()); // override GRIB

    String pathStart = parentCollectionName == null ? getPath() : getPath() + "/" + parentCollectionName;

    for (GribCollectionImmutable.Dataset ds : fromGc.getDatasets()) {
      boolean isSingleGroup = ds.getGroupsSize() == 1;
      Iterable<GribCollectionImmutable.GroupGC> groups = ds.getGroups();

      if (ds.getType() == GribCollectionImmutable.Type.TwoD) {

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.TwoD)) {
          DatasetBuilder twoD = new DatasetBuilder(result);
          twoD.setName(getDatasetNameTwoD(result.getName()));
          String path = pathStart + "/" + TWOD_DATASET;
          twoD.put(Dataset.UrlPath, path);
          twoD.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Two time dimensions: reference and forecast; full access to all GRIB records"));
          result.addDataset(twoD);

          // Collections.sort(groups);
          makeDatasetsFromGroups(catURI, twoD, groups, isSingleGroup);
        }

      } else if (ds.getType() == GribCollectionImmutable.Type.Best) {

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Best)) {
          DatasetBuilder best = new DatasetBuilder(result);
          best.setName(getDatasetNameBest(result.getName()));
          String path = pathStart + "/" + BEST_DATASET;
          best.put(Dataset.UrlPath, path);
          best.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Single time dimension: for each forecast time, use GRIB record with smallest offset from reference time"));
          result.addDataset(best);

          makeDatasetsFromGroups(catURI, best, groups, isSingleGroup);
        }

      } else if (ds.getType() == GribCollectionImmutable.Type.TP) {
        DatasetBuilder tp = new DatasetBuilder(result);
        tp.setName(getDatasetNameTP(result.getName()));
        String path = pathStart + "/" + TP_DATASET;
        tp.put(Dataset.UrlPath, path);
        tp.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Multiple reference, single time Grib Partition"));

        makeDatasetsFromGroups(catURI, tp, groups, isSingleGroup);

      } else { // not a partition

        result.put(Dataset.UrlPath, pathStart);

        if (ds.getType() == GribCollectionImmutable.Type.SRC) {
          CoordinateRuntime runCoord = fromGc.getMasterRuntime();
          assert runCoord.getSize() == 1;
          CalendarDate runtime = runCoord.getFirstDate();
          result.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Single reference time Grib Collection"));
          result.addToList(Dataset.Documentation, new Documentation(null, null, null, "Reference Time", runtime.toString()));

        } else if (ds.getType() == GribCollectionImmutable.Type.MRSTC) {
          result.addToList(Dataset.Documentation, new Documentation(null, null, null, "summary", "Multiple reference time Grib Collection"));
        }

        makeDatasetsFromGroups(catURI, result, groups, isSingleGroup);

        if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Files)) {
          String filesPath = pathStart + "/" + FILES;
          CatalogRefBuilder filesRef = new CatalogRefBuilder(result); // LOOK this isnt used
          filesRef.setName(FILES);
          filesRef.setHref(getCatalogHref(filesPath));
          addFileDatasets(parentCatalog, result, fromGc, config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Latest));
        }
      }

    }

    if (fromGc instanceof PartitionCollectionImmutable) {
      if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Latest) && parentCollectionName == null) {  // latest for top only
        DatasetBuilder ds = new DatasetBuilder(result);
        ds.setName(getDatasetNameLatest(result.getName()));
        ds.put(Dataset.UrlPath, LATEST_DATASET_CATALOG);
        ds.put(Dataset.ServiceName, LATEST_SERVICE);
        result.addDataset(ds);
      }

      PartitionCollectionImmutable pc = (PartitionCollectionImmutable) fromGc;
      for (PartitionCollectionImmutable.Partition partition : pc.getPartitionsSorted()) {
        DatasetBuilder partDs = makeDatasetFromPartition(result, parentCollectionName, partition, pc.isPartitionOfPartitions());
        result.addDataset(partDs);
      }
    }

    return result;
  }

  private void makeDatasetsFromGroups(URI catURI, DatasetBuilder parent, Iterable<GribCollectionImmutable.GroupGC> groups, boolean isSingleGroup) {

    for (GribCollectionImmutable.GroupGC group : groups) {
      DatasetBuilder ds;
      String dpath;
      if (isSingleGroup) {
        ds = parent;
        dpath = (String) parent.get(Dataset.UrlPath);
        ds.put(Dataset.Id, dpath);

      } else {
        ds = new DatasetBuilder(parent);
        ds.setName(group.getDescription());

        dpath = parent.get(Dataset.UrlPath) + "/" + group.getId();
        ds.put(Dataset.Id, dpath);
        ds.put(Dataset.UrlPath, dpath);

        // remove the urlPath on the parent if multiple groups;
        // cannot get a dataset with multiple groups in it
        parent.put(Dataset.UrlPath, null);
        parent.addDataset(ds);
      }

      ThreddsMetadata tmi = ds.getInheritableMetadata();
      tmi.set(Dataset.GeospatialCoverage, extractGeospatial(group));
      tmi.set(Dataset.TimeCoverage, group.makeCalendarDateRange());
      tmi.set(Dataset.VariableMapLinkURI, new ThreddsMetadata.UriResolved(makeMetadataLink(dpath, VARIABLES), catURI));
    }
  }

  private DatasetBuilder makeDatasetFromPartition(DatasetBuilder parent, String parentCollectionName, PartitionCollectionImmutable.Partition partition, boolean isPofP) throws IOException {
    String dsName = makeCollectionShortName(partition.getName());

    String startPath = parentCollectionName == null ? getPath() : getPath() + "/" + parentCollectionName;
    String dpath = startPath + "/" + partition.getName();
    CatalogRefBuilder result = new CatalogRefBuilder(parent);
    result.setName(dsName);
    result.setHref(buildCatalogServiceHref(dpath));  // LOOK could be plain if not PoP ??
    result.put(Dataset.Id, dpath);
    result.put(Dataset.UrlPath, dpath);
    result.put(Dataset.ServiceName, Virtual_Services_Name);

    return result;
  }

  /* file datasets of the partition catalog are InvCatalogRef pointing to "FileCatalogs"
  private void addFileDatasets(Dataset parent, String prefix) {
    String name = (prefix == null) ? FILES : prefix + "/" + FILES;
    InvCatalogRef ds = new InvCatalogRef(this, FILES, getCatalogHref(name));
    ds.finish();
    parent.addDataset(ds);
  }  */

  // this catalog lists the individual files comprising the collection.
  protected void addFileDatasets(CatalogBuilder parentCatalog, DatasetBuilder parent, GribCollectionImmutable fromGc, boolean addLatest) throws IOException {

    DatasetBuilder filesParent = new DatasetBuilder(parent);
    filesParent.setName("Raw Files");
    ThreddsMetadata tmi = filesParent.getInheritableMetadata();
    tmi.set(Dataset.ServiceName, Download_Services_Name);
    parent.addDataset(filesParent);
    if (parentCatalog != null)
      parentCatalog.addService(makeDownloadService());

    List<MFile> mfiles = new ArrayList<>(fromGc.getFiles());
    Collections.sort(mfiles);
    for (MFile mfile : mfiles) {
      DatasetBuilder ds = new DatasetBuilder(parent);
      ds.setName(mfile.getName());
      String lpath = parent.get(Dataset.UrlPath) + "/" + FILES + "/" + mfile.getName();  // LOOK wrong
      ds.put(Dataset.UrlPath, lpath);
      ds.put(Dataset.Id, lpath);
      ds.put(Dataset.DataSize, mfile.getLength());
      // ds.tm.setXXXX(mfile.getLastModified());
      filesParent.addDataset(ds);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////


  @Override
  protected CatalogBuilder makeCatalogTop(URI catURI, State localState) throws IOException, URISyntaxException {
    CatalogBuilder result = super.makeCatalogTop(catURI, localState);

    if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.Latest))
      result.addService(StandardServices.latest.getService());

    return result;
  }

  // called by DataRootHandler.makeDynamicCatalog() when a catref is requested
  // see top javadoc for possible URLs
  @Override
  public Catalog makeCatalog(String match, String reqPath, URI catURI) throws IOException {
    StateGrib localState = (StateGrib) checkState();
    if (localState == null) return null; // not ready yet maybe

    try {

      // case 0
      if ((match == null) || (match.length() == 0)) {
        CatalogBuilder catb = makeCatalogTop(catURI, localState);  // top catalog : uses state.top previously made in checkState()
        return catb.makeCatalog();
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

  private Catalog makeCatalogFromPartition(PartitionCollectionImmutable pc, String[] paths, int idx, URI catURI) throws IOException {
    if (paths.length < idx + 1) return null;
    PartitionCollectionImmutable.Partition pcp = pc.getPartitionByName(paths[idx]);
    if (pcp == null) return null;

    try (GribCollectionImmutable gc = pcp.getGribCollection()) {
      if (paths.length > idx + 1 && gc instanceof PartitionCollectionImmutable) {
        PartitionCollectionImmutable pcNested = (PartitionCollectionImmutable) gc;
        PartitionCollectionImmutable.Partition pcpNested = pcNested.getPartitionByName(paths[idx + 1]);
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

  private Catalog makeCatalogFromCollection(GribCollectionImmutable fromGc, String parentCollectionName, URI catURI) throws IOException { // }, URISyntaxException {
    Catalog parentCatalog = parent.getParentCatalog();

    CatalogBuilder result = new CatalogBuilder();
    result.setName(makeCollectionShortName(fromGc.getName()));
    result.setVersion(parentCatalog.getVersion());
    result.setBaseURI(catURI);
    result.addService(virtualService);

    DatasetBuilder ds = makeDatasetFromCollection(catURI, false, result, parentCollectionName, fromGc);
    result.addDataset(ds);

    return result.makeCatalog();
  }

  /////////////////////////////////////


  @Override
  protected void makeDatasetTop(State state) throws IOException {
    StateGrib localState = (StateGrib) state;
    localState.top = makeDatasetFromCollection(null, true, null, null, localState.gribCollection);
  }

  // path/latest.xml
  @Override
  public Catalog makeLatest(String matchPath, String reqPath, URI catURI) throws IOException {
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
        f.format("%s", p);
      }

      synchronized (lock) {
        localState.latest = latest;
        localState.latestPath = f.toString();
      }
    }

    return makeCatalogFromCollection(localState.latest, localState.latestPath, catURI);
  }

  ///////////////////////////////////////////////////////////////////////////


  // LOOK how come we arent using NetadataExtractor ??
  private ThreddsMetadata.GeospatialCoverage extractGeospatial(GribCollectionImmutable.GroupGC group) {
    GdsHorizCoordSys gdsCoordSys = group.getGdsHorizCoordSys();
    LatLonRect llbb = GridCoordSys.getLatLonBoundingBox(gdsCoordSys.proj, gdsCoordSys.getStartX(), gdsCoordSys.getStartY(),
            gdsCoordSys.getEndX(), gdsCoordSys.getEndY());


    double dx = 0.0, dy = 0.0;
    if (gdsCoordSys.isLatLon()) {
      dx = gdsCoordSys.dx;
      dy = gdsCoordSys.dy;
    }

    return new ThreddsMetadata.GeospatialCoverage(llbb, null, dx, dy);
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
      return "Latest Collection for " + dsName;
    }
  }

  protected String getDatasetNameBest(String dsName) {
    if (config.gribConfig != null && config.gribConfig.bestNamer != null) {
      return config.gribConfig.bestNamer;
    } else {
      return "Best " + dsName + " Time Series";
    }
  }

  protected String getDatasetNameTP(String dsName) {
    return "Full Collection Dataset";
  }

  protected String getDatasetNameTwoD(String dsName) {
    return "Full Collection (Reference / Forecast Time) Dataset";
  }

  @Override
  public File getFile(String remaining) {
    try {
      StateGrib localState = (StateGrib) checkState();
      int pos = remaining.lastIndexOf("/");
      final String filename = (pos >= 0) && (remaining.length() > 1) ? remaining.substring(pos + 1) : remaining;

      MFile result = (MFile) findDataset(remaining, localState.gribCollection, new DatasetCreator() {
        @Override
        public Object obtain(GribCollectionImmutable gc, GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) throws IOException {
          return gc.findMFileByName(filename);
        }
      });

      if (result == null) return null;
      return new File(result.getPath());

    } catch (IOException iow) {
      return null;
    }

  }

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

  // see top javadoc for possible URLs
  // returns visitor.obtain(), either a GridDataset or a NetcdfDataset
  private Object findDataset(String matchPath, GribCollectionImmutable topCollection, DatasetCreator visit) throws IOException {
    // LOOK Do we need to strip off a GC in case of bookmarked URLs?

    String[] paths = matchPath.split("/");
    List<String> pathList = (paths.length < 1) ? new ArrayList<String>() : Arrays.asList(paths);
    DatasetAndGroup dg = findDatasetAndGroup(pathList, topCollection);
    if (dg != null)
      return visit.obtain(topCollection, dg.ds, dg.group);

    if (!(topCollection instanceof PartitionCollectionImmutable)) return null;
    PartitionCollectionImmutable pc = (PartitionCollectionImmutable) topCollection;
    return findDatasetPartition(visit, pc, pathList);
  }

  private static class DatasetAndGroup {
    GribCollectionImmutable.Dataset ds;
    GribCollectionImmutable.GroupGC group;

    private DatasetAndGroup(GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) {
      this.ds = ds;
      this.group = group;
    }
  }

  /* case 0: path/dataset (dataset = "")              // single dataset
     case 1: path/dataset (dataset = BEST, TWOD, TP)  // single group
     case 2: path/dataset/groupName
     case 3: path/groupName                           // single dataset not sure this is actually used ??
  */
  private DatasetAndGroup findDatasetAndGroup(List<String> paths, GribCollectionImmutable gc) {
    if (paths.size() < 1 || paths.get(0).length() == 0) {   // case 0: use first dataset,group in the collection
      GribCollectionImmutable.Dataset ds = gc.getDataset(0);
      GribCollectionImmutable.GroupGC dg = ds.getGroup(0);
      return new DatasetAndGroup(ds, dg);
    }

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

    if (paths.size() == 1) {                               // case 3
      String groupName = paths.get(0);
      ds = gc.getDataset(0);
      GribCollectionImmutable.GroupGC g = ds.findGroupById(groupName);
      if (g != null)
        return new DatasetAndGroup(ds, g);
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

    try (GribCollectionImmutable gc = pcp.getGribCollection()) {
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
