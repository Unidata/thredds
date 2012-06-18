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
import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionUpdater;
import thredds.inventory.MFileCollectionManager;
import thredds.inventory.TimePartitionCollection;
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
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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
  static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFcGrib.class);
  static private final String COLLECTION = "collection";
  static private final String VARIABLES = "?metadata=variableMap";
  static private final boolean addLatest = false;  // not ready for use

  /////////////////////////////////////////////////////////////////////////////
  protected class StateGrib extends State {
    TimePartition timePartition;
    GribCollection gribCollection;

    protected StateGrib(StateGrib from) {
      super(from);
      if (from != null) {
        this.timePartition = from.timePartition;
        this.gribCollection = from.gribCollection;
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  private final FeatureCollectionConfig.GribConfig gribConfig;
  private final AtomicBoolean needsUpdate = new AtomicBoolean();
  private final AtomicBoolean needsProto = new AtomicBoolean();
  private boolean first = true;
  private DataFormatType format;

  public InvDatasetFcGrib(InvDatasetImpl parent, String name, String path, FeatureType featureType, FeatureCollectionConfig config) {
    super(parent, name, path, featureType, config);
    this.gribConfig = config.gribConfig;

    Formatter errlog = new Formatter();
    if (config.useIndexOnly)
      this.dcm = TimePartitionCollection.fromExistingIndices(config, errlog);
    else if (config.timePartition != null) {
      this.dcm = TimePartitionCollection.factory(config, errlog);
      this.dcm.setChangeChecker(GribIndex.getChangeChecker());
    } else {
      this.dcm = new MFileCollectionManager(config, errlog);
      this.dcm.setChangeChecker(GribIndex.getChangeChecker());
    }

    // sneak in extra config info
    if (config.gribConfig != null)
      dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);

    String errs = errlog.toString();
    if (errs.length() > 0) logger.warn("{}: CollectionManager parse error = {} ", name, errs);

    tmi.setDataType(FeatureType.GRID); // override GRIB
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
    makeTopDatasets(localState);
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
      CollectionManager.Force ff = (tdsUsingTdm || config.updateConfig.isStatic()) ? CollectionManager.Force.nocheck : CollectionManager.Force.test;

      // update local copy of state, then switch all at once
      StateGrib localState = new StateGrib((StateGrib) state);
      updateCollection(localState, ff);

      makeTopDatasets(localState);
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
      localState.timePartition = TimePartition.factory(format == DataFormatType.GRIB1, (TimePartitionCollection) this.dcm, force, new Formatter());
      localState.gribCollection = null;
      if (previous != null) previous.close(); // LOOK thread safety
      logger.debug("{}: TimePartition object was recreated", getName());

    } else {
      GribCollection previous = localState.gribCollection;
      localState.gribCollection = GribCollection.factory(format == DataFormatType.GRIB1, dcm, force, new Formatter());
      localState.timePartition = null;
      if (previous != null) previous.close(); // LOOK thread safety
      logger.debug("{}: GribCollection object was recreated", getName());
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private void makeTopDatasets(StateGrib localState) {
    List<InvDataset> datasets = new ArrayList<InvDataset>();

    if (localState.timePartition == null) {

      List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(localState.gribCollection.getGroups());
      Collections.sort(groups);
      boolean isSingleGroup = (groups.size() == 1);

      for (GribCollection.GroupHcs group : groups) {
        InvDatasetImpl ds;
        String groupId;
        String dpath;

        if (isSingleGroup) {
          ds = new InvDatasetImpl(this, getName());
          groupId = null;
          dpath = getPath() + "/" + COLLECTION;

        } else {
          ds = new InvDatasetImpl(this, group.getDescription());
          groupId = group.getId();
          dpath = getPath() + "/" + groupId + "/" + COLLECTION;
        }

        ds.setUrlPath(dpath);
        ds.setID(dpath);
        ds.tmi.addVariableMapLink(makeMetadataLink(dpath, VARIABLES));
        addFileDatasets(ds, groupId);

        // metadata is specific to each group
        ds.tmi.setGeospatialCoverage(extractGeospatial(group));
        CalendarDateRange cdr = extractCalendarDateRange(group);
        if (cdr != null) ds.tmi.setTimeCoverage(cdr);

        ds.finish();
        datasets.add(ds);
      }

    } else { // is a time partition

      // the entire collection
      InvCatalogRef ds = new InvCatalogRef(this, COLLECTION, getCatalogHref(COLLECTION));
      //ds.setUrlPath(getPath() + "/" + COLLECTION);
      //ds.setID(getPath() + "/" + COLLECTION);
      ds.finish();
      datasets.add(ds);

      for (TimePartition.Partition dc : localState.timePartition.getPartitions()) {
        String dname = dc.getName();
        ds = new InvCatalogRef(this, dname, getCatalogHref(dname));
        /*dname = StringUtil2.replace(dname, ' ', "_");
        ds.setUrlPath(this.path + "/" + dname);
        ds.setID(this.path + "/" + dname);
        ds.tmi.addVariableMapLink(makeMetadataLink( this.path + "/" + dname, VARIABLES));  */
        //ThreddsMetadata tm = ds.getLocalMetadata();
        //tm.addDocumentation("summary", "Best time series, taking the data from the most recent run available.");
        //ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());

        //addDatasets(ds, null);
        ds.finish();
        datasets.add(ds);
      }
    }

    localState.datasets = datasets;
    this.datasets = datasets;
    finish();
  }

  // file datasets of the partition catalog are InvCatalogRef pointing to "FileCatalogs"
  private void addFileDatasets(InvDatasetImpl parent, String prefix) {
    String name = (prefix == null) ? FILES : prefix + "/" + FILES;
    InvCatalogRef ds = new InvCatalogRef(this, FILES, getCatalogHref(name));
    ds.finish();
    parent.addDataset(ds);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when a catref is requested
  /* Possible catref paths:
      1. path/files/catalog.xml
      2. path/partitionName/files/catalog.xml
      3. path/groupName/files/catalog.xml

      4. path/collection/catalog.xml
      5. path/partitionName/catalog.xml
   */
  @Override
  public InvCatalogImpl makeCatalog(String match, String orgPath, URI baseURI) {
    //logger.debug("{}: make catalog for {} {}", name, match, baseURI);
    StateGrib localState;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    try {
      // top catalog : uses state.datasets previously made in checkState()
      if ((match == null) || (match.length() == 0)) {
        InvCatalogImpl main = makeCatalogTop(baseURI, localState);
        main.addService(virtualService);
        main.getDataset().getLocalMetadataInheritable().setServiceName(virtualService.getName());
        main.finish();
        return main;
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
          return makeFilesCatalog(localState.gribCollection, group, baseURI, localState);
        }

      } else { // time partitions

        if (match.equals(COLLECTION)) {
          return makeGribCollectionCatalog(localState.timePartition, baseURI, localState);
        }

        TimePartition.Partition tpp = localState.timePartition.getPartitionByName(match);
        if (tpp != null) {
          GribCollection gc = tpp.getGribCollection();
          InvCatalogImpl result = makeGribCollectionCatalog(gc, baseURI, localState);
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
            result = makeFilesCatalog(gc, group, baseURI, localState);
          } else {
            GribCollection.GroupHcs group = gc.findGroupById(path[1]);
            if (group == null) return null;
            result = makeFilesCatalog(gc, group, baseURI, localState);
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

  // each partition gets its own catalog, showing the different groups (horiz coord sys)
  private InvCatalogImpl makeGribCollectionCatalog(GribCollection gribCollection, URI baseURI, State localState) throws IOException {

    String collectionName = gribCollection.getName();
    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getCatalogHref(collectionName));  // LOOK ??
    InvCatalogImpl partCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), myURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(collectionName);

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    //if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    partCatalog.addDataset(top);

    // services need to be local
    partCatalog.addService(virtualService);
    top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gribCollection.getGroups());
    Collections.sort(groups);
    boolean isSingleGroup = (groups.size() == 1);

    for (GribCollection.GroupHcs group : groups) {
      InvDatasetImpl ds;
      String groupId = group.getId();
      String dpath;
      String cname;

      if (isSingleGroup) {
        ds = new InvDatasetImpl(this, collectionName);
        dpath = this.path + "/" + collectionName + "/" + COLLECTION;
        cname = collectionName;

      } else {
        ds = new InvDatasetImpl(this, group.getDescription());
        dpath = this.path + "/" + collectionName + "/" + groupId;
        cname = collectionName + "/" + groupId;
      }

      ds.setUrlPath(dpath);
      ds.setID(dpath);
      if (!(gribCollection instanceof TimePartition)) // dont add files for collection dataset
        addFileDatasets(ds, cname);
      ds.tmi.addVariableMapLink(makeMetadataLink(dpath, VARIABLES));

      // metadata is specific to each group
      ds.tmi.setGeospatialCoverage(extractGeospatial(group));
      CalendarDateRange cdr = extractCalendarDateRange(group);
      if (cdr != null) ds.tmi.setTimeCoverage(cdr);

      ds.finish();
      top.addDataset(ds);
    }

    partCatalog.finish();

    return partCatalog;
  }

  // this catalog lists the individual files comprising a grib collection.
  // cant use InvDatasetScan because we might have multiple hcs
  private InvCatalogImpl makeFilesCatalog(GribCollection gc, GribCollection.GroupHcs group, URI baseURI, State localState) throws IOException {

    //String collectionName = gc.getName();
    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    //URI myURI = baseURI.resolve(getCatalogHref(collectionName));
    URI myURI = baseURI.resolve(getCatalogHref(FILES));
    InvCatalogImpl result = new InvCatalogImpl(getFullName(), parent.getVersion(), myURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(FILES);

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    result.addDataset(top);

    // services need to be local
    result.addService(orgService);
    top.getLocalMetadataInheritable().setServiceName(orgService.getName());

    String id = getID();
    if (id == null) id = getPath();

    if (addLatest) {
      InvDatasetImpl ds = new InvDatasetImpl(this, LATEST_DATASET_NAME);
      ds.setUrlPath(this.path + "/" + FILES + "/" + LATEST_DATASET);
      ds.setID(id + "/" + FILES + "/" + LATEST_DATASET);
      ds.setServiceName(LATEST_SERVICE);
      ds.finish();
      top.addDataset(ds);
    }

    for (String f : group.getFilenames()) {
      if (!f.startsWith(topDirectory))
        logger.warn("File {} doesnt start with topDir {}", f, topDirectory);

      String fname = f.substring(topDirectory.length() + 1);
      String path = FILES + "/" + fname;
      InvDatasetImpl ds = new InvDatasetImpl(this, fname);
      ds.setUrlPath(this.path + "/" + path);
      ds.setID(id + "/" + path);
      ds.tmi.addVariableMapLink(makeMetadataLink(this.path + "/" + path, VARIABLES));
      File file = new File(f);
      ds.tm.setDataSize(file.length());
      ds.finish();
      top.addDataset(ds);
    }

    result.finish();
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////

  private String makeMetadataLink(String datasetName, String metadata) {
    String result = context + "/medadata/" + datasetName + metadata;
    return result;
  }

  private ThreddsMetadata.GeospatialCoverage extractGeospatial(GribCollection.GroupHcs group) {
    GdsHorizCoordSys gdsCoordSys = group.hcs; // .gds.makeHorizCoordSys();
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

  @Override
  public ucar.nc2.dt.GridDataset getGridDataset(String matchPath) throws IOException {
    // handle FILES
    GridDataset result = super.getGridDataset(matchPath);
    if (result != null) return result;

    StateGrib localState = null;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    DatasetParse dp = parse(matchPath, localState);
    if (dp == null) return null;

    if (localState.timePartition == null) {
      return localState.gribCollection.getGridDataset(dp.group, dp.filename, gribConfig);

    } else {
      if (dp.partition != null)
        return dp.partition.getGribCollection().getGridDataset(dp.group, dp.filename, gribConfig);
      else
        return localState.timePartition.getGridDataset(dp.group, dp.filename, gribConfig);
    }
  }

  @Override
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    // handle FILES
    NetcdfDataset result = super.getNetcdfDataset(matchPath); // case 7
    if (result != null) return result;

    StateGrib localState = null;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    DatasetParse dp = parse(matchPath, localState);
    if (dp == null) return null;

    if (localState.timePartition == null) {
      return localState.gribCollection.getNetcdfDataset(dp.group, dp.filename, gribConfig); // case 1 and 2

    } else {
      if (dp.partition != null)
        return dp.partition.getGribCollection().getNetcdfDataset(dp.group, dp.filename, gribConfig); // case 4
      else
        return localState.timePartition.getNetcdfDataset(dp.group, dp.filename, gribConfig); // case 3
    }
  }

  /* possible forms of path:
    regular:
      1. COLLECTION               single group
      2. groupName/COLLECTION     multiple groups

   time partition, single group:
      3. name/COLLECTION              overall collection for all partitions
      4. partitionName/COLLECTION     one partition

   time partition, multiple groups:
      5. name/groupName               overall collection for group
      6. partitionName/groupName      collection for group and partition

   all:
      7. FILES/filename
  */
  private DatasetParse parse(String matchPath, StateGrib localState) {
    if ((matchPath == null) || (matchPath.length() == 0)) return null;
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;

    if (paths.length == 2 && paths[0].equals(FILES))
      return new DatasetParse(paths[1]); // case 7

    if (localState.timePartition == null) {
      boolean isCollection = paths[0].equals(COLLECTION);
      String groupName = isCollection ? localState.gribCollection.getGroup(0).getId() : paths[0];
      return new DatasetParse(null, groupName, isCollection); // case 1 and 2

    } else { // is a time partition

      /* if (paths.length == 1) {
        boolean isCollection = paths[0].equals(COLLECTION);
        if (isCollection) {
          String groupName = localState.timePartition.getGroup(0).getId();
          return new DatasetParse(null, groupName, true); // case 3
        }
      } */

      if (paths.length == 2) {
        boolean isCollection = paths[1].equals(COLLECTION);
        TimePartition.Partition tpp = localState.timePartition.getPartitionByName(paths[0]);
        String name = localState.timePartition.getName();
        if (isCollection) {
          if (tpp != null)
            return new DatasetParse(tpp, localState.timePartition.getGroup(0).getId(), true); // case 4 :  overall collection for partition, one group
          else if (paths[0].equals(name))
            return new DatasetParse(null, localState.timePartition.getGroup(0).getId(), true); // case 3 :  overall collection for partition, one group

        } else {
          if (tpp != null)
            return new DatasetParse(tpp, paths[1], false); // case 6
          else
            return new DatasetParse(null, paths[1], true);  // case 5 : overall collection for group, multiple partitions
        }
      }
    }

    return null;
  }

  private class DatasetParse {
    TimePartition.Partition partition; // missing for collection level
    String group;
    String filename; // only for isFile
    boolean isCollection, isFile;

    private DatasetParse(TimePartition.Partition tpp, String group, boolean collection) {
      this.partition = tpp;
      this.group = group;
      isCollection = collection;
    }

    private DatasetParse(String filename) {
      this.filename = filename;
      this.isFile = true;
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
      String paramCategory = cust2.getTableValue("4.1." + vindex.discipline, vindex.category);
      if (paramCategory == null) paramCategory = "Unknown";
      String paramName = cust2.getVariableName(vindex.discipline, vindex.category, vindex.parameter);
      tv.setVocabularyName(paramDisc + " / " + paramCategory + " / " + paramName);
      map.put(vindex.cdmHash, tv);
      return tv;
    }
  }

  private class Vhash {
    int center, subcenter, local, master;
    int discipline, category, param;
  }

}
