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
  static private final String COLLECTION = "collection";
  static private final String GC_DATASET = "GC";
  static private final String BEST_DATASET = "Best";
  static private final String TWOD_DATASET = "TwoD";
  static private final String PARTITION = "partition";

  /////////////////////////////////////////////////////////////////////////////
  protected class StateGrib extends State {
    GribCollection gribCollection;

    protected StateGrib(StateGrib from) {
      super(from);
      if (from != null) {
        this.gribCollection = from.gribCollection;
      }
    }

    @Override
    public State copy() {
      return new StateGrib(this);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  private final FeatureCollectionConfig config;
  private final boolean isGrib1;
  private final boolean isFilePartition;
  private final boolean isDirectoryPartition;

  public InvDatasetFcGrib(InvDatasetImpl parent, String name, String path, FeatureCollectionType fcType, FeatureCollectionConfig config) {
    super(parent, name, path, fcType, config);
    this.config = config;
    this.isGrib1 = config.type == FeatureCollectionType.GRIB1;
    this.isFilePartition = (config.ptype == FeatureCollectionConfig.PartitionType.file);
    this.isDirectoryPartition = (config.ptype == FeatureCollectionConfig.PartitionType.directory);

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

      localState.gribCollection = GribCdmIndex.openGribCollection(this.config, force, logger);
      logger.debug("{}: GribCollection object was recreated", getName());
      if (previous != null) previous.close(); // LOOK may be another thread using - other thread will fail

    } catch (IOException ioe) {
      logger.error("GribFc updateCollection", ioe);
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private InvCatalogImpl makeCatalogFromCollection(URI catURI, String parentName, GribCollection gc) throws IOException, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), catURI);

    if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.LatestFile))
      mainCatalog.addService(InvService.latest);
    mainCatalog.addDataset(makeDatasetFromCollection(parentName, gc));
    mainCatalog.addService(virtualService);
    mainCatalog.finish();

    return mainCatalog;
  }

  private InvDatasetImpl makeDatasetFromCollection( String parentName, GribCollection gc) {
    // StateGrib localState = (StateGrib) state;

    InvDatasetImpl result = new InvDatasetImpl(this);
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
          InvDatasetImpl best = new InvDatasetImpl(this, getDatasetNameBest());
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

        Iterable<GribCollection.GroupGC> groups = ds.getGroups();
        InvDatasetImpl gcds = new InvDatasetImpl(this, getDatasetNameGC());
        String path = pathStart + "/" + GC_DATASET;
        gcds.setUrlPath(path);
        gcds.tmi.addDocumentation("summary", "Single reference time Grib Collection");
        gcds.tmi.addVariableMapLink(makeMetadataLink(path, VARIABLES));
        gcds.tmi.setTimeCoverage(extractCalendarDateRange(groups));
        result.addDataset(gcds);

        // Collections.sort(groups);
        makeDatasetsFromGroups(gcds, groups, isSingleGroup);
      }

    }

    if (gc instanceof PartitionCollection) {
      if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.LatestFile)) {  // LOOK not right
        InvDatasetImpl ds = new InvDatasetImpl(this, getLatestFileName());
        ds.setUrlPath(FILES + "/" + LATEST_DATASET_CATALOG);
        // ds.setID(getPath() + "/" + FILES + "/" + LATEST_DATASET_CATALOG);
        ds.setServiceName(LATEST_SERVICE);
        ds.finish();
        result.addDataset(ds);
        this.addService(InvService.latest);
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

      ////
      1. path/files/catalog.xml
      2. path/partitionName/files/catalog.xml
      3. path/groupName/files/catalog.xml

      4. path/TWOD/catalog.xml
      4. path/BEST/catalog.xml
      5. path/partitionName/catalog.xml
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
        String pname = paths[0];
        PartitionCollection pc = (PartitionCollection) localState.gribCollection;
        PartitionCollection.Partition tpp = pc.getPartitionByName(pname);
        if (tpp == null) return null;
        return makeCatalogFromCollection(catURI, tpp.getName(), tpp.getGribCollection());
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

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }

  @Override
  protected InvCatalogImpl makeCatalogTop(URI catURI, State localState) throws IOException, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), catURI);

    if (config.gribConfig.hasDatasetType(FeatureCollectionConfig.GribDatasetType.LatestFile))
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

/*  @Override
  public InvCatalogImpl makeLatest(String matchPath, String reqPath, URI catURI) {
    StateGrib localState = (StateGrib) checkState();

    GribCollection gc = localState.gribCollection;
    List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gc.getGroups());

     //1. files
     //2. domain1/files
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;

    try {
      if (isGribCollection || isFilePartition) {

        if ((paths.length == 1) && paths[0].equals(FILES)) {
          return makeLatestCatalog(gc, groups.get(0), catURI, localState);  // case 1
        } if ((paths.length == 2) && paths[1].equals(FILES)) {
          return makeLatestCatalog(gc, gc.findGroupById(paths[0]), catURI, localState); // case 2
        }

      } else {

        PartitionCollection pc =  (PartitionCollection) localState.gribCollection;

        if ((paths.length == 1) && paths[0].equals(FILES)) {
          PartitionCollection.Partition p = pc.getPartitionLast();
          GribCollection pgc = p.getGribCollection();
          InvCatalogImpl cat = makeLatestCatalog(pgc, groups.get(0), catURI, localState);  // case 1
          pgc.close();
          return cat;

         } if ((paths.length == 2) && paths[1].equals(FILES)) {
           PartitionCollection.Partition p = pc.getPartitionByName(paths[0]);
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
  }   */

    // this catalog lists the individual files comprising a grib collection.
  // cant use InvDatasetScan because we might have multiple hcs
/*  private InvCatalogImpl makeLatestCatalog(GribCollection gc, GribCollection.GroupHcs group, URI catURI, State localState) throws IOException {

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
  } */

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

  protected String getDatasetNameBest() {
    if (config.gribConfig != null && config.gribConfig.bestNamer != null) {
      return config.gribConfig.bestNamer;
    } else {
      return "Best "+name +" Time Series";
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
      GridDataset gd = gc.getGridDataset(dp.dataset, dp.group, dp.filename, config, null, logger);
      gc.close(); // LOOK WTF ??
      return gd;
    }

    return localState.gribCollection.getGridDataset(dp.dataset, dp.group, dp.filename, config, null, logger);
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
      NetcdfDataset gd = gc.getNetcdfDataset(dp.dataset, dp.group, dp.filename, config, null, logger);
      gc.close();
      return gd;
    }

    return localState.gribCollection.getNetcdfDataset(dp.dataset, dp.group, dp.filename, config, null, logger);
  }

  /* possible forms of path:

    regular, single group:
      1. dataset (BEST, TWOD, GC)

     regular, multiple group:
      2. dataset/groupName

     //////////////////////////////
     partition, single group:
      4. dataset/PARTITION/partitionName

     partition, multiple groups:
      5. dataset/PARTITION/partitionName/groupName

   all:
      7. FILES/filename
      8. allow COLLECTION as alias for BEST
  */
  private DatasetParse parse(String matchPath, StateGrib localState) {
    if ((matchPath == null) || (matchPath.length() == 0)) return null;
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;

    GribCollection.Dataset ds = localState.gribCollection.findDataset(paths[0]);

    if (ds != null) {
      boolean isSingleGroup = ds.getGroupsSize() == 1;
      if (paths.length == 1) {                               // case 1
        if (!isSingleGroup) return null;
        GribCollection.GroupGC g = ds.getGroup(0);
        return new DatasetParse(null, paths[0], g.getId());
      }

      if (paths.length == 2) {                              // case 2
        String groupName = paths[1];
        GribCollection.GroupGC g = ds.findGroupById(groupName);
        if (g != null)
          return new DatasetParse(null, paths[0], groupName);
        else
          return null;
      }
    }

    if (localState.gribCollection instanceof PartitionCollection) {
      PartitionCollection pc = (PartitionCollection) localState.gribCollection;
      PartitionCollection.Partition tpp = pc.getPartitionByName(paths[0]);
      if (tpp == null) return null;
      return new DatasetParse(tpp);
    }


    /* if (isGribCollection || isFilePartition) {
      boolean isBest = paths[0].equals(BEST_DATASET) || paths[0].equals(COLLECTION);
      String groupName = isBest ? localState.gribCollection.getGroup(0).getId() : paths[0];
      return new DatasetParse(null, groupName); // case 1 and 2

    } else { // is a time partition
      PartitionCollection pc = (PartitionCollection) localState.gribCollection;

      List<GribCollection.GroupHcs> groups = localState.gribCollection.getGroups();
      boolean isSingleGroup = groups.size() == 1;

      if (paths.length == 1) {
        boolean isBest = paths[0].equals(BEST_DATASET) || paths[0].equals(COLLECTION);
        if (isBest) {
          String groupName = localState.gribCollection.getGroup(0).getId();
          return new DatasetParse(null, groupName); // case 3
        }
      }

      if (paths.length == 2) {
        boolean isBest = paths[1].equals(BEST_DATASET) || paths[1].equals(COLLECTION);
        if (isSingleGroup) {
          PartitionCollection.Partition tpp = pc.getPartitionByName(paths[0]);
          if (tpp != null)
            return new DatasetParse(tpp, pc.getGroup(0).getId()); // case 4 :  overall collection for partition, one group
        } else {
          return new DatasetParse(null, paths[0]);  // case 5 : overall collection for group, multiple partitions
        }
      }

      if (paths.length == 3) {
         boolean isBest = paths[2].equals(BEST_DATASET) || paths[2].equals(COLLECTION);
         PartitionCollection.Partition tpp = pc.getPartitionByName(paths[0]);
         String groupName = paths[1];
         return new DatasetParse(tpp, groupName); // case 6
      }
    }  */

    return null;
  }

  private class DatasetParse {
    PartitionCollection.Partition partition; // missing for collection level
    String dataset;
    String group;
    String filename; // only for isFile
    //FeatureCollectionConfig.GribDatasetType dtype;
    //boolean isFile;

    private DatasetParse(PartitionCollection.Partition tpp, String dataset, String group) {
      this.partition = tpp;
      this.dataset = dataset;
      this.group = group;
    }

    private DatasetParse(String filename) {
      this.filename = filename;
      //this.isFile = true;
    }

    private DatasetParse(PartitionCollection.Partition tpp) {
      this.partition = tpp;
    }

  }

}
